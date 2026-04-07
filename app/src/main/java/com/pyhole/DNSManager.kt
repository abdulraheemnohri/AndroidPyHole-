package com.pyhole

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log

class DNSManager {
    private val client = OkHttpClient()
    private val apiBaseUrl = "http://127.0.0.1:8080"

    suspend fun getStats(): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$apiBaseUrl/stats")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                return@withContext JSONObject(response.body?.string() ?: "{}")
            }
        } catch (e: Exception) {
            Log.e("DNSManager", "Failed to fetch stats", e)
            null
        }
    }

    suspend fun toggleBlock(domain: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$apiBaseUrl/block/$domain")
            .post(okhttp3.RequestBody.create(null, ""))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
