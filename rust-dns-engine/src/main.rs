use std::sync::Arc;
use tokio::sync::Mutex;
use pyhole_rust::blocklist::Blocklist;
use pyhole_rust::{logger, api, dns_server};

#[tokio::main]
async fn main() -> pyhole_rust::Result<()> {
    println!("Starting PyHoleX High-Performance Engine...");

    let blocklist = Arc::new(Mutex::new(Blocklist::new()));

    logger::init_db().map_err(|e| Box::new(e) as Box<dyn std::error::Error + Send + Sync>)?;

    let api_bl = blocklist.clone();
    let api_task = tokio::spawn(async move {
        api::run_server(api_bl).await;
    });

    let dns_bl = blocklist.clone();
    let dns_task = tokio::spawn(async move {
        if let Err(e) = dns_server::run(dns_bl).await {
            eprintln!("DNS Server error: {}", e);
        }
    });

    let _ = tokio::join!(api_task, dns_task);
    Ok(())
}
