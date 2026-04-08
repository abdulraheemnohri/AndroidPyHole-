pub struct AIHeuristic {
    suspicious_keywords: Vec<&'static str>,
}

impl AIHeuristic {
    pub fn new() -> Self {
        AIHeuristic {
            suspicious_keywords: vec!["malware", "phish", "scam", "free-vpn", "ads-track"],
        }
    }

    pub fn calculate_entropy(s: &str) -> f64 {
        let mut frequencies = std::collections::HashMap::new();
        for c in s.chars() {
            *frequencies.entry(c).or_insert(0) += 1;
        }
        let len = s.len() as f64;
        frequencies.values().map(|&count| {
            let p = count as f64 / len;
            -p * p.log2()
        }).sum()
    }

    pub fn analyze(&self, domain: &str) -> (bool, f64) {
        let entropy = Self::calculate_entropy(domain);
        let mut score = 0.0;

        // 1. Check for suspicious keywords
        for kw in &self.suspicious_keywords {
            if domain.contains(kw) {
                score += 0.5;
            }
        }

        // 2. High entropy check (common for DGA - Domain Generation Algorithms)
        if entropy > 3.5 {
            score += 0.4;
        }

        (score > 0.7, score)
    }
}
