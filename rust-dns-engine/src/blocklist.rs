use std::collections::HashSet;
use regex::Regex;
use serde::{Serialize, Deserialize};
use std::fs;

#[derive(Serialize, Deserialize, Clone, Copy, PartialEq, Debug)]
pub enum ShieldProfile { Standard, Strict, DNSOnly }

#[derive(Serialize, Deserialize, Clone)]
pub struct BlocklistConfig {
    pub enabled_urls: Vec<String>,
    pub custom_blacklist: HashSet<String>,
    pub custom_whitelist: HashSet<String>,
    pub regex_blacklist: Vec<String>,
    pub parental_control_enabled: bool,
    pub active_profile: ShieldProfile,
    pub upstream_dns: String,
}

impl Default for BlocklistConfig {
    fn default() -> Self {
        BlocklistConfig {
            enabled_urls: vec!["https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts".to_string()],
            custom_blacklist: HashSet::new(),
            custom_whitelist: HashSet::new(),
            regex_blacklist: Vec::new(),
            parental_control_enabled: false,
            active_profile: ShieldProfile::Standard,
            upstream_dns: "8.8.8.8:53".to_string(),
        }
    }
}

pub struct Blocklist {
    pub domains: HashSet<String>,
    pub whitelist: HashSet<String>,
    pub blacklist: HashSet<String>,
    pub urls: Vec<String>,
    pub regex_cache: Vec<Regex>,
    pub parental_control: bool,
    pub profile: ShieldProfile,
    pub upstream_dns: String,
}

impl Blocklist {
    pub fn new() -> Self {
        let config = Self::load_config().unwrap_or_default();
        Blocklist {
            domains: HashSet::new(),
            whitelist: config.custom_whitelist,
            blacklist: config.custom_blacklist,
            urls: config.enabled_urls,
            regex_cache: config.regex_blacklist.iter().filter_map(|s| Regex::new(s).ok()).collect(),
            parental_control: config.parental_control_enabled,
            profile: config.active_profile,
            upstream_dns: config.upstream_dns,
        }
    }

    fn load_config() -> Option<BlocklistConfig> {
        let data = fs::read_to_string("pyholex_config.json").ok()?;
        serde_json::from_str(&data).ok()
    }

    pub fn save_config(&self) {
        let config = BlocklistConfig {
            enabled_urls: self.urls.clone(),
            custom_blacklist: self.blacklist.clone(),
            custom_whitelist: self.whitelist.clone(),
            regex_blacklist: self.regex_cache.iter().map(|r| r.as_str().to_string()).collect(),
            parental_control_enabled: self.parental_control,
            active_profile: self.profile,
            upstream_dns: self.upstream_dns.clone(),
        };
        if let Ok(data) = serde_json::to_string_pretty(&config) {
            let _ = fs::write("pyholex_config.json", data);
        }
    }

    pub fn is_blocked(&self, domain: &str) -> (bool, Option<String>) {
        if self.profile == ShieldProfile::DNSOnly { return (false, None); }
        let domain_clean = domain.trim_end_matches('.').to_lowercase();
        if self.whitelist.contains(&domain_clean) { return (false, Some("Whitelisted".to_string())); }
        if self.blacklist.contains(&domain_clean) { return (true, Some("User Blacklisted".to_string())); }
        for re in &self.regex_cache { if re.is_match(&domain_clean) { return (true, Some("Regex Match".to_string())); } }
        if self.domains.contains(&domain_clean) { return (true, Some("Blocklist Match".to_string())); }
        if self.parental_control {
            for kw in &["porn", "sex", "gamble", "casino"] {
                if domain_clean.contains(kw) { return (true, Some("Parental Control Block".to_string())); }
            }
        }
        if self.profile == ShieldProfile::Strict {
            if domain_clean.starts_with("analytics.") || domain_clean.starts_with("telemetry.") {
                return (true, Some("Strict Mode Block".to_string()));
            }
        }
        (false, None)
    }

    pub async fn sync_remote(&mut self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut new_domains = HashSet::new();
        for url in &self.urls {
            if let Ok(resp) = reqwest::get(url).await {
                if let Ok(text) = resp.text().await {
                    for line in text.lines() {
                        if line.starts_with("0.0.0.0 ") || line.starts_with("127.0.0.1 ") {
                            let parts: Vec<&str> = line.split_whitespace().collect();
                            if parts.len() >= 2 { new_domains.insert(parts[1].to_string()); }
                        }
                    }
                }
            }
        }
        self.domains = new_domains;
        self.save_config();
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_standard_blocking() {
        let mut bl = Blocklist::new();
        bl.domains.insert("ads.example.com".to_string());
        let (blocked, _) = bl.is_blocked("ads.example.com");
        assert!(blocked);
    }

    #[test]
    fn test_whitelist() {
        let mut bl = Blocklist::new();
        bl.domains.insert("google.com".to_string());
        bl.whitelist.insert("google.com".to_string());
        let (blocked, _) = bl.is_blocked("google.com");
        assert!(!blocked);
    }

    #[test]
    fn test_strict_mode() {
        let mut bl = Blocklist::new();
        bl.profile = ShieldProfile::Strict;
        let (blocked, _) = bl.is_blocked("analytics.google.com");
        assert!(blocked);
    }
}
