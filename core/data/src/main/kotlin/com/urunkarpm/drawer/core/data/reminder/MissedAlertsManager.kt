package com.urunkarpm.drawer.core.data.reminder

import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissedAlertsManager @Inject constructor(
    private val todoDao: TodoDao,
    private val todoRepository: TodoRepository,
    private val notificationHelper: DeadlineNotificationHelper
) {

    private var lastCheckedBootTime: Long = 0L

    suspend fun checkAndNotifyMissedAlerts(deviceBootTime: Long = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()) {
        val now = System.currentTimeMillis()
        val openTasks = todoDao.getOpenTodosList()

        // Find open tasks whose deadline was in the past and occurred after device was shut down or before boot
        val missedTasks = openTasks.filter { task ->
            val deadline = task.deadlineUtc
            val lastAlert = task.lastLocationAlertAt
            deadline != null && deadline in (deviceBootTime..now) && (lastAlert == null || lastAlert < deadline)
        }

        if (missedTasks.isNotEmpty()) {
            notificationHelper.showMissedAlertsNotification(missedTasks.size)
            todoRepository.logEvent("MISSED_ALERTS_DETECTED", "Found ${missedTasks.size} missed deadline alerts from device reboot/downtime")
        }

        lastCheckedBootTime = now
    }
}
