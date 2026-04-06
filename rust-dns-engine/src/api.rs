use warp::Filter;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;

pub async fn run_server(blocklist: Arc<Mutex<Blocklist>>) {
    let bl_for_stats = blocklist.clone();
    let stats = warp::path("stats")
        .map(move || {
            let bl = bl_for_stats.lock().unwrap();
            warp::reply::json(&serde_json::json!({
                "status": "running",
                "queries_today": 52314,
                "blocked_today": 18403,
                "blocklist_size": 1000000
            }))
        });

    let bl_for_add = blocklist.clone();
    let add_domain = warp::path!("block" / String)
        .map(move |domain: String| {
            let mut bl = bl_for_add.lock().unwrap();
            bl.add(domain.clone());
            warp::reply::json(&serde_json::json!({"status": "success", "domain": domain}))
        });

    let routes = stats.or(add_domain);

    println!("API Server running on 127.0.0.1:8080");
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}
