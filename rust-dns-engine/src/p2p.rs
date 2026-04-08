use std::collections::{HashSet, HashMap};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Clone)]
pub struct Peer {
    pub id: String,
    pub last_seen: u64,
}

pub struct P2PNode {
    pub shared_threats: HashSet<String>,
    pub peers: HashMap<String, Peer>,
    pub local_node_id: String,
}

impl P2PNode {
    pub fn new() -> Self {
        P2PNode {
            shared_threats: HashSet::new(),
            peers: HashMap::new(),
            local_node_id: format!("node_{}", SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs()),
        }
    }

    pub async fn gossip_sync(&mut self) {
        println!("Node {} initiating gossip protocol sync...", self.local_node_id);
        // Blueprint for P2P threat sharing
        // In real use, this would broadcast new blocked domains to known peers
        self.shared_threats.insert("decentralized-malware-domain.io".to_string());
    }

    pub fn register_peer(&mut self, id: String) {
        let now = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs();
        self.peers.insert(id.clone(), Peer { id, last_seen: now });
    }

    pub fn is_p2p_threat(&self, domain: &str) -> bool {
        self.shared_threats.contains(domain)
    }
}
