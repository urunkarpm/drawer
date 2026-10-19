package com.urunkarpm.drawer.core.needle.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolCallResult(
    val toolName: String,
    val arguments: JsonObject
)
