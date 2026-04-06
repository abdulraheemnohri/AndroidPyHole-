package com.pyhole

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class VPNServiceBridge : VpnService(), Runnable {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            setupVPN()
            isRunning = true
            vpnThread = Thread(this, "VPNThread")
            vpnThread?.start()
        }
        return START_STICKY
    }

    private fun setupVPN() {
        val builder = Builder()
        builder.setSession("PyHoleX")
        builder.addAddress("10.0.0.2", 32)
        // Redirect all DNS to local Rust engine port 5353
        builder.addDnsServer("127.0.0.1")
        builder.addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()
        Log.i("VPNService", "PyHoleX VPN established. DNS -> 127.0.0.1:5353")
    }

    override fun run() {
        // High-performance packet loop (blueprint)
        try {
            val input = FileInputStream(vpnInterface?.fileDescriptor)
            val output = FileOutputStream(vpnInterface?.fileDescriptor)
            val packet = ByteBuffer.allocate(32767)

            while (isRunning) {
                val length = input.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    output.write(packet.array(), 0, length)
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("VPNService", "VPN runtime error: ${e.message}")
        }
    }

    override fun onDestroy() {
        isRunning = false
        vpnThread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
