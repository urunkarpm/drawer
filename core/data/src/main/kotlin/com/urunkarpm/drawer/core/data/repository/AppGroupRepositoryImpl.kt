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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        AppGroupEntity(id = "group_work", name = "Work", iconName = "work", colorHex = "#1E88E5", orderIndex = 0, isExpanded = false),
        AppGroupEntity(id = "group_social", name = "Social", iconName = "chat", colorHex = "#E91E63", orderIndex = 1, isExpanded = false),
        AppGroupEntity(id = "group_finance", name = "Finance", iconName = "account_balance", colorHex = "#4CAF50", orderIndex = 2, isExpanded = false),
        AppGroupEntity(id = "group_media", name = "Media", iconName = "play_circle", colorHex = "#FF9800", orderIndex = 3, isExpanded = false),
        AppGroupEntity(id = "group_tools", name = "Tools", iconName = "build", colorHex = "#9C27B0", orderIndex = 4, isExpanded = false)
    )

    private var hasSeedAttempted = false

    private suspend fun ensureSeeded() {
        val existing = appGroupDao.getAllGroups().first()
        if (existing.isEmpty()) {
            appGroupDao.insertGroups(defaultGroups)
        }
        if (hasSeedAttempted) return
        val existingItems = appGroupDao.getAllGroupItems().first()
        // If items are sparse or default groups are mostly empty, trigger seed
        if (existingItems.size < 4) {
            CoroutineScope(ioDispatcher).launch {
                seedDefaultGroupItemsReactive()
            }
        } else {
            hasSeedAttempted = true
        }
    }

    private suspend fun seedDefaultGroupItemsReactive() {
        try {
            val appRepo = appRepositoryProvider.get()
            var apps = appRepo.installedApps.first()
            if (apps.isEmpty()) {
                appRepo.refreshApps()
                apps = appRepo.installedApps.first()
            }
            doSeedItems(apps)
            hasSeedAttempted = true
        } catch (_: Exception) {
            hasSeedAttempted = true
        }
    }

    override suspend fun autoPopulateGroups() = withContext(ioDispatcher) {
        val appRepo = appRepositoryProvider.get()
        var apps = appRepo.installedApps.first()
        if (apps.isEmpty()) {
            appRepo.refreshApps()
            apps = appRepo.installedApps.first()
        }
        if (apps.isNotEmpty()) {
            doSeedItems(apps)
        }
    }

    private suspend fun doSeedItems(apps: List<AppInfo>) {
        val groups = appGroupDao.getAllGroups().first()
        val existingItems = appGroupDao.getAllGroupItems().first()
        val itemsByGroup = existingItems.groupBy { it.groupId }
        val allAssignedComponents = existingItems.map { "${it.packageName}/${it.activityName}#${it.userHandleId}" }.toMutableSet()

        val itemsToInsert = mutableListOf<AppGroupItemEntity>()

        for (group in groups) {
            val currentGroupItems = itemsByGroup[group.id].orEmpty()
            if (currentGroupItems.size >= 12) continue

            val groupPackageNames = currentGroupItems.map { it.packageName }.toSet()
            var currentOrder = currentGroupItems.size

            for (app in apps) {
                if (currentOrder >= 12) break
                val targetCategory = determineCategory(app) ?: continue
                val categoryName = targetCategory.removePrefix("group_")
                val matchesGroup = targetCategory == group.id ||
                    group.name.equals(categoryName, ignoreCase = true) ||
                    group.id.contains(categoryName, ignoreCase = true)
                if (matchesGroup) {
                    if (app.packageName in groupPackageNames) continue
                    if (app.componentKey in allAssignedComponents) continue

                    itemsToInsert.add(
                        AppGroupItemEntity(
                            id = UUID.randomUUID().toString(),
                            groupId = group.id,
                            packageName = app.packageName,
                            activityName = app.activityName,
                            userHandleId = app.userHandleId,
                            orderIndex = currentOrder,
                            customLabel = app.label
                        )
                    )
                    allAssignedComponents.add(app.componentKey)
                    currentOrder++
                }
            }
        }

        if (itemsToInsert.isNotEmpty()) {
            appGroupDao.insertGroupItems(itemsToInsert)
        }
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
            pkg.contains("frontpage") || pkg.contains("pinterest") ||
            label.contains("chat") || label.contains("message") || label.contains("social") ||
            label.contains("reddit") || label.contains("instagram") || label.contains("telegram")) {
            return "group_social"
        }

        // 2. Finance
        if (pkg.contains("pay") || pkg.contains("wallet") || pkg.contains("bank") ||
            pkg.contains("gpay") || pkg.contains("phonepe") || pkg.contains("paytm") ||
            pkg.contains("cred") || pkg.contains("crypto") || pkg.contains("binance") ||
            pkg.contains("coinbase") || pkg.contains("zerodha") || pkg.contains("groww") ||
            pkg.contains("upstox") || pkg.contains("revolut") || pkg.contains("paypal") ||
            pkg.contains("splitwise") || pkg.contains("paisa") || pkg.contains("money") ||
            pkg.contains("bhishi") || pkg.contains("acko") || pkg.contains("saraswat") ||
            label.contains("bank") || label.contains("finance") || label.contains("money") ||
            label.contains("wallet") || label.contains("pay") || label.contains("groww") ||
            label.contains("splitwise")) {
            return "group_finance"
        }

        // 3. Work / Productivity
        if (pkg.contains("mail") || pkg.contains("gmail") || pkg.contains("docs") ||
            pkg.contains("sheets") || pkg.contains("slides") || pkg.contains("drive") ||
            pkg.contains("office") || pkg.contains("slack") || pkg.contains("teams") ||
            pkg.contains("calendar") || pkg.contains("notes") || pkg.contains("keep") ||
            pkg.contains("tasks") || pkg.contains("meet") || pkg.contains("zoom") ||
            pkg.contains("notion") || pkg.contains("trello") || pkg.contains("jira") ||
            pkg.contains("outlook") || pkg.contains("wps") || pkg.contains("github") ||
            pkg.contains("claude") || pkg.contains("chatgpt") || pkg.contains("openai") ||
            pkg.contains("kimichat") || pkg.contains("deepseek") || pkg.contains("qwen") ||
            pkg.contains("grok") || pkg.contains("perplexity") || pkg.contains("libreoffice") ||
            label.contains("mail") || label.contains("calendar") || label.contains("notes") ||
            label.contains("task") || label.contains("office") || label.contains("teams") ||
            label.contains("chatgpt") || label.contains("claude") || label.contains("docs")) {
            return "group_work"
        }

        // 4. Media
        if (pkg.contains("youtube") || pkg.contains("music") || pkg.contains("spotify") ||
            pkg.contains("netflix") || pkg.contains("prime") || pkg.contains("hotstar") ||
            pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photos") ||
            pkg.contains("vlc") || pkg.contains("podcast") || pkg.contains("twitch") ||
            pkg.contains("sound") || pkg.contains("radio") || pkg.contains("video") ||
            pkg.contains("player") || pkg.contains("shazam") || pkg.contains("stremio") ||
            pkg.contains("spotiflac") || pkg.contains("fiio") || pkg.contains("soundcloud") ||
            label.contains("music") || label.contains("camera") || label.contains("gallery") ||
            label.contains("photo") || label.contains("video") || label.contains("vlc") ||
            label.contains("shazam") || label.contains("spotify")) {
            return "group_media"
        }

        // 5. Tools
        if (pkg.contains("settings") || pkg.contains("calculator") || pkg.contains("clock") ||
            pkg.contains("files") || pkg.contains("filemanager") || pkg.contains("browser") ||
            pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("edge") ||
            pkg.contains("brave") || pkg.contains("localsend") || pkg.contains("rar") ||
            pkg.contains("termux") || pkg.contains("compass") || pkg.contains("weather") ||
            pkg.contains("terminal") || pkg.contains("vending") || pkg.contains("lens") ||
            pkg.contains("authenticator") || pkg.contains("digilocker") || pkg.contains("fdm") ||
            label.contains("settings") || label.contains("calculator") || label.contains("clock") ||
            label.contains("files") || label.contains("browser") || label.contains("terminal")) {
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
        appGroupDao.updateGroups(reindexed)
    }

    override suspend fun toggleGroupExpanded(groupId: String) = withContext(ioDispatcher) {
        val currentGroups = appGroupDao.getAllGroups().first()
        val target = currentGroups.find { it.id == groupId } ?: return@withContext
        val newExpanded = !target.isExpanded
        // "at a time only one drawer can be opened" - expand target, collapse all others via SQL UPDATE
        appGroupDao.setOnlyGroupExpanded(groupId, newExpanded)
    }

    override suspend fun collapseAllGroups() = withContext(ioDispatcher) {
        appGroupDao.collapseAllGroups()
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
        appGroupDao.updateGroups(reindexed)
    }

    override suspend fun assignAppToGroup(
        groupId: String,
        app: AppInfo,
        allowMultiGroup: Boolean,
        targetIndex: Int?
    ) = withContext(ioDispatcher) {
        val currentItems = appGroupDao.getItemsForGroup(groupId).first().toMutableList()
        val existingItem = currentItems.find { it.packageName == app.packageName && it.activityName == app.activityName }
        
        if (existingItem != null) {
            currentItems.remove(existingItem)
        }
        
        if (!allowMultiGroup) {
            // Delete from ALL other groups in DB if multi-group is disabled
            appGroupDao.deleteItemsByComponent(app.packageName, app.activityName)
        } else {
            appGroupDao.deleteGroupItem(groupId, app.packageName, app.activityName)
        }
        
        val insertIndex = targetIndex?.coerceIn(0, currentItems.size) ?: currentItems.size
        
        val newItem = (existingItem ?: AppGroupItemEntity(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            packageName = app.packageName,
            activityName = app.activityName,
            userHandleId = app.userHandleId,
            orderIndex = insertIndex,
            customLabel = app.label
        )).copy(
            id = existingItem?.id ?: UUID.randomUUID().toString(),
            groupId = groupId,
            orderIndex = insertIndex
        )
        
        currentItems.add(insertIndex, newItem)
        
        // Re-index all items to ensure gaps are closed and bounds are correct
        val reindexed = currentItems.mapIndexed { index, entity ->
            entity.copy(orderIndex = index)
        }
        
        appGroupDao.insertGroupItems(reindexed)
    }

    override suspend fun moveAppBetweenGroups(
        sourceGroupId: String,
        targetGroupId: String,
        app: AppInfo,
        targetIndex: Int?
    ) = withContext(ioDispatcher) {
        if (sourceGroupId == targetGroupId) {
            assignAppToGroup(targetGroupId, app, allowMultiGroup = true, targetIndex = targetIndex)
            return@withContext
        }

        // 1. Remove from sourceGroupId and reindex
        appGroupDao.deleteGroupItemsByPackage(sourceGroupId, app.packageName)
        val remainingSource = appGroupDao.getItemsForGroup(sourceGroupId).first().sortedBy { it.orderIndex }
        val reindexedSource = remainingSource.mapIndexed { index, entity ->
            entity.copy(orderIndex = index)
        }
        appGroupDao.insertGroupItems(reindexedSource)

        // 2. Remove any pre-existing instance in targetGroupId and insert at targetIndex
        appGroupDao.deleteGroupItemsByPackage(targetGroupId, app.packageName)
        val currentTargetItems = appGroupDao.getItemsForGroup(targetGroupId).first().sortedBy { it.orderIndex }.toMutableList()
        val insertIndex = targetIndex?.coerceIn(0, currentTargetItems.size) ?: currentTargetItems.size
        val newItem = AppGroupItemEntity(
            id = UUID.randomUUID().toString(),
            groupId = targetGroupId,
            packageName = app.packageName,
            activityName = app.activityName,
            userHandleId = app.userHandleId,
            orderIndex = insertIndex,
            customLabel = app.label
        )
        currentTargetItems.add(insertIndex, newItem)
        val reindexedTarget = currentTargetItems.mapIndexed { index, entity ->
            entity.copy(orderIndex = index)
        }
        appGroupDao.insertGroupItems(reindexedTarget)
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
