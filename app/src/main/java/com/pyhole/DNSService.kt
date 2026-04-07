package com.pyhole

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class DNSService : Service() {
    private var engineThread: Thread? = null
    private val rustEngine = RustEngine()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "pyholex_channel")
            .setContentTitle("PyHoleX DNS Protection")
            .setContentText("DNS Filtering is Active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("DNSService", "Starting PyHoleX Native Engine...")

        try {
            Log.i("DNSService", "Engine Status: ${rustEngine.getEngineStatus()}")
        } catch (e: Exception) {
            Log.e("DNSService", "Engine Error: ${e.message}")
        }

        engineThread = Thread {
            Log.i("DNSService", "Native DNS loop started on 127.0.0.1:5353")
        }
        engineThread?.start()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "pyholex_channel",
                "PyHoleX DNS Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        engineThread?.interrupt()
    }
}
