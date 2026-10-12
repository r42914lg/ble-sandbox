package com.r42914lg.blesandbox.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.r42914lg.blesandbox.coredata.AirNodeRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "CleanupTempWorker"

class CleanupTemperatureWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: AirNodeRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val deletedCount = repository.deleteTemperaturesOlderThan(cutoffMs = 3_600_000L) // 1 hour
            Log.d(TAG, "Successfully cleaned up $deletedCount old temperature records")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old temperature records", e)
            Result.retry()
        }
    }
}
