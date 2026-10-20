package com.urunkarpm.drawer.feature.settings

import com.urunkarpm.drawer.core.data.repository.BackupRepository
import com.urunkarpm.drawer.core.data.repository.UpdateRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelNeedleTest {

    private val testDispatcher = StandardTestDispatcher()
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val backupRepository: BackupRepository = mockk(relaxed = true)
    private val updateRepository: UpdateRepository = mockk(relaxed = true)
    private val mockEngine: NeedleEngine = mockk(relaxed = true)
    private val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockEngine.modelStatus } returns statusFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNeedleStatusReflected() = runTest {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = mockEngine
        )

        assertEquals(ModelStatus.NotDownloaded, viewModel.needleModelStatus.value)

        statusFlow.value = ModelStatus.Ready(modelPath = "needle-3.bin", sizeMb = 10.3f)
        assertEquals(ModelStatus.Ready(modelPath = "needle-3.bin", sizeMb = 10.3f), viewModel.needleModelStatus.value)
    }

    @Test
    fun testNeedleStatusDefaultsToNotDownloadedWhenEngineNull() {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = null
        )

        assertEquals(ModelStatus.NotDownloaded, viewModel.needleModelStatus.value)
    }

    @Test
    fun testDownloadNeedleModelDelegatesToEngine() = runTest {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = mockEngine
        )

        viewModel.downloadNeedleModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockEngine.downloadModel() }
    }

    @Test
    fun testDeleteNeedleModelDelegatesToEngine() = runTest {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = mockEngine
        )

        viewModel.deleteNeedleModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockEngine.deleteModel() }
    }

    @Test
    fun testSetAiCategorizationStrictnessDelegatesToPreferences() = runTest {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = mockEngine
        )

        viewModel.setAiCategorizationStrictness("AGGRESSIVE")
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesDataSource.setAiCategorizationStrictness("AGGRESSIVE") }
    }

    @Test
    fun testSetAiCategorizeAllAppsDelegatesToPreferences() = runTest {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = mockEngine
        )

        viewModel.setAiCategorizeAllApps(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesDataSource.setAiCategorizeAllApps(true) }
    }

    @Test
    fun testSetAiForceWorkProfileDelegatesToPreferences() = runTest {
        val viewModel = SettingsViewModel(
            preferencesDataSource = preferencesDataSource,
            backupRepository = backupRepository,
            updateRepository = updateRepository,
            ioDispatcher = testDispatcher,
            needleEngine = mockEngine
        )

        viewModel.setAiForceWorkProfile(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { preferencesDataSource.setAiForceWorkProfile(false) }
    }
}
