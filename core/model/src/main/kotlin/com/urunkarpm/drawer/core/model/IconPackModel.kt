package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class IconPackInfo(
    val packageName: String,
    val name: String,
    val isSystemDefault: Boolean = false
)

@Serializable
data class IconOverride(
    val componentName: String,
    val iconPackPackageName: String,
    val drawableName: String
)
