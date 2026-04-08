use warp::Filter;
use std::sync::Arc;
use tokio::sync::Mutex;
use crate::blocklist::{Blocklist, ShieldProfile};
use crate::logger;
use crate::network_scanner;

pub async fn run_server(blocklist: Arc<Mutex<Blocklist>>) {
    let bl_for_stats = blocklist.clone();
    let stats = warp::path("stats")
        .then(move || {
            let bl_clone = bl_for_stats.clone();
            async move {
                let bl = bl_clone.lock().await;
                let mut s = logger::get_stats().unwrap_or_else(|_| serde_json::json!({}));
                if let Some(obj) = s.as_object_mut() {
                    obj.insert("domains_loaded".to_string(), serde_json::json!(bl.domains.len()));
                    obj.insert("parental_control".to_string(), serde_json::json!(bl.parental_control));
                    obj.insert("profile".to_string(), match bl.profile {
                        ShieldProfile::Standard => serde_json::json!("standard"),
                        ShieldProfile::Strict => serde_json::json!("strict"),
                        ShieldProfile::DNSOnly => serde_json::json!("dns_only"),
                    });
                    obj.insert("upstream_dns".to_string(), serde_json::json!(bl.upstream_dns));
                }
                warp::reply::json(&s)
            }
        });

    let bl_for_profile = blocklist.clone();
    let set_profile = warp::path!("profile" / String)
        .and(warp::post())
        .then(move |p: String| {
            let bl_clone = bl_for_profile.clone();
            async move {
                let mut bl = bl_clone.lock().await;
                bl.profile = match p.as_str() {
                    "strict" => ShieldProfile::Strict,
                    "dns_only" => ShieldProfile::DNSOnly,
                    _ => ShieldProfile::Standard,
                };
                bl.save_config();
                warp::reply::json(&serde_json::json!({"status": "profile_changed"}))
            }
        });

    let bl_for_upstream = blocklist.clone();
    let set_upstream = warp::path!("upstream" / String)
        .and(warp::post())
        .then(move |u: String| {
            let bl_clone = bl_for_upstream.clone();
            async move {
                let mut bl = bl_clone.lock().await;
                bl.upstream_dns = u;
                bl.save_config();
                warp::reply::json(&serde_json::json!({"status": "upstream_changed"}))
            }
        });

    let bl_for_urls = blocklist.clone();
    let urls = warp::path("urls")
        .and(warp::get())
        .then(move || {
            let bl_clone = bl_for_urls.clone();
            async move {
                let bl = bl_clone.lock().await;
                warp::reply::json(&bl.urls)
            }
        });

    let logs = warp::path("logs")
        .map(|| {
            match logger::get_recent_logs(100) {
                Ok(l) => warp::reply::json(&l),
                Err(_) => warp::reply::json(&Vec::<String>::new())
            }
        });

    let bl_for_toggle = blocklist.clone();
    let toggle_parental = warp::path("parental")
        .and(warp::post())
        .then(move || {
            let bl_clone = bl_for_toggle.clone();
            async move {
                let mut bl = bl_clone.lock().await;
                bl.parental_control = !bl.parental_control;
                bl.save_config();
                warp::reply::json(&serde_json::json!({"enabled": bl.parental_control}))
            }
        });

    let bl_for_sync = blocklist.clone();
    let sync = warp::path("sync")
        .and(warp::post())
        .map(move || {
            let bl_clone = bl_for_sync.clone();
            tokio::spawn(async move {
                let mut bl = bl_clone.lock().await;
                let _ = bl.sync_remote().await;
            });
            warp::reply::json(&serde_json::json!({"status": "sync_started"}))
        });

    let clients = warp::path("clients")
        .map(|| {
            let devices = network_scanner::scan();
            warp::reply::json(&devices)
        });

    let export = warp::path("export")
        .map(|| {
            let db_content = std::fs::read("pyholex_logs.db").unwrap_or_default();
            warp::reply::with_header(db_content, "Content-Disposition", "attachment; filename=\"pyholex_backup.db\"")
        });

    let routes = stats.or(set_profile).or(set_upstream).or(urls).or(logs).or(toggle_parental).or(sync).or(clients).or(export);
    println!("PyHoleX Management API active on 127.0.0.1:8080");
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}
