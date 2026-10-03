package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity
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

        val preferencesBackup = LauncherPreferencesBackup(
            themeMode = preferencesDataSource.themeMode.first(),
            dynamicColor = preferencesDataSource.dynamicColor.first(),
            hideStatusBar = preferencesDataSource.hideStatusBar.first(),
            showWeather = preferencesDataSource.showWeather.first(),
            weatherUnit = preferencesDataSource.weatherUnit.first(),
            is24Hour = preferencesDataSource.is24Hour.first(),
            manualCityName = preferencesDataSource.manualCityName.first(),
            manualLat = preferencesDataSource.manualLat.first(),
            manualLon = preferencesDataSource.manualLon.first(),
            dockBackground = preferencesDataSource.dockBackground.first(),
            dockIconSize = preferencesDataSource.dockIconSize.first(),
            dockShowLabels = preferencesDataSource.dockShowLabels.first(),
            dockCornerRadius = preferencesDataSource.dockCornerRadius.first(),
            multiGroupApps = preferencesDataSource.multiGroupApps.first(),
            notificationsPrivacyMode = preferencesDataSource.notificationsPrivacyMode.first(),
            activeIconPack = preferencesDataSource.activeIconPack.first(),
            adaptiveIconShape = preferencesDataSource.adaptiveIconShape.first()
        )

        val backup = LauncherBackup(
            version = 1,
            exportTimestampMillis = System.currentTimeMillis(),
            groups = groups,
            dockItems = dockItems,
            mutedRules = mutedRules,
            iconOverrides = iconOverrides,
            preferences = preferencesBackup
        )

        json.encodeToString(LauncherBackup.serializer(), backup)
    }

    override suspend fun restoreBackupJson(jsonString: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val backup = json.decodeFromString(LauncherBackup.serializer(), jsonString)

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

            // 5. Restore Preferences
            val prefs = backup.preferences
            preferencesDataSource.setThemeMode(prefs.themeMode)
            preferencesDataSource.setDynamicColor(prefs.dynamicColor)
            preferencesDataSource.setHideStatusBar(prefs.hideStatusBar)
            preferencesDataSource.setShowWeather(prefs.showWeather)
            preferencesDataSource.setWeatherUnit(prefs.weatherUnit)
            preferencesDataSource.setIs24Hour(prefs.is24Hour)
            preferencesDataSource.setManualLocation(prefs.manualCityName, prefs.manualLat, prefs.manualLon)
            preferencesDataSource.setDockBackground(prefs.dockBackground)
            preferencesDataSource.setDockIconSize(prefs.dockIconSize)
            preferencesDataSource.setDockShowLabels(prefs.dockShowLabels)
            preferencesDataSource.setDockCornerRadius(prefs.dockCornerRadius)
            preferencesDataSource.setMultiGroupApps(prefs.multiGroupApps)
            preferencesDataSource.setNotificationsPrivacyMode(prefs.notificationsPrivacyMode)
            preferencesDataSource.setActiveIconPack(prefs.activeIconPack)
            preferencesDataSource.setAdaptiveIconShape(prefs.adaptiveIconShape)
        }
    }
}
