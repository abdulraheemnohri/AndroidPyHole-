package com.androidpyhole
class NativeEngine {
    external fun stringFromJNI(): String
    external fun processDnsPacket(packet: ByteArray): Boolean
    companion object { init { System.loadLibrary("pyhole_native") } }
}
