package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
import kotlinx.coroutines.flow.Flow

interface DockRepository {
    val dockItems: Flow<List<DockItem>>
    suspend fun addToDock(app: AppInfo): Boolean
    suspend fun removeFromDock(position: Int)
    suspend fun reorderDock(fromPosition: Int, toPosition: Int)
    suspend fun clearDock()
}
