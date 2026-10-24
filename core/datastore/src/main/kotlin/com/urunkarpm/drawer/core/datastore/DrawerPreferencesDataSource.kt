package com.urunkarpm.drawer.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.urunkarpm.drawer.core.model.LauncherPreferencesBackup
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
import com.urunkarpm.drawer.core.model.WeatherInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val LAST_KNOWN_WEATHER_LAT = doublePreferencesKey("last_known_weather_lat")
        val LAST_KNOWN_WEATHER_LON = doublePreferencesKey("last_known_weather_lon")
        val LAST_KNOWN_WEATHER_CITY = stringPreferencesKey("last_known_weather_city")
        val CACHED_WEATHER_INFO = stringPreferencesKey("cached_weather_info")

        val DOCK_BACKGROUND = stringPreferencesKey("dock_background")
        val DOCK_ICON_SIZE = floatPreferencesKey("dock_icon_size")
        val DOCK_SHOW_LABELS = booleanPreferencesKey("dock_show_labels")
        val DOCK_CORNER_RADIUS = floatPreferencesKey("dock_corner_radius")

        val SURFACE_THEME_STYLE = stringPreferencesKey("surface_theme_style")
        val SURFACE_OPACITY = floatPreferencesKey("surface_opacity")
        val SURFACE_BLUR_RADIUS = floatPreferencesKey("surface_blur_radius")
        val SURFACE_STROKE_OPACITY = floatPreferencesKey("surface_stroke_opacity")
        val SURFACE_CORNER_RADIUS = floatPreferencesKey("surface_corner_radius")
        val CINEMATIC_PALETTE = stringPreferencesKey("cinematic_palette")

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
        val DOCK_EXPANDED_MAX_APPS = androidx.datastore.preferences.core.intPreferencesKey("dock_expanded_max_apps")
        val APP_LAUNCH_COUNTS = stringPreferencesKey("app_launch_counts")
        val APP_LAST_LAUNCHED_TIMES = stringPreferencesKey("app_last_launched_times")
        val AUTO_CATEGORIZE_NEW_APPS = booleanPreferencesKey("auto_categorize_new_apps")
        val AI_CATEGORIZATION_STRICTNESS = stringPreferencesKey("ai_categorization_strictness")
        val AI_CATEGORIZE_ALL_APPS = booleanPreferencesKey("ai_categorize_all_apps")
        val AI_FORCE_WORK_PROFILE = booleanPreferencesKey("ai_force_work_profile")
        val ENABLE_ON_DEVICE_AI = booleanPreferencesKey("enable_on_device_ai")
        val DRAWER_TITLE_ANIMATION = stringPreferencesKey("drawer_title_animation")
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
    val lastKnownWeatherLat: Flow<Double?> = dataStore.data.map { it[PreferencesKeys.LAST_KNOWN_WEATHER_LAT] }
    val lastKnownWeatherLon: Flow<Double?> = dataStore.data.map { it[PreferencesKeys.LAST_KNOWN_WEATHER_LON] }
    val lastKnownWeatherCity: Flow<String?> = dataStore.data.map { it[PreferencesKeys.LAST_KNOWN_WEATHER_CITY] }
    val cachedWeatherInfo: Flow<WeatherInfo?> = dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.CACHED_WEATHER_INFO] ?: return@map null
        try {
            Json.decodeFromString<WeatherInfo>(raw)
        } catch (_: Throwable) {
            null
        }
    }

    val dockBackground: Flow<String> = dataStore.data.map { it[PreferencesKeys.DOCK_BACKGROUND] ?: "BLUR" }
    val dockIconSize: Flow<Float> = dataStore.data.map { it[PreferencesKeys.DOCK_ICON_SIZE] ?: 56f }
    val dockShowLabels: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DOCK_SHOW_LABELS] ?: false }
    val dockCornerRadius: Flow<Float> = dataStore.data.map { it[PreferencesKeys.DOCK_CORNER_RADIUS] ?: 24f }
    val dockExpandedMaxApps: Flow<Int> = dataStore.data.map { it[PreferencesKeys.DOCK_EXPANDED_MAX_APPS] ?: 8 }

    val surfaceThemeStyle: Flow<String> = dataStore.data.map {
        it[PreferencesKeys.SURFACE_THEME_STYLE] ?: it[PreferencesKeys.DOCK_BACKGROUND] ?: "LIQUID_GLASS"
    }
    val surfaceOpacity: Flow<Float> = dataStore.data.map { it[PreferencesKeys.SURFACE_OPACITY] ?: 0.65f }
    val surfaceBlurRadius: Flow<Float> = dataStore.data.map { it[PreferencesKeys.SURFACE_BLUR_RADIUS] ?: 25f }
    val surfaceStrokeOpacity: Flow<Float> = dataStore.data.map { it[PreferencesKeys.SURFACE_STROKE_OPACITY] ?: 0.40f }
    val surfaceCornerRadius: Flow<Float> = dataStore.data.map {
        it[PreferencesKeys.SURFACE_CORNER_RADIUS] ?: it[PreferencesKeys.DOCK_CORNER_RADIUS] ?: 24f
    }
    val cinematicPalette: Flow<String> = dataStore.data.map { it[PreferencesKeys.CINEMATIC_PALETTE] ?: "DEFAULT" }

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
    // ponytail: default icon shape is CIRCLE across launcher preferences
    val adaptiveIconShape: Flow<String> = dataStore.data.map { it[PreferencesKeys.ADAPTIVE_ICON_SHAPE] ?: "CIRCLE" }
    // ponytail: drawer title animation mode on open/close (ACCENT_POP, FLUID_SPRING, KINETIC_DECEL, SNAPPY, OFF)
    val drawerTitleAnimation: Flow<String> = dataStore.data.map { it[PreferencesKeys.DRAWER_TITLE_ANIMATION] ?: "ACCENT_POP" }
    val twoDrawersSideBySide: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.TWO_DRAWERS_SIDE_BY_SIDE] ?: false }
    val wallpaperBlur: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_BLUR] ?: true }
    val wallpaperBlurRadius: Flow<Float> = dataStore.data.map { it[PreferencesKeys.WALLPAPER_BLUR_RADIUS] ?: 25f }
    val lockLayout: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.LOCK_LAYOUT] ?: false }
    val autoArrangeApps: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AUTO_ARRANGE_APPS] ?: true }
    val autoCategorizeNewApps: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AUTO_CATEGORIZE_NEW_APPS] ?: true }
    val aiCategorizationStrictness: Flow<String> = dataStore.data.map { it[PreferencesKeys.AI_CATEGORIZATION_STRICTNESS] ?: "BALANCED" }
    val aiCategorizeAllApps: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AI_CATEGORIZE_ALL_APPS] ?: false }
    val aiForceWorkProfile: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.AI_FORCE_WORK_PROFILE] ?: true }
    val enableOnDeviceAi: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ENABLE_ON_DEVICE_AI] ?: false }

    suspend fun setEnableOnDeviceAi(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ENABLE_ON_DEVICE_AI] = enabled }
    }

    suspend fun setAutoArrangeApps(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_ARRANGE_APPS] = enabled }
    }

    suspend fun setAiCategorizationStrictness(strictness: String) {
        dataStore.edit { it[PreferencesKeys.AI_CATEGORIZATION_STRICTNESS] = strictness }
    }

    suspend fun setAiCategorizeAllApps(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AI_CATEGORIZE_ALL_APPS] = enabled }
    }

    suspend fun setAiForceWorkProfile(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AI_FORCE_WORK_PROFILE] = enabled }
    }

    suspend fun setAutoCategorizeNewApps(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_CATEGORIZE_NEW_APPS] = enabled }
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

    suspend fun setLastKnownWeatherLocation(lat: Double, lon: Double, city: String?) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_KNOWN_WEATHER_LAT] = lat
            prefs[PreferencesKeys.LAST_KNOWN_WEATHER_LON] = lon
            if (!city.isNullOrBlank() && !city.equals("My Location", ignoreCase = true) && !city.equals("Unknown", ignoreCase = true) && !city.equals("Local Weather", ignoreCase = true)) {
                prefs[PreferencesKeys.LAST_KNOWN_WEATHER_CITY] = city
            }
        }
    }

    suspend fun setCachedWeatherInfo(info: WeatherInfo) {
        dataStore.edit { prefs ->
            try {
                prefs[PreferencesKeys.CACHED_WEATHER_INFO] = Json.encodeToString(info)
            } catch (_: Throwable) {}
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

    suspend fun setCinematicPalette(palette: String) {
        dataStore.edit { it[PreferencesKeys.CINEMATIC_PALETTE] = palette }
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

    suspend fun setDockExpandedMaxApps(maxApps: Int) {
        dataStore.edit { it[PreferencesKeys.DOCK_EXPANDED_MAX_APPS] = maxApps.coerceIn(6, 10) }
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

    suspend fun setDrawerTitleAnimation(mode: String) {
        dataStore.edit { it[PreferencesKeys.DRAWER_TITLE_ANIMATION] = mode }
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

    // ponytail: reads all preferences in a single DataStore transaction rather than launching 40+ separate flows.
    suspend fun getPreferencesBackup(): LauncherPreferencesBackup {
        val p = dataStore.data.first()
        val launchCounts = p[PreferencesKeys.APP_LAUNCH_COUNTS]?.let {
            try { Json.decodeFromString<Map<String, Int>>(it) } catch (_: Throwable) { emptyMap() }
        } ?: emptyMap()
        val launchTimes = p[PreferencesKeys.APP_LAST_LAUNCHED_TIMES]?.let {
            try { Json.decodeFromString<Map<String, Long>>(it) } catch (_: Throwable) { emptyMap() }
        } ?: emptyMap()

        return LauncherPreferencesBackup(
            themeMode = p[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            drawerThemeMode = p[PreferencesKeys.DRAWER_THEME_MODE] ?: "SYSTEM",
            dynamicColor = p[PreferencesKeys.DYNAMIC_COLOR] ?: true,
            hideStatusBar = p[PreferencesKeys.HIDE_STATUS_BAR] ?: true,
            showWeather = p[PreferencesKeys.SHOW_WEATHER] ?: true,
            weatherUnit = p[PreferencesKeys.WEATHER_UNIT] ?: "CELSIUS",
            is24Hour = p[PreferencesKeys.IS_24_HOUR] ?: true,
            manualCityName = p[PreferencesKeys.MANUAL_CITY_NAME],
            manualLat = p[PreferencesKeys.MANUAL_LAT],
            manualLon = p[PreferencesKeys.MANUAL_LON],
            dockBackground = p[PreferencesKeys.DOCK_BACKGROUND] ?: "BLUR",
            dockIconSize = p[PreferencesKeys.DOCK_ICON_SIZE] ?: 56f,
            dockShowLabels = p[PreferencesKeys.DOCK_SHOW_LABELS] ?: false,
            dockCornerRadius = p[PreferencesKeys.DOCK_CORNER_RADIUS] ?: 24f,
            surfaceThemeStyle = p[PreferencesKeys.SURFACE_THEME_STYLE] ?: p[PreferencesKeys.DOCK_BACKGROUND] ?: "LIQUID_GLASS",
            surfaceOpacity = p[PreferencesKeys.SURFACE_OPACITY] ?: 0.65f,
            surfaceBlurRadius = p[PreferencesKeys.SURFACE_BLUR_RADIUS] ?: 25f,
            surfaceStrokeOpacity = p[PreferencesKeys.SURFACE_STROKE_OPACITY] ?: 0.40f,
            surfaceCornerRadius = p[PreferencesKeys.SURFACE_CORNER_RADIUS] ?: p[PreferencesKeys.DOCK_CORNER_RADIUS] ?: 24f,
            cinematicPalette = p[PreferencesKeys.CINEMATIC_PALETTE] ?: "DEFAULT",
            multiGroupApps = p[PreferencesKeys.MULTI_GROUP_APPS] ?: false,
            notificationsPrivacyMode = p[PreferencesKeys.NOTIFICATIONS_PRIVACY_MODE] ?: false,
            notificationSwipeRightAction = p[PreferencesKeys.NOTIFICATION_SWIPE_RIGHT_ACTION] ?: "MARK_AS_READ",
            notificationSwipeLeftAction = p[PreferencesKeys.NOTIFICATION_SWIPE_LEFT_ACTION] ?: "DISMISS",
            activeIconPack = p[PreferencesKeys.ACTIVE_ICON_PACK],
            adaptiveIconShape = p[PreferencesKeys.ADAPTIVE_ICON_SHAPE] ?: "CIRCLE",
            drawerTitleAnimation = p[PreferencesKeys.DRAWER_TITLE_ANIMATION] ?: "ACCENT_POP",
            twoDrawersSideBySide = p[PreferencesKeys.TWO_DRAWERS_SIDE_BY_SIDE] ?: false,
            wallpaperBlur = p[PreferencesKeys.WALLPAPER_BLUR] ?: true,
            wallpaperBlurRadius = p[PreferencesKeys.WALLPAPER_BLUR_RADIUS] ?: 25f,
            enableWidgetsPage = p[PreferencesKeys.ENABLE_WIDGETS_PAGE] ?: true,
            lockLayout = p[PreferencesKeys.LOCK_LAYOUT] ?: false,
            autoArrangeApps = p[PreferencesKeys.AUTO_ARRANGE_APPS] ?: true,
            autoOpenKeyboardInDrawer = p[PreferencesKeys.AUTO_OPEN_KEYBOARD_IN_DRAWER] ?: false,
            showDuoStatusWidget = p[PreferencesKeys.SHOW_DUO_STATUS_WIDGET] ?: true,
            enableCameraMirror = p[PreferencesKeys.ENABLE_CAMERA_MIRROR] ?: true,
            categoryAlignment = p[PreferencesKeys.CATEGORY_ALIGNMENT] ?: "BOTTOM",
            headerFontFamily = p[PreferencesKeys.HEADER_FONT_FAMILY] ?: "GOOGLE_SANS",
            appLabelScale = p[PreferencesKeys.APP_LABEL_SCALE] ?: "NORMAL",
            openDrawerFromDockOnly = p[PreferencesKeys.OPEN_DRAWER_FROM_DOCK_ONLY] ?: false,
            duoStatusPosition = p[PreferencesKeys.DUO_STATUS_POSITION] ?: "TEMP",
            hideDrawerIcons = p[PreferencesKeys.HIDE_DRAWER_ICONS] ?: false,
            stealthTheme = p[PreferencesKeys.STEALTH_THEME] ?: false,
            stealthContrastLevel = p[PreferencesKeys.STEALTH_CONTRAST_LEVEL] ?: "LOW",
            stealthLimitBrightness = p[PreferencesKeys.STEALTH_LIMIT_BRIGHTNESS] ?: false,
            drawerNotificationGlow = p[PreferencesKeys.DRAWER_NOTIFICATION_GLOW] ?: true,
            dynamicWallpaperEnabled = p[PreferencesKeys.DYNAMIC_WALLPAPER_ENABLED] ?: false,
            wallpaperGradientSpeedSeconds = p[PreferencesKeys.WALLPAPER_GRADIENT_SPEED_SECONDS] ?: 3,
            wallpaperGradientBlurPercent = p[PreferencesKeys.WALLPAPER_GRADIENT_BLUR_PERCENT] ?: 40,
            wallpaperGradientPalette = p[PreferencesKeys.WALLPAPER_GRADIENT_PALETTE] ?: "FULL_SPECTRUM",
            wallpaperGradientAngle = p[PreferencesKeys.WALLPAPER_GRADIENT_ANGLE] ?: "DIAGONAL",
            dockMesmerizingGlow = p[PreferencesKeys.DOCK_MESMERIZING_GLOW] ?: true,
            dockExpandedMaxApps = p[PreferencesKeys.DOCK_EXPANDED_MAX_APPS] ?: 8,
            autoCategorizeNewApps = p[PreferencesKeys.AUTO_CATEGORIZE_NEW_APPS] ?: true,
            aiCategorizationStrictness = p[PreferencesKeys.AI_CATEGORIZATION_STRICTNESS] ?: "BALANCED",
            aiCategorizeAllApps = p[PreferencesKeys.AI_CATEGORIZE_ALL_APPS] ?: false,
            aiForceWorkProfile = p[PreferencesKeys.AI_FORCE_WORK_PROFILE] ?: true,
            enableOnDeviceAi = p[PreferencesKeys.ENABLE_ON_DEVICE_AI] ?: false,
            appLaunchCounts = launchCounts,
            appLastLaunchedTimes = launchTimes
        )
    }

    // ponytail: atomic bulk update of all preferences in a single DataStore edit transaction to avoid dozens of sequential disk writes and duplicate UI re-render triggers.
    suspend fun restorePreferences(prefs: LauncherPreferencesBackup) {
        dataStore.edit { p ->
            p[PreferencesKeys.THEME_MODE] = prefs.themeMode
            p[PreferencesKeys.DRAWER_THEME_MODE] = prefs.drawerThemeMode
            p[PreferencesKeys.DYNAMIC_COLOR] = prefs.dynamicColor
            p[PreferencesKeys.HIDE_STATUS_BAR] = prefs.hideStatusBar
            p[PreferencesKeys.SHOW_WEATHER] = prefs.showWeather
            p[PreferencesKeys.WEATHER_UNIT] = prefs.weatherUnit
            p[PreferencesKeys.IS_24_HOUR] = prefs.is24Hour

            val cityName = prefs.manualCityName
            if (cityName != null) p[PreferencesKeys.MANUAL_CITY_NAME] = cityName else p.remove(PreferencesKeys.MANUAL_CITY_NAME)
            val lat = prefs.manualLat
            if (lat != null) p[PreferencesKeys.MANUAL_LAT] = lat else p.remove(PreferencesKeys.MANUAL_LAT)
            val lon = prefs.manualLon
            if (lon != null) p[PreferencesKeys.MANUAL_LON] = lon else p.remove(PreferencesKeys.MANUAL_LON)

            p[PreferencesKeys.DOCK_BACKGROUND] = prefs.dockBackground
            p[PreferencesKeys.DOCK_ICON_SIZE] = prefs.dockIconSize
            p[PreferencesKeys.DOCK_SHOW_LABELS] = prefs.dockShowLabels
            p[PreferencesKeys.DOCK_CORNER_RADIUS] = prefs.dockCornerRadius

            p[PreferencesKeys.SURFACE_THEME_STYLE] = prefs.surfaceThemeStyle
            p[PreferencesKeys.SURFACE_OPACITY] = prefs.surfaceOpacity
            p[PreferencesKeys.SURFACE_BLUR_RADIUS] = prefs.surfaceBlurRadius
            p[PreferencesKeys.SURFACE_STROKE_OPACITY] = prefs.surfaceStrokeOpacity
            p[PreferencesKeys.SURFACE_CORNER_RADIUS] = prefs.surfaceCornerRadius
            p[PreferencesKeys.CINEMATIC_PALETTE] = prefs.cinematicPalette

            p[PreferencesKeys.MULTI_GROUP_APPS] = prefs.multiGroupApps
            p[PreferencesKeys.NOTIFICATIONS_PRIVACY_MODE] = prefs.notificationsPrivacyMode
            p[PreferencesKeys.NOTIFICATION_SWIPE_RIGHT_ACTION] = prefs.notificationSwipeRightAction
            p[PreferencesKeys.NOTIFICATION_SWIPE_LEFT_ACTION] = prefs.notificationSwipeLeftAction

            val activePack = prefs.activeIconPack
            if (activePack != null) p[PreferencesKeys.ACTIVE_ICON_PACK] = activePack else p.remove(PreferencesKeys.ACTIVE_ICON_PACK)
            p[PreferencesKeys.ADAPTIVE_ICON_SHAPE] = prefs.adaptiveIconShape
            p[PreferencesKeys.DRAWER_TITLE_ANIMATION] = prefs.drawerTitleAnimation
            p[PreferencesKeys.TWO_DRAWERS_SIDE_BY_SIDE] = prefs.twoDrawersSideBySide
            p[PreferencesKeys.WALLPAPER_BLUR] = prefs.wallpaperBlur
            p[PreferencesKeys.WALLPAPER_BLUR_RADIUS] = prefs.wallpaperBlurRadius
            p[PreferencesKeys.LOCK_LAYOUT] = prefs.lockLayout
            p[PreferencesKeys.AUTO_ARRANGE_APPS] = prefs.autoArrangeApps
            p[PreferencesKeys.AUTO_OPEN_KEYBOARD_IN_DRAWER] = prefs.autoOpenKeyboardInDrawer
            p[PreferencesKeys.SHOW_DUO_STATUS_WIDGET] = prefs.showDuoStatusWidget
            p[PreferencesKeys.ENABLE_CAMERA_MIRROR] = prefs.enableCameraMirror
            p[PreferencesKeys.CATEGORY_ALIGNMENT] = prefs.categoryAlignment
            p[PreferencesKeys.ENABLE_WIDGETS_PAGE] = prefs.enableWidgetsPage
            p[PreferencesKeys.HEADER_FONT_FAMILY] = prefs.headerFontFamily
            p[PreferencesKeys.APP_LABEL_SCALE] = prefs.appLabelScale
            p[PreferencesKeys.OPEN_DRAWER_FROM_DOCK_ONLY] = prefs.openDrawerFromDockOnly
            p[PreferencesKeys.DUO_STATUS_POSITION] = prefs.duoStatusPosition
            p[PreferencesKeys.HIDE_DRAWER_ICONS] = prefs.hideDrawerIcons
            p[PreferencesKeys.STEALTH_THEME] = prefs.stealthTheme
            p[PreferencesKeys.STEALTH_CONTRAST_LEVEL] = prefs.stealthContrastLevel
            p[PreferencesKeys.STEALTH_LIMIT_BRIGHTNESS] = prefs.stealthLimitBrightness
            p[PreferencesKeys.DRAWER_NOTIFICATION_GLOW] = prefs.drawerNotificationGlow
            p[PreferencesKeys.DYNAMIC_WALLPAPER_ENABLED] = prefs.dynamicWallpaperEnabled
            p[PreferencesKeys.WALLPAPER_GRADIENT_SPEED_SECONDS] = prefs.wallpaperGradientSpeedSeconds
            p[PreferencesKeys.WALLPAPER_GRADIENT_BLUR_PERCENT] = prefs.wallpaperGradientBlurPercent
            p[PreferencesKeys.WALLPAPER_GRADIENT_PALETTE] = prefs.wallpaperGradientPalette
            p[PreferencesKeys.WALLPAPER_GRADIENT_ANGLE] = prefs.wallpaperGradientAngle
            p[PreferencesKeys.DOCK_MESMERIZING_GLOW] = prefs.dockMesmerizingGlow
            p[PreferencesKeys.DOCK_EXPANDED_MAX_APPS] = prefs.dockExpandedMaxApps
            p[PreferencesKeys.AUTO_CATEGORIZE_NEW_APPS] = prefs.autoCategorizeNewApps
            p[PreferencesKeys.AI_CATEGORIZATION_STRICTNESS] = prefs.aiCategorizationStrictness
            p[PreferencesKeys.AI_CATEGORIZE_ALL_APPS] = prefs.aiCategorizeAllApps
            p[PreferencesKeys.AI_FORCE_WORK_PROFILE] = prefs.aiForceWorkProfile
            p[PreferencesKeys.ENABLE_ON_DEVICE_AI] = prefs.enableOnDeviceAi

            if (prefs.appLaunchCounts.isNotEmpty()) {
                p[PreferencesKeys.APP_LAUNCH_COUNTS] = Json.encodeToString(prefs.appLaunchCounts)
            }
            if (prefs.appLastLaunchedTimes.isNotEmpty()) {
                p[PreferencesKeys.APP_LAST_LAUNCHED_TIMES] = Json.encodeToString(prefs.appLastLaunchedTimes)
            }
        }
    }
}
