package com.androidpyhole

class RustEngine {
    external fun getEngineStatus(): String

    companion object {
        init {
            System.loadLibrary("pyhole_rust")
        }
    }
}
