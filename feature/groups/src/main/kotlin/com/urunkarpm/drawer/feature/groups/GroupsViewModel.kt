package com.urunkarpm.drawer.feature.groups

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.AppGroupRepository
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResolvedAppGroup(
    val group: AppGroup,
    val apps: List<AppInfo>
)

data class GroupsDialogsState(
    val selectedAppForAction: Pair<AppInfo, AppGroup>? = null,
    val groupBeingEdited: AppGroup? = null,
    val showCreateDialog: Boolean = false
)

data class GroupsUiState(
    val groups: List<ResolvedAppGroup> = emptyList(),
    val multiGroupEnabled: Boolean = false,
    val selectedAppForAction: Pair<AppInfo, AppGroup>? = null,
    val groupBeingEdited: AppGroup? = null,
    val showCreateDialog: Boolean = false,
    val lockLayout: Boolean = false
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val appGroupRepository: AppGroupRepository,
    private val appRepository: AppRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource
) : ViewModel() {

    private val _selectedAppForAction = MutableStateFlow<Pair<AppInfo, AppGroup>?>(null)
    private val _groupBeingEdited = MutableStateFlow<AppGroup?>(null)
    private val _showCreateDialog = MutableStateFlow(false)

    private val dialogsState = combine(
        _selectedAppForAction,
        _groupBeingEdited,
        _showCreateDialog
    ) { selectedApp, groupBeingEdited, showCreate ->
        GroupsDialogsState(selectedApp, groupBeingEdited, showCreate)
    }

    val uiState: StateFlow<GroupsUiState> = combine(
        appGroupRepository.groups,
        appRepository.installedApps,
        preferencesDataSource.multiGroupApps,
        preferencesDataSource.lockLayout,
        dialogsState
    ) { groups, installedApps, multiGroup, lockLayout, dialogs ->
        // Primary lookup: packageName/activityName
        val appMap = installedApps.associateBy { "${it.packageName}/${it.activityName}" }
        // Fallback lookup: packageName only (first match) - handles cases where activityName differs
        val appByPkg = installedApps.groupBy { it.packageName }
        val resolvedGroups = groups.map { group ->
            val resolvedApps = group.items.mapNotNull { item ->
                appMap["${item.packageName}/${item.activityName}"]
                    ?: appByPkg[item.packageName]?.firstOrNull()
            }.distinctBy { it.componentKey }
            val sortedApps = when (group.sortOrder) {
                GroupSortOrder.MANUAL -> resolvedApps
                GroupSortOrder.ALPHABETICAL -> resolvedApps.sortedBy { it.label.lowercase() }
                GroupSortOrder.USAGE -> resolvedApps
            }
            ResolvedAppGroup(group = group, apps = sortedApps)
        }
        GroupsUiState(
            groups = resolvedGroups,
            multiGroupEnabled = multiGroup,
            selectedAppForAction = dialogs.selectedAppForAction,
            groupBeingEdited = dialogs.groupBeingEdited,
            showCreateDialog = dialogs.showCreateDialog,
            lockLayout = lockLayout
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupsUiState()
    )

    init {
        viewModelScope.launch {
            try {
                val groups = appGroupRepository.groups.first { it.isNotEmpty() }
                if (groups.all { it.items.isEmpty() }) {
                    appGroupRepository.autoPopulateGroups()
                }
            } catch (_: Exception) {}
        }
    }

    fun createGroup(
        name: String,
        iconName: String = "folder",
        colorHex: String = "#6750A4",
        viewType: GroupViewType = GroupViewType.GRID,
        columnCount: Int = 4,
        sortOrder: GroupSortOrder = GroupSortOrder.MANUAL
    ) {
        viewModelScope.launch {
            appGroupRepository.createGroup(name, iconName, colorHex, viewType, columnCount, sortOrder)
            _showCreateDialog.value = false
        }
    }

    fun updateGroup(group: AppGroup) {
        viewModelScope.launch {
            appGroupRepository.updateGroup(group)
            _groupBeingEdited.value = null
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            appGroupRepository.deleteGroup(groupId)
            if (_groupBeingEdited.value?.id == groupId) {
                _groupBeingEdited.value = null
            }
        }
    }

    fun toggleGroupExpanded(groupId: String) {
        viewModelScope.launch {
            appGroupRepository.toggleGroupExpanded(groupId)
        }
    }

    fun collapseAllGroups() {
        viewModelScope.launch {
            appGroupRepository.collapseAllGroups()
        }
    }

    fun autoPopulateGroups() {
        viewModelScope.launch {
            appGroupRepository.autoPopulateGroups()
        }
    }

    fun moveGroupUp(groupIndex: Int) {
        if (groupIndex > 0) {
            viewModelScope.launch {
                appGroupRepository.reorderGroups(groupIndex, groupIndex - 1)
            }
        }
    }

    fun moveGroupDown(groupIndex: Int, totalGroups: Int) {
        if (groupIndex < totalGroups - 1) {
            viewModelScope.launch {
                appGroupRepository.reorderGroups(groupIndex, groupIndex + 1)
            }
        }
    }

    fun assignAppToGroup(groupId: String, app: AppInfo, targetIndex: Int? = null) {
        viewModelScope.launch {
            val allowMulti = preferencesDataSource.multiGroupApps.first()
            appGroupRepository.assignAppToGroup(groupId, app, allowMulti, targetIndex)
        }
    }

    fun moveAppBetweenGroups(
        sourceGroupId: String,
        targetGroupId: String,
        app: AppInfo,
        targetIndex: Int? = null
    ) {
        viewModelScope.launch {
            appGroupRepository.moveAppBetweenGroups(sourceGroupId, targetGroupId, app, targetIndex)
        }
    }

    fun moveAppEarlier(groupId: String, app: AppInfo) {
        viewModelScope.launch {
            val resolvedGroup = uiState.value.groups.find { it.group.id == groupId } ?: return@launch
            val currentIndex = resolvedGroup.apps.indexOfFirst { it.componentKey == app.componentKey }
            if (currentIndex > 0) {
                val allowMulti = preferencesDataSource.multiGroupApps.first()
                appGroupRepository.assignAppToGroup(groupId, app, allowMulti, currentIndex - 1)
            }
            _selectedAppForAction.value = null
        }
    }

    fun moveAppLater(groupId: String, app: AppInfo) {
        viewModelScope.launch {
            val resolvedGroup = uiState.value.groups.find { it.group.id == groupId } ?: return@launch
            val currentIndex = resolvedGroup.apps.indexOfFirst { it.componentKey == app.componentKey }
            if (currentIndex in 0 until resolvedGroup.apps.size - 1) {
                val allowMulti = preferencesDataSource.multiGroupApps.first()
                appGroupRepository.assignAppToGroup(groupId, app, allowMulti, currentIndex + 1)
            }
            _selectedAppForAction.value = null
        }
    }

    fun removeAppFromGroup(groupId: String, packageName: String, activityName: String) {
        viewModelScope.launch {
            appGroupRepository.removeAppFromGroup(groupId, packageName, activityName)
            _selectedAppForAction.value = null
        }
    }

    fun selectAppForAction(app: AppInfo, group: AppGroup) {
        _selectedAppForAction.value = app to group
    }

    fun dismissAppAction() {
        _selectedAppForAction.value = null
    }

    fun startEditingGroup(group: AppGroup) {
        _groupBeingEdited.value = group
    }

    fun dismissEditGroup() {
        _groupBeingEdited.value = null
    }

    fun setShowCreateDialog(show: Boolean) {
        _showCreateDialog.value = show
    }

    fun launchApp(app: AppInfo): Boolean = appRepository.launchApp(app)

    fun openAppDetails(packageName: String) = appRepository.openAppDetails(packageName)

    fun uninstallApp(packageName: String) = appRepository.uninstallApp(packageName)

    suspend fun getAppIcon(app: AppInfo): Drawable? = appRepository.getAppIcon(app)
}
