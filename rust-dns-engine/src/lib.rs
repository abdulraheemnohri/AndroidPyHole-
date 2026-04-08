pub mod classifier;
pub mod p2p;
pub mod dns_server;
pub mod blocklist;
pub mod logger;
pub mod api;
pub mod network_scanner;

pub type Result<T> = std::result::Result<T, Box<dyn std::error::Error + Send + Sync>>;

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jstring;
use std::sync::Arc;
use tokio::sync::Mutex;
use crate::blocklist::Blocklist;

#[no_mangle]
pub extern "system" fn Java_com_androidpyhole_RustEngine_getEngineStatus(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = "PyHoleX Engine v5.2.0 - Core Active";
    env.new_string(output).expect("Couldn't create java string!").into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_androidpyhole_RustEngine_startNativeEngine(
    _env: JNIEnv,
    _class: JClass,
) {
    std::thread::spawn(|| {
        let rt = tokio::runtime::Runtime::new().unwrap();
        rt.block_on(async {
            println!("Starting PyHoleX High-Performance Engine via JNI...");
            let blocklist = Arc::new(Mutex::new(Blocklist::new()));
            let _ = logger::init_db();

            let api_bl = blocklist.clone();
            tokio::spawn(async move {
                api::run_server(api_bl).await;
            });

            let dns_bl = blocklist.clone();
            if let Err(e) = dns_server::run(dns_bl).await {
                eprintln!("DNS Server error: {}", e);
            }
        });
    });
}
