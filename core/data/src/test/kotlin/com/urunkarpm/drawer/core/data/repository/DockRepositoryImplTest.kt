package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.model.AppInfo
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
class DockRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dockDao: DockDao = mockk(relaxed = true)
    private val dockFlow = MutableStateFlow<List<DockItemEntity>>(emptyList())

    private lateinit var repository: DockRepositoryImpl

    private val sampleApp = AppInfo(
        packageName = "com.test.phone",
        activityName = "com.test.phone.Dialer",
        label = "Phone"
    )

    @Before
    fun setUp() {
        every { dockDao.getDockItems() } returns dockFlow
        repository = DockRepositoryImpl(dockDao, testDispatcher)
    }

    @Test
    fun addToDock_succeedsWhenDockHasSpace() = runTest(testDispatcher) {
        dockFlow.value = emptyList()

        val added = repository.addToDock(sampleApp)
        assertTrue(added)

        val slot = slot<DockItemEntity>()
        coVerify { dockDao.insertDockItem(capture(slot)) }
        assertEquals(0, slot.captured.position)
        assertEquals("com.test.phone", slot.captured.packageName)
    }

    @Test
    fun addToDock_failsWhenDockHasFiveItems() = runTest(testDispatcher) {
        dockFlow.value = (0..4).map { i ->
            DockItemEntity(position = i, packageName = "pkg.$i", activityName = "act.$i")
        }

        val added = repository.addToDock(sampleApp)
        assertFalse(added)
        coVerify(exactly = 0) { dockDao.insertDockItem(any()) }
    }

    @Test
    fun addToDock_failsWhenAppAlreadyInDock() = runTest(testDispatcher) {
        dockFlow.value = listOf(
            DockItemEntity(position = 0, packageName = sampleApp.packageName, activityName = sampleApp.activityName)
        )

        val added = repository.addToDock(sampleApp)
        assertFalse(added)
        coVerify(exactly = 0) { dockDao.insertDockItem(any()) }
    }

    @Test
    fun removeFromDock_reindexesRemainingItems() = runTest(testDispatcher) {
        dockFlow.value = listOf(
            DockItemEntity(position = 0, packageName = "pkg.0", activityName = "act.0"),
            DockItemEntity(position = 1, packageName = "pkg.1", activityName = "act.1"),
            DockItemEntity(position = 2, packageName = "pkg.2", activityName = "act.2")
        )

        val replacedList = slot<List<DockItemEntity>>()
        coEvery { dockDao.replaceDock(capture(replacedList)) } returns Unit

        repository.removeFromDock(1)

        coVerify { dockDao.replaceDock(any()) }
        val remaining = replacedList.captured
        assertEquals(2, remaining.size)
        assertEquals(0, remaining[0].position)
        assertEquals("pkg.0", remaining[0].packageName)
        assertEquals(1, remaining[1].position)
        assertEquals("pkg.2", remaining[1].packageName)
    }

    @Test
    fun reorderDock_swapsAndReindexesProperly() = runTest(testDispatcher) {
        dockFlow.value = listOf(
            DockItemEntity(position = 0, packageName = "pkg.0", activityName = "act.0"),
            DockItemEntity(position = 1, packageName = "pkg.1", activityName = "act.1"),
            DockItemEntity(position = 2, packageName = "pkg.2", activityName = "act.2")
        )

        val replacedList = slot<List<DockItemEntity>>()
        coEvery { dockDao.replaceDock(capture(replacedList)) } returns Unit

        // Move item at 0 to position 2
        repository.reorderDock(0, 2)

        coVerify { dockDao.replaceDock(any()) }
        val reordered = replacedList.captured
        assertEquals(3, reordered.size)
        assertEquals("pkg.1", reordered[0].packageName)
        assertEquals(0, reordered[0].position)
        assertEquals("pkg.2", reordered[1].packageName)
        assertEquals(1, reordered[1].position)
        assertEquals("pkg.0", reordered[2].packageName)
        assertEquals(2, reordered[2].position)
    }
}
