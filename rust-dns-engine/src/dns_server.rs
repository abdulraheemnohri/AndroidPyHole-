use tokio::net::UdpSocket;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;
use crate::logger;

pub async fn run(blocklist: Arc<Mutex<Blocklist>>) -> Result<(), Box<dyn std::error::Error>> {
    let socket = UdpSocket::bind("127.0.0.1:5353").await?;

    let mut buf = [0; 1024];
    loop {
        let (len, addr) = socket.recv_from(&mut buf).await?;
        let _packet = &buf[..len];

        let domain = "example.com";
        let client_ip = addr.ip().to_string();

        let (is_blocked, reason) = {
            let bl = blocklist.lock().unwrap();
            bl.is_blocked(domain, None) // Providing None for app_id in blueprint
        };

        if is_blocked {
            let _ = logger::log_query(domain, &client_ip, true);
            println!("Blocked {}: {:?}", domain, reason);
        } else {
            let _ = logger::log_query(domain, &client_ip, false);
        }
    }
}
