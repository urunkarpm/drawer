package com.urunkarpm.drawer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.data.repository.BackupRepository
import com.urunkarpm.drawer.core.data.repository.IconPackRepository
import com.urunkarpm.drawer.core.data.repository.UpdateRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppUpdateInfo
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
    val drawerThemeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val hideStatusBar: Boolean = true,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val autoOpenKeyboardInDrawer: Boolean = false,
    val categoryAlignment: String = "BOTTOM",
    val enableWidgetsPage: Boolean = true
)

data class GlanceSettings(
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val is24Hour: Boolean = true,
    val manualCityName: String? = null,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true
)

data class DockSettings(
    val dockBackground: String = "LIQUID_GLASS",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f
)

data class SurfaceSettings(
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f
)

data class MiscSettings(
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "SQUIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val lockLayout: Boolean = false,
    val autoArrangeApps: Boolean = true
)

data class SettingsActionState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isCheckingForUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val userMessage: String? = null
)

private data class MiscPart2(
    val adaptiveIconShape: String,
    val twoDrawersSideBySide: Boolean,
    val lockLayout: Boolean,
    val autoArrangeApps: Boolean
)

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val drawerThemeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val hideStatusBar: Boolean = true,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val is24Hour: Boolean = true,
    val manualCityName: String? = null,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true,
    val categoryAlignment: String = "BOTTOM",
    val autoOpenKeyboardInDrawer: Boolean = false,
    val enableWidgetsPage: Boolean = true,
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f,
    val dockBackground: String = "LIQUID_GLASS",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f,
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "SYSTEM",
    val twoDrawersSideBySide: Boolean = false,
    val lockLayout: Boolean = false,
    val autoArrangeApps: Boolean = true,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isCheckingForUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val userMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val backupRepository: BackupRepository,
    private val updateRepository: UpdateRepository,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val iconPackRepository: IconPackRepository? = null
) : ViewModel() {

    private val actionState = MutableStateFlow(SettingsActionState())

    private val themeFlow = combine<Any, ThemeSettings>(
        preferencesDataSource.themeMode,
        preferencesDataSource.drawerThemeMode,
        preferencesDataSource.dynamicColor,
        preferencesDataSource.hideStatusBar,
        preferencesDataSource.wallpaperBlur,
        preferencesDataSource.wallpaperBlurRadius,
        preferencesDataSource.autoOpenKeyboardInDrawer,
        preferencesDataSource.categoryAlignment,
        preferencesDataSource.enableWidgetsPage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ThemeSettings(
            themeMode = values[0] as String,
            drawerThemeMode = values[1] as String,
            dynamicColor = values[2] as Boolean,
            hideStatusBar = values[3] as Boolean,
            wallpaperBlur = values[4] as Boolean,
            wallpaperBlurRadius = values[5] as Float,
            autoOpenKeyboardInDrawer = values[6] as Boolean,
            categoryAlignment = values[7] as String,
            enableWidgetsPage = values[8] as Boolean
        )
    }

    private val glanceFlow = combine<Any?, GlanceSettings>(
        preferencesDataSource.showWeather,
        preferencesDataSource.weatherUnit,
        preferencesDataSource.is24Hour,
        preferencesDataSource.manualCityName,
        preferencesDataSource.showDuoStatusWidget,
        preferencesDataSource.enableCameraMirror
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        GlanceSettings(
            showWeather = values[0] as Boolean,
            weatherUnit = values[1] as String,
            is24Hour = values[2] as Boolean,
            manualCityName = values[3] as? String,
            showDuoStatusWidget = values[4] as Boolean,
            enableCameraMirror = values[5] as Boolean
        )
    }

    private val surfaceFlow = combine(
        preferencesDataSource.surfaceThemeStyle,
        preferencesDataSource.surfaceOpacity,
        preferencesDataSource.surfaceBlurRadius,
        preferencesDataSource.surfaceStrokeOpacity,
        preferencesDataSource.surfaceCornerRadius
    ) { themeStyle, opacity, blurRadius, strokeOpacity, cornerRadius ->
        SurfaceSettings(themeStyle, opacity, blurRadius, strokeOpacity, cornerRadius)
    }

    private val dockFlow = combine(
        preferencesDataSource.dockBackground,
        preferencesDataSource.dockIconSize,
        preferencesDataSource.dockShowLabels,
        preferencesDataSource.dockCornerRadius
    ) { dockBackground, dockIconSize, dockShowLabels, dockCornerRadius ->
        DockSettings(dockBackground, dockIconSize, dockShowLabels, dockCornerRadius)
    }

    private val dockAndSurfaceFlow = combine(dockFlow, surfaceFlow) { dock, surface ->
        dock to surface
    }

    private val miscFlowPart1 = combine(
        preferencesDataSource.multiGroupApps,
        preferencesDataSource.notificationsPrivacyMode,
        preferencesDataSource.activeIconPack
    ) { multiGroupApps, notificationsPrivacyMode, activeIconPack ->
        Triple(multiGroupApps, notificationsPrivacyMode, activeIconPack)
    }

    private val miscFlowPart2 = combine(
        preferencesDataSource.adaptiveIconShape,
        preferencesDataSource.twoDrawersSideBySide,
        preferencesDataSource.lockLayout,
        preferencesDataSource.autoArrangeApps
    ) { adaptiveIconShape, twoDrawersSideBySide, lockLayout, autoArrangeApps ->
        MiscPart2(adaptiveIconShape, twoDrawersSideBySide, lockLayout, autoArrangeApps)
    }

    private val miscFlow = combine(miscFlowPart1, miscFlowPart2) { p1, p2 ->
        MiscSettings(p1.first, p1.second, p1.third, p2.adaptiveIconShape, p2.twoDrawersSideBySide, p2.lockLayout, p2.autoArrangeApps)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        themeFlow,
        glanceFlow,
        dockAndSurfaceFlow,
        miscFlow,
        actionState
    ) { theme, glance, (dock, surface), misc, action ->
        SettingsUiState(
            themeMode = theme.themeMode,
            drawerThemeMode = theme.drawerThemeMode,
            dynamicColor = theme.dynamicColor,
            hideStatusBar = theme.hideStatusBar,
            wallpaperBlur = theme.wallpaperBlur,
            wallpaperBlurRadius = theme.wallpaperBlurRadius,
            showWeather = glance.showWeather,
            weatherUnit = glance.weatherUnit,
            is24Hour = glance.is24Hour,
            manualCityName = glance.manualCityName,
            showDuoStatusWidget = glance.showDuoStatusWidget,
            enableCameraMirror = glance.enableCameraMirror,
            categoryAlignment = theme.categoryAlignment,
            autoOpenKeyboardInDrawer = theme.autoOpenKeyboardInDrawer,
            enableWidgetsPage = theme.enableWidgetsPage,
            surfaceThemeStyle = surface.surfaceThemeStyle,
            surfaceOpacity = surface.surfaceOpacity,
            surfaceBlurRadius = surface.surfaceBlurRadius,
            surfaceStrokeOpacity = surface.surfaceStrokeOpacity,
            surfaceCornerRadius = surface.surfaceCornerRadius,
            dockBackground = surface.surfaceThemeStyle,
            dockIconSize = dock.dockIconSize,
            dockShowLabels = dock.dockShowLabels,
            dockCornerRadius = surface.surfaceCornerRadius,
            multiGroupApps = misc.multiGroupApps,
            notificationsPrivacyMode = misc.notificationsPrivacyMode,
            activeIconPack = misc.activeIconPack,
            adaptiveIconShape = misc.adaptiveIconShape,
            twoDrawersSideBySide = misc.twoDrawersSideBySide,
            lockLayout = misc.lockLayout,
            autoArrangeApps = misc.autoArrangeApps,
            isBackingUp = action.isBackingUp,
            isRestoring = action.isRestoring,
            isCheckingForUpdate = action.isCheckingForUpdate,
            updateInfo = action.updateInfo,
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

    fun setDrawerThemeMode(mode: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDrawerThemeMode(mode)
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

    fun setWallpaperBlur(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWallpaperBlur(enabled)
        }
    }

    fun setWallpaperBlurRadius(radius: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWallpaperBlurRadius(radius)
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

    fun setSurfaceThemeStyle(style: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setSurfaceThemeStyle(style)
        }
    }

    fun setSurfaceOpacity(opacity: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setSurfaceOpacity(opacity)
        }
    }

    fun setSurfaceBlurRadius(radius: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setSurfaceBlurRadius(radius)
        }
    }

    fun setSurfaceStrokeOpacity(opacity: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setSurfaceStrokeOpacity(opacity)
        }
    }

    fun setSurfaceCornerRadius(radiusDp: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setSurfaceCornerRadius(radiusDp)
        }
    }

    fun setDockBackground(style: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setSurfaceThemeStyle(style)
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

    fun setAutoArrangeApps(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAutoArrangeApps(enabled)
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
            iconPackRepository?.setActiveIconPack(packageName)
        }
    }

    fun setAdaptiveIconShape(shape: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAdaptiveIconShape(shape)
        }
    }

    fun setTwoDrawersSideBySide(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setTwoDrawersSideBySide(enabled)
        }
    }

    fun setLockLayout(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setLockLayout(enabled)
        }
    }

    fun setAutoOpenKeyboardInDrawer(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAutoOpenKeyboardInDrawer(enabled)
        }
    }

    fun setShowDuoStatusWidget(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setShowDuoStatusWidget(enabled)
        }
    }

    fun setEnableCameraMirror(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setEnableCameraMirror(enabled)
        }
    }

    fun setCategoryAlignment(alignment: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setCategoryAlignment(alignment)
        }
    }

    fun setEnableWidgetsPage(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setEnableWidgetsPage(enabled)
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

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch(ioDispatcher) {
            actionState.update { it.copy(isCheckingForUpdate = true) }
            val result = updateRepository.checkForUpdates(currentVersion)
            result.onSuccess { info ->
                actionState.update {
                    it.copy(
                        isCheckingForUpdate = false,
                        updateInfo = info,
                        userMessage = if (!info.isUpdateAvailable) "You're on the latest version ($currentVersion)" else null
                    )
                }
            }.onFailure { error ->
                actionState.update {
                    it.copy(
                        isCheckingForUpdate = false,
                        userMessage = "Failed to check for updates: ${error.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        actionState.update { it.copy(updateInfo = null) }
    }

    fun clearUserMessage() {
        actionState.update { it.copy(userMessage = null) }
    }
}
