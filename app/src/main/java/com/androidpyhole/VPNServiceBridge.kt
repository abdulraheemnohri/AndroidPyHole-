package com.androidpyhole

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class VPNServiceBridge : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val executor = Executors.newSingleThreadExecutor()
    companion object { const val ACTION_START = "START" }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("vpn", "VPN", NotificationManager.IMPORTANCE_LOW)
                getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
            val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            startForeground(1, NotificationCompat.Builder(this, "vpn").setContentTitle("PyHoleX Shield").setSmallIcon(android.R.drawable.ic_lock_lock).setContentIntent(pi).build())
            vpnInterface = Builder().setSession("PyHoleX").addAddress("10.0.0.2", 32).addDnsServer("127.0.0.1").addRoute("0.0.0.0", 0).establish()
            executor.submit {
                val input = FileInputStream(vpnInterface!!.fileDescriptor)
                val output = FileOutputStream(vpnInterface!!.fileDescriptor)
                val packet = ByteBuffer.allocate(32767)
                while (!Thread.interrupted()) {
                    val len = input.read(packet.array())
                    if (len > 0) { output.write(packet.array(), 0, len); packet.clear() }
                }
            }
        }
        return START_STICKY
    }
    override fun onDestroy() { executor.shutdownNow(); vpnInterface?.close(); super.onDestroy() }
}
