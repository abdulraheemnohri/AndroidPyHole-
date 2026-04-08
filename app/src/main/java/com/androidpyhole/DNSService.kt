package com.androidpyhole

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
    private val nativeEngine = NativeEngine()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "pyholex_channel")
            .setContentTitle("PyHoleX DNS Protection")
            .setContentText("Native Engine: ${nativeEngine.stringFromJNI()}")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("DNSService", "Starting PyHoleX Native Service...")

        engineThread = Thread {
            try {
                Log.i("DNSService", "Native DNS loop active on 127.0.0.1:5353")
                while (!Thread.currentThread().isInterrupted) {
                    // Logic to listen on UDP port 5353 and call nativeEngine.processDnsPacket()
                    Thread.sleep(10000)
                }
            } catch (e: InterruptedException) {
                Log.i("DNSService", "Engine thread interrupted")
            }
        }
        engineThread?.start()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "pyholex_channel",
                "PyHoleX DNS Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engineThread?.interrupt()
        super.onDestroy()
    }
}
