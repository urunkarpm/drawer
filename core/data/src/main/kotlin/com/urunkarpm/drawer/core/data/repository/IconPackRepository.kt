package com.urunkarpm.drawer.core.data.repository

import android.graphics.drawable.Drawable
import com.urunkarpm.drawer.core.model.IconOverride
import com.urunkarpm.drawer.core.model.IconPackInfo
import kotlinx.coroutines.flow.Flow

interface IconPackRepository {
    val installedIconPacks: Flow<List<IconPackInfo>>
    val activeIconPack: Flow<String?>
    val overrides: Flow<List<IconOverride>>

    suspend fun getInstalledIconPacks(): List<IconPackInfo>
    suspend fun setActiveIconPack(packageName: String?)
    suspend fun setAppOverride(componentName: String, iconPackPackage: String, drawableName: String)
    suspend fun removeAppOverride(componentName: String)
    suspend fun loadIcon(
        packageName: String,
        activityName: String,
        fallback: suspend () -> Drawable?
    ): Drawable?
    suspend fun parseAppFilter(iconPackPackage: String): Map<String, String>
}
