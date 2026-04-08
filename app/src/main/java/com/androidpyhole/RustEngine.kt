package com.androidpyhole

class RustEngine {
    external fun getEngineStatus(): String
    external fun startNativeEngine()

    companion object {
        init {
            try {
                System.loadLibrary("pyhole_rust")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }
}
