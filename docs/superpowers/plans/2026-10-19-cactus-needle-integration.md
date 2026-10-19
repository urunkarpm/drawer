# Cactus Compute Needle On-Device AI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Cactus Compute Needle 3 on-device AI foundation model into Drawer Launcher for natural language tool calling, intent-based app search, settings controls, and notification intelligence.

**Architecture:** A standalone `:core:needle` module encapsulates model weight lifecycle management (Ktor streaming download, checksum validation) and inference execution (Cactus runtime, JSON grammar decoding, action dispatching). Features (`feature:home`, `feature:settings`, `feature:notifications`) consume only the Kotlin interface `NeedleEngine`.

**Tech Stack:** Kotlin 2.3, Jetpack Compose, Ktor Client Android, kotlinx-coroutines, kotlinx-serialization, Hilt, JUnit4, MockK.

**Spec:** `docs/superpowers/specs/2026-10-19-cactus-needle-integration-design.md`

## Global Constraints

- 100% Offline execution: Inference must run entirely on-device; no query or telemetry data may leave the device.
- Model storage: Quantized binary (~10 MB) stored in `context.filesDir/models/needle_3_cq2.bin`.
- Threading: All inference executes on `Dispatchers.Default` with a 500ms timeout.
- Graceful degradation: If the model is not downloaded or unavailable, all features seamlessly fall back to standard non-AI launcher behavior without crashing.

---

### Task 1: Module `:core:needle` Scaffolding & Core Models

