package com.urunkarpm.drawer.feature.home

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.WeatherRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppShortcutInfo
import com.urunkarpm.drawer.core.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import com.urunkarpm.drawer.core.designsystem.component.AppIconCache
import com.urunkarpm.drawer.core.designsystem.component.toImageBitmapSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlancePrefs(
    val is24Hour: Boolean = true,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val hideStatusBar: Boolean = true,
    val adaptiveIconShape: String = "SYSTEM",
    val twoDrawersSideBySide: Boolean = false,
    val wallpaperBlur: Boolean = false,
    val wallpaperBlurRadius: Float = 25f
)

data class HomeDialogsState(
    val searchQuery: String = "",
    val isAllAppsOpen: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val shortcuts: List<AppShortcutInfo> = emptyList(),
    val userMessage: String? = null
)

data class HomeUiState(
    val installedApps: List<AppInfo> = emptyList(),
    val installedAppsByPackage: Map<String, AppInfo> = emptyMap(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isAllAppsOpen: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val shortcuts: List<AppShortcutInfo> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
    val weatherInfo: WeatherInfo? = null,
    val is24Hour: Boolean = true,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val hideStatusBar: Boolean = true,
    val adaptiveIconShape: String = "SYSTEM",
    val twoDrawersSideBySide: Boolean = false,
    val wallpaperBlur: Boolean = false,
    val wallpaperBlurRadius: Float = 25f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val dockRepository: DockRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isAllAppsOpen = MutableStateFlow(false)
    private val _selectedAppForMenu = MutableStateFlow<AppInfo?>(null)
    private val _shortcuts = MutableStateFlow<List<AppShortcutInfo>>(emptyList())
    private val _userMessage = MutableStateFlow<String?>(null)

    private val glancePrefsFlow = combine<Any, GlancePrefs>(
        preferencesDataSource.is24Hour,
        preferencesDataSource.showWeather,
        preferencesDataSource.weatherUnit,
        preferencesDataSource.hideStatusBar,
        preferencesDataSource.adaptiveIconShape,
        preferencesDataSource.twoDrawersSideBySide,
        preferencesDataSource.wallpaperBlur,
        preferencesDataSource.wallpaperBlurRadius
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val is24Hour = values[0] as Boolean
        val showWeather = values[1] as Boolean
        val weatherUnit = values[2] as String
        val hideStatusBar = values[3] as Boolean
        val adaptiveIconShape = values[4] as String
        val twoDrawersSideBySide = values[5] as Boolean
        val wallpaperBlur = values[6] as Boolean
        val wallpaperBlurRadius = values[7] as Float
        GlancePrefs(is24Hour, showWeather, weatherUnit, hideStatusBar, adaptiveIconShape, twoDrawersSideBySide, wallpaperBlur, wallpaperBlurRadius)
    }

    private val homeDialogsFlow = combine(
        _searchQuery,
        _isAllAppsOpen,
        _selectedAppForMenu,
        _shortcuts,
        _userMessage
    ) { searchQuery, isAllAppsOpen, selectedApp, shortcuts, userMessage ->
        HomeDialogsState(searchQuery, isAllAppsOpen, selectedApp, shortcuts, userMessage)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.installedApps,
        weatherRepository.weatherInfo,
        glancePrefsFlow,
        homeDialogsFlow
    ) { apps, weather, glancePrefs, dialogs ->
        val filtered = if (dialogs.searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { it.label.contains(dialogs.searchQuery, ignoreCase = true) }
        }
        val appsByPkg = apps.associateBy { it.packageName }
        HomeUiState(
            installedApps = apps,
            installedAppsByPackage = appsByPkg,
            filteredApps = filtered,
            searchQuery = dialogs.searchQuery,
            isAllAppsOpen = dialogs.isAllAppsOpen,
            selectedAppForMenu = dialogs.selectedAppForMenu,
            shortcuts = dialogs.shortcuts,
            isLoading = apps.isEmpty(),
            userMessage = dialogs.userMessage,
            weatherInfo = weather,
            is24Hour = glancePrefs.is24Hour,
            showWeather = glancePrefs.showWeather,
            weatherUnit = glancePrefs.weatherUnit,
            hideStatusBar = glancePrefs.hideStatusBar,
            adaptiveIconShape = glancePrefs.adaptiveIconShape,
            twoDrawersSideBySide = glancePrefs.twoDrawersSideBySide,
            wallpaperBlur = glancePrefs.wallpaperBlur,
            wallpaperBlurRadius = glancePrefs.wallpaperBlurRadius
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        refreshWeather()
        viewModelScope.launch(Dispatchers.IO) {
            dockRepository.dockItems.collect { dockItems ->
                val apps = appRepository.installedApps.first()
                val appsMap = apps.associateBy { "${it.packageName}/${it.activityName}" }
                for (item in dockItems) {
                    val targetApp: AppInfo? = appsMap["${item.packageName}/${item.activityName}"]
                    if (targetApp != null && AppIconCache.get(targetApp.componentKey) == null) {
                        val d = appRepository.getAppIcon(targetApp)
                        val bmp = d?.toImageBitmapSafe()
                        if (bmp != null) {
                            AppIconCache.put(targetApp.componentKey, bmp)
                        }
                    }
                }
            }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            weatherRepository.refreshWeather()
        }
    }

    fun pinToDock(app: AppInfo) {
        viewModelScope.launch {
            val added = dockRepository.addToDock(app)
            _userMessage.value = if (added) {
                "${app.label} pinned to dock"
            } else {
                "Cannot pin to dock: full (max 5) or already in dock"
            }
            dismissAppMenu()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun openAllApps() {
        _isAllAppsOpen.value = true
    }

    fun closeAllApps() {
        _isAllAppsOpen.value = false
        _searchQuery.value = ""
    }

    fun toggleAllApps() {
        if (_isAllAppsOpen.value) {
            closeAllApps()
        } else {
            openAllApps()
        }
    }

    fun onAppClicked(app: AppInfo): Boolean {
        val launched = appRepository.launchApp(app)
        if (launched && _isAllAppsOpen.value) {
            closeAllApps()
        }
        return launched
    }

    fun onAppLongClicked(app: AppInfo) {
        _selectedAppForMenu.value = app
        _shortcuts.value = appRepository.getShortcuts(app)
    }

    fun dismissAppMenu() {
        _selectedAppForMenu.value = null
        _shortcuts.value = emptyList()
    }

    fun launchShortcut(app: AppInfo, shortcutId: String) {
        val launched = appRepository.launchShortcut(app, shortcutId)
        if (launched && _isAllAppsOpen.value) {
            closeAllApps()
        }
        dismissAppMenu()
    }

    suspend fun getShortcutIcon(app: AppInfo, shortcutId: String): Drawable? {
        return appRepository.getShortcutIcon(app, shortcutId)
    }

    fun openAppDetails(app: AppInfo) {
        appRepository.openAppDetails(app.packageName)
        dismissAppMenu()
    }

    fun uninstallApp(app: AppInfo) {
        appRepository.uninstallApp(app.packageName)
        dismissAppMenu()
    }

    suspend fun getAppIcon(app: AppInfo): Drawable? {
        return appRepository.getAppIcon(app)
    }

    fun refreshApps() {
        viewModelScope.launch {
            appRepository.refreshApps()
        }
    }
}
