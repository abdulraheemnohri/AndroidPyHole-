package com.androidpyhole

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("PyHoleX-Boot", "Device boot completed. Restarting Shield services...")
            // In a production app, we would check if the service was running before reboot
            context.startService(Intent(context, VPNServiceBridge::class.java).setAction(VPNServiceBridge.ACTION_START))
            context.startService(Intent(context, DNSService::class.java))
        }
    }
}
