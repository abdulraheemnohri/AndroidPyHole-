package com.androidpyhole

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class DNSService : Service() {
    private val rustEngine = RustEngine()
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("pyholex_dns", "PyHoleX DNS", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, "pyholex_dns")
            .setContentTitle("PyHoleX DNS")
            .setContentText("DNS Engine is filtering traffic")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(2, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        rustEngine.startNativeEngine()
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
