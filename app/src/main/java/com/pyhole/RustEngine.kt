package com.pyhole

class RustEngine {
    external fun getEngineStatus(): String

    companion object {
        init {
            System.loadLibrary("pyhole_rust")
        }
    }
}
