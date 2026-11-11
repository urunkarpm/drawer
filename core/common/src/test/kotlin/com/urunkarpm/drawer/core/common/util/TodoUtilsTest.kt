package com.urunkarpm.drawer.core.common.util

import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TodoUtilsTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun isStale_passedDeadline_returnsTrue() {
        val now = 1000000000000L
        val task = TodoItem(
            title = "Task",
            status = TodoStatus.OPEN,
            deadlineUtc = now - 1000L,
            createdAt = now - 5000L
        )
        assertTrue(TodoUtils.isStale(task, now))
    }

    @Test
    fun isStale_futureDeadline_returnsFalseEvenIfOld() {
        val now = 1000000000000L
        // Created 48 hours ago, but deadline is tomorrow
        val task = TodoItem(
            title = "Task",
            status = TodoStatus.OPEN,
            deadlineUtc = now + 86400000L,
            createdAt = now - 2 * 86400000L
        )
        assertFalse(TodoUtils.isStale(task, now))
    }

    @Test
    fun isStale_noDeadline_olderThan24Hours_returnsTrue() {
        val now = 1000000000000L
        val task = TodoItem(
            title = "Task",
            status = TodoStatus.OPEN,
            deadlineUtc = null,
            createdAt = now - (25 * 3600 * 1000L) // 25 hours
        )
        assertTrue(TodoUtils.isStale(task, now))
    }

    @Test
    fun isStale_noDeadline_recent_returnsFalse() {
        val now = 1000000000000L
        val task = TodoItem(
            title = "Task",
            status = TodoStatus.OPEN,
            deadlineUtc = null,
            createdAt = now - (10 * 3600 * 1000L) // 10 hours
        )
        assertFalse(TodoUtils.isStale(task, now))
    }

    @Test
    fun isStale_snoozed_returnsFalse() {
        val now = 1000000000000L
        val task = TodoItem(
            title = "Task",
            status = TodoStatus.OPEN,
            snoozedUntil = now + 3600000L,
            deadlineUtc = now - 1000L,
            createdAt = now - 2 * 86400000L
        )
        assertFalse(TodoUtils.isStale(task, now))
    }

    @Test
    fun isStale_done_returnsFalse() {
        val now = 1000000000000L
        val task = TodoItem(
            title = "Task",
            status = TodoStatus.DONE,
            deadlineUtc = now - 1000L,
            createdAt = now - 2 * 86400000L
        )
        assertFalse(TodoUtils.isStale(task, now))
    }

    @Test
    fun isDueToday_matchesToday() {
        val today = LocalDate.of(2026, 11, 12)
        val now = ZonedDateTime.of(today, LocalTime.of(10, 0), zoneId).toInstant().toEpochMilli()
        val deadlineToday = ZonedDateTime.of(today, LocalTime.of(18, 0), zoneId).toInstant().toEpochMilli()
        val deadlineTomorrow = ZonedDateTime.of(today.plusDays(1), LocalTime.of(9, 0), zoneId).toInstant().toEpochMilli()

        assertTrue(TodoUtils.isDueToday(deadlineToday, "UTC", now))
        assertFalse(TodoUtils.isDueToday(deadlineTomorrow, "UTC", now))
    }

    @Test
    fun isOverdue_checksPastDeadline() {
        val now = 1000000L
        assertTrue(TodoUtils.isOverdue(now - 100L, now))
        assertFalse(TodoUtils.isOverdue(now + 100L, now))
        assertFalse(TodoUtils.isOverdue(null, now))
    }

    @Test
    fun isInQuietHours_overnightInterval() {
        // Quiet hours 22:00 to 07:00
        val date = LocalDate.of(2026, 11, 12)

        val time2300 = ZonedDateTime.of(date, LocalTime.of(23, 0), zoneId).toInstant().toEpochMilli()
        val time0300 = ZonedDateTime.of(date, LocalTime.of(3, 0), zoneId).toInstant().toEpochMilli()
        val time1200 = ZonedDateTime.of(date, LocalTime.of(12, 0), zoneId).toInstant().toEpochMilli()
        val time2159 = ZonedDateTime.of(date, LocalTime.of(21, 59), zoneId).toInstant().toEpochMilli()

        assertTrue(TodoUtils.isInQuietHours(time2300, "22:00", "07:00", zoneId))
        assertTrue(TodoUtils.isInQuietHours(time0300, "22:00", "07:00", zoneId))
        assertFalse(TodoUtils.isInQuietHours(time1200, "22:00", "07:00", zoneId))
        assertFalse(TodoUtils.isInQuietHours(time2159, "22:00", "07:00", zoneId))
    }
}
