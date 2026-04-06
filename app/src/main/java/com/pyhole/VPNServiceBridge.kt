package com.pyhole

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import android.content.pm.PackageManager

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
        builder.addDnsServer("127.0.0.1")
        builder.addRoute("0.0.0.0", 0)

        // Example: Exclude PyHoleX itself from the VPN to avoid loops
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("VPN", "Package not found", e)
        }

        vpnInterface = builder.establish()
    }

    override fun run() {
        try {
            val input = FileInputStream(vpnInterface?.fileDescriptor)
            val output = FileOutputStream(vpnInterface?.fileDescriptor)
            val packet = ByteBuffer.allocate(32767)

            while (isRunning) {
                val length = input.read(packet.array())
                if (length > 0) {
                    // Logic to identify source app (simplified blueprint)
                    // On Android, we can track UIDs if we parse the IP headers manually
                    // For the blueprint, we assume app identification is handled via Netlink or UID tracking
                    packet.limit(length)
                    output.write(packet.array(), 0, length)
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("VPNService", "Error: ${e.message}")
        }
    }

    override fun onDestroy() {
        isRunning = false
        vpnThread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }
}
