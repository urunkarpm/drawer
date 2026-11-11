package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.urunkarpm.drawer.core.database.entity.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: SavedPlaceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<SavedPlaceEntity>)

    @Query("DELETE FROM saved_place")
    suspend fun clearAll()

    @Delete
    suspend fun delete(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_place WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM saved_place ORDER BY name ASC")
    fun getAll(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_place ORDER BY name ASC")
    suspend fun getAllList(): List<SavedPlaceEntity>

    @Query("SELECT * FROM saved_place WHERE id = :id")
    suspend fun getById(id: Long): SavedPlaceEntity?
}
