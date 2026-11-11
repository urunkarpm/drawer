package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.urunkarpm.drawer.core.database.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {

    @Insert
    suspend fun insert(log: EventLogEntity): Long

    @Query("SELECT * FROM event_log ORDER BY time DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_log ORDER BY time DESC LIMIT :limit")
    suspend fun getRecentList(limit: Int = 20): List<EventLogEntity>

    @Query("DELETE FROM event_log WHERE time < :cutoffTime")
    suspend fun pruneOlderThan(cutoffTime: Long): Int
}
