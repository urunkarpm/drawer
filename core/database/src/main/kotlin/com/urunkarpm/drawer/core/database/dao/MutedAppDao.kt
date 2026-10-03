package com.urunkarpm.drawer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MutedAppDao {
    @Query("SELECT * FROM muted_app_rules")
    fun getAllRules(): Flow<List<MutedAppRuleEntity>>

    @Query("SELECT * FROM muted_app_rules WHERE package_name = :packageName LIMIT 1")
    suspend fun getRuleForPackage(packageName: String): MutedAppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: MutedAppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<MutedAppRuleEntity>)

    @Query("DELETE FROM muted_app_rules WHERE package_name = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("DELETE FROM muted_app_rules")
    suspend fun clearAllRules()
}
