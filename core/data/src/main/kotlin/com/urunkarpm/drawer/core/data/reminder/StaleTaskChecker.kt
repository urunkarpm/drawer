package com.urunkarpm.drawer.core.data.reminder

import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaleTaskChecker @Inject constructor(
    private val todoDao: TodoDao,
    private val todoRepository: TodoRepository,
    private val notificationHelper: StaleNotificationHelper
) {

    suspend fun runDailyCheck(
        now: Long = System.currentTimeMillis(),
        isPowerOnCatchup: Boolean = false
    ): Int {
        val settings = todoRepository.settings.first()
        if (!settings.staleAlertsEnabled) return 0

        // Quiet hours check
        if (TodoUtils.isInQuietHours(now, settings.quietHoursStart, settings.quietHoursEnd)) {
            todoRepository.logEvent(
                "STALE_CHECK_DEFERRED",
                "Deferred daily stale check: currently in quiet hours (${settings.quietHoursStart} - ${settings.quietHoursEnd})"
            )
            return 0
        }

        val allOpen = todoDao.getOpenTodosList().map { it.toDomain() }
        val staleTasks = allOpen.filter { TodoUtils.isStale(it, now) }

        if (staleTasks.isEmpty()) {
            todoRepository.logEvent("STALE_CHECK_COMPLETED", "No stale tasks found")
            return 0
        }

        // Rule 9: If phone was off at check time, run at next start with one grouped alert
        if (isPowerOnCatchup) {
            notificationHelper.showGroupedStaleAlert(
                staleCount = staleTasks.size,
                sampleTitles = staleTasks.map { it.title },
                hideTextOnLockScreen = settings.hideTaskTextOnLockScreen
            )
            staleTasks.forEach { task ->
                todoDao.getTodoById(task.id)?.let { entity ->
                    todoDao.update(
                        entity.copy(
                            lastNudgeAt = now,
                            nudgeCount = entity.nudgeCount + 1,
                            staleSince = entity.staleSince ?: entity.createdAt
                        )
                    )
                }
            }
            todoRepository.logEvent("STALE_CATCHUP_SENT", "Sent grouped catch-up alert for ${staleTasks.size} stale tasks")
            return staleTasks.size
        }

        // Rule 10: Idempotent execution using last_nudge_at
        // Filter out tasks already nudged today
        val unnudgedToday = staleTasks.filter { task ->
            val lastNudge = task.lastNudgeAt
            if (lastNudge == null || lastNudge <= 0L) {
                true
            } else {
                !isSameDay(lastNudge, now)
            }
        }

        if (unnudgedToday.isEmpty()) {
            todoRepository.logEvent("STALE_CHECK_SKIPPED", "All ${staleTasks.size} stale tasks already nudged today")
            return 0
        }

        // Sort: HIGH priority first, then longest open
        val sorted = unnudgedToday.sortedWith(
            compareByDescending<TodoItem> { it.priority == TodoPriority.HIGH }
                .thenBy { it.createdAt }
        )

        // Rule 8: Send 3 stale alerts each day at most. Group the rest.
        val maxIndividual = settings.maxStaleAlertsPerDay.coerceAtLeast(1)
        val individualList = sorted.take(maxIndividual)
        val remainingList = sorted.drop(maxIndividual)

        for (task in individualList) {
            val staleStartTime = task.staleSince ?: (task.createdAt + if (task.deadlineUtc != null) 0L else TodoUtils.ONE_DAY_MILLIS)
            val daysStale = ((now - staleStartTime) / TodoUtils.ONE_DAY_MILLIS).toInt() + 1
            val nudgeNum = task.nudgeCount + 1

            when {
                daysStale >= 7 || nudgeNum >= 7 -> notificationHelper.showDay7Alert(task.id, task.title, settings.hideTaskTextOnLockScreen)
                nudgeNum == 1 || daysStale == 1 -> notificationHelper.showDay1Alert(task.id, task.title, settings.hideTaskTextOnLockScreen)
                nudgeNum == 2 || daysStale == 2 -> notificationHelper.showDay2Alert(task.id, task.title, settings.hideTaskTextOnLockScreen)
                else -> notificationHelper.showDay2Alert(task.id, task.title, settings.hideTaskTextOnLockScreen)
            }

            todoDao.getTodoById(task.id)?.let { entity ->
                todoDao.update(
                    entity.copy(
                        lastNudgeAt = now,
                        nudgeCount = entity.nudgeCount + 1,
                        staleSince = entity.staleSince ?: staleStartTime
                    )
                )
            }
        }

        if (remainingList.isNotEmpty()) {
            notificationHelper.showGroupedStaleAlert(
                staleCount = remainingList.size,
                sampleTitles = remainingList.map { it.title },
                hideTextOnLockScreen = settings.hideTaskTextOnLockScreen
            )
            for (task in remainingList) {
                todoDao.getTodoById(task.id)?.let { entity ->
                    todoDao.update(
                        entity.copy(
                            lastNudgeAt = now,
                            nudgeCount = entity.nudgeCount + 1,
                            staleSince = entity.staleSince ?: entity.createdAt
                        )
                    )
                }
            }
        }

        todoRepository.logEvent(
            "STALE_CHECK_COMPLETED",
            "Dispatched alerts: ${individualList.size} individual, ${remainingList.size} grouped"
        )

        return sorted.size
    }

    /**
     * Rule 5: For HIGH priority tasks, repeat the alert every 4 hours in the daytime. Send 3 alerts at most.
     */
    suspend fun runHighPriorityDaytimeCheck(now: Long = System.currentTimeMillis()): Int {
        val settings = todoRepository.settings.first()
        if (!settings.staleAlertsEnabled) return 0
        if (TodoUtils.isInQuietHours(now, settings.quietHoursStart, settings.quietHoursEnd)) return 0

        val allOpen = todoDao.getOpenTodosList().map { it.toDomain() }
        val highPriorityStale = allOpen.filter {
            it.status == TodoStatus.OPEN &&
            it.priority == TodoPriority.HIGH &&
            TodoUtils.isStale(it, now) &&
            it.nudgeCount < 3
        }

        var dispatched = 0
        for (task in highPriorityStale) {
            val lastNudge = task.lastNudgeAt
            // Must be at least 4 hours since last nudge
            if (lastNudge == null || (now - lastNudge) >= 4 * 3600 * 1000L) {
                notificationHelper.showDay1Alert(task.id, task.title, settings.hideTaskTextOnLockScreen)
                todoDao.getTodoById(task.id)?.let { entity ->
                    todoDao.update(entity.copy(lastNudgeAt = now, nudgeCount = entity.nudgeCount + 1))
                }
                dispatched++
            }
        }

        if (dispatched > 0) {
            todoRepository.logEvent("HIGH_PRIORITY_REPEAT_SENT", "Dispatched 4-hour daytime repeat alert for $dispatched tasks")
        }

        return dispatched
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val zoneId = ZoneId.systemDefault()
        val d1 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp1), zoneId).toLocalDate()
        val d2 = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp2), zoneId).toLocalDate()
        return d1.isEqual(d2)
    }
}
