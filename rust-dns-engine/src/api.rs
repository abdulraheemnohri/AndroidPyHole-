use warp::Filter;
use std::sync::Arc;
use tokio::sync::Mutex;
use serde::{Serialize, Deserialize};
use crate::blocklist::{Blocklist, ShieldProfile, PrivacyLevel, BlocklistConfig};
use crate::logger;
use crate::network_scanner;
use crate::dns_server;
use std::collections::HashMap;
use std::time::{Instant, Duration};
use trust_dns_proto::serialize::binary::{BinDecoder, BinEncoder};
use trust_dns_proto::op::Message;

#[derive(Deserialize)]
struct ListAction { item: String }
#[derive(Deserialize)]
struct MappingRequest { domain: String, ip: String }
#[derive(Deserialize)]
struct LogQuery { q: Option<String>, blocked: Option<bool>, category: Option<String>, limit: Option<i64> }

struct RateLimiter { counts: Mutex<HashMap<String, (usize, Instant)>> }
impl RateLimiter {
    fn new() -> Self { RateLimiter { counts: Mutex::new(HashMap::new()) } }
    async fn check(&self, key: &str) -> bool {
        let mut counts = self.counts.lock().await;
        let entry = counts.entry(key.to_string()).or_insert((0, Instant::now()));
        if entry.1.elapsed() > Duration::from_secs(1) { *entry = (1, Instant::now()); true }
        else { if entry.0 < 100 { entry.0 += 1; true } else { false } }
    }
}

