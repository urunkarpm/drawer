package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetItem(
    val id: String,
    val appWidgetId: Int,
    val packageName: String,
    val providerClassName: String,
    val orderIndex: Int,
    val heightDp: Int = 180
)
