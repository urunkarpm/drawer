package com.urunkarpm.drawer.core.data.repository

import android.graphics.drawable.Drawable
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppShortcutInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val installedApps: Flow<List<AppInfo>>
    suspend fun refreshApps()
    fun launchApp(app: AppInfo): Boolean
    fun openAppDetails(packageName: String)
    fun uninstallApp(packageName: String)
    suspend fun getAppIcon(app: AppInfo): Drawable?
    fun clearIconCache()
    fun getShortcuts(app: AppInfo): List<AppShortcutInfo>
    fun launchShortcut(app: AppInfo, shortcutId: String): Boolean
    suspend fun getShortcutIcon(app: AppInfo, shortcutId: String): Drawable?
}
