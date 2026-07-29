package com.example.monitoring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.DeviceRepository
import java.util.concurrent.TimeUnit

class DeviceMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = DeviceRepository(applicationContext)
            repository.refreshAllData()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_device_monitor_worker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<DeviceMonitorWorker>(
                15, TimeUnit.MINUTES // WorkManager minimum periodic interval is 15 minutes
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
