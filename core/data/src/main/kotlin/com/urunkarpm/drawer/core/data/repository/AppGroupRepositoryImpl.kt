package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppGroupRepositoryImpl @Inject constructor(
    private val appGroupDao: AppGroupDao,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : AppGroupRepository {

    private val defaultGroups = listOf(
        AppGroupEntity(id = "group_work", name = "Work", iconName = "work", colorHex = "#1E88E5", orderIndex = 0, isExpanded = true),
        AppGroupEntity(id = "group_social", name = "Social", iconName = "chat", colorHex = "#E91E63", orderIndex = 1, isExpanded = true),
        AppGroupEntity(id = "group_finance", name = "Finance", iconName = "account_balance", colorHex = "#4CAF50", orderIndex = 2, isExpanded = true),
        AppGroupEntity(id = "group_media", name = "Media", iconName = "play_circle", colorHex = "#FF9800", orderIndex = 3, isExpanded = true),
        AppGroupEntity(id = "group_tools", name = "Tools", iconName = "build", colorHex = "#9C27B0", orderIndex = 4, isExpanded = true)
    )

    private suspend fun ensureSeeded() {
        val existing = appGroupDao.getAllGroups().first()
        if (existing.isEmpty()) {
            appGroupDao.insertGroups(defaultGroups)
        }
    }

    override val groups: Flow<List<AppGroup>> = combine(
        appGroupDao.getAllGroups(),
        appGroupDao.getAllGroupItems()
    ) { groupEntities, itemEntities ->
        val itemsByGroup = itemEntities.groupBy { it.groupId }
        groupEntities.map { groupEntity ->
            val items = itemsByGroup[groupEntity.id]
                ?.sortedBy { it.orderIndex }
                ?.map { it.toDomain() }
                ?: emptyList()
            groupEntity.toDomain().copy(items = items)
        }
    }.onStart {
        ensureSeeded()
    }.flowOn(ioDispatcher)

    override suspend fun createGroup(
        name: String,
        iconName: String,
        colorHex: String,
        viewType: GroupViewType,
        columnCount: Int,
        sortOrder: GroupSortOrder
    ): AppGroup = withContext(ioDispatcher) {
        val currentGroups = appGroupDao.getAllGroups().first()
        val id = UUID.randomUUID().toString()
        val entity = AppGroupEntity(
            id = id,
            name = name.trim(),
            iconName = iconName,
            colorHex = colorHex,
            orderIndex = currentGroups.size,
            isExpanded = true,
            viewType = viewType.name,
            columnCount = columnCount,
            sortOrder = sortOrder.name
        )
        appGroupDao.insertGroup(entity)
        entity.toDomain()
    }

    override suspend fun updateGroup(group: AppGroup) = withContext(ioDispatcher) {
        appGroupDao.updateGroup(AppGroupEntity.fromDomain(group))
    }

    override suspend fun deleteGroup(groupId: String) = withContext(ioDispatcher) {
        appGroupDao.deleteGroupById(groupId)
        // Re-index remaining groups
        val remaining = appGroupDao.getAllGroups().first().sortedBy { it.orderIndex }
        val reindexed = remaining.mapIndexed { index, entity ->
            entity.copy(orderIndex = index)
        }
        appGroupDao.insertGroups(reindexed)
    }

    override suspend fun toggleGroupExpanded(groupId: String) = withContext(ioDispatcher) {
        val currentGroups = appGroupDao.getAllGroups().first()
        val target = currentGroups.find { it.id == groupId } ?: return@withContext
        appGroupDao.updateGroup(target.copy(isExpanded = !target.isExpanded))
    }

    override suspend fun reorderGroups(fromIndex: Int, toIndex: Int) = withContext(ioDispatcher) {
        val currentGroups = appGroupDao.getAllGroups().first().sortedBy { it.orderIndex }.toMutableList()
        if (fromIndex !in currentGroups.indices || toIndex !in currentGroups.indices || fromIndex == toIndex) {
            return@withContext
        }
        val item = currentGroups.removeAt(fromIndex)
        currentGroups.add(toIndex, item)
        val reindexed = currentGroups.mapIndexed { index, entity ->
            entity.copy(orderIndex = index)
        }
        appGroupDao.insertGroups(reindexed)
    }

    override suspend fun assignAppToGroup(
        groupId: String,
        app: AppInfo,
        allowMultiGroup: Boolean
    ) = withContext(ioDispatcher) {
        if (!allowMultiGroup) {
            appGroupDao.deleteItemsByComponent(app.packageName, app.activityName)
        }
        val currentItems = appGroupDao.getItemsForGroup(groupId).first()
        if (currentItems.any { it.packageName == app.packageName && it.activityName == app.activityName }) {
            return@withContext
        }

        val nextIndex = currentItems.size
        appGroupDao.insertGroupItem(
            AppGroupItemEntity(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                packageName = app.packageName,
                activityName = app.activityName,
                userHandleId = app.userHandleId,
                orderIndex = nextIndex,
                customLabel = app.label
            )
        )
    }

    override suspend fun removeAppFromGroup(
        groupId: String,
        packageName: String,
        activityName: String
    ) = withContext(ioDispatcher) {
        appGroupDao.deleteGroupItem(groupId, packageName, activityName)
        val remaining = appGroupDao.getItemsForGroup(groupId).first().sortedBy { it.orderIndex }
        val reindexed = remaining.mapIndexed { index, entity ->
            entity.copy(orderIndex = index)
        }
        appGroupDao.insertGroupItems(reindexed)
    }

    override suspend fun removeGroupItemById(itemId: String) = withContext(ioDispatcher) {
        appGroupDao.deleteGroupItemById(itemId)
    }
}
