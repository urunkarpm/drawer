package com.urunkarpm.drawer.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawerPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")

        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val WEATHER_UNIT = stringPreferencesKey("weather_unit")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val MANUAL_CITY_NAME = stringPreferencesKey("manual_city_name")
        val MANUAL_LAT = doublePreferencesKey("manual_lat")
        val MANUAL_LON = doublePreferencesKey("manual_lon")

        val DOCK_BACKGROUND = stringPreferencesKey("dock_background")
        val DOCK_ICON_SIZE = floatPreferencesKey("dock_icon_size")
        val DOCK_SHOW_LABELS = booleanPreferencesKey("dock_show_labels")
        val DOCK_CORNER_RADIUS = floatPreferencesKey("dock_corner_radius")

        val SURFACE_THEME_STYLE = stringPreferencesKey("surface_theme_style")
        val SURFACE_OPACITY = floatPreferencesKey("surface_opacity")
        val SURFACE_BLUR_RADIUS = floatPreferencesKey("surface_blur_radius")
        val SURFACE_STROKE_OPACITY = floatPreferencesKey("surface_stroke_opacity")
        val SURFACE_CORNER_RADIUS = floatPreferencesKey("surface_corner_radius")

        val MULTI_GROUP_APPS = booleanPreferencesKey("multi_group_apps")

        val NOTIFICATIONS_PRIVACY_MODE = booleanPreferencesKey("notifications_privacy_mode")
        val NOTIFICATION_SWIPE_RIGHT_ACTION = stringPreferencesKey("notification_swipe_right_action")
        val NOTIFICATION_SWIPE_LEFT_ACTION = stringPreferencesKey("notification_swipe_left_action")

        val ACTIVE_ICON_PACK = stringPreferencesKey("active_icon_pack")
        val ADAPTIVE_ICON_SHAPE = stringPreferencesKey("adaptive_icon_shape")
        val TWO_DRAWERS_SIDE_BY_SIDE = booleanPreferencesKey("two_drawers_side_by_side")
        val WALLPAPER_BLUR = booleanPreferencesKey("wallpaper_blur")
        val WALLPAPER_BLUR_RADIUS = floatPreferencesKey("wallpaper_blur_radius")
        val LOCK_LAYOUT = booleanPreferencesKey("lock_layout")
        val DRAWER_THEME_MODE = stringPreferencesKey("drawer_theme_mode")
        val HAS_SEEDED_DEFAULT_DOCK = booleanPreferencesKey("has_seeded_default_dock_v3")
        val AUTO_ARRANGE_APPS = booleanPreferencesKey("auto_arrange_apps")
        val AUTO_OPEN_KEYBOARD_IN_DRAWER = booleanPreferencesKey("auto_open_keyboard_in_drawer")
        val SHOW_DUO_STATUS_WIDGET = booleanPreferencesKey("show_duo_status_widget")
        val ENABLE_CAMERA_MIRROR = booleanPreferencesKey("enable_camera_mirror")
        val CATEGORY_ALIGNMENT = stringPreferencesKey("category_alignment")
        val ENABLE_WIDGETS_PAGE = booleanPreferencesKey("enable_widgets_page")
        val HEADER_FONT_FAMILY = stringPreferencesKey("header_font_family")
        val APP_LABEL_SCALE = stringPreferencesKey("app_label_scale")
        val OPEN_DRAWER_FROM_DOCK_ONLY = booleanPreferencesKey("open_drawer_from_dock_only")
        val DUO_STATUS_POSITION = stringPreferencesKey("duo_status_position")
        val HAS_COMPLETED_WALKTHROUGH = booleanPreferencesKey("has_completed_walkthrough")
        val HIDE_DRAWER_ICONS = booleanPreferencesKey("hide_drawer_icons")
        val STEALTH_THEME = booleanPreferencesKey("stealth_theme")
        val STEALTH_CONTRAST_LEVEL = stringPreferencesKey("stealth_contrast_level")
        val STEALTH_LIMIT_BRIGHTNESS = booleanPreferencesKey("stealth_limit_brightness")
        val DRAWER_NOTIFICATION_GLOW = booleanPreferencesKey("drawer_notification_glow")
        val DYNAMIC_WALLPAPER_ENABLED = booleanPreferencesKey("dynamic_wallpaper_enabled")
        val WALLPAPER_GRADIENT_SPEED_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("wallpaper_gradient_speed_seconds")
        val WALLPAPER_GRADIENT_BLUR_PERCENT = androidx.datastore.preferences.core.intPreferencesKey("wallpaper_gradient_blur_percent")
        val WALLPAPER_GRADIENT_PALETTE = stringPreferencesKey("wallpaper_gradient_palette")
        val WALLPAPER_GRADIENT_ANGLE = stringPreferencesKey("wallpaper_gradient_angle")
        val DOCK_MESMERIZING_GLOW = booleanPreferencesKey("dock_mesmerizing_glow")
        val APP_LAUNCH_COUNTS = stringPreferencesKey("app_launch_counts")
        val APP_LAST_LAUNCHED_TIMES = stringPreferencesKey("app_last_launched_times")
    }

    val appLaunchCounts: Flow<Map<String, Int>> = dataStore.data.map { prefs ->
        val json = prefs[PreferencesKeys.APP_LAUNCH_COUNTS] ?: return@map emptyMap()
        try {
            Json.decodeFromString<Map<String, Int>>(json)
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    val appLastLaunchedTimes: Flow<Map<String, Long>> = dataStore.data.map { prefs ->
        val json = prefs[PreferencesKeys.APP_LAST_LAUNCHED_TIMES] ?: return@map emptyMap()
        try {
            Json.decodeFromString<Map<String, Long>>(json)
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    val dynamicWallpaperEnabled: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DYNAMIC_WALLPAPER_ENABLED] ?: false }
    val wallpaperGradientSpeedSeconds: Flow<Int> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_GRADIENT_SPEED_SECONDS] ?: 3 }
    val wallpaperGradientBlurPercent: Flow<Int> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_GRADIENT_BLUR_PERCENT] ?: 40 }
    val wallpaperGradientPalette: Flow<String> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_GRADIENT_PALETTE] ?: "FULL_SPECTRUM" }
    val wallpaperGradientAngle: Flow<String> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_GRADIENT_ANGLE] ?: "DIAGONAL" }
    val dockMesmerizingGlow: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DOCK_MESMERIZING_GLOW] ?: true }

    val hasCompletedWalkthrough: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.HAS_COMPLETED_WALKTHROUGH] ?: false }
    val openDrawerFromDockOnly: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.OPEN_DRAWER_FROM_DOCK_ONLY] ?: false }
    val duoStatusPosition: Flow<String> = dataStore.data.map { it[PreferencesKeys.DUO_STATUS_POSITION] ?: "TEMP" }
    val hideDrawerIcons: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.HIDE_DRAWER_ICONS] ?: false }
    val drawerNotificationGlow: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DRAWER_NOTIFICATION_GLOW] ?: true }
    val stealthTheme: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.STEALTH_THEME] ?: false }
    val stealthContrastLevel: Flow<String> = dataStore.data.map { it[PreferencesKeys.STEALTH_CONTRAST_LEVEL] ?: "LOW" }
    val stealthLimitBrightness: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.STEALTH_LIMIT_BRIGHTNESS] ?: false }

    val autoOpenKeyboardInDrawer: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AUTO_OPEN_KEYBOARD_IN_DRAWER] ?: false }
    val showDuoStatusWidget: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.SHOW_DUO_STATUS_WIDGET] ?: true }
    val enableCameraMirror: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ENABLE_CAMERA_MIRROR] ?: true }
    val categoryAlignment: Flow<String> = dataStore.data.map { it[PreferencesKeys.CATEGORY_ALIGNMENT] ?: "BOTTOM" }
    val enableWidgetsPage: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ENABLE_WIDGETS_PAGE] ?: true }

    val hasSeededDefaultDock: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.HAS_SEEDED_DEFAULT_DOCK] ?: false }

    suspend fun setHasSeededDefaultDock(seeded: Boolean) {
        dataStore.edit { it[PreferencesKeys.HAS_SEEDED_DEFAULT_DOCK] = seeded }
    }

    val themeMode: Flow<String> = dataStore.data.map { it[PreferencesKeys.THEME_MODE] ?: "SYSTEM" }
    val drawerThemeMode: Flow<String> = dataStore.data.map { it[PreferencesKeys.DRAWER_THEME_MODE] ?: "SYSTEM" }
    val headerFontFamily: Flow<String> = dataStore.data.map { it[PreferencesKeys.HEADER_FONT_FAMILY] ?: "GOOGLE_SANS" }
    val appLabelScale: Flow<String> = dataStore.data.map { it[PreferencesKeys.APP_LABEL_SCALE] ?: "NORMAL" }
    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DYNAMIC_COLOR] ?: true }
    val hideStatusBar: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.HIDE_STATUS_BAR] ?: true }

    val showWeather: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.SHOW_WEATHER] ?: true }
    val weatherUnit: Flow<String> = dataStore.data.map { it[PreferencesKeys.WEATHER_UNIT] ?: "CELSIUS" }
    val is24Hour: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.IS_24_HOUR] ?: true }
    val manualCityName: Flow<String?> = dataStore.data.map { it[PreferencesKeys.MANUAL_CITY_NAME] }
    val manualLat: Flow<Double?> = dataStore.data.map { it[PreferencesKeys.MANUAL_LAT] }
    val manualLon: Flow<Double?> = dataStore.data.map { it[PreferencesKeys.MANUAL_LON] }

    val dockBackground: Flow<String> = dataStore.data.map { it[PreferencesKeys.DOCK_BACKGROUND] ?: "BLUR" }
    val dockIconSize: Flow<Float> = dataStore.data.map { it[PreferencesKeys.DOCK_ICON_SIZE] ?: 56f }
    val dockShowLabels: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DOCK_SHOW_LABELS] ?: false }
    val dockCornerRadius: Flow<Float> = dataStore.data.map { it[PreferencesKeys.DOCK_CORNER_RADIUS] ?: 24f }

    val surfaceThemeStyle: Flow<String> = dataStore.data.map {
        it[PreferencesKeys.SURFACE_THEME_STYLE] ?: it[PreferencesKeys.DOCK_BACKGROUND] ?: "LIQUID_GLASS"
    }
    val surfaceOpacity: Flow<Float> = dataStore.data.map { it[PreferencesKeys.SURFACE_OPACITY] ?: 0.65f }
    val surfaceBlurRadius: Flow<Float> = dataStore.data.map { it[PreferencesKeys.SURFACE_BLUR_RADIUS] ?: 25f }
    val surfaceStrokeOpacity: Flow<Float> = dataStore.data.map { it[PreferencesKeys.SURFACE_STROKE_OPACITY] ?: 0.40f }
    val surfaceCornerRadius: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.SURFACE_CORNER_RADIUS] ?: it[PreferencesKeys.DOCK_CORNER_RADIUS] ?: 24f
    }

    val multiGroupApps: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.MULTI_GROUP_APPS] ?: false }

    val notificationsPrivacyMode: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.NOTIFICATIONS_PRIVACY_MODE] ?: false }
    val notificationSwipeRightAction: Flow<NotificationSwipeAction> = dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.NOTIFICATION_SWIPE_RIGHT_ACTION]
        if (raw != null) {
            try {
                NotificationSwipeAction.valueOf(raw)
            } catch (_: Exception) {
                NotificationSwipeAction.MARK_AS_READ
            }
        } else {
            NotificationSwipeAction.MARK_AS_READ
        }
    }
    val notificationSwipeLeftAction: Flow<NotificationSwipeAction> = dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.NOTIFICATION_SWIPE_LEFT_ACTION]
        if (raw != null) {
            try {
                NotificationSwipeAction.valueOf(raw)
            } catch (_: Exception) {
                NotificationSwipeAction.DISMISS
            }
        } else {
            NotificationSwipeAction.DISMISS
        }
    }

    val activeIconPack: Flow<String?> = dataStore.data.map { it[PreferencesKeys.ACTIVE_ICON_PACK] }
    val adaptiveIconShape: Flow<String> = dataStore.data.map { it[PreferencesKeys.ADAPTIVE_ICON_SHAPE] ?: "SQUIRCLE" }
    val twoDrawersSideBySide: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.TWO_DRAWERS_SIDE_BY_SIDE] ?: false }
    val wallpaperBlur: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_BLUR] ?: true }
    val wallpaperBlurRadius: Flow<Float> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_BLUR_RADIUS] ?: 25f }
    val lockLayout: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.LOCK_LAYOUT] ?: false }
    val autoArrangeApps: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AUTO_ARRANGE_APPS] ?: true }

    suspend fun setAutoArrangeApps(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_ARRANGE_APPS] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
    }

    suspend fun setDrawerThemeMode(mode: String) {
        dataStore.edit { it[PreferencesKeys.DRAWER_THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setHideStatusBar(hide: Boolean) {
        dataStore.edit { it[PreferencesKeys.HIDE_STATUS_BAR] = hide }
    }

    suspend fun setShowWeather(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_WEATHER] = show }
    }

    suspend fun setWeatherUnit(unit: String) {
        dataStore.edit { it[PreferencesKeys.WEATHER_UNIT] = unit }
    }

    suspend fun setIs24Hour(is24: Boolean) {
        dataStore.edit { it[PreferencesKeys.IS_24_HOUR] = is24 }
    }

    suspend fun setManualLocation(cityName: String?, lat: Double?, lon: Double?) {
        dataStore.edit { prefs ->
            if (cityName != null) prefs[PreferencesKeys.MANUAL_CITY_NAME] = cityName else prefs.remove(PreferencesKeys.MANUAL_CITY_NAME)
            if (lat != null) prefs[PreferencesKeys.MANUAL_LAT] = lat else prefs.remove(PreferencesKeys.MANUAL_LAT)
            if (lon != null) prefs[PreferencesKeys.MANUAL_LON] = lon else prefs.remove(PreferencesKeys.MANUAL_LON)
        }
    }

    suspend fun setDockBackground(style: String) {
        dataStore.edit {
            it[PreferencesKeys.DOCK_BACKGROUND] = style
            it[PreferencesKeys.SURFACE_THEME_STYLE] = style
        }
    }

    suspend fun setSurfaceThemeStyle(style: String) {
        dataStore.edit {
            it[PreferencesKeys.SURFACE_THEME_STYLE] = style
            it[PreferencesKeys.DOCK_BACKGROUND] = style
        }
    }

    suspend fun setSurfaceOpacity(opacity: Float) {
        dataStore.edit { it[PreferencesKeys.SURFACE_OPACITY] = opacity }
    }

    suspend fun setSurfaceBlurRadius(radius: Float) {
        dataStore.edit { it[PreferencesKeys.SURFACE_BLUR_RADIUS] = radius }
    }

    suspend fun setSurfaceStrokeOpacity(opacity: Float) {
        dataStore.edit { it[PreferencesKeys.SURFACE_STROKE_OPACITY] = opacity }
    }

    suspend fun setSurfaceCornerRadius(radiusDp: Float) {
        dataStore.edit {
            it[PreferencesKeys.SURFACE_CORNER_RADIUS] = radiusDp
            it[PreferencesKeys.DOCK_CORNER_RADIUS] = radiusDp
        }
    }

    suspend fun setDockIconSize(sizeDp: Float) {
        dataStore.edit { it[PreferencesKeys.DOCK_ICON_SIZE] = sizeDp }
    }

    suspend fun setDockShowLabels(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.DOCK_SHOW_LABELS] = show }
    }

    suspend fun setDockCornerRadius(radiusDp: Float) {
        dataStore.edit { it[PreferencesKeys.DOCK_CORNER_RADIUS] = radiusDp }
    }

    suspend fun setMultiGroupApps(multi: Boolean) {
        dataStore.edit { it[PreferencesKeys.MULTI_GROUP_APPS] = multi }
    }

    suspend fun setNotificationsPrivacyMode(privacy: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_PRIVACY_MODE] = privacy }
    }

    suspend fun setNotificationSwipeRightAction(action: NotificationSwipeAction) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATION_SWIPE_RIGHT_ACTION] = action.name }
    }

    suspend fun setNotificationSwipeLeftAction(action: NotificationSwipeAction) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATION_SWIPE_LEFT_ACTION] = action.name }
    }

    suspend fun setActiveIconPack(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName != null) prefs[PreferencesKeys.ACTIVE_ICON_PACK] = packageName
            else prefs.remove(PreferencesKeys.ACTIVE_ICON_PACK)
        }
    }

    suspend fun setAdaptiveIconShape(shape: String) {
        dataStore.edit { it[PreferencesKeys.ADAPTIVE_ICON_SHAPE] = shape }
    }

    suspend fun setTwoDrawersSideBySide(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.TWO_DRAWERS_SIDE_BY_SIDE] = enabled }
    }

    suspend fun setWallpaperBlur(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.WALLPAPER_BLUR] = enabled }
    }

    suspend fun setWallpaperBlurRadius(radius: Float) {
        dataStore.edit { it[PreferencesKeys.WALLPAPER_BLUR_RADIUS] = radius }
    }

    suspend fun setLockLayout(locked: Boolean) {
        dataStore.edit { it[PreferencesKeys.LOCK_LAYOUT] = locked }
    }

    suspend fun setAutoOpenKeyboardInDrawer(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_OPEN_KEYBOARD_IN_DRAWER] = enabled }
    }

    suspend fun setShowDuoStatusWidget(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.SHOW_DUO_STATUS_WIDGET] = show }
    }

    suspend fun setEnableCameraMirror(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ENABLE_CAMERA_MIRROR] = enabled }
    }

    suspend fun setCategoryAlignment(alignment: String) {
        dataStore.edit { it[PreferencesKeys.CATEGORY_ALIGNMENT] = alignment }
    }

    suspend fun setEnableWidgetsPage(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ENABLE_WIDGETS_PAGE] = enabled }
    }


    suspend fun setHeaderFontFamily(font: String) {
        dataStore.edit { it[PreferencesKeys.HEADER_FONT_FAMILY] = font }
    }

    suspend fun setAppLabelScale(scale: String) {
        dataStore.edit { it[PreferencesKeys.APP_LABEL_SCALE] = scale }
    }

    suspend fun setOpenDrawerFromDockOnly(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.OPEN_DRAWER_FROM_DOCK_ONLY] = enabled }
    }

    suspend fun setDuoStatusPosition(position: String) {
        dataStore.edit { it[PreferencesKeys.DUO_STATUS_POSITION] = position }
    }

    suspend fun setHasCompletedWalkthrough(completed: Boolean) {
        dataStore.edit { it[PreferencesKeys.HAS_COMPLETED_WALKTHROUGH] = completed }
    }

    suspend fun setHideDrawerIcons(hide: Boolean) {
        dataStore.edit { it[PreferencesKeys.HIDE_DRAWER_ICONS] = hide }
    }

    suspend fun setStealthTheme(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.STEALTH_THEME] = enabled }
    }

    suspend fun setStealthContrastLevel(level: String) {
        dataStore.edit { it[PreferencesKeys.STEALTH_CONTRAST_LEVEL] = level }
    }

    suspend fun setStealthLimitBrightness(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.STEALTH_LIMIT_BRIGHTNESS] = enabled }
    }

    suspend fun setDrawerNotificationGlow(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DRAWER_NOTIFICATION_GLOW] = enabled }
    }

    suspend fun setDynamicWallpaperEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DYNAMIC_WALLPAPER_ENABLED] = enabled }
    }

    suspend fun setWallpaperGradientSpeedSeconds(seconds: Int) {
        dataStore.edit { it[PreferencesKeys.WALLPAPER_GRADIENT_SPEED_SECONDS] = seconds }
    }

    suspend fun setWallpaperGradientBlurPercent(percent: Int) {
        dataStore.edit { it[PreferencesKeys.WALLPAPER_GRADIENT_BLUR_PERCENT] = percent }
    }

    suspend fun setWallpaperGradientPalette(palette: String) {
        dataStore.edit { it[PreferencesKeys.WALLPAPER_GRADIENT_PALETTE] = palette }
    }

    suspend fun setWallpaperGradientAngle(angle: String) {
        dataStore.edit { it[PreferencesKeys.WALLPAPER_GRADIENT_ANGLE] = angle }
    }

    suspend fun setDockMesmerizingGlow(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DOCK_MESMERIZING_GLOW] = enabled }
    }

    suspend fun recordAppLaunch(componentKey: String) {
        dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val currentJson = prefs[PreferencesKeys.APP_LAUNCH_COUNTS]
            val currentMap = currentJson?.let {
                try {
                    Json.decodeFromString<Map<String, Int>>(it)
                } catch (_: Throwable) {
                    emptyMap()
                }
            } ?: emptyMap()
            val updated = currentMap.toMutableMap()
            updated[componentKey] = (updated[componentKey] ?: 0) + 1
            // ponytail: prune to top 100 most launched apps when map exceeds 150 entries to keep preference payload negligible; ceiling is dropping launch counts for rarely used apps; upgrade path is room database with timestamped sessions.
            val pruned = if (updated.size > 150) {
                updated.entries.sortedByDescending { it.value }.take(100).associate { it.key to it.value }
            } else {
                updated
            }
            prefs[PreferencesKeys.APP_LAUNCH_COUNTS] = Json.encodeToString(pruned)

            val currentTimesJson = prefs[PreferencesKeys.APP_LAST_LAUNCHED_TIMES]
            val currentTimesMap = currentTimesJson?.let {
                try {
                    Json.decodeFromString<Map<String, Long>>(it)
                } catch (_: Throwable) {
                    emptyMap()
                }
            } ?: emptyMap()
            val updatedTimes = currentTimesMap.toMutableMap()
            updatedTimes[componentKey] = now
            val prunedTimes = if (updatedTimes.size > 150) {
                updatedTimes.entries.sortedByDescending { it.value }.take(100).associate { it.key to it.value }
            } else {
                updatedTimes
            }
            prefs[PreferencesKeys.APP_LAST_LAUNCHED_TIMES] = Json.encodeToString(prunedTimes)
        }
    }
}
