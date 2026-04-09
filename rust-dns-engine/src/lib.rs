pub mod classifier;
pub mod p2p;
pub mod dns_server;
pub mod blocklist;
pub mod logger;
pub mod api;
pub mod network_scanner;

pub type Result<T> = std::result::Result<T, Box<dyn std::error::Error + Send + Sync>>;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::sync::Arc;
use tokio::sync::Mutex;
use crate::blocklist::Blocklist;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::Duration;

static ENGINE_STARTED: AtomicBool = AtomicBool::new(false);

#[no_mangle]
pub extern "system" fn Java_com_androidpyhole_RustEngine_getEngineStatus(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = "PyHoleX Engine v5.2.1 - Core Active";
    env.new_string(output).expect("Couldn't create java string!").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_androidpyhole_RustEngine_startNativeEngine(
    mut env: JNIEnv,
    _class: JClass,
    storage_path: JString,
) {
    if ENGINE_STARTED.swap(true, Ordering::SeqCst) {
        println!("Engine already running.");
        return;
    }

    let storage_path: String = env.get_string(&storage_path).expect("Couldn't get java string!").into();
    let base_path = PathBuf::from(storage_path);

    std::thread::spawn(move || {
        let rt = tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .unwrap();

        rt.block_on(async {
            println!("Starting PyHoleX Engine with storage at: {:?}", base_path);
            let config_path = base_path.join("pyholex_config.json");
            let db_path = base_path.join("pyholex_logs.db");

            let blocklist = Arc::new(Mutex::new(Blocklist::new_with_path(config_path.clone())));
            if let Err(e) = logger::init_db_with_path(db_path) {
                eprintln!("Failed to initialize database: {}", e);
            }

            let api_bl = blocklist.clone();
            tokio::spawn(async move { api::run_server(api_bl).await; });

            let p2p_bl = blocklist.clone();
            tokio::spawn(async move {
                let mut bl = p2p_bl.lock().await;
                bl.p2p_node.start_mesh_sync().await;
            });

            // Monitoring & Alerts Task
            tokio::spawn(async move {
                loop {
                    tokio::time::sleep(Duration::from_secs(3600)).await; // Hourly check
                    if let Ok(stats) = logger::get_stats() {
                        if let Some(blocked) = stats.get("blocked_queries").and_then(|v| v.as_i64()) {
                            if blocked > 500 {
                                println!("ALERT: Unusual DNS blocking activity detected! (>500 blocks)");
                                // In a full implementation, we'd trigger a JNI callback to Android Notifications
                            }
                        }
                    }
                }
            });

            let maintenance_bl = blocklist.clone();
            tokio::spawn(async move {
                loop {
                    let retention = { let bl = maintenance_bl.lock().await; bl.retention };
                    let _ = logger::apply_retention(retention);
                    tokio::time::sleep(Duration::from_secs(86400)).await;
                }
            });

            let dns_bl = blocklist.clone();
            if let Err(e) = dns_server::run(dns_bl).await {
                eprintln!("DNS Server error: {}", e);
            }
        });
    });
}
