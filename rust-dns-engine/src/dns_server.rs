use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::Mutex;
use trust_dns_proto::op::{Message, MessageType, OpCode, ResponseCode};
use trust_dns_proto::serialize::binary::{BinDecoder, BinDecodable, BinEncoder, BinEncodable};
use tokio::net::UdpSocket;
use crate::blocklist::Blocklist;
use crate::logger;
use crate::Result;
use lru::LruCache;

struct DnsState {
    cache: LruCache<String, Message>,
}

pub async fn run(blocklist: Arc<Mutex<Blocklist>>) -> Result<()> {
    let socket = UdpSocket::bind("127.0.0.1:5353").await?;
    let state = Arc::new(Mutex::new(DnsState {
        cache: LruCache::new(1000),
    }));

    println!("DNS Engine listening on 127.0.0.1:5353");

    let mut buf = [0u8; 2048];
    loop {
        let (len, addr) = socket.recv_from(&mut buf).await?;
        let request_data = buf[..len].to_vec();

        let bl = blocklist.clone();
        let st = state.clone();

        let mut decoder = BinDecoder::new(&request_data);
        if let Ok(request) = Message::read(&mut decoder) {
            let response = handle_dns_request(request, &bl, &st, &addr).await;
            let mut response_buf = Vec::with_capacity(512);
            let mut encoder = BinEncoder::new(&mut response_buf);
            if response.emit(&mut encoder).is_ok() {
                let _ = socket.send_to(&response_buf, addr).await;
            }
        }
    }
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

        let (is_blocked, upstream) = {
            let bl = blocklist.lock().await;
            (bl.is_blocked(&domain).0, bl.upstream_dns.clone())
        };

        if is_blocked {
            let _ = logger::log_query(&domain, &client_ip, true);
            response.set_response_code(ResponseCode::NXDomain);
        } else {
            {
                let mut st = state.lock().await;
                if let Some(cached) = st.cache.get(&domain) {
                    let mut res = cached.clone();
                    res.set_id(request.id());
                    return res;
                }
            }

            let _ = logger::log_query(&domain, &client_ip, false);

            if let Ok(upstream_res) = forward_query(&request, &upstream).await {
                let mut st = state.lock().await;
                st.cache.put(domain, upstream_res.clone());
                return upstream_res;
            }

            response.set_response_code(ResponseCode::NoError);
        }
        response.add_query(query.clone());
    }

    response
}

async fn forward_query(request: &Message, upstream: &str) -> Result<Message> {
    let upstream_addr: SocketAddr = upstream.parse()?;
    let socket = UdpSocket::bind("0.0.0.0:0").await?;

    let mut buf = Vec::with_capacity(512);
    let mut encoder = BinEncoder::new(&mut buf);
    request.emit(&mut encoder)?;

    socket.send_to(&buf, upstream_addr).await?;

    let mut res_buf = [0u8; 2048];
    let (len, _) = tokio::time::timeout(std::time::Duration::from_secs(2), socket.recv_from(&mut res_buf)).await??;

    let mut decoder = BinDecoder::new(&res_buf[..len]);
    Ok(Message::read(&mut decoder)?)
}
