use std::collections::{HashSet, HashMap};
use crate::classifier::AIHeuristic;
use crate::p2p::P2PNode;

pub enum DomainCategory {
    Ads,
    Social,
    Adult,
    Malware,
    Tracking,
    _Custom,
}

pub struct Blocklist {
    pub domains: HashMap<String, DomainCategory>,
    pub wildcards: Vec<String>,
    pub app_rules: HashMap<String, HashSet<String>>,
    pub ai: AIHeuristic,
    pub p2p: P2PNode,
}

impl Blocklist {
    pub fn new() -> Self {
        let mut domains = HashMap::new();
        domains.insert("doubleclick.net".to_string(), DomainCategory::Ads);
        Blocklist {
            domains,
            wildcards: vec!["ads.".to_string(), "track.".to_string()],
            app_rules: HashMap::new(),
            ai: AIHeuristic::new(),
            p2p: P2PNode::new(),
        }
    }

    pub fn is_blocked(&self, domain: &str, app_id: Option<&str>) -> (bool, Option<String>) {
        if let Some(_cat) = self.domains.get(domain) {
            return (true, Some("Blocklist Match".to_string()));
        }

        for w in &self.wildcards {
            if domain.starts_with(w) {
                return (true, Some(format!("Wildcard Match ({})", w)));
            }
        }

        if self.p2p.is_p2p_threat(domain) {
            return (true, Some("P2P Community Block".to_string()));
        }

        let (is_ai_threat, score) = self.ai.analyze(domain);
        if is_ai_threat {
            return (true, Some(format!("AI Preventative (Score: {:.2})", score)));
        }

        if let Some(aid) = app_id {
            if let Some(blocked) = self.app_rules.get(aid) {
                if blocked.contains(domain) { return (true, Some("App Restriction".to_string())); }
            }
        }

        (false, None)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_is_blocked() {
        let bl = Blocklist::new();
        let (blocked, _) = bl.is_blocked("doubleclick.net", None);
        assert!(blocked);
        let (blocked, _) = bl.is_blocked("google.com", None);
        assert!(!blocked);
    }
}
