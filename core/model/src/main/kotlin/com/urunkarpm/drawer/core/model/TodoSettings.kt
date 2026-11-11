package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoSettings(
    val locationFeaturesEnabled: Boolean = true,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val dailyCheckTime: String = "09:00",
    val staleAlertsEnabled: Boolean = true,
    val maxStaleAlertsPerDay: Int = 3,
    val doneAutoDeleteDays: Int = 30, // -1 means Never
    val hideTaskTextOnLockScreen: Boolean = false
)
