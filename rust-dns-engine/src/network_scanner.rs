pub fn scan() -> Vec<serde_json::Value> {
    // Blueprint for ARP table scanning or mDNS discovery
    vec![
        serde_json::json!({"ip": "192.168.1.1", "mac": "00:11:22:33:44:55", "vendor": "Router"}),
        serde_json::json!({"ip": "192.168.1.50", "mac": "AA:BB:CC:DD:EE:FF", "vendor": "Android Device"})
    ]
}
