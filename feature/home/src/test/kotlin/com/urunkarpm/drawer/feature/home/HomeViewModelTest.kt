package com.urunkarpm.drawer.feature.home

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.UpdateRepository
import com.urunkarpm.drawer.core.data.repository.WeatherRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppUpdateInfo
import com.urunkarpm.drawer.core.model.WeatherInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appRepository: AppRepository = mockk(relaxed = true)
    private val dockRepository: DockRepository = mockk(relaxed = true)
    private val weatherRepository: WeatherRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val widgetRepository: com.urunkarpm.drawer.core.data.repository.WidgetRepository = mockk(relaxed = true)
    private val updateRepository: UpdateRepository = mockk(relaxed = true)

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val weatherFlow = MutableStateFlow<WeatherInfo?>(null)
    private val widgetsFlow = MutableStateFlow<List<com.urunkarpm.drawer.core.model.WidgetItem>>(emptyList())
    private val drawersFlow = MutableStateFlow<List<com.urunkarpm.drawer.core.model.WidgetDrawer>>(emptyList())
    private val is24HourFlow = MutableStateFlow(true)
    private val showWeatherFlow = MutableStateFlow(true)
    private val weatherUnitFlow = MutableStateFlow("CELSIUS")
    private val hideStatusBarFlow = MutableStateFlow(true)
    private val adaptiveIconShapeFlow = MutableStateFlow("CIRCLE")
    private val twoDrawersSideBySideFlow = MutableStateFlow(false)
    private val wallpaperBlurFlow = MutableStateFlow(false)
    private val wallpaperBlurRadiusFlow = MutableStateFlow(25f)
    private val drawerThemeModeFlow = MutableStateFlow("SYSTEM")
    private val drawerLiquidGlassFlow = MutableStateFlow(false)
    private val accentColorFlow = MutableStateFlow("DYNAMIC")
    private val autoOpenKeyboardInDrawerFlow = MutableStateFlow(false)
    private val showDuoStatusWidgetFlow = MutableStateFlow(true)
    private val enableCameraMirrorFlow = MutableStateFlow(true)
    private val categoryAlignmentFlow = MutableStateFlow("TOP")
    private val enableWidgetsPageFlow = MutableStateFlow(false)
    private val surfaceThemeStyleFlow = MutableStateFlow("LIQUID_GLASS")
    private val surfaceOpacityFlow = MutableStateFlow(0.65f)
    private val surfaceBlurRadiusFlow = MutableStateFlow(25f)
    private val surfaceStrokeOpacityFlow = MutableStateFlow(0.40f)
    private val surfaceCornerRadiusFlow = MutableStateFlow(24f)
    private val cinematicPaletteFlow = MutableStateFlow("DEFAULT")
    private val headerFontFamilyFlow = MutableStateFlow("SYSTEM")
    private val appLabelScaleFlow = MutableStateFlow("NORMAL")
    private val openDrawerFromDockOnlyFlow = MutableStateFlow(false)
    private val duoStatusPositionFlow = MutableStateFlow("TEMP")
    private val hasCompletedWalkthroughFlow = MutableStateFlow(true)
    private val hideDrawerIconsFlow = MutableStateFlow(false)
    private val stealthThemeFlow = MutableStateFlow(false)
    private val stealthContrastLevelFlow = MutableStateFlow("LOW")
    private val stealthLimitBrightnessFlow = MutableStateFlow(false)
    private val dynamicWallpaperEnabledFlow = MutableStateFlow(false)
    private val wallpaperGradientSpeedSecondsFlow = MutableStateFlow(3)
    private val wallpaperGradientBlurPercentFlow = MutableStateFlow(40)
    private val wallpaperGradientPaletteFlow = MutableStateFlow("FULL_SPECTRUM")
    private val wallpaperGradientAngleFlow = MutableStateFlow("DIAGONAL")
    private val dockMesmerizingGlowFlow = MutableStateFlow(true)
    private val drawerTitleAnimationFlow = MutableStateFlow("ACCENT_POP")
    private val blurAppListOnOpenFlow = MutableStateFlow(false)
    private val appDrawerColumnsFlow = MutableStateFlow(4)
    private val dockIconSizeFlow = MutableStateFlow(56f)
    private val dockExpandedMaxAppsFlow = MutableStateFlow(8)
    private val lockLayoutFlow = MutableStateFlow(false)
    private val appLaunchCountsFlow = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val appLastLaunchedTimesFlow = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val appUsageProfilesFlow = MutableStateFlow<Map<String, com.urunkarpm.drawer.core.model.AppUsageProfile>>(emptyMap())
    private val suggestionBlacklistFlow = MutableStateFlow<Set<String>>(emptySet())
    private val showDrawerSuggestionsFlow = MutableStateFlow(true)
    private val mindfulAppsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val autoCheckUpdatesFlow = MutableStateFlow(true)
    private val lastUpdateCheckTimeFlow = MutableStateFlow(0L)
    private val dismissedUpdateVersionFlow = MutableStateFlow("")

    private val sampleApps = listOf(
        AppInfo(
            packageName = "com.google.android.calculator",
            activityName = "com.google.android.calculator.Calculator",
            label = "Calculator"
        ),
        AppInfo(
            packageName = "com.android.chrome",
            activityName = "com.google.android.apps.chrome.Main",
            label = "Chrome"
        ),
        AppInfo(
            packageName = "com.google.android.youtube",
            activityName = "com.google.android.youtube.HomeActivity",
            label = "YouTube"
        )
    )

    private val sampleWeather = WeatherInfo(
        temperatureCelsius = 22.0,
        weatherCode = 1,
        conditionDescription = "Mainly clear",
        cityName = "London",
        lastUpdatedMillis = 1000L
    )

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appRepository.installedApps } returns appsFlow
        every { weatherRepository.weatherInfo } returns weatherFlow
        every { preferencesDataSource.is24Hour } returns is24HourFlow
        every { preferencesDataSource.showWeather } returns showWeatherFlow
        every { preferencesDataSource.weatherUnit } returns weatherUnitFlow
        every { preferencesDataSource.hideStatusBar } returns hideStatusBarFlow
        every { preferencesDataSource.adaptiveIconShape } returns adaptiveIconShapeFlow
        every { preferencesDataSource.twoDrawersSideBySide } returns twoDrawersSideBySideFlow
        every { preferencesDataSource.wallpaperBlur } returns wallpaperBlurFlow
        every { preferencesDataSource.wallpaperBlurRadius } returns wallpaperBlurRadiusFlow
        every { preferencesDataSource.drawerThemeMode } returns drawerThemeModeFlow
        every { preferencesDataSource.drawerLiquidGlass } returns drawerLiquidGlassFlow
        every { preferencesDataSource.accentColor } returns accentColorFlow
        every { preferencesDataSource.autoOpenKeyboardInDrawer } returns autoOpenKeyboardInDrawerFlow
        every { preferencesDataSource.showDuoStatusWidget } returns showDuoStatusWidgetFlow
        every { preferencesDataSource.enableCameraMirror } returns enableCameraMirrorFlow
        every { preferencesDataSource.categoryAlignment } returns categoryAlignmentFlow
        every { preferencesDataSource.enableWidgetsPage } returns enableWidgetsPageFlow
        every { preferencesDataSource.headerFontFamily } returns headerFontFamilyFlow
        every { preferencesDataSource.appLabelScale } returns appLabelScaleFlow
        every { preferencesDataSource.openDrawerFromDockOnly } returns openDrawerFromDockOnlyFlow
        every { preferencesDataSource.duoStatusPosition } returns duoStatusPositionFlow
        every { preferencesDataSource.hasCompletedWalkthrough } returns hasCompletedWalkthroughFlow
        every { preferencesDataSource.hideDrawerIcons } returns hideDrawerIconsFlow
        every { preferencesDataSource.stealthTheme } returns stealthThemeFlow
        every { preferencesDataSource.stealthContrastLevel } returns stealthContrastLevelFlow
        every { preferencesDataSource.stealthLimitBrightness } returns stealthLimitBrightnessFlow
        every { preferencesDataSource.dynamicWallpaperEnabled } returns dynamicWallpaperEnabledFlow
        every { preferencesDataSource.wallpaperGradientSpeedSeconds } returns wallpaperGradientSpeedSecondsFlow
        every { preferencesDataSource.wallpaperGradientBlurPercent } returns wallpaperGradientBlurPercentFlow
        every { preferencesDataSource.wallpaperGradientPalette } returns wallpaperGradientPaletteFlow
        every { preferencesDataSource.wallpaperGradientAngle } returns wallpaperGradientAngleFlow
        every { preferencesDataSource.dockMesmerizingGlow } returns dockMesmerizingGlowFlow
        every { preferencesDataSource.drawerTitleAnimation } returns drawerTitleAnimationFlow
        every { preferencesDataSource.blurAppListOnOpen } returns blurAppListOnOpenFlow
        every { preferencesDataSource.appDrawerColumns } returns appDrawerColumnsFlow
        every { preferencesDataSource.dockIconSize } returns dockIconSizeFlow
        every { preferencesDataSource.dockExpandedMaxApps } returns dockExpandedMaxAppsFlow
        every { preferencesDataSource.lockLayout } returns lockLayoutFlow
        every { preferencesDataSource.surfaceThemeStyle } returns surfaceThemeStyleFlow
        every { preferencesDataSource.surfaceOpacity } returns surfaceOpacityFlow
        every { preferencesDataSource.surfaceBlurRadius } returns surfaceBlurRadiusFlow
        every { preferencesDataSource.surfaceStrokeOpacity } returns surfaceStrokeOpacityFlow
        every { preferencesDataSource.surfaceCornerRadius } returns surfaceCornerRadiusFlow
        every { preferencesDataSource.cinematicPalette } returns cinematicPaletteFlow
        every { preferencesDataSource.appLaunchCounts } returns appLaunchCountsFlow
        every { preferencesDataSource.appLastLaunchedTimes } returns appLastLaunchedTimesFlow
        every { preferencesDataSource.appUsageProfiles } returns appUsageProfilesFlow
        every { preferencesDataSource.suggestionBlacklist } returns suggestionBlacklistFlow
        every { preferencesDataSource.showDrawerSuggestions } returns showDrawerSuggestionsFlow
        every { preferencesDataSource.mindfulApps } returns mindfulAppsFlow
        every { preferencesDataSource.autoCheckUpdates } returns autoCheckUpdatesFlow
        every { preferencesDataSource.lastUpdateCheckTime } returns lastUpdateCheckTimeFlow
        every { preferencesDataSource.dismissedUpdateVersion } returns dismissedUpdateVersionFlow
        every { widgetRepository.getAllWidgets() } returns widgetsFlow
        every { widgetRepository.getAllDrawers() } returns drawersFlow

        appsFlow.value = sampleApps
        weatherFlow.value = sampleWeather

        viewModel = HomeViewModel(
            appRepository = appRepository,
            dockRepository = dockRepository,
            weatherRepository = weatherRepository,
            preferencesDataSource = preferencesDataSource,
            widgetRepository = widgetRepository,
            updateRepository = updateRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoadsInstalledAppsAndWeather() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val loadedState = expectMostRecentItem()
            assertEquals(3, loadedState.installedApps.size)
            assertEquals(3, loadedState.filteredApps.size)
            assertEquals("", loadedState.searchQuery)
            assertFalse(loadedState.isAllAppsOpen)
            assertNull(loadedState.selectedAppForMenu)
            assertEquals(sampleWeather, loadedState.weatherInfo)
            assertTrue(loadedState.is24Hour)
            assertTrue(loadedState.showWeather)
            assertEquals("CELSIUS", loadedState.weatherUnit)
            assertTrue(loadedState.hideStatusBar)
            assertEquals("LIQUID_GLASS", loadedState.surfaceThemeStyle)
            assertEquals(0.65f, loadedState.surfaceOpacity)
            assertEquals(25f, loadedState.surfaceBlurRadius)
            assertEquals(0.40f, loadedState.surfaceStrokeOpacity)
            assertEquals(24f, loadedState.surfaceCornerRadius)
            assertEquals("SYSTEM", loadedState.headerFontFamily)
            assertEquals("NORMAL", loadedState.appLabelScale)
            assertFalse(loadedState.openDrawerFromDockOnly)
            assertEquals("TEMP", loadedState.duoStatusPosition)
        }
    }

    @Test
    fun uiState_updatesTypographyPreferences() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            headerFontFamilyFlow.value = "TEXAS_TROUPE"
            appLabelScaleFlow.value = "LARGE"
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("TEXAS_TROUPE", state.headerFontFamily)
            assertEquals("LARGE", state.appLabelScale)
        }
    }

    @Test
    fun uiState_updatesDockOnlyAndDuoStatusPosition() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            openDrawerFromDockOnlyFlow.value = true
            duoStatusPositionFlow.value = "TIME"
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.openDrawerFromDockOnly)
            assertEquals("TIME", state.duoStatusPosition)
        }
    }

    @Test
    fun getAqiSeverityColor_mapsAllCategoriesAccurately() {
        // Dark wallpaper scale
        assertEquals(androidx.compose.ui.graphics.Color(0xFF4ADE80), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(25, false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFBBF24), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(75, false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFB923C), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(125, false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF87171), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(175, false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFA78BFA), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(242, false))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFB7185), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(350, false))

        // Light wallpaper scale
        assertEquals(androidx.compose.ui.graphics.Color(0xFF15803D), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(25, true))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFB45309), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(75, true))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFC2410C), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(125, true))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFB91C1C), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(175, true))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF7E22CE), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(242, true))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF881337), com.urunkarpm.drawer.feature.home.component.getAqiSeverityColor(350, true))
    }

    @Test
    fun refreshWeather_delegatesToRepository() = runTest {
        viewModel.refreshWeather()
        testScheduler.advanceUntilIdle()

        coVerify { weatherRepository.refreshWeather() }
    }

    @Test
    fun onSearchQueryChanged_filtersAppsCorrectly() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            viewModel.onSearchQueryChanged("calc")
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("calc", state.searchQuery)
            assertEquals(1, state.filteredApps.size)
            assertEquals("Calculator", state.filteredApps.first().label)
        }
    }

    @Test
    fun openAndCloseAllApps_updatesState() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            viewModel.openAllApps()
            testScheduler.advanceUntilIdle()
            var state = expectMostRecentItem()
            assertTrue(state.isAllAppsOpen)

            viewModel.onSearchQueryChanged("chr")
            viewModel.closeAllApps()
            testScheduler.advanceUntilIdle()
            state = expectMostRecentItem()
            assertFalse(state.isAllAppsOpen)
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun testOpenAllApps_freezesPredictedSuggestions() = runTest {
        // When drawer opens, suggestedApps snapshot is taken
        viewModel.openAllApps()
        assertTrue(viewModel.isAllAppsOpen.value)
        val initialSuggestions = viewModel.suggestedApps.value
        assertEquals(3, initialSuggestions.size)

        // Hiding an app from suggestions immediately filters snapshot and updates preferences
        viewModel.hideFromSuggestions("com.android.chrome")
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.suggestedApps.value.any { it.packageName == "com.android.chrome" })
        coVerify { preferencesDataSource.hideAppFromSuggestions("com.android.chrome") }
    }

    @Test
    fun onAppClicked_launchesAppAndClosesDrawerIfOpen() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val app = sampleApps.first()
            every { appRepository.launchApp(app) } returns true

            viewModel.openAllApps()
            testScheduler.advanceUntilIdle()
            assertTrue(expectMostRecentItem().isAllAppsOpen)

            val launched = viewModel.onAppClicked(app)
            testScheduler.advanceUntilIdle()
            assertTrue(launched)
            verify { appRepository.launchApp(app) }

            assertFalse(expectMostRecentItem().isAllAppsOpen)
        }
    }

    @Test
    fun appMenuActions_delegateToRepository() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val app = sampleApps.first()

            viewModel.onAppLongClicked(app)
            testScheduler.advanceUntilIdle()
            assertEquals(app, expectMostRecentItem().selectedAppForMenu)

            viewModel.openAppDetails(app)
            testScheduler.advanceUntilIdle()
            verify { appRepository.openAppDetails(app.packageName) }
            assertNull(expectMostRecentItem().selectedAppForMenu)

            viewModel.onAppLongClicked(app)
            testScheduler.advanceUntilIdle()
            assertEquals(app, expectMostRecentItem().selectedAppForMenu)

            viewModel.uninstallApp(app)
            testScheduler.advanceUntilIdle()
            verify { appRepository.uninstallApp(app.packageName) }
            assertNull(expectMostRecentItem().selectedAppForMenu)
        }
    }

    @Test
    fun pinToDock_delegatesToDockRepositoryAndSetsUserMessage() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val app = sampleApps.first()
            coEvery { dockRepository.addToDock(app, any()) } returns true

            viewModel.pinToDock(app)
            testScheduler.advanceUntilIdle()

            coVerify { dockRepository.addToDock(app, 5) }
            val state = expectMostRecentItem()
            assertEquals("Calculator pinned to dock", state.userMessage)
        }
    }

    @Test
    fun pinToDock_whenSmallerIconSize_allowsMoreAppsToDock() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            dockIconSizeFlow.value = 40f
            val app = sampleApps.first()
            coEvery { dockRepository.addToDock(app, any()) } returns true

            viewModel.pinToDock(app)
            testScheduler.advanceUntilIdle()

            coVerify { dockRepository.addToDock(app, 8) }
            val state = expectMostRecentItem()
            assertEquals("Calculator pinned to dock", state.userMessage)
        }
    }

    @Test
    fun pinToDock_whenFullOrDuplicate_setsFailureUserMessage() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val app = sampleApps.first()
            coEvery { dockRepository.addToDock(app, any()) } returns false

            viewModel.pinToDock(app)
            testScheduler.advanceUntilIdle()

            coVerify { dockRepository.addToDock(app, 5) }
            val state = expectMostRecentItem()
            assertEquals("Cannot pin to dock: full (max 5) or already in dock", state.userMessage)
        }
    }

    @Test
    fun addAndRemoveAppFromDock_delegatesToDockRepository() = runTest {
        val app = sampleApps.first()
        coEvery { dockRepository.addToDock(app) } returns true
        val added = viewModel.addAppToDock(app)
        assertTrue(added)
        coVerify { dockRepository.addToDock(app) }

        val dockItem = com.urunkarpm.drawer.core.model.DockItem(
            position = 0,
            packageName = app.packageName,
            activityName = app.activityName
        )
        every { dockRepository.dockItems } returns MutableStateFlow(listOf(dockItem))
        coEvery { dockRepository.removeFromDock(0) } returns Unit

        val removed = viewModel.removeAppFromDock(app)
        assertTrue(removed)
        coVerify { dockRepository.removeFromDock(0) }
    }

    @Test
    fun createWidgetDrawer_delegatesToRepository() = runTest {
        coEvery { widgetRepository.createDrawer("Finance", 4, 3) } returns "drawer-123"

        viewModel.createWidgetDrawer("Finance", 4, 3)
        testScheduler.advanceUntilIdle()

        coVerify(timeout = 2000) { widgetRepository.createDrawer("Finance", 4, 3) }
    }

    @Test
    fun deleteWidgetDrawer_delegatesToRepository() = runTest {
        viewModel.deleteWidgetDrawer("drawer-123", deleteChildren = true)
        testScheduler.advanceUntilIdle()

        coVerify(timeout = 2000) { widgetRepository.deleteDrawer("drawer-123", true) }
    }

    @Test
    fun moveWidgetToDrawer_delegatesToRepository() = runTest {
        viewModel.moveWidgetToDrawer("widget-1", "drawer-123")
        testScheduler.advanceUntilIdle()

        coVerify(timeout = 2000) { widgetRepository.assignWidgetToDrawer("widget-1", "drawer-123") }
    }

    @Test
    fun updateWidgetSpan_updatesWidgetWithSpanAndHeight() = runTest {
        val widget = com.urunkarpm.drawer.core.model.WidgetItem(
            id = "w-1",
            appWidgetId = 10,
            packageName = "pkg",
            providerClassName = "cls",
            spanX = 4,
            spanY = 2,
            heightDp = 140
        )

        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            widgetsFlow.value = listOf(widget)
            testScheduler.advanceUntilIdle()

            viewModel.updateWidgetSpan("w-1", spanX = 2, spanY = 3, heightDp = 210)
            testScheduler.advanceUntilIdle()

            val slot = io.mockk.slot<com.urunkarpm.drawer.core.model.WidgetItem>()
            coVerify { widgetRepository.updateWidget(capture(slot)) }
            assertEquals(2, slot.captured.spanX)
            assertEquals(3, slot.captured.spanY)
            assertEquals(210, slot.captured.heightDp)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun completeWalkthrough_updatesPreferences() = runTest {
        viewModel.completeWalkthrough()
        testScheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.setHasCompletedWalkthrough(true) }
    }

    @Test
    fun addWidget_standalone_insertsWidgetAndEnablesFeedIfDisabled() = runTest {
        enableWidgetsPageFlow.value = false
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            viewModel.addWidget(
                appWidgetId = 42,
                packageName = "com.example.widget",
                providerClassName = "com.example.widget.MyWidgetProvider",
                drawerId = null,
                spanX = 4,
                spanY = 2,
                heightDp = 180
            )
            testScheduler.advanceUntilIdle()

            coVerify { preferencesDataSource.setEnableWidgetsPage(true) }
            val slot = io.mockk.slot<com.urunkarpm.drawer.core.model.WidgetItem>()
            coVerify { widgetRepository.addWidget(capture(slot)) }
            assertEquals(42, slot.captured.appWidgetId)
            assertEquals("com.example.widget", slot.captured.packageName)
            assertNull(slot.captured.drawerId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun uiState_reflectsHasCompletedWalkthrough() = runTest {
        hasCompletedWalkthroughFlow.value = false
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            assertFalse(expectMostRecentItem().hasCompletedWalkthrough)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setDynamicWallpaperEnabled_updatesPreferences() = runTest {
        viewModel.setDynamicWallpaperEnabled(true)
        testScheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.setDynamicWallpaperEnabled(true) }
    }

    @Test
    fun walkthroughPreferenceSetters_updatePreferences() = runTest {
        viewModel.setTwoDrawersSideBySide(true)
        viewModel.setIs24Hour(false)
        viewModel.setShowWeather(false)
        viewModel.setAdaptiveIconShape("CIRCLE")
        viewModel.setDrawerThemeMode("DARK")
        viewModel.setDrawerLiquidGlass(true)
        viewModel.setSurfaceThemeStyle("SOLID")
        viewModel.setDockMesmerizingGlow(false)
        viewModel.setHeaderFontFamily("MONOSPACE")
        viewModel.setStealthTheme(true)
        testScheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setTwoDrawersSideBySide(true) }
        coVerify { preferencesDataSource.setIs24Hour(false) }
        coVerify { preferencesDataSource.setShowWeather(false) }
        coVerify { preferencesDataSource.setAdaptiveIconShape("CIRCLE") }
        coVerify { preferencesDataSource.setDrawerThemeMode("DARK") }
        coVerify { preferencesDataSource.setDrawerLiquidGlass(true) }
        coVerify { preferencesDataSource.setSurfaceThemeStyle("SOLID") }
        coVerify { preferencesDataSource.setDockMesmerizingGlow(false) }
        coVerify { preferencesDataSource.setHeaderFontFamily("MONOSPACE") }
        coVerify { preferencesDataSource.setStealthTheme(true) }
    }

    @Test
    fun appLaunchCounts_emitsFromPreferences() = runTest {
        val counts = mapOf("com.example.app/MainActivity#0" to 5)
        viewModel.appLaunchCounts.test {
            assertEquals(emptyMap<String, Int>(), awaitItem())
            appLaunchCountsFlow.value = counts
            assertEquals(counts, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun changeAppSetting_updatesPreferencesCorrectly() = runTest {
        val summary1 = viewModel.changeAppSetting("STEALTH_THEME", "true")
        assertEquals("Stealth theme enabled", summary1)
        coVerify { preferencesDataSource.setStealthTheme(true) }

        val summary2 = viewModel.changeAppSetting("LOCK_LAYOUT", "true")
        assertEquals("Layout locked", summary2)
        coVerify { preferencesDataSource.setLockLayout(true) }

        val summary3 = viewModel.changeAppSetting("THEME_MODE", "DARK")
        assertEquals("Theme set to Dark", summary3)
        coVerify { preferencesDataSource.setThemeMode("DARK") }
    }

    @Test
    fun onAppClicked_nonMindfulApp_launchesImmediately() = runTest {
        val app = sampleApps.first()
        every { appRepository.launchApp(app) } returns true

        val launched = viewModel.onAppClicked(app)
        assertTrue(launched)
        verify { appRepository.launchApp(app) }
        assertNull(viewModel.uiState.value.pendingMindfulApp)
    }

    @Test
    fun onAppClicked_mindfulApp_interceptsAndSetsPendingMindfulApp() = runTest {
        viewModel.uiState.test {
            awaitItem()

            val app = sampleApps.first()
            mindfulAppsFlow.value = setOf(app.packageName)
            testScheduler.advanceUntilIdle()
            awaitItem()

            every { appRepository.launchApp(app) } returns true

            val launched = viewModel.onAppClicked(app)
            assertFalse(launched)
            verify(exactly = 0) { appRepository.launchApp(app) }
            val stateWithPending = awaitItem()
            assertEquals(app, stateWithPending.pendingMindfulApp)

            // Confirming launch executes the pending app
            viewModel.confirmMindfulLaunch()
            verify(exactly = 1) { appRepository.launchApp(app) }
            val stateAfterConfirm = awaitItem()
            assertNull(stateAfterConfirm.pendingMindfulApp)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cancelMindfulLaunch_clearsPendingWithoutLaunching() = runTest {
        viewModel.uiState.test {
            awaitItem()

            val app = sampleApps.first()
            mindfulAppsFlow.value = setOf(app.packageName)
            testScheduler.advanceUntilIdle()
            awaitItem()

            viewModel.onAppClicked(app)
            val stateWithPending = awaitItem()
            assertEquals(app, stateWithPending.pendingMindfulApp)

            viewModel.cancelMindfulLaunch()
            val stateAfterCancel = awaitItem()
            assertNull(stateAfterCancel.pendingMindfulApp)
            verify(exactly = 0) { appRepository.launchApp(app) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleMindfulApp_delegatesToPreferencesDataSource() = runTest {
        viewModel.toggleMindfulApp("com.twitter.android")
        testScheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.toggleMindfulApp("com.twitter.android") }
    }

    @Test
    fun setAccentColor_delegatesToPreferencesDataSource() = runTest {
        viewModel.setAccentColor("#10B981")
        testScheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.setAccentColor("#10B981") }
    }

    @Test
    fun checkForUpdateOnStartup_whenUpdateAvailable_emitsAvailableUpdate() = runTest {
        val updateInfo = AppUpdateInfo(
            latestVersionTag = "v2.9.0",
            releaseName = "v2.9.0",
            releaseNotes = "New features",
            apkDownloadUrl = "https://example.com/drawer.apk",
            htmlUrl = "https://example.com",
            isUpdateAvailable = true
        )
        coEvery { updateRepository.checkForUpdates("2.8.0") } returns Result.success(updateInfo)

        viewModel.checkForUpdateOnStartup("2.8.0")
        testScheduler.advanceUntilIdle()

        assertEquals(updateInfo, viewModel.availableUpdate.value)
        coVerify { preferencesDataSource.setLastUpdateCheckTime(any()) }
    }

    @Test
    fun checkForUpdateOnStartup_whenAlreadyDismissed_doesNotEmit() = runTest {
        dismissedUpdateVersionFlow.value = "v2.9.0"
        val updateInfo = AppUpdateInfo(
            latestVersionTag = "v2.9.0",
            releaseName = "v2.9.0",
            releaseNotes = "New features",
            apkDownloadUrl = "https://example.com/drawer.apk",
            htmlUrl = "https://example.com",
            isUpdateAvailable = true
        )
        coEvery { updateRepository.checkForUpdates("2.8.0") } returns Result.success(updateInfo)

        viewModel.checkForUpdateOnStartup("2.8.0")
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.availableUpdate.value)
    }

    @Test
    fun checkForUpdateOnStartup_whenAutoCheckDisabled_doesNotQueryRepo() = runTest {
        autoCheckUpdatesFlow.value = false

        viewModel.checkForUpdateOnStartup("2.8.0")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { updateRepository.checkForUpdates(any()) }
        assertNull(viewModel.availableUpdate.value)
    }

    @Test
    fun checkForUpdateOnStartup_whenCheckedRecently_skipsQuery() = runTest {
        lastUpdateCheckTimeFlow.value = System.currentTimeMillis() - 10_000L // 10 seconds ago

        viewModel.checkForUpdateOnStartup("2.8.0")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { updateRepository.checkForUpdates(any()) }
        assertNull(viewModel.availableUpdate.value)
    }

    @Test
    fun dismissUpdate_clearsStateAndPersistsDismissedVersion() = runTest {
        val updateInfo = AppUpdateInfo(
            latestVersionTag = "v2.9.0",
            releaseName = "v2.9.0",
            releaseNotes = "New features",
            apkDownloadUrl = "https://example.com/drawer.apk",
            htmlUrl = "https://example.com",
            isUpdateAvailable = true
        )
        coEvery { updateRepository.checkForUpdates("2.8.0") } returns Result.success(updateInfo)

        viewModel.checkForUpdateOnStartup("2.8.0")
        testScheduler.advanceUntilIdle()
        assertEquals(updateInfo, viewModel.availableUpdate.value)

        viewModel.dismissUpdate("v2.9.0")
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.availableUpdate.value)
        coVerify { preferencesDataSource.setDismissedUpdateVersion("v2.9.0") }
    }
}

