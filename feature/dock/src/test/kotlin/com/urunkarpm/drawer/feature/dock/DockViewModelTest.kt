package com.urunkarpm.drawer.feature.dock

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dockRepository: DockRepository = mockk(relaxed = true)
    private val appRepository: AppRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val dockItemsFlow = MutableStateFlow<List<DockItem>>(emptyList())
    private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val bgStyleFlow = MutableStateFlow("BLUR")
    private val iconSizeFlow = MutableStateFlow(56f)
    private val showLabelsFlow = MutableStateFlow(false)
    private val cornerRadiusFlow = MutableStateFlow(24f)

    private lateinit var viewModel: DockViewModel

    private val sampleApp = AppInfo(
        packageName = "com.test.browser",
        activityName = "com.test.browser.MainActivity",
        label = "Browser"
    )

    private val sampleDockItem = DockItem(
        position = 0,
        packageName = "com.test.browser",
        activityName = "com.test.browser.MainActivity"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { dockRepository.dockItems } returns dockItemsFlow
        every { appRepository.installedApps } returns installedAppsFlow
        every { preferencesDataSource.dockBackground } returns bgStyleFlow
        every { preferencesDataSource.dockIconSize } returns iconSizeFlow
        every { preferencesDataSource.dockShowLabels } returns showLabelsFlow
        every { preferencesDataSource.dockCornerRadius } returns cornerRadiusFlow

        dockItemsFlow.value = listOf(sampleDockItem)
        installedAppsFlow.value = listOf(sampleApp)

        viewModel = DockViewModel(dockRepository, appRepository, preferencesDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_resolvesDockItemsWithMatchingAppsAndPreferences() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.resolvedApps.size)
            assertEquals("Browser", state.resolvedApps.first().second?.label)
            assertEquals("BLUR", state.backgroundStyle)
            assertEquals(56f, state.iconSizeDp)
            assertFalse(state.showLabels)
            assertEquals(24f, state.cornerRadiusDp)
        }
    }

    @Test
    fun onAppClicked_launchesAppViaRepository() = runTest {
        viewModel.onAppClicked(sampleApp)
        verify { appRepository.launchApp(sampleApp) }
    }

    @Test
    fun removeItem_delegatesToDockRepository() = runTest {
        viewModel.removeItem(0)
        testScheduler.advanceUntilIdle()
        coVerify { dockRepository.removeFromDock(0) }
    }

    @Test
    fun moveItemLeftAndRight_delegatesToDockRepository() = runTest {
        dockItemsFlow.value = listOf(
            DockItem(position = 0, packageName = "pkg.0", activityName = "act.0"),
            DockItem(position = 1, packageName = "pkg.1", activityName = "act.1")
        )
        testScheduler.advanceUntilIdle()

        viewModel.moveItemLeft(1)
        testScheduler.advanceUntilIdle()
        coVerify { dockRepository.reorderDock(1, 0) }

        viewModel.moveItemRight(0)
        testScheduler.advanceUntilIdle()
        coVerify { dockRepository.reorderDock(0, 1) }
    }

    @Test
    fun toggleEditMode_updatesState() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            assertFalse(expectMostRecentItem().isEditMode)

            viewModel.toggleEditMode()
            testScheduler.advanceUntilIdle()
            assertTrue(expectMostRecentItem().isEditMode)
        }
    }
}
