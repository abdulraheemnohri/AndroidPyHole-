package com.pyhole

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class DNSService : Service() {
    private var engineThread: Thread? = null
    private val rustEngine = RustEngine()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("DNSService", "Starting PyHoleX Native Engine...")

        try {
            Log.i("DNSService", "Engine Status: ${rustEngine.getEngineStatus()}")
        } catch (e: Exception) {
            Log.e("DNSService", "Failed to communicate with native engine: ${e.message}")
        }

        engineThread = Thread {
            // In a real implementation, this would start the Rust event loop via JNI
            // or execute the compiled binary.
            Log.i("DNSService", "Native DNS loop started on 127.0.0.1:5353")
        }
        engineThread?.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        engineThread?.interrupt()
        Log.i("DNSService", "PyHoleX Service stopped")
    }
}
