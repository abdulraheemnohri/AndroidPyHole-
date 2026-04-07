use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_com_androidpyhole_RustEngine_getEngineStatus(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = "Rust Engine V5.2.0 - Active";
    env.new_string(output).expect("Couldn't create java string!").into_raw()
}
