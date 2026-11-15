package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    // ponytail: Precomputed component key avoids allocating string on every frame during grid bind/cache lookup; ceiling: memory of one string ref per app; upgrade path: inline value class.
    @Transient
    val componentKey: String = "$packageName/$activityName#$userHandleId"
}
