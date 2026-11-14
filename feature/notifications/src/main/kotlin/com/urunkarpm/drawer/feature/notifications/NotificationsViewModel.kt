package com.urunkarpm.drawer.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.NotificationRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import com.urunkarpm.drawer.core.model.NotificationItem
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppNotificationGroup(
    val packageName: String,
    val appName: String,
    val items: List<NotificationItem>
)

data class NotificationsUiState(
    val isPermissionGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val activeNotifications: List<NotificationItem> = emptyList(),
    val groupedNotifications: List<AppNotificationGroup> = emptyList(),
    val mutedRules: List<MutedAppRule> = emptyList(),
    val privacyMode: Boolean = false,
    val swipeRightAction: NotificationSwipeAction = NotificationSwipeAction.MARK_AS_READ,
    val swipeLeftAction: NotificationSwipeAction = NotificationSwipeAction.DISMISS,
    val selectedItemForMute: NotificationItem? = null,
    val showRulesSheet: Boolean = false,
    val hiddenNotifUnlockCount: Int = 0
)

private data class NotificationPreferencesData(
    val privacyMode: Boolean,
    val swipeRightAction: NotificationSwipeAction,
    val swipeLeftAction: NotificationSwipeAction,
    val hiddenAppPackages: Set<String>,
    val hiddenNotifUnlockCount: Int
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val appRepository: AppRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource
) : ViewModel() {

    private val _selectedItemForMute = MutableStateFlow<NotificationItem?>(null)
    private val _showRulesSheet = MutableStateFlow(false)
    private val _permissionGrantedState = MutableStateFlow(notificationRepository.checkPermission())
    private var previousHiddenKeys = emptySet<String>()

    private val uiControlsFlow = combine(
        _selectedItemForMute,
        _showRulesSheet,
        _permissionGrantedState
    ) { selectedItem, showRules, permission ->
        Triple(selectedItem, showRules, permission)
    }

    private val notificationPreferencesFlow = combine(
        preferencesDataSource.notificationsPrivacyMode,
        preferencesDataSource.notificationSwipeRightAction,
        preferencesDataSource.notificationSwipeLeftAction,
        preferencesDataSource.hiddenAppPackages,
        preferencesDataSource.hiddenNotifUnlockCount
    ) { args: Array<Any?> ->
        NotificationPreferencesData(
            privacyMode = args[0] as Boolean,
            swipeRightAction = args[1] as NotificationSwipeAction,
            swipeLeftAction = args[2] as NotificationSwipeAction,
            hiddenAppPackages = @Suppress("UNCHECKED_CAST") (args[3] as Set<String>),
            hiddenNotifUnlockCount = args[4] as Int
        )
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.activeNotifications,
        notificationRepository.mutedRules,
        notificationPreferencesFlow,
        uiControlsFlow
    ) { notifications, rules, notifPrefs, controls ->
        val (selectedItem, showRules, permission) = controls
        val hiddenPackages = notifPrefs.hiddenAppPackages

        // Detect newly posted notifications from hidden apps and arm the unlock indicator
        val hiddenItems = notifications.filter { it.packageName in hiddenPackages }
        val currentHiddenKeys = hiddenItems.map { it.key }.toSet()
        val newHiddenKeys = currentHiddenKeys - previousHiddenKeys
        if (newHiddenKeys.isNotEmpty()) {
            viewModelScope.launch {
                preferencesDataSource.setHiddenNotifUnlockCount(2)
            }
        }
        previousHiddenKeys = currentHiddenKeys

        // Suppress notifications for hidden apps from drawer and DuoStatus
        val visibleNotifications = notifications.filter { it.packageName !in hiddenPackages }

        val grouped = visibleNotifications
            .groupBy { it.packageName }
            .map { (pkg, items) ->
                AppNotificationGroup(
                    packageName = pkg,
                    appName = items.firstOrNull()?.appName ?: pkg,
                    items = items.sortedByDescending { it.postTimeMillis }
                )
            }
            .sortedByDescending { it.items.firstOrNull()?.postTimeMillis ?: 0L }

        NotificationsUiState(
            isPermissionGranted = permission,
            isServiceConnected = notificationRepository.checkPermission(),
            activeNotifications = visibleNotifications,
            groupedNotifications = grouped,
            mutedRules = rules,
            privacyMode = notifPrefs.privacyMode,
            swipeRightAction = notifPrefs.swipeRightAction,
            swipeLeftAction = notifPrefs.swipeLeftAction,
            selectedItemForMute = selectedItem,
            showRulesSheet = showRules,
            hiddenNotifUnlockCount = notifPrefs.hiddenNotifUnlockCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState(
            isPermissionGranted = notificationRepository.checkPermission()
        )
    )

    fun checkPermission() {
        val granted = notificationRepository.checkPermission()
        _permissionGrantedState.value = granted
        if (granted && !uiState.value.isServiceConnected) {
            notificationRepository.rebindService()
        }
    }

    fun rebindService() {
        if (_permissionGrantedState.value && !uiState.value.isServiceConnected) {
            notificationRepository.rebindService()
        }
    }

    fun openNotificationAccessSettings() {
        notificationRepository.openNotificationAccessSettings()
    }

    fun openAppNotificationSettings(packageName: String) {
        notificationRepository.openAppNotificationSettings(packageName)
    }

    fun dismissNotification(key: String) {
        notificationRepository.dismissNotification(key)
    }

    fun markNotificationAsRead(key: String) {
        notificationRepository.markNotificationAsRead(key)
    }

    fun clearAllNotifications() {
        notificationRepository.clearAllNotifications()
    }

    fun openNotification(key: String) {
        notificationRepository.openNotification(key)
    }

    fun selectItemForMute(item: NotificationItem) {
        _selectedItemForMute.value = item
    }

    fun dismissMuteSheet() {
        _selectedItemForMute.value = null
    }

    fun setShowRulesSheet(show: Boolean) {
        _showRulesSheet.value = show
    }

    fun muteApp(packageName: String, ruleType: MuteRuleType, durationMillis: Long? = null, autoDismiss: Boolean = false) {
        viewModelScope.launch {
            notificationRepository.muteApp(packageName, ruleType, durationMillis, autoDismiss)
            _selectedItemForMute.value = null
        }
    }

    fun unmuteApp(packageName: String) {
        viewModelScope.launch {
            notificationRepository.unmuteApp(packageName)
        }
    }

    fun togglePrivacyMode() {
        viewModelScope.launch {
            val current = uiState.value.privacyMode
            preferencesDataSource.setNotificationsPrivacyMode(!current)
        }
    }
}
