package com.androidpyhole

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VPNServiceBridge : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val nativeEngine = NativeEngine()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setupVPN()
        executorService.submit { runVpnLoop() }
        return START_STICKY
    }

    private fun setupVPN() {
        val builder = Builder()
        builder.setSession("PyHoleX Global")
        builder.addAddress("10.0.0.2", 32)
        // Redirect DNS queries to our internal DNS server
        builder.addDnsServer("127.0.0.1")
        builder.addRoute("0.0.0.0", 0)
        builder.setBlocking(true)
        vpnInterface = builder.establish()
        Log.i("VPNService", "VPN interface established")
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
                    // Inspect packet headers (simplified)
                    val protocol = packet.get(9).toInt()
                    if (protocol == 17) { // UDP
                        // Potentially a DNS packet if destined for port 53
                        // nativeEngine.processDnsPacket(...) could be called here
                    }

                    packet.limit(length)
                    output.write(packet.array(), 0, length)
                    packet.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("VPNService", "Vpn loop error: ${e.message}")
        }
    }

    override fun onDestroy() {
        executorService.shutdownNow()
        vpnInterface?.close()
        super.onDestroy()
    }
}
