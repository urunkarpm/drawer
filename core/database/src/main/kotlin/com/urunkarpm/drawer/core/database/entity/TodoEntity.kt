package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoStatus

@Entity(tableName = "todo")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val note: String? = null,
    @ColumnInfo(name = "place_name")
    val placeName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ColumnInfo(name = "radius_m")
    val radiusM: Float = 200f,
    @ColumnInfo(name = "deadline_utc")
    val deadlineUtc: Long? = null,
    @ColumnInfo(name = "deadline_zone_id")
    val deadlineZoneId: String? = null,
    val priority: String = "NORMAL",
    val status: String = "OPEN",
    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: Long? = null,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    @ColumnInfo(name = "stale_since")
    val staleSince: Long? = null,
    @ColumnInfo(name = "nudge_count")
    val nudgeCount: Int = 0,
    @ColumnInfo(name = "last_nudge_at")
    val lastNudgeAt: Long? = null,
    @ColumnInfo(name = "last_location_alert_at")
    val lastLocationAlertAt: Long? = null,
    @ColumnInfo(name = "repeat_rule")
    val repeatRule: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): TodoItem = TodoItem(
        id = id,
        title = title,
        note = note,
        placeName = placeName,
        latitude = latitude,
        longitude = longitude,
        radiusM = radiusM,
        deadlineUtc = deadlineUtc,
        deadlineZoneId = deadlineZoneId,
        priority = try { TodoPriority.valueOf(priority) } catch (_: Exception) { TodoPriority.NORMAL },
        status = try { TodoStatus.valueOf(status) } catch (_: Exception) { TodoStatus.OPEN },
        snoozedUntil = snoozedUntil,
        completedAt = completedAt,
        staleSince = staleSince,
        nudgeCount = nudgeCount,
        lastNudgeAt = lastNudgeAt,
        lastLocationAlertAt = lastLocationAlertAt,
        repeatRule = repeatRule,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(todo: TodoItem): TodoEntity = TodoEntity(
            id = todo.id,
            title = todo.title,
            note = todo.note,
            placeName = todo.placeName,
            latitude = todo.latitude,
            longitude = todo.longitude,
            radiusM = todo.radiusM,
            deadlineUtc = todo.deadlineUtc,
            deadlineZoneId = todo.deadlineZoneId,
            priority = todo.priority.name,
            status = todo.status.name,
            snoozedUntil = todo.snoozedUntil,
            completedAt = todo.completedAt,
            staleSince = todo.staleSince,
            nudgeCount = todo.nudgeCount,
            lastNudgeAt = todo.lastNudgeAt,
            lastLocationAlertAt = todo.lastLocationAlertAt,
            repeatRule = todo.repeatRule,
            createdAt = todo.createdAt
        )
    }
}
