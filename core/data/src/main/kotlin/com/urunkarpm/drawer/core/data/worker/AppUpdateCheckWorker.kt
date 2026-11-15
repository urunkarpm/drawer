package com.urunkarpm.drawer.core.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.urunkarpm.drawer.core.data.repository.UpdateRepository
import com.urunkarpm.drawer.core.data.update.UpdateNotificationHelper
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UpdateWorkerEntryPoint {
    fun updateRepository(): UpdateRepository
    fun preferencesDataSource(): DrawerPreferencesDataSource
    fun updateNotificationHelper(): UpdateNotificationHelper
}

/**
 * WorkManager periodic worker that checks for new Drawer releases from GitHub
 * and posts a system notification when an update is available.
 */
class AppUpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // ponytail: WorkManager background 12h periodic update check with network constraint; ceiling: GitHub public API rate limit (60/hr); upgrade path: dedicated update feed or push notification.
    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                UpdateWorkerEntryPoint::class.java
            )
            val preferences = entryPoint.preferencesDataSource()
            val autoCheck = preferences.autoCheckUpdates.first()
            if (!autoCheck) return Result.success()

            val currentVersion = runCatching {
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName ?: "0.0.0"
            }.getOrDefault("0.0.0")

            val repo = entryPoint.updateRepository()
            val updateResult = repo.checkForUpdates(currentVersion)
            updateResult.onSuccess { info ->
                preferences.setLastUpdateCheckTime(System.currentTimeMillis())
                if (info.isUpdateAvailable) {
                    val dismissed = preferences.dismissedUpdateVersion.first()
                    if (info.latestVersionTag != dismissed) {
                        entryPoint.updateNotificationHelper().showUpdateNotification(info)
                    }
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "AppUpdateCheckWorker"

        fun enqueuePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(
                    12, TimeUnit.HOURS
                ).setConstraints(constraints).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (_: Throwable) {}
        }
    }
}
