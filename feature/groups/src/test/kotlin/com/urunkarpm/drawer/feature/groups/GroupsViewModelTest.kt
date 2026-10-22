package com.urunkarpm.drawer.feature.groups

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.ai.CategorizationSuggestion
import com.urunkarpm.drawer.core.data.repository.AppGroupRepository
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppGroupItem
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
class GroupsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appGroupRepository: AppGroupRepository = mockk(relaxed = true)
    private val appRepository: AppRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val groupsFlow = MutableStateFlow<List<AppGroup>>(emptyList())
    private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val multiGroupFlow = MutableStateFlow(false)
    private val lockLayoutFlow = MutableStateFlow(false)
    private val drawerNotificationGlowFlow = MutableStateFlow(true)
    private val drawerTitleAnimationFlow = MutableStateFlow("ACCENT_POP")

    private lateinit var viewModel: GroupsViewModel

    private val appA = AppInfo(packageName = "com.a", activityName = "com.a.Act", label = "Alpha")
    private val appZ = AppInfo(packageName = "com.z", activityName = "com.z.Act", label = "Zulu")

    private val sampleGroup = AppGroup(
        id = "group_work",
        name = "Work",
        iconName = "work",
        colorHex = "#1E88E5",
        orderIndex = 0,
        isExpanded = true,
        viewType = GroupViewType.GRID,
        columnCount = 4,
        sortOrder = GroupSortOrder.ALPHABETICAL,
        items = listOf(
            AppGroupItem(id = "item_1", groupId = "group_work", packageName = "com.z", activityName = "com.z.Act", orderIndex = 0),
            AppGroupItem(id = "item_2", groupId = "group_work", packageName = "com.a", activityName = "com.a.Act", orderIndex = 1)
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appGroupRepository.groups } returns groupsFlow
        every { appRepository.installedApps } returns installedAppsFlow
        every { preferencesDataSource.multiGroupApps } returns multiGroupFlow
        every { preferencesDataSource.lockLayout } returns lockLayoutFlow
        every { preferencesDataSource.drawerNotificationGlow } returns drawerNotificationGlowFlow
        every { preferencesDataSource.drawerTitleAnimation } returns drawerTitleAnimationFlow

        groupsFlow.value = listOf(sampleGroup)
        installedAppsFlow.value = listOf(appZ, appA)

        viewModel = GroupsViewModel(appGroupRepository, appRepository, preferencesDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_resolvesAppsAndSortsAlphabetically() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.groups.size)
            val resolvedGroup = state.groups.first()
            assertEquals("Work", resolvedGroup.group.name)
            // Even though item order was com.z then com.a, alphabetical sort puts Alpha first
            assertEquals(2, resolvedGroup.apps.size)
            assertEquals("Alpha", resolvedGroup.apps[0].label)
            assertEquals("Zulu", resolvedGroup.apps[1].label)
        }
    }

    @Test
    fun createGroup_delegatesToRepository() = runTest {
        viewModel.createGroup(
            name = "Finance",
            iconName = "account_balance",
            colorHex = "#4CAF50",
            viewType = GroupViewType.GRID,
            columnCount = 4,
            sortOrder = GroupSortOrder.MANUAL
        )
        testScheduler.advanceUntilIdle()

        coVerify {
            appGroupRepository.createGroup("Finance", "account_balance", "#4CAF50", GroupViewType.GRID, 4, GroupSortOrder.MANUAL)
        }
    }

    @Test
    fun toggleGroupExpanded_delegatesToRepository() = runTest {
        viewModel.toggleGroupExpanded("group_work")
        testScheduler.advanceUntilIdle()

        coVerify { appGroupRepository.toggleGroupExpanded("group_work") }
    }

    @Test
    fun deleteGroup_delegatesToRepository() = runTest {
        viewModel.deleteGroup("group_work")
        testScheduler.advanceUntilIdle()

        coVerify { appGroupRepository.deleteGroup("group_work") }
    }

    @Test
    fun moveGroupUpAndDown_delegatesToRepository() = runTest {
        viewModel.moveGroupUp(1)
        testScheduler.advanceUntilIdle()
        coVerify { appGroupRepository.reorderGroups(1, 0) }

        viewModel.moveGroupDown(0, 3)
        testScheduler.advanceUntilIdle()
        coVerify { appGroupRepository.reorderGroups(0, 1) }
    }

    @Test
    fun assignAppToGroup_checksMultiGroupPreferenceAndDelegates() = runTest {
        multiGroupFlow.value = false
        viewModel.assignAppToGroup("group_work", appA)
        testScheduler.advanceUntilIdle()

        coVerify { appGroupRepository.assignAppToGroup("group_work", appA, false) }
    }

    @Test
    fun removeAppFromGroup_delegatesToRepository() = runTest {
        viewModel.removeAppFromGroup("group_work", "com.a", "com.a.Act")
        testScheduler.advanceUntilIdle()

        coVerify { appGroupRepository.removeAppFromGroup("group_work", "com.a", "com.a.Act") }
    }

    @Test
    fun moveAppBetweenGroups_delegatesToRepository() = runTest {
        viewModel.moveAppBetweenGroups("group_work", "group_tools", appA, 2)
        testScheduler.advanceUntilIdle()

        coVerify { appGroupRepository.moveAppBetweenGroups("group_work", "group_tools", appA, 2) }
    }

    @Test
    fun launchApp_delegatesToAppRepository() = runTest {
        viewModel.launchApp(appA)
        verify { appRepository.launchApp(appA) }
    }

    @Test
    fun uiState_reflectsDrawerNotificationGlowPreference() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.drawerNotificationGlow)

            drawerNotificationGlowFlow.value = false
            val updated = awaitItem()
            assertFalse(updated.drawerNotificationGlow)
        }
    }

    @Test
    fun startAiCategorization_emitsSuggestionsAndSetsLoading() = runTest {
        val suggestion = CategorizationSuggestion(group = sampleGroup, suggestedApps = listOf(appA))
        coEvery { appGroupRepository.suggestCategorization(any()) } coAnswers {
            assertTrue(viewModel.isAiCategorizing.value)
            listOf(suggestion)
        }

        assertFalse(viewModel.isAiCategorizing.value)
        assertNull(viewModel.aiSuggestions.value)

        viewModel.startAiCategorization(unassignedOnly = true)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.isAiCategorizing.value)
        assertEquals(listOf(suggestion), viewModel.aiSuggestions.value)
        coVerify { appGroupRepository.suggestCategorization(true) }
    }

    @Test
    fun applyAiSuggestions_callsRepositoryAndClearsState() = runTest {
        val suggestion = CategorizationSuggestion(group = sampleGroup, suggestedApps = listOf(appA))
        coEvery { appGroupRepository.suggestCategorization(any()) } returns listOf(suggestion)

        viewModel.startAiCategorization(unassignedOnly = true)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(suggestion), viewModel.aiSuggestions.value)

        viewModel.applyAiSuggestions(listOf(suggestion))
        testScheduler.advanceUntilIdle()

        coVerify { appGroupRepository.applyCategorization(listOf(suggestion)) }
        assertNull(viewModel.aiSuggestions.value)
    }

    @Test
    fun dismissAiSuggestions_clearsSuggestionsState() = runTest {
        val suggestion = CategorizationSuggestion(group = sampleGroup, suggestedApps = listOf(appA))
        coEvery { appGroupRepository.suggestCategorization(any()) } returns listOf(suggestion)

        viewModel.startAiCategorization(unassignedOnly = true)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(suggestion), viewModel.aiSuggestions.value)

        viewModel.dismissAiSuggestions()
        assertNull(viewModel.aiSuggestions.value)
        assertFalse(viewModel.isAiCategorizing.value)
    }

    @Test
    fun uiState_reflectsAiCategorizationState() = runTest {
        val suggestion = CategorizationSuggestion(group = sampleGroup, suggestedApps = listOf(appA))
        coEvery { appGroupRepository.suggestCategorization(any()) } returns listOf(suggestion)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isAiCategorizing)
            assertNull(initial.aiSuggestions)

            viewModel.startAiCategorization()
            testScheduler.advanceUntilIdle()

            val updated = expectMostRecentItem()
            assertFalse(updated.isAiCategorizing)
            assertEquals(listOf(suggestion), updated.aiSuggestions)

            viewModel.dismissAiSuggestions()
            testScheduler.advanceUntilIdle()

            val dismissed = expectMostRecentItem()
            assertNull(dismissed.aiSuggestions)
        }
    }

    @Test
    fun startAiCategorization_whenDismissedWhileRunning_cancelsJobAndResetsState() = runTest {
        coEvery { appGroupRepository.suggestCategorization(any()) } coAnswers {
            delay(5000)
            listOf(CategorizationSuggestion(group = sampleGroup, suggestedApps = listOf(appA)))
        }

        viewModel.startAiCategorization()
        testScheduler.advanceTimeBy(100)
        assertTrue(viewModel.isAiCategorizing.value)

        viewModel.dismissAiSuggestions()
        assertFalse(viewModel.isAiCategorizing.value)
        assertNull(viewModel.aiSuggestions.value)

        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.isAiCategorizing.value)
        assertNull(viewModel.aiSuggestions.value)
    }

    @Test
    fun startAiCategorization_tracksIsAiModelReadyAndResetsOnDismiss() = runTest {
        every { appGroupRepository.isAiModelReady } returns true
        val suggestion = CategorizationSuggestion(group = sampleGroup, suggestedApps = listOf(appA))
        coEvery { appGroupRepository.suggestCategorization(any()) } returns listOf(suggestion)

        assertFalse(viewModel.isAiModelReady.value)

        viewModel.startAiCategorization()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.isAiModelReady.value)
        assertEquals(listOf(suggestion), viewModel.aiSuggestions.value)

        viewModel.dismissAiSuggestions()
        assertFalse(viewModel.isAiModelReady.value)
        assertNull(viewModel.aiSuggestions.value)
    }
}
