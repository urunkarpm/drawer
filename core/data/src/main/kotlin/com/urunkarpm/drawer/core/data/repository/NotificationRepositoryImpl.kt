package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.data.notification.NotificationBridge
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import com.urunkarpm.drawer.core.model.NotificationItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationBridge: NotificationBridge,
    private val mutedAppDao: MutedAppDao,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : NotificationRepository {

    override fun checkPermission(): Boolean {
        return try {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            val isEnabledInSettings = enabledListeners.contains(context.packageName)
            val isEnabledInCompat = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            isEnabledInSettings || isEnabledInCompat
        } catch (_: Exception) {
            false
        }
    }

    override val isPermissionGranted: Flow<Boolean> = flow {
        emit(checkPermission())
    }.flowOn(ioDispatcher)

    override val isServiceConnected: Flow<Boolean> = notificationBridge.isServiceConnected

    override val mutedRules: Flow<List<MutedAppRule>> = mutedAppDao.getAllRules().map { list ->
        list.map { it.toDomain() }
    }.flowOn(ioDispatcher)

    override val activeNotifications: Flow<List<NotificationItem>> = combine(
        notificationBridge.notifications,
        mutedRules
    ) { notifications, rules ->
        val ruleMap = rules.associateBy { it.packageName }
        val now = System.currentTimeMillis()

        // ponytail: deduplicate identical (packageName, title, text) defensive pass; ceiling: drops twin alerts; upgrade path: time-bucketed dedup.
        val deduped = notifications
            .groupBy { Triple(it.packageName, it.title.trim(), it.text.trim()) }
            .map { (_, duplicates) -> duplicates.maxByOrNull { it.postTimeMillis } ?: duplicates.first() }
            .distinctBy { it.key }

        deduped.filter { item ->
            val rule = ruleMap[item.packageName] ?: return@filter true

            if (rule.autoDismiss) {
                dismissNotification(item.key)
                return@filter false
            }

            when (rule.ruleType) {
                MuteRuleType.HIDE -> false
                MuteRuleType.SNOOZE_PERMANENT -> false
                MuteRuleType.SNOOZE_1H, MuteRuleType.SNOOZE_UNTIL_TOMORROW -> {
                    val until = rule.snoozedUntilTimestamp ?: 0L
                    now >= until // keep only if snooze period has elapsed
                }
            }
        }
    }.flowOn(ioDispatcher)

    override fun openNotificationAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    override fun openAppNotificationSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback
        }
    }

    override fun dismissNotification(key: String) {
        notificationBridge.onDismissNotification?.invoke(key)
    }

    override fun clearAllNotifications() {
        notificationBridge.onClearAllNotifications?.invoke()
    }

    override fun openNotification(key: String) {
        val pendingIntent = notificationBridge.onGetPendingIntent?.invoke(key)
        val packageName = notificationBridge.onGetPackageName?.invoke(key)
        if (pendingIntent != null) {
            try {
                val options = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.app.ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }.toBundle()
                } else null
                pendingIntent.send(context, 0, null, null, null, null, options)
                return
            } catch (_: Throwable) {}
        }
        if (packageName != null) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return
                }
            } catch (_: Throwable) {}
        }
        notificationBridge.onOpenNotification?.invoke(key)
    }

    override suspend fun muteApp(
        packageName: String,
        ruleType: MuteRuleType,
        snoozeDurationMillis: Long?,
        autoDismiss: Boolean
    ) = withContext(ioDispatcher) {
        val snoozedUntil = if (snoozeDurationMillis != null) {
            System.currentTimeMillis() + snoozeDurationMillis
        } else null

        val entity = MutedAppRuleEntity(
            packageName = packageName,
            ruleType = ruleType.name,
            snoozedUntilTimestamp = snoozedUntil,
            autoDismiss = autoDismiss
        )
        mutedAppDao.upsertRule(entity)
    }

    override suspend fun unmuteApp(packageName: String) = withContext(ioDispatcher) {
        mutedAppDao.deleteRule(packageName)
    }

    override fun rebindService() {
        // ponytail: official requestRebind handles service reconnect; avoid setComponentEnabledSetting which sends PACKAGE_CHANGED and restarts the launcher; ceiling is requiring permission toggle if unbound; upgrade path is foreground listener service.
        try {
            notificationBridge.onRebindRequest?.invoke()
            val componentName = android.content.ComponentName(
                context.packageName,
                "com.urunkarpm.drawer.service.DrawerNotificationListener"
            )
            android.service.notification.NotificationListenerService.requestRebind(componentName)
        } catch (_: Throwable) {}
    }
}
