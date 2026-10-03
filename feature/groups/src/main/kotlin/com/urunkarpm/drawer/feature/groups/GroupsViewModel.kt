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
    val showCreateDialog: Boolean = false
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
        dialogsState
    ) { groups, installedApps, multiGroup, dialogs ->
        val appMap = installedApps.associateBy { "${it.packageName}/${it.activityName}" }
        val resolvedGroups = groups.map { group ->
            val resolvedApps = group.items.mapNotNull { item ->
                appMap["${item.packageName}/${item.activityName}"]
            }
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
            showCreateDialog = dialogs.showCreateDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupsUiState()
    )

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

    fun assignAppToGroup(groupId: String, app: AppInfo) {
        viewModelScope.launch {
            val allowMulti = preferencesDataSource.multiGroupApps.first()
            appGroupRepository.assignAppToGroup(groupId, app, allowMulti)
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
