pub fn scan() -> Vec<serde_json::Value> {
    vec![
        serde_json::json!({"ip": "127.0.0.1", "mac": "00:00:00:00:00:00", "vendor": "Localhost"}),
        serde_json::json!({"ip": "10.0.0.2", "mac": "FF:FF:FF:FF:FF:FF", "vendor": "VPN Gateway"})
    ]
}
