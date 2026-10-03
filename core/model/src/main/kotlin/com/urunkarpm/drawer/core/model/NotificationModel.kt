package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

data class NotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTimeMillis: Long,
    val isClearable: Boolean = true
)

@Serializable
enum class MuteRuleType {
    HIDE,
    SNOOZE_1H,
    SNOOZE_UNTIL_TOMORROW,
    SNOOZE_PERMANENT
}

@Serializable
data class MutedAppRule(
    val packageName: String,
    val ruleType: MuteRuleType,
    val snoozedUntilTimestamp: Long? = null,
    val autoDismiss: Boolean = false
)
