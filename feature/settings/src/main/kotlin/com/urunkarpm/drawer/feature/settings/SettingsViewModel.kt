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
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
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
    val drawerLiquidGlass: Boolean = false,
    val drawerLook: String = "SOLID",
    val dynamicColor: Boolean = true,
    val accentColor: String = "DYNAMIC",
    val hideStatusBar: Boolean = true,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val autoOpenKeyboardInDrawer: Boolean = false,
    val blurAppListOnOpen: Boolean = false,
    val appDrawerColumns: Int = 4,
    val categoryAlignment: String = "BOTTOM",
    val enableWidgetsPage: Boolean = true,
    val showDrawerSuggestions: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true
)

data class WallpaperSettings(
    val dynamicWallpaperEnabled: Boolean = false,
    val wallpaperGradientSpeedSeconds: Int = 3,
    val wallpaperGradientBlurPercent: Int = 40,
    val wallpaperGradientPalette: String = "FULL_SPECTRUM",
    val wallpaperGradientAngle: String = "DIAGONAL",
    val dockMesmerizingGlow: Boolean = true
)

data class GlanceSettings(
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val is24Hour: Boolean = true,
    val manualCityName: String? = null,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true,
    val duoStatusPosition: String = "TEMP"
)

data class DockSettings(
    val dockBackground: String = "LIQUID_GLASS",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f,
    val openDrawerFromDockOnly: Boolean = false,
    val dockExpandedMaxApps: Int = 8
)

data class SurfaceSettings(
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f,
    val cinematicPalette: String = "DEFAULT",
    val matchDeviceScreenCurve: Boolean = true,
    val deviceCurveOffsetDp: Float = 0f
)

data class MiscSettings(
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "CIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val lockLayout: Boolean = false,
    val autoArrangeApps: Boolean = true,
    val hideDrawerIcons: Boolean = false,
    val notificationSwipeRightAction: NotificationSwipeAction = NotificationSwipeAction.MARK_AS_READ,
    val notificationSwipeLeftAction: NotificationSwipeAction = NotificationSwipeAction.DISMISS,
    val stealthTheme: Boolean = false,
    val stealthContrastLevel: String = "LOW",
    val stealthLimitBrightness: Boolean = false,
    val drawerNotificationGlow: Boolean = true,
    val autoCategorizeNewApps: Boolean = true,
    val aiCategorizationStrictness: String = "BALANCED",
    val aiCategorizeAllApps: Boolean = false,
    val aiForceWorkProfile: Boolean = true,
    val drawerTitleAnimation: String = "ACCENT_POP"
)

data class SettingsActionState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isCheckingForUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val userMessage: String? = null
)

private data class MiscPart1(
    val multiGroupApps: Boolean,
    val notificationsPrivacyMode: Boolean,
    val activeIconPack: String?,
    val swipeRight: NotificationSwipeAction,
    val swipeLeft: NotificationSwipeAction
)

private data class MiscPart2(
    val adaptiveIconShape: String,
    val twoDrawersSideBySide: Boolean,
    val lockLayout: Boolean,
    val autoArrangeApps: Boolean,
    val hideDrawerIcons: Boolean
)

private data class MiscPart3(
    val stealthTheme: Boolean,
    val stealthContrastLevel: String,
    val stealthLimitBrightness: Boolean,
    val drawerNotificationGlow: Boolean,
    val autoCategorizeNewApps: Boolean
)

private data class MiscPart4(
    val aiCategorizationStrictness: String,
    val aiCategorizeAllApps: Boolean,
    val aiForceWorkProfile: Boolean,
    val drawerTitleAnimation: String
)

private data class GradientVisuals(
    val speed: Int,
    val blur: Int,
    val palette: String,
    val angle: String
)

