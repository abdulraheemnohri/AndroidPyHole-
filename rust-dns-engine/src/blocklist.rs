use std::collections::{HashSet, HashMap};
use regex::Regex;
use serde::{Serialize, Deserialize};
use std::fs;
use crate::classifier::AIHeuristic;
use crate::p2p::P2PNode;
use std::path::PathBuf;
use chrono::{DateTime, Utc};

#[derive(Serialize, Deserialize, Clone, Copy, PartialEq, Debug)]
pub enum ShieldProfile { Standard, Strict, DNSOnly }

#[derive(Serialize, Deserialize, Clone, Copy, PartialEq, Debug)]
pub enum PrivacyLevel { Default, Anonymous, Ghost }

#[derive(Serialize, Deserialize, Clone)]
pub struct BlocklistConfig {
    pub enabled_urls: Vec<String>,
    pub custom_blacklist: HashSet<String>,
    pub custom_whitelist: HashSet<String>,
    pub regex_blacklist: Vec<String>,
    pub local_mappings: HashMap<String, String>,
    pub parental_control_enabled: bool,
    pub ai_guard_enabled: bool,
    pub privacy_level: PrivacyLevel,
    pub active_profile: ShieldProfile,
    pub upstream_dns: String,
    pub keyword_blocks: Vec<String>,
    pub dashboard_password: Option<String>,
    pub log_retention_days: i64,
    pub excluded_apps: Vec<String>,
    pub last_sync: Option<DateTime<Utc>>,
}

impl Default for BlocklistConfig {
    fn default() -> Self {
        let mut mappings = HashMap::new();
        mappings.insert("pi.hole".to_string(), "127.0.0.1".to_string());
        BlocklistConfig {
            enabled_urls: vec!["https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts".to_string()],
            custom_blacklist: HashSet::new(),
            custom_whitelist: HashSet::new(),
            regex_blacklist: Vec::new(),
            local_mappings: mappings,
            parental_control_enabled: false,
            ai_guard_enabled: true,
            privacy_level: PrivacyLevel::Default,
            active_profile: ShieldProfile::Standard,
            upstream_dns: "1.1.1.1:53".to_string(),
            keyword_blocks: vec!["porn".to_string(), "sex".to_string(), "gamble".to_string(), "casino".to_string()],
            dashboard_password: None,
            log_retention_days: 7,
            excluded_apps: Vec::new(),
            last_sync: None,
        }
    }
}

pub struct Blocklist {
    pub domains: HashSet<String>,
    pub whitelist: HashSet<String>,
    pub blacklist: HashSet<String>,
    pub urls: Vec<String>,
    pub regex_cache: Vec<Regex>,
    pub local_mappings: HashMap<String, String>,
    pub parental_control: bool,
    pub ai_guard: bool,
    pub privacy: PrivacyLevel,
    pub password: Option<String>,
    pub retention: i64,
    pub excluded_apps: Vec<String>,
    pub last_sync: Option<DateTime<Utc>>,
    pub p2p_node: P2PNode,
    pub ai_engine: AIHeuristic,
    pub profile: ShieldProfile,
    pub upstream_dns: String,
    pub keyword_blocks: Vec<String>,
    config_path: PathBuf,
}

impl Blocklist {
    pub fn new_with_path(path: PathBuf) -> Self {
        let config = Self::load_config(&path).unwrap_or_default();
        Blocklist {
            domains: HashSet::with_capacity(100_000),
            whitelist: config.custom_whitelist,
            blacklist: config.custom_blacklist,
            urls: config.enabled_urls,
            regex_cache: config.regex_blacklist.iter().filter_map(|s| Regex::new(s).ok()).collect(),
            local_mappings: config.local_mappings,
            parental_control: config.parental_control_enabled,
            ai_guard: config.ai_guard_enabled,
            privacy: config.privacy_level,
            password: config.dashboard_password,
            retention: config.log_retention_days,
            excluded_apps: config.excluded_apps,
            last_sync: config.last_sync,
            p2p_node: P2PNode::new(),
            ai_engine: AIHeuristic::new(),
            profile: config.active_profile,
            upstream_dns: config.upstream_dns,
            keyword_blocks: config.keyword_blocks,
            config_path: path,
        }
    }

