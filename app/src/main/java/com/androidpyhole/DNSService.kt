package com.androidpyhole

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class DNSService : Service() {
    private val rustEngine = RustEngine()
    private var isEngineStarted = false

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("pyholex_dns", "PyHoleX DNS", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, "pyholex_dns")
            .setContentTitle("PyHoleX DNS Engine")
            .setContentText("DNS Engine is filtering traffic")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isEngineStarted) {
            try {
                rustEngine.startNativeEngine()
                isEngineStarted = true
                Log.i("DNSService", "Native Rust Engine started successfully")
            } catch (e: Exception) {
                Log.e("DNSService", "Failed to start Native Rust Engine: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Rust engine currently runs in a spawned thread and doesn't have a clean stop JNI call in this MVP
        super.onDestroy()
    }
}