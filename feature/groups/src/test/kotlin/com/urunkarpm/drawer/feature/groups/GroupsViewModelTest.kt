package com.urunkarpm.drawer.feature.groups

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.AppGroupRepository
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppGroupItem
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
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
class GroupsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appGroupRepository: AppGroupRepository = mockk(relaxed = true)
    private val appRepository: AppRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val groupsFlow = MutableStateFlow<List<AppGroup>>(emptyList())
    private val installedAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val multiGroupFlow = MutableStateFlow(false)

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
    fun launchApp_delegatesToAppRepository() = runTest {
        viewModel.launchApp(appA)
        verify { appRepository.launchApp(appA) }
    }
}
