package com.urunkarpm.drawer.core.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TodoActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var todoRepository: TodoRepository

    @Inject
    lateinit var todoDao: TodoDao

    @Inject
    lateinit var deadlineNotificationHelper: DeadlineNotificationHelper

    @Inject
    lateinit var locationNotificationHelper: com.urunkarpm.drawer.core.data.location.LocationNotificationHelper

    companion object {
        const val ACTION_COMPLETE = "com.urunkarpm.drawer.ACTION_COMPLETE_TODO"
        const val ACTION_SNOOZE_1H = "com.urunkarpm.drawer.ACTION_SNOOZE_1H_TODO"
        const val ACTION_TOMORROW = "com.urunkarpm.drawer.ACTION_TOMORROW_TODO"
        const val ACTION_DELETE = "com.urunkarpm.drawer.ACTION_DELETE_TODO"
        const val ACTION_NOT_NOW_PLACE = "com.urunkarpm.drawer.ACTION_NOT_NOW_PLACE"
        const val ACTION_REDUCE_ALERTS_PLACE = "com.urunkarpm.drawer.ACTION_REDUCE_ALERTS_PLACE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_PLACE_NAME = "extra_place_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val placeName = intent.getStringExtra(EXTRA_PLACE_NAME)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (notificationId != -1) {
                    locationNotificationHelper.cancelNotification(notificationId)
                }

                when (action) {
                    ACTION_COMPLETE -> {
                        if (taskId != -1L) {
                            todoRepository.completeTask(taskId)
                            deadlineNotificationHelper.cancelDeadlineNotification(taskId)
                        }
                    }
                    ACTION_SNOOZE_1H -> {
                        if (taskId != -1L) {
                            val task = todoDao.getTodoById(taskId)
                            if (task != null) {
                                val snoozedTime = System.currentTimeMillis() + (60 * 60 * 1000L)
                                todoDao.update(task.copy(snoozedUntil = snoozedTime))
                                deadlineNotificationHelper.cancelDeadlineNotification(taskId)
                                todoRepository.logEvent("TODO_SNOOZED", "Snoozed task ${task.title} for 1 hour")
                            }
                        }
                    }
                    ACTION_TOMORROW -> {
                        if (taskId != -1L) {
                            val task = todoDao.getTodoById(taskId)
                            if (task != null) {
                                val tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                                todoDao.update(
                                    task.copy(
                                        deadlineUtc = tomorrow,
                                        staleSince = null,
                                        lastNudgeAt = null,
                                        nudgeCount = 0
                                    )
                                )
                                deadlineNotificationHelper.cancelDeadlineNotification(taskId)
                                todoRepository.logEvent("TODO_MOVED_TOMORROW", "Moved task ${task.title} to tomorrow")
                            }
                        }
                    }
                    ACTION_DELETE -> {
                        if (taskId != -1L) {
                            todoRepository.deleteTodo(taskId)
                            deadlineNotificationHelper.cancelDeadlineNotification(taskId)
                        }
                    }
                    ACTION_NOT_NOW_PLACE -> {
                        val place = placeName ?: "Place"
                        val prefs = context.getSharedPreferences("todo_location_prefs", Context.MODE_PRIVATE)
                        val key = "not_now_count_${place.lowercase()}"
                        val currentCount = prefs.getInt(key, 0) + 1
                        prefs.edit().putInt(key, currentCount).apply()

                        todoRepository.logEvent("LOCATION_NOT_NOW", "User tapped 'Not now' for place $place (count: $currentCount)")

                        // Requirement 13: "After 'Not now' is tapped 3 times for one place, ask: 'Reduce alerts for this place?'"
                        if (currentCount >= 3) {
                            locationNotificationHelper.showReduceAlertsPrompt(place)
                        }
                    }
                    ACTION_REDUCE_ALERTS_PLACE -> {
                        val place = placeName ?: "Place"
                        val prefs = context.getSharedPreferences("todo_location_prefs", Context.MODE_PRIVATE)
                        val suppressUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                        prefs.edit().putLong("suppress_place_${place.lowercase()}", suppressUntil).apply()

                        todoRepository.logEvent("LOCATION_ALERTS_REDUCED", "Reduced location alerts for place: $place for 24 hours")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
