package com.androidpyhole

class RustEngine {
    external fun getEngineStatus(): String

    companion object {
        init {
            try {
                System.loadLibrary("pyhole_rust")
            } catch (e: UnsatisfiedLinkError) {
                // Fallback or log if needed, though for now we want to know if it fails
                e.printStackTrace()
            }
        }
    }
}
