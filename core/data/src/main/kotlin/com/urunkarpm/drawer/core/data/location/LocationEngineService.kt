package com.urunkarpm.drawer.core.data.location

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationEngineService : Service() {

    @Inject
    lateinit var notificationHelper: LocationNotificationHelper

    @Inject
    lateinit var locationEngineManager: LocationEngineManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = notificationHelper.buildForegroundServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                LocationNotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(LocationNotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationEngineManager.registerListeners(intervalMs = 45_000L)
        return START_STICKY
    }

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, LocationEngineService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, LocationEngineService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }
}
