package com.urunkarpm.drawer.core.needle.action

import android.content.Intent
import android.provider.AlarmClock
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
            else -> null
        }
    }
}
