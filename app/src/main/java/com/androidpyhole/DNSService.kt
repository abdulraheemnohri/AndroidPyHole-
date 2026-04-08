package com.androidpyhole

import android.app.Notification
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "pyholex_channel")
            .setContentTitle("PyHoleX Protection")
            .setContentText("High-performance DNS filtering is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("DNSService", "Initializing Rust DNS Engine...")
        try {
            rustEngine.startNativeEngine()
            Log.i("DNSService", "Engine status: ${rustEngine.getEngineStatus()}")
        } catch (e: Exception) {
            Log.e("DNSService", "Failed to start native engine: ${e.message}")
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "pyholex_channel",
                "PyHoleX DNS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
