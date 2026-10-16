package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetItem(
    val id: String,
    val appWidgetId: Int,
    val packageName: String,
    val providerClassName: String,
    val drawerId: String? = null,
    val orderIndex: Int = 0,
    val spanX: Int = 4,
    val spanY: Int = 2,
    val heightDp: Int = 180
)
