package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LauncherPreferencesBackup(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val hideStatusBar: Boolean = false,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val is24Hour: Boolean = true,
    val manualCityName: String? = null,
    val manualLat: Double? = null,
    val manualLon: Double? = null,
    val dockBackground: String = "BLUR",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f,
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "SYSTEM"
)

@Serializable
data class LauncherBackup(
    val version: Int = 1,
    val exportTimestampMillis: Long,
    val groups: List<AppGroup>,
    val dockItems: List<DockItem>,
    val mutedRules: List<MutedAppRule> = emptyList(),
    val iconOverrides: List<IconOverride> = emptyList(),
    val preferences: LauncherPreferencesBackup = LauncherPreferencesBackup()
)
