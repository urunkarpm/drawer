package com.urunkarpm.drawer.feature.groups

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.ai.CategorizationSuggestion
import com.urunkarpm.drawer.core.data.repository.AppGroupRepository
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val showCreateDialog: Boolean = false,
    val installSuggestion: Pair<AppInfo, AppGroup>? = null
)

data class GroupsUiState(
    val groups: List<ResolvedAppGroup> = emptyList(),
    val multiGroupEnabled: Boolean = false,
    val selectedAppForAction: Pair<AppInfo, AppGroup>? = null,
    val groupBeingEdited: AppGroup? = null,
    val showCreateDialog: Boolean = false,
    val lockLayout: Boolean = false,
    val drawerNotificationGlow: Boolean = true,
    val allApps: List<AppInfo> = emptyList(), // ponytail: exposed for bulk add
    val isAiCategorizing: Boolean = false,
    val aiSuggestions: List<CategorizationSuggestion>? = null,
    val isAiModelReady: Boolean = false,
    val installSuggestion: Pair<AppInfo, AppGroup>? = null
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
    private val _isAiCategorizing = MutableStateFlow(false)
    private val _aiSuggestions = MutableStateFlow<List<CategorizationSuggestion>?>(null)
    private val _isAiModelReady = MutableStateFlow(false)
    private val _installSuggestion = MutableStateFlow<Pair<AppInfo, AppGroup>?>(null)

    val isAiCategorizing: StateFlow<Boolean> = _isAiCategorizing.asStateFlow()
    val aiSuggestions: StateFlow<List<CategorizationSuggestion>?> = _aiSuggestions.asStateFlow()
    val isAiModelReady: StateFlow<Boolean> = _isAiModelReady.asStateFlow()
    val installSuggestion: StateFlow<Pair<AppInfo, AppGroup>?> = _installSuggestion.asStateFlow()

    private var aiCategorizationJob: Job? = null

    private val dialogsState = combine(
        _selectedAppForAction,
        _groupBeingEdited,
        _showCreateDialog,
        _installSuggestion
    ) { selectedApp, groupBeingEdited, showCreate, installSuggestion ->
        GroupsDialogsState(selectedApp, groupBeingEdited, showCreate, installSuggestion)
    }

    private val aiState = combine(
        _isAiCategorizing,
        _aiSuggestions,
        _isAiModelReady
    ) { isCategorizing, suggestions, modelReady ->
        Triple(isCategorizing, suggestions, modelReady)
    }

    private val groupPrefsFlow = combine(
        preferencesDataSource.multiGroupApps,
        preferencesDataSource.lockLayout,
        preferencesDataSource.drawerNotificationGlow
    ) { multiGroup, lockLayout, drawerGlow ->
        Triple(multiGroup, lockLayout, drawerGlow)
    }

    val uiState: StateFlow<GroupsUiState> = combine(
        appGroupRepository.groups,
        appRepository.installedApps,
        groupPrefsFlow,
        dialogsState,
        aiState
    ) { groups, installedApps, (multiGroup, lockLayout, drawerGlow), dialogs, (isCategorizing, suggestions, modelReady) ->
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
            lockLayout = lockLayout,
            drawerNotificationGlow = drawerGlow,
            allApps = installedApps.sortedBy { it.label.lowercase() },
            isAiCategorizing = isCategorizing,
            aiSuggestions = suggestions,
            isAiModelReady = modelReady,
            installSuggestion = dialogs.installSuggestion
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

        // Background scan when a new app is installed from Play Store
        viewModelScope.launch {
            appRepository.newlyInstalledApp.collect { newApp ->
                try {
                    val isAiEnabled = preferencesDataSource.enableOnDeviceAi.first()
                    val isAutoSuggest = preferencesDataSource.autoCategorizeNewApps.first()
                    if (isAiEnabled && isAutoSuggest) {
                        val targetDrawer = appGroupRepository.suggestDrawerForApp(newApp)
                        if (targetDrawer != null) {
                            _installSuggestion.value = newApp to targetDrawer
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun acceptInstallSuggestion(app: AppInfo, group: AppGroup) {
        viewModelScope.launch {
            assignAppToGroupDirectly(group.id, app)
            _installSuggestion.value = null
        }
    }

    fun dismissInstallSuggestion() {
        _installSuggestion.value = null
    }

    fun createGroup(
        name: String,
        iconName: String = "folder",
        colorHex: String = "#6750A4",
        viewType: GroupViewType = GroupViewType.GRID,
        columnCount: Int = 4,
        sortOrder: GroupSortOrder = GroupSortOrder.MANUAL,
        selectedApps: List<AppInfo> = emptyList() // ponytail: bulk selection
    ) {
        viewModelScope.launch {
            val group = appGroupRepository.createGroup(name, iconName, colorHex, viewType, columnCount, sortOrder)
            if (selectedApps.isNotEmpty()) {
                appGroupRepository.setGroupApps(group.id, selectedApps, preferencesDataSource.multiGroupApps.first())
            }
            _showCreateDialog.value = false
        }
    }

    fun updateGroup(group: AppGroup, selectedApps: List<AppInfo>? = null) {
        viewModelScope.launch {
            appGroupRepository.updateGroup(group)
            if (selectedApps != null) {
                appGroupRepository.setGroupApps(group.id, selectedApps, preferencesDataSource.multiGroupApps.first())
            }
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

    private fun cleanGroupName(name: String): String {
        return name.trim()
            .trim('"', '\'', '`', '“', '”')
            .trimEnd('.', '?', '!', ',', ';')
            .replaceFirst(Regex("(?i)^(?:the\\s+)?(?:folder|drawer|group|category)\\s+"), "")
            .replace(Regex("(?i)\\s+(?:folder|drawer|group|category)$"), "")
            .trim()
            .trimEnd('.', '?', '!', ',', ';')
    }

    suspend fun getOrCreateGroupByName(name: String): AppGroup {
        val existing = findGroupByName(name)
        if (existing != null) return existing
        val cleaned = cleanGroupName(name)
        return appGroupRepository.createGroup(name = cleaned.ifBlank { name.trim() })
    }

    suspend fun findGroupByName(name: String): AppGroup? {
        val cleaned = cleanGroupName(name)
        val all = appGroupRepository.groups.first()
        return all.find {
            it.name.equals(name.trim(), ignoreCase = true) ||
            it.name.equals(cleaned, ignoreCase = true)
        } ?: all.find {
            it.name.contains(cleaned, ignoreCase = true) ||
            cleaned.contains(it.name, ignoreCase = true)
        }
    }

    suspend fun deleteGroupDirectly(groupId: String) {
        appGroupRepository.deleteGroup(groupId)
    }

    suspend fun renameGroupDirectly(groupId: String, newName: String) {
        val cleaned = cleanGroupName(newName)
        val existing = appGroupRepository.groups.first().find { it.id == groupId }
        if (existing != null) {
            appGroupRepository.updateGroup(existing.copy(name = cleaned.ifBlank { newName.trim() }))
        }
    }

    suspend fun restoreGroupWithApps(group: AppGroup, apps: List<AppInfo>) {
        val created = appGroupRepository.createGroup(
            name = group.name,
            iconName = group.iconName,
            colorHex = group.colorHex,
            viewType = group.viewType,
            columnCount = group.columnCount,
            sortOrder = group.sortOrder
        )
        if (apps.isNotEmpty()) {
            val allowMulti = preferencesDataSource.multiGroupApps.first()
            appGroupRepository.setGroupApps(created.id, apps, allowMulti)
        }
    }

    suspend fun assignAppToGroupDirectly(groupId: String, app: AppInfo) {
        val allowMulti = preferencesDataSource.multiGroupApps.first()
        appGroupRepository.assignAppToGroup(groupId, app, allowMulti)
    }

    suspend fun removeAppFromGroupDirectly(groupId: String, packageName: String, activityName: String) {
        appGroupRepository.removeAppFromGroup(groupId, packageName, activityName)
    }

    suspend fun moveAppBetweenGroupsDirectly(sourceGroupId: String, targetGroupId: String, app: AppInfo) {
        appGroupRepository.moveAppBetweenGroups(sourceGroupId, targetGroupId, app)
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

    fun startAiCategorization(unassignedOnly: Boolean = true) {
        aiCategorizationJob?.cancel()
        aiCategorizationJob = viewModelScope.launch {
            _isAiCategorizing.value = true
            _isAiModelReady.value = appGroupRepository.isAiModelReady
            try {
                val suggestions = appGroupRepository.suggestCategorization(unassignedOnly)
                _aiSuggestions.value = suggestions
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiSuggestions.value = emptyList()
            } finally {
                _isAiCategorizing.value = false
            }
        }
    }

    fun applyAiSuggestions(approvedSuggestions: List<CategorizationSuggestion> = _aiSuggestions.value.orEmpty()) {
        viewModelScope.launch {
            appGroupRepository.applyCategorization(approvedSuggestions)
            _aiSuggestions.value = null
        }
    }

    fun dismissAiSuggestions() {
        aiCategorizationJob?.cancel()
        aiCategorizationJob = null
        _aiSuggestions.value = null
        _isAiCategorizing.value = false
        _isAiModelReady.value = false
    }

    override fun onCleared() {
        super.onCleared()
        aiCategorizationJob?.cancel()
    }

    fun launchApp(app: AppInfo): Boolean = appRepository.launchApp(app)

    fun openAppDetails(packageName: String) = appRepository.openAppDetails(packageName)

    fun uninstallApp(packageName: String) = appRepository.uninstallApp(packageName)

    suspend fun getAppIcon(app: AppInfo): Drawable? = appRepository.getAppIcon(app)
}
