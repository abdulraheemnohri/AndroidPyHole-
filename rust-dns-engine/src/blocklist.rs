use std::collections::HashSet;
use std::fs::File;
use std::io::{BufRead, BufReader};

pub struct Blocklist {
    domains: HashSet<String>,
}

impl Blocklist {
    pub fn new() -> Self {
        let mut domains = HashSet::new();
        // Load from local storage if exists
        if let Ok(file) = File::open("blocklists/list.txt") {
            let reader = BufReader::new(file);
            for line in reader.lines() {
                if let Ok(domain) = line {
                    let trimmed = domain.trim();
                    if !trimmed.is_empty() && !trimmed.starts_with('#') {
                        domains.insert(trimmed.to_string());
                    }
                }
            }
        }
        Blocklist { domains }
    }

    pub fn is_blocked(&self, domain: &str) -> bool {
        self.domains.contains(domain)
    }

    pub fn add(&mut self, domain: String) {
        self.domains.insert(domain);
    }

    pub fn remove(&mut self, domain: &str) {
        self.domains.remove(domain);
    }
}
