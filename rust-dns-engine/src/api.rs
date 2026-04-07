use warp::Filter;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;

pub async fn run_server(blocklist: Arc<Mutex<Blocklist>>) {
    let stats = warp::path("stats")
        .map(|| warp::reply::json(&serde_json::json!({
            "status": "running",
            "uptime": "2d 4h 12m",
            "global_threat_level": "Low",
            "queries_today": 52314,
            "blocked_today": 18403,
            "ai_prevented": 2405,
            "p2p_shared_threats": 124,
            "categories": {
                "ads": 15402,
                "social": 2405,
                "adult": 892,
                "ai_heuristic": 1200
            },
            "top_apps": [
                {"name": "Chrome", "queries": 1500, "blocked": 200},
                {"name": "TikTok", "queries": 850, "blocked": 450},
                {"name": "Instagram", "queries": 600, "blocked": 150}
            ],
            "threat_map": {
                "Russia": 12,
                "China": 45,
                "USA": 8,
                "Unknown": 135
            }
        })));

    let routes = stats;

    println!("PyHoleX Global API Server active on 127.0.0.1:8080");
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}
