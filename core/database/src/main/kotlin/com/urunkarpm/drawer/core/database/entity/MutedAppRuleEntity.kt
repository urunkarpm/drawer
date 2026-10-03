package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule

@Entity(tableName = "muted_app_rules")
data class MutedAppRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "rule_type")
    val ruleType: String,
    @ColumnInfo(name = "snoozed_until_timestamp")
    val snoozedUntilTimestamp: Long? = null,
    @ColumnInfo(name = "auto_dismiss")
    val autoDismiss: Boolean = false
) {
    fun toDomain(): MutedAppRule = MutedAppRule(
        packageName = packageName,
        ruleType = try { MuteRuleType.valueOf(ruleType) } catch (_: Exception) { MuteRuleType.HIDE },
        snoozedUntilTimestamp = snoozedUntilTimestamp,
        autoDismiss = autoDismiss
    )

    companion object {
        fun fromDomain(rule: MutedAppRule): MutedAppRuleEntity = MutedAppRuleEntity(
            packageName = rule.packageName,
            ruleType = rule.ruleType.name,
            snoozedUntilTimestamp = rule.snoozedUntilTimestamp,
            autoDismiss = rule.autoDismiss
        )
    }
}
