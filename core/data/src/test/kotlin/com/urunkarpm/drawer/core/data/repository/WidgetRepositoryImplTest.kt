package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.WidgetDao
import com.urunkarpm.drawer.core.database.entity.WidgetDrawerEntity
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity
import com.urunkarpm.drawer.core.model.WidgetItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRepositoryImplTest {

    private val widgetDao: WidgetDao = mockk(relaxed = true)
    private val drawersFlow = MutableStateFlow<List<WidgetDrawerEntity>>(emptyList())
    private val widgetsFlow = MutableStateFlow<List<WidgetItemEntity>>(emptyList())

    private lateinit var repository: WidgetRepositoryImpl

    @Before
    fun setUp() {
        every { widgetDao.getAllDrawers() } returns drawersFlow
        every { widgetDao.getAllWidgets() } returns widgetsFlow
        repository = WidgetRepositoryImpl(widgetDao)
    }

    @Test
    fun getAllDrawers_combinesDrawersAndChildrenCorrectly() = runTest {
        val drawerEntity = WidgetDrawerEntity(
            id = "drawer-1",
            title = "Finance",
            orderIndex = 0,
            spanX = 4,
            spanY = 3,
            isExpanded = true
        )
        val widgetEntity1 = WidgetItemEntity(
            id = "w-1",
            appWidgetId = 101,
            packageName = "com.finance.app",
            providerClassName = "com.finance.Widget",
            drawerId = "drawer-1",
            orderIndex = 0,
            spanX = 2,
            spanY = 2,
            heightDp = 140
        )
        val widgetEntity2 = WidgetItemEntity(
            id = "w-2",
            appWidgetId = 102,
            packageName = "com.news.app",
            providerClassName = "com.news.Widget",
            drawerId = null,
            orderIndex = 1,
            spanX = 4,
            spanY = 2,
            heightDp = 180
        )

        drawersFlow.value = listOf(drawerEntity)
        widgetsFlow.value = listOf(widgetEntity1, widgetEntity2)

        val result = repository.getAllDrawers().first()
        assertEquals(1, result.size)
        assertEquals("Finance", result[0].title)
        assertEquals(1, result[0].widgets.size)
        assertEquals("w-1", result[0].widgets[0].id)
    }

    @Test
    fun createDrawer_insertsNewDrawerEntity() = runTest {
        drawersFlow.value = emptyList()

        val drawerId = repository.createDrawer("Media", spanX = 4, spanY = 3)
        assertTrue(drawerId.isNotBlank())

        val slot = slot<WidgetDrawerEntity>()
        coVerify { widgetDao.insertDrawer(capture(slot)) }
        assertEquals("Media", slot.captured.title)
        assertEquals(0, slot.captured.orderIndex)
    }

    @Test
    fun deleteDrawer_detachesChildrenWhenDeleteChildrenFalse() = runTest {
        repository.deleteDrawer("drawer-1", deleteChildren = false)

        coVerify { widgetDao.detachWidgetsFromDrawer("drawer-1") }
        coVerify { widgetDao.deleteDrawerById("drawer-1") }
    }

    @Test
    fun deleteDrawer_deletesChildrenWhenDeleteChildrenTrue() = runTest {
        repository.deleteDrawer("drawer-1", deleteChildren = true)

        coVerify { widgetDao.deleteWidgetsByDrawerId("drawer-1") }
        coVerify { widgetDao.deleteDrawerById("drawer-1") }
    }

    @Test
    fun assignWidgetToDrawer_updatesWidgetDrawerId() = runTest {
        repository.assignWidgetToDrawer("w-1", "drawer-1")

        coVerify { widgetDao.updateWidgetDrawerId("w-1", "drawer-1") }
    }

    @Test
    fun addWidget_insertsWidgetEntity() = runTest {
        val widget = WidgetItem(
            id = "w-1",
            appWidgetId = 101,
            packageName = "com.test",
            providerClassName = "com.test.Widget",
            drawerId = null,
            orderIndex = 0,
            spanX = 2,
            spanY = 2,
            heightDp = 140
        )

        repository.addWidget(widget)

        val slot = slot<WidgetItemEntity>()
        coVerify { widgetDao.insertWidget(capture(slot)) }
        assertEquals("w-1", slot.captured.id)
        assertEquals(2, slot.captured.spanX)
    }
}
