#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_pyhole_NativeEngine_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Native DNS Engine Active";
    return env->NewStringUTF(hello.c_str());
}
