package com.urunkarpm.drawer.core.data.notification

import com.urunkarpm.drawer.core.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationBridge @Inject constructor() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    var onDismissNotification: ((key: String) -> Unit)? = null
    var onClearAllNotifications: (() -> Unit)? = null
    var onOpenNotification: ((key: String) -> Unit)? = null
    var onGetPendingIntent: ((key: String) -> android.app.PendingIntent?)? = null
    var onGetPackageName: ((key: String) -> String?)? = null
    var onRebindRequest: (() -> Unit)? = null

    fun updateNotifications(items: List<NotificationItem>) {
        _notifications.value = items
    }

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }
}
