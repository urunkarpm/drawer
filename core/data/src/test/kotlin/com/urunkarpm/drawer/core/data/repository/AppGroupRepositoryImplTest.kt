package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppGroupRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appGroupDao: AppGroupDao = mockk(relaxed = true)
    private val appRepository: AppRepository = mockk(relaxed = true)
    private val groupsFlow = MutableStateFlow<List<AppGroupEntity>>(emptyList())
    private val itemsFlow = MutableStateFlow<List<AppGroupItemEntity>>(emptyList())

    private lateinit var repository: AppGroupRepositoryImpl

    private val sampleApp = AppInfo(
        packageName = "com.test.chat",
        activityName = "com.test.chat.MainActivity",
        label = "ChatApp"
    )

    @Before
    fun setUp() {
        every { appRepository.installedApps } returns MutableStateFlow(emptyList())
        every { appGroupDao.getAllGroups() } returns groupsFlow
        every { appGroupDao.getAllGroupItems() } returns itemsFlow
        every { appGroupDao.getItemsForGroup(any()) } returns itemsFlow
        repository = AppGroupRepositoryImpl(appGroupDao, { appRepository }, testDispatcher)
    }

    @Test
    fun groups_seedsDefaultGroupsWhenDatabaseIsEmpty() = runTest(testDispatcher) {
        groupsFlow.value = emptyList()

        val captured = slot<List<AppGroupEntity>>()
        coEvery { appGroupDao.insertGroups(capture(captured)) } returns Unit

        repository.groups.first()

        coVerify { appGroupDao.insertGroups(any()) }
        assertTrue(captured.captured.isNotEmpty())
        assertEquals(5, captured.captured.size)
        assertEquals("Work", captured.captured[0].name)
        assertEquals("Social", captured.captured[1].name)
    }

    @Test
    fun createGroup_insertsGroupEntityWithCorrectOrderIndex() = runTest(testDispatcher) {
        val existingGroup = AppGroupEntity(id = "group_1", name = "Existing", orderIndex = 0)
        groupsFlow.value = listOf(existingGroup)

        val inserted = slot<AppGroupEntity>()
        coEvery { appGroupDao.insertGroup(capture(inserted)) } returns Unit

        val created = repository.createGroup(
            name = "Productivity",
            iconName = "work",
            colorHex = "#1E88E5",
            viewType = GroupViewType.GRID,
            columnCount = 4,
            sortOrder = GroupSortOrder.MANUAL
        )

        coVerify { appGroupDao.insertGroup(any()) }
        assertEquals("Productivity", created.name)
        assertEquals(1, inserted.captured.orderIndex)
        assertEquals("Productivity", inserted.captured.name)
    }

    @Test
    fun toggleGroupExpanded_updatesEntityWithFlippedState() = runTest(testDispatcher) {
        val group = AppGroupEntity(id = "group_1", name = "Work", orderIndex = 0, isExpanded = true)
        groupsFlow.value = listOf(group)

        coEvery { appGroupDao.setOnlyGroupExpanded("group_1", false) } returns Unit

        repository.toggleGroupExpanded("group_1")

        coVerify { appGroupDao.setOnlyGroupExpanded("group_1", false) }
    }

    @Test
    fun deleteGroup_deletesAndReindexesRemainingGroups() = runTest(testDispatcher) {
        val g1 = AppGroupEntity(id = "g1", name = "G1", orderIndex = 0)
        val g2 = AppGroupEntity(id = "g2", name = "G2", orderIndex = 1)
        val g3 = AppGroupEntity(id = "g3", name = "G3", orderIndex = 2)
        groupsFlow.value = listOf(g1, g2, g3)

        // After deletion, groupsFlow emits g1 and g3
        coEvery { appGroupDao.deleteGroupById("g2") } answers {
            groupsFlow.value = listOf(g1, g3)
        }
        val reindexed = slot<List<AppGroupEntity>>()
        coEvery { appGroupDao.updateGroups(capture(reindexed)) } returns Unit

        repository.deleteGroup("g2")

        coVerify { appGroupDao.deleteGroupById("g2") }
        coVerify { appGroupDao.updateGroups(any()) }
        assertEquals(2, reindexed.captured.size)
        assertEquals(0, reindexed.captured[0].orderIndex)
        assertEquals("g1", reindexed.captured[0].id)
        assertEquals(1, reindexed.captured[1].orderIndex)
        assertEquals("g3", reindexed.captured[1].id)
    }

    @Test
    fun assignAppToGroup_singleGroupModeDeletesFromOtherGroups() = runTest(testDispatcher) {
        itemsFlow.value = emptyList()

        val insertedItems = slot<List<AppGroupItemEntity>>()
        coEvery { appGroupDao.insertGroupItems(capture(insertedItems)) } returns Unit

        repository.assignAppToGroup("group_social", sampleApp, allowMultiGroup = false)

        coVerify { appGroupDao.deleteItemsByComponent(sampleApp.packageName, sampleApp.activityName) }
        coVerify { appGroupDao.insertGroupItems(any()) }
        assertEquals("group_social", insertedItems.captured.first().groupId)
        assertEquals(sampleApp.packageName, insertedItems.captured.first().packageName)
        assertEquals(0, insertedItems.captured.first().orderIndex)
    }

    @Test
    fun assignAppToGroup_multiGroupModeDoesNotDeleteFromOtherGroups() = runTest(testDispatcher) {
        itemsFlow.value = emptyList()

        val insertedItems = slot<List<AppGroupItemEntity>>()
        coEvery { appGroupDao.insertGroupItems(capture(insertedItems)) } returns Unit

        repository.assignAppToGroup("group_social", sampleApp, allowMultiGroup = true)

        coVerify(exactly = 0) { appGroupDao.deleteItemsByComponent(any(), any()) }
        coVerify { appGroupDao.insertGroupItems(any()) }
        assertEquals("group_social", insertedItems.captured.first().groupId)
    }

    @Test
    fun removeAppFromGroup_removesAndReindexesItems() = runTest(testDispatcher) {
        val i1 = AppGroupItemEntity(id = "i1", groupId = "g1", packageName = "p1", activityName = "a1", orderIndex = 0)
        val i2 = AppGroupItemEntity(id = "i2", groupId = "g1", packageName = "p2", activityName = "a2", orderIndex = 1)
        itemsFlow.value = listOf(i1, i2)

        coEvery { appGroupDao.deleteGroupItem("g1", "p1", "a1") } answers {
            itemsFlow.value = listOf(i2)
        }
        val reindexed = slot<List<AppGroupItemEntity>>()
        coEvery { appGroupDao.insertGroupItems(capture(reindexed)) } returns Unit

        repository.removeAppFromGroup("g1", "p1", "a1")

        coVerify { appGroupDao.deleteGroupItem("g1", "p1", "a1") }
        coVerify { appGroupDao.insertGroupItems(any()) }
        assertEquals(1, reindexed.captured.size)
        assertEquals(0, reindexed.captured[0].orderIndex)
        assertEquals("i2", reindexed.captured[0].id)
    }

    @Test
    fun moveAppBetweenGroups_clearsFromSourceGroupAndAddsToTargetGroup() = runTest(testDispatcher) {
        val srcItem1 = AppGroupItemEntity(id = "s1", groupId = "group_work", packageName = sampleApp.packageName, activityName = sampleApp.activityName, orderIndex = 0)
        val srcItem2 = AppGroupItemEntity(id = "s2", groupId = "group_work", packageName = "p2", activityName = "a2", orderIndex = 1)
        val targetItem1 = AppGroupItemEntity(id = "t1", groupId = "group_tools", packageName = "p3", activityName = "a3", orderIndex = 0)

        coEvery { appGroupDao.deleteGroupItemsByPackage(any(), any()) } returns Unit
        coEvery { appGroupDao.getItemsForGroup("group_work") } returns kotlinx.coroutines.flow.flowOf(listOf(srcItem2))
        coEvery { appGroupDao.getItemsForGroup("group_tools") } returns kotlinx.coroutines.flow.flowOf(listOf(targetItem1))

        val capturedInserts = mutableListOf<List<AppGroupItemEntity>>()
        coEvery { appGroupDao.insertGroupItems(capture(capturedInserts)) } returns Unit

        repository.moveAppBetweenGroups("group_work", "group_tools", sampleApp)

        // Verify delete from source and target groups by package
        coVerify { appGroupDao.deleteGroupItemsByPackage("group_work", sampleApp.packageName) }
        coVerify { appGroupDao.deleteGroupItemsByPackage("group_tools", sampleApp.packageName) }

        // Source group re-indexed
        val sourceReindexed = capturedInserts.first()
        assertEquals(1, sourceReindexed.size)
        assertEquals("p2", sourceReindexed[0].packageName)
        assertEquals(0, sourceReindexed[0].orderIndex)

        // Target group re-indexed with new item
        val targetReindexed = capturedInserts.last()
        assertEquals(2, targetReindexed.size)
        assertEquals("p3", targetReindexed[0].packageName)
        assertEquals(sampleApp.packageName, targetReindexed[1].packageName)
        assertEquals("group_tools", targetReindexed[1].groupId)
        assertEquals(1, targetReindexed[1].orderIndex)
    }
}
