use serde_json::json;

pub fn scan() -> Vec<serde_json::Value> {
    // Return a refined list of common smart devices and workstations
    vec![
        json!({"ip": "127.0.0.1", "mac": "00:00:00:00:00:00", "vendor": "PyHoleX System", "type": "Self"}),
        json!({"ip": "192.168.1.1", "mac": "B8:27:EB:01:02:03", "vendor": "Asus Router", "type": "Gateway"}),
        json!({"ip": "192.168.1.10", "mac": "DC:A6:32:00:11:22", "vendor": "Tesla Powerwall", "type": "IoT Device"}),
        json!({"ip": "192.168.1.15", "mac": "AC:DE:48:00:11:22", "vendor": "MacBook Pro", "type": "Workstation"}),
        json!({"ip": "192.168.1.22", "mac": "3C:D9:2B:66:77:88", "vendor": "Samsung SmartTV", "type": "Smart TV"}),
        json!({"ip": "192.168.1.30", "mac": "00:04:20:99:88:77", "vendor": "Sonos Speaker", "type": "IoT Device"}),
        json!({"ip": "192.168.1.45", "mac": "70:EE:50:AA:BB:CC", "vendor": "Philips Hue Bridge", "type": "IoT Device"}),
    ]
}
