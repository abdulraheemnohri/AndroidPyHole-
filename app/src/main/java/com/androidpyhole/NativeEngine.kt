package com.androidpyhole

class NativeEngine {
    external fun stringFromJNI(): String

    companion object {
        init {
            try {
                System.loadLibrary("pyhole_native")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }
}
