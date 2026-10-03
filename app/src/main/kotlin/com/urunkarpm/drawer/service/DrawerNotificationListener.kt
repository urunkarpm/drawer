package com.urunkarpm.drawer.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DrawerNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        _activeNotificationsFlow.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val current = activeNotifications?.toList() ?: emptyList()
            _activeNotificationsFlow.value = current
        } catch (_: SecurityException) {
            _activeNotificationsFlow.value = emptyList()
        }
    }

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _activeNotificationsFlow = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val activeNotificationsFlow: StateFlow<List<StatusBarNotification>> = _activeNotificationsFlow.asStateFlow()

        var instance: DrawerNotificationListener? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
