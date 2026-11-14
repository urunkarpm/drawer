package com.urunkarpm.drawer.feature.settings.subscreen

enum class SettingsSection(
    val title: String,
    val subtitle: String
) {
    THEMES(
        title = "Themes & Accents",
        subtitle = "Theme mode, app drawer look, dynamic color, and vibrant accent hues"
    ),
    WALLPAPER(
        title = "Wallpaper & Effects",
        subtitle = "Cinematic film palettes, wallpaper blur, and Pantone gradient"
    ),
    SURFACES(
        title = "Glass & Surfaces",
        subtitle = "Glassmorphism, opacity, blur intensity, corner radius, and borders"
    ),
    TYPOGRAPHY_ICONS(
        title = "Typography & Icons",
        subtitle = "Font families, text scale, icon pack picker, and adaptive shapes"
    ),
    HOME_DRAWERS(
        title = "Home Screen & Drawers",
        subtitle = "Category alignment, dual columns, lock layout, and title animations"
    ),
    DOCK(
        title = "Dock Bar & Gestures",
        subtitle = "Dock size, app labels, auto-arrange, tablet capacity, and prismatic glow"
    ),
    GLANCE(
        title = "Glance Header & Widgets",
        subtitle = "24-hour clock, weather, location fallback, status widget, and punch-hole mirror"
    ),
    PRIVACY(
        title = "Notifications & Privacy",
        subtitle = "Private app vault, listener permissions, privacy mode, and stealth theme"
    ),
    ABOUT(
        title = "Backup, Updates & About",
        subtitle = "JSON backup export/import, tour walkthrough, GitHub updates, and app details"
    )
}
