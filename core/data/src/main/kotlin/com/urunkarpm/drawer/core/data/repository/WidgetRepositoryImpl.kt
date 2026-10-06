package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.WidgetDao
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity
import com.urunkarpm.drawer.core.model.WidgetItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepositoryImpl @Inject constructor(
    private val widgetDao: WidgetDao
) : WidgetRepository {

    override fun getAllWidgets(): Flow<List<WidgetItem>> {
        return widgetDao.getAllWidgets().map { entities ->
            entities.map { it.toDomain() }
        }
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
}
