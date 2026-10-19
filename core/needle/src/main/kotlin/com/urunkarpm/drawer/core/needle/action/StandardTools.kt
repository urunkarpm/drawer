package com.urunkarpm.drawer.core.needle.action

import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

object StandardTools {
    val setTimerTool = ToolDefinition(
        name = "set_timer",
        description = "Sets a countdown timer for a specified number of seconds",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("seconds") {
                    put("type", "integer")
                    put("description", "Duration in seconds")
                }
                putJsonObject("label") {
                    put("type", "string")
                    put("description", "Optional label for timer")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("seconds"))))
        }
    )

    val setAlarmTool = ToolDefinition(
        name = "set_alarm",
        description = "Sets an alarm for a specific hour and minute",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("hour") {
                    put("type", "integer")
                    put("description", "Hour in 24-hour format (0-23)")
                }
                putJsonObject("minute") {
                    put("type", "integer")
                    put("description", "Minute (0-59)")
                }
                putJsonObject("message") {
                    put("type", "string")
                    put("description", "Alarm message or title")
                }
            }
            put("required", JsonArray(listOf(
                JsonPrimitive("hour"),
                JsonPrimitive("minute")
            )))
        }
    )

    val allTools = listOf(setTimerTool, setAlarmTool)
}
