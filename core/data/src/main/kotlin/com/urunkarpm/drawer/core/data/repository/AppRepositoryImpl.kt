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
    private val iconPackRepositoryProvider: javax.inject.Provider<IconPackRepository>? = null
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
    }

    private fun registerLauncherAppsCallback() {
        try {
            val looper = Looper.getMainLooper() ?: return
            val callback = object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) {
                    scope.launch { refreshApps() }
                }

                override fun onPackageRemoved(packageName: String, user: UserHandle) {
                    scope.launch { refreshApps() }
                }

                override fun onPackageChanged(packageName: String, user: UserHandle) {
                    scope.launch { refreshApps() }
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
        iconCache.evictAll()
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

                appsList.add(
                    AppInfo(
                        packageName = packageName,
                        activityName = activityName,
                        label = label,
                        userHandleId = userHandleId,
                        isWorkProfile = isWorkProfile,
                        installTimeMillis = installTime,
                        lastUpdateTimeMillis = updateTime
                    )
                )
            }
        }

        appsList.sortBy { it.label.lowercase() }
        _installedApps.value = appsList
    }

    override fun launchApp(app: AppInfo): Boolean {
        return try {
            val profiles = userManager.userProfiles
            val targetUser = profiles.find { it.hashCode() == app.userHandleId }
                ?: profiles.firstOrNull()
                ?: try { Process.myUserHandle() } catch (_: Throwable) { null }
                ?: return false
            val component = android.content.ComponentName(app.packageName, app.activityName)
            launcherApps.startMainActivity(component, targetUser, null, null)
            true
        } catch (_: Exception) {
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

    private val iconCache = LruCache<String, Drawable>(250)

    override suspend fun getAppIcon(app: AppInfo): Drawable? = withContext(ioDispatcher) {
        val iconPackRepo = iconPackRepositoryProvider?.get()
        if (iconPackRepo != null) {
            iconPackRepo.loadIcon(app.packageName, app.activityName) {
                loadDefaultIcon(app)
            }
        } else {
            loadDefaultIcon(app)
        }
    }

    private fun loadDefaultIcon(app: AppInfo): Drawable? {
        val key = app.componentKey
        iconCache.get(key)?.let { return it }

        val profiles = userManager.userProfiles
        val targetUser = profiles.find { it.hashCode() == app.userHandleId } ?: Process.myUserHandle()
        val component = android.content.ComponentName(app.packageName, app.activityName)
        val activityList = launcherApps.getActivityList(app.packageName, targetUser)
        val activityInfo = activityList.find { it.componentName == component } ?: activityList.firstOrNull()

        val density = context.resources.displayMetrics.densityDpi
        val drawable = activityInfo?.getBadgedIcon(density)
            ?: try {
                val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                val icon = packageManager.getApplicationIcon(appInfo)
                packageManager.getUserBadgedIcon(icon, targetUser)
            } catch (_: Exception) {
                null
            }

        if (drawable != null) {
            iconCache.put(key, drawable)
        }
        return drawable
    }
}
