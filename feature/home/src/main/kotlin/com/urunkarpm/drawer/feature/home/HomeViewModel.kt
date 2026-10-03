package com.urunkarpm.drawer.feature.home

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.WeatherRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlancePrefs(
    val is24Hour: Boolean = true,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val hideStatusBar: Boolean = false
)

data class HomeDialogsState(
    val searchQuery: String = "",
    val isAllAppsOpen: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val userMessage: String? = null
)

data class HomeUiState(
    val installedApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isAllAppsOpen: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val isLoading: Boolean = true,
    val userMessage: String? = null,
    val weatherInfo: WeatherInfo? = null,
    val is24Hour: Boolean = true,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val hideStatusBar: Boolean = false
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
    private val _userMessage = MutableStateFlow<String?>(null)

    private val glancePrefsFlow = combine(
        preferencesDataSource.is24Hour,
        preferencesDataSource.showWeather,
        preferencesDataSource.weatherUnit,
        preferencesDataSource.hideStatusBar
    ) { is24Hour, showWeather, weatherUnit, hideStatusBar ->
        GlancePrefs(is24Hour, showWeather, weatherUnit, hideStatusBar)
    }

    private val homeDialogsFlow = combine(
        _searchQuery,
        _isAllAppsOpen,
        _selectedAppForMenu,
        _userMessage
    ) { searchQuery, isAllAppsOpen, selectedApp, userMessage ->
        HomeDialogsState(searchQuery, isAllAppsOpen, selectedApp, userMessage)
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
        HomeUiState(
            installedApps = apps,
            filteredApps = filtered,
            searchQuery = dialogs.searchQuery,
            isAllAppsOpen = dialogs.isAllAppsOpen,
            selectedAppForMenu = dialogs.selectedAppForMenu,
            isLoading = apps.isEmpty(),
            userMessage = dialogs.userMessage,
            weatherInfo = weather,
            is24Hour = glancePrefs.is24Hour,
            showWeather = glancePrefs.showWeather,
            weatherUnit = glancePrefs.weatherUnit,
            hideStatusBar = glancePrefs.hideStatusBar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        refreshWeather()
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
    }

    fun dismissAppMenu() {
        _selectedAppForMenu.value = null
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
