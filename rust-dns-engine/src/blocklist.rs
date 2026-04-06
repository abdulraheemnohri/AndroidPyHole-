use crate::p2p::P2PNode;
use std::collections::{HashSet, HashMap};
use crate::classifier::AIHeuristic;

pub enum DomainCategory {
    Ads,
    Social,
    Adult,
    Malware,
    Tracking,
    Custom,
}

pub struct Blocklist {
    pub domains: HashMap<String, DomainCategory>,
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
            app_rules: HashMap::new(),
            ai: AIHeuristic::new(),
            p2p: P2PNode::new(),
        }
    }

    pub fn is_blocked(&self, domain: &str, app_id: Option<&str>) -> (bool, Option<String>) {
        if let Some(cat) = self.domains.get(domain) {
            return (true, Some("Category Match".to_string()));
        }

        if self.p2p.is_p2p_threat(domain) { return (true, Some("P2P Intelligence".to_string())); }
        let (is_ai_threat, score) = self.ai.analyze(domain);
        if is_ai_threat {
            return (true, Some(format!("AI Heuristic (Score: {:.2})", score)));
        }

        if let Some(aid) = app_id {
            if let Some(blocked) = self.app_rules.get(aid) {
                if blocked.contains(domain) { return (true, Some("App Rule".to_string())); }
            }
        }

        (false, None)
    }
}
