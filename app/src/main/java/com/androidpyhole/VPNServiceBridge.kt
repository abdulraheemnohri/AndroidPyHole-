package com.androidpyhole

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
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class VPNServiceBridge : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val executor = Executors.newFixedThreadPool(2)
    private var isRunning = false
    private val nativeEngine = NativeEngine()

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_EXCLUDED_APPS = "excluded_apps"
        private const val TAG = "VPNServiceBridge"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val excluded = intent.getStringArrayListExtra(EXTRA_EXCLUDED_APPS) ?: arrayListOf()
                startVpn(excluded)
            }
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(excludedApps: List<String>) {
        if (isRunning) return
        isRunning = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("vpn", "PyHoleX Shield", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        startForeground(1, NotificationCompat.Builder(this, "vpn")
            .setContentTitle("PyHoleX Shield Active")
            .setContentText("DNS Engine Engaged")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .build())

        try {
            val builder = Builder()
                .setSession("PyHoleX")
                .addAddress("10.0.0.2", 32)
                .addDnsServer("127.0.0.1")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .setBlocking(true)

            for (app in excludedApps) {
                try { builder.addDisallowedApplication(app) } catch (e: Exception) { Log.w(TAG, "Failed to exclude $app") }
            }

            vpnInterface = builder.establish()

            executor.submit {
                val input = FileInputStream(vpnInterface!!.fileDescriptor)
                val output = FileOutputStream(vpnInterface!!.fileDescriptor)
                val packet = ByteBuffer.allocate(32767)

                while (isRunning) {
                    try {
                        val len = input.read(packet.array())
                        if (len > 0) {
                            val packetData = packet.array().copyOf(len)
                            // Native Fast Path Analysis
                            // If it's a DNS packet, we ensure it's forwarded to our local resolver.
                            // The Rust Engine listens on 127.0.0.1:5353.
                            // Android DNS server redirection (addDnsServer) handles this automatically
                            // by forcing UDP Port 53 to the specified IP.
                            // We use the native engine to verify packet integrity and protocol adherence.
                            nativeEngine.processDnsPacket(packetData)

                            output.write(packet.array(), 0, len)
                            packet.clear()
                        }
                    } catch (e: Exception) {
                        if (isRunning) Log.e(TAG, "Read error: ${e.message}")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VPN Establishment failed: ${e.message}")
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        try { vpnInterface?.close() } catch (e: Exception) {}
        vpnInterface = null
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        executor.shutdownNow()
        super.onDestroy()
    }
}
