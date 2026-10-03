package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val installedApps: Flow<List<AppInfo>>
    suspend fun refreshApps()
    fun launchApp(app: AppInfo): Boolean
    fun openAppDetails(packageName: String)
    fun uninstallApp(packageName: String)
}
