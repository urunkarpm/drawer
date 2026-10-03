package com.urunkarpm.drawer.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.urunkarpm.drawer.core.data.notification.NotificationBridge
import com.urunkarpm.drawer.core.model.NotificationItem
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationListenerEntryPoint {
    fun notificationBridge(): NotificationBridge
}

class DrawerNotificationListener : NotificationListenerService() {

    private val bridge: NotificationBridge by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationListenerEntryPoint::class.java
        ).notificationBridge()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        bridge.setServiceConnected(true)
        bridge.onDismissNotification = { key ->
            try {
                cancelNotification(key)
                refreshNotifications()
            } catch (_: Exception) {}
        }
        bridge.onClearAllNotifications = {
            try {
                cancelAllNotifications()
                refreshNotifications()
            } catch (_: Exception) {}
        }
        bridge.onOpenNotification = { key ->
            try {
                activeNotifications?.find { it.key == key }?.notification?.contentIntent?.send()
            } catch (_: Exception) {}
        }
        bridge.onRebindRequest = {
            try {
                requestRebind(ComponentName(this, DrawerNotificationListener::class.java))
            } catch (_: Exception) {}
        }
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        bridge.setServiceConnected(false)
        bridge.updateNotifications(emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val sbnList = activeNotifications ?: return
            val items = sbnList.mapNotNull { sbn ->
                try {
                    val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: appName
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                        ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                        ?: ""
                    NotificationItem(
                        key = sbn.key,
                        packageName = sbn.packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        postTimeMillis = sbn.postTime,
                        isClearable = sbn.isClearable
                    )
                } catch (_: Exception) {
                    null
                }
            }
            bridge.updateNotifications(items)
        } catch (_: SecurityException) {
            bridge.updateNotifications(emptyList())
        }
    }
}
