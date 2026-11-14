package com.urunkarpm.drawer.feature.home

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.UpdateRepository
import com.urunkarpm.drawer.core.data.repository.WeatherRepository
import com.urunkarpm.drawer.core.data.prediction.AppPredictor
import com.urunkarpm.drawer.core.model.AppUpdateInfo
import com.urunkarpm.drawer.core.model.AppUsageProfile
import com.urunkarpm.drawer.core.data.util.DevicePostureUtils
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppShortcutInfo
import com.urunkarpm.drawer.core.model.DockCapacityUtils
import com.urunkarpm.drawer.core.model.WeatherInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.urunkarpm.drawer.core.designsystem.component.AppIconCache
import com.urunkarpm.drawer.core.designsystem.component.toImageBitmapSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlancePrefs(
    val is24Hour: Boolean = true,
    val showWeather: Boolean = true,
    val weatherUnit: String = "CELSIUS",
    val hideStatusBar: Boolean = true,
    val adaptiveIconShape: String = "CIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val drawerThemeMode: String = "SYSTEM",
    val drawerLiquidGlass: Boolean = false,
    val autoOpenKeyboardInDrawer: Boolean = false,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true,
    val categoryAlignment: String = "BOTTOM",
    val enableWidgetsPage: Boolean = true,
    val headerFontFamily: String = "SYSTEM",
    val appLabelScale: String = "NORMAL",
    val openDrawerFromDockOnly: Boolean = false,
    val duoStatusPosition: String = "TEMP",
    val hasCompletedWalkthrough: Boolean = true,
    val hideDrawerIcons: Boolean = false,
    val stealthTheme: Boolean = false,
    val stealthContrastLevel: String = "LOW",
    val stealthLimitBrightness: Boolean = false,
    val dynamicWallpaperEnabled: Boolean = false,
    val wallpaperGradientSpeedSeconds: Int = 3,
    val wallpaperGradientBlurPercent: Int = 40,
    val wallpaperGradientPalette: String = "FULL_SPECTRUM",
    val wallpaperGradientAngle: String = "DIAGONAL",
    val dockMesmerizingGlow: Boolean = true,
    val drawerTitleAnimation: String = "ACCENT_POP",
    val blurAppListOnOpen: Boolean = true,
    val appDrawerColumns: Int = 4
)

data class HomeDialogsState(
    val searchQuery: String = "",
    val isAllAppsOpen: Boolean = false,
    val selectedAppForMenu: AppInfo? = null,
    val shortcuts: List<AppShortcutInfo> = emptyList(),
    val userMessage: String? = null,
    val pendingMindfulApp: AppInfo? = null,
    val mindfulApps: Set<String> = emptySet()
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
    val adaptiveIconShape: String = "CIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val drawerThemeMode: String = "SYSTEM",
    val drawerLiquidGlass: Boolean = false,
    val autoOpenKeyboardInDrawer: Boolean = false,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true,
    val categoryAlignment: String = "BOTTOM",
    val enableWidgetsPage: Boolean = true,
    val widgets: List<com.urunkarpm.drawer.core.model.WidgetItem> = emptyList(),
    val widgetDrawers: List<com.urunkarpm.drawer.core.model.WidgetDrawer> = emptyList(),
    val standaloneWidgets: List<com.urunkarpm.drawer.core.model.WidgetItem> = emptyList(),
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f,
    val cinematicPalette: String = "DEFAULT",
    val drawerLook: String = "SOLID",
    val matchDeviceScreenCurve: Boolean = true,
    val deviceCurveOffsetDp: Float = 0f,
    val headerFontFamily: String = "SYSTEM",
    val appLabelScale: String = "NORMAL",
    val openDrawerFromDockOnly: Boolean = false,
    val duoStatusPosition: String = "TEMP",
    val hasCompletedWalkthrough: Boolean = true,
    val hideDrawerIcons: Boolean = false,
    val stealthTheme: Boolean = false,
    val stealthContrastLevel: String = "LOW",
    val stealthLimitBrightness: Boolean = false,
    val dynamicWallpaperEnabled: Boolean = false,
    val wallpaperGradientSpeedSeconds: Int = 3,
    val wallpaperGradientBlurPercent: Int = 40,
    val wallpaperGradientPalette: String = "FULL_SPECTRUM",
    val wallpaperGradientAngle: String = "DIAGONAL",
    val dockMesmerizingGlow: Boolean = true,
    val drawerTitleAnimation: String = "ACCENT_POP",
    val blurAppListOnOpen: Boolean = true,
    val appDrawerColumns: Int = 4,
    val mindfulApps: Set<String> = emptySet(),
    val pendingMindfulApp: AppInfo? = null,
    val isVaultOpen: Boolean = false,
    val hiddenAppPackages: Set<String> = emptySet(),
    val hiddenApps: List<AppInfo> = emptyList(),
    val rawInstalledApps: List<AppInfo> = emptyList(),
    val hiddenNotifUnlockCount: Int = 0
)

