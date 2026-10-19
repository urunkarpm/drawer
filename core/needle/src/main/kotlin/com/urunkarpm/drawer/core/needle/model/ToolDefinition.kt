package com.urunkarpm.drawer.core.needle.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject
)
