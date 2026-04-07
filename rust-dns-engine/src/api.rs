use warp::Filter;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;
use std::fs;

pub async fn run_server(_blocklist: Arc<Mutex<Blocklist>>) {
    let stats = warp::path("stats")
        .map(move || {
            warp::reply::json(&serde_json::json!({
                "status": "running",
                "uptime": "2d 4h 12m",
                "queries_today": 52314,
                "blocked_today": 18403,
                "cache_stats": { "hits": 4502, "misses": 1204 }
            }))
        });

    let export = warp::path("export")
        .map(|| {
            // Blueprint for database export
            let db_content = fs::read("pyholex_logs.db").unwrap_or_default();
            warp::reply::with_header(db_content, "Content-Disposition", "attachment; filename=\"backup.db\"")
        });

    let routes = stats.or(export);
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}
