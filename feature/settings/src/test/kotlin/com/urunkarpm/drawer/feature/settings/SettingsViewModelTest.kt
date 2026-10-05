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

    private val themeModeFlow = MutableStateFlow("SYSTEM")
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

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { preferencesDataSource.themeMode } returns themeModeFlow
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
        every { preferencesDataSource.multiGroupApps } returns multiGroupAppsFlow
        every { preferencesDataSource.notificationsPrivacyMode } returns notificationsPrivacyModeFlow
        every { preferencesDataSource.activeIconPack } returns activeIconPackFlow
        every { preferencesDataSource.adaptiveIconShape } returns adaptiveIconShapeFlow
        every { preferencesDataSource.twoDrawersSideBySide } returns twoDrawersSideBySideFlow
        every { preferencesDataSource.lockLayout } returns lockLayoutFlow

        viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
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
            assertEquals("BLUR", state.dockBackground)
            assertEquals(56f, state.dockIconSize)
            assertEquals("SYSTEM", state.adaptiveIconShape)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setThemeMode_callsPreferencesDataSource() = runTest(testDispatcher) {
        viewModel.setThemeMode("AMOLED")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesDataSource.setThemeMode("AMOLED") }
    }

    @Test
    fun setDockOptions_callsPreferencesDataSource() = runTest(testDispatcher) {
        viewModel.setDockBackground("SOLID")
        viewModel.setDockIconSize(64f)
        viewModel.setDockShowLabels(true)
        viewModel.setDockCornerRadius(16f)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setDockBackground("SOLID") }
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
}