data class SurfaceThemePrefs(
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f,
    val cinematicPalette: String = "DEFAULT",
    val drawerLook: String = "SOLID",
    val matchDeviceScreenCurve: Boolean = true,
    val deviceCurveOffsetDp: Float = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val dockRepository: DockRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val widgetRepository: com.urunkarpm.drawer.core.data.repository.WidgetRepository,
    private val appPredictor: AppPredictor = AppPredictor(),
    @param:ApplicationContext private val context: Context? = null,
    private val widgetHostManager: DrawerWidgetHostManager? = null,
    private val todoRepository: com.urunkarpm.drawer.core.data.repository.TodoRepository? = null,
    private val updateRepository: UpdateRepository? = null
) : ViewModel() {

    val staleTodoCount: StateFlow<Int> = if (todoRepository != null) {
        todoRepository.observeOpenTodos()
            .map { todos ->
                val now = System.currentTimeMillis()
                todos.count { com.urunkarpm.drawer.core.common.util.TodoUtils.isStale(it, now) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    } else {
        MutableStateFlow(0)
    }

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _isAllAppsOpen = MutableStateFlow(false)
    val isAllAppsOpen: StateFlow<Boolean> = _isAllAppsOpen.asStateFlow()
    private val _selectedAppForMenu = MutableStateFlow<AppInfo?>(null)
    private val _shortcuts = MutableStateFlow<List<AppShortcutInfo>>(emptyList())
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _pendingMindfulApp = MutableStateFlow<AppInfo?>(null)
    private val _suggestedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val suggestedApps: StateFlow<List<AppInfo>> = _suggestedApps.asStateFlow()
    // ponytail: Immediate in-memory optimistic tracking for instantaneous 0ms widget drawer collapse & expand
    private val _expandedWidgetDrawerIds = MutableStateFlow<Set<String>?>(null)
    private val _isVaultOpen = MutableStateFlow(false)
    val isVaultOpen: StateFlow<Boolean> = _isVaultOpen.asStateFlow()

    private val vaultPrefsFlow = combine(
        preferencesDataSource.hiddenAppPackages,
        preferencesDataSource.hiddenNotifUnlockCount,
        _isVaultOpen
    ) { hiddenPackages, unlockCount, isVaultOpen ->
        Triple(hiddenPackages, unlockCount, isVaultOpen)
    }

    val showDrawerSuggestions: StateFlow<Boolean> = preferencesDataSource.showDrawerSuggestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoArrangeApps: StateFlow<Boolean> = preferencesDataSource.autoArrangeApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dockShowLabels: StateFlow<Boolean> = preferencesDataSource.dockShowLabels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = preferencesDataSource.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dynamicColor: StateFlow<Boolean> = preferencesDataSource.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val accentColor: StateFlow<String> = preferencesDataSource.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DYNAMIC")

    val dockIconSize: StateFlow<Float> = preferencesDataSource.dockIconSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 56f)

    val multiGroupApps: StateFlow<Boolean> = preferencesDataSource.multiGroupApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockLayout: StateFlow<Boolean> = preferencesDataSource.lockLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoCategorizeNewApps: StateFlow<Boolean> = preferencesDataSource.autoCategorizeNewApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var currentProfiles: Map<String, AppUsageProfile> = emptyMap()
    private var currentBlacklist: Set<String> = emptySet()
    private var currentMindfulApps: Set<String> = emptySet()
    private var currentDockedPackages: Set<String> = emptySet()
    private var currentInstalledApps: List<AppInfo> = emptyList()

    private val glancePrefsFlow = combine<Any, GlancePrefs>(
        preferencesDataSource.is24Hour,
        preferencesDataSource.showWeather,
        preferencesDataSource.weatherUnit,
        preferencesDataSource.hideStatusBar,
        preferencesDataSource.adaptiveIconShape,
        preferencesDataSource.twoDrawersSideBySide,
        preferencesDataSource.wallpaperBlur,
        preferencesDataSource.wallpaperBlurRadius,
        preferencesDataSource.drawerThemeMode,
        preferencesDataSource.drawerLiquidGlass,
        preferencesDataSource.autoOpenKeyboardInDrawer,
        preferencesDataSource.showDuoStatusWidget,
        preferencesDataSource.enableCameraMirror,
        preferencesDataSource.categoryAlignment,
        preferencesDataSource.enableWidgetsPage,
        preferencesDataSource.headerFontFamily,
        preferencesDataSource.appLabelScale,
        preferencesDataSource.openDrawerFromDockOnly,
        preferencesDataSource.duoStatusPosition,
        preferencesDataSource.hasCompletedWalkthrough,
        preferencesDataSource.hideDrawerIcons,
        preferencesDataSource.stealthTheme,
        preferencesDataSource.stealthContrastLevel,
        preferencesDataSource.stealthLimitBrightness,
        preferencesDataSource.dynamicWallpaperEnabled,
        preferencesDataSource.wallpaperGradientSpeedSeconds,
        preferencesDataSource.wallpaperGradientBlurPercent,
        preferencesDataSource.wallpaperGradientPalette,
        preferencesDataSource.wallpaperGradientAngle,
        preferencesDataSource.dockMesmerizingGlow,
        preferencesDataSource.drawerTitleAnimation,
        preferencesDataSource.blurAppListOnOpen,
        preferencesDataSource.appDrawerColumns
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
        val drawerThemeMode = values[8] as String
        val drawerLiquidGlass = values[9] as Boolean
        val autoOpenKeyboardInDrawer = values[10] as Boolean
        val showDuoStatusWidget = values[11] as Boolean
        val enableCameraMirror = values[12] as Boolean
        val categoryAlignment = values[13] as String
        val enableWidgetsPage = values[14] as Boolean
        val headerFontFamily = values[15] as String
        val appLabelScale = values[16] as String
        val openDrawerFromDockOnly = values[17] as Boolean
        val duoStatusPosition = values[18] as String
        val hasCompletedWalkthrough = values[19] as Boolean
        val hideDrawerIcons = values[20] as Boolean
        val stealthTheme = values[21] as Boolean
        val stealthContrastLevel = values[22] as String
        val stealthLimitBrightness = values[23] as Boolean
        val dynamicWallpaperEnabled = values[24] as Boolean
        val wallpaperGradientSpeedSeconds = values[25] as Int
        val wallpaperGradientBlurPercent = values[26] as Int
        val wallpaperGradientPalette = values[27] as String
        val wallpaperGradientAngle = values[28] as String
        val dockMesmerizingGlow = values[29] as Boolean
        val drawerTitleAnimation = values[30] as String
        val blurAppListOnOpen = values[31] as Boolean
        val appDrawerColumns = values[32] as Int
        GlancePrefs(
            is24Hour,
            showWeather,
            weatherUnit,
            hideStatusBar,
            adaptiveIconShape,
            twoDrawersSideBySide,
            wallpaperBlur,
            wallpaperBlurRadius,
            drawerThemeMode,
            drawerLiquidGlass,
            autoOpenKeyboardInDrawer,
            showDuoStatusWidget,
            enableCameraMirror,
            categoryAlignment,
            enableWidgetsPage,
            headerFontFamily,
            appLabelScale,
            openDrawerFromDockOnly,
            duoStatusPosition,
            hasCompletedWalkthrough,
            hideDrawerIcons,
            stealthTheme,
            stealthContrastLevel,
            stealthLimitBrightness,
            dynamicWallpaperEnabled,
            wallpaperGradientSpeedSeconds,
            wallpaperGradientBlurPercent,
            wallpaperGradientPalette,
            wallpaperGradientAngle,
            dockMesmerizingGlow,
            drawerTitleAnimation,
            blurAppListOnOpen,
            appDrawerColumns
        )
    }

    private val homeDialogsFlow = combine(
        _searchQuery,
        _isAllAppsOpen,
        _selectedAppForMenu,
        _shortcuts,
        combine(_userMessage, _pendingMindfulApp, preferencesDataSource.mindfulApps) { msg, pending, mindful ->
            Triple(msg, pending, mindful)
        }
    ) { searchQuery, isAllAppsOpen, selectedApp, shortcuts, (userMessage, pendingMindful, mindful) ->
        HomeDialogsState(
            searchQuery = searchQuery,
            isAllAppsOpen = isAllAppsOpen,
            selectedAppForMenu = selectedApp,
            shortcuts = shortcuts,
            userMessage = userMessage,
            pendingMindfulApp = pendingMindful,
            mindfulApps = mindful
        )
    }

    private val surfacePrefsFlow = combine(
        combine(
            preferencesDataSource.surfaceThemeStyle,
            preferencesDataSource.surfaceCornerRadius,
            preferencesDataSource.cinematicPalette,
            preferencesDataSource.drawerLook
        ) { theme, cornerRadius, palette, look -> listOf(theme, cornerRadius, palette, look) },
        combine(
            preferencesDataSource.surfaceOpacity,
            preferencesDataSource.surfaceBlurRadius,
            preferencesDataSource.surfaceStrokeOpacity,
            preferencesDataSource.matchDeviceScreenCurve,
            preferencesDataSource.deviceCurveOffsetDp
        ) { opacity, blurRadius, strokeOpacity, matchCurve, offsetDp ->
            listOf(opacity, blurRadius, strokeOpacity, matchCurve, offsetDp)
        }
    ) { surface1, surface2 ->
        SurfaceThemePrefs(
            surfaceThemeStyle = surface1[0] as String,
            surfaceCornerRadius = surface1[1] as Float,
            cinematicPalette = surface1[2] as String,
            drawerLook = surface1[3] as String,
            surfaceOpacity = surface2[0] as Float,
            surfaceBlurRadius = surface2[1] as Float,
            surfaceStrokeOpacity = surface2[2] as Float,
            matchDeviceScreenCurve = surface2[3] as Boolean,
            deviceCurveOffsetDp = surface2[4] as Float
        )
    }

    private var cachedAppsRef: List<AppInfo>? = null
    private var cachedAppsByPkg: Map<String, AppInfo> = emptyMap()

    var pendingAddTargetDrawerId: String? = null
    var pendingAddAppWidgetId: Int? = null

    val appLaunchCounts: StateFlow<Map<String, Int>> = preferencesDataSource.appLaunchCounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val appLastLaunchedTimes: StateFlow<Map<String, Long>> = preferencesDataSource.appLastLaunchedTimes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap()
        )

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.installedApps,
        weatherRepository.weatherInfo,
        widgetRepository.getAllDrawers(),
        widgetRepository.getAllWidgets(),
        glancePrefsFlow,
        homeDialogsFlow,
        surfacePrefsFlow,
        _expandedWidgetDrawerIds,
        vaultPrefsFlow
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val apps = args[0] as List<AppInfo>
        val weather = args[1] as? WeatherInfo
        @Suppress("UNCHECKED_CAST")
        val rawDrawersList = args[2] as List<com.urunkarpm.drawer.core.model.WidgetDrawer>
        @Suppress("UNCHECKED_CAST")
        val widgetsList = args[3] as List<com.urunkarpm.drawer.core.model.WidgetItem>
        val glancePrefs = args[4] as GlancePrefs
        val dialogs = args[5] as HomeDialogsState
        val surfacePrefs = args[6] as SurfaceThemePrefs
        @Suppress("UNCHECKED_CAST")
        val expandedDrawerOverride = args[7] as? Set<String>
        @Suppress("UNCHECKED_CAST")
        val vaultPrefs = args[8] as Triple<Set<String>, Int, Boolean>
        val hiddenPackages = vaultPrefs.first
        val unlockCount = vaultPrefs.second
        val isVaultOpen = vaultPrefs.third

        val drawersList = rawDrawersList.map { drawer ->
            if (expandedDrawerOverride != null) {
                val isExp = expandedDrawerOverride.contains(drawer.id)
                if (drawer.isExpanded != isExp) drawer.copy(isExpanded = isExp) else drawer
            } else {
                drawer
            }
        }

        val standalone = widgetsList.filter { it.drawerId == null }

        val visibleApps = apps.filter { it.packageName !in hiddenPackages }
        val hiddenApps = apps.filter { it.packageName in hiddenPackages }

        val filtered = if (dialogs.searchQuery.isBlank()) {
            visibleApps
        } else {
            visibleApps.filter { it.label.contains(dialogs.searchQuery, ignoreCase = true) }
        }
        val appsByPkg = if (cachedAppsRef === visibleApps) {
            cachedAppsByPkg
        } else {
            cachedAppsRef = visibleApps
            visibleApps.associateBy { it.packageName }.also { cachedAppsByPkg = it }
        }
        HomeUiState(
            installedApps = visibleApps,
            installedAppsByPackage = appsByPkg,
            filteredApps = filtered,
            rawInstalledApps = apps,
            hiddenApps = hiddenApps,
            hiddenAppPackages = hiddenPackages,
            isVaultOpen = isVaultOpen,
            hiddenNotifUnlockCount = unlockCount,
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
            wallpaperBlurRadius = glancePrefs.wallpaperBlurRadius,
            drawerThemeMode = glancePrefs.drawerThemeMode,
            drawerLiquidGlass = glancePrefs.drawerLiquidGlass,
            autoOpenKeyboardInDrawer = glancePrefs.autoOpenKeyboardInDrawer,
            showDuoStatusWidget = glancePrefs.showDuoStatusWidget,
            enableCameraMirror = glancePrefs.enableCameraMirror,
            categoryAlignment = glancePrefs.categoryAlignment,
            enableWidgetsPage = glancePrefs.enableWidgetsPage,
            widgets = widgetsList,
            widgetDrawers = drawersList,
            standaloneWidgets = standalone,
            surfaceThemeStyle = surfacePrefs.surfaceThemeStyle,
            surfaceOpacity = surfacePrefs.surfaceOpacity,
            surfaceBlurRadius = surfacePrefs.surfaceBlurRadius,
            surfaceStrokeOpacity = surfacePrefs.surfaceStrokeOpacity,
            surfaceCornerRadius = surfacePrefs.surfaceCornerRadius,
            cinematicPalette = surfacePrefs.cinematicPalette,
            drawerLook = surfacePrefs.drawerLook,
            matchDeviceScreenCurve = surfacePrefs.matchDeviceScreenCurve,
            deviceCurveOffsetDp = surfacePrefs.deviceCurveOffsetDp,
            headerFontFamily = glancePrefs.headerFontFamily,
            appLabelScale = glancePrefs.appLabelScale,
            openDrawerFromDockOnly = glancePrefs.openDrawerFromDockOnly,
            duoStatusPosition = glancePrefs.duoStatusPosition,
            hasCompletedWalkthrough = glancePrefs.hasCompletedWalkthrough,
            hideDrawerIcons = glancePrefs.hideDrawerIcons,
            stealthTheme = glancePrefs.stealthTheme,
            stealthContrastLevel = glancePrefs.stealthContrastLevel,
            stealthLimitBrightness = glancePrefs.stealthLimitBrightness,
            dynamicWallpaperEnabled = glancePrefs.dynamicWallpaperEnabled,
            wallpaperGradientSpeedSeconds = glancePrefs.wallpaperGradientSpeedSeconds,
            wallpaperGradientBlurPercent = glancePrefs.wallpaperGradientBlurPercent,
            wallpaperGradientPalette = glancePrefs.wallpaperGradientPalette,
            wallpaperGradientAngle = glancePrefs.wallpaperGradientAngle,
            dockMesmerizingGlow = glancePrefs.dockMesmerizingGlow,
            drawerTitleAnimation = glancePrefs.drawerTitleAnimation,
            blurAppListOnOpen = glancePrefs.blurAppListOnOpen,
            appDrawerColumns = glancePrefs.appDrawerColumns,
            mindfulApps = dialogs.mindfulApps,
            pendingMindfulApp = dialogs.pendingMindfulApp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        collapseAllWidgetDrawers()
        refreshWeather()
        // ponytail: self-heals dangling widget IDs (e.g. from restored backups or system process death) by checking AppWidgetManager and rebinding live providers.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingWidgets = widgetRepository.getAllWidgets().first()
                existingWidgets.forEach { widget ->
                    val info = try {
                        widgetHostManager?.appWidgetManager?.getAppWidgetInfo(widget.appWidgetId)
                    } catch (_: Exception) {
                        null
                    }
                    if (info == null && widgetHostManager != null) {
                        val resolvedId = widgetHostManager.rebindOrRestoreWidget(
                            packageName = widget.packageName,
                            providerClassName = widget.providerClassName,
                            existingAppWidgetId = widget.appWidgetId,
                            spanX = widget.spanX,
                            heightDp = widget.heightDp
                        )
                        if (resolvedId != widget.appWidgetId) {
                            widgetRepository.updateWidget(widget.copy(appWidgetId = resolvedId))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            preferencesDataSource.appUsageProfiles.collect {
                currentProfiles = it
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            preferencesDataSource.suggestionBlacklist.collect {
                currentBlacklist = it
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            preferencesDataSource.mindfulApps.collect {
                currentMindfulApps = it
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            dockRepository.dockItems.collect { dockItems ->
                currentDockedPackages = dockItems.map { it.packageName }.toSet()
            }
        }
        viewModelScope.launch(Dispatchers.Unconfined) {
            appRepository.installedApps.collect { apps ->
                currentInstalledApps = apps
            }
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.installedApps.collect { apps ->
                for (app in apps) {
                    if (AppIconCache.get(app.componentKey) == null) {
                        val d = appRepository.getAppIcon(app)
                        val bmp = d?.toImageBitmapSafe()
                        if (bmp != null) {
                            AppIconCache.put(app.componentKey, bmp)
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

    fun checkForUpdateOnStartup(installedVersion: String) {
        val repo = updateRepository ?: return
        viewModelScope.launch {
            val autoCheck = preferencesDataSource.autoCheckUpdates.first()
            if (!autoCheck) return@launch

            val lastCheck = preferencesDataSource.lastUpdateCheckTime.first()
            val now = System.currentTimeMillis()
            if (now - lastCheck < 24 * 60 * 60 * 1000L && now >= lastCheck) {
                return@launch
            }

            preferencesDataSource.setLastUpdateCheckTime(now)

            val result = repo.checkForUpdates(installedVersion)
            result.onSuccess { info ->
                if (info.isUpdateAvailable) {
                    val dismissed = preferencesDataSource.dismissedUpdateVersion.first()
                    if (info.latestVersionTag != dismissed) {
                        _availableUpdate.value = info
                    }
                }
            }
        }
    }

    fun dismissUpdate(versionTag: String) {
        _availableUpdate.value = null
        viewModelScope.launch {
            preferencesDataSource.setDismissedUpdateVersion(versionTag)
        }
    }

    fun pinToDock(app: AppInfo) {
        viewModelScope.launch {
            val isFoldableOrTab = context != null && DevicePostureUtils.isFoldableOrTablet(context)
            val iconSize = preferencesDataSource.dockIconSize.first()
            val expandedPref = preferencesDataSource.dockExpandedMaxApps.first()
            val maxLimit = DockCapacityUtils.calculateCapacity(iconSize, isFoldableOrTab, expandedPref)
            val added = dockRepository.addToDock(app, maxLimit)
            _userMessage.value = if (added) {
                "${app.label} pinned to dock"
            } else {
                "Cannot pin to dock: full (max $maxLimit) or already in dock"
            }
            dismissAppMenu()
        }
    }

    fun unpinFromDock(app: AppInfo) {
        viewModelScope.launch {
            removeAppFromDock(app)
            dismissAppMenu()
        }
    }

    suspend fun addAppToDock(app: AppInfo): Boolean {
        return dockRepository.addToDock(app)
    }

    suspend fun removeAppFromDock(app: AppInfo): Boolean {
        val items = dockRepository.dockItems.first()
        val item = items.find { it.packageName == app.packageName && (app.activityName.isBlank() || it.activityName == app.activityName) }
            ?: items.find { it.packageName == app.packageName }
        return if (item != null) {
            dockRepository.removeFromDock(item.position)
            true
        } else {
            false
        }
    }

    suspend fun changeAppSetting(setting: String, value: String?): String {
        val currentUiState = uiState.value
        val upperSetting = setting.uppercase().trim()
        val lowerVal = value?.lowercase()?.trim() ?: "toggle"

        fun resolveBoolean(current: Boolean): Boolean {
            return when (lowerVal) {
                "true", "on", "enable", "enabled", "show", "lock" -> true
                "false", "off", "disable", "disabled", "hide", "unlock" -> false
                else -> !current
            }
        }

        return when (upperSetting) {
            "STEALTH_THEME", "STEALTH" -> {
                val next = resolveBoolean(currentUiState.stealthTheme)
                preferencesDataSource.setStealthTheme(next)
                if (next) "Stealth theme enabled" else "Stealth theme disabled"
            }
            "LOCK_LAYOUT", "LOCK" -> {
                val next = when (lowerVal) {
                    "true", "on", "enable", "enabled", "lock" -> true
                    "false", "off", "disable", "disabled", "unlock" -> false
                    else -> !kotlin.runCatching { preferencesDataSource.lockLayout.first() }.getOrDefault(false)
                }
                preferencesDataSource.setLockLayout(next)
                if (next) "Layout locked" else "Layout unlocked"
            }
            "DYNAMIC_WALLPAPER" -> {
                val next = resolveBoolean(currentUiState.dynamicWallpaperEnabled)
                preferencesDataSource.setDynamicWallpaperEnabled(next)
                if (next) "Dynamic wallpaper enabled" else "Dynamic wallpaper disabled"
            }
            "HIDE_STATUS_BAR", "STATUS_BAR" -> {
                val next = resolveBoolean(currentUiState.hideStatusBar)
                preferencesDataSource.setHideStatusBar(next)
                if (next) "Status bar hidden" else "Status bar visible"
            }
            "TWO_DRAWERS" -> {
                val next = resolveBoolean(currentUiState.twoDrawersSideBySide)
                preferencesDataSource.setTwoDrawersSideBySide(next)
                if (next) "Two-drawer side-by-side mode enabled" else "Single column drawer mode enabled"
            }
            "HIDE_DRAWER_ICONS" -> {
                val next = resolveBoolean(currentUiState.hideDrawerIcons)
                preferencesDataSource.setHideDrawerIcons(next)
                if (next) "Drawer icons hidden" else "Drawer icons visible"
            }
            "CAMERA_MIRROR" -> {
                val next = resolveBoolean(currentUiState.enableCameraMirror)
                preferencesDataSource.setEnableCameraMirror(next)
                if (next) "Camera mirror enabled" else "Camera mirror disabled"
            }
            "DUO_STATUS" -> {
                val next = resolveBoolean(currentUiState.showDuoStatusWidget)
                preferencesDataSource.setShowDuoStatusWidget(next)
                if (next) "Status widget shown" else "Status widget hidden"
            }
            "THEME_MODE" -> {
                val mode = when (value?.uppercase()?.trim()) {
                    "DARK" -> "DARK"
                    "LIGHT" -> "LIGHT"
                    else -> "SYSTEM"
                }
                preferencesDataSource.setThemeMode(mode)
                preferencesDataSource.setDrawerThemeMode(mode)
                "Theme set to ${mode.lowercase().replaceFirstChar { it.uppercase() }}"
            }
            "ICON_SHAPE" -> {
                val shape = value?.uppercase()?.trim() ?: "CIRCLE"
                preferencesDataSource.setAdaptiveIconShape(shape)
                "Icon shape set to ${shape.lowercase().replace("_", " ")}"
            }
            "WEATHER" -> {
                val next = resolveBoolean(currentUiState.showWeather)
                preferencesDataSource.setShowWeather(next)
                if (next) "Weather shown" else "Weather hidden"
            }
            "24_HOUR" -> {
                val next = resolveBoolean(currentUiState.is24Hour)
                preferencesDataSource.setIs24Hour(next)
                if (next) "24-hour time enabled" else "12-hour time enabled"
            }
            else -> "Setting updated"
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

    private var lastLaunchedPackage: String? = null
    private var lastLaunchTimestamp: Long = 0L

    fun onSearchQueryChange(query: String) = onSearchQueryChanged(query)

    fun refreshSuggestions(
        dockedPackages: Set<String> = currentDockedPackages,
        activeNotificationPackages: Set<String> = emptySet()
    ) {
        val installed = currentInstalledApps.ifEmpty { uiState.value.installedApps }
        val now = System.currentTimeMillis()
        val recentExited = if (now - lastLaunchTimestamp <= 60_000L) lastLaunchedPackage else null
        _suggestedApps.value = appPredictor.predictTopApps(
            installedApps = installed,
            usageProfiles = currentProfiles,
            dockedPackages = dockedPackages,
            blacklistedPackages = currentBlacklist,
            activeNotificationPackages = activeNotificationPackages,
            lastExitedPackage = recentExited,
            currentTimeMs = now
        )
    }

    fun freezeSuggestionsForDrawerOpen(
        dockedPackages: Set<String> = currentDockedPackages,
        activeNotificationPackages: Set<String> = emptySet()
    ) = refreshSuggestions(dockedPackages, activeNotificationPackages)

    fun hideFromSuggestions(packageName: String) {
        currentBlacklist = currentBlacklist + packageName
        _suggestedApps.value = _suggestedApps.value.filter { it.packageName != packageName }
        viewModelScope.launch {
            preferencesDataSource.hideAppFromSuggestions(packageName)
            refreshSuggestions()
        }
    }

    fun hideAppFromSuggestions(packageName: String) = hideFromSuggestions(packageName)

    fun openAllApps(activeNotificationPackages: Set<String> = emptySet()) {
        refreshSuggestions(activeNotificationPackages = activeNotificationPackages)
        _isAllAppsOpen.value = true
    }

    fun closeAllApps() {
        _isAllAppsOpen.value = false
        _searchQuery.value = ""
    }

    fun openVault() {
        _isVaultOpen.value = true
        viewModelScope.launch {
            preferencesDataSource.setHiddenNotifUnlockCount(0)
        }
    }

    fun closeVault() {
        _isVaultOpen.value = false
    }

    fun addHiddenApp(packageName: String) {
        viewModelScope.launch {
            preferencesDataSource.addHiddenApp(packageName)
        }
    }

    fun removeHiddenApp(packageName: String) {
        viewModelScope.launch {
            preferencesDataSource.removeHiddenApp(packageName)
        }
    }

    fun onUserUnlocked() {
        viewModelScope.launch {
            preferencesDataSource.decrementHiddenNotifUnlockCount()
        }
    }

    fun toggleAllApps(activeNotificationPackages: Set<String> = emptySet()) {
        if (_isAllAppsOpen.value) {
            closeAllApps()
        } else {
            openAllApps(activeNotificationPackages)
        }
    }

    fun onAppClicked(app: AppInfo, bypassMindful: Boolean = false): Boolean {
        if (!bypassMindful && currentMindfulApps.contains(app.packageName)) {
            _pendingMindfulApp.value = app
            return false
        }
        val launched = appRepository.launchApp(app)
        if (launched) {
            lastLaunchedPackage = app.packageName
            lastLaunchTimestamp = System.currentTimeMillis()
            if (_isAllAppsOpen.value) {
                closeAllApps()
            }
        }
        return launched
    }

    fun confirmMindfulLaunch() {
        val app = _pendingMindfulApp.value ?: return
        _pendingMindfulApp.value = null
        onAppClicked(app, bypassMindful = true)
    }

    fun cancelMindfulLaunch() {
        _pendingMindfulApp.value = null
    }

    fun toggleMindfulApp(packageName: String) {
        currentMindfulApps = if (currentMindfulApps.contains(packageName)) currentMindfulApps - packageName else currentMindfulApps + packageName
        viewModelScope.launch {
            preferencesDataSource.toggleMindfulApp(packageName)
        }
    }

    fun onAppLongClicked(app: AppInfo) {
        _selectedAppForMenu.value = app
        _shortcuts.value = emptyList()
        viewModelScope.launch {
            val shortcuts = withContext(Dispatchers.IO) {
                appRepository.getShortcuts(app)
            }
            if (_selectedAppForMenu.value == app) {
                _shortcuts.value = shortcuts
            }
        }
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

    fun addWidget(
        appWidgetId: Int,
        packageName: String,
        providerClassName: String,
        drawerId: String? = null,
        spanX: Int = 4,
        spanY: Int = 2,
        heightDp: Int = 180
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!uiState.value.enableWidgetsPage) {
                preferencesDataSource.setEnableWidgetsPage(true)
            }
            val nextOrder = if (drawerId != null) {
                uiState.value.widgetDrawers.find { it.id == drawerId }?.widgets?.size ?: 0
            } else {
                uiState.value.standaloneWidgets.size
            }
            val widget = com.urunkarpm.drawer.core.model.WidgetItem(
                id = java.util.UUID.randomUUID().toString(),
                appWidgetId = appWidgetId,
                packageName = packageName,
                providerClassName = providerClassName,
                drawerId = drawerId,
                orderIndex = nextOrder,
                spanX = spanX,
                spanY = spanY,
                heightDp = heightDp
            )
            widgetRepository.addWidget(widget)

            // Ensure target drawer is expanded and adequately sized so user sees their new widget immediately
            if (drawerId != null) {
                val targetDrawer = uiState.value.widgetDrawers.find { it.id == drawerId }
                if (targetDrawer != null) {
                    val updatedSpanY = maxOf(targetDrawer.spanY, spanY)
                    widgetRepository.updateDrawer(targetDrawer.copy(isExpanded = true, spanY = updatedSpanY))
                }
            }
        }
    }

    fun deleteWidget(id: String, appWidgetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val widgetToDelete = uiState.value.widgets.find { it.id == id }
            widgetRepository.deleteWidget(id, appWidgetId)
            val drawerId = widgetToDelete?.drawerId
            if (drawerId != null) {
                val drawer = uiState.value.widgetDrawers.find { it.id == drawerId }
                drawer?.widgets?.filter { it.id != id }?.forEachIndexed { i, w ->
                    widgetRepository.updateWidget(w.copy(orderIndex = i))
                }
            } else {
                uiState.value.standaloneWidgets.filter { it.id != id }.forEachIndexed { i, w ->
                    widgetRepository.updateWidget(w.copy(orderIndex = i))
                }
            }
        }
    }

    fun updateWidgetHeight(id: String, newHeightDp: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val widget = uiState.value.widgets.find { it.id == id } ?: return@launch
            widgetRepository.updateWidget(widget.copy(heightDp = newHeightDp.coerceIn(80, 600)))
        }
    }

    // ponytail: resizing a widget in a drawer synchronizes the drawer's spanY so the cover flow container visually adapts without requiring multi-dimensional per-item layout math; ceiling is uniform drawer height; upgrade path is per-widget dynamic card viewport scaling.
    fun updateWidgetSpan(id: String, spanX: Int, spanY: Int, heightDp: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val widget = uiState.value.widgets.find { it.id == id } ?: return@launch
            val computedHeight = heightDp ?: (spanY * 70)
            widgetRepository.updateWidget(
                widget.copy(
                    spanX = spanX.coerceIn(1, 4),
                    spanY = spanY.coerceIn(1, 6),
                    heightDp = computedHeight.coerceIn(80, 600)
                )
            )
            val drawerId = widget.drawerId
            if (drawerId != null) {
                val drawer = uiState.value.widgetDrawers.find { it.id == drawerId }
                if (drawer != null) {
                    widgetRepository.updateDrawer(drawer.copy(spanY = spanY.coerceIn(1, 6)))
                }
            }
        }
    }

    fun createWidgetDrawer(
        title: String,
        iconName: String = "folder",
        colorHex: String = "#1E88E5",
        spanX: Int = 4,
        spanY: Int = 3,
        onCreated: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerId = widgetRepository.createDrawer(title.trim(), iconName, colorHex, spanX, spanY)
            onCreated?.invoke(drawerId)
        }
    }

    fun createWidgetDrawer(
        title: String,
        spanX: Int,
        spanY: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            widgetRepository.createDrawer(title.trim(), spanX, spanY)
        }
    }

    fun updateWidgetDrawer(drawer: com.urunkarpm.drawer.core.model.WidgetDrawer) {
        viewModelScope.launch(Dispatchers.IO) {
            widgetRepository.updateDrawer(drawer)
        }
    }

    fun collapseAllWidgetDrawers() {
        // Immediate optimistic in-memory collapse (0ms latency)
        _expandedWidgetDrawerIds.value = emptySet()
        viewModelScope.launch(Dispatchers.IO) {
            val fromState = uiState.value.widgetDrawers.filter { it.isExpanded }
            val fromRepo = try {
                widgetRepository.getAllDrawers().first().filter { it.isExpanded }
            } catch (e: Exception) {
                emptyList()
            }
            val toCollapse = (fromState + fromRepo).distinctBy { it.id }
            toCollapse.forEach { drawer ->
                widgetRepository.updateDrawer(drawer.copy(isExpanded = false))
            }
        }
    }

    fun toggleWidgetDrawerExpand(drawerId: String) {
        // Immediate optimistic in-memory toggle (0ms latency)
        val current = _expandedWidgetDrawerIds.value ?: uiState.value.widgetDrawers.filter { it.isExpanded }.map { it.id }.toSet()
        _expandedWidgetDrawerIds.value = if (current.contains(drawerId)) current - drawerId else current + drawerId
        viewModelScope.launch(Dispatchers.IO) {
            val drawer = uiState.value.widgetDrawers.find { it.id == drawerId } ?: return@launch
            widgetRepository.updateDrawer(drawer.copy(isExpanded = !drawer.isExpanded))
        }
    }

    fun deleteWidgetDrawer(drawerId: String, deleteChildren: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            widgetRepository.deleteDrawer(drawerId, deleteChildren)
        }
    }

    fun moveWidgetToDrawer(widgetId: String, drawerId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetDrawer = if (drawerId != null) {
                uiState.value.widgetDrawers.find { it.id == drawerId }
            } else null
            val targetOrder = if (drawerId != null) {
                targetDrawer?.widgets?.size ?: 0
            } else {
                uiState.value.standaloneWidgets.size
            }
            val widget = uiState.value.widgets.find { it.id == widgetId }
            if (widget != null) {
                widgetRepository.updateWidget(widget.copy(drawerId = drawerId, orderIndex = targetOrder))
            } else {
                widgetRepository.assignWidgetToDrawer(widgetId, drawerId)
            }
            if (targetDrawer != null) {
                val widgetSpanY = widget?.spanY ?: 2
                val updatedSpanY = maxOf(targetDrawer.spanY, widgetSpanY)
                widgetRepository.updateDrawer(targetDrawer.copy(isExpanded = true, spanY = updatedSpanY))
            }
        }
    }

    fun reorderWidgetsInDrawer(drawerId: String, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val drawer = uiState.value.widgetDrawers.find { it.id == drawerId } ?: return@launch
            val list = drawer.widgets.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                list.forEachIndexed { i, w ->
                    widgetRepository.updateWidget(w.copy(orderIndex = i))
                }
            }
        }
    }

    fun moveWidget(index: Int, up: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = uiState.value.standaloneWidgets.toMutableList()
            val targetIndex = if (up) index - 1 else index + 1
            if (targetIndex in list.indices) {
                val temp = list[index]
                list[index] = list[targetIndex]
                list[targetIndex] = temp
                list.forEachIndexed { i, item ->
                    widgetRepository.updateWidget(item.copy(orderIndex = i))
                }
            }
        }
    }

    fun moveWidgetDrawer(index: Int, up: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = uiState.value.widgetDrawers.toMutableList()
            val targetIndex = if (up) index - 1 else index + 1
            if (targetIndex in list.indices) {
                val temp = list[index]
                list[index] = list[targetIndex]
                list[targetIndex] = temp
                list.forEachIndexed { i, drawer ->
                    widgetRepository.updateDrawer(drawer.copy(orderIndex = i))
                }
            }
        }
    }

    fun completeWalkthrough() {
        viewModelScope.launch {
            preferencesDataSource.setHasCompletedWalkthrough(true)
        }
    }

    fun setDynamicWallpaperEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setDynamicWallpaperEnabled(enabled)
        }
    }

    // ponytail: Direct preference mutations from walkthrough flow; ceiling: doesn't validate conflicting preference matrix; upgrade path: unified batch configuration transaction.
    fun setTwoDrawersSideBySide(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setTwoDrawersSideBySide(enabled)
        }
    }

    fun setIs24Hour(is24: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setIs24Hour(is24)
        }
    }

    fun setShowWeather(show: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setShowWeather(show)
        }
    }

    fun setAdaptiveIconShape(shape: String) {
        viewModelScope.launch {
            preferencesDataSource.setAdaptiveIconShape(shape)
        }
    }

    fun setDrawerThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesDataSource.setDrawerThemeMode(mode)
        }
    }

    fun setDrawerLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setDrawerLiquidGlass(enabled)
        }
    }

    fun setDrawerLook(look: String) {
        viewModelScope.launch {
            preferencesDataSource.setDrawerLook(look)
        }
    }

    fun setMatchDeviceScreenCurve(match: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setMatchDeviceScreenCurve(match)
        }
    }

    fun setDeviceCurveOffsetDp(offset: Float) {
        viewModelScope.launch {
            preferencesDataSource.setDeviceCurveOffsetDp(offset)
        }
    }

    fun setSurfaceThemeStyle(style: String) {
        viewModelScope.launch {
            preferencesDataSource.setSurfaceThemeStyle(style)
        }
    }

    fun setCinematicPalette(palette: String) {
        viewModelScope.launch {
            preferencesDataSource.setCinematicPalette(palette)
        }
    }

    fun setDockMesmerizingGlow(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setDockMesmerizingGlow(enabled)
        }
    }

    fun setHeaderFontFamily(font: String) {
        viewModelScope.launch {
            preferencesDataSource.setHeaderFontFamily(font)
        }
    }

    fun setStealthTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setStealthTheme(enabled)
        }
    }

    fun setCategoryAlignment(alignment: String) {
        viewModelScope.launch {
            preferencesDataSource.setCategoryAlignment(alignment)
        }
    }

    fun setWeatherUnit(unit: String) {
        viewModelScope.launch {
            preferencesDataSource.setWeatherUnit(unit)
        }
    }

    fun setShowDuoStatusWidget(show: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setShowDuoStatusWidget(show)
        }
    }

    fun setEnableWidgetsPage(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setEnableWidgetsPage(enabled)
        }
    }

    fun setOpenDrawerFromDockOnly(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setOpenDrawerFromDockOnly(enabled)
        }
    }

    fun setAutoArrangeApps(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setAutoArrangeApps(enabled)
        }
    }

    fun setHideDrawerIcons(hide: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setHideDrawerIcons(hide)
        }
    }

    fun setDockShowLabels(show: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setDockShowLabels(show)
        }
    }

    fun setAutoOpenKeyboardInDrawer(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setAutoOpenKeyboardInDrawer(enabled)
        }
    }

    fun setHideStatusBar(hide: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setHideStatusBar(hide)
        }
    }

    fun setThemeMode(mode: String) { viewModelScope.launch { preferencesDataSource.setThemeMode(mode) } }
    fun setDynamicColor(enabled: Boolean) { viewModelScope.launch { preferencesDataSource.setDynamicColor(enabled) } }
    fun setAccentColor(color: String) { viewModelScope.launch { preferencesDataSource.setAccentColor(color) } }
    fun setSurfaceOpacity(opacity: Float) { viewModelScope.launch { preferencesDataSource.setSurfaceOpacity(opacity) } }
    fun setSurfaceBlurRadius(radius: Float) { viewModelScope.launch { preferencesDataSource.setSurfaceBlurRadius(radius) } }
    fun setSurfaceStrokeOpacity(opacity: Float) { viewModelScope.launch { preferencesDataSource.setSurfaceStrokeOpacity(opacity) } }
    fun setSurfaceCornerRadius(radiusDp: Float) { viewModelScope.launch { preferencesDataSource.setSurfaceCornerRadius(radiusDp) } }
    fun setWallpaperBlur(enabled: Boolean) { viewModelScope.launch { preferencesDataSource.setWallpaperBlur(enabled) } }
    fun setWallpaperBlurRadius(radius: Float) { viewModelScope.launch { preferencesDataSource.setWallpaperBlurRadius(radius) } }
    fun setWallpaperGradientSpeedSeconds(seconds: Int) { viewModelScope.launch { preferencesDataSource.setWallpaperGradientSpeedSeconds(seconds) } }
    fun setWallpaperGradientBlurPercent(percent: Int) { viewModelScope.launch { preferencesDataSource.setWallpaperGradientBlurPercent(percent) } }
    fun setWallpaperGradientPalette(palette: String) { viewModelScope.launch { preferencesDataSource.setWallpaperGradientPalette(palette) } }
    fun setWallpaperGradientAngle(angle: String) { viewModelScope.launch { preferencesDataSource.setWallpaperGradientAngle(angle) } }
    fun setAppLabelScale(scale: String) { viewModelScope.launch { preferencesDataSource.setAppLabelScale(scale) } }
    fun setDrawerTitleAnimation(mode: String) { viewModelScope.launch { preferencesDataSource.setDrawerTitleAnimation(mode) } }
    fun setDockIconSize(sizeDp: Float) { viewModelScope.launch { preferencesDataSource.setDockIconSize(sizeDp) } }
    fun setMultiGroupApps(multi: Boolean) { viewModelScope.launch { preferencesDataSource.setMultiGroupApps(multi) } }
    fun setLockLayout(locked: Boolean) { viewModelScope.launch { preferencesDataSource.setLockLayout(locked) } }
    fun setAutoCategorizeNewApps(enabled: Boolean) { viewModelScope.launch { preferencesDataSource.setAutoCategorizeNewApps(enabled) } }
    fun setBlurAppListOnOpen(enabled: Boolean) { viewModelScope.launch { preferencesDataSource.setBlurAppListOnOpen(enabled) } }
    fun setStealthContrastLevel(level: String) { viewModelScope.launch { preferencesDataSource.setStealthContrastLevel(level) } }
    fun setShowDrawerSuggestions(enabled: Boolean) { viewModelScope.launch { preferencesDataSource.setShowDrawerSuggestions(enabled) } }
}
