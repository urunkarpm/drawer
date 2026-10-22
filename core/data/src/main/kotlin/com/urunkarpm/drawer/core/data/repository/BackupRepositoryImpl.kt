package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.dao.WidgetDao
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity
import com.urunkarpm.drawer.core.database.entity.WidgetDrawerEntity
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.LauncherBackup
import com.urunkarpm.drawer.core.model.LauncherPreferencesBackup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val appGroupDao: AppGroupDao,
    private val dockDao: DockDao,
    private val mutedAppDao: MutedAppDao,
    private val iconPackOverrideDao: IconPackOverrideDao,
    private val widgetDao: WidgetDao,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun createBackupJson(): String = withContext(ioDispatcher) {
        val groupEntities = appGroupDao.getAllGroups().first()
        val allItemEntities = appGroupDao.getAllGroupItems().first()
        val itemsByGroupId = allItemEntities.groupBy { it.groupId }

        val groups: List<AppGroup> = groupEntities.map { gEntity ->
            val domainGroup = gEntity.toDomain()
            val groupItems = itemsByGroupId[gEntity.id]?.map { it.toDomain() } ?: emptyList()
            domainGroup.copy(items = groupItems)
        }

        val dockItems = dockDao.getDockItems().first().map { it.toDomain() }
        val mutedRules = mutedAppDao.getAllRules().first().map { it.toDomain() }
        val iconOverrides = iconPackOverrideDao.getAllOverrides().first().map { it.toDomain() }
        val rawWidgets = widgetDao.getAllWidgets().first().map { it.toDomain() }
        val widgetsByDrawerId = rawWidgets.filter { it.drawerId != null }.groupBy { it.drawerId }
        val widgetDrawers = widgetDao.getAllDrawers().first().map { drawerEntity ->
            drawerEntity.toDomain(widgetsByDrawerId[drawerEntity.id] ?: emptyList())
        }

        val preferencesBackup = preferencesDataSource.getPreferencesBackup()

        val backup = LauncherBackup(
            version = 3,
            exportTimestampMillis = System.currentTimeMillis(),
            groups = groups,
            dockItems = dockItems,
            mutedRules = mutedRules,
            iconOverrides = iconOverrides,
            preferences = preferencesBackup,
            widgetDrawers = widgetDrawers,
            widgets = rawWidgets
        )

        json.encodeToString(LauncherBackup.serializer(), backup)
    }

    override suspend fun restoreBackupJson(jsonString: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // ponytail: sanitize BOM and whitespace so backups modified on Windows or text editors decode reliably
            val sanitized = jsonString.trim().trimStart('\uFEFF')
            val backup = json.decodeFromString(LauncherBackup.serializer(), sanitized)

            // 1. Restore Groups and Items
            val groupEntities = backup.groups.map { AppGroupEntity.fromDomain(it) }
            val itemEntities = backup.groups.flatMap { group ->
                group.items.map { AppGroupItemEntity.fromDomain(it) }
            }
            appGroupDao.replaceAllGroups(groupEntities, itemEntities)

            // 2. Restore Dock Items
            val dockEntities = backup.dockItems.map { DockItemEntity.fromDomain(it) }
            dockDao.replaceDock(dockEntities)

            // 3. Restore Muted Rules
            mutedAppDao.clearAllRules()
            if (backup.mutedRules.isNotEmpty()) {
                val ruleEntities = backup.mutedRules.map { MutedAppRuleEntity.fromDomain(it) }
                mutedAppDao.insertRules(ruleEntities)
            }

            // 4. Restore Icon Overrides
            iconPackOverrideDao.clearOverrides()
            if (backup.iconOverrides.isNotEmpty()) {
                val overrideEntities = backup.iconOverrides.map { IconPackOverrideEntity.fromDomain(it) }
                iconPackOverrideDao.insertOverrides(overrideEntities)
            }

            // 5. Restore Widgets and Widget Drawers
            // ponytail: merge flat widgets list and drawer-nested widgets to guarantee no widgets or drawers are lost across backup versions.
            widgetDao.clearAll()
            widgetDao.clearAllDrawers()
            if (backup.widgetDrawers.isNotEmpty()) {
                backup.widgetDrawers.forEach { drawer ->
                    widgetDao.insertDrawer(WidgetDrawerEntity.fromDomain(drawer))
                }
            }
            val allWidgets = (backup.widgets + backup.widgetDrawers.flatMap { it.widgets })
                .distinctBy { it.id }
            if (allWidgets.isNotEmpty()) {
                allWidgets.forEach { widget ->
                    widgetDao.insertWidget(WidgetItemEntity.fromDomain(widget))
                }
            }

            // 6. Restore Preferences
            preferencesDataSource.restorePreferences(backup.preferences)
        }
    }
}
