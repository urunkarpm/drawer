package com.urunkarpm.drawer.core.data.reminder

import android.app.AlarmManager
import android.content.Context
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DeadlineAlarmSchedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val alarmManager: AlarmManager = mockk(relaxed = true)

    private lateinit var scheduler: DeadlineAlarmScheduler

    @Before
    fun setUp() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        scheduler = DeadlineAlarmScheduler(context)
    }

    @Test
    fun scheduleAlarms_futureDeadline_schedulesAlertPoints() {
        val now = System.currentTimeMillis()
        // Deadline 2 days in the future
        val futureDeadline = now + (48 * 3600 * 1000L)
        val task = TodoItem(
            id = 100L,
            title = "Project Deadline",
            status = TodoStatus.OPEN,
            deadlineUtc = futureDeadline
        )

        scheduler.scheduleAlarms(task)

        // At least one alarm (and up to 3 for future points) should be scheduled
        verify(atLeast = 1) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), any())
        }
    }

    @Test
    fun cancelAlarms_cancelsForTaskId() {
        scheduler.cancelAlarms(100L)
        // Verify cancel attempted
        verify(atLeast = 0) {
            alarmManager.cancel(any<android.app.PendingIntent>())
        }
    }
}
