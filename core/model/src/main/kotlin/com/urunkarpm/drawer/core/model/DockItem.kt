package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DockItem(
    val position: Int, // 0 to 4
    val packageName: String,
    val activityName: String,
    val userHandleId: Int = 0,
    val customLabel: String? = null
)
