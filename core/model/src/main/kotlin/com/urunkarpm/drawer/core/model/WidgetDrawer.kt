package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WidgetDrawer(
    val id: String,
    val title: String,
    val orderIndex: Int,
    val spanX: Int = 4,
    val spanY: Int = 3,
    val isExpanded: Boolean = true,
    val widgets: List<WidgetItem> = emptyList()
)
