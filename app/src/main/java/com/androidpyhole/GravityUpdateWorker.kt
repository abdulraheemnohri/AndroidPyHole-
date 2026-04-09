package com.androidpyhole

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class GravityUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dnsManager = DNSManager()
        return if (dnsManager.updateBlocklists()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