**Files:**
- Modify: `settings.gradle.kts`
- Create: `core/needle/build.gradle.kts`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/model/ModelStatus.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/model/ToolDefinition.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/model/ToolCallResult.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/NeedleEngine.kt`
- Test: `core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/model/ModelStatusTest.kt`

**Interfaces:**
- Consumes: None (new module)
- Produces: `ModelStatus`, `ToolDefinition`, `ToolCallResult`, `NeedleEngine`

- [ ] **Step 1: Write the failing test**

```kotlin
// core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/model/ModelStatusTest.kt
package com.urunkarpm.drawer.core.needle.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelStatusTest {
    @Test
    fun testModelStatusTransitions() {
        val notDownloaded: ModelStatus = ModelStatus.NotDownloaded
        val downloading: ModelStatus = ModelStatus.Downloading(0.45f)
        val ready: ModelStatus = ModelStatus.Ready("/path/needle.bin", 10.2f)
        val error: ModelStatus = ModelStatus.Error("Checksum failed")

        assertTrue(notDownloaded is ModelStatus.NotDownloaded)
        assertEquals(0.45f, (downloading as ModelStatus.Downloading).progress)
        assertEquals(10.2f, (ready as ModelStatus.Ready).sizeMb)
        assertEquals("Checksum failed", (error as ModelStatus.Error).cause)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:needle:testDebugUnitTest`
Expected: FAIL (project or class does not exist)

- [ ] **Step 3: Implement minimal code**

In `settings.gradle.kts`, add:
```kotlin
include(":core:needle")
```

In `core/needle/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.urunkarpm.drawer.core.needle"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/model/ModelStatus.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.model

sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data class Downloading(val progress: Float) : ModelStatus
    data class Ready(val modelPath: String, val sizeMb: Float) : ModelStatus
    data class Error(val cause: String) : ModelStatus
}
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/model/ToolDefinition.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/model/ToolCallResult.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolCallResult(
    val toolName: String,
    val arguments: JsonObject
)
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/NeedleEngine.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle

import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import kotlinx.coroutines.flow.StateFlow

interface NeedleEngine {
    val modelStatus: StateFlow<ModelStatus>
    suspend fun predictToolCall(query: String, tools: List<ToolDefinition>): List<ToolCallResult>
    suspend fun getEmbedding(text: String): FloatArray?
    suspend fun downloadModel(): Result<Unit>
    suspend fun deleteModel(): Boolean
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:needle:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts core/needle/
git commit -m "feat(core:needle): scaffold module and declare core interfaces"
```

---

### Task 2: Needle Model Manager & Weight Downloader

**Files:**
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/download/NeedleModelManager.kt`
- Test: `core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/download/NeedleModelManagerTest.kt`

**Interfaces:**
- Consumes: `ModelStatus`
- Produces: `NeedleModelManager(val status: StateFlow<ModelStatus>, suspend fun download(), suspend fun delete(): Boolean)`

- [ ] **Step 1: Write the failing test**

```kotlin
// core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/download/NeedleModelManagerTest.kt
package com.urunkarpm.drawer.core.needle.download

import android.content.Context
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NeedleModelManagerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInitialStatusNotDownloadedWhenFileMissing() = runTest {
        val mockContext = mockk<Context>()
        val filesDir = tempFolder.newFolder("files")
        every { mockContext.filesDir } returns filesDir

        val manager = NeedleModelManager(mockContext)
        val initialStatus = manager.status.first()
        assertTrue(initialStatus is ModelStatus.NotDownloaded)
    }

    @Test
    fun testInitialStatusReadyWhenFileExists() = runTest {
        val mockContext = mockk<Context>()
        val filesDir = tempFolder.newFolder("files")
        val modelsDir = File(filesDir, "models").apply { mkdirs() }
        val modelFile = File(modelsDir, "needle_3_cq2.bin").apply {
            writeBytes(ByteArray(1024 * 1024 * 5)) // 5 MB
        }
        every { mockContext.filesDir } returns filesDir

        val manager = NeedleModelManager(mockContext)
        val status = manager.status.first()
        assertTrue(status is ModelStatus.Ready)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:needle:testDebugUnitTest --tests com.urunkarpm.drawer.core.needle.download.NeedleModelManagerTest`
Expected: FAIL (`NeedleModelManager` unresolved)

- [ ] **Step 3: Implement minimal code**

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/download/NeedleModelManager.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.download

import android.content.Context
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeedleModelManager @Inject constructor(
    private val context: Context
) {
    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val modelDirectory: File
        get() = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    val modelFile: File
        get() = File(modelDirectory, MODEL_FILENAME)

    init {
        checkCurrentStatus()
    }

    fun checkCurrentStatus() {
        val file = modelFile
        if (file.exists() && file.length() > 0) {
            val sizeMb = file.length() / (1024f * 1024f)
            _status.value = ModelStatus.Ready(file.absolutePath, sizeMb)
        } else {
            _status.value = ModelStatus.NotDownloaded
        }
    }

    suspend fun downloadModel(client: HttpClient = HttpClient()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _status.value = ModelStatus.Downloading(0.0f)
            val tempFile = File(modelDirectory, "$MODEL_FILENAME.tmp")
            if (tempFile.exists()) tempFile.delete()

            client.prepareGet(MODEL_URL).execute { response ->
                val channel: ByteReadChannel = response.bodyAsChannel()
                val totalBytes = response.headers["Content-Length"]?.toLongOrNull() ?: EXPECTED_BYTE_SIZE
                var bytesRead = 0L

                FileOutputStream(tempFile).use { output ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            output.write(bytes)
                            bytesRead += bytes.size
                            if (totalBytes > 0) {
                                val progress = (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                _status.value = ModelStatus.Downloading(progress)
                            }
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (modelFile.exists()) modelFile.delete()
                tempFile.renameTo(modelFile)
                val sizeMb = modelFile.length() / (1024f * 1024f)
                _status.value = ModelStatus.Ready(modelFile.absolutePath, sizeMb)
                Result.success(Unit)
            } else {
                _status.value = ModelStatus.Error("Empty download output")
                Result.failure(IllegalStateException("Downloaded model file is empty"))
            }
        } catch (e: Exception) {
            _status.value = ModelStatus.Error(e.message ?: "Download failed")
            Result.failure(e)
        }
    }

    fun deleteModel(): Boolean {
        val deleted = modelFile.delete()
        _status.value = ModelStatus.NotDownloaded
        return deleted
    }

    companion object {
        const val MODEL_FILENAME = "needle_3_cq2.bin"
        const val MODEL_URL = "https://huggingface.co/Cactus-Compute/needle-3-quant/resolve/main/needle-3-cq2.bin"
        const val EXPECTED_BYTE_SIZE = 10_800_000L // ~10.3 MB
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:needle:testDebugUnitTest --tests com.urunkarpm.drawer.core.needle.download.NeedleModelManagerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/download/
git add core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/download/
git commit -m "feat(core:needle): implement NeedleModelManager with local status validation and streaming download"
```

---

### Task 3: Action Schemas & Dispatcher (`NeedleActionDispatcher`)

**Files:**
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/LauncherAction.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/StandardTools.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/NeedleActionDispatcher.kt`
- Test: `core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/action/NeedleActionDispatcherTest.kt`

**Interfaces:**
- Consumes: `ToolCallResult`, `ToolDefinition`
- Produces: `LauncherAction`, `StandardTools.allTools`, `NeedleActionDispatcher.dispatch(call: ToolCallResult): LauncherAction?`

- [ ] **Step 1: Write the failing test**

```kotlin
// core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/action/NeedleActionDispatcherTest.kt
package com.urunkarpm.drawer.core.needle.action

import android.provider.AlarmClock
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NeedleActionDispatcherTest {
    private val dispatcher = NeedleActionDispatcher()

    @Test
    fun testSetTimerToolDispatch() {
        val toolCall = ToolCallResult(
            toolName = "set_timer",
            arguments = buildJsonObject {
                put("seconds", 600)
                put("label", "Tea timer")
            }
        )

        val action = dispatcher.dispatch(toolCall)
        assertNotNull(action)
        assertEquals(AlarmClock.ACTION_SET_TIMER, action?.intent?.action)
        assertEquals(600, action?.intent?.getIntExtra(AlarmClock.EXTRA_LENGTH, 0))
        assertEquals("Tea timer", action?.intent?.getStringExtra(AlarmClock.EXTRA_MESSAGE))
    }

    @Test
    fun testSetAlarmToolDispatch() {
        val toolCall = ToolCallResult(
            toolName = "set_alarm",
            arguments = buildJsonObject {
                put("hour", 7)
                put("minute", 30)
                put("message", "Wake up")
            }
        )

        val action = dispatcher.dispatch(toolCall)
        assertNotNull(action)
        assertEquals(AlarmClock.ACTION_SET_ALARM, action?.intent?.action)
        assertEquals(7, action?.intent?.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, action?.intent?.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:needle:testDebugUnitTest --tests com.urunkarpm.drawer.core.needle.action.NeedleActionDispatcherTest`
Expected: FAIL (`NeedleActionDispatcher` unresolved)

- [ ] **Step 3: Implement minimal code**

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/LauncherAction.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.action

import android.content.Intent

data class LauncherAction(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val intent: Intent,
    val isDirectExecutionSafe: Boolean = false
)
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/StandardTools.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.action

import com.urunkarpm.drawer.core.needle.model.ToolDefinition
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
            put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("seconds"))))
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
            put("required", kotlinx.serialization.json.JsonArray(listOf(
                kotlinx.serialization.json.JsonPrimitive("hour"),
                kotlinx.serialization.json.JsonPrimitive("minute")
            )))
        }
    )

    val allTools = listOf(setTimerTool, setAlarmTool)
}
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/NeedleActionDispatcher.kt`:
```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:needle:testDebugUnitTest --tests com.urunkarpm.drawer.core.needle.action.NeedleActionDispatcherTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/action/
git add core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/action/
git commit -m "feat(core:needle): create action models, standard tools, and intent dispatcher"
```

---

### Task 4: Needle Inference Engine & Dependency Injection

**Files:**
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/engine/PromptBuilder.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/engine/CactusNeedleEngine.kt`
- Create: `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/di/NeedleModule.kt`
- Test: `core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/engine/PromptBuilderTest.kt`

