package com.urunkarpm.drawer.feature.home.action

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.urunkarpm.drawer.core.model.LauncherAction
import java.util.Locale
import java.util.regex.Pattern

// ponytail: direct regex router mapping launcher queries into platform system Intents; ceiling: English action keywords; upgrade path: locale-aware intent parsing or Android App Actions / ShortcutsManager.
object LauncherActionRouter {

    private val timerPattern = Pattern.compile("(?i)(?:(?:set|start|create)\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(\\d+)\\s*(m|min|minute|minutes|s|sec|second|seconds)?")
    private val timerSuffixPattern = Pattern.compile("(?i)^(\\d+)\\s*(m|min|minute|minutes|s|sec|second|seconds)\\s+timer$")
    private val alarmPattern = Pattern.compile("(?i)(?:(?:set|create)\\s+(?:an?\\s+)?)?alarm\\s+(?:for\\s+)?(\\d{1,2})(?::|\\s+)?(\\d{2})?\\s*(am|pm)?")

    fun parseAction(query: String): LauncherAction? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null
        val lower = trimmed.lowercase()

        // 1. Timers
        val timerMatch = timerPattern.matcher(trimmed)
        if (timerMatch.find()) {
            val count = timerMatch.group(1)?.toIntOrNull() ?: 1
            val unit = timerMatch.group(2)?.lowercase() ?: "m"
            val seconds = if (unit.startsWith("s")) count else count * 60
            val durationText = if (seconds >= 60) "${seconds / 60} min" else "${seconds}s"
            return LauncherAction(
                id = "timer_${System.currentTimeMillis()}",
                title = "Set timer for $durationText",
                subtitle = "Countdown timer",
                intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                },
                isDirectExecutionSafe = true
            )
        }

        val suffixMatch = timerSuffixPattern.matcher(trimmed)
        if (suffixMatch.find()) {
            val count = suffixMatch.group(1)?.toIntOrNull() ?: 1
            val unit = suffixMatch.group(2)?.lowercase() ?: "m"
            val seconds = if (unit.startsWith("s")) count else count * 60
            val durationText = if (seconds >= 60) "${seconds / 60} min" else "${seconds}s"
            return LauncherAction(
                id = "timer_${System.currentTimeMillis()}",
                title = "Set timer for $durationText",
                subtitle = "Countdown timer",
                intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                },
                isDirectExecutionSafe = true
            )
        }

        // 2. Alarms
        val alarmMatch = alarmPattern.matcher(trimmed)
        if (alarmMatch.find()) {
            var hour = alarmMatch.group(1)?.toIntOrNull() ?: 7
            val minute = alarmMatch.group(2)?.toIntOrNull() ?: 0
            val amPm = alarmMatch.group(3)?.lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            val timeFormat = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
            return LauncherAction(
                id = "alarm_${System.currentTimeMillis()}",
                title = "Set alarm for $timeFormat",
                subtitle = "Alarm",
                intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                },
                isDirectExecutionSafe = true
            )
        }

        // 3. System Settings & Hardware Toggles
        return when {
            lower.contains("bluetooth") -> LauncherAction(
                id = "bluetooth_${System.currentTimeMillis()}",
                title = "Open Bluetooth Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                isDirectExecutionSafe = true
            )
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") -> LauncherAction(
                id = "wifi_${System.currentTimeMillis()}",
                title = "Open Wi-Fi Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_WIFI_SETTINGS),
                isDirectExecutionSafe = true
            )
            lower.contains("hotspot") || lower.contains("tethering") -> LauncherAction(
                id = "hotspot_${System.currentTimeMillis()}",
                title = "Open Hotspot Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_WIRELESS_SETTINGS),
                isDirectExecutionSafe = true
            )
            lower.contains("sound") || lower.contains("volume") -> LauncherAction(
                id = "sound_${System.currentTimeMillis()}",
                title = "Open Sound Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_SOUND_SETTINGS),
                isDirectExecutionSafe = true
            )
            lower.contains("brightness") || lower.contains("display") -> LauncherAction(
                id = "display_${System.currentTimeMillis()}",
                title = "Open Display Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_DISPLAY_SETTINGS),
                isDirectExecutionSafe = true
            )
            lower.contains("battery") -> LauncherAction(
                id = "battery_${System.currentTimeMillis()}",
                title = "Open Battery Settings",
                subtitle = "System Settings",
                intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                isDirectExecutionSafe = true
            )
            lower.contains("airplane mode") || lower == "airplane" -> LauncherAction(
                id = "airplane_${System.currentTimeMillis()}",
                title = "Open Airplane Mode Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS),
                isDirectExecutionSafe = true
            )
            lower.contains("camera") || lower.contains("take photo") -> LauncherAction(
                id = "camera_${System.currentTimeMillis()}",
                title = "Open Camera",
                subtitle = "Camera",
                intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                isDirectExecutionSafe = true
            )
            lower == "settings" || lower == "open settings" -> LauncherAction(
                id = "settings_${System.currentTimeMillis()}",
                title = "Open Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_SETTINGS),
                isDirectExecutionSafe = true
            )
            else -> null
        }
    }
}
