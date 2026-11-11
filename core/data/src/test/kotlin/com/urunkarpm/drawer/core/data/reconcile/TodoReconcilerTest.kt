package com.urunkarpm.drawer.core.data.reconcile

import android.content.Context
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import com.urunkarpm.drawer.core.data.reminder.DeadlineAlarmScheduler
import com.urunkarpm.drawer.core.data.reminder.DeadlineNotificationHelper
import com.urunkarpm.drawer.core.data.reminder.MissedAlertsManager
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.TodoSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TodoReconcilerTest {

    private val context: Context = mockk(relaxed = true)
    private val todoDao: TodoDao = mockk(relaxed = true)
    private val todoRepository: TodoRepository = mockk(relaxed = true)
    private val deadlineAlarmScheduler: DeadlineAlarmScheduler = mockk(relaxed = true)
    private val notificationHelper: DeadlineNotificationHelper = mockk(relaxed = true)
    private val missedAlertsManager: MissedAlertsManager = mockk(relaxed = true)
    private val locationEngineManager: com.urunkarpm.drawer.core.data.location.LocationEngineManager = mockk(relaxed = true)
    private val staleAlarmScheduler: com.urunkarpm.drawer.core.data.reminder.StaleAlarmScheduler = mockk(relaxed = true)
    private val staleTaskChecker: com.urunkarpm.drawer.core.data.reminder.StaleTaskChecker = mockk(relaxed = true)

    private val settingsFlow = MutableStateFlow(TodoSettings())
    private lateinit var reconciler: TodoReconciler

    @Before
    fun setUp() {
        coEvery { todoRepository.settings } returns settingsFlow
        reconciler = TodoReconciler(
            context = context,
            todoDao = todoDao,
            todoRepository = todoRepository,
            deadlineAlarmScheduler = deadlineAlarmScheduler,
            notificationHelper = notificationHelper,
            missedAlertsManager = missedAlertsManager,
            locationEngineManager = javax.inject.Provider { locationEngineManager },
            staleAlarmScheduler = javax.inject.Provider { staleAlarmScheduler },
            staleTaskChecker = javax.inject.Provider { staleTaskChecker }
        )
    }

    @Test
    fun reconcile_checksMissedAlerts_andSchedulesFutureDeadlineAlarms() = runTest {
        val now = System.currentTimeMillis()
        val taskWithFutureDeadline = TodoEntity(
            id = 1L,
            title = "Task with future deadline",
            status = "OPEN",
            deadlineUtc = now + 100_000L
        )
        val taskWithPastDeadline = TodoEntity(
            id = 2L,
            title = "Task with past deadline",
            status = "OPEN",
            deadlineUtc = now - 100_000L
        )
        val taskWithoutDeadline = TodoEntity(
            id = 3L,
            title = "Task without deadline",
            status = "OPEN",
            deadlineUtc = null
        )

        coEvery { todoDao.getOpenTodosList() } returns listOf(
            taskWithFutureDeadline,
            taskWithPastDeadline,
            taskWithoutDeadline
        )

        val result = reconciler.reconcile()

        assertEquals(3, result.openTasksCount)
        assertEquals(1, result.scheduledAlarmsCount)
        assertEquals(0, result.locationTasksCount)

        coVerify { missedAlertsManager.checkAndNotifyMissedAlerts(any()) }
        coVerify { deadlineAlarmScheduler.cancelAlarms(1L) }
        coVerify { deadlineAlarmScheduler.scheduleAlarms(match { it.id == 1L }) }
        coVerify(exactly = 0) { deadlineAlarmScheduler.scheduleAlarms(match { it.id == 2L || it.id == 3L }) }
        coVerify { todoRepository.logEvent("RECONCILE_SUCCESS", any()) }
        assertTrue(reconciler.getLastReconciledTime() > 0L)
    }
}
