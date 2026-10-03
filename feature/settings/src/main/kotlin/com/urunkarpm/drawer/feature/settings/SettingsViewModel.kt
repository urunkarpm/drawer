package com.urunkarpm.drawer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.data.repository.BackupRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeSettings(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val hideStatusBar: Boolean = false
)

data class GlanceSettings(
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val is24Hour: Boolean = true,
    val manualCityName: String? = null
)

data class DockSettings(
    val dockBackground: String = "BLUR",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f
)

data class MiscSettings(
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "SYSTEM"
)

data class SettingsActionState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val userMessage: String? = null
)

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val hideStatusBar: Boolean = false,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val is24Hour: Boolean = true,
    val manualCityName: String? = null,
    val dockBackground: String = "BLUR",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f,
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "SYSTEM",
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val backupRepository: BackupRepository,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val actionState = MutableStateFlow(SettingsActionState())

    private val themeFlow = combine(
        preferencesDataSource.themeMode,
        preferencesDataSource.dynamicColor,
        preferencesDataSource.hideStatusBar
    ) { themeMode, dynamicColor, hideStatusBar ->
        ThemeSettings(themeMode, dynamicColor, hideStatusBar)
    }

    private val glanceFlow = combine(
        preferencesDataSource.showWeather,
        preferencesDataSource.weatherUnit,
        preferencesDataSource.is24Hour,
        preferencesDataSource.manualCityName
    ) { showWeather, weatherUnit, is24Hour, manualCityName ->
        GlanceSettings(showWeather, weatherUnit, is24Hour, manualCityName)
    }

    private val dockFlow = combine(
        preferencesDataSource.dockBackground,
        preferencesDataSource.dockIconSize,
        preferencesDataSource.dockShowLabels,
        preferencesDataSource.dockCornerRadius
    ) { dockBackground, dockIconSize, dockShowLabels, dockCornerRadius ->
        DockSettings(dockBackground, dockIconSize, dockShowLabels, dockCornerRadius)
    }

    private val miscFlow = combine(
        preferencesDataSource.multiGroupApps,
        preferencesDataSource.notificationsPrivacyMode,
        preferencesDataSource.activeIconPack,
        preferencesDataSource.adaptiveIconShape
    ) { multiGroupApps, notificationsPrivacyMode, activeIconPack, adaptiveIconShape ->
        MiscSettings(multiGroupApps, notificationsPrivacyMode, activeIconPack, adaptiveIconShape)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        themeFlow,
        glanceFlow,
        dockFlow,
        miscFlow,
        actionState
    ) { theme, glance, dock, misc, action ->
        SettingsUiState(
            themeMode = theme.themeMode,
            dynamicColor = theme.dynamicColor,
            hideStatusBar = theme.hideStatusBar,
            showWeather = glance.showWeather,
            weatherUnit = glance.weatherUnit,
            is24Hour = glance.is24Hour,
            manualCityName = glance.manualCityName,
            dockBackground = dock.dockBackground,
            dockIconSize = dock.dockIconSize,
            dockShowLabels = dock.dockShowLabels,
            dockCornerRadius = dock.dockCornerRadius,
            multiGroupApps = misc.multiGroupApps,
            notificationsPrivacyMode = misc.notificationsPrivacyMode,
            activeIconPack = misc.activeIconPack,
            adaptiveIconShape = misc.adaptiveIconShape,
            isBackingUp = action.isBackingUp,
            isRestoring = action.isRestoring,
            userMessage = action.userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDynamicColor(enabled)
        }
    }

    fun setHideStatusBar(hide: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setHideStatusBar(hide)
        }
    }

    fun setShowWeather(show: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setShowWeather(show)
        }
    }

    fun setWeatherUnit(unit: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWeatherUnit(unit)
        }
    }

    fun setIs24Hour(is24: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setIs24Hour(is24)
        }
    }

    fun setManualCityName(cityName: String?) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setManualLocation(cityName, null, null)
        }
    }

    fun setDockBackground(style: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDockBackground(style)
        }
    }

    fun setDockIconSize(sizeDp: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDockIconSize(sizeDp)
        }
    }

    fun setDockShowLabels(show: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDockShowLabels(show)
        }
    }

    fun setDockCornerRadius(radiusDp: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDockCornerRadius(radiusDp)
        }
    }

    fun setMultiGroupApps(multi: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setMultiGroupApps(multi)
        }
    }

    fun setNotificationsPrivacyMode(privacy: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setNotificationsPrivacyMode(privacy)
        }
    }

    fun setActiveIconPack(packageName: String?) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setActiveIconPack(packageName)
        }
    }

    fun setAdaptiveIconShape(shape: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAdaptiveIconShape(shape)
        }
    }

    suspend fun getBackupJson(): String {
        return backupRepository.createBackupJson()
    }

    fun restoreBackup(jsonContent: String) {
        viewModelScope.launch(ioDispatcher) {
            actionState.update { it.copy(isRestoring = true) }
            val result = backupRepository.restoreBackupJson(jsonContent)
            actionState.update {
                it.copy(
                    isRestoring = false,
                    userMessage = if (result.isSuccess) "Backup restored successfully!" else "Failed to restore backup: invalid file format"
                )
            }
        }
    }

    fun notifyBackupExported() {
        actionState.update { it.copy(userMessage = "Settings and layout exported successfully!") }
    }

    fun clearUserMessage() {
        actionState.update { it.copy(userMessage = null) }
    }
}
