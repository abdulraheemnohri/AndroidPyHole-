#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "NativeEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_androidpyhole_NativeEngine_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "PyHoleX Native Core Active";
    LOGI("Native core initialized successfully");
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_androidpyhole_NativeEngine_processDnsPacket(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray packet) {
    // Placeholder for native DNS packet processing
    // In a full implementation, this would parse the DNS header and query
    LOGI("Processing DNS packet in native layer...");
    return JNI_TRUE;
}
