package com.androidpyhole
class RustEngine {
    external fun getEngineStatus(): String
    external fun startNativeEngine(storagePath: String)
    companion object { init { System.loadLibrary("pyhole_rust") } }
}
