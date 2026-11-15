package com.urunkarpm.drawer.feature.settings.subscreen

enum class SettingsSection(
    val title: String,
    val subtitle: String
) {
    FEATURES(
        title = "Features & Guides",
        subtitle = "App vault, tasks, notification muting, power metrics, and shortcuts"
    ),
    APPEARANCE(
        title = "Appearance & Style",
        subtitle = "Theme, accent colors, icons, fonts, glass surfaces, and wallpaper"
    ),
    DRAWERS(
        title = "Drawers & Folders",
        subtitle = "Category folders, all apps drawer, columns, and gestures"
    ),
    WORKSPACE(
        title = "Home Screen & Dock",
        subtitle = "Dock bar, glance header, weather, clock, widgets, and status bar"
    ),
    SYSTEM(
        title = "Security & System",
        subtitle = "Private app vault, notifications, stealth theme, backups, and updates"
    ),

    // Deprecated legacy aliases for backwards-compatibility with saved backstacks
    @Deprecated("Use APPEARANCE") THEMES(title = "Appearance & Style", subtitle = ""),
    @Deprecated("Use APPEARANCE") WALLPAPER(title = "Appearance & Style", subtitle = ""),
    @Deprecated("Use APPEARANCE") SURFACES(title = "Appearance & Style", subtitle = ""),
    @Deprecated("Use APPEARANCE") TYPOGRAPHY_ICONS(title = "Appearance & Style", subtitle = ""),
    @Deprecated("Use DRAWERS") HOME_DRAWERS(title = "Drawers & Folders", subtitle = ""),
    @Deprecated("Use WORKSPACE") DOCK(title = "Home Screen & Dock", subtitle = ""),
    @Deprecated("Use WORKSPACE") GLANCE(title = "Home Screen & Dock", subtitle = ""),
    @Deprecated("Use SYSTEM") PRIVACY(title = "Security & System", subtitle = ""),
    @Deprecated("Use SYSTEM") ABOUT(title = "Security & System", subtitle = "")
}
