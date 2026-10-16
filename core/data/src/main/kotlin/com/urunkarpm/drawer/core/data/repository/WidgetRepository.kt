package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.WidgetDrawer
import com.urunkarpm.drawer.core.model.WidgetItem
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    fun getAllDrawers(): Flow<List<WidgetDrawer>>
    fun getAllWidgets(): Flow<List<WidgetItem>>
    fun getStandaloneWidgets(): Flow<List<WidgetItem>>
    fun getWidgetsByDrawerId(drawerId: String): Flow<List<WidgetItem>>

    suspend fun createDrawer(title: String, spanX: Int = 4, spanY: Int = 3): String
    suspend fun updateDrawer(drawer: WidgetDrawer)
    suspend fun deleteDrawer(id: String, deleteChildren: Boolean = false)

    suspend fun addWidget(widget: WidgetItem)
    suspend fun updateWidget(widget: WidgetItem)
    suspend fun deleteWidget(id: String, appWidgetId: Int)
    suspend fun assignWidgetToDrawer(widgetId: String, drawerId: String?)
}
