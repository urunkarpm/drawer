package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widgets ORDER BY order_index ASC")
    fun getAllWidgets(): Flow<List<WidgetItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetItemEntity)

    @Update
    suspend fun updateWidget(widget: WidgetItemEntity)

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteWidgetById(id: String)

    @Query("DELETE FROM widgets WHERE app_widget_id = :appWidgetId")
    suspend fun deleteWidgetByAppWidgetId(appWidgetId: Int)

    @Query("DELETE FROM widgets")
    suspend fun clearAll()
}
