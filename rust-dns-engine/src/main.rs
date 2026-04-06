mod dns_server;
mod blocklist;
mod logger;
mod api;

use std::sync::{Arc, Mutex};
use crate::blocklist::Blocklist;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Starting PyHoleX High-Performance Engine...");

    let blocklist = Arc::new(Mutex::new(Blocklist::new()));

    logger::init_db()?;

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

    api_task.await?;
    dns_task.await?;
    Ok(())
}
