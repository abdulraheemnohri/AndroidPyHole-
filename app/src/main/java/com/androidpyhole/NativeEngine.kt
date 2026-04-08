package com.androidpyhole

class NativeEngine {
    external fun stringFromJNI(): String
    external fun processDnsPacket(packet: ByteArray): Boolean

    companion object {
        init {
            try {
                System.loadLibrary("pyhole_native")
            } catch (e: UnsatisfiedLinkError) {
                // Consider logging this to a crash reporter in a real app
                e.printStackTrace()
            }
        }
    }
}
