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
    val backgroundStyle: String = "BLUR", // "BLUR", "TRANSPARENT", "SOLID"
    val iconSizeDp: Float = 56f,
    val showLabels: Boolean = false,
    val cornerRadiusDp: Float = 24f,
    val selectedDockItemForMenu: Pair<DockItem, AppInfo?>? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class DockViewModel @Inject constructor(
    private val dockRepository: DockRepository,
    private val appRepository: AppRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource
) : ViewModel() {

    private val _selectedDockItemForMenu = MutableStateFlow<Pair<DockItem, AppInfo?>?>(null)
    private val _isEditMode = MutableStateFlow(false)

    private val dockPreferencesFlow = combine(
        preferencesDataSource.dockBackground,
        preferencesDataSource.dockIconSize,
        preferencesDataSource.dockShowLabels,
        preferencesDataSource.dockCornerRadius
    ) { bgStyle, iconSize, showLabels, cornerRadius ->
        DockPreferences(bgStyle, iconSize, showLabels, cornerRadius)
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
            selectedDockItemForMenu = selectedItem,
            isEditMode = isEditMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DockUiState()
    )

private data class DockPreferences(
    val backgroundStyle: String,
    val iconSize: Float,
    val showLabels: Boolean,
    val cornerRadius: Float
)

    fun onAppClicked(app: AppInfo) {
        appRepository.launchApp(app)
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
