package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import com.urunkarpm.drawer.core.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val isPermissionGranted: Flow<Boolean>
    val isServiceConnected: Flow<Boolean>
    val activeNotifications: Flow<List<NotificationItem>>
    val mutedRules: Flow<List<MutedAppRule>>

    fun checkPermission(): Boolean
    fun openNotificationAccessSettings()
    fun dismissNotification(key: String)
    fun clearAllNotifications()
    fun openNotification(key: String)
    suspend fun muteApp(
        packageName: String,
        ruleType: MuteRuleType,
        snoozeDurationMillis: Long? = null,
        autoDismiss: Boolean = false
    )
    suspend fun unmuteApp(packageName: String)
    fun openAppNotificationSettings(packageName: String)
    fun rebindService()
}
