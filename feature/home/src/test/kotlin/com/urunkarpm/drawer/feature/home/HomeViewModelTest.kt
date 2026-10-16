package com.urunkarpm.drawer.feature.home

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.WeatherRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
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

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val weatherFlow = MutableStateFlow<WeatherInfo?>(null)
    private val widgetsFlow = MutableStateFlow<List<com.urunkarpm.drawer.core.model.WidgetItem>>(emptyList())
    private val drawersFlow = MutableStateFlow<List<com.urunkarpm.drawer.core.model.WidgetDrawer>>(emptyList())
    private val is24HourFlow = MutableStateFlow(true)
    private val showWeatherFlow = MutableStateFlow(true)
    private val weatherUnitFlow = MutableStateFlow("CELSIUS")
    private val hideStatusBarFlow = MutableStateFlow(true)
    private val adaptiveIconShapeFlow = MutableStateFlow("SYSTEM")
    private val twoDrawersSideBySideFlow = MutableStateFlow(false)
    private val wallpaperBlurFlow = MutableStateFlow(false)
    private val wallpaperBlurRadiusFlow = MutableStateFlow(25f)
    private val drawerThemeModeFlow = MutableStateFlow("SYSTEM")
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
    private val headerFontFamilyFlow = MutableStateFlow("SYSTEM")
    private val appLabelScaleFlow = MutableStateFlow("NORMAL")
    private val openDrawerFromDockOnlyFlow = MutableStateFlow(false)
    private val duoStatusPositionFlow = MutableStateFlow("TEMP")
    private val hasCompletedWalkthroughFlow = MutableStateFlow(true)

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
        every { preferencesDataSource.surfaceThemeStyle } returns surfaceThemeStyleFlow
        every { preferencesDataSource.surfaceOpacity } returns surfaceOpacityFlow
        every { preferencesDataSource.surfaceBlurRadius } returns surfaceBlurRadiusFlow
        every { preferencesDataSource.surfaceStrokeOpacity } returns surfaceStrokeOpacityFlow
        every { preferencesDataSource.surfaceCornerRadius } returns surfaceCornerRadiusFlow
        every { widgetRepository.getAllWidgets() } returns widgetsFlow
        every { widgetRepository.getAllDrawers() } returns drawersFlow

        appsFlow.value = sampleApps
        weatherFlow.value = sampleWeather

        viewModel = HomeViewModel(appRepository, dockRepository, weatherRepository, preferencesDataSource, widgetRepository)
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
            coEvery { dockRepository.addToDock(app) } returns true

            viewModel.pinToDock(app)
            testScheduler.advanceUntilIdle()

            coVerify { dockRepository.addToDock(app) }
            val state = expectMostRecentItem()
            assertEquals("Calculator pinned to dock", state.userMessage)
        }
    }

    @Test
    fun createWidgetDrawer_delegatesToRepository() = runTest {
        coEvery { widgetRepository.createDrawer("Finance", 4, 3) } returns "drawer-123"

        viewModel.createWidgetDrawer("Finance", 4, 3)
        testScheduler.advanceUntilIdle()

        coVerify { widgetRepository.createDrawer("Finance", 4, 3) }
    }

    @Test
    fun deleteWidgetDrawer_delegatesToRepository() = runTest {
        viewModel.deleteWidgetDrawer("drawer-123", deleteChildren = true)
        testScheduler.advanceUntilIdle()

        coVerify { widgetRepository.deleteDrawer("drawer-123", true) }
    }

    @Test
    fun moveWidgetToDrawer_delegatesToRepository() = runTest {
        viewModel.moveWidgetToDrawer("widget-1", "drawer-123")
        testScheduler.advanceUntilIdle()

        coVerify { widgetRepository.assignWidgetToDrawer("widget-1", "drawer-123") }
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
    fun uiState_reflectsHasCompletedWalkthrough() = runTest {
        hasCompletedWalkthroughFlow.value = false
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()
            assertFalse(expectMostRecentItem().hasCompletedWalkthrough)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

