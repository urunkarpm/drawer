package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppGroupDao {
    @Query("SELECT * FROM app_groups ORDER BY order_index ASC")
    fun getAllGroups(): Flow<List<AppGroupEntity>>

    @Query("SELECT * FROM app_group_items WHERE group_id = :groupId ORDER BY order_index ASC")
    fun getItemsForGroup(groupId: String): Flow<List<AppGroupItemEntity>>

    @Query("SELECT * FROM app_group_items ORDER BY order_index ASC")
    fun getAllGroupItems(): Flow<List<AppGroupItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: AppGroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(groups: List<AppGroupEntity>)

    @Update
    suspend fun updateGroup(group: AppGroupEntity)

    @Update
    suspend fun updateGroups(groups: List<AppGroupEntity>)

    @Query("UPDATE app_groups SET is_expanded = CASE WHEN id = :groupId THEN :expanded ELSE 0 END")
    suspend fun setOnlyGroupExpanded(groupId: String, expanded: Boolean)

    @Query("UPDATE app_groups SET is_expanded = 0")
    suspend fun collapseAllGroups()

    @Query("DELETE FROM app_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupItem(item: AppGroupItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupItems(items: List<AppGroupItemEntity>)
    
    @Update
    suspend fun updateGroupItems(items: List<AppGroupItemEntity>)

    @Query("DELETE FROM app_group_items WHERE id = :itemId")
    suspend fun deleteGroupItemById(itemId: String)

    @Query("DELETE FROM app_group_items WHERE package_name = :packageName")
    suspend fun deleteItemsByPackageName(packageName: String)

    @Query("DELETE FROM app_group_items WHERE package_name = :packageName AND activity_name = :activityName")
    suspend fun deleteItemsByComponent(packageName: String, activityName: String)

    @Query("DELETE FROM app_group_items WHERE group_id = :groupId AND package_name = :packageName AND activity_name = :activityName")
    suspend fun deleteGroupItem(groupId: String, packageName: String, activityName: String)

    @Query("DELETE FROM app_group_items WHERE group_id = :groupId AND package_name = :packageName")
    suspend fun deleteGroupItemsByPackage(groupId: String, packageName: String)

    @Query("DELETE FROM app_group_items WHERE group_id = :groupId")
    suspend fun clearGroupItems(groupId: String)

    @Transaction
    suspend fun replaceAllGroups(groups: List<AppGroupEntity>, items: List<AppGroupItemEntity>) {
        deleteAllGroups()
        insertGroups(groups)
        insertGroupItems(items)
    }

    @Query("DELETE FROM app_groups")
    suspend fun deleteAllGroups()
}
