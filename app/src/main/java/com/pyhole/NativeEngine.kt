package com.pyhole

class NativeEngine {
    external fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("pyhole_native")
        }
    }
}