private data class TypographyAndAction(
    val headerFontFamily: String,
    val appLabelScale: String,
    val autoCheckUpdates: Boolean,
    val action: SettingsActionState
)

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val drawerThemeMode: String = "SYSTEM",
    val drawerLiquidGlass: Boolean = false,
    val drawerLook: String = "SOLID",
    val dynamicColor: Boolean = true,
    val accentColor: String = "DYNAMIC",
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
    val blurAppListOnOpen: Boolean = false,
    val appDrawerColumns: Int = 4,
    val enableWidgetsPage: Boolean = true,
    val showDrawerSuggestions: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f,
    val matchDeviceScreenCurve: Boolean = true,
    val deviceCurveOffsetDp: Float = 0f,
    val cinematicPalette: String = "DEFAULT",
    val dockBackground: String = "LIQUID_GLASS",
    val dockIconSize: Float = 56f,
    val dockShowLabels: Boolean = false,
    val dockCornerRadius: Float = 24f,
    val multiGroupApps: Boolean = false,
    val notificationsPrivacyMode: Boolean = false,
    val notificationSwipeRightAction: NotificationSwipeAction = NotificationSwipeAction.MARK_AS_READ,
    val notificationSwipeLeftAction: NotificationSwipeAction = NotificationSwipeAction.DISMISS,
    val activeIconPack: String? = null,
    val adaptiveIconShape: String = "CIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val lockLayout: Boolean = false,
    val autoArrangeApps: Boolean = true,
    val hideDrawerIcons: Boolean = false,
    val headerFontFamily: String = "GOOGLE_SANS",
    val appLabelScale: String = "NORMAL",
    val openDrawerFromDockOnly: Boolean = false,
    val duoStatusPosition: String = "TEMP",
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isCheckingForUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val userMessage: String? = null,
    val stealthTheme: Boolean = false,
    val stealthContrastLevel: String = "LOW",
    val stealthLimitBrightness: Boolean = false,
    val drawerNotificationGlow: Boolean = true,
    val autoCategorizeNewApps: Boolean = true,
    val aiCategorizationStrictness: String = "BALANCED",
    val aiCategorizeAllApps: Boolean = false,
    val aiForceWorkProfile: Boolean = true,
    val dynamicWallpaperEnabled: Boolean = false,
    val wallpaperGradientSpeedSeconds: Int = 3,
    val wallpaperGradientBlurPercent: Int = 40,
    val wallpaperGradientPalette: String = "FULL_SPECTRUM",
    val wallpaperGradientAngle: String = "DIAGONAL",
    val dockMesmerizingGlow: Boolean = true,
    val dockExpandedMaxApps: Int = 8,
    val drawerTitleAnimation: String = "ACCENT_POP",
    val autoCheckUpdates: Boolean = true
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
        preferencesDataSource.drawerLiquidGlass,
        preferencesDataSource.drawerLook,
        preferencesDataSource.dynamicColor,
        preferencesDataSource.accentColor,
        preferencesDataSource.hideStatusBar,
        preferencesDataSource.wallpaperBlur,
        preferencesDataSource.wallpaperBlurRadius,
        preferencesDataSource.autoOpenKeyboardInDrawer,
        preferencesDataSource.blurAppListOnOpen,
        preferencesDataSource.appDrawerColumns,
        preferencesDataSource.categoryAlignment,
        preferencesDataSource.enableWidgetsPage,
        preferencesDataSource.showDrawerSuggestions,
        preferencesDataSource.hapticFeedbackEnabled
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ThemeSettings(
            themeMode = values[0] as String,
            drawerThemeMode = values[1] as String,
            drawerLiquidGlass = values[2] as Boolean,
            drawerLook = values[3] as String,
            dynamicColor = values[4] as Boolean,
            accentColor = values[5] as String,
            hideStatusBar = values[6] as Boolean,
            wallpaperBlur = values[7] as Boolean,
            wallpaperBlurRadius = values[8] as Float,
            autoOpenKeyboardInDrawer = values[9] as Boolean,
            blurAppListOnOpen = values[10] as Boolean,
            appDrawerColumns = values[11] as Int,
            categoryAlignment = values[12] as String,
            enableWidgetsPage = values[13] as Boolean,
            showDrawerSuggestions = values[14] as Boolean,
            hapticFeedbackEnabled = values[15] as Boolean
        )
    }

    private val glanceFlow = combine<Any?, GlanceSettings>(
        preferencesDataSource.showWeather,
        preferencesDataSource.weatherUnit,
        preferencesDataSource.is24Hour,
        preferencesDataSource.manualCityName,
        preferencesDataSource.showDuoStatusWidget,
        preferencesDataSource.enableCameraMirror,
        preferencesDataSource.duoStatusPosition
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        GlanceSettings(
            showWeather = values[0] as Boolean,
            weatherUnit = values[1] as String,
            is24Hour = values[2] as Boolean,
            manualCityName = values[3] as? String,
            showDuoStatusWidget = values[4] as Boolean,
            enableCameraMirror = values[5] as Boolean,
            duoStatusPosition = values[6] as String
        )
    }

    private val surfaceFlow = combine(
        combine(
            preferencesDataSource.surfaceThemeStyle,
            preferencesDataSource.surfaceCornerRadius,
            preferencesDataSource.cinematicPalette,
            preferencesDataSource.matchDeviceScreenCurve
        ) { themeStyle, cornerRadius, palette, matchCurve -> listOf(themeStyle, cornerRadius, palette, matchCurve) },
        combine(
            preferencesDataSource.surfaceOpacity,
            preferencesDataSource.surfaceBlurRadius,
            preferencesDataSource.surfaceStrokeOpacity,
            preferencesDataSource.deviceCurveOffsetDp
        ) { opacity, blurRadius, strokeOpacity, offsetDp -> listOf(opacity, blurRadius, strokeOpacity, offsetDp) }
    ) { surface1, surface2 ->
        SurfaceSettings(
            surfaceThemeStyle = surface1[0] as String,
            surfaceCornerRadius = surface1[1] as Float,
            cinematicPalette = surface1[2] as String,
            matchDeviceScreenCurve = surface1[3] as Boolean,
            surfaceOpacity = surface2[0] as Float,
            surfaceBlurRadius = surface2[1] as Float,
            surfaceStrokeOpacity = surface2[2] as Float,
            deviceCurveOffsetDp = surface2[3] as Float
        )
    }

    private val dockFlow = combine<Any, DockSettings>(
        preferencesDataSource.dockBackground,
        preferencesDataSource.dockIconSize,
        preferencesDataSource.dockShowLabels,
        preferencesDataSource.dockCornerRadius,
        preferencesDataSource.openDrawerFromDockOnly,
        preferencesDataSource.dockExpandedMaxApps
    ) { values ->
        DockSettings(
            dockBackground = values[0] as String,
            dockIconSize = values[1] as Float,
            dockShowLabels = values[2] as Boolean,
            dockCornerRadius = values[3] as Float,
            openDrawerFromDockOnly = values[4] as Boolean,
            dockExpandedMaxApps = values[5] as Int
        )
    }

    private val dockAndSurfaceFlow = combine(dockFlow, surfaceFlow) { dock, surface ->
        dock to surface
    }

    private val miscFlowPart1 = combine(
        preferencesDataSource.multiGroupApps,
        preferencesDataSource.notificationsPrivacyMode,
        preferencesDataSource.activeIconPack,
        preferencesDataSource.notificationSwipeRightAction,
        preferencesDataSource.notificationSwipeLeftAction
    ) { multiGroupApps, notificationsPrivacyMode, activeIconPack, swipeRight, swipeLeft ->
        MiscPart1(multiGroupApps, notificationsPrivacyMode, activeIconPack, swipeRight, swipeLeft)
    }

    private val miscFlowPart2 = combine(
        preferencesDataSource.adaptiveIconShape,
        preferencesDataSource.twoDrawersSideBySide,
        preferencesDataSource.lockLayout,
        preferencesDataSource.autoArrangeApps,
        preferencesDataSource.hideDrawerIcons
    ) { adaptiveIconShape, twoDrawersSideBySide, lockLayout, autoArrangeApps, hideDrawerIcons ->
        MiscPart2(adaptiveIconShape, twoDrawersSideBySide, lockLayout, autoArrangeApps, hideDrawerIcons)
    }

    private val miscFlowPart3 = combine(
        preferencesDataSource.stealthTheme,
        preferencesDataSource.stealthContrastLevel,
        preferencesDataSource.stealthLimitBrightness,
        preferencesDataSource.drawerNotificationGlow,
        preferencesDataSource.autoCategorizeNewApps
    ) { stealthTheme, stealthContrastLevel, stealthLimitBrightness, drawerNotificationGlow, autoCategorizeNewApps ->
        MiscPart3(stealthTheme, stealthContrastLevel, stealthLimitBrightness, drawerNotificationGlow, autoCategorizeNewApps)
    }

    private val miscFlowPart4 = combine(
        preferencesDataSource.aiCategorizationStrictness,
        preferencesDataSource.aiCategorizeAllApps,
        preferencesDataSource.aiForceWorkProfile,
        preferencesDataSource.drawerTitleAnimation
    ) { strictness, categorizeAll, forceWorkProfile, titleAnim ->
        MiscPart4(strictness, categorizeAll, forceWorkProfile, titleAnim)
    }

    private val miscFlow = combine(miscFlowPart1, miscFlowPart2, miscFlowPart3, miscFlowPart4) { p1, p2, p3, p4 ->
        MiscSettings(
            multiGroupApps = p1.multiGroupApps,
            notificationsPrivacyMode = p1.notificationsPrivacyMode,
            activeIconPack = p1.activeIconPack,
            adaptiveIconShape = p2.adaptiveIconShape,
            twoDrawersSideBySide = p2.twoDrawersSideBySide,
            lockLayout = p2.lockLayout,
            autoArrangeApps = p2.autoArrangeApps,
            hideDrawerIcons = p2.hideDrawerIcons,
            notificationSwipeRightAction = p1.swipeRight,
            notificationSwipeLeftAction = p1.swipeLeft,
            stealthTheme = p3.stealthTheme,
            stealthContrastLevel = p3.stealthContrastLevel,
            stealthLimitBrightness = p3.stealthLimitBrightness,
            drawerNotificationGlow = p3.drawerNotificationGlow,
            autoCategorizeNewApps = p3.autoCategorizeNewApps,
            aiCategorizationStrictness = p4.aiCategorizationStrictness,
            aiCategorizeAllApps = p4.aiCategorizeAllApps,
            aiForceWorkProfile = p4.aiForceWorkProfile,
            drawerTitleAnimation = p4.drawerTitleAnimation
        )
    }

    private val typographyFlow = combine(
        preferencesDataSource.headerFontFamily,
        preferencesDataSource.appLabelScale,
        preferencesDataSource.autoCheckUpdates
    ) { font, scale, autoCheckUpdates ->
        Triple(font, scale, autoCheckUpdates)
    }

    private val typographyAndActionFlow = combine(typographyFlow, actionState) { (font, scale, autoCheckUpdates), action ->
        TypographyAndAction(font, scale, autoCheckUpdates, action)
    }

    private val wallpaperVisualsFlow = combine(
        preferencesDataSource.wallpaperGradientSpeedSeconds,
        preferencesDataSource.wallpaperGradientBlurPercent,
        preferencesDataSource.wallpaperGradientPalette,
        preferencesDataSource.wallpaperGradientAngle
    ) { speed, blur, palette, angle ->
        GradientVisuals(speed, blur, palette, angle)
    }

    private val wallpaperFlow = combine(
        preferencesDataSource.dynamicWallpaperEnabled,
        preferencesDataSource.dockMesmerizingGlow,
        wallpaperVisualsFlow
    ) { enabled, glow, visuals ->
        WallpaperSettings(
            dynamicWallpaperEnabled = enabled,
            wallpaperGradientSpeedSeconds = visuals.speed,
            wallpaperGradientBlurPercent = visuals.blur,
            wallpaperGradientPalette = visuals.palette,
            wallpaperGradientAngle = visuals.angle,
            dockMesmerizingGlow = glow
        )
    }

    private val themeAndWallpaperFlow = combine(themeFlow, wallpaperFlow) { theme, wallpaper ->
        theme to wallpaper
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        themeAndWallpaperFlow,
        glanceFlow,
        dockAndSurfaceFlow,
        miscFlow,
        typographyAndActionFlow
    ) { (theme, wallpaper), glance, (dock, surface), misc, (headerFontFamily, appLabelScale, autoCheckUpdates, action) ->
        SettingsUiState(
            themeMode = theme.themeMode,
            drawerThemeMode = theme.drawerThemeMode,
            drawerLiquidGlass = theme.drawerLiquidGlass,
            drawerLook = theme.drawerLook,
            dynamicColor = theme.dynamicColor,
            accentColor = theme.accentColor,
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
            blurAppListOnOpen = theme.blurAppListOnOpen,
            appDrawerColumns = theme.appDrawerColumns,
            enableWidgetsPage = theme.enableWidgetsPage,
            showDrawerSuggestions = theme.showDrawerSuggestions,
            hapticFeedbackEnabled = theme.hapticFeedbackEnabled,
            surfaceThemeStyle = surface.surfaceThemeStyle,
            surfaceOpacity = surface.surfaceOpacity,
            surfaceBlurRadius = surface.surfaceBlurRadius,
            surfaceStrokeOpacity = surface.surfaceStrokeOpacity,
            surfaceCornerRadius = surface.surfaceCornerRadius,
            matchDeviceScreenCurve = surface.matchDeviceScreenCurve,
            deviceCurveOffsetDp = surface.deviceCurveOffsetDp,
            cinematicPalette = surface.cinematicPalette,
            dockBackground = surface.surfaceThemeStyle,
            dockIconSize = dock.dockIconSize,
            dockShowLabels = dock.dockShowLabels,
            dockCornerRadius = surface.surfaceCornerRadius,
            dockExpandedMaxApps = dock.dockExpandedMaxApps,
            multiGroupApps = misc.multiGroupApps,
            notificationsPrivacyMode = misc.notificationsPrivacyMode,
            notificationSwipeRightAction = misc.notificationSwipeRightAction,
            notificationSwipeLeftAction = misc.notificationSwipeLeftAction,
            activeIconPack = misc.activeIconPack,
            adaptiveIconShape = misc.adaptiveIconShape,
            twoDrawersSideBySide = misc.twoDrawersSideBySide,
            lockLayout = misc.lockLayout,
            autoArrangeApps = misc.autoArrangeApps,
            hideDrawerIcons = misc.hideDrawerIcons,
            headerFontFamily = headerFontFamily,
            appLabelScale = appLabelScale,
            openDrawerFromDockOnly = dock.openDrawerFromDockOnly,
            duoStatusPosition = glance.duoStatusPosition,
            isBackingUp = action.isBackingUp,
            isRestoring = action.isRestoring,
            isCheckingForUpdate = action.isCheckingForUpdate,
            updateInfo = action.updateInfo,
            userMessage = action.userMessage,
            stealthTheme = misc.stealthTheme,
            stealthContrastLevel = misc.stealthContrastLevel,
            stealthLimitBrightness = misc.stealthLimitBrightness,
            drawerNotificationGlow = misc.drawerNotificationGlow,
            autoCategorizeNewApps = misc.autoCategorizeNewApps,
            aiCategorizationStrictness = misc.aiCategorizationStrictness,
            aiCategorizeAllApps = misc.aiCategorizeAllApps,
            aiForceWorkProfile = misc.aiForceWorkProfile,
            dynamicWallpaperEnabled = wallpaper.dynamicWallpaperEnabled,
            wallpaperGradientSpeedSeconds = wallpaper.wallpaperGradientSpeedSeconds,
            wallpaperGradientBlurPercent = wallpaper.wallpaperGradientBlurPercent,
            wallpaperGradientPalette = wallpaper.wallpaperGradientPalette,
            wallpaperGradientAngle = wallpaper.wallpaperGradientAngle,
            dockMesmerizingGlow = wallpaper.dockMesmerizingGlow,
            drawerTitleAnimation = misc.drawerTitleAnimation,
            autoCheckUpdates = autoCheckUpdates
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAutoCheckUpdates(enabled)
        }
    }

    fun setAiCategorizationStrictness(strictness: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAiCategorizationStrictness(strictness)
        }
    }

    fun setAiCategorizeAllApps(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAiCategorizeAllApps(enabled)
        }
    }

    fun setAiForceWorkProfile(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAiForceWorkProfile(enabled)
        }
    }

    fun setDynamicWallpaperEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDynamicWallpaperEnabled(enabled)
        }
    }

    fun setWallpaperGradientSpeedSeconds(seconds: Int) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWallpaperGradientSpeedSeconds(seconds)
        }
    }

    fun setWallpaperGradientBlurPercent(percent: Int) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWallpaperGradientBlurPercent(percent)
        }
    }

    fun setWallpaperGradientPalette(palette: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWallpaperGradientPalette(palette)
        }
    }

    fun setWallpaperGradientAngle(angle: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setWallpaperGradientAngle(angle)
        }
    }

    fun setDockMesmerizingGlow(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDockMesmerizingGlow(enabled)
        }
    }

    fun setOpenDrawerFromDockOnly(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setOpenDrawerFromDockOnly(enabled)
        }
    }

    fun setDuoStatusPosition(position: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDuoStatusPosition(position)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setHapticFeedbackEnabled(enabled)
        }
    }

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

    fun setDrawerLiquidGlass(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDrawerLiquidGlass(enabled)
        }
    }

    fun setDrawerLook(look: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDrawerLook(look)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDynamicColor(enabled)
        }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAccentColor(color)
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

    fun setMatchDeviceScreenCurve(match: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setMatchDeviceScreenCurve(match)
        }
    }

    fun setDeviceCurveOffsetDp(offset: Float) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDeviceCurveOffsetDp(offset)
        }
    }

    fun setCinematicPalette(palette: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setCinematicPalette(palette)
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

    fun setDockExpandedMaxApps(maxApps: Int) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDockExpandedMaxApps(maxApps)
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

    fun setAutoCategorizeNewApps(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAutoCategorizeNewApps(enabled)
        }
    }

    fun setNotificationsPrivacyMode(privacy: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setNotificationsPrivacyMode(privacy)
        }
    }

    fun setNotificationSwipeRightAction(action: NotificationSwipeAction) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setNotificationSwipeRightAction(action)
        }
    }

    fun setNotificationSwipeLeftAction(action: NotificationSwipeAction) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setNotificationSwipeLeftAction(action)
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

    fun setDrawerTitleAnimation(mode: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDrawerTitleAnimation(mode)
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

    fun setHideDrawerIcons(hide: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setHideDrawerIcons(hide)
        }
    }

    fun setStealthTheme(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setStealthTheme(enabled)
        }
    }

    fun setStealthContrastLevel(level: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setStealthContrastLevel(level)
        }
    }

    fun setStealthLimitBrightness(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setStealthLimitBrightness(enabled)
        }
    }

    fun setDrawerNotificationGlow(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setDrawerNotificationGlow(enabled)
        }
    }

    fun setAutoOpenKeyboardInDrawer(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAutoOpenKeyboardInDrawer(enabled)
        }
    }

    fun setBlurAppListOnOpen(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setBlurAppListOnOpen(enabled)
        }
    }

    fun setAppDrawerColumns(columns: Int) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAppDrawerColumns(columns)
        }
    }

    fun setShowDrawerSuggestions(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setShowDrawerSuggestions(enabled)
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

    fun setHeaderFontFamily(fontFamily: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setHeaderFontFamily(fontFamily)
        }
    }

    fun setAppLabelScale(scale: String) {
        viewModelScope.launch(ioDispatcher) {
            preferencesDataSource.setAppLabelScale(scale)
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
