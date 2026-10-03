package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DockDao {
    @Query("SELECT * FROM dock_items ORDER BY position ASC")
    fun getDockItems(): Flow<List<DockItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDockItem(item: DockItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDockItems(items: List<DockItemEntity>)

    @Query("DELETE FROM dock_items WHERE position = :position")
    suspend fun deleteDockItemAt(position: Int)

    @Query("DELETE FROM dock_items WHERE package_name = :packageName")
    suspend fun deleteDockItemsByPackage(packageName: String)

    @Query("DELETE FROM dock_items")
    suspend fun clearDock()

    @Transaction
    suspend fun replaceDock(items: List<DockItemEntity>) {
        clearDock()
        insertDockItems(items)
    }
}
