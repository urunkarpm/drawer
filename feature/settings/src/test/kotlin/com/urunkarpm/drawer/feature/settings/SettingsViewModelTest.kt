package com.urunkarpm.drawer.feature.settings

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.BackupRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val backupRepository: BackupRepository = mockk(relaxed = true)
    private val updateRepository: com.urunkarpm.drawer.core.data.repository.UpdateRepository = mockk(relaxed = true)

    private val themeModeFlow = MutableStateFlow("SYSTEM")
    private val drawerThemeModeFlow = MutableStateFlow("SYSTEM")
    private val dynamicColorFlow = MutableStateFlow(true)
    private val hideStatusBarFlow = MutableStateFlow(true)
    private val showWeatherFlow = MutableStateFlow(true)
    private val weatherUnitFlow = MutableStateFlow("CELSIUS")
    private val is24HourFlow = MutableStateFlow(true)
    private val manualCityNameFlow = MutableStateFlow<String?>(null)
    private val dockBackgroundFlow = MutableStateFlow("BLUR")
    private val dockIconSizeFlow = MutableStateFlow(56f)
    private val dockShowLabelsFlow = MutableStateFlow(false)
    private val dockCornerRadiusFlow = MutableStateFlow(24f)
    private val multiGroupAppsFlow = MutableStateFlow(false)
    private val notificationsPrivacyModeFlow = MutableStateFlow(false)
    private val activeIconPackFlow = MutableStateFlow<String?>(null)
    private val adaptiveIconShapeFlow = MutableStateFlow("SYSTEM")
    private val twoDrawersSideBySideFlow = MutableStateFlow(false)
    private val wallpaperBlurFlow = MutableStateFlow(false)
    private val wallpaperBlurRadiusFlow = MutableStateFlow(25f)
    private val lockLayoutFlow = MutableStateFlow(false)
    private val surfaceThemeStyleFlow = MutableStateFlow("LIQUID_GLASS")
    private val surfaceOpacityFlow = MutableStateFlow(0.65f)
    private val surfaceBlurRadiusFlow = MutableStateFlow(25f)
    private val surfaceStrokeOpacityFlow = MutableStateFlow(0.40f)
    private val surfaceCornerRadiusFlow = MutableStateFlow(24f)
    private val autoArrangeAppsFlow = MutableStateFlow(true)
    private val showDuoStatusWidgetFlow = MutableStateFlow(true)
    private val enableCameraMirrorFlow = MutableStateFlow(true)
    private val autoOpenKeyboardInDrawerFlow = MutableStateFlow(false)
    private val categoryAlignmentFlow = MutableStateFlow("TOP")
    private val enableWidgetsPageFlow = MutableStateFlow(false)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { preferencesDataSource.themeMode } returns themeModeFlow
        every { preferencesDataSource.drawerThemeMode } returns drawerThemeModeFlow
        every { preferencesDataSource.dynamicColor } returns dynamicColorFlow
        every { preferencesDataSource.hideStatusBar } returns hideStatusBarFlow
        every { preferencesDataSource.wallpaperBlur } returns wallpaperBlurFlow
        every { preferencesDataSource.wallpaperBlurRadius } returns wallpaperBlurRadiusFlow
        every { preferencesDataSource.showWeather } returns showWeatherFlow
        every { preferencesDataSource.weatherUnit } returns weatherUnitFlow
        every { preferencesDataSource.is24Hour } returns is24HourFlow
        every { preferencesDataSource.manualCityName } returns manualCityNameFlow
        every { preferencesDataSource.dockBackground } returns dockBackgroundFlow
        every { preferencesDataSource.dockIconSize } returns dockIconSizeFlow
        every { preferencesDataSource.dockShowLabels } returns dockShowLabelsFlow
        every { preferencesDataSource.dockCornerRadius } returns dockCornerRadiusFlow
        every { preferencesDataSource.surfaceThemeStyle } returns surfaceThemeStyleFlow
        every { preferencesDataSource.surfaceOpacity } returns surfaceOpacityFlow
        every { preferencesDataSource.surfaceBlurRadius } returns surfaceBlurRadiusFlow
        every { preferencesDataSource.surfaceStrokeOpacity } returns surfaceStrokeOpacityFlow
        every { preferencesDataSource.surfaceCornerRadius } returns surfaceCornerRadiusFlow
        every { preferencesDataSource.autoArrangeApps } returns autoArrangeAppsFlow
        every { preferencesDataSource.multiGroupApps } returns multiGroupAppsFlow
        every { preferencesDataSource.notificationsPrivacyMode } returns notificationsPrivacyModeFlow
        every { preferencesDataSource.activeIconPack } returns activeIconPackFlow
        every { preferencesDataSource.adaptiveIconShape } returns adaptiveIconShapeFlow
        every { preferencesDataSource.twoDrawersSideBySide } returns twoDrawersSideBySideFlow
        every { preferencesDataSource.lockLayout } returns lockLayoutFlow
        every { preferencesDataSource.autoOpenKeyboardInDrawer } returns autoOpenKeyboardInDrawerFlow
        every { preferencesDataSource.categoryAlignment } returns categoryAlignmentFlow
        every { preferencesDataSource.enableWidgetsPage } returns enableWidgetsPageFlow
        every { preferencesDataSource.showDuoStatusWidget } returns showDuoStatusWidgetFlow
        every { preferencesDataSource.enableCameraMirror } returns enableCameraMirrorFlow

        viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_reflectsInitialPreferences() = runTest(testDispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("SYSTEM", state.themeMode)
            assertTrue(state.dynamicColor)
            assertTrue(state.hideStatusBar)
            assertTrue(state.showWeather)
            assertEquals("CELSIUS", state.weatherUnit)
            assertEquals("LIQUID_GLASS", state.dockBackground)
            assertEquals(56f, state.dockIconSize)
            assertEquals("SYSTEM", state.adaptiveIconShape)
            assertEquals("LIQUID_GLASS", state.surfaceThemeStyle)
            assertEquals(0.65f, state.surfaceOpacity)
            assertEquals(25f, state.surfaceBlurRadius)
            assertEquals(0.40f, state.surfaceStrokeOpacity)
            assertEquals(24f, state.surfaceCornerRadius)
            assertTrue(state.autoArrangeApps)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setAutoArrangeApps_callsPreferencesDataSource() = runTest(testDispatcher) {
        viewModel.setAutoArrangeApps(false)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.setAutoArrangeApps(false) }
    }

    @Test
    fun setThemeMode_callsPreferencesDataSource() = runTest(testDispatcher) {
        viewModel.setThemeMode("AMOLED")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.setThemeMode("AMOLED") }
    }

    @Test
    fun setSurfaceThemeOptions_callsPreferencesDataSource() = runTest(testDispatcher) {
        viewModel.setSurfaceThemeStyle("LIQUID_GLASS")
        viewModel.setSurfaceOpacity(0.80f)
        viewModel.setSurfaceBlurRadius(35f)
        viewModel.setSurfaceStrokeOpacity(0.50f)
        viewModel.setSurfaceCornerRadius(28f)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setSurfaceThemeStyle("LIQUID_GLASS") }
        coVerify { preferencesDataSource.setSurfaceOpacity(0.80f) }
        coVerify { preferencesDataSource.setSurfaceBlurRadius(35f) }
        coVerify { preferencesDataSource.setSurfaceStrokeOpacity(0.50f) }
        coVerify { preferencesDataSource.setSurfaceCornerRadius(28f) }
    }

    @Test
    fun setDockOptions_callsPreferencesDataSource() = runTest(testDispatcher) {
        viewModel.setDockBackground("SOLID")
        viewModel.setDockIconSize(64f)
        viewModel.setDockShowLabels(true)
        viewModel.setDockCornerRadius(16f)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setSurfaceThemeStyle("SOLID") }
        coVerify { preferencesDataSource.setDockIconSize(64f) }
        coVerify { preferencesDataSource.setDockShowLabels(true) }
        coVerify { preferencesDataSource.setDockCornerRadius(16f) }
    }

    @Test
    fun restoreBackup_successful_updatesUserMessage() = runTest(testDispatcher) {
        coEvery { backupRepository.restoreBackupJson(any()) } returns Result.success(Unit)

        viewModel.uiState.test {
            awaitItem() // Initial emission

            viewModel.restoreBackup("{\"version\":1}")
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("Backup restored successfully!", state.userMessage)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupRepository.restoreBackupJson("{\"version\":1}") }
    }

    @Test
    fun restoreBackup_failure_updatesUserMessageWithError() = runTest(testDispatcher) {
        coEvery { backupRepository.restoreBackupJson(any()) } returns Result.failure(IllegalArgumentException("corrupt"))

        viewModel.uiState.test {
            awaitItem() // Initial emission

            viewModel.restoreBackup("bad_json")
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.userMessage?.contains("Failed to restore backup") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setDrawerThemeMode_persistsSetting() = runTest(testDispatcher) {
        viewModel.setDrawerThemeMode("WHITE")
        testScheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setDrawerThemeMode("WHITE") }
    }
}
