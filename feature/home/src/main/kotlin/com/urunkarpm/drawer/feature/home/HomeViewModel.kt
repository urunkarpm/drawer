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
    val adaptiveIconShape: String = "SQUIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val drawerThemeMode: String = "SYSTEM",
    val autoOpenKeyboardInDrawer: Boolean = false,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true,
    val categoryAlignment: String = "TOP",
    val enableWidgetsPage: Boolean = true,
    val headerFontFamily: String = "SYSTEM",
    val appLabelScale: String = "NORMAL",
    val openDrawerFromDockOnly: Boolean = false,
    val duoStatusPosition: String = "TEMP"
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
    val adaptiveIconShape: String = "SQUIRCLE",
    val twoDrawersSideBySide: Boolean = false,
    val wallpaperBlur: Boolean = true,
    val wallpaperBlurRadius: Float = 25f,
    val drawerThemeMode: String = "SYSTEM",
    val autoOpenKeyboardInDrawer: Boolean = false,
    val showDuoStatusWidget: Boolean = true,
    val enableCameraMirror: Boolean = true,
    val categoryAlignment: String = "TOP",
    val enableWidgetsPage: Boolean = true,
    val widgets: List<com.urunkarpm.drawer.core.model.WidgetItem> = emptyList(),
    val widgetDrawers: List<com.urunkarpm.drawer.core.model.WidgetDrawer> = emptyList(),
    val standaloneWidgets: List<com.urunkarpm.drawer.core.model.WidgetItem> = emptyList(),
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f,
    val headerFontFamily: String = "SYSTEM",
    val appLabelScale: String = "NORMAL",
    val openDrawerFromDockOnly: Boolean = false,
    val duoStatusPosition: String = "TEMP"
)

data class SurfaceThemePrefs(
    val surfaceThemeStyle: String = "LIQUID_GLASS",
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val surfaceCornerRadius: Float = 24f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val dockRepository: DockRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val widgetRepository: com.urunkarpm.drawer.core.data.repository.WidgetRepository
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
        preferencesDataSource.wallpaperBlurRadius,
        preferencesDataSource.drawerThemeMode,
        preferencesDataSource.autoOpenKeyboardInDrawer,
        preferencesDataSource.showDuoStatusWidget,
        preferencesDataSource.enableCameraMirror,
        preferencesDataSource.categoryAlignment,
        preferencesDataSource.enableWidgetsPage,
        preferencesDataSource.headerFontFamily,
        preferencesDataSource.appLabelScale,
        preferencesDataSource.openDrawerFromDockOnly,
        preferencesDataSource.duoStatusPosition
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
        val autoOpenKeyboardInDrawer = values[9] as Boolean
        val showDuoStatusWidget = values[10] as Boolean
        val enableCameraMirror = values[11] as Boolean
        val categoryAlignment = values[12] as String
        val enableWidgetsPage = values[13] as Boolean
        val headerFontFamily = values[14] as String
        val appLabelScale = values[15] as String
        val openDrawerFromDockOnly = values[16] as Boolean
        val duoStatusPosition = values[17] as String
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
            autoOpenKeyboardInDrawer,
            showDuoStatusWidget,
            enableCameraMirror,
            categoryAlignment,
            enableWidgetsPage,
            headerFontFamily,
            appLabelScale,
            openDrawerFromDockOnly,
            duoStatusPosition
        )
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

    private val surfacePrefsFlow = combine(
        preferencesDataSource.surfaceThemeStyle,
        preferencesDataSource.surfaceOpacity,
        preferencesDataSource.surfaceBlurRadius,
        preferencesDataSource.surfaceStrokeOpacity,
        preferencesDataSource.surfaceCornerRadius
    ) { theme, opacity, blurRadius, strokeOpacity, cornerRadius ->
        SurfaceThemePrefs(theme, opacity, blurRadius, strokeOpacity, cornerRadius)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.installedApps,
        weatherRepository.weatherInfo,
        widgetRepository.getAllDrawers(),
        widgetRepository.getAllWidgets(),
        glancePrefsFlow,
        homeDialogsFlow,
        surfacePrefsFlow
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val apps = args[0] as List<AppInfo>
        val weather = args[1] as? WeatherInfo
        @Suppress("UNCHECKED_CAST")
        val drawersList = args[2] as List<com.urunkarpm.drawer.core.model.WidgetDrawer>
        @Suppress("UNCHECKED_CAST")
        val widgetsList = args[3] as List<com.urunkarpm.drawer.core.model.WidgetItem>
        val glancePrefs = args[4] as GlancePrefs
        val dialogs = args[5] as HomeDialogsState
        val surfacePrefs = args[6] as SurfaceThemePrefs

        val standalone = widgetsList.filter { it.drawerId == null }

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
            wallpaperBlurRadius = glancePrefs.wallpaperBlurRadius,
            drawerThemeMode = glancePrefs.drawerThemeMode,
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
            headerFontFamily = glancePrefs.headerFontFamily,
            appLabelScale = glancePrefs.appLabelScale,
            openDrawerFromDockOnly = glancePrefs.openDrawerFromDockOnly,
            duoStatusPosition = glancePrefs.duoStatusPosition
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
        viewModelScope.launch(Dispatchers.IO) {
            val expanded = uiState.value.widgetDrawers.filter { it.isExpanded }
            expanded.forEach { drawer ->
                widgetRepository.updateDrawer(drawer.copy(isExpanded = false))
            }
        }
    }

    fun toggleWidgetDrawerExpand(drawerId: String) {
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
            val targetOrder = if (drawerId != null) {
                uiState.value.widgetDrawers.find { it.id == drawerId }?.widgets?.size ?: 0
            } else {
                uiState.value.standaloneWidgets.size
            }
            val widget = uiState.value.widgets.find { it.id == widgetId }
            if (widget != null) {
                widgetRepository.updateWidget(widget.copy(drawerId = drawerId, orderIndex = targetOrder))
            } else {
                widgetRepository.assignWidgetToDrawer(widgetId, drawerId)
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
}
