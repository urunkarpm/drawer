package com.urunkarpm.drawer

import android.app.Application
import com.urunkarpm.drawer.core.data.worker.TodoWatchdogWorker
import com.urunkarpm.drawer.core.data.worker.TodoWorkerEntryPoint
import com.urunkarpm.drawer.core.data.worker.WeatherRefreshWorker
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class DrawerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WeatherRefreshWorker.enqueuePeriodic(this)
        TodoWatchdogWorker.enqueuePeriodic(this)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    this@DrawerApplication,
                    TodoWorkerEntryPoint::class.java
                )
                entryPoint.todoReconciler().reconcile()
            } catch (_: Exception) {
                // Safeguard against any cold start exceptions
            }
        }
    }
}
