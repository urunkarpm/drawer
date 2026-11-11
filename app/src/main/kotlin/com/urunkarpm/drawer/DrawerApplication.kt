package com.urunkarpm.drawer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DrawerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.urunkarpm.drawer.core.data.worker.WeatherRefreshWorker.enqueuePeriodic(this)
    }
}
