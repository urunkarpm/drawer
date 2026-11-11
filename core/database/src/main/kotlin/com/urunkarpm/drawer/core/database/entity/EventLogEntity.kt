package com.urunkarpm.drawer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.EventLog

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val time: Long = System.currentTimeMillis(),
    val type: String,
    val text: String
) {
    fun toDomain(): EventLog = EventLog(
        id = id,
        time = time,
        type = type,
        text = text
    )

    companion object {
        fun fromDomain(log: EventLog): EventLogEntity = EventLogEntity(
            id = log.id,
            time = log.time,
            type = log.type,
            text = log.text
        )
    }
}
