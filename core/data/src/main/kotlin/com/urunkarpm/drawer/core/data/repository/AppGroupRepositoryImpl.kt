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
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AppGroupRepositoryImpl @Inject constructor(
    private val appGroupDao: AppGroupDao,
    private val appRepositoryProvider: Provider<AppRepository>,
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
        val existingItems = appGroupDao.getAllGroupItems().first()
        if (existingItems.isEmpty()) {
            seedDefaultGroupItems()
        }
    }

    private suspend fun seedDefaultGroupItems() {
        try {
            val appRepo = appRepositoryProvider.get()
            var apps: List<AppInfo> = appRepo.installedApps.first()
            if (apps.isEmpty()) {
                appRepo.refreshApps()
                apps = appRepo.installedApps.first()
            }
            if (apps.isEmpty()) return

            val itemsToInsert = mutableListOf<AppGroupItemEntity>()
            val groupCounters = mutableMapOf<String, Int>()

            for (app in apps) {
                val targetGroupId = determineCategory(app) ?: continue
                val currentIndex = groupCounters.getOrDefault(targetGroupId, 0)
                if (currentIndex < 12) {
                    itemsToInsert.add(
                        AppGroupItemEntity(
                            id = UUID.randomUUID().toString(),
                            groupId = targetGroupId,
                            packageName = app.packageName,
                            activityName = app.activityName,
                            userHandleId = app.userHandleId,
                            orderIndex = currentIndex,
                            customLabel = app.label
                        )
                    )
                    groupCounters[targetGroupId] = currentIndex + 1
                }
            }

            if (itemsToInsert.isNotEmpty()) {
                appGroupDao.insertGroupItems(itemsToInsert)
            }
        } catch (_: Exception) {}
    }

    private fun determineCategory(app: AppInfo): String? {
        if (app.isWorkProfile) return "group_work"

        val pkg = app.packageName.lowercase()
        val label = app.label.lowercase()

        // 1. Social
        if (pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
            pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("twitter") ||
            pkg.contains("threads") || pkg.contains("snapchat") || pkg.contains("reddit") ||
            pkg.contains("discord") || pkg.contains("linkedin") || pkg.contains("tiktok") ||
            pkg.contains("wechat") || pkg.contains("messaging") || pkg.contains("messages") ||
            label.contains("chat") || label.contains("message")) {
            return "group_social"
        }

        // 2. Finance
        if (pkg.contains("pay") || pkg.contains("wallet") || pkg.contains("bank") ||
            pkg.contains("gpay") || pkg.contains("phonepe") || pkg.contains("paytm") ||
            pkg.contains("cred") || pkg.contains("crypto") || pkg.contains("binance") ||
            pkg.contains("coinbase") || pkg.contains("zerodha") || pkg.contains("groww") ||
            pkg.contains("upstox") || pkg.contains("revolut") || pkg.contains("paypal") ||
            label.contains("bank") || label.contains("finance") || label.contains("money") ||
            label.contains("wallet") || label.contains("pay")) {
            return "group_finance"
        }

        // 3. Work / Productivity
        if (pkg.contains("mail") || pkg.contains("gmail") || pkg.contains("docs") ||
            pkg.contains("sheets") || pkg.contains("slides") || pkg.contains("drive") ||
            pkg.contains("office") || pkg.contains("slack") || pkg.contains("teams") ||
            pkg.contains("calendar") || pkg.contains("notes") || pkg.contains("keep") ||
            pkg.contains("tasks") || pkg.contains("meet") || pkg.contains("zoom") ||
            pkg.contains("notion") || pkg.contains("trello") || pkg.contains("jira") ||
            pkg.contains("outlook") || pkg.contains("wps") || label.contains("mail") ||
            label.contains("calendar") || label.contains("notes") || label.contains("task")) {
            return "group_work"
        }

        // 4. Media
        if (pkg.contains("youtube") || pkg.contains("music") || pkg.contains("spotify") ||
            pkg.contains("netflix") || pkg.contains("prime") || pkg.contains("hotstar") ||
            pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photos") ||
            pkg.contains("vlc") || pkg.contains("podcast") || pkg.contains("twitch") ||
            pkg.contains("sound") || pkg.contains("radio") || pkg.contains("video") ||
            pkg.contains("player") || label.contains("music") || label.contains("camera") ||
            label.contains("gallery") || label.contains("photo") || label.contains("video")) {
            return "group_media"
        }

        // 5. Tools
        if (pkg.contains("settings") || pkg.contains("calculator") || pkg.contains("clock") ||
            pkg.contains("files") || pkg.contains("filemanager") || pkg.contains("browser") ||
            pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("edge") ||
            pkg.contains("compass") || pkg.contains("weather") || pkg.contains("terminal") ||
            pkg.contains("vending") || label.contains("settings") || label.contains("calculator") ||
            label.contains("clock") || label.contains("files") || label.contains("browser")) {
            return "group_tools"
        }

        return null
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
