package com.pyhole

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class VPNServiceBridge : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
        builder.setSession("AndroidPyHole")
        builder.addAddress("10.0.0.2", 32)
        builder.addDnsServer("127.0.0.1")
        builder.addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()
        Log.i("VPNService", "VPN established")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnInterface?.close()
        Log.i("VPNService", "VPN destroyed")
    }
}
