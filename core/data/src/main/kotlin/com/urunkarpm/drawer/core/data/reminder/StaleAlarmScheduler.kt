package com.urunkarpm.drawer.core.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.model.TodoSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaleAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val ACTION_DAILY_STALE_CHECK = "com.urunkarpm.drawer.ACTION_DAILY_STALE_CHECK"
        const val ACTION_HIGH_PRIORITY_CHECK = "com.urunkarpm.drawer.ACTION_HIGH_PRIORITY_CHECK"
        const val DAILY_CHECK_REQUEST_CODE = 4001
        const val HIGH_PRIORITY_CHECK_REQUEST_CODE = 4002
    }

    fun scheduleDailyCheck(settings: TodoSettings) {
        if (!settings.staleAlertsEnabled) {
            cancelDailyCheck()
            return
        }

        val am = alarmManager ?: return
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)

        val checkTime = TodoUtils.parseLocalTime(settings.dailyCheckTime, LocalTime.of(9, 0))
        var targetDate = now.toLocalDate()
        var targetZdt = ZonedDateTime.of(targetDate, checkTime, zoneId)

        // If time already passed today, schedule for tomorrow
        if (targetZdt.isBefore(now)) {
            targetDate = targetDate.plusDays(1)
            targetZdt = ZonedDateTime.of(targetDate, checkTime, zoneId)
        }

        // If target falls inside quiet hours, defer to quiet hours end
        val quietStart = TodoUtils.parseLocalTime(settings.quietHoursStart, LocalTime.of(22, 0))
        val quietEnd = TodoUtils.parseLocalTime(settings.quietHoursEnd, LocalTime.of(7, 0))

        val isQuiet = TodoUtils.isInQuietHours(
            now = targetZdt.toInstant().toEpochMilli(),
            startStr = settings.quietHoursStart,
            endStr = settings.quietHoursEnd,
            zoneId = zoneId
        )

        if (isQuiet) {
            targetZdt = ZonedDateTime.of(targetDate, quietEnd, zoneId)
            if (targetZdt.isBefore(now)) {
                targetZdt = ZonedDateTime.of(targetDate.plusDays(1), quietEnd, zoneId)
            }
        }

        val triggerAtMillis = targetZdt.toInstant().toEpochMilli()

        val intent = Intent(context, StaleAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_STALE_CHECK
        }
        val pi = PendingIntent.getBroadcast(
            context,
            DAILY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun scheduleHighPriorityDaytimeCheck(delayHours: Long = 4) {
        val am = alarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (delayHours * 3600 * 1000L)

        val intent = Intent(context, StaleAlarmReceiver::class.java).apply {
            action = ACTION_HIGH_PRIORITY_CHECK
        }
        val pi = PendingIntent.getBroadcast(
            context,
            HIGH_PRIORITY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancelDailyCheck() {
        val am = alarmManager ?: return
        val intent = Intent(context, StaleAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_STALE_CHECK
        }
        val pi = PendingIntent.getBroadcast(
            context,
            DAILY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }
}
