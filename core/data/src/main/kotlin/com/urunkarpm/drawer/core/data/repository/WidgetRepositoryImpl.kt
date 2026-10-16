package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.WidgetDao
import com.urunkarpm.drawer.core.database.entity.WidgetDrawerEntity
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity
import com.urunkarpm.drawer.core.model.WidgetDrawer
import com.urunkarpm.drawer.core.model.WidgetItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepositoryImpl @Inject constructor(
    private val widgetDao: WidgetDao
) : WidgetRepository {

    override fun getAllDrawers(): Flow<List<WidgetDrawer>> {
        return combine(
            widgetDao.getAllDrawers(),
            widgetDao.getAllWidgets()
        ) { drawerEntities, widgetEntities ->
            val domainWidgets = widgetEntities.map { it.toDomain() }
            val widgetsByDrawer = domainWidgets.filter { it.drawerId != null }.groupBy { it.drawerId }
            drawerEntities.map { drawerEntity ->
                val children = widgetsByDrawer[drawerEntity.id]?.sortedBy { it.orderIndex } ?: emptyList()
                drawerEntity.toDomain(children)
            }
        }
    }

    override fun getAllWidgets(): Flow<List<WidgetItem>> {
        return widgetDao.getAllWidgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getStandaloneWidgets(): Flow<List<WidgetItem>> {
        return widgetDao.getStandaloneWidgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getWidgetsByDrawerId(drawerId: String): Flow<List<WidgetItem>> {
        return widgetDao.getWidgetsByDrawerId(drawerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createDrawer(
        title: String,
        iconName: String,
        colorHex: String,
        spanX: Int,
        spanY: Int
    ): String {
        val currentDrawers = widgetDao.getAllDrawers().first()
        val nextOrder = currentDrawers.size
        val id = UUID.randomUUID().toString()
        val entity = WidgetDrawerEntity(
            id = id,
            title = title,
            orderIndex = nextOrder,
            spanX = spanX,
            spanY = spanY,
            isExpanded = true,
            iconName = iconName,
            colorHex = colorHex
        )
        widgetDao.insertDrawer(entity)
        return id
    }

    override suspend fun createDrawer(title: String, spanX: Int, spanY: Int): String {
        return createDrawer(title, "folder", "#1E88E5", spanX, spanY)
    }

    override suspend fun updateDrawer(drawer: WidgetDrawer) {
        widgetDao.updateDrawer(WidgetDrawerEntity.fromDomain(drawer))
    }

    override suspend fun deleteDrawer(id: String, deleteChildren: Boolean) {
        if (deleteChildren) {
            widgetDao.deleteWidgetsByDrawerId(id)
        } else {
            widgetDao.detachWidgetsFromDrawer(id)
        }
        widgetDao.deleteDrawerById(id)
    }

    override suspend fun addWidget(widget: WidgetItem) {
        widgetDao.insertWidget(WidgetItemEntity.fromDomain(widget))
    }

    override suspend fun updateWidget(widget: WidgetItem) {
        widgetDao.updateWidget(WidgetItemEntity.fromDomain(widget))
    }

    override suspend fun deleteWidget(id: String, appWidgetId: Int) {
        widgetDao.deleteWidgetById(id)
    }

    override suspend fun assignWidgetToDrawer(widgetId: String, drawerId: String?) {
        widgetDao.updateWidgetDrawerId(widgetId, drawerId)
    }
}
