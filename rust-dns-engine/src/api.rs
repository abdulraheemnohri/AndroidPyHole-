use warp::Filter;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;

pub async fn run_server(blocklist: Arc<Mutex<Blocklist>>) {
    let bl_stats = blocklist.clone();
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

    // DNS-over-HTTPS (DoH) Blueprint
    let bl_doh = blocklist.clone();
    let doh_query = warp::path("dns-query")
        .and(warp::query::<std::collections::HashMap<String, String>>())
        .map(move |params: std::collections::HashMap<String, String>| {
            if let Some(dns_param) = params.get("dns") {
                // Decode base64 DNS packet
                // let packet = base64::decode(dns_param);
                // Process through filter
                warp::reply::with_status("DoH Response Placeholder", warp::http::StatusCode::OK)
            } else {
                warp::reply::with_status("Missing DNS param", warp::http::StatusCode::BAD_REQUEST)
            }
        });

    let routes = stats.or(doh_query);

    println!("PyHoleX Mesh API & DoH Server active on 127.0.0.1:8080");
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}
