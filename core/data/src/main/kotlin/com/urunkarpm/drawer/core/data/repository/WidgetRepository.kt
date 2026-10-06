package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.WidgetItem
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    fun getAllWidgets(): Flow<List<WidgetItem>>
    suspend fun addWidget(widget: WidgetItem)
    suspend fun updateWidget(widget: WidgetItem)
    suspend fun deleteWidget(id: String, appWidgetId: Int)
}
