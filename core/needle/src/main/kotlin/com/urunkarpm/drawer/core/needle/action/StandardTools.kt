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

    val toggleSettingTool = ToolDefinition(
        name = "toggle_setting",
        description = "Opens settings or toggles device setting: bluetooth, wifi, hotspot, sound, display, battery",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("setting") {
                    put("type", "string")
                    put("description", "Setting to toggle: BLUETOOTH, WIFI, HOTSPOT, SOUND, DISPLAY, BATTERY")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("setting"))))
        }
    )

    val openSettingsTool = ToolDefinition(
        name = "open_settings",
        description = "Opens system settings",
        parameters = buildJsonObject {
            put("type", "object")
        }
    )

    val openCameraTool = ToolDefinition(
        name = "open_camera",
        description = "Opens device camera",
        parameters = buildJsonObject {
            put("type", "object")
        }
    )

    val addAppToDrawerTool = ToolDefinition(
        name = "add_app_to_drawer",
        description = "Adds or puts an app into a drawer/group",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("app") {
                    put("type", "string")
                    put("description", "Name of the app to add")
                }
                putJsonObject("drawer") {
                    put("type", "string")
                    put("description", "Target drawer name")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("app"), JsonPrimitive("drawer"))))
        }
    )

    val removeAppFromDrawerTool = ToolDefinition(
        name = "remove_app_from_drawer",
        description = "Removes an app from a drawer or group",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("app") {
                    put("type", "string")
                    put("description", "Name of the app to remove")
                }
                putJsonObject("drawer") {
                    put("type", "string")
                    put("description", "Optional drawer name to remove from")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("app"))))
        }
    )

    val moveAppBetweenDrawersTool = ToolDefinition(
        name = "move_app_between_drawers",
        description = "Moves an app from one drawer to another",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("app") {
                    put("type", "string")
                    put("description", "Name of the app to move")
                }
                putJsonObject("from_drawer") {
                    put("type", "string")
                    put("description", "Source drawer name")
                }
                putJsonObject("to_drawer") {
                    put("type", "string")
                    put("description", "Destination drawer name")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("app"), JsonPrimitive("from_drawer"), JsonPrimitive("to_drawer"))))
        }
    )

    val addAppToDockTool = ToolDefinition(
        name = "add_app_to_dock",
        description = "Pins or adds an app to the home dock",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("app") {
                    put("type", "string")
                    put("description", "Name of the app to pin to dock")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("app"))))
        }
    )

    val removeAppFromDockTool = ToolDefinition(
        name = "remove_app_from_dock",
        description = "Removes or unpins an app from the home dock",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("app") {
                    put("type", "string")
                    put("description", "Name of the app to remove from dock")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("app"))))
        }
    )

    val createDrawerTool = ToolDefinition(
        name = "create_drawer",
        description = "Creates a new drawer or folder to organize apps",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("drawer") {
                    put("type", "string")
                    put("description", "Name of the drawer or folder to create")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("drawer"))))
        }
    )

    val deleteDrawerTool = ToolDefinition(
        name = "delete_drawer",
        description = "Deletes an existing drawer or folder",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("drawer") {
                    put("type", "string")
                    put("description", "Name of the drawer or folder to delete")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("drawer"))))
        }
    )

    val renameDrawerTool = ToolDefinition(
        name = "rename_drawer",
        description = "Renames a drawer or folder",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("from_name") {
                    put("type", "string")
                    put("description", "Current name of the drawer or folder")
                }
                putJsonObject("to_name") {
                    put("type", "string")
                    put("description", "New name for the drawer or folder")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("from_name"), JsonPrimitive("to_name"))))
        }
    )

    val calculateTool = ToolDefinition(
        name = "calculate",
        description = "Evaluates mathematical expressions, percentages, or formulas",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("expression") {
                    put("type", "string")
                    put("description", "Mathematical expression to evaluate")
                }
                putJsonObject("result") {
                    put("type", "string")
                    put("description", "Evaluated result")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("expression"))))
        }
    )

    val changeAppSettingTool = ToolDefinition(
        name = "change_app_setting",
        description = "Changes a launcher setting directly such as stealth theme, dynamic wallpaper, lock layout, status bar, duo status, side-by-side drawers, AI toggle, icon shape, drawer icons, weather, or theme mode",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("setting") {
                    put("type", "string")
                    put("description", "Setting key: STEALTH_THEME, DYNAMIC_WALLPAPER, LOCK_LAYOUT, HIDE_STATUS_BAR, DUO_STATUS, TWO_DRAWERS, HIDE_DRAWER_ICONS, CAMERA_MIRROR, THEME_MODE, ON_DEVICE_AI, 24_HOUR, WEATHER, ICON_SHAPE")
                }
                putJsonObject("value") {
                    put("type", "string")
                    put("description", "New value: true, false, toggle, or specific string")
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("setting"))))
        }
    )

    val setBrightnessTool = ToolDefinition(
        name = "set_brightness",
        description = "Changes or adjusts screen brightness directly",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("percent") {
                    put("type", "integer")
                    put("description", "Target brightness level 0 to 100")
                }
                putJsonObject("action") {
                    put("type", "string")
                    put("description", "increase, decrease, max, min, or set")
                }
            }
        }
    )

    val setVolumeTool = ToolDefinition(
        name = "set_volume",
        description = "Adjusts media, ring, alarm or notification volume, or mutes sound directly",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("stream") {
                    put("type", "string")
                    put("description", "MEDIA, RING, NOTIFICATION, ALARM, ALL")
                }
                putJsonObject("percent") {
                    put("type", "integer")
                    put("description", "Volume percentage 0 to 100")
                }
                putJsonObject("action") {
                    put("type", "string")
                    put("description", "mute, unmute, increase, decrease, or set")
                }
            }
        }
    )

    val setDnsTool = ToolDefinition(
        name = "set_dns",
        description = "Sets or changes Custom / Private DNS provider",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("host") {
                    put("type", "string")
                    put("description", "Hostname of the DNS provider, e.g. dns.adguard.com, one.one.one.one, dns.google")
                }
                putJsonObject("mode") {
                    put("type", "string")
                    put("description", "hostname, off, or opportunistic")
                }
            }
        }
    )

    val allTools = listOf(
        setTimerTool,
        setAlarmTool,
        toggleSettingTool,
        openSettingsTool,
        openCameraTool,
        addAppToDrawerTool,
        removeAppFromDrawerTool,
        moveAppBetweenDrawersTool,
        addAppToDockTool,
        removeAppFromDockTool,
        createDrawerTool,
        deleteDrawerTool,
        renameDrawerTool,
        calculateTool,
        changeAppSettingTool,
        setBrightnessTool,
        setVolumeTool,
        setDnsTool
    )
}
