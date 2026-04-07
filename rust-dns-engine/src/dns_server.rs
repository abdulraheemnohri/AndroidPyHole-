use tokio::net::UdpSocket;
use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;
use crate::logger;

pub async fn run(blocklist: Arc<Mutex<Blocklist>>) -> Result<(), Box<dyn std::error::Error>> {
    let socket = UdpSocket::bind("127.0.0.1:5353").await?;
    println!("DNS Engine listening on 127.0.0.1:5353");

    let mut buf = [0; 1024];
    loop {
        match socket.recv_from(&mut buf).await {
            Ok((len, addr)) => {
                let _packet = &buf[..len];
                let domain = "example.com";
                let client_ip = addr.ip().to_string();

                let (is_blocked, _reason) = {
                    let bl = blocklist.lock().unwrap();
                    bl.is_blocked(domain, None)
                };

                if is_blocked {
                    let _ = logger::log_query(domain, &client_ip, true);
                } else {
                    let _ = logger::log_query(domain, &client_ip, false);
                }
            }
            Err(e) => eprintln!("UDP recv error: {}", e),
        }
    }
}
