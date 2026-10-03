package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IconPackOverrideDao {
    @Query("SELECT * FROM icon_pack_overrides")
    fun getAllOverrides(): Flow<List<IconPackOverrideEntity>>

    @Query("SELECT * FROM icon_pack_overrides WHERE component_name = :componentName LIMIT 1")
    suspend fun getOverrideForComponent(componentName: String): IconPackOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: IconPackOverrideEntity)

    @Query("DELETE FROM icon_pack_overrides WHERE component_name = :componentName")
    suspend fun deleteOverride(componentName: String)

    @Query("DELETE FROM icon_pack_overrides")
    suspend fun clearOverrides()
}
