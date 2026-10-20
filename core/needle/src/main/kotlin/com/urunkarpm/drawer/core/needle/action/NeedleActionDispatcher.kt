package com.urunkarpm.drawer.core.needle.action

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeedleActionDispatcher @Inject constructor() {
    fun dispatch(call: ToolCallResult): LauncherAction? {
        return when (call.toolName) {
            "set_timer" -> {
                val seconds = call.arguments["seconds"]?.jsonPrimitive?.intOrNull ?: return null
                val label = call.arguments["label"]?.jsonPrimitive?.content
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    if (!label.isNullOrBlank()) {
                        putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    }
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                val durationText = if (seconds >= 60) "${seconds / 60} min" else "${seconds}s"
                LauncherAction(
                    id = "timer_${System.currentTimeMillis()}",
                    title = "Set timer for $durationText",
                    subtitle = label ?: "Countdown timer",
                    intent = intent,
                    isDirectExecutionSafe = true
                )
            }
            "set_alarm" -> {
                val hour = call.arguments["hour"]?.jsonPrimitive?.intOrNull ?: return null
                val minute = call.arguments["minute"]?.jsonPrimitive?.intOrNull ?: return null
                val message = call.arguments["message"]?.jsonPrimitive?.content
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    if (!message.isNullOrBlank()) {
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                    }
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                val timeFormat = String.format("%02d:%02d", hour, minute)
                LauncherAction(
                    id = "alarm_${System.currentTimeMillis()}",
                    title = "Set alarm for $timeFormat",
                    subtitle = message ?: "Alarm",
                    intent = intent,
                    isDirectExecutionSafe = true
                )
            }
            "toggle_setting" -> {
                val setting = call.arguments["setting"]?.jsonPrimitive?.content?.uppercase() ?: return null
                when (setting) {
                    "BLUETOOTH" -> LauncherAction(
                        id = "bluetooth_${System.currentTimeMillis()}",
                        title = "Open Bluetooth Settings",
                        subtitle = "System Settings",
                        intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                        isDirectExecutionSafe = true
                    )
                    "WIFI" -> LauncherAction(
                        id = "wifi_${System.currentTimeMillis()}",
                        title = "Open Wi-Fi Settings",
                        subtitle = "System Settings",
                        intent = Intent(Settings.ACTION_WIFI_SETTINGS),
                        isDirectExecutionSafe = true
                    )
                    "HOTSPOT" -> LauncherAction(
                        id = "hotspot_${System.currentTimeMillis()}",
                        title = "Open Hotspot Settings",
                        subtitle = "System Settings",
                        intent = Intent(Settings.ACTION_WIRELESS_SETTINGS),
                        isDirectExecutionSafe = true
                    )
                    "SOUND", "VOLUME" -> LauncherAction(
                        id = "sound_${System.currentTimeMillis()}",
                        title = "Open Sound Settings",
                        subtitle = "System Settings",
                        intent = Intent(Settings.ACTION_SOUND_SETTINGS),
                        isDirectExecutionSafe = true
                    )
                    "DISPLAY", "BRIGHTNESS" -> LauncherAction(
                        id = "display_${System.currentTimeMillis()}",
                        title = "Open Display Settings",
                        subtitle = "System Settings",
                        intent = Intent(Settings.ACTION_DISPLAY_SETTINGS),
                        isDirectExecutionSafe = true
                    )
                    "BATTERY" -> LauncherAction(
                        id = "battery_${System.currentTimeMillis()}",
                        title = "Open Battery Settings",
                        subtitle = "System Settings",
                        intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
                        isDirectExecutionSafe = true
                    )
                    else -> null
                }
            }
            "open_settings" -> LauncherAction(
                id = "settings_${System.currentTimeMillis()}",
                title = "Open Settings",
                subtitle = "System Settings",
                intent = Intent(Settings.ACTION_SETTINGS),
                isDirectExecutionSafe = true
            )
            "open_camera" -> LauncherAction(
                id = "camera_${System.currentTimeMillis()}",
                title = "Open Camera",
                subtitle = "Camera",
                intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                isDirectExecutionSafe = true
            )
            else -> null
        }
    }
}
