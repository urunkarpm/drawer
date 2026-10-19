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
