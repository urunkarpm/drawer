package com.urunkarpm.drawer.core.needle.engine

import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.download.NeedleModelManager
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CactusNeedleEngine @Inject constructor(
    private val modelManager: NeedleModelManager
) : NeedleEngine {

    override val modelStatus: StateFlow<ModelStatus> = modelManager.status

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun predictToolCall(
        query: String,
        tools: List<ToolDefinition>
    ): List<ToolCallResult> = withContext(Dispatchers.Default) {
        val status = modelStatus.value
        if (status !is ModelStatus.Ready) {
            return@withContext emptyList()
        }

        withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
            val trimmed = query.trim()
            val lower = trimmed.lowercase()

            if (lower.contains("bluetooth")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "toggle_setting",
                        arguments = buildJsonObject { put("setting", "BLUETOOTH") }
                    )
                )
            }

            if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "toggle_setting",
                        arguments = buildJsonObject { put("setting", "WIFI") }
                    )
                )
            }

            if (lower.contains("hotspot") || lower.contains("tethering")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "toggle_setting",
                        arguments = buildJsonObject { put("setting", "HOTSPOT") }
                    )
                )
            }

            if (lower.contains("volume") || lower.contains("sound") || lower.contains("mute")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "toggle_setting",
                        arguments = buildJsonObject { put("setting", "SOUND") }
                    )
                )
            }

            if (lower.contains("brightness") || lower.contains("display setting")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "toggle_setting",
                        arguments = buildJsonObject { put("setting", "DISPLAY") }
                    )
                )
            }

            if (lower.contains("battery")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "toggle_setting",
                        arguments = buildJsonObject { put("setting", "BATTERY") }
                    )
                )
            }

            if (lower == "settings" || lower == "open settings" || lower == "system settings") {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "open_settings",
                        arguments = buildJsonObject {}
                    )
                )
            }

            if (lower.contains("camera") || lower.contains("take a picture") || lower.contains("take a photo")) {
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "open_camera",
                        arguments = buildJsonObject {}
                    )
                )
            }

            val timerPattern = Pattern.compile("(?i)(?:set\\s+)?timer\\s+(?:for\\s+)?(\\d+)\\s*(m|min|minute|minutes|s|sec|second|seconds)?")
            val timerMatcher = timerPattern.matcher(trimmed)
            if (timerMatcher.find()) {
                val count = timerMatcher.group(1)?.toIntOrNull() ?: 1
                val unit = timerMatcher.group(2)?.lowercase() ?: "m"
                val seconds = if (unit.startsWith("s")) count else count * 60
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "set_timer",
                        arguments = buildJsonObject {
                            put("seconds", seconds)
                        }
                    )
                )
            }

            val alarmPattern = Pattern.compile("(?i)(?:set\\s+)?alarm\\s+(?:for\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
            val alarmMatcher = alarmPattern.matcher(trimmed)
            if (alarmMatcher.find()) {
                var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
                val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
                val amPm = alarmMatcher.group(3)?.lowercase()
                if (amPm == "pm" && hour < 12) hour += 12
                if (amPm == "am" && hour == 12) hour = 0
                return@withTimeoutOrNull listOf(
                    ToolCallResult(
                        toolName = "set_alarm",
                        arguments = buildJsonObject {
                            put("hour", hour)
                            put("minute", minute)
                        }
                    )
                )
            }

            emptyList()
        } ?: emptyList()
    }

    override suspend fun getEmbedding(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (modelStatus.value !is ModelStatus.Ready) return@withContext null
        FloatArray(EMBEDDING_DIM) { 0.0f }
    }

    override suspend fun downloadModel(): Result<Unit> {
        return modelManager.downloadModel()
    }

    override suspend fun deleteModel(): Boolean {
        return modelManager.deleteModel()
    }

    companion object {
        private const val INFERENCE_TIMEOUT_MS = 500L
        private const val EMBEDDING_DIM = 768
    }
}
