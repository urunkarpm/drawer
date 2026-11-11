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
class StaleNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_STALE_ALERTS = "drawer_stale_alerts"
        const val GROUP_STALE_NOTIFICATION_ID = 5001
        const val SMART_DEADLINE_NOTIFICATION_ID = 5002
        const val STALE_NOTIFICATION_ID_BASE = 6000
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_STALE_ALERTS,
                "Stale Task Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for open tasks that need attention."
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun showDay1Alert(
        taskId: Long,
        title: String,
        hideTextOnLockScreen: Boolean = false
    ) {
        val notificationId = (STALE_NOTIFICATION_ID_BASE + taskId).toInt()
        val notification = buildStaleNotification(
            notificationId = notificationId,
            taskId = taskId,
            headline = "Still open: $title",
            content = "This task has been open for 1 day.",
            isDay7 = false,
            hideTextOnLockScreen = hideTextOnLockScreen
        )
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun showDay2Alert(
        taskId: Long,
        title: String,
        hideTextOnLockScreen: Boolean = false
    ) {
        val notificationId = (STALE_NOTIFICATION_ID_BASE + taskId).toInt()
        val notification = buildStaleNotification(
            notificationId = notificationId,
            taskId = taskId,
            headline = "2 days open: $title",
            content = "This task has been open for 2 days.",
            isDay7 = false,
            hideTextOnLockScreen = hideTextOnLockScreen
        )
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun showDay7Alert(
        taskId: Long,
        title: String,
        hideTextOnLockScreen: Boolean = false
    ) {
        val notificationId = (STALE_NOTIFICATION_ID_BASE + taskId).toInt()
        val notification = buildStaleNotification(
            notificationId = notificationId,
            taskId = taskId,
            headline = "Open for a week: $title",
            content = "Would you like to complete it, move it to tomorrow, or delete it?",
            isDay7 = true,
            hideTextOnLockScreen = hideTextOnLockScreen
        )
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun showGroupedStaleAlert(
        staleCount: Int,
        sampleTitles: List<String>,
        hideTextOnLockScreen: Boolean = false
    ) {
        val title = "$staleCount tasks are stale"
        val content = sampleTitles.take(3).joinToString(", ") + if (sampleTitles.size > 3) "..." else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_STALE_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVisibility(if (hideTextOnLockScreen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(GROUP_STALE_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun showDeadlineApproachingAwayAlert(
        taskId: Long,
        taskTitle: String,
        placeName: String,
        distanceStr: String,
        timeUntilDueStr: String,
        hideTextOnLockScreen: Boolean = false
    ) {
        val notificationId = (SMART_DEADLINE_NOTIFICATION_ID + taskId).toInt()
        val title = "Due in $timeUntilDueStr"
        val content = "$placeName is $distanceStr away. $taskTitle"

        val notification = NotificationCompat.Builder(context, CHANNEL_STALE_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(if (hideTextOnLockScreen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    private fun buildStaleNotification(
        notificationId: Int,
        taskId: Long,
        headline: String,
        content: String,
        isDay7: Boolean,
        hideTextOnLockScreen: Boolean
    ): android.app.Notification {
        // Actions: Done, Tomorrow, Snooze, Delete
        val doneIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_COMPLETE
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val donePI = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tomorrowIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_TOMORROW
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val tomorrowPI = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            tomorrowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_SNOOZE_1H
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val snoozePI = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, TodoActionReceiver::class.java).apply {
            action = TodoActionReceiver.ACTION_DELETE
            putExtra(TodoActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val deletePI = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 4,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_STALE_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(headline)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setVisibility(if (hideTextOnLockScreen) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_manage, if (isDay7) "Do it" else "Done", donePI)
            .addAction(android.R.drawable.ic_menu_today, if (isDay7) "Move date" else "Tomorrow", tomorrowPI)

        if (!isDay7) {
            builder.addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePI)
        }
        builder.addAction(android.R.drawable.ic_menu_delete, "Delete", deletePI)

        return builder.build()
    }
}
