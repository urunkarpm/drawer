package com.urunkarpm.drawer.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.NotificationRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import com.urunkarpm.drawer.core.model.NotificationItem
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
    val selectedItemForMute: NotificationItem? = null,
    val showRulesSheet: Boolean = false
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

    private val uiControlsFlow = combine(
        _selectedItemForMute,
        _showRulesSheet,
        _permissionGrantedState
    ) { selectedItem, showRules, permission ->
        Triple(selectedItem, showRules, permission)
    }

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.activeNotifications,
        notificationRepository.mutedRules,
        preferencesDataSource.notificationsPrivacyMode,
        uiControlsFlow
    ) { notifications, rules, privacyMode, controls ->
        val (selectedItem, showRules, permission) = controls
        val grouped = notifications
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
            activeNotifications = notifications,
            groupedNotifications = grouped,
            mutedRules = rules,
            privacyMode = privacyMode,
            selectedItemForMute = selectedItem,
            showRulesSheet = showRules
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState(
            isPermissionGranted = notificationRepository.checkPermission()
        )
    )

    fun checkPermission() {
        _permissionGrantedState.value = notificationRepository.checkPermission()
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

    fun rebindService() {
        notificationRepository.rebindService()
    }
}
