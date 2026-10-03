package com.urunkarpm.drawer.feature.iconpacks

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.IconPackRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.IconOverride
import com.urunkarpm.drawer.core.model.IconPackInfo
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IconPackViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val iconPackRepository: IconPackRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val installedPacksFlow = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val activePackFlow = MutableStateFlow<String?>(null)
    private val adaptiveShapeFlow = MutableStateFlow("SYSTEM")
    private val overridesFlow = MutableStateFlow<List<IconOverride>>(emptyList())

    private lateinit var viewModel: IconPackViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { iconPackRepository.installedIconPacks } returns installedPacksFlow
        every { preferencesDataSource.activeIconPack } returns activePackFlow
        every { preferencesDataSource.adaptiveIconShape } returns adaptiveShapeFlow
        every { iconPackRepository.overrides } returns overridesFlow

        installedPacksFlow.value = listOf(
            IconPackInfo(packageName = "", name = "System Default", isSystemDefault = true),
            IconPackInfo(packageName = "com.icon.pack", name = "Test Pack")
        )

        viewModel = IconPackViewModel(iconPackRepository, preferencesDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_resolvesPacksAndPreferences() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(2, state.installedPacks.size)
            assertEquals("SYSTEM", state.adaptiveIconShape)
        }
    }

    @Test
    fun selectIconPack_delegatesToRepository() = runTest {
        viewModel.selectIconPack("com.icon.pack")
        testScheduler.advanceUntilIdle()

        coVerify { iconPackRepository.setActiveIconPack("com.icon.pack") }
    }

    @Test
    fun setAdaptiveIconShape_updatesPreferences() = runTest {
        viewModel.setAdaptiveIconShape("CIRCLE")
        testScheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setAdaptiveIconShape("CIRCLE") }
    }

    @Test
    fun setAppOverride_delegatesToRepository() = runTest {
        viewModel.setAppOverride("com.app/com.app.Act", "com.icon.pack", "ic_app")
        testScheduler.advanceUntilIdle()

        coVerify { iconPackRepository.setAppOverride("com.app/com.app.Act", "com.icon.pack", "ic_app") }
    }
}
