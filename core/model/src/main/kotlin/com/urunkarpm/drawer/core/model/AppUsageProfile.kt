package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageProfile(
    val totalScore: Float = 0f,
    val lastLaunchTime: Long = 0L,
    // Slots: 0=Morning (06-12), 1=Afternoon (12-17), 2=Evening (17-22), 3=Night (22-06)
    val weekdaySlots: List<Int> = listOf(0, 0, 0, 0),
    val weekendSlots: List<Int> = listOf(0, 0, 0, 0)
)
