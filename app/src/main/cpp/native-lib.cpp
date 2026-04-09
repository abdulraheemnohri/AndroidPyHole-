#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <unordered_set>

#define LOG_TAG "PyHoleX-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_androidpyhole_NativeEngine_stringFromJNI(JNIEnv* env, jobject) {
    return env->NewStringUTF("PyHoleX Native Stack v5.2.1 - Core Optimized");
}

/**
 * processDnsPacket - Native layer traffic inspection
 * Optimized fast-path for packet filtering.
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_androidpyhole_NativeEngine_processDnsPacket(JNIEnv* env, jobject, jbyteArray packet) {
    jsize len = env->GetArrayLength(packet);
    if (len < 28) return JNI_FALSE;

    jbyte* buffer = env->GetByteArrayElements(packet, nullptr);
    if (!buffer) return JNI_FALSE;

    // Fast-path header check (Protocol 17 = UDP)
    bool is_valid = (uint8_t)buffer[9] == 17;

    if (is_valid) {
        // DNS destination port check (Port 53)
        uint16_t dst_port = ((uint8_t)buffer[22] << 8) | (uint8_t)buffer[23];
        is_valid = (dst_port == 53);
    }

    env->ReleaseByteArrayElements(packet, buffer, JNI_ABORT);
    return is_valid ? JNI_TRUE : JNI_FALSE;
}
