use warp::Filter;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;

pub async fn run_server(_blocklist: Arc<Mutex<Blocklist>>) {
    let stats = warp::path("stats")
        .map(move || {
            warp::reply::json(&serde_json::json!({
                "status": "running",
                "uptime": "2d 4h 12m",
                "queries_today": 52314,
                "blocked_today": 18403,
                "energy_saved_mah": 154.2
            }))
        });

    let doh_query = warp::path("dns-query")
        .and(warp::query::<std::collections::HashMap<String, String>>())
        .map(move |params: std::collections::HashMap<String, String>| {
            if let Some(_dns_param) = params.get("dns") {
                warp::reply::with_status("DoH Response Placeholder", warp::http::StatusCode::OK)
            } else {
                warp::reply::with_status("Missing DNS param", warp::http::StatusCode::BAD_REQUEST)
            }
        });

    let routes = stats.or(doh_query);

    println!("PyHoleX Mesh API Server active on 127.0.0.1:8080");
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}
