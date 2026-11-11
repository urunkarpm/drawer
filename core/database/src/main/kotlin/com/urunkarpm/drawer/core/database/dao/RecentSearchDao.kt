package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.urunkarpm.drawer.core.database.entity.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: RecentSearchEntity)

    @Query("SELECT * FROM recent_search ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 5): Flow<List<RecentSearchEntity>>

    @Query("SELECT * FROM recent_search ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentList(limit: Int = 5): List<RecentSearchEntity>

    @Query("DELETE FROM recent_search WHERE query NOT IN (SELECT query FROM recent_search ORDER BY created_at DESC LIMIT :keepCount)")
    suspend fun pruneOld(keepCount: Int = 5)

    @Transaction
    suspend fun saveAndPrune(search: RecentSearchEntity, keepCount: Int = 5) {
        insert(search)
        pruneOld(keepCount)
    }
}