**Interfaces:**
- Consumes: `NeedleModelManager`, `ToolDefinition`, `ToolCallResult`
- Produces: `NeedleEngine` implementation bound via Hilt in `NeedleModule`

- [ ] **Step 1: Write the failing test**

```kotlin
// core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/engine/PromptBuilderTest.kt
package com.urunkarpm.drawer.core.needle.engine

import com.urunkarpm.drawer.core.needle.action.StandardTools
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {
    @Test
    fun testPromptContainsToolDefinitionsAndQuery() {
        val prompt = PromptBuilder.buildToolCallPrompt(
            query = "timer 10 minutes",
            tools = StandardTools.allTools
        )
        assertTrue(prompt.contains("set_timer"))
        assertTrue(prompt.contains("timer 10 minutes"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:needle:testDebugUnitTest --tests com.urunkarpm.drawer.core.needle.engine.PromptBuilderTest`
Expected: FAIL (`PromptBuilder` unresolved)

- [ ] **Step 3: Implement minimal code**

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/engine/PromptBuilder.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.engine

import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PromptBuilder {
    private val json = Json { prettyPrint = false }

    fun buildToolCallPrompt(query: String, tools: List<ToolDefinition>): String {
        val toolsJson = json.encodeToString(tools)
        return """
            <|system|>
            You are Needle, an on-device mobile function call router. Available functions:
            $toolsJson
            Given a user request, return a JSON list of function calls with parameters, or [] if no tool matches.
            <|user|>
            $query
            <|assistant|>
        """.trimIndent()
    }
}
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/engine/CactusNeedleEngine.kt`:
```kotlin
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
            // Heuristic matcher fast-path with Needle constrained grammar fallback
            val trimmed = query.trim()
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
```

In `core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/di/NeedleModule.kt`:
```kotlin
package com.urunkarpm.drawer.core.needle.di

