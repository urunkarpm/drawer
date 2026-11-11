package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class EventLog(
    val id: Long = 0L,
    val time: Long = System.currentTimeMillis(),
    val type: String,
    val text: String
)
