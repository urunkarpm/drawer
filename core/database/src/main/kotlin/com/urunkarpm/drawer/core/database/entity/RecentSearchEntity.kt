package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.RecentSearch

@Entity(tableName = "recent_search")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): RecentSearch = RecentSearch(
        query = query,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(search: RecentSearch): RecentSearchEntity = RecentSearchEntity(
            query = search.query,
            createdAt = search.createdAt
        )
    }
}
