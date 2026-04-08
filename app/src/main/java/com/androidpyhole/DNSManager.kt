package com.androidpyhole

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

class DNSManager {
    private val client = OkHttpClient()
    private val apiBaseUrl = "http://127.0.0.1:8080"

    suspend fun getStats(): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/stats").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body?.string() ?: "{}")
        } } catch (e: Exception) { null }
    }

    suspend fun setProfile(profile: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/profile/$profile").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun setUpstream(upstream: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/upstream/$upstream").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun getLogs(): JSONArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/logs").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext JSONArray()
            JSONArray(response.body?.string() ?: "[]")
        } } catch (e: Exception) { JSONArray() }
    }

    suspend fun toggleParentalControl(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/parental").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun updateBlocklists(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/sync").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }
}
