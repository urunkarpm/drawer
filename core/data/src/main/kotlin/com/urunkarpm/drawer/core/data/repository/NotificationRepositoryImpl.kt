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

        notifications.filter { item ->
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
        try {
            notificationBridge.onRebindRequest?.invoke()
            val componentName = android.content.ComponentName(
                context.packageName,
                "com.urunkarpm.drawer.service.DrawerNotificationListener"
            )
            try {
                android.service.notification.NotificationListenerService.requestRebind(componentName)
            } catch (_: Exception) {}

            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                componentName,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {}
    }
}
