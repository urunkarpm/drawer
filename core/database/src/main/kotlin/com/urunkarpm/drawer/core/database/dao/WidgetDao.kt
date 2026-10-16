package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.urunkarpm.drawer.core.database.entity.WidgetDrawerEntity
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {
    // ── Drawer Operations ──
    @Query("SELECT * FROM widget_drawers ORDER BY order_index ASC")
    fun getAllDrawers(): Flow<List<WidgetDrawerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawer(drawer: WidgetDrawerEntity)

    @Update
    suspend fun updateDrawer(drawer: WidgetDrawerEntity)

    @Query("DELETE FROM widget_drawers WHERE id = :id")
    suspend fun deleteDrawerById(id: String)

    @Query("DELETE FROM widget_drawers")
    suspend fun clearAllDrawers()

    // ── Widget Operations ──
    @Query("SELECT * FROM widgets ORDER BY order_index ASC")
    fun getAllWidgets(): Flow<List<WidgetItemEntity>>

    @Query("SELECT * FROM widgets WHERE drawer_id IS NULL ORDER BY order_index ASC")
    fun getStandaloneWidgets(): Flow<List<WidgetItemEntity>>

    @Query("SELECT * FROM widgets WHERE drawer_id = :drawerId ORDER BY order_index ASC")
    fun getWidgetsByDrawerId(drawerId: String): Flow<List<WidgetItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetItemEntity)

    @Update
    suspend fun updateWidget(widget: WidgetItemEntity)

    @Query("UPDATE widgets SET drawer_id = :drawerId WHERE id = :widgetId")
    suspend fun updateWidgetDrawerId(widgetId: String, drawerId: String?)

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteWidgetById(id: String)

    @Query("DELETE FROM widgets WHERE app_widget_id = :appWidgetId")
    suspend fun deleteWidgetByAppWidgetId(appWidgetId: Int)

    @Query("DELETE FROM widgets WHERE drawer_id = :drawerId")
    suspend fun deleteWidgetsByDrawerId(drawerId: String)

    @Query("UPDATE widgets SET drawer_id = NULL WHERE drawer_id = :drawerId")
    suspend fun detachWidgetsFromDrawer(drawerId: String)

    @Query("DELETE FROM widgets")
    suspend fun clearAll()
}
