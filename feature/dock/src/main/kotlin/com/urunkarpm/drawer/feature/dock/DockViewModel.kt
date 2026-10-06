package com.urunkarpm.drawer.feature.dock

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DockUiState(
    val dockItems: List<DockItem> = emptyList(),
    val resolvedApps: List<Pair<DockItem, AppInfo?>> = emptyList(),
    val backgroundStyle: String = "LIQUID_GLASS", // "LIQUID_GLASS", "BLUR", "TRANSPARENT", "SOLID"
    val iconSizeDp: Float = 56f,
    val showLabels: Boolean = false,
    val cornerRadiusDp: Float = 24f,
    val surfaceOpacity: Float = 0.65f,
    val surfaceBlurRadius: Float = 25f,
    val surfaceStrokeOpacity: Float = 0.40f,
    val selectedDockItemForMenu: Pair<DockItem, AppInfo?>? = null,
    val isEditMode: Boolean = false,
    val autoArrangeApps: Boolean = true
)

@HiltViewModel
class DockViewModel @Inject constructor(
    private val dockRepository: DockRepository,
    private val appRepository: AppRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource
) : ViewModel() {

    private val _selectedDockItemForMenu = MutableStateFlow<Pair<DockItem, AppInfo?>?>(null)
    private val _isEditMode = MutableStateFlow(false)

    private val dockThemePrefsFlow = combine(
        preferencesDataSource.surfaceThemeStyle,
        preferencesDataSource.surfaceCornerRadius,
        preferencesDataSource.surfaceOpacity,
        preferencesDataSource.surfaceBlurRadius,
        preferencesDataSource.surfaceStrokeOpacity
    ) { theme, cornerRadius, opacity, blurRadius, strokeOpacity ->
        DockThemePreferences(theme, opacity, blurRadius, strokeOpacity, cornerRadius)
    }

    private val dockLayoutPrefsFlow = combine(
        preferencesDataSource.dockIconSize,
        preferencesDataSource.dockShowLabels,
        preferencesDataSource.autoArrangeApps
    ) { iconSize, showLabels, autoArrangeApps ->
        Triple(iconSize, showLabels, autoArrangeApps)
    }

    private val dockPreferencesFlow = combine(
        dockThemePrefsFlow,
        dockLayoutPrefsFlow
    ) { theme, layout ->
        val (iconSize, showLabels, autoArrangeApps) = layout
        DockPreferences(
            backgroundStyle = theme.themeStyle,
            iconSize = iconSize,
            showLabels = showLabels,
            cornerRadius = theme.cornerRadius,
            opacity = theme.opacity,
            blurRadius = theme.blurRadius,
            strokeOpacity = theme.strokeOpacity,
            autoArrangeApps = autoArrangeApps
        )
    }

    val uiState: StateFlow<DockUiState> = combine(
        dockRepository.dockItems,
        appRepository.installedApps,
        dockPreferencesFlow,
        _selectedDockItemForMenu,
        _isEditMode
    ) { items, installedApps, prefs, selectedItem, isEditMode ->
        val resolved = items.sortedBy { it.position }.map { dockItem ->
            val matchingApp = installedApps.find {
                it.packageName == dockItem.packageName &&
                it.activityName == dockItem.activityName &&
                it.userHandleId == dockItem.userHandleId
            } ?: installedApps.find {
                it.packageName == dockItem.packageName &&
                it.activityName == dockItem.activityName
            } ?: installedApps.find {
                it.packageName == dockItem.packageName
            }
            dockItem to matchingApp
        }

        DockUiState(
            dockItems = items,
            resolvedApps = resolved,
            backgroundStyle = prefs.backgroundStyle,
            iconSizeDp = prefs.iconSize,
            showLabels = prefs.showLabels,
            cornerRadiusDp = prefs.cornerRadius,
            surfaceOpacity = prefs.opacity,
            surfaceBlurRadius = prefs.blurRadius,
            surfaceStrokeOpacity = prefs.strokeOpacity,
            selectedDockItemForMenu = selectedItem,
            isEditMode = isEditMode,
            autoArrangeApps = prefs.autoArrangeApps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DockUiState()
    )

private data class DockThemePreferences(
    val themeStyle: String,
    val opacity: Float,
    val blurRadius: Float,
    val strokeOpacity: Float,
    val cornerRadius: Float
)

private data class DockPreferences(
    val backgroundStyle: String,
    val iconSize: Float,
    val showLabels: Boolean,
    val cornerRadius: Float,
    val opacity: Float,
    val blurRadius: Float,
    val strokeOpacity: Float,
    val autoArrangeApps: Boolean
)

    fun reorderDock(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        viewModelScope.launch {
            dockRepository.reorderDock(fromPosition, toPosition)
        }
    }

    fun onAppClicked(app: AppInfo) {
        appRepository.launchApp(app)
    }

    fun onAppClicked(dockItem: DockItem, app: AppInfo?) {
        if (app != null) {
            appRepository.launchApp(app)
        } else {
            val fallbackApp = AppInfo(
                packageName = dockItem.packageName,
                activityName = dockItem.activityName,
                label = dockItem.customLabel ?: dockItem.packageName,
                userHandleId = dockItem.userHandleId
            )
            appRepository.launchApp(fallbackApp)
        }
    }

    fun onItemLongClicked(dockItem: DockItem, app: AppInfo?) {
        _selectedDockItemForMenu.value = dockItem to app
    }

    fun dismissMenu() {
        _selectedDockItemForMenu.value = null
    }

    fun removeItem(position: Int) {
        viewModelScope.launch {
            dockRepository.removeFromDock(position)
            dismissMenu()
        }
    }

    fun moveItemLeft(currentPosition: Int) {
        if (currentPosition > 0) {
            viewModelScope.launch {
                dockRepository.reorderDock(currentPosition, currentPosition - 1)
                dismissMenu()
            }
        }
    }

    fun moveItemRight(currentPosition: Int) {
        viewModelScope.launch {
            val total = dockRepository.dockItems.first().size
            if (currentPosition < total - 1) {
                dockRepository.reorderDock(currentPosition, currentPosition + 1)
                dismissMenu()
            }
        }
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    suspend fun getAppIcon(app: AppInfo): Drawable? {
        return appRepository.getAppIcon(app)
    }

    fun openAppDetails(packageName: String) {
        appRepository.openAppDetails(packageName)
        dismissMenu()
    }

    fun uninstallApp(packageName: String) {
        appRepository.uninstallApp(packageName)
        dismissMenu()
    }
}
