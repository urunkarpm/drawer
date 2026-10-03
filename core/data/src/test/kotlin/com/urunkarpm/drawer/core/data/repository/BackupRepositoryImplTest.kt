package com.urunkarpm.drawer.core.data.repository

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
import com.urunkarpm.drawer.core.model.DockItem
import com.urunkarpm.drawer.core.model.LauncherBackup
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appGroupDao: AppGroupDao = mockk(relaxed = true)
    private val dockDao: DockDao = mockk(relaxed = true)
    private val mutedAppDao: MutedAppDao = mockk(relaxed = true)
    private val iconPackOverrideDao: IconPackOverrideDao = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val groupsFlow = MutableStateFlow<List<AppGroupEntity>>(emptyList())
    private val groupItemsFlow = MutableStateFlow<List<AppGroupItemEntity>>(emptyList())
    private val dockFlow = MutableStateFlow<List<DockItemEntity>>(emptyList())
    private val mutedRulesFlow = MutableStateFlow<List<MutedAppRuleEntity>>(emptyList())
    private val overridesFlow = MutableStateFlow<List<IconPackOverrideEntity>>(emptyList())

    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        every { appGroupDao.getAllGroups() } returns groupsFlow
        every { appGroupDao.getAllGroupItems() } returns groupItemsFlow
        every { dockDao.getDockItems() } returns dockFlow
        every { mutedAppDao.getAllRules() } returns mutedRulesFlow
        every { iconPackOverrideDao.getAllOverrides() } returns overridesFlow

        every { preferencesDataSource.themeMode } returns MutableStateFlow("DARK")
        every { preferencesDataSource.dynamicColor } returns MutableStateFlow(true)
        every { preferencesDataSource.hideStatusBar } returns MutableStateFlow(false)
        every { preferencesDataSource.showWeather } returns MutableStateFlow(true)
        every { preferencesDataSource.weatherUnit } returns MutableStateFlow("CELSIUS")
        every { preferencesDataSource.is24Hour } returns MutableStateFlow(true)
        every { preferencesDataSource.manualCityName } returns MutableStateFlow("Berlin")
        every { preferencesDataSource.manualLat } returns MutableStateFlow(52.52)
        every { preferencesDataSource.manualLon } returns MutableStateFlow(13.405)
        every { preferencesDataSource.dockBackground } returns MutableStateFlow("BLUR")
        every { preferencesDataSource.dockIconSize } returns MutableStateFlow(56f)
        every { preferencesDataSource.dockShowLabels } returns MutableStateFlow(false)
        every { preferencesDataSource.dockCornerRadius } returns MutableStateFlow(24f)
        every { preferencesDataSource.multiGroupApps } returns MutableStateFlow(false)
        every { preferencesDataSource.notificationsPrivacyMode } returns MutableStateFlow(false)
        every { preferencesDataSource.activeIconPack } returns MutableStateFlow(null)
        every { preferencesDataSource.adaptiveIconShape } returns MutableStateFlow("SQUIRCLE")

        repository = BackupRepositoryImpl(
            appGroupDao = appGroupDao,
            dockDao = dockDao,
            mutedAppDao = mutedAppDao,
            iconPackOverrideDao = iconPackOverrideDao,
            preferencesDataSource = preferencesDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun createBackupJson_serializesCurrentDatabaseAndPreferences() = runTest(testDispatcher) {
        val testGroup = AppGroupEntity(id = "g1", name = "Work", orderIndex = 0)
        val testItem = AppGroupItemEntity(id = "i1", groupId = "g1", packageName = "com.work.app", activityName = "MainActivity")
        val testDock = DockItemEntity(position = 0, packageName = "com.phone.app", activityName = "Dialer")
        val testMute = MutedAppRuleEntity(packageName = "com.spam.app", ruleType = "HIDE", autoDismiss = true)

        groupsFlow.value = listOf(testGroup)
        groupItemsFlow.value = listOf(testItem)
        dockFlow.value = listOf(testDock)
        mutedRulesFlow.value = listOf(testMute)

        val jsonString = repository.createBackupJson()

        assertTrue(jsonString.contains("com.work.app"))
        assertTrue(jsonString.contains("com.phone.app"))
        assertTrue(jsonString.contains("com.spam.app"))
        assertTrue(jsonString.contains("\"themeMode\": \"DARK\""))
        assertTrue(jsonString.contains("\"adaptiveIconShape\": \"SQUIRCLE\""))

        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(LauncherBackup.serializer(), jsonString)
        assertEquals(1, decoded.groups.size)
        assertEquals("Work", decoded.groups[0].name)
        assertEquals(1, decoded.groups[0].items.size)
        assertEquals(1, decoded.dockItems.size)
        assertEquals("com.phone.app", decoded.dockItems[0].packageName)
        assertEquals(1, decoded.mutedRules.size)
        assertEquals("com.spam.app", decoded.mutedRules[0].packageName)
        assertEquals("DARK", decoded.preferences.themeMode)
    }

    @Test
    fun restoreBackupJson_replacesDatabaseTablesAndPreferences() = runTest(testDispatcher) {
        val backup = LauncherBackup(
            version = 1,
            exportTimestampMillis = 1000L,
            groups = listOf(
                AppGroup(
                    id = "g_social",
                    name = "Social",
                    items = listOf(
                        com.urunkarpm.drawer.core.model.AppGroupItem(
                            id = "i_chat",
                            groupId = "g_social",
                            packageName = "com.chat",
                            activityName = "ChatActivity"
                        )
                    )
                )
            ),
            dockItems = listOf(
                DockItem(
                    position = 1,
                    packageName = "com.browser",
                    activityName = "BrowserActivity"
                )
            ),
            mutedRules = listOf(
                MutedAppRule(
                    packageName = "com.promo",
                    ruleType = MuteRuleType.SNOOZE_1H,
                    snoozedUntilTimestamp = 5000L,
                    autoDismiss = false
                )
            ),
            preferences = com.urunkarpm.drawer.core.model.LauncherPreferencesBackup(
                themeMode = "AMOLED",
                dynamicColor = false,
                dockBackground = "SOLID",
                adaptiveIconShape = "CIRCLE"
            )
        )

        val jsonString = Json.encodeToString(LauncherBackup.serializer(), backup)
        val result = repository.restoreBackupJson(jsonString)

        assertTrue(result.isSuccess)
        coVerify { appGroupDao.replaceAllGroups(match { it.size == 1 && it[0].id == "g_social" }, match { it.size == 1 && it[0].packageName == "com.chat" }) }
        coVerify { dockDao.replaceDock(match { it.size == 1 && it[0].packageName == "com.browser" }) }
        coVerify { mutedAppDao.clearAllRules() }
        coVerify { mutedAppDao.insertRules(match { it.size == 1 && it[0].packageName == "com.promo" }) }
        coVerify { preferencesDataSource.setThemeMode("AMOLED") }
        coVerify { preferencesDataSource.setDynamicColor(false) }
        coVerify { preferencesDataSource.setDockBackground("SOLID") }
        coVerify { preferencesDataSource.setAdaptiveIconShape("CIRCLE") }
    }

    @Test
    fun restoreBackupJson_returnsFailureOnMalformedJson() = runTest(testDispatcher) {
        val result = repository.restoreBackupJson("invalid json content")
        assertTrue(result.isFailure)
    }
}
