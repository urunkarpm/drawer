package com.urunkarpm.drawer.core.data.reconcile

import android.content.Context
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.data.reminder.DeadlineAlarmScheduler
import com.urunkarpm.drawer.core.data.reminder.DeadlineNotificationHelper
import com.urunkarpm.drawer.core.data.reminder.MissedAlertsManager
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.TodoStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoReconciler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val todoDao: TodoDao,
    private val todoRepository: TodoRepository,
    private val deadlineAlarmScheduler: DeadlineAlarmScheduler,
    private val notificationHelper: DeadlineNotificationHelper,
    private val missedAlertsManager: MissedAlertsManager,
    private val locationEngineManager: javax.inject.Provider<com.urunkarpm.drawer.core.data.location.LocationEngineManager>,
    private val staleAlarmScheduler: javax.inject.Provider<com.urunkarpm.drawer.core.data.reminder.StaleAlarmScheduler>,
    private val staleTaskChecker: javax.inject.Provider<com.urunkarpm.drawer.core.data.reminder.StaleTaskChecker>
) {

    private val mutex = Mutex()
    private var lastReconciledTimestamp: Long = 0L

    /**
     * Idempotent reconcile function. Rebuilds all alarms, the check list, and the engine state
     * from the database. The database is the only source of truth.
     */
    suspend fun reconcile(): ReconcileResult = mutex.withLock {
        val now = System.currentTimeMillis()
        val allOpenTasks = todoDao.getOpenTodosList()
        val settings = todoRepository.settings.first()

        // 1. Check for missed deadline alerts occurred while device was off/restarting
        missedAlertsManager.checkAndNotifyMissedAlerts()

        // 2. Rebuild deadline alarms for all active open tasks with future deadlines
        var scheduledAlarmsCount = 0
        allOpenTasks.forEach { entity ->
            val task = entity.toDomain()
            // Cancel any old alarms first to guarantee clean slate and idempotency
            deadlineAlarmScheduler.cancelAlarms(task.id)

            val deadlineUtc = task.deadlineUtc
            if (task.status == TodoStatus.OPEN && deadlineUtc != null && deadlineUtc > now) {
                deadlineAlarmScheduler.scheduleAlarms(task)
                scheduledAlarmsCount++
            }
        }

        // 3. Location engine check list:
        // Filter tasks that have places
        val tasksWithPlaces = allOpenTasks.filter {
            it.latitude != null && it.longitude != null && it.status == TodoStatus.OPEN.name
        }
        locationEngineManager.get().updateTargetsAndEvaluate()

        // 4. Stale task daily check & missed check catch-up
        if (settings.staleAlertsEnabled) {
            val checkTime = com.urunkarpm.drawer.core.common.util.TodoUtils.parseLocalTime(
                settings.dailyCheckTime,
                java.time.LocalTime.of(9, 0)
            )
            val nowTime = java.time.LocalTime.now()
            if (nowTime.isAfter(checkTime)) {
                staleTaskChecker.get().runDailyCheck(now = now, isPowerOnCatchup = true)
            }
            staleAlarmScheduler.get().scheduleDailyCheck(settings)
        }

        lastReconciledTimestamp = now
        todoRepository.logEvent(
            "RECONCILE_SUCCESS",
            "Reconciled: ${allOpenTasks.size} open tasks, $scheduledAlarmsCount deadline alarms, ${tasksWithPlaces.size} location targets"
        )

        return@withLock ReconcileResult(
            openTasksCount = allOpenTasks.size,
            scheduledAlarmsCount = scheduledAlarmsCount,
            locationTasksCount = tasksWithPlaces.size,
            timestamp = now
        )
    }

    fun getLastReconciledTime(): Long = lastReconciledTimestamp
}

data class ReconcileResult(
    val openTasksCount: Int,
    val scheduledAlarmsCount: Int,
    val locationTasksCount: Int,
    val timestamp: Long
)
