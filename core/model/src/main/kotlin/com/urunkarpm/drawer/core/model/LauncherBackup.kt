package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LauncherBackup(
    val version: Int = 1,
    val exportTimestampMillis: Long,
    val groups: List<AppGroup>,
    val dockItems: List<DockItem>,
    val mutedRules: List<MutedAppRule>
)
