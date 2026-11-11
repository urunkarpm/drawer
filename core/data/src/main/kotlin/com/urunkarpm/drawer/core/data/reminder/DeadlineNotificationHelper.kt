package com.urunkarpm.drawer.core.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_DEADLINES = "todo_deadlines"
        const val CHANNEL_LOCATIONS = "todo_locations"
        const val CHANNEL_STALE = "todo_stale"
        const val CHANNEL_ENGINE = "todo_engine"

        const val MISSED_ALERTS_NOTIFICATION_ID = 99991
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Deadline alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when a task deadline arrives or approaches"
                enableVibration(true)
            }

            val locationChannel = NotificationChannel(
                CHANNEL_LOCATIONS,
                "Location reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders when you arrive at a task location"
                enableVibration(true)
            }

            val staleChannel = NotificationChannel(
                CHANNEL_STALE,
                "Stale tasks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily checks and escalation for unfinished stale tasks"
            }

            val engineChannel = NotificationChannel(
                CHANNEL_ENGINE,
                "Engine status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location watcher status"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(deadlineChannel, locationChannel, staleChannel, engineChannel)
            )
        }
    }

    fun showDeadlineAlert(
        taskId: Long,
        title: String,
        note: String?,
        alertPointText: String,
        hideTextOnLockScreen: Boolean
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        // Action: Done
        val doneIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_COMPLETE
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 100 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 1 hour
        val snoozeIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_SNOOZE_1H
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId * 100 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (!note.isNullOrBlank()) {
            "$alertPointText • $note"
        } else {
            alertPointText
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze (1h)", snoozePendingIntent)

        if (hideTextOnLockScreen) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            val publicNotification = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("To-Do Reminder")
                .setContentText("You have a scheduled task deadline")
                .build()
            builder.setPublicVersion(publicNotification)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        try {
            manager.notify("todo_deadline", taskId.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Notification permission revoked
        }
    }

    fun showMissedAlertsNotification(missedCount: Int) {
        if (missedCount <= 0) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val text = if (missedCount == 1) {
            "1 task deadline was missed while your phone was off."
        } else {
            "$missedCount task deadlines were missed while your phone was off."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed alert")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify("missed_alerts", MISSED_ALERTS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelDeadlineNotification(taskId: Long) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel("todo_deadline", taskId.toInt())
    }
}
