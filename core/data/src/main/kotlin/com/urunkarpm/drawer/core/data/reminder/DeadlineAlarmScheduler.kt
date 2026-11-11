package com.urunkarpm.drawer.core.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.urunkarpm.drawer.core.model.TodoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class DeadlineAlertPoint(val offsetMillis: Long, val label: String) {
    ONE_DAY_BEFORE(-24 * 60 * 60 * 1000L, "Due tomorrow"),
    ONE_HOUR_BEFORE(-60 * 60 * 1000L, "Due in 1 hour"),
    AT_DEADLINE(0L, "Due now")
}

@Singleton
class DeadlineAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules the default alert points (1 day before, 1 hour before, and at deadline).
     */
    fun scheduleAlarms(task: TodoItem) {
        val deadlineUtc = task.deadlineUtc ?: return
        if (deadlineUtc <= 0) return

        val now = System.currentTimeMillis()

        DeadlineAlertPoint.entries.forEachIndexed { index, point ->
            val triggerTime = deadlineUtc + point.offsetMillis
            if (triggerTime > now) {
                scheduleSingleAlarm(
                    taskId = task.id,
                    point = point,
                    pointIndex = index,
                    triggerAt = triggerTime
                )
            }
        }
    }

    private fun scheduleSingleAlarm(
        taskId: Long,
        point: DeadlineAlertPoint,
        pointIndex: Int,
        triggerAt: Long
    ) {
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            action = DeadlineAlarmReceiver.ACTION_DEADLINE_ALERT
            putExtra(DeadlineAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(DeadlineAlarmReceiver.EXTRA_ALERT_POINT, point.name)
        }

        val requestCode = (taskId * 10 + pointIndex).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Exact alarm permission not granted, fallback to inexact
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Cancels all scheduled deadline alarms for a task.
     */
    fun cancelAlarms(taskId: Long) {
        DeadlineAlertPoint.entries.forEachIndexed { index, _ ->
            val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
                action = DeadlineAlarmReceiver.ACTION_DEADLINE_ALERT
                putExtra(DeadlineAlarmReceiver.EXTRA_TASK_ID, taskId)
            }
            val requestCode = (taskId * 10 + index).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
