package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val userHandleId: Int = 0,
    val isWorkProfile: Boolean = false,
    val installTimeMillis: Long = 0L,
    val lastUpdateTimeMillis: Long = 0L
) {
    val componentKey: String get() = "$packageName/$activityName#$userHandleId"
}
