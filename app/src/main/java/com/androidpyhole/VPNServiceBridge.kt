package com.androidpyhole

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VPNServiceBridge : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        const val ACTION_START = "com.androidpyhole.START"
        const val ACTION_STOP = "com.androidpyhole.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vpn_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification("PyHoleX Global Shield Active"))
                setupVPN()
                executorService.submit { runVpnLoop() }
            }
            ACTION_STOP -> {
                stopVPN()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupVPN() {
        val builder = Builder()
        builder.setSession("PyHoleX Global Shield")
        builder.addAddress("10.0.0.2", 32)
        // Redirect DNS queries to local engine
        builder.addDnsServer("127.0.0.1")
        builder.addRoute("0.0.0.0", 0)
        builder.setMtu(1500)
        builder.setBlocking(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        vpnInterface = builder.establish()
        Log.i("VPNService", "VPN Tunnel Established")
    }

    private fun runVpnLoop() {
        try {
            val fd = vpnInterface?.fileDescriptor ?: return
            val input = FileInputStream(fd)
            val output = FileOutputStream(fd)
            val packet = ByteBuffer.allocate(32767)

            while (!Thread.interrupted()) {
                val length = input.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    // Packet processing would happen here for deep packet inspection
                    output.write(packet.array(), 0, length)
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("VPNService", "Loop error: ${e.message}")
        }
    }

    private fun stopVPN() {
        executorService.shutdownNow()
        vpnInterface?.close()
        vpnInterface = null
    }

    private fun createNotification(content: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "VPN Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PyHoleX")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopVPN()
        super.onDestroy()
    }
}
