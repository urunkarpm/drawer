package com.urunkarpm.drawer.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

        val MULTI_GROUP_APPS = booleanPreferencesKey("multi_group_apps")

        val NOTIFICATIONS_PRIVACY_MODE = booleanPreferencesKey("notifications_privacy_mode")

        val ACTIVE_ICON_PACK = stringPreferencesKey("active_icon_pack")
        val ADAPTIVE_ICON_SHAPE = stringPreferencesKey("adaptive_icon_shape")
    }

    val themeMode: Flow<String> = dataStore.data.map { it[PreferencesKeys.THEME_MODE] ?: "SYSTEM" }
    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DYNAMIC_COLOR] ?: true }
    val hideStatusBar: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.HIDE_STATUS_BAR] ?: false }

    val showWeather: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.SHOW_WEATHER] ?: true }
    val weatherUnit: Flow<String> = dataStore.data.map { it[PreferencesKeys.WEATHER_UNIT] ?: "CELSIUS" }
    val is24Hour: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.IS_24_HOUR] ?: true }
    val manualCityName: Flow<String?> = dataStore.data.map { it[PreferencesKeys.MANUAL_CITY_NAME] }
    val manualLat: Flow<Double?> = dataStore.data.map { it[PreferencesKeys.MANUAL_LAT] }
    val manualLon: Flow<Double?> = dataStore.data.map { it[PreferencesKeys.MANUAL_LON] }

    val dockBackground: Flow<String> = dataStore.data.map { it[PreferencesKeys.DOCK_BACKGROUND] ?: "BLUR" }
    val dockIconSize: Flow<Float> = dataStore.data.map { it[PreferencesKeys.DOCK_ICON_SIZE] ?: 56f }
    val dockShowLabels: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.DOCK_SHOW_LABELS] ?: false }

    val multiGroupApps: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.MULTI_GROUP_APPS] ?: false }

    val notificationsPrivacyMode: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.NOTIFICATIONS_PRIVACY_MODE] ?: false }

    val activeIconPack: Flow<String?> = dataStore.data.map { it[PreferencesKeys.ACTIVE_ICON_PACK] }
    val adaptiveIconShape: Flow<String> = dataStore.data.map { it[PreferencesKeys.ADAPTIVE_ICON_SHAPE] ?: "SYSTEM" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
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
        dataStore.edit { it[PreferencesKeys.DOCK_BACKGROUND] = style }
    }

    suspend fun setDockShowLabels(show: Boolean) {
        dataStore.edit { it[PreferencesKeys.DOCK_SHOW_LABELS] = show }
    }

    suspend fun setMultiGroupApps(multi: Boolean) {
        dataStore.edit { it[PreferencesKeys.MULTI_GROUP_APPS] = multi }
    }

    suspend fun setNotificationsPrivacyMode(privacy: Boolean) {
        dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_PRIVACY_MODE] = privacy }
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
}
