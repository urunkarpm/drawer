package com.urunkarpm.drawer.core.common.util

import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoStatus
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object TodoUtils {

    const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

    /**
     * A task is stale when its status is OPEN and one of these is true:
     * a. The deadline has passed.
     * b. The task has no deadline, and 24 hours have passed since the task was added or reset (staleSince or createdAt).
     *
     * A snoozed task is never stale.
     * A task with a future deadline is not stale because of its age.
     */
    fun isStale(task: TodoItem, now: Long = System.currentTimeMillis()): Boolean {
        if (task.status != TodoStatus.OPEN) return false

        // If snoozed_until is in future, it's not stale
        val snoozedUntil = task.snoozedUntil
        if (snoozedUntil != null && snoozedUntil > now) return false

        val deadline = task.deadlineUtc
        if (deadline != null && deadline > 0) {
            // Task has a deadline
            return deadline < now
        }

        // Task has no deadline
        val baseline = task.staleSince ?: task.createdAt
        return (now - baseline) >= ONE_DAY_MILLIS
    }

    /**
     * Checks if task deadline falls within today in local time zone.
     */
    fun isDueToday(
        deadlineUtc: Long?,
        zoneIdString: String? = null,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        if (deadlineUtc == null || deadlineUtc <= 0) return false
        val zoneId = try {
            if (!zoneIdString.isNullOrBlank()) ZoneId.of(zoneIdString) else ZoneId.systemDefault()
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val deadlineDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(deadlineUtc), zoneId).toLocalDate()
        val todayDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zoneId).toLocalDate()

        return deadlineDate.isEqual(todayDate)
    }

    /**
     * Checks if deadline is strictly in the past.
     */
    fun isOverdue(
        deadlineUtc: Long?,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        return deadlineUtc != null && deadlineUtc > 0 && deadlineUtc < now
    }

    /**
     * Parses HH:mm string safely.
     */
    fun parseLocalTime(timeStr: String, default: LocalTime): LocalTime {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } else {
                default
            }
        } catch (_: Exception) {
            default
        }
    }

    /**
     * Checks whether given timestamp falls within quiet hours (e.g. 22:00 to 07:00).
     */
    fun isInQuietHours(
        now: Long = System.currentTimeMillis(),
        startStr: String = "22:00",
        endStr: String = "07:00",
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val currentTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zoneId).toLocalTime()
        val start = parseLocalTime(startStr, LocalTime.of(22, 0))
        val end = parseLocalTime(endStr, LocalTime.of(7, 0))

        return if (start.isBefore(end)) {
            // e.g. 01:00 to 06:00
            !currentTime.isBefore(start) && currentTime.isBefore(end)
        } else {
            // Overnight, e.g. 22:00 to 07:00
            !currentTime.isBefore(start) || currentTime.isBefore(end)
        }
    }
}
