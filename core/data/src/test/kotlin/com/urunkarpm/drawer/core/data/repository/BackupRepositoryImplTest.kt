package com.urunkarpm.drawer.core.data.repository

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
import com.urunkarpm.drawer.core.model.DockItem
import com.urunkarpm.drawer.core.model.LauncherBackup
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import com.urunkarpm.drawer.core.model.WidgetDrawer
import com.urunkarpm.drawer.core.model.WidgetItem
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
    private val widgetDao: WidgetDao = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val groupsFlow = MutableStateFlow<List<AppGroupEntity>>(emptyList())
    private val groupItemsFlow = MutableStateFlow<List<AppGroupItemEntity>>(emptyList())
    private val dockFlow = MutableStateFlow<List<DockItemEntity>>(emptyList())
    private val mutedRulesFlow = MutableStateFlow<List<MutedAppRuleEntity>>(emptyList())
    private val overridesFlow = MutableStateFlow<List<IconPackOverrideEntity>>(emptyList())
    private val widgetDrawersFlow = MutableStateFlow<List<WidgetDrawerEntity>>(emptyList())
    private val widgetsFlow = MutableStateFlow<List<WidgetItemEntity>>(emptyList())

    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        every { appGroupDao.getAllGroups() } returns groupsFlow
        every { appGroupDao.getAllGroupItems() } returns groupItemsFlow
        every { dockDao.getDockItems() } returns dockFlow
        every { mutedAppDao.getAllRules() } returns mutedRulesFlow
        every { iconPackOverrideDao.getAllOverrides() } returns overridesFlow
        every { widgetDao.getAllDrawers() } returns widgetDrawersFlow
        every { widgetDao.getAllWidgets() } returns widgetsFlow

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
        every { preferencesDataSource.twoDrawersSideBySide } returns MutableStateFlow(true)
        every { preferencesDataSource.enableWidgetsPage } returns MutableStateFlow(true)
        every { preferencesDataSource.lockLayout } returns MutableStateFlow(false)
        every { preferencesDataSource.autoArrangeApps } returns MutableStateFlow(true)
        every { preferencesDataSource.autoCategorizeNewApps } returns MutableStateFlow(true)
        every { preferencesDataSource.drawerNotificationGlow } returns MutableStateFlow(true)

        repository = BackupRepositoryImpl(
            appGroupDao = appGroupDao,
            dockDao = dockDao,
            mutedAppDao = mutedAppDao,
            iconPackOverrideDao = iconPackOverrideDao,
            widgetDao = widgetDao,
            preferencesDataSource = preferencesDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun createBackupJson_serializesCurrentDatabaseAndPreferencesIncludingWidgets() = runTest(testDispatcher) {
        val testGroup = AppGroupEntity(id = "g1", name = "Work", orderIndex = 0)
        val testItem = AppGroupItemEntity(id = "i1", groupId = "g1", packageName = "com.work.app", activityName = "MainActivity")
        val testDock = DockItemEntity(position = 0, packageName = "com.phone.app", activityName = "Dialer")
        val testMute = MutedAppRuleEntity(packageName = "com.spam.app", ruleType = "HIDE", autoDismiss = true)
        val testWidgetDrawer = WidgetDrawerEntity(id = "wd1", title = "Tools", orderIndex = 0)
        val testWidget = WidgetItemEntity(id = "w1", appWidgetId = 101, packageName = "com.weather", providerClassName = "WeatherWidget", drawerId = "wd1")

        groupsFlow.value = listOf(testGroup)
        groupItemsFlow.value = listOf(testItem)
        dockFlow.value = listOf(testDock)
        mutedRulesFlow.value = listOf(testMute)
        widgetDrawersFlow.value = listOf(testWidgetDrawer)
        widgetsFlow.value = listOf(testWidget)

        val jsonString = repository.createBackupJson()

        assertTrue(jsonString.contains("com.work.app"))
        assertTrue(jsonString.contains("com.phone.app"))
        assertTrue(jsonString.contains("com.spam.app"))
        assertTrue(jsonString.contains("com.weather"))
        assertTrue(jsonString.contains("WeatherWidget"))
        assertTrue(jsonString.contains("Tools"))

        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString(LauncherBackup.serializer(), jsonString)
        assertEquals(1, decoded.groups.size)
        assertEquals(1, decoded.dockItems.size)
        assertEquals(1, decoded.mutedRules.size)
        assertEquals(1, decoded.widgetDrawers.size)
        assertEquals("Tools", decoded.widgetDrawers[0].title)
        assertEquals(1, decoded.widgets.size)
        assertEquals("com.weather", decoded.widgets[0].packageName)
        assertEquals(101, decoded.widgets[0].appWidgetId)
        assertTrue(decoded.preferences.twoDrawersSideBySide)
        assertTrue(decoded.preferences.autoCategorizeNewApps)
    }

    @Test
    fun restoreBackupJson_restoresDatabasePreferencesAndWidgets() = runTest(testDispatcher) {
        val backup = LauncherBackup(
            version = 2,
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
                adaptiveIconShape = "CIRCLE",
                twoDrawersSideBySide = true,
                autoCategorizeNewApps = false
            ),
            widgetDrawers = listOf(
                WidgetDrawer(
                    id = "wd_fin",
                    title = "Finance Widgets",
                    orderIndex = 0
                )
            ),
            widgets = listOf(
                WidgetItem(
                    id = "w_stock",
                    appWidgetId = 42,
                    packageName = "com.stocks",
                    providerClassName = "StockWidget",
                    drawerId = "wd_fin"
                )
            )
        )

        val jsonString = Json.encodeToString(LauncherBackup.serializer(), backup)
        val result = repository.restoreBackupJson(jsonString)

        assertTrue(result.isSuccess)
        coVerify { appGroupDao.replaceAllGroups(match { it.size == 1 && it[0].id == "g_social" }, match { it.size == 1 && it[0].packageName == "com.chat" }) }
        coVerify { dockDao.replaceDock(match { it.size == 1 && it[0].packageName == "com.browser" }) }
        coVerify { mutedAppDao.clearAllRules() }
        coVerify { mutedAppDao.insertRules(match { it.size == 1 && it[0].packageName == "com.promo" }) }
        coVerify { widgetDao.clearAll() }
        coVerify { widgetDao.clearAllDrawers() }
        coVerify { widgetDao.insertDrawer(match { it.id == "wd_fin" && it.title == "Finance Widgets" }) }
        coVerify { widgetDao.insertWidget(match { it.id == "w_stock" && it.packageName == "com.stocks" && it.drawerId == "wd_fin" }) }
        coVerify { preferencesDataSource.setThemeMode("AMOLED") }
        coVerify { preferencesDataSource.setDynamicColor(false) }
        coVerify { preferencesDataSource.setDockBackground("SOLID") }
        coVerify { preferencesDataSource.setAdaptiveIconShape("CIRCLE") }
        coVerify { preferencesDataSource.setTwoDrawersSideBySide(true) }
        coVerify { preferencesDataSource.setAutoCategorizeNewApps(false) }
    }

    @Test
    fun restoreBackupJson_returnsFailureOnMalformedJson() = runTest(testDispatcher) {
        val result = repository.restoreBackupJson("invalid json content")
        assertTrue(result.isFailure)
    }
}