    fn load_config(path: &PathBuf) -> Option<BlocklistConfig> {
        let data = fs::read_to_string(path).ok()?;
        serde_json::from_str(&data).ok()
    }

    pub fn save_config(&self) {
        let config = BlocklistConfig {
            enabled_urls: self.urls.clone(),
            custom_blacklist: self.blacklist.clone(),
            custom_whitelist: self.whitelist.clone(),
            regex_blacklist: self.regex_cache.iter().map(|r| r.as_str().to_string()).collect(),
            local_mappings: self.local_mappings.clone(),
            parental_control_enabled: self.parental_control,
            ai_guard_enabled: self.ai_guard,
            privacy_level: self.privacy,
            active_profile: self.profile,
            upstream_dns: self.upstream_dns.clone(),
            keyword_blocks: self.keyword_blocks.clone(),
            dashboard_password: self.password.clone(),
            log_retention_days: self.retention,
            excluded_apps: self.excluded_apps.clone(),
            last_sync: self.last_sync,
        };
        if let Ok(data) = serde_json::to_string_pretty(&config) {
            let _ = fs::write(&self.config_path, data);
        }
    }

    pub fn is_blocked(&self, domain: &str) -> (bool, Option<String>) {
        if self.profile == ShieldProfile::DNSOnly { return (false, None); }
        let domain_clean = domain.trim_end_matches('.').to_lowercase();
        if self.whitelist.contains(&domain_clean) { return (false, Some("Whitelisted".to_string())); }
        if self.blacklist.contains(&domain_clean) { return (true, Some("User Blacklisted".to_string())); }
        for re in &self.regex_cache { if re.is_match(&domain_clean) { return (true, Some("Regex Match".to_string())); } }
        if self.domains.contains(&domain_clean) { return (true, Some("Blocklist Match".to_string())); }
        if self.ai_guard {
            let (is_ai_threat, confidence) = self.ai_engine.analyze(&domain_clean);
            if is_ai_threat && confidence > 0.8 {
                return (true, Some(format!("AI Heuristic Block ({:.0}%)", confidence * 100.0)));
            }
        }
        if self.p2p_node.is_p2p_threat(&domain_clean) { return (true, Some("P2P Threat".to_string())); }
        if self.parental_control {
            for kw in &self.keyword_blocks { if domain_clean.contains(kw) { return (true, Some("Parental Control".to_string())); } }
        }
        if self.profile == ShieldProfile::Strict {
            if domain_clean.starts_with("analytics.") || domain_clean.starts_with("telemetry.") || domain_clean.contains("track") {
                return (true, Some("Strict Mode".to_string()));
            }
        }
        (false, None)
    }

    pub fn resolve_local(&self, domain: &str) -> Option<String> {
        let domain_clean = domain.trim_end_matches('.').to_lowercase();
        self.local_mappings.get(&domain_clean).cloned()
    }

    pub async fn sync_remote(&mut self) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let mut new_domains = HashSet::with_capacity(50_000);
        for url in &self.urls {
            if let Ok(resp) = reqwest::get(url).await {
                if let Ok(text) = resp.text().await {
                    for line in text.lines() {
                        let line = line.trim();
                        if line.is_empty() || line.starts_with('#') { continue; }
                        let domain = if line.starts_with("0.0.0.0 ") || line.starts_with("127.0.0.1 ") {
                            line.split_whitespace().nth(1)
                        } else { Some(line) };
                        if let Some(d) = domain { new_domains.insert(d.to_lowercase()); }
                    }
                }
            }
        }
        self.domains = new_domains;
        self.domains.shrink_to_fit();
        self.last_sync = Some(Utc::now());
        self.save_config();
        Ok(())
    }
}
