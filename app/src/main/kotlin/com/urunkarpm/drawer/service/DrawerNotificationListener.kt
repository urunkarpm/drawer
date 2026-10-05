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

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap

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

    private val appLabelCache = ConcurrentHashMap<String, String>()
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { refreshNotifications() }

    private fun scheduleRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, 200L)
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
        appLabelCache.clear()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        bridge.setServiceConnected(true)
        bridge.onGetPendingIntent = { key ->
            try {
                activeNotifications?.find { it.key == key }?.notification?.contentIntent
            } catch (_: Throwable) { null }
        }
        bridge.onGetPackageName = { key ->
            try {
                activeNotifications?.find { it.key == key }?.packageName
            } catch (_: Throwable) { null }
        }
        bridge.onDismissNotification = { key ->
            try {
                cancelNotification(key)
                refreshNotifications()
            } catch (_: Throwable) {}
        }
        bridge.onClearAllNotifications = {
            try {
                cancelAllNotifications()
                refreshNotifications()
            } catch (_: Throwable) {}
        }
        bridge.onOpenNotification = { key ->
            try {
                val sbn = activeNotifications?.find { it.key == key }
                if (sbn != null) {
                    val pendingIntent = sbn.notification?.contentIntent
                    if (pendingIntent != null) {
                        val options = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            android.app.ActivityOptions.makeBasic().apply {
                                pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            }.toBundle()
                        } else null

                        try {
                            pendingIntent.send(this, 0, null, null, null, null, options)
                        } catch (_: Throwable) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(sbn.packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                            }
                        }
                    } else {
                        val launchIntent = packageManager.getLaunchIntentForPackage(sbn.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
        bridge.onRebindRequest = {
            try {
                requestRebind(ComponentName(this, DrawerNotificationListener::class.java))
            } catch (_: Throwable) {}
        }
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        refreshHandler.removeCallbacks(refreshRunnable)
        bridge.setServiceConnected(false)
        bridge.updateNotifications(emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        scheduleRefresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        scheduleRefresh()
    }

    private fun refreshNotifications() {
        try {
            val sbnList = activeNotifications ?: return

            // Filter out redundant group summary notifications when child notifications exist
            val nonSummarySbns = sbnList.filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            val nonSummaryGroupKeys = nonSummarySbns.mapNotNull { it.groupKey }.toSet()
            val nonSummaryPackages = nonSummarySbns.map { it.packageName }.toSet()

            val filteredSbns = sbnList.filter { sbn ->
                val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                if (isSummary) {
                    val hasChildrenInGroup = sbn.groupKey?.let { it in nonSummaryGroupKeys } ?: false
                    val hasChildrenInPkg = sbn.packageName in nonSummaryPackages
                    !(hasChildrenInGroup || hasChildrenInPkg)
                } else {
                    true
                }
            }

            val items = filteredSbns.mapNotNull { sbn ->
                try {
                    val appName = appLabelCache.getOrPut(sbn.packageName) {
                        try {
                            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            sbn.packageName
                        }
                    }
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

            // ponytail: deduplicate notifications with identical (packageName, title, text) by picking newest; ceiling: drops intentional twin alerts from same app; upgrade path: match sub-second postTime window.
            val distinctItems = items
                .groupBy { Triple(it.packageName, it.title.trim(), it.text.trim()) }
                .map { (_, duplicates) -> duplicates.maxByOrNull { it.postTimeMillis } ?: duplicates.first() }
                .distinctBy { it.key }

            bridge.updateNotifications(distinctItems)
        } catch (_: SecurityException) {
            bridge.updateNotifications(emptyList())
        }
    }
}

