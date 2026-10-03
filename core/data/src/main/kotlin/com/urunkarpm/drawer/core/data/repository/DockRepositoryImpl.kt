package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DockRepositoryImpl @Inject constructor(
    private val dockDao: DockDao,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : DockRepository {

    override val dockItems: Flow<List<DockItem>> = dockDao.getDockItems().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun addToDock(app: AppInfo): Boolean = withContext(ioDispatcher) {
        val currentItems = dockDao.getDockItems().first()
        if (currentItems.size >= 5) {
            return@withContext false
        }

        // Avoid adding the exact same app twice to the dock
        if (currentItems.any { it.packageName == app.packageName && it.activityName == app.activityName }) {
            return@withContext false
        }

        val nextPosition = currentItems.size
        dockDao.insertDockItem(
            DockItemEntity(
                position = nextPosition,
                packageName = app.packageName,
                activityName = app.activityName,
                userHandleId = app.userHandleId,
                customLabel = app.label
            )
        )
        true
    }

    override suspend fun removeFromDock(position: Int) = withContext(ioDispatcher) {
        val currentItems = dockDao.getDockItems().first().toMutableList()
        currentItems.removeAll { it.position == position }

        // Re-index remaining items so positions are contiguous 0..(size-1)
        val reindexed = currentItems.sortedBy { it.position }.mapIndexed { index, entity ->
            entity.copy(position = index)
        }
        dockDao.replaceDock(reindexed)
    }

    override suspend fun reorderDock(fromPosition: Int, toPosition: Int) = withContext(ioDispatcher) {
        val currentItems = dockDao.getDockItems().first().sortedBy { it.position }.toMutableList()
        if (fromPosition !in currentItems.indices || toPosition !in currentItems.indices || fromPosition == toPosition) {
            return@withContext
        }

        val itemToMove = currentItems.removeAt(fromPosition)
        currentItems.add(toPosition, itemToMove)

        val reindexed = currentItems.mapIndexed { index, entity ->
            entity.copy(position = index)
        }
        dockDao.replaceDock(reindexed)
    }

    override suspend fun clearDock() = withContext(ioDispatcher) {
        dockDao.clearDock()
    }
}
