package com.urunkarpm.drawer.core.needle.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ToolCallResult(
    val toolName: String,
    val arguments: JsonObject
) {
    fun getString(key: String): String? = arguments[key]?.jsonPrimitive?.content
    fun getInt(key: String): Int? = arguments[key]?.jsonPrimitive?.content?.toIntOrNull()
}