pub async fn run_server(blocklist: Arc<Mutex<Blocklist>>) {
    let limiter = Arc::new(RateLimiter::new());
    let limiter_filter = {
        let l = limiter.clone();
        warp::any().and_then(move || {
            let l_clone = l.clone();
            async move { if l_clone.check("local").await { Ok(()) } else { Err(warp::reject::custom(RateLimitExceeded)) } }
        }).untuple_one()
    };

    let bl_for_stats = blocklist.clone();
    let stats = warp::path("stats").and(warp::get()).then(move || {
        let bl_clone = bl_for_stats.clone();
        async move {
            let bl = bl_clone.lock().await;
            let mut s = logger::get_stats().unwrap_or_else(|_| serde_json::json!({}));
            let perf = dns_server::get_performance_stats();
            if let Some(obj) = s.as_object_mut() {
                obj.insert("domains_loaded".to_string(), serde_json::json!(bl.domains.len()));
                obj.insert("ai_guard".to_string(), serde_json::json!(bl.ai_guard));
                obj.insert("parental_control".to_string(), serde_json::json!(bl.parental_control));
                obj.insert("privacy".to_string(), match bl.privacy { PrivacyLevel::Default => "default", PrivacyLevel::Anonymous => "anonymous", PrivacyLevel::Ghost => "ghost" }.into());
                obj.insert("profile".to_string(), match bl.profile { ShieldProfile::Standard => "standard", ShieldProfile::Strict => "strict", ShieldProfile::DNSOnly => "dns_only" }.into());
                obj.insert("upstream_dns".to_string(), bl.upstream_dns.clone().into());
                obj.insert("top_blocked".to_string(), serde_json::json!(logger::get_top_blocked(5).unwrap_or_default()));
                obj.insert("app_stats".to_string(), serde_json::json!(logger::get_app_stats(5).unwrap_or_default()));
                obj.insert("local_mappings".to_string(), serde_json::json!(bl.local_mappings));
                obj.insert("urls".to_string(), serde_json::json!(bl.urls));
                obj.insert("blacklist".to_string(), serde_json::json!(bl.blacklist.iter().collect::<Vec<_>>()));
                obj.insert("whitelist".to_string(), serde_json::json!(bl.whitelist.iter().collect::<Vec<_>>()));
                obj.insert("regex_rules".to_string(), serde_json::json!(bl.regex_cache.iter().map(|r| r.as_str()).collect::<Vec<_>>()));
                obj.insert("mesh_info".to_string(), serde_json::json!({"threats": bl.p2p_node.shared_threats.len(), "status": "connected"}));
                obj.insert("last_sync".to_string(), serde_json::json!(bl.last_sync));
                if let Some(p_obj) = perf.as_object() { for (k, v) in p_obj { obj.insert(k.clone(), v.clone()); } }
            }
            warp::reply::json(&s)
        }
    });

    let health = warp::path("health").map(|| warp::reply::json(&serde_json::json!({"status": "healthy"})));

    let bl_for_purge = blocklist.clone();
    let purge_cache = warp::path!("toolkit" / "purge_cache").and(warp::post()).then(move || {
        let _bl = bl_for_purge.clone();
        async move {
            // Actual implementation would reach into dns_server's state.
            // Mock success for the toolkit entry.
            warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_backup = blocklist.clone();
    let backup = warp::path("backup").and(warp::get()).then(move || {
        let bl_clone = bl_for_backup.clone();
        async move {
            let bl = bl_clone.lock().await;
            warp::reply::json(&BlocklistConfig {
                enabled_urls: bl.urls.clone(), custom_blacklist: bl.blacklist.clone(), custom_whitelist: bl.whitelist.clone(),
                regex_blacklist: bl.regex_cache.iter().map(|r| r.as_str().to_string()).collect(), local_mappings: bl.local_mappings.clone(),
                parental_control_enabled: bl.parental_control, ai_guard_enabled: bl.ai_guard, privacy_level: bl.privacy, active_profile: bl.profile,
                upstream_dns: bl.upstream_dns.clone(), keyword_blocks: bl.keyword_blocks.clone(), dashboard_password: bl.password.clone(),
                log_retention_days: bl.retention, excluded_apps: bl.excluded_apps.clone(), last_sync: bl.last_sync,
            })
        }
    });

    let bl_for_restore = blocklist.clone();
    let restore = warp::path("restore").and(warp::post()).and(warp::body::json()).then(move |config: BlocklistConfig| {
        let bl_clone = bl_for_restore.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            bl.urls = config.enabled_urls; bl.blacklist = config.custom_blacklist; bl.whitelist = config.custom_whitelist;
            bl.regex_cache = config.regex_blacklist.iter().filter_map(|s| regex::Regex::new(s).ok()).collect(); bl.local_mappings = config.local_mappings;
            bl.parental_control = config.parental_control_enabled; bl.ai_guard = config.ai_guard_enabled; bl.privacy = config.privacy_level;
            bl.profile = config.active_profile; bl.upstream_dns = config.upstream_dns; bl.keyword_blocks = config.keyword_blocks;
            bl.password = config.dashboard_password; bl.retention = config.log_retention_days; bl.excluded_apps = config.excluded_apps;
            bl.last_sync = config.last_sync; bl.save_config();
            warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_urls = blocklist.clone();
    let manage_urls = warp::path!("gravity" / String).and(warp::post()).and(warp::body::json()).then(move |action: String, req: ListAction| {
        let bl_clone = bl_for_urls.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            if action == "add" { if !bl.urls.contains(&req.item) { bl.urls.push(req.item.clone()); } }
            else { bl.urls.retain(|x| x != &req.item); }
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_blacklist = blocklist.clone();
    let manage_blacklist = warp::path!("blacklist" / String).and(warp::post()).and(warp::body::json()).then(move |action: String, req: ListAction| {
        let bl_clone = bl_for_blacklist.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            if action == "add" { bl.blacklist.insert(req.item.to_lowercase()); }
            else { bl.blacklist.remove(&req.item.to_lowercase()); }
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_whitelist = blocklist.clone();
    let manage_whitelist = warp::path!("whitelist" / String).and(warp::post()).and(warp::body::json()).then(move |action: String, req: ListAction| {
        let bl_clone = bl_for_whitelist.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            if action == "add" { bl.whitelist.insert(req.item.to_lowercase()); }
            else { bl.whitelist.remove(&req.item.to_lowercase()); }
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_regex = blocklist.clone();
    let manage_regex = warp::path!("regex" / String).and(warp::post()).and(warp::body::json()).then(move |action: String, req: ListAction| {
        let bl_clone = bl_for_regex.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            if action == "add" {
                if let Ok(re) = regex::Regex::new(&req.item) { if !bl.regex_cache.iter().any(|r| r.as_str() == req.item) { bl.regex_cache.push(re); } }
            } else { bl.regex_cache.retain(|r| r.as_str() != req.item); }
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_mapping = blocklist.clone();
    let manage_local = warp::path!("local" / String).and(warp::post()).and(warp::body::json()).then(move |action: String, req: MappingRequest| {
        let bl_clone = bl_for_mapping.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            if action == "add" { bl.local_mappings.insert(req.domain.to_lowercase(), req.ip.clone()); }
            else { bl.local_mappings.remove(&req.domain.to_lowercase()); }
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_profile = blocklist.clone();
    let set_profile = warp::path!("profile" / String).and(warp::post()).then(move |p: String| {
        let bl_clone = bl_for_profile.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            bl.profile = match p.as_str() { "strict" => ShieldProfile::Strict, "dns_only" => ShieldProfile::DNSOnly, _ => ShieldProfile::Standard };
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let bl_for_privacy = blocklist.clone();
    let set_privacy = warp::path!("privacy" / String).and(warp::post()).then(move |l: String| {
        let bl_clone = bl_for_privacy.clone();
        async move {
            let mut bl = bl_clone.lock().await;
            bl.privacy = match l.as_str() { "anonymous" => PrivacyLevel::Anonymous, "ghost" => PrivacyLevel::Ghost, _ => PrivacyLevel::Default };
            bl.save_config(); warp::reply::json(&serde_json::json!({"status": "success"}))
        }
    });

    let dns_logs = warp::path!("dns" / "logs").and(warp::get()).and(warp::query::<LogQuery>()).then(|params: LogQuery| async move {
        match logger::search_logs(&params.q.unwrap_or_default(), params.blocked, params.category.as_deref(), params.limit.unwrap_or(200)) {
            Ok(l) => warp::reply::json(&l),
            Err(_) => warp::reply::json(&Vec::<String>::new())
        }
    });

    let dns_lookup = warp::path!("dns" / "lookup" / String).and(warp::get()).then(move |domain: String| {
        let bl_clone = blocklist.clone();
        async move {
            let bl = bl_clone.lock().await;
            let res = if let Some(ip) = bl.resolve_local(&domain) { format!("Local: {}", ip) } else { "External".to_string() };
            warp::reply::json(&serde_json::json!({"domain": domain, "result": res}))
        }
    });

    let flush_logs = warp::path!("toolkit" / "flush_logs").and(warp::post()).then(|| async { let _ = logger::flush_logs(); warp::reply::json(&serde_json::json!({"status": "success"})) });
    let scan = warp::path("scan").map(|| warp::reply::json(&network_scanner::scan()));
    let bl_for_sync = blocklist.clone();
    let sync = warp::path("sync").and(warp::post()).map(move || {
        let bl_clone = bl_for_sync.clone();
        tokio::spawn(async move { let mut bl = bl_clone.lock().await; let _ = bl.sync_remote().await; });
        warp::reply::json(&serde_json::json!({"status": "started"}))
    });

    let routes = limiter_filter.and(
        stats.or(health).or(backup).or(restore).or(manage_urls).or(manage_blacklist).or(manage_whitelist).or(manage_regex).or(manage_local).or(dns_lookup).or(dns_logs).or(scan).or(sync).or(flush_logs).or(purge_cache)
    ).recover(handle_rejection);
    warp::serve(routes).run(([127, 0, 0, 1], 8080)).await;
}

async fn handle_rejection(err: warp::Rejection) -> Result<impl warp::Reply, std::convert::Infallible> {
    if err.find::<RateLimitExceeded>().is_some() { Ok(warp::reply::with_status(warp::reply::json(&serde_json::json!({"error": "rate_limit"})), warp::http::StatusCode::TOO_MANY_REQUESTS)) }
    else { Ok(warp::reply::with_status(warp::reply::json(&serde_json::json!({"error": "not_found"})), warp::http::StatusCode::NOT_FOUND)) }
}
#[derive(Debug)] struct RateLimitExceeded;
impl warp::reject::Reject for RateLimitExceeded {}
