use std::net::{SocketAddr, Ipv4Addr};
use std::sync::Arc;
use std::time::{Instant, Duration};
use tokio::sync::Mutex;
use trust_dns_proto::op::{Message, MessageType, OpCode, ResponseCode};
use trust_dns_proto::rr::{ResourceRecord, RData};
use trust_dns_proto::serialize::binary::{BinDecoder, BinDecodable, BinEncoder, BinEncodable};
use tokio::net::UdpSocket;
use crate::blocklist::{Blocklist, PrivacyLevel};
use crate::logger;
use crate::Result;
use lru::LruCache;
use trust_dns_resolver::TokioAsyncResolver;
use trust_dns_resolver::config::{ResolverConfig, ResolverOpts};
use std::sync::atomic::{AtomicU64, Ordering};

pub struct PerformanceMetrics {
    pub cache_hits: AtomicU64,
    pub cache_misses: AtomicU64,
    pub total_latency_ms: AtomicU64,
    pub total_queries: AtomicU64,
    pub last_sec_queries: AtomicU64,
}

struct DnsState {
    cache: LruCache<String, Message>,
    resolver: TokioAsyncResolver,
}

static METRICS: PerformanceMetrics = PerformanceMetrics {
    cache_hits: AtomicU64::new(0),
    cache_misses: AtomicU64::new(0),
    total_latency_ms: AtomicU64::new(0),
    total_queries: AtomicU64::new(0),
    last_sec_queries: AtomicU64::new(0),
};

pub async fn run(blocklist: Arc<Mutex<Blocklist>>) -> Result<()> {
    let socket = UdpSocket::bind("127.0.0.1:5353").await?;
    let resolver = TokioAsyncResolver::tokio(ResolverConfig::cloudflare(), ResolverOpts::default())?;

    let state = Arc::new(Mutex::new(DnsState {
        cache: LruCache::new(5000),
        resolver,
    }));

    tokio::spawn(async move {
        loop {
            tokio::time::sleep(Duration::from_secs(1)).await;
            METRICS.last_sec_queries.store(0, Ordering::Relaxed);
        }
    });

    println!("PyHoleX DNS Engine listening on 127.0.0.1:5353");

    let mut buf = [0u8; 4096];
    loop {
        match socket.recv_from(&mut buf).await {
            Ok((len, addr)) => {
                let request_data = buf[..len].to_vec();
                let bl = blocklist.clone();
                let st = state.clone();
                let sock = Arc::new(socket.try_clone().unwrap_or(UdpSocket::bind("0.0.0.0:0").await?));

                tokio::spawn(async move {
                    let start_time = Instant::now();
                    METRICS.total_queries.fetch_add(1, Ordering::Relaxed);
                    METRICS.last_sec_queries.fetch_add(1, Ordering::Relaxed);

                    let mut decoder = BinDecoder::new(&request_data);
                    if let Ok(request) = Message::read(&mut decoder) {
                        let response = handle_dns_request(request, &bl, &st, &addr).await;
                        let mut response_buf = Vec::with_capacity(512);
                        let mut encoder = BinEncoder::new(&mut response_buf);
                        if response.emit(&mut encoder).is_ok() {
                            let _ = sock.send_to(&response_buf, addr).await;
                        }
                    }
                    let duration = start_time.elapsed().as_millis() as u64;
                    METRICS.total_latency_ms.fetch_add(duration, Ordering::Relaxed);
                });
            }
            Err(e) => {
                eprintln!("UDP Receive error: {}", e);
                tokio::time::sleep(Duration::from_millis(100)).await; // Prevent busy-wait on error
            }
        }
    }
}

pub fn get_performance_stats() -> serde_json::Value {
    let hits = METRICS.cache_hits.load(Ordering::Relaxed);
    let misses = METRICS.cache_misses.load(Ordering::Relaxed);
    let total = METRICS.total_queries.load(Ordering::Relaxed);
    let latency = METRICS.total_latency_ms.load(Ordering::Relaxed);
    let qps = METRICS.last_sec_queries.load(Ordering::Relaxed);
    let hit_rate = if hits + misses > 0 { (hits as f64 / (hits + misses) as f64) * 100.0 } else { 0.0 };
    let avg_latency = if total > 0 { latency as f64 / total as f64 } else { 0.0 };
    serde_json::json!({"cache_hits": hits, "cache_misses": misses, "cache_hit_rate": hit_rate, "avg_latency_ms": avg_latency, "qps": qps})
}

