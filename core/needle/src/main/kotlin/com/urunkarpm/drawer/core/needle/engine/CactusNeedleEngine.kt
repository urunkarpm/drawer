package com.urunkarpm.drawer.core.needle.engine

import android.util.Log
import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.download.NeedleModelManager
import com.urunkarpm.drawer.core.needle.model.ModelProcessingState
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ProcessingStep
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import com.urunkarpm.drawer.core.needle.util.MathEvaluator
import com.urunkarpm.drawer.core.needle.util.SpokenTextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CactusNeedleEngine @Inject constructor(
    private val modelManager: NeedleModelManager
) : NeedleEngine {

    override val modelStatus: StateFlow<ModelStatus> = modelManager.status

    private val _processingState = MutableStateFlow(ModelProcessingState())
    override val processingState: StateFlow<ModelProcessingState> = _processingState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var lastToolsJsonHash: Int? = null

    override fun updateProcessingSuccess(summary: String) {
        _processingState.value = _processingState.value.copy(
            step = ProcessingStep.Success,
            summary = summary,
            timestampMs = System.currentTimeMillis()
        )
    }

    override fun updateProcessingError(summary: String) {
        _processingState.value = _processingState.value.copy(
            step = ProcessingStep.Error,
            summary = summary,
            timestampMs = System.currentTimeMillis()
        )
    }

    override fun resetProcessingState() {
        _processingState.value = ModelProcessingState()
    }

    override suspend fun predictToolCall(
        query: String,
        tools: List<ToolDefinition>
    ): List<ToolCallResult> = withContext(Dispatchers.Default) {
        val clauses = splitCompoundClauses(query)

        // 1. FAST PATH: Check deterministic pattern matching first across all clauses (< 1ms execution)
        val fastResults = clauses.map { clause -> clause to predictSingleToolCall(clause, tools) }
        val allFastMatched = fastResults.all { it.second.isNotEmpty() }

        if (allFastMatched) {
            val results = fastResults.flatMap { it.second }
            val first = results.first()
            val argsMap = first.arguments.mapValues {
                try {
                    it.value.jsonPrimitive.content
                } catch (_: Throwable) {
                    it.value.toString()
                }
            }
            _processingState.value = ModelProcessingState(
                query = query,
                step = ProcessingStep.Executing,
                modelName = "Instant Intent Router",
                reasoning = "Matched high-confidence deterministic action pattern",
                toolName = first.toolName,
                arguments = argsMap,
                isNativeInference = false,
                timestampMs = System.currentTimeMillis()
            )
            return@withContext results
        }

        // 2. SLOW PATH: Ambiguous/complex natural language queries -> on-device neural model (if downloaded)
        val status = modelStatus.value
        val hasNativeModel = status is ModelStatus.Ready && NeedleNativeBridge.isAvailable()

        _processingState.value = ModelProcessingState(
            query = query,
            step = ProcessingStep.Inferring,
            modelName = if (hasNativeModel) "Needle 3 (CQ2 Laddered SAN · 121M)" else "Needle (Instant Router)",
            isNativeInference = hasNativeModel,
            timestampMs = System.currentTimeMillis()
        )

        val results = withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
            clauses.flatMap { clause ->
                // Check if this specific clause matched fast path
                val fastForClause = predictSingleToolCall(clause, tools)
                if (fastForClause.isNotEmpty()) {
                    fastForClause
                } else if (hasNativeModel && shouldAttemptNativeInference(clause)) {
                    val nativeResults = predictWithNativeModel(clause, tools, status.modelPath)
                    if (nativeResults.isNotEmpty()) {
                        nativeResults
                    } else {
                        predictSingleToolCall(clause, tools)
                    }
                } else {
                    emptyList()
                }
            }
        } ?: emptyList()

        if (results.isEmpty()) {
            _processingState.value = _processingState.value.copy(
                step = ProcessingStep.Executing,
                summary = "Checking launcher actions..."
            )
        }

        results
    }

    private fun shouldAttemptNativeInference(clause: String): Boolean {
        val trimmed = clause.trim()
        if (trimmed.length < 3) return false
        // Single short token with no spaces is typically a search prefix (e.g. typing app name)
        if (!trimmed.contains(' ') && trimmed.length < 5) return false
        return true
    }

    private fun predictWithNativeModel(
        clause: String,
        tools: List<ToolDefinition>,
        modelPath: String
    ): List<ToolCallResult> {
        if (!NeedleNativeBridge.isAvailable()) return emptyList()

        return try {
            val file = File(modelPath)
            if (!file.exists() || file.length() == 0L) {
                return emptyList()
            }

            val loaded = NeedleNativeBridge.loadModel(file.absolutePath)
            if (!loaded) {
                Log.w(TAG, "Native loadModel failed: ${NeedleNativeBridge.getLastError()}")
                return emptyList()
            }

            val toolsJson = serializeTools(tools)
            val toolsHash = toolsJson.hashCode()

            if (lastToolsJsonHash != toolsHash) {
                NeedleNativeBridge.reset()
                val initialized = NeedleNativeBridge.init(
                    systemPrompt = "You are an intelligent home launcher assistant that calls tools accurately.",
                    toolsJson = toolsJson
                )
                if (!initialized) {
                    Log.w(TAG, "Native init failed: ${NeedleNativeBridge.getLastError()}")
                    return emptyList()
                }
                lastToolsJsonHash = toolsHash
            } else {
                NeedleNativeBridge.reset()
            }

            val rawJson = NeedleNativeBridge.complete(clause, maxTokens = 128)
            if (rawJson.isNullOrBlank()) {
                return emptyList()
            }

            parseNeedleResponse(rawJson, clause)
        } catch (e: Throwable) {
            Log.e(TAG, "Exception during native model prediction: ${e.message}", e)
            emptyList()
        }
    }

    private fun serializeTools(tools: List<ToolDefinition>): String {
        val array = buildJsonArray {
            tools.forEach { tool ->
                add(
                    buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("parameters", tool.parameters)
                    }
                )
            }
        }
        return array.toString()
    }

    private fun parseNeedleResponse(rawJson: String, query: String): List<ToolCallResult> {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val functionCalls = root["function_calls"]?.jsonArray ?: return emptyList()

            val reasoning = root["reasoning"]?.jsonPrimitive?.content
            val confidence = root["confidence"]?.jsonPrimitive?.floatOrNull
            val prefillTps = root["prefill_tps"]?.jsonPrimitive?.floatOrNull
            val decodeTps = root["decode_tps"]?.jsonPrimitive?.floatOrNull
            val peakRamMb = root["peak_ram_mb"]?.jsonPrimitive?.floatOrNull

            val results = functionCalls.mapNotNull { element ->
                val callObj = element.jsonObject
                val name = callObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val args = callObj["arguments"]?.jsonObject ?: buildJsonObject {}
                ToolCallResult(toolName = name, arguments = args)
            }

            if (results.isNotEmpty()) {
                val first = results.first()
                val argsMap = first.arguments.mapValues {
                    try {
                        it.value.jsonPrimitive.content
                    } catch (_: Throwable) {
                        it.value.toString()
                    }
                }
                _processingState.value = ModelProcessingState(
                    query = query,
                    step = ProcessingStep.Executing,
                    modelName = "Needle 3 (CQ2 · 121M On-Device)",
                    reasoning = reasoning,
                    confidence = confidence,
                    prefillTps = prefillTps,
                    decodeTps = decodeTps,
                    peakRamMb = peakRamMb,
                    toolName = first.toolName,
                    arguments = argsMap,
                    rawJson = rawJson,
                    isNativeInference = true,
                    timestampMs = System.currentTimeMillis()
                )
            }

            results
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Needle response: ${e.message}")
            emptyList()
        }
    }

    internal fun splitCompoundClauses(query: String): List<String> {
        val trimmed = query.trim()
        val parts = trimmed.split(Regex("(?i)\\s+(?:and|then)\\s+|;\\s*"))
        if (parts.size <= 1) return listOf(trimmed)

        val actionVerbRegex = Regex("(?i)^(?:put|add|move|remove|delete|pin|unpin|open|set|toggle|launch|place|transfer|turn)\\b")
        val isCompound = parts.drop(1).all { part ->
            actionVerbRegex.containsMatchIn(part.trim())
        }
        return if (isCompound) parts.map { it.trim() }.filter { it.isNotBlank() } else listOf(trimmed)
    }

    private fun predictSingleToolCall(
        query: String,
        tools: List<ToolDefinition>
    ): List<ToolCallResult> {
        val trimmed = query.trim()
        val normalized = SpokenTextNormalizer.normalize(trimmed)
        val cleanQuery = normalized.replace(Regex("(?i)^(?:please\\s+|can you\\s+|could you\\s+)"), "")
            .trim()
            .trim('"', '\'', '`', '“', '”')
            .trimEnd('.', '?', '!', ',', ';')
            .trim()
        val lower = cleanQuery.lowercase()

        // 0. Mathematical calculations & formulas ("calculate 25 * 4", "what is 15 + 30", "20% of 150", "sqrt(144)")
        if (MathEvaluator.isMathQuery(cleanQuery)) {
            val eval = MathEvaluator.evaluate(cleanQuery)
            if (eval != null) {
                return listOf(
                    ToolCallResult(
                        toolName = "calculate",
                        arguments = buildJsonObject {
                            put("expression", eval.expression)
                            put("result", eval.result)
                        }
                    )
                )
            }
        }

        if (tools.any { it.name == "categorize_app" }) {
            val category = when {
                lower.contains("finance") || lower.contains("money") || lower.contains("bank") ||
                    lower.contains("wallet") || lower.contains("crypto") || lower.contains("invest") ||
                    lower.contains("pay") -> "finance"
                lower.contains("social") || lower.contains("chat") || lower.contains("messag") ||
                    lower.contains("talk") || lower.contains("friend") -> "social"
                lower.contains("game") || lower.contains("play") || lower.contains("arcade") ||
                    lower.contains("craft") || lower.contains("quest") -> "games"
                lower.contains("media") || lower.contains("music") || lower.contains("video") ||
                    lower.contains("stream") || lower.contains("photo") || lower.contains("audio") ||
                    lower.contains("movie") || lower.contains("radio") || lower.contains("song") -> "media"
                lower.contains("health") || lower.contains("fit") || lower.contains("workout") ||
                    lower.contains("gym") || lower.contains("run") || lower.contains("diet") -> "health"
                lower.contains("shop") || lower.contains("store") || lower.contains("food") ||
                    lower.contains("deliver") || lower.contains("grocer") || lower.contains("order") ||
                    lower.contains("cart") || lower.contains("deal") || lower.contains("market") -> "shopping"
                lower.contains("work") || lower.contains("office") || lower.contains("doc") ||
                    lower.contains("note") || lower.contains("task") || lower.contains("pdf") ||
                    lower.contains("sheet") || lower.contains("slide") || lower.contains("mail") -> "productivity"
                lower.contains("util") || lower.contains("tool") || lower.contains("calc") ||
                    lower.contains("clock") || lower.contains("setting") || lower.contains("file") ||
                    lower.contains("system") || lower.contains("clean") || lower.contains("vpn") -> "utilities"
                else -> "utilities"
            }
            return listOf(
                ToolCallResult(
                    toolName = "categorize_app",
                    arguments = buildJsonObject { put("category", category) }
                )
            )
        }

        // Helper closures for clean app & folder/drawer name normalization
        fun cleanTargetApp(app: String): String {
            return app.trim()
                .trim('"', '\'', '`', '“', '”')
                .trimEnd('.', '?', '!', ',', ';')
                .replaceFirst(Regex("(?i)^(?:the\\s+app\\s+|app\\s+|the\\s+)"), "")
                .trim()
        }

        fun cleanTargetDrawer(drawer: String): String {
            return drawer.trim()
                .trim('"', '\'', '`', '“', '”')
                .trimEnd('.', '?', '!', ',', ';')
                .replaceFirst(Regex("(?i)^(?:the\\s+)?(?:folder|drawer|group|category)\\s+"), "")
                .replace(Regex("(?i)\\s+(?:folder|drawer|group|category)$"), "")
                .trim()
                .trimEnd('.', '?', '!', ',', ';')
        }

        // 1. Move app between folders/drawers: "move [app] from [A] to [B]"
        val moveDrawerPattern = Pattern.compile("(?i)^(?:move|transfer|shift|put)\\s+(.+?)\\s+from\\s+(.+?)\\s+(?:to|into|in)\\s+(.+?)$")
        val moveMatcher = moveDrawerPattern.matcher(cleanQuery)
        if (moveMatcher.find()) {
            val app = cleanTargetApp(moveMatcher.group(1)?.trim() ?: "")
            val fromDrawer = cleanTargetDrawer(moveMatcher.group(2)?.trim() ?: "")
            val toDrawer = cleanTargetDrawer(moveMatcher.group(3)?.trim() ?: "")
            if (app.isNotBlank() && fromDrawer.isNotBlank() && toDrawer.isNotBlank()) {
                return listOf(
                    ToolCallResult(
                        toolName = "move_app_between_drawers",
                        arguments = buildJsonObject {
                            put("app", app)
                            put("from_drawer", fromDrawer)
                            put("to_drawer", toDrawer)
                        }
                    )
                )
            }
        }

        // Reverse syntax: "move [app] to [B] from [A]"
        val moveDrawerReversePattern = Pattern.compile("(?i)^(?:move|transfer|shift|put)\\s+(.+?)\\s+(?:to|into|in)\\s+(.+?)\\s+from\\s+(.+?)$")
        val moveReverseMatcher = moveDrawerReversePattern.matcher(cleanQuery)
        if (moveReverseMatcher.find()) {
            val app = cleanTargetApp(moveReverseMatcher.group(1)?.trim() ?: "")
            val toDrawer = cleanTargetDrawer(moveReverseMatcher.group(2)?.trim() ?: "")
            val fromDrawer = cleanTargetDrawer(moveReverseMatcher.group(3)?.trim() ?: "")
            if (app.isNotBlank() && fromDrawer.isNotBlank() && toDrawer.isNotBlank()) {
                return listOf(
                    ToolCallResult(
                        toolName = "move_app_between_drawers",
                        arguments = buildJsonObject {
                            put("app", app)
                            put("from_drawer", fromDrawer)
                            put("to_drawer", toDrawer)
                        }
                    )
                )
            }
        }

        // 2. Remove from drawer/folder or dock: "remove [app] from [drawer] (folder|drawer)"
        val removeDrawerPattern = Pattern.compile("(?i)^(?:remove|delete|drop|take)\\s+(.+?)\\s+(?:from|out\\s+of)\\s+(.+?)$")
        val removeDrawerMatcher = removeDrawerPattern.matcher(cleanQuery)
        if (removeDrawerMatcher.find()) {
            val rawApp = removeDrawerMatcher.group(1)?.trim() ?: ""
            val rawDrawer = removeDrawerMatcher.group(2)?.trim() ?: ""
            val app = cleanTargetApp(rawApp)
            val drawer = cleanTargetDrawer(rawDrawer)
            if (app.isNotBlank()) {
                if (drawer.equals("dock", ignoreCase = true) || drawer.equals("the dock", ignoreCase = true)) {
                    return listOf(
                        ToolCallResult(
                            toolName = "remove_app_from_dock",
                            arguments = buildJsonObject { put("app", app) }
                        )
                    )
                }
                return listOf(
                    ToolCallResult(
                        toolName = "remove_app_from_drawer",
                        arguments = buildJsonObject {
                            put("app", app)
                            if (drawer.isNotBlank()) {
                                put("drawer", drawer)
                            }
                        }
                    )
                )
            }
        }

        // 3. Add / Put into drawer/folder or dock: "put [app] in [drawer]", "add [app] to [drawer] folder"
        val addDrawerPattern = Pattern.compile("(?i)^(?:put|add|place|assign)\\s+(.+?)\\s+(?:in|into|to|on)\\s+(.+?)$")
        val addDrawerMatcher = addDrawerPattern.matcher(cleanQuery)
        if (addDrawerMatcher.find()) {
            val rawApp = addDrawerMatcher.group(1)?.trim() ?: ""
            val rawDrawer = addDrawerMatcher.group(2)?.trim() ?: ""
            val app = cleanTargetApp(rawApp)
            val drawer = cleanTargetDrawer(rawDrawer)
            if (app.isNotBlank() && drawer.isNotBlank()) {
                if (drawer.equals("dock", ignoreCase = true) || drawer.equals("the dock", ignoreCase = true)) {
                    return listOf(
                        ToolCallResult(
                            toolName = "add_app_to_dock",
                            arguments = buildJsonObject { put("app", app) }
                        )
                    )
                }
                return listOf(
                    ToolCallResult(
                        toolName = "add_app_to_drawer",
                        arguments = buildJsonObject {
                            put("app", app)
                            put("drawer", drawer)
                        }
                    )
                )
            }
        }

        // 4. Pin to dock: "pin [app] to dock"
        val pinDockPattern = Pattern.compile("(?i)^pin\\s+(.+?)\\s+(?:to|in|into|on)\\s+(?:the\\s+)?dock$")
        val pinDockMatcher = pinDockPattern.matcher(cleanQuery)
        if (pinDockMatcher.find()) {
            val app = cleanTargetApp(pinDockMatcher.group(1)?.trim() ?: "")
            if (app.isNotBlank()) {
                return listOf(
                    ToolCallResult(
                        toolName = "add_app_to_dock",
                        arguments = buildJsonObject { put("app", app) }
                    )
                )
            }
        }

        // 5. Unpin from dock: "unpin [app] from dock"
        val unpinDockPattern = Pattern.compile("(?i)^unpin\\s+(.+?)\\s+from\\s+(?:the\\s+)?dock$")
        val unpinDockMatcher = unpinDockPattern.matcher(cleanQuery)
        if (unpinDockMatcher.find()) {
            val app = cleanTargetApp(unpinDockMatcher.group(1)?.trim() ?: "")
            if (app.isNotBlank()) {
                return listOf(
                    ToolCallResult(
                        toolName = "remove_app_from_dock",
                        arguments = buildJsonObject { put("app", app) }
                    )
                )
            }
        }

        // 6. Create folder/drawer: "create folder Work", "new folder Games", "make folder Social"
        val createDrawerPattern = Pattern.compile("(?i)^(?:create|make|add|new)\\s+(?:a\\s+|an\\s+|new\\s+)?(?:folder|drawer|group|category)(?:\\s+(?:named|called))?\\s+(.+?)$")
        val createMatcher = createDrawerPattern.matcher(cleanQuery)
        if (createMatcher.find()) {
            val rawName = createMatcher.group(1)?.trim() ?: ""
            val drawerName = cleanTargetDrawer(rawName)
            if (drawerName.isNotBlank() && !drawerName.equals("app", ignoreCase = true) && !drawerName.startsWith("app ", ignoreCase = true)) {
                return listOf(
                    ToolCallResult(
                        toolName = "create_drawer",
                        arguments = buildJsonObject { put("drawer", drawerName) }
                    )
                )
            }
        }

        // 7. Delete folder/drawer: "delete folder Work", "remove folder Games", "delete Social folder"
        val deleteDrawerPattern = Pattern.compile("(?i)^(?:delete|remove|destroy|drop)\\s+(?:the\\s+)?(?:folder|drawer|group|category)\\s+(.+?)$")
        val deleteMatcher = deleteDrawerPattern.matcher(cleanQuery)
        if (deleteMatcher.find() && !cleanQuery.contains(" from ", ignoreCase = true) && !cleanQuery.contains(" out of ", ignoreCase = true)) {
            val rawName = deleteMatcher.group(1)?.trim() ?: ""
            val drawerName = cleanTargetDrawer(rawName)
            if (drawerName.isNotBlank()) {
                return listOf(
                    ToolCallResult(
                        toolName = "delete_drawer",
                        arguments = buildJsonObject { put("drawer", drawerName) }
                    )
                )
            }
        }

        val deleteDrawerSuffixPattern = Pattern.compile("(?i)^(?:delete|remove|destroy|drop)\\s+(.+?)\\s+(?:folder|drawer|group|category)$")
        val deleteSuffixMatcher = deleteDrawerSuffixPattern.matcher(cleanQuery)
        if (deleteSuffixMatcher.find() && !cleanQuery.contains(" from ", ignoreCase = true) && !cleanQuery.contains(" out of ", ignoreCase = true)) {
            val rawName = deleteSuffixMatcher.group(1)?.trim() ?: ""
            val drawerName = cleanTargetDrawer(rawName)
            if (drawerName.isNotBlank() && !drawerName.equals("app", ignoreCase = true) && !drawerName.startsWith("app ", ignoreCase = true)) {
                return listOf(
                    ToolCallResult(
                        toolName = "delete_drawer",
                        arguments = buildJsonObject { put("drawer", drawerName) }
                    )
                )
            }
        }

        // 8. Rename folder/drawer: "rename folder Work to Office", "rename Social folder to Chat"
        val isExcludedSetting = lower.contains("dns") || lower.contains("brightness") || lower.contains("wallpaper") || lower.contains("volume") || lower.contains("theme") || lower.contains("icon") || lower.contains("setting")
        val renameDrawerPattern = Pattern.compile("(?i)^(?:rename\\s+(?:the\\s+)?(?:folder|drawer|group|category)?|change\\s+(?:the\\s+)?(?:folder|drawer|group|category))\\s*(.+?)\\s+(?:to|into|as)\\s+(?:folder|drawer|group|category)?\\s*(.+?)$")
        val renameMatcher = renameDrawerPattern.matcher(cleanQuery)
        if (!isExcludedSetting && renameMatcher.find()) {
            val fromName = cleanTargetDrawer(renameMatcher.group(1)?.trim() ?: "")
            val toName = cleanTargetDrawer(renameMatcher.group(2)?.trim() ?: "")
            if (fromName.isNotBlank() && toName.isNotBlank()) {
                return listOf(
                    ToolCallResult(
                        toolName = "rename_drawer",
                        arguments = buildJsonObject {
                            put("from_name", fromName)
                            put("to_name", toName)
                        }
                    )
                )
            }
        }

        // 9. App / Launcher Settings Controls
        if (lower.contains("stealth theme") || lower.contains("stealth mode") || lower == "stealth" || lower == "toggle stealth") {
            val v = if (lower.contains("off") || lower.contains("disable")) "false" else if (lower.contains("on") || lower.contains("enable")) "true" else "toggle"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "STEALTH_THEME")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("lock layout") || lower.contains("unlock layout") || lower.contains("lock the layout")) {
            val v = if (lower.contains("unlock") || lower.contains("off")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "LOCK_LAYOUT")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("dynamic wallpaper") || lower.contains("live wallpaper") || lower.contains("gradient wallpaper")) {
            val v = if (lower.contains("off") || lower.contains("disable")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "DYNAMIC_WALLPAPER")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("status bar")) {
            val v = if (lower.contains("show") || lower.contains("enable") || lower.contains("unhide")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "HIDE_STATUS_BAR")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("two drawers") || lower.contains("drawers side by side") || lower.contains("dual column drawers") || lower.contains("side by side drawers")) {
            val v = if (lower.contains("off") || lower.contains("disable") || lower.contains("single")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "TWO_DRAWERS")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("drawer icons") || lower.contains("drawer icon")) {
            val v = if (lower.contains("hide") || lower.contains("remove") || lower.contains("off")) "true" else "false"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "HIDE_DRAWER_ICONS")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("camera mirror") || lower.contains("cutout mirror") || lower.contains("selfie mirror")) {
            val v = if (lower.contains("off") || lower.contains("disable")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "CAMERA_MIRROR")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("duo status") || lower.contains("status widget")) {
            val v = if (lower.contains("off") || lower.contains("disable") || lower.contains("hide")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "DUO_STATUS")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("on-device ai") || lower.contains("device ai") || lower.contains("ai drawer") || lower.contains("assistant drawer")) {
            val v = if (lower.contains("off") || lower.contains("disable") || lower.contains("turn off")) "false" else "true"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "ON_DEVICE_AI")
                        put("value", v)
                    }
                )
            )
        }

        if (lower.contains("dark theme") || lower.contains("dark mode")) {
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "THEME_MODE")
                        put("value", "DARK")
                    }
                )
            )
        }

        if (lower.contains("light theme") || lower.contains("light mode")) {
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "THEME_MODE")
                        put("value", "LIGHT")
                    }
                )
            )
        }

        if (lower.contains("system theme") || lower.contains("system mode")) {
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "THEME_MODE")
                        put("value", "SYSTEM")
                    }
                )
            )
        }

        val iconShapePattern = Pattern.compile("(?i)(?:change\\s+)?icon\\s+shape\\s+(?:to\\s+)?(circle|squircle|rounded\\s*rectangle|teardrop|hexagon|octagon)")
        val iconShapeMatcher = iconShapePattern.matcher(cleanQuery)
        if (iconShapeMatcher.find()) {
            val shape = iconShapeMatcher.group(1)?.replace(" ", "_")?.uppercase() ?: "CIRCLE"
            return listOf(
                ToolCallResult(
                    toolName = "change_app_setting",
                    arguments = buildJsonObject {
                        put("setting", "ICON_SHAPE")
                        put("value", shape)
                    }
                )
            )
        }

        // 10. Direct Brightness Control
        val brightnessPctPattern = Pattern.compile("(?i)(?:(?:set|change)\\s+)?(?:the\\s+)?brightness\\s+(?:to\\s+)?(\\d{1,3})%?|(?:^|\\s+)(\\d{1,3})%?\\s+brightness")
        val brightnessPctMatcher = brightnessPctPattern.matcher(cleanQuery)
        if (brightnessPctMatcher.find()) {
            val pctStr = brightnessPctMatcher.group(1) ?: brightnessPctMatcher.group(2)
            val pct = pctStr?.toIntOrNull()?.coerceIn(0, 100) ?: 50
            return listOf(
                ToolCallResult(
                    toolName = "set_brightness",
                    arguments = buildJsonObject {
                        put("percent", pct)
                        put("action", "set")
                    }
                )
            )
        }

        if (lower.contains("max brightness") || lower.contains("maximum brightness") || lower.contains("full brightness") || lower.contains("100% brightness")) {
            return listOf(
                ToolCallResult(
                    toolName = "set_brightness",
                    arguments = buildJsonObject {
                        put("percent", 100)
                        put("action", "max")
                    }
                )
            )
        }

        if (lower.contains("min brightness") || lower.contains("minimum brightness") || lower.contains("lowest brightness")) {
            return listOf(
                ToolCallResult(
                    toolName = "set_brightness",
                    arguments = buildJsonObject {
                        put("percent", 5)
                        put("action", "min")
                    }
                )
            )
        }

        if (lower.contains("increase brightness") || lower.contains("turn up brightness") || lower.contains("raise brightness") || lower == "brighter") {
            return listOf(
                ToolCallResult(
                    toolName = "set_brightness",
                    arguments = buildJsonObject { put("action", "increase") }
                )
            )
        }

        if (lower.contains("decrease brightness") || lower.contains("turn down brightness") || lower.contains("lower brightness") || lower.contains("dim screen") || lower == "dimmer") {
            return listOf(
                ToolCallResult(
                    toolName = "set_brightness",
                    arguments = buildJsonObject { put("action", "decrease") }
                )
            )
        }

        if (lower.contains("brightness") && !lower.contains("display setting")) {
            return listOf(
                ToolCallResult(
                    toolName = "set_brightness",
                    arguments = buildJsonObject { put("action", "prompt") }
                )
            )
        }

        // 11. Direct Volume & Mute Control
        if (lower == "mute" || lower == "mute volume" || lower == "mute sound" || lower == "silence phone" || lower == "mute audio") {
            return listOf(
                ToolCallResult(
                    toolName = "set_volume",
                    arguments = buildJsonObject { put("action", "mute") }
                )
            )
        }

        if (lower == "unmute" || lower == "unmute volume" || lower == "unmute sound" || lower == "turn on sound") {
            return listOf(
                ToolCallResult(
                    toolName = "set_volume",
                    arguments = buildJsonObject { put("action", "unmute") }
                )
            )
        }

        val volumePctPattern = Pattern.compile("(?i)(?:(?:set|change)\\s+)?(?:(media|ring|alarm)\\s+)?volume\\s+(?:to\\s+)?(\\d{1,3})%?")
        val volumePctMatcher = volumePctPattern.matcher(cleanQuery)
        if (volumePctMatcher.find()) {
            val stream = volumePctMatcher.group(1)?.uppercase() ?: "MEDIA"
            val pct = volumePctMatcher.group(2)?.toIntOrNull()?.coerceIn(0, 100) ?: 50
            return listOf(
                ToolCallResult(
                    toolName = "set_volume",
                    arguments = buildJsonObject {
                        put("stream", stream)
                        put("percent", pct)
                        put("action", "set")
                    }
                )
            )
        }

        if (lower.contains("increase volume") || lower.contains("turn up volume") || lower.contains("raise volume") || lower == "louder") {
            return listOf(
                ToolCallResult(
                    toolName = "set_volume",
                    arguments = buildJsonObject {
                        put("stream", "MEDIA")
                        put("action", "increase")
                    }
                )
            )
        }

        if (lower.contains("decrease volume") || lower.contains("turn down volume") || lower.contains("lower volume") || lower == "quieter") {
            return listOf(
                ToolCallResult(
                    toolName = "set_volume",
                    arguments = buildJsonObject {
                        put("stream", "MEDIA")
                        put("action", "decrease")
                    }
                )
            )
        }

        // 12. Direct Custom / Private DNS Control
        if (lower.contains("dns") || lower.contains("private dns")) {
            val dnsHost = when {
                lower.contains("adguard") -> "dns.adguard.com"
                lower.contains("cloudflare") || lower.contains("1.1.1.1") -> "one.one.one.one"
                lower.contains("google") || lower.contains("8.8.8.8") -> "dns.google"
                lower.contains("quad9") || lower.contains("9.9.9.9") -> "dns.quad9.net"
                else -> {
                    val customHostPattern = Pattern.compile("(?i)(?:dns|private\\s+dns)\\s+(?:to\\s+)?([a-zA-Z0-9\\.\\-]+)")
                    val m = customHostPattern.matcher(cleanQuery)
                    if (m.find()) m.group(1) else null
                }
            }

            val mode = if (lower.contains("off") || lower.contains("disable") || lower.contains("turn off")) "off" else "hostname"

            return listOf(
                ToolCallResult(
                    toolName = "set_dns",
                    arguments = buildJsonObject {
                        if (dnsHost != null && mode != "off") {
                            put("host", dnsHost)
                        }
                        put("mode", mode)
                    }
                )
            )
        }

        if (lower.contains("fingerprint") || lower.contains("biometric")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "FINGERPRINT") }
                )
            )
        }

        if (lower.contains("vpn")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "VPN") }
                )
            )
        }

        if (lower.contains("nfc")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "NFC") }
                )
            )
        }

        if (lower.contains("developer") || lower.contains("debugging")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "DEVELOPER") }
                )
            )
        }

        if (lower.contains("storage")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "STORAGE") }
                )
            )
        }

        if (lower.contains("accessibility")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "ACCESSIBILITY") }
                )
            )
        }

        if (lower.contains("bluetooth")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "BLUETOOTH") }
                )
            )
        }

        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "WIFI") }
                )
            )
        }

        if (lower.contains("hotspot") || lower.contains("tethering")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "HOTSPOT") }
                )
            )
        }

        if (lower.contains("volume") || lower.contains("sound") || lower.contains("mute")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "SOUND") }
                )
            )
        }

        if (lower.contains("brightness") || lower.contains("display setting")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "DISPLAY") }
                )
            )
        }

        if (lower.contains("battery")) {
            return listOf(
                ToolCallResult(
                    toolName = "toggle_setting",
                    arguments = buildJsonObject { put("setting", "BATTERY") }
                )
            )
        }

        if (lower == "settings" || lower == "open settings" || lower == "system settings") {
            return listOf(
                ToolCallResult(
                    toolName = "open_settings",
                    arguments = buildJsonObject {}
                )
            )
        }

        if (lower.contains("camera") || lower.contains("take a picture") || lower.contains("take a photo")) {
            return listOf(
                ToolCallResult(
                    toolName = "open_camera",
                    arguments = buildJsonObject {}
                )
            )
        }

        val compoundTimerPattern = Pattern.compile("(?i)(?:(?:set|start|create)\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(\\d+)\\s*(?:m|min|minute|minutes)\\s*(?:and\\s+)?(\\d+)\\s*(?:s|sec|second|seconds)?")
        val compoundTimerMatcher = compoundTimerPattern.matcher(cleanQuery)
        if (compoundTimerMatcher.find()) {
            val mins = compoundTimerMatcher.group(1)?.toIntOrNull() ?: 0
            val secs = compoundTimerMatcher.group(2)?.toIntOrNull() ?: 0
            val totalSeconds = mins * 60 + secs
            if (totalSeconds > 0) {
                return listOf(
                    ToolCallResult(
                        toolName = "set_timer",
                        arguments = buildJsonObject {
                            put("seconds", totalSeconds)
                        }
                    )
                )
            }
        }

        val timerPattern = Pattern.compile("(?i)(?:(?:set|start|create)\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(\\d+)\\s*(m|min|minute|minutes|s|sec|second|seconds)?")
        val timerMatcher = timerPattern.matcher(cleanQuery)
        if (timerMatcher.find()) {
            val count = timerMatcher.group(1)?.toIntOrNull() ?: 1
            val unit = timerMatcher.group(2)?.lowercase() ?: "m"
            val seconds = if (unit.startsWith("s")) count else count * 60
            return listOf(
                ToolCallResult(
                    toolName = "set_timer",
                    arguments = buildJsonObject {
                        put("seconds", seconds)
                    }
                )
            )
        }

        val timerSuffixPattern = Pattern.compile("(?i)^(\\d+)\\s*(m|min|minute|minutes|s|sec|second|seconds)\\s+timer$")
        val timerSuffixMatcher = timerSuffixPattern.matcher(cleanQuery)
        if (timerSuffixMatcher.find()) {
            val count = timerSuffixMatcher.group(1)?.toIntOrNull() ?: 1
            val unit = timerSuffixMatcher.group(2)?.lowercase() ?: "m"
            val seconds = if (unit.startsWith("s")) count else count * 60
            return listOf(
                ToolCallResult(
                    toolName = "set_timer",
                    arguments = buildJsonObject {
                        put("seconds", seconds)
                    }
                )
            )
        }

        val alarmPattern = Pattern.compile("(?i)(?:(?:set|create)\\s+(?:an?\\s+)?)?alarm\\s+(?:for\\s+)?(\\d{1,2})(?::|\\s+)?(\\d{2})?\\s*(am|pm)?")
        val alarmMatcher = alarmPattern.matcher(cleanQuery)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
            val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = alarmMatcher.group(3)?.lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return listOf(
                ToolCallResult(
                    toolName = "set_alarm",
                    arguments = buildJsonObject {
                        put("hour", hour)
                        put("minute", minute)
                    }
                )
            )
        }

        return emptyList()
    }

    override suspend fun getEmbedding(text: String): FloatArray? = withContext(Dispatchers.Default) {
        if (modelStatus.value !is ModelStatus.Ready) return@withContext null
        FloatArray(EMBEDDING_DIM) { 0.0f }
    }

    override suspend fun downloadModel(): Result<Unit> {
        return modelManager.downloadModel()
    }

    override suspend fun deleteModel(): Boolean {
        lastToolsJsonHash = null
        NeedleNativeBridge.reset()
        val deleted = modelManager.deleteModel()
        _processingState.value = ModelProcessingState()
        return deleted
    }

    companion object {
        private const val TAG = "CactusNeedleEngine"
        private const val INFERENCE_TIMEOUT_MS = 3000L
        private const val EMBEDDING_DIM = 768
    }
}
