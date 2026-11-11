package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    @Query("DELETE FROM todo")
    suspend fun clearAll()

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("DELETE FROM todo WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM todo WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoEntity?

    @Query("SELECT * FROM todo WHERE id = :id")
    fun getTodoByIdFlow(id: Long): Flow<TodoEntity?>

    @Query("SELECT * FROM todo ORDER BY created_at DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo WHERE status = 'OPEN' OR status = 'SNOOZED' ORDER BY created_at DESC")
    fun getOpenTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo WHERE status = 'DONE' ORDER BY completed_at DESC")
    fun getDoneTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo WHERE status = 'OPEN' OR status = 'SNOOZED'")
    suspend fun getOpenTodosList(): List<TodoEntity>

    @Query("SELECT * FROM todo WHERE status = 'DONE'")
    suspend fun getDoneTodosList(): List<TodoEntity>

    @Query("SELECT * FROM todo")
    suspend fun getAllTodosList(): List<TodoEntity>

    @Query("UPDATE todo SET status = :status, completed_at = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedAt: Long?)

    @Query("DELETE FROM todo WHERE status = 'DONE' AND completed_at IS NOT NULL AND completed_at < :cutoffTimestamp")
    suspend fun deleteOldDone(cutoffTimestamp: Long): Int

    @Query("DELETE FROM todo WHERE status = 'DONE'")
    suspend fun clearAllDone(): Int
}
