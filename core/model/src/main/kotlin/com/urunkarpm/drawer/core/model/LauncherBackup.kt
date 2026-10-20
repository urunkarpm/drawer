package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LauncherPreferencesBackup(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val hideStatusBar: Boolean = true,
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
    val adaptiveIconShape: String = "SYSTEM",
    val twoDrawersSideBySide: Boolean = false,
    val enableWidgetsPage: Boolean = true,
    val lockLayout: Boolean = false,
    val autoArrangeApps: Boolean = true,
    val autoCategorizeNewApps: Boolean = true,
    val drawerNotificationGlow: Boolean = true
)

@Serializable
data class LauncherBackup(
    val version: Int = 2,
    val exportTimestampMillis: Long,
    val groups: List<AppGroup>,
    val dockItems: List<DockItem>,
    val mutedRules: List<MutedAppRule> = emptyList(),
    val iconOverrides: List<IconOverride> = emptyList(),
    val preferences: LauncherPreferencesBackup = LauncherPreferencesBackup(),
    val widgetDrawers: List<WidgetDrawer> = emptyList(),
    val widgets: List<WidgetItem> = emptyList()
)
