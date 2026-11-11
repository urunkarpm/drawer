package com.urunkarpm.drawer.core.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeadlineAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var todoDao: TodoDao

    @Inject
    lateinit var todoRepository: TodoRepository

    @Inject
    lateinit var notificationHelper: DeadlineNotificationHelper

    companion object {
        const val ACTION_DEADLINE_ALERT = "com.urunkarpm.drawer.ACTION_DEADLINE_ALERT"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ALERT_POINT = "extra_alert_point"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val alertPointName = intent.getStringExtra(EXTRA_ALERT_POINT) ?: DeadlineAlertPoint.AT_DEADLINE.name
        val point = try {
            DeadlineAlertPoint.valueOf(alertPointName)
        } catch (_: Exception) {
            DeadlineAlertPoint.AT_DEADLINE
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = todoDao.getTodoById(taskId) ?: return@launch
                if (entity.status != TodoStatus.OPEN.name) return@launch

                // Check if snoozed
                val now = System.currentTimeMillis()
                val snoozedUntil = entity.snoozedUntil
                if (snoozedUntil != null && snoozedUntil > now) {
                    todoRepository.logEvent("DEADLINE_SKIPPED", "Skipped deadline alert for ${entity.title}: snoozed until $snoozedUntil")
                    return@launch
                }

                // Check quiet hours
                val settings = todoRepository.settings.first()
                val isHighPriority = entity.priority == TodoPriority.HIGH.name
                val isAtDeadline = point == DeadlineAlertPoint.AT_DEADLINE

                val inQuietHours = TodoUtils.isInQuietHours(
                    now = now,
                    startStr = settings.quietHoursStart,
                    endStr = settings.quietHoursEnd
                )

                // High priority with deadline now bypasses quiet hours
                if (inQuietHours && !(isHighPriority && isAtDeadline)) {
                    todoRepository.logEvent("DEADLINE_DEFERRED", "Deferred alert for ${entity.title}: currently in quiet hours (${settings.quietHoursStart} - ${settings.quietHoursEnd})")
                    return@launch
                }

                notificationHelper.showDeadlineAlert(
                    taskId = entity.id,
                    title = entity.title,
                    note = entity.note,
                    alertPointText = point.label,
                    hideTextOnLockScreen = settings.hideTaskTextOnLockScreen
                )

                todoRepository.logEvent("DEADLINE_ALERT_SENT", "Dispatched ${point.label} alert for ${entity.title}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
