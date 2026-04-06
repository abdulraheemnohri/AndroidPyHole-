package com.pyhole

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class DNSService : Service() {
    private var pythonThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        pythonThread = Thread {
            val py = Python.getInstance()
            val dnsServer = py.getModule("dns_server")
            val dashboard = py.getModule("dashboard")

            // Run Flask in background
            Thread {
                dashboard.callAttr("main")
            }.start()

            // Start DNS server
            dnsServer.callAttr("main")
        }
        pythonThread?.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        pythonThread?.interrupt()
    }
}
