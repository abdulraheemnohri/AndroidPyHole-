package com.pyhole

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var startStopBtn: Button
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        startStopBtn = findViewById(R.id.startStopBtn)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("http://127.0.0.1:8080")

        startStopBtn.setOnClickListener {
            if (isServiceRunning) {
                stopDNSService()
            } else {
                startDNSService()
            }
        }
    }

    private fun startDNSService() {
        val intent = Intent(this, DNSService::class.java)
        startService(intent)
        isServiceRunning = true
        startStopBtn.text = "Stop Service"
        Toast.makeText(this, "DNS Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopDNSService() {
        val intent = Intent(this, DNSService::class.java)
        stopService(intent)
        isServiceRunning = false
        startStopBtn.text = "Start Service"
        Toast.makeText(this, "DNS Service Stopped", Toast.LENGTH_SHORT).show()
    }
}
