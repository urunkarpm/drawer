package com.urunkarpm.drawer.core.data.reminder

import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StaleTaskCheckerTest {

    private val todoDao: TodoDao = mockk(relaxed = true)
    private val todoRepository: TodoRepository = mockk(relaxed = true)
    private val notificationHelper: StaleNotificationHelper = mockk(relaxed = true)

    private val settingsFlow = MutableStateFlow(
        TodoSettings(
            staleAlertsEnabled = true,
            quietHoursStart = "22:00",
            quietHoursEnd = "07:00",
            maxStaleAlertsPerDay = 3
        )
    )

    private lateinit var checker: StaleTaskChecker

    @Before
    fun setUp() {
        coEvery { todoRepository.settings } returns settingsFlow
        checker = StaleTaskChecker(
            todoDao = todoDao,
            todoRepository = todoRepository,
            notificationHelper = notificationHelper
        )
    }

    @Test
    fun runDailyCheck_escalatesDay1Task() = runTest {
        // Daytime: 12:00 PM
        val now = 1794474000000L
        val day1Task = TodoEntity(
            id = 101L,
            title = "Task day 1",
            status = "OPEN",
            createdAt = now - (25 * 3600 * 1000L), // 25 hours old
            lastNudgeAt = null
        )

        coEvery { todoDao.getOpenTodosList() } returns listOf(day1Task)
        coEvery { todoDao.getTodoById(101L) } returns day1Task

        val count = checker.runDailyCheck(now = now)

        assertEquals(1, count)
        coVerify { notificationHelper.showDay1Alert(101L, "Task day 1", any()) }
    }

    @Test
    fun runDailyCheck_isIdempotent_skipsIfAlreadyNudgedToday() = runTest {
        val now = 1794474000000L
        val alreadyNudgedToday = TodoEntity(
            id = 102L,
            title = "Already nudged task",
            status = "OPEN",
            createdAt = now - (30 * 3600 * 1000L),
            lastNudgeAt = now - 1000L // 1 second ago (today)
        )

        coEvery { todoDao.getOpenTodosList() } returns listOf(alreadyNudgedToday)

        val count = checker.runDailyCheck(now = now)

        assertEquals(0, count)
        coVerify(exactly = 0) { notificationHelper.showDay1Alert(any(), any(), any()) }
        coVerify(exactly = 0) { notificationHelper.showGroupedStaleAlert(any(), any(), any()) }
    }

    @Test
    fun runDailyCheck_groupsRemaining_whenExceedsMaxPerDay() = runTest {
        val now = 1794474000000L
        val tasks = (1..5).map { i ->
            TodoEntity(
                id = i.toLong(),
                title = "Task $i",
                status = "OPEN",
                createdAt = now - ((25 + i) * 3600 * 1000L),
                lastNudgeAt = null
            )
        }

        coEvery { todoDao.getOpenTodosList() } returns tasks
        tasks.forEach { t -> coEvery { todoDao.getTodoById(t.id) } returns t }

        val count = checker.runDailyCheck(now = now)

        assertEquals(5, count)
        // Max 3 individual alerts
        coVerify(exactly = 3) { notificationHelper.showDay1Alert(any(), any(), any()) }
        // Remaining 2 are grouped
        coVerify(exactly = 1) { notificationHelper.showGroupedStaleAlert(2, any(), any()) }
    }

    @Test
    fun runDailyCheck_powerOnCatchup_sendsSingleGroupedAlert() = runTest {
        val now = 1794474000000L
        val tasks = (1..4).map { i ->
            TodoEntity(
                id = i.toLong(),
                title = "Task $i",
                status = "OPEN",
                createdAt = now - (30 * 3600 * 1000L),
                lastNudgeAt = null
            )
        }

        coEvery { todoDao.getOpenTodosList() } returns tasks
        tasks.forEach { t -> coEvery { todoDao.getTodoById(t.id) } returns t }

        val count = checker.runDailyCheck(now = now, isPowerOnCatchup = true)

        assertEquals(4, count)
        coVerify(exactly = 0) { notificationHelper.showDay1Alert(any(), any(), any()) }
        coVerify(exactly = 1) { notificationHelper.showGroupedStaleAlert(4, any(), any()) }
    }
}