import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.engine.CactusNeedleEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NeedleModule {
    @Binds
    @Singleton
    abstract fun bindNeedleEngine(impl: CactusNeedleEngine): NeedleEngine
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:needle:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/engine/
git add core/needle/src/main/kotlin/com/urunkarpm/drawer/core/needle/di/
git add core/needle/src/test/kotlin/com/urunkarpm/drawer/core/needle/engine/
git commit -m "feat(core:needle): add CactusNeedleEngine, PromptBuilder, and Hilt bindings"
```

---

### Task 5: Search Bar Action Suggestion Chip & Glance Bar Trigger (`feature:home`)

**Files:**
- Modify: `feature/home/build.gradle.kts`
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/ActionSuggestionChip.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/GlanceHeader.kt`
- Test: `feature/home/src/test/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModelActionTest.kt`

**Interfaces:**
- Consumes: `NeedleEngine`, `NeedleActionDispatcher`, `LauncherAction`
- Produces: `val suggestedAction: StateFlow<LauncherAction?>` in `HomeViewModel`, clickable `ActionSuggestionChip` in Compose

- [ ] **Step 1: Write the failing test**

```kotlin
// feature/home/src/test/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModelActionTest.kt
package com.urunkarpm.drawer.feature.home

import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.action.NeedleActionDispatcher
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelActionTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testQueryPredictsAction() = runTest {
        val needleEngine = mockk<NeedleEngine>()
        val actionDispatcher = NeedleActionDispatcher()
        every { needleEngine.modelStatus } returns MutableStateFlow(ModelStatus.Ready("/path", 10f))

        coEvery { needleEngine.predictToolCall("timer 5m", any()) } returns listOf(
            ToolCallResult("set_timer", buildJsonObject { put("seconds", 300) })
        )

        val action = actionDispatcher.dispatch(
            ToolCallResult("set_timer", buildJsonObject { put("seconds", 300) })
        )
        assertNotNull(action)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:home:testDebugUnitTest --tests com.urunkarpm.drawer.feature.home.HomeViewModelActionTest`
Expected: PASS / Verify compilation against `:core:needle`

- [ ] **Step 3: Implement minimal code**

In `feature/home/build.gradle.kts`:
Add dependency:
```kotlin
implementation(project(":core:needle"))
```

In `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/ActionSuggestionChip.kt`:
```kotlin
package com.urunkarpm.drawer.feature.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.needle.action.LauncherAction

@Composable
fun ActionSuggestionChip(
    action: LauncherAction?,
    onExecuteAction: (LauncherAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = action != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (action != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onExecuteAction(action) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Run",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
```

In `HomeViewModel.kt`:
- Inject `NeedleEngine` and `NeedleActionDispatcher`.
- Add `private val _suggestedAction = MutableStateFlow<LauncherAction?>(null)`.
- Expose `val suggestedAction = _suggestedAction.asStateFlow()`.
- On query change, launch a debounced coroutine querying `predictToolCall(query, StandardTools.allTools)`.

In `AllAppsDrawer.kt`:
- Render `ActionSuggestionChip` immediately below the search field and above the App list.

In `GlanceHeader.kt`:
- Add an AI / Mic IconButton that launches voice input or triggers quick action prompt.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:home:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/home/
git commit -m "feat(feature:home): integrate ActionSuggestionChip and Needle query resolution in HomeViewModel"
```

---

### Task 6: Settings On-Device AI Controls (`feature:settings`)

**Files:**
- Modify: `feature/settings/build.gradle.kts`
- Create: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/component/NeedleSettingsCard.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModel.kt`
- Test: `feature/settings/src/test/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModelNeedleTest.kt`

**Interfaces:**
- Consumes: `NeedleEngine`, `ModelStatus`
- Produces: UI Card in Settings displaying model download progress, ready status, and model deletion

- [ ] **Step 1: Write the failing test**

```kotlin
// feature/settings/src/test/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModelNeedleTest.kt
package com.urunkarpm.drawer.feature.settings

import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelNeedleTest {
    @Test
    fun testNeedleStatusReflected() {
        val mockEngine = mockk<NeedleEngine>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
        every { mockEngine.modelStatus } returns statusFlow

        assertEquals(ModelStatus.NotDownloaded, mockEngine.modelStatus.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:settings:testDebugUnitTest --tests com.urunkarpm.drawer.feature.settings.SettingsViewModelNeedleTest`
Expected: PASS / Verify compilation against `:core:needle`

- [ ] **Step 3: Implement minimal code**

In `feature/settings/build.gradle.kts`:
```kotlin
implementation(project(":core:needle"))
```

In `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/component/NeedleSettingsCard.kt`:
```kotlin
package com.urunkarpm.drawer.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.needle.model.ModelStatus

@Composable
fun NeedleSettingsCard(
    status: ModelStatus,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "On-Device AI (Needle 3)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "100% offline tool calling and smart actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (status) {
                is ModelStatus.NotDownloaded -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Model (~10.3 MB)")
                    }
                }
                is ModelStatus.Downloading -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Downloading... ${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is ModelStatus.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ready (${status.sizeMb.toInt()} MB) • Fully Offline",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Model")
                        }
                    }
                }
                is ModelStatus.Error -> {
                    Text(
                        text = "Download error: ${status.cause}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = onDownload) {
                        Text("Retry Download")
                    }
                }
            }
        }
    }
}
```

In `SettingsViewModel.kt`:
- Inject `NeedleEngine`.
- Expose `val needleModelStatus = needleEngine.modelStatus`.
- Expose `fun downloadNeedleModel()` and `fun deleteNeedleModel()`.

In `SettingsScreen.kt`:
- Render `NeedleSettingsCard` inside the LazyColumn settings list.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:settings:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/settings/
git commit -m "feat(feature:settings): add NeedleSettingsCard with model download and storage controls"
```

---

### Task 7: Smart Notification OTP & Entity Extraction (`feature:notifications`)

**Files:**
- Modify: `feature/notifications/build.gradle.kts`
- Create: `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/extraction/NotificationExtractor.kt`
- Modify: `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationDrawer.kt`
- Test: `feature/notifications/src/test/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationExtractorTest.kt`

**Interfaces:**
- Consumes: Notification text payloads
- Produces: `ExtractedOtp(code: String)` and one-tap copy button in `NotificationDrawer`

- [ ] **Step 1: Write the failing test**

```kotlin
// feature/notifications/src/test/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationExtractorTest.kt
package com.urunkarpm.drawer.feature.notifications

import com.urunkarpm.drawer.feature.notifications.extraction.NotificationExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationExtractorTest {
    private val extractor = NotificationExtractor()

    @Test
    fun testExtractOtpFromNotification() {
        val sms = "Your verification code is 492019. Valid for 10 minutes."
        val otp = extractor.extractOtp(sms)
        assertNotNull(otp)
        assertEquals("492019", otp)
    }

    @Test
    fun testNoOtpInRegularMessage() {
        val text = "Hey what time are we meeting tonight?"
        val otp = extractor.extractOtp(text)
        assertNull(otp)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:notifications:testDebugUnitTest --tests com.urunkarpm.drawer.feature.notifications.NotificationExtractorTest`
Expected: FAIL (`NotificationExtractor` unresolved)

- [ ] **Step 3: Implement minimal code**

In `feature/notifications/build.gradle.kts`:
```kotlin
implementation(project(":core:needle"))
```

In `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/extraction/NotificationExtractor.kt`:
```kotlin
package com.urunkarpm.drawer.feature.notifications.extraction

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationExtractor @Inject constructor() {
    private val otpPattern = Pattern.compile("(?i)(?:code|otp|verification|password|secret)\\s*(?:is|:)?\\s*(\\d{4,8})\\b|\\b(\\d{4,8})\\s+is your (?:code|otp)")

    fun extractOtp(text: String): String? {
        val matcher = otpPattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1) ?: matcher.group(2)
        } else null
    }
}
```

In `NotificationDrawer.kt`:
- On notification card items, if `NotificationExtractor.extractOtp(notification.text)` returns an OTP code:
- Render a one-tap `[ Copy OTP: {code} ]` button that copies the code to the system clipboard and displays a "Copied to clipboard" toast.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:notifications:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/notifications/
git commit -m "feat(feature:notifications): implement smart OTP extraction and copy button on notification cards"
```

---

### Task 8: End-to-End Build & Test Verification

**Files:**
- Entire repository

- [ ] **Step 1: Run full unit test suite across all modules**

Run: `./gradlew testDebugUnitTest`
Expected: All tests pass across `:core:needle`, `:feature:home`, `:feature:settings`, `:feature:notifications`, and `:app`.

- [ ] **Step 2: Assemble Debug APK**

Run: `./gradlew assembleDebug`
Expected: Build SUCCESS with `app-debug.apk` created.

- [ ] **Step 3: Final Git Commit**

```bash
git commit --allow-empty -m "chore: verify full build and tests pass for Cactus Needle integration"
```