async fn handle_dns_request(request: Message, blocklist: &Arc<Mutex<Blocklist>>, state: &Arc<Mutex<DnsState>>, addr: &SocketAddr) -> Message {
    let mut response = Message::new();
    response.set_id(request.id());
    response.set_op_code(OpCode::Query);
    response.set_message_type(MessageType::Response);
    response.set_recursion_available(true);

    if let Some(query) = request.queries().first() {
        let domain = query.name().to_string();
        let client_ip = addr.ip().to_string();
        let (is_blocked, block_reason, upstream, privacy) = {
            let bl = blocklist.lock().await;
            let (blocked, reason) = bl.is_blocked(&domain);
            (blocked, reason, bl.upstream_dns.clone(), bl.privacy)
        };

        let category = if !is_blocked { "allowed" }
                       else if block_reason.as_ref().map(|s| s.contains("AI")).unwrap_or(false) { "malware" }
                       else if domain.contains("track") || domain.contains("telemetry") { "tracker" }
                       else { "ads" };

        if privacy != PrivacyLevel::Ghost {
            let log_domain = if privacy == PrivacyLevel::Anonymous { "HIDDEN.DOMAIN".to_string() } else { domain.clone() };
            let _ = logger::log_query_with_metadata(&log_domain, &client_ip, is_blocked, block_reason.clone(), category, "unknown");
        }

        let local_res = { let bl = blocklist.lock().await; bl.resolve_local(&domain) };
        if let Some(ip_str) = local_res {
            if let Ok(ip) = ip_str.parse::<Ipv4Addr>() {
                response.set_response_code(ResponseCode::NoError);
                let mut record = ResourceRecord::new();
                record.set_name(query.name().clone()); record.set_ttl(3600); record.set_data(Some(RData::A(ip)));
                response.add_answer(record); response.add_query(query.clone());
                return response;
            }
        }

        if is_blocked {
            response.set_response_code(ResponseCode::NXDomain);
            response.add_query(query.clone());
        } else {
            {
                let mut st = state.lock().await;
                if let Some(cached) = st.cache.get(&domain) {
                    METRICS.cache_hits.fetch_add(1, Ordering::Relaxed);
                    let mut res = cached.clone(); res.set_id(request.id()); return res;
                }
                METRICS.cache_misses.fetch_add(1, Ordering::Relaxed);
            }
            if upstream.starts_with("https://") {
                if let Ok(upstream_res) = forward_query_doh(&request, &upstream).await {
                     let mut st = state.lock().await; st.cache.put(domain, upstream_res.clone()); return upstream_res;
                }
            } else if let Ok(upstream_res) = forward_query_udp(&request, &upstream).await {
                let mut st = state.lock().await; st.cache.put(domain, upstream_res.clone()); return upstream_res;
            }
            response.set_response_code(ResponseCode::ServFail);
            response.add_query(query.clone());
        }
    }
    response
}

async fn forward_query_udp(request: &Message, upstream: &str) -> Result<Message> {
    let upstream_addr: SocketAddr = upstream.parse().unwrap_or("1.1.1.1:53".parse()?);
    let socket = UdpSocket::bind("0.0.0.0:0").await?;
    let mut buf = Vec::with_capacity(512);
    let mut encoder = BinEncoder::new(&mut buf);
    request.emit(&mut encoder)?;
    socket.send_to(&buf, upstream_addr).await?;
    let mut res_buf = [0u8; 4096];
    // Fast Timeout: 2s for Production stability
    let (len, _) = tokio::time::timeout(Duration::from_secs(2), socket.recv_from(&mut res_buf)).await??;
    let mut decoder = BinDecoder::new(&res_buf[..len]);
    Ok(Message::read(&mut decoder)?)
}

async fn forward_query_doh(request: &Message, doh_url: &str) -> Result<Message> {
    let mut buf = Vec::with_capacity(512);
    let mut encoder = BinEncoder::new(&mut buf);
    request.emit(&mut encoder)?;
    let client = reqwest::Client::builder().timeout(Duration::from_secs(3)).build()?;
    let resp = client.post(doh_url).header("content-type", "application/dns-message").header("accept", "application/dns-message").body(buf).send().await?;
    if resp.status().is_success() {
        let body = resp.bytes().await?;
        let mut decoder = BinDecoder::new(&body);
        return Ok(Message::read(&mut decoder)?);
    }
    Err("DoH request failed".into())
}
