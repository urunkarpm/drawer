package com.urunkarpm.drawer.feature.home

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.model.AppInfo
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
    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

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

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appRepository.installedApps } returns appsFlow
        appsFlow.value = sampleApps
        viewModel = HomeViewModel(appRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoadsInstalledApps() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            testScheduler.advanceUntilIdle()
            val loadedState = if (state.installedApps.isEmpty()) awaitItem() else state
            assertEquals(3, loadedState.installedApps.size)
            assertEquals(3, loadedState.filteredApps.size)
            assertEquals("", loadedState.searchQuery)
            assertFalse(loadedState.isAllAppsOpen)
            assertNull(loadedState.selectedAppForMenu)
        }
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
}
