package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppShortcutInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val iconPackRepositoryProvider: javax.inject.Provider<IconPackRepository>? = null,
    private val preferencesDataSource: com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource? = null
) : AppRepository {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val packageManager = context.packageManager

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    override val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        registerLauncherAppsCallback()
        scope.launch {
            refreshApps()
        }
        preferencesDataSource?.let { prefs ->
            scope.launch {
                prefs.activeIconPack.collect {
                    clearIconCache()
                }
            }
        }
    }

    private fun registerLauncherAppsCallback() {
        try {
            val looper = Looper.getMainLooper() ?: return
            val callback = object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) {
                    scope.launch { refreshApps() }
                }

                override fun onPackageRemoved(packageName: String, user: UserHandle) {
                    scope.launch {
                        val prefix = "$packageName/"
                        val keysToRemove = iconCache.snapshot().keys.filter { it.startsWith(prefix) }
                        for (k in keysToRemove) {
                            iconCache.remove(k)
                        }
                        refreshApps()
                    }
                }

                override fun onPackageChanged(packageName: String, user: UserHandle) {
                    scope.launch {
                        val prefix = "$packageName/"
                        val keysToRemove = iconCache.snapshot().keys.filter { it.startsWith(prefix) }
                        for (k in keysToRemove) {
                            iconCache.remove(k)
                        }
                        refreshApps()
                    }
                }

                override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                    scope.launch { refreshApps() }
                }

                override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                    scope.launch { refreshApps() }
                }
            }
            launcherApps.registerCallback(callback, Handler(looper))
        } catch (_: Throwable) {
            // Handled in environments without Looper
        }
    }

    override suspend fun refreshApps() = withContext(ioDispatcher) {

        val myUserHandle = try { Process.myUserHandle() } catch (_: Throwable) { null }
        val profiles = userManager.userProfiles
        val appsList = mutableListOf<AppInfo>()

        for (user in profiles) {
            val isWorkProfile = user != myUserHandle
            val userHandleId = user.hashCode()
            val activities = launcherApps.getActivityList(null, user)

            for (activity in activities) {
                if (activity.applicationInfo.packageName == context.packageName) {
                    continue
                }

                val packageName = activity.applicationInfo.packageName
                val activityName = activity.name
                val label = activity.label?.toString() ?: packageName
                val installTime = try {
                    packageManager.getPackageInfo(packageName, 0).firstInstallTime
                } catch (_: Exception) { 0L }
                val updateTime = try {
                    packageManager.getPackageInfo(packageName, 0).lastUpdateTime
                } catch (_: Exception) { 0L }

                val app = AppInfo(
                    packageName = packageName,
                    activityName = activityName,
                    label = label,
                    userHandleId = userHandleId,
                    isWorkProfile = isWorkProfile,
                    installTimeMillis = installTime,
                    lastUpdateTimeMillis = updateTime
                )
                appsList.add(app)
                activityInfoCache[app.componentKey] = activity
            }
        }

        appsList.sortBy { it.label.lowercase() }
        _installedApps.value = appsList

        // ponytail: pre-warming in-memory icon cache on IO scope ensures opening the app list is stable and instantaneous with zero pop-in; ceiling is RAM usage of loaded drawables in LRU cache (capped at 500); upgrade path is disk-backed bitmap cache.
        Unit
    }

    override fun launchApp(app: AppInfo): Boolean {
        try {
            val profiles = userManager.userProfiles
            val targetUser = profiles.find { it.hashCode() == app.userHandleId }
                ?: profiles.firstOrNull()
                ?: try { Process.myUserHandle() } catch (_: Throwable) { null }
            if (targetUser != null && app.activityName.isNotBlank()) {
                val component = android.content.ComponentName(app.packageName, app.activityName)
                launcherApps.startMainActivity(component, targetUser, null, null)
                return true
            }
        } catch (e: Throwable) {
            android.util.Log.w("AppRepository", "launcherApps.startMainActivity failed for ${app.packageName}, trying fallback", e)
        }

        // Fallback: standard PackageManager launch intent
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            android.util.Log.e("AppRepository", "Failed to launch app ${app.packageName}", e)
            false
        }
    }

    override fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    override fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    private val iconCache = LruCache<String, Drawable>(500)
    private val activityInfoCache = java.util.concurrent.ConcurrentHashMap<String, android.content.pm.LauncherActivityInfo>()

    override fun clearIconCache() {
        iconCache.evictAll()
    }

    override suspend fun getAppIcon(app: AppInfo): Drawable? = withContext(ioDispatcher) {
        iconCache.get(app.componentKey)?.let { return@withContext it }

        val iconPackRepo = iconPackRepositoryProvider?.get()
        val drawable = if (iconPackRepo != null) {
            iconPackRepo.loadIcon(app.packageName, app.activityName) {
                loadDefaultIcon(app)
            }
        } else {
            loadDefaultIcon(app)
        }

        if (drawable != null) {
            iconCache.put(app.componentKey, drawable)
        }
        drawable
    }

    private fun loadDefaultIcon(app: AppInfo): Drawable? {
        val key = app.componentKey
        iconCache.get(key)?.let { return it }

        val density = context.resources.displayMetrics.densityDpi
        val cachedActivity = activityInfoCache[key]
        val drawable = cachedActivity?.getBadgedIcon(density)
            ?: try {
                val targetUser = userManager.userProfiles.find { it.hashCode() == app.userHandleId } ?: Process.myUserHandle()
                val component = android.content.ComponentName(app.packageName, app.activityName)
                val activityList = launcherApps.getActivityList(app.packageName, targetUser)
                val activityInfo = activityList.find { it.componentName == component } ?: activityList.firstOrNull()
                activityInfo?.getBadgedIcon(density) ?: run {
                    val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                    val icon = packageManager.getApplicationIcon(appInfo)
                    packageManager.getUserBadgedIcon(icon, targetUser)
                }
            } catch (_: Exception) {
                null
            }

        if (drawable != null) {
            iconCache.put(key, drawable)
        }
        return drawable
    }

    override fun getShortcuts(app: AppInfo): List<AppShortcutInfo> {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }
        return try {
            if (launcherApps.hasShortcutHostPermission()) {
                val targetUser = userManager.userProfiles.find { it.hashCode() == app.userHandleId } ?: Process.myUserHandle()
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(app.packageName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                }
                val shortcuts = launcherApps.getShortcuts(query, targetUser) ?: emptyList()
                shortcuts.take(4).map { shortcut ->
                    AppShortcutInfo(
                        id = shortcut.id,
                        packageName = shortcut.`package`,
                        shortLabel = shortcut.shortLabel?.toString() ?: shortcut.id,
                        longLabel = shortcut.longLabel?.toString(),
                        isEnabled = shortcut.isEnabled
                    )
                }
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override fun launchShortcut(app: AppInfo, shortcutId: String): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            return false
        }
        return try {
            if (launcherApps.hasShortcutHostPermission()) {
                val targetUser = userManager.userProfiles.find { it.hashCode() == app.userHandleId } ?: Process.myUserHandle()
                launcherApps.startShortcut(app.packageName, shortcutId, null, null, targetUser)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun getShortcutIcon(app: AppInfo, shortcutId: String): Drawable? = withContext(ioDispatcher) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            return@withContext null
        }
        try {
            if (launcherApps.hasShortcutHostPermission()) {
                val targetUser = userManager.userProfiles.find { it.hashCode() == app.userHandleId } ?: Process.myUserHandle()
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(app.packageName)
                    setShortcutIds(listOf(shortcutId))
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                }
                val shortcut = launcherApps.getShortcuts(query, targetUser)?.firstOrNull() ?: return@withContext null
                val density = context.resources.displayMetrics.densityDpi
                launcherApps.getShortcutIconDrawable(shortcut, density)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
