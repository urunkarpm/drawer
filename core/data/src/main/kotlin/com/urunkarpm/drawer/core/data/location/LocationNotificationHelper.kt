package com.urunkarpm.drawer.core.data.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.urunkarpm.drawer.core.data.reminder.TodoActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_LOCATION_SERVICE = "drawer_location_service"
        const val CHANNEL_LOCATION_ALERTS = "drawer_location_alerts"
        const val SERVICE_NOTIFICATION_ID = 2001
        const val GROUP_LOCATION_NOTIFICATION_ID = 2002
        const val REDUCE_ALERTS_NOTIFICATION_ID = 2003
        const val NOTIFICATION_ID_BASE = 3000
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_LOCATION_SERVICE,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing location monitoring status for nearby tasks."
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_LOCATION_ALERTS,
                "Nearby Task Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you arrive at a task place."
                enableVibration(true)
                setShowBadge(true)
            }

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(serviceChannel)
            nm?.createNotificationChannel(alertsChannel)
        }
    }

    fun buildForegroundServiceNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_LOCATION_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Drawer")
            .setContentText("Drawer is watching nearby task places.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showLocationAlert(
        taskId: Long,
        placeName: String,
        taskTitle: String,
        hideTextOnLockScreen: Boolean = false
    ) {
        val notificationId = (NOTIFICATION_ID_BASE + taskId).toInt()

        // 1. Done Action
        val doneIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_COMPLETE
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Snooze 1h Action
        val snoozeIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_SNOOZE_1H
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Not now Action
        val notNowIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_NOT_NOW_PLACE
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TodoActionReceiver.EXTRA_PLACE_NAME, placeName)
            putExtra(TodoActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val notNowPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            notNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicVersion = if (hideTextOnLockScreen) {
            NotificationCompat.Builder(context, CHANNEL_LOCATION_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Nearby task reminder")
                .setContentText("You are near a task location")
                .build()
        } else null

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCATION_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("You are near $placeName")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(if (hideTextOnLockScreen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicVersion)
            .addAction(android.R.drawable.ic_menu_manage, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze (1h)", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Not now", notNowPendingIntent)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Notification permission missing
        }
    }

    fun showGroupedLocationAlert(placesCount: Int, placeNames: List<String>, hideTextOnLockScreen: Boolean = false) {
        val title = "Nearby task places"
        val content = "You are near $placesCount task places: ${placeNames.joinToString(", ")}"

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCATION_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(if (hideTextOnLockScreen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(GROUP_LOCATION_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission missing
        }
    }

    fun showReduceAlertsPrompt(placeName: String) {
        val snoozePlaceIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_REDUCE_ALERTS_PLACE
            putExtra(TodoActionReceiver.EXTRA_PLACE_NAME, placeName)
            putExtra(TodoActionReceiver.EXTRA_NOTIFICATION_ID, REDUCE_ALERTS_NOTIFICATION_ID)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            REDUCE_ALERTS_NOTIFICATION_ID * 10 + 1,
            snoozePlaceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LOCATION_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Reduce alerts for $placeName?")
            .setContentText("You've tapped 'Not now' 3 times for $placeName. Would you like to snooze alerts for this place for 1 day?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 1 day", snoozePendingIntent)
            .build()

        try {
            notificationManager.notify(REDUCE_ALERTS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission missing
        }
    }

    fun cancelNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
        } catch (_: Exception) {}
    }
}
