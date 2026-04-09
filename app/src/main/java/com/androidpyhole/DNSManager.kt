package com.androidpyhole

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class DNSManager {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
    private val apiBaseUrl = "http://127.0.0.1:8080"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun getStats(): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/stats").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body?.string() ?: "{}")
        } } catch (e: Exception) { null }
    }

    suspend fun lookup(domain: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/dns/lookup/$domain").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext "Lookup failed"
            val json = JSONObject(response.body?.string() ?: "{}")
            json.optString("result", "Not found")
        } } catch (e: Exception) { "Error: ${e.message}" }
    }

    suspend fun searchLogs(query: String = "", blocked: Boolean? = null, category: String? = null, limit: Int = 100): JSONArray = withContext(Dispatchers.IO) {
        val urlBuilder = "$apiBaseUrl/dns/logs".toHttpUrl().newBuilder()
        if (query.isNotEmpty()) urlBuilder.addQueryParameter("q", query)
        if (blocked != null) urlBuilder.addQueryParameter("blocked", blocked.toString())
        if (category != null) urlBuilder.addQueryParameter("category", category)
        urlBuilder.addQueryParameter("limit", limit.toString())
        val request = Request.Builder().url(urlBuilder.build()).build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext JSONArray()
            JSONArray(response.body?.string() ?: "[]")
        } } catch (e: Exception) { JSONArray() }
    }

    suspend fun manageList(listType: String, action: String, item: String): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply { put("item", item) }
        val request = Request.Builder().url("$apiBaseUrl/$listType/$action").post(json.toString().toRequestBody(JSON)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun manageLocal(action: String, domain: String, ip: String): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply { put("domain", domain); put("ip", ip) }
        val request = Request.Builder().url("$apiBaseUrl/local/$action").post(json.toString().toRequestBody(JSON)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun getLocalMappings(): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/local").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext JSONObject()
            JSONObject(response.body?.string() ?: "{}")
        } } catch (e: Exception) { JSONObject() }
    }

    suspend fun setProfile(profile: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/profile/$profile").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun setPrivacyLevel(level: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/privacy/$level").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun updateBlocklists(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/sync").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun purgeCache(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/toolkit/purge_cache").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun getBackup(): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/backup").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            response.body?.string()
        } } catch (e: Exception) { null }
    }

    suspend fun restoreBackup(json: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/restore").post(json.toRequestBody(JSON)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun checkHealth(): JSONObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/health").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body?.string() ?: "{}")
        } } catch (e: Exception) { null }
    }

    suspend fun flushLogs(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/toolkit/flush_logs").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun setUpstream(upstream: String): Boolean = withContext(Dispatchers.IO) {
        val safeUpstream = upstream.replace(":", "-").replace("/", "_")
        val request = Request.Builder().url("$apiBaseUrl/upstream/$safeUpstream").post("".toRequestBody(null)).build()
        try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    suspend fun scanNetwork(): JSONArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$apiBaseUrl/scan").build()
        try { client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext JSONArray()
            JSONArray(response.body?.string() ?: "[]")
        } } catch (e: Exception) { JSONArray() }
    }
}
