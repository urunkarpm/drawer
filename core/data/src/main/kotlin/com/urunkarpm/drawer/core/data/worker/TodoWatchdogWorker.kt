package com.urunkarpm.drawer.core.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.urunkarpm.drawer.core.data.reconcile.TodoReconciler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TodoWorkerEntryPoint {
    fun todoReconciler(): TodoReconciler
}

/**
 * 15-minute WorkManager watchdog worker that ensures the todo state, deadline alarms,
 * and geofence/location checks stay reconciled even if alarms were delayed or killed.
 */
class TodoWatchdogWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                TodoWorkerEntryPoint::class.java
            )
            entryPoint.todoReconciler().reconcile()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "TodoWatchdogWorker"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<TodoWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
