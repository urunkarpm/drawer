package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.EventLogDao
import com.urunkarpm.drawer.core.database.dao.RecentSearchDao
import com.urunkarpm.drawer.core.database.dao.SavedPlaceDao
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoSettings
import com.urunkarpm.drawer.core.model.TodoStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TodoRepositoryTest {

    private val todoDao: TodoDao = mockk(relaxed = true)
    private val savedPlaceDao: SavedPlaceDao = mockk(relaxed = true)
    private val recentSearchDao: RecentSearchDao = mockk(relaxed = true)
    private val eventLogDao: EventLogDao = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val deadlineAlarmScheduler: com.urunkarpm.drawer.core.data.reminder.DeadlineAlarmScheduler = mockk(relaxed = true)
    private val deadlineNotificationHelper: com.urunkarpm.drawer.core.data.reminder.DeadlineNotificationHelper = mockk(relaxed = true)
    private val locationEngineManager: com.urunkarpm.drawer.core.data.location.LocationEngineManager = mockk(relaxed = true)

    private val settingsFlow = MutableStateFlow(TodoSettings())
    private lateinit var repository: DefaultTodoRepository

    @Before
    fun setUp() {
        every { preferencesDataSource.todoSettings } returns settingsFlow
        repository = DefaultTodoRepository(
            todoDao = todoDao,
            savedPlaceDao = savedPlaceDao,
            recentSearchDao = recentSearchDao,
            eventLogDao = eventLogDao,
            preferencesDataSource = preferencesDataSource,
            deadlineAlarmScheduler = deadlineAlarmScheduler,
            deadlineNotificationHelper = deadlineNotificationHelper,
            locationEngineManager = javax.inject.Provider { locationEngineManager }
        )
    }

    @Test
    fun calculateNextDeadline_daily_addsOneDay() {
        val zoneId = ZoneId.of("UTC")
        val currentZdt = ZonedDateTime.of(LocalDate.of(2026, 11, 12), LocalTime.of(15, 0), zoneId)
        val currentUtc = currentZdt.toInstant().toEpochMilli()

        val nextUtc = DefaultTodoRepository.calculateNextDeadline(
            currentDeadlineUtc = currentUtc,
            zoneIdString = "UTC",
            repeatRule = "DAILY",
            nowUtc = currentUtc
        )

        val nextZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nextUtc), zoneId)
        assertEquals(LocalDate.of(2026, 11, 13), nextZdt.toLocalDate())
        assertEquals(15, nextZdt.hour)
    }

    @Test
    fun calculateNextDeadline_weekdays_skipsWeekend() {
        val zoneId = ZoneId.of("UTC")
        // Friday Nov 13, 2026
        val friday = ZonedDateTime.of(LocalDate.of(2026, 11, 13), LocalTime.of(10, 0), zoneId)
        val fridayUtc = friday.toInstant().toEpochMilli()

        val nextUtc = DefaultTodoRepository.calculateNextDeadline(
            currentDeadlineUtc = fridayUtc,
            zoneIdString = "UTC",
            repeatRule = "WEEKDAYS",
            nowUtc = fridayUtc
        )

        val nextZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nextUtc), zoneId)
        assertEquals(DayOfWeek.MONDAY, nextZdt.dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 16), nextZdt.toLocalDate())
    }

    @Test
    fun completeTask_updatesStatusAndCompletedAt() = runTest {
        val todo = TodoEntity(
            id = 42L,
            title = "Test Task",
            status = "OPEN",
            repeatRule = null
        )
        coEvery { todoDao.getTodoById(42L) } returns todo andThen todo.copy(status = "DONE", completedAt = 123456L)

        val result = repository.completeTask(42L, now = 123456L)
        assertNotNull(result)
        coVerify { todoDao.updateStatus(42L, "DONE", 123456L) }
        coVerify { deadlineAlarmScheduler.cancelAlarms(42L) }
        coVerify { deadlineNotificationHelper.cancelDeadlineNotification(42L) }
    }

    @Test
    fun completeTask_withRepeatRule_createsNextTask() = runTest {
        val zoneId = "UTC"
        val now = 1700000000000L
        val todo = TodoEntity(
            id = 10L,
            title = "Recurring Task",
            status = "OPEN",
            deadlineUtc = now,
            deadlineZoneId = zoneId,
            repeatRule = "DAILY"
        )
        coEvery { todoDao.getTodoById(10L) } returns todo andThen todo.copy(status = "DONE", completedAt = now)

        val slot = slot<TodoEntity>()
        coEvery { todoDao.insert(capture(slot)) } returns 11L

        repository.completeTask(10L, now = now)

        coVerify { todoDao.updateStatus(10L, "DONE", now) }
        assertTrue(slot.isCaptured)
        assertEquals("Recurring Task", slot.captured.title)
        assertEquals("OPEN", slot.captured.status)
        assertEquals("DAILY", slot.captured.repeatRule)
    }

    @Test
    fun restoreTask_resetsStatusToOpen() = runTest {
        val todo = TodoEntity(
            id = 20L,
            title = "Done Task",
            status = "DONE",
            completedAt = 99999L
        )
        coEvery { todoDao.getTodoById(20L) } returns todo

        repository.restoreTask(20L)

        val slot = slot<TodoEntity>()
        coVerify { todoDao.update(capture(slot)) }
        assertEquals("OPEN", slot.captured.status)
        assertEquals(null, slot.captured.completedAt)
    }
}
