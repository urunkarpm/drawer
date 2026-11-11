package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TodoPriority {
    LOW,
    NORMAL,
    HIGH
}

@Serializable
enum class TodoStatus {
    OPEN,
    DONE,
    SNOOZED
}

@Serializable
data class TodoItem(
    val id: Long = 0L,
    val title: String,
    val note: String? = null,
    val placeName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusM: Float = 200f,
    val deadlineUtc: Long? = null,
    val deadlineZoneId: String? = null,
    val priority: TodoPriority = TodoPriority.NORMAL,
    val status: TodoStatus = TodoStatus.OPEN,
    val snoozedUntil: Long? = null,
    val completedAt: Long? = null,
    val staleSince: Long? = null,
    val nudgeCount: Int = 0,
    val lastNudgeAt: Long? = null,
    val lastLocationAlertAt: Long? = null,
    val repeatRule: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
