use std::collections::{HashSet, HashMap};
use crate::classifier::AIHeuristic;
use crate::p2p::P2PNode;

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
    pub wildcards: Vec<String>, // *.example.com
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
        // 1. Precise Match
        if let Some(cat) = self.domains.get(domain) {
            return (true, Some("Blocklist Match".to_string()));
        }

        // 2. Wildcard / Prefix Match
        for w in &self.wildcards {
            if domain.starts_with(w) {
                return (true, Some(format!("Wildcard Match ({})", w)));
            }
        }

        // 3. P2P Intelligence
        if self.p2p.is_p2p_threat(domain) {
            return (true, Some("P2P Community Block".to_string()));
        }

        // 4. AI Heuristic
        let (is_ai_threat, score) = self.ai.analyze(domain);
        if is_ai_threat {
            return (true, Some(format!("AI Preventative (Score: {:.2})", score)));
        }

        // 5. App Rules
        if let Some(aid) = app_id {
            if let Some(blocked) = self.app_rules.get(aid) {
                if blocked.contains(domain) { return (true, Some("App Restriction".to_string())); }
            }
        }

        (false, None)
    }
}
