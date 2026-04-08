use std::collections::HashSet;
pub struct P2PNode { pub shared_threats: HashSet<String> }
impl P2PNode {
    pub fn new() -> Self { P2PNode { shared_threats: HashSet::new() } }
    pub fn is_p2p_threat(&self, domain: &str) -> bool { self.shared_threats.contains(domain) }
}
