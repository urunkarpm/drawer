package com.urunkarpm.drawer.feature.home

import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.WeatherRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.action.NeedleActionDispatcher
import com.urunkarpm.drawer.core.needle.action.StandardTools
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelActionTest {
    private val testDispatcher = StandardTestDispatcher()

    private val appRepository: AppRepository = mockk(relaxed = true)
    private val dockRepository: DockRepository = mockk(relaxed = true)
    private val weatherRepository: WeatherRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val widgetRepository: com.urunkarpm.drawer.core.data.repository.WidgetRepository = mockk(relaxed = true)
    private val needleEngine: NeedleEngine = mockk(relaxed = true)
    private val needleActionDispatcher: NeedleActionDispatcher = NeedleActionDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesDataSource.is24Hour } returns MutableStateFlow(true)
        every { preferencesDataSource.showWeather } returns MutableStateFlow(true)
        every { preferencesDataSource.weatherUnit } returns MutableStateFlow("CELSIUS")
        every { preferencesDataSource.hideStatusBar } returns MutableStateFlow(true)
        every { preferencesDataSource.adaptiveIconShape } returns MutableStateFlow("SYSTEM")
        every { preferencesDataSource.twoDrawersSideBySide } returns MutableStateFlow(false)
        every { preferencesDataSource.wallpaperBlur } returns MutableStateFlow(false)
        every { preferencesDataSource.wallpaperBlurRadius } returns MutableStateFlow(25f)
        every { preferencesDataSource.drawerThemeMode } returns MutableStateFlow("SYSTEM")
        every { preferencesDataSource.autoOpenKeyboardInDrawer } returns MutableStateFlow(false)
        every { preferencesDataSource.showDuoStatusWidget } returns MutableStateFlow(true)
        every { preferencesDataSource.enableCameraMirror } returns MutableStateFlow(true)
        every { preferencesDataSource.categoryAlignment } returns MutableStateFlow("TOP")
        every { preferencesDataSource.enableWidgetsPage } returns MutableStateFlow(false)
        every { preferencesDataSource.headerFontFamily } returns MutableStateFlow("SYSTEM")
        every { preferencesDataSource.appLabelScale } returns MutableStateFlow("NORMAL")
        every { preferencesDataSource.openDrawerFromDockOnly } returns MutableStateFlow(false)
        every { preferencesDataSource.duoStatusPosition } returns MutableStateFlow("TEMP")
        every { preferencesDataSource.hasCompletedWalkthrough } returns MutableStateFlow(true)
        every { preferencesDataSource.hideDrawerIcons } returns MutableStateFlow(false)
        every { preferencesDataSource.stealthTheme } returns MutableStateFlow(false)
        every { preferencesDataSource.stealthContrastLevel } returns MutableStateFlow("LOW")
        every { preferencesDataSource.stealthLimitBrightness } returns MutableStateFlow(false)
        every { preferencesDataSource.dynamicWallpaperEnabled } returns MutableStateFlow(false)
        every { preferencesDataSource.wallpaperGradientSpeedSeconds } returns MutableStateFlow(3)
        every { preferencesDataSource.wallpaperGradientBlurPercent } returns MutableStateFlow(40)
        every { preferencesDataSource.wallpaperGradientPalette } returns MutableStateFlow("FULL_SPECTRUM")
        every { preferencesDataSource.wallpaperGradientAngle } returns MutableStateFlow("DIAGONAL")
        every { preferencesDataSource.dockMesmerizingGlow } returns MutableStateFlow(true)
        every { preferencesDataSource.enableOnDeviceAi } returns MutableStateFlow(true)
        every { preferencesDataSource.drawerTitleAnimation } returns MutableStateFlow("ACCENT_POP")
        every { preferencesDataSource.surfaceThemeStyle } returns MutableStateFlow("LIQUID_GLASS")
        every { preferencesDataSource.surfaceOpacity } returns MutableStateFlow(0.65f)
        every { preferencesDataSource.surfaceBlurRadius } returns MutableStateFlow(25f)
        every { preferencesDataSource.surfaceStrokeOpacity } returns MutableStateFlow(0.40f)
        every { preferencesDataSource.surfaceCornerRadius } returns MutableStateFlow(24f)
        every { preferencesDataSource.appLaunchCounts } returns MutableStateFlow(emptyMap())
        every { preferencesDataSource.appLastLaunchedTimes } returns MutableStateFlow(emptyMap())
        every { widgetRepository.getAllWidgets() } returns MutableStateFlow(emptyList())
        every { widgetRepository.getAllDrawers() } returns MutableStateFlow(emptyList())
        every { needleEngine.modelStatus } returns MutableStateFlow(ModelStatus.Ready("/path", 10f))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testQueryPredictsAction() = runTest {
        coEvery { needleEngine.predictToolCall("timer 5m", any()) } returns listOf(
            ToolCallResult("set_timer", buildJsonObject { put("seconds", 300) })
        )

        val action = needleActionDispatcher.dispatch(
            ToolCallResult("set_timer", buildJsonObject { put("seconds", 300) })
        )
        assertNotNull(action)
        assertEquals("Set timer for 5 min", action?.title)
    }

    @Test
    fun testHomeViewModelSearchQueryResolvesAction() = runTest {
        coEvery { needleEngine.predictToolCall("timer 5m", StandardTools.allTools) } returns listOf(
            ToolCallResult("set_timer", buildJsonObject { put("seconds", 300) })
        )

        val viewModel = HomeViewModel(
            appRepository,
            dockRepository,
            weatherRepository,
            preferencesDataSource,
            widgetRepository,
            needleEngine,
            needleActionDispatcher
        )

        assertNull(viewModel.suggestedAction.value)

        viewModel.onSearchQueryChanged("timer 5m")
        advanceTimeBy(200)
        advanceUntilIdle()

        val action = viewModel.suggestedAction.value
        assertNotNull(action)
        assertEquals("Set timer for 5 min", action?.title)

        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()
        assertNull(viewModel.suggestedAction.value)
    }
}
