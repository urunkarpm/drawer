package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType

@Entity(tableName = "app_groups")
data class AppGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "icon_name")
    val iconName: String = "folder",
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#6750A4",
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    @ColumnInfo(name = "is_expanded")
    val isExpanded: Boolean = true,
    @ColumnInfo(name = "view_type")
    val viewType: String = "GRID",
    @ColumnInfo(name = "column_count")
    val columnCount: Int = 4,
    @ColumnInfo(name = "sort_order")
    val sortOrder: String = "MANUAL"
) {
    fun toDomain(): AppGroup = AppGroup(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        orderIndex = orderIndex,
        isExpanded = isExpanded,
        viewType = try { GroupViewType.valueOf(viewType) } catch (_: Exception) { GroupViewType.GRID },
        columnCount = columnCount,
        sortOrder = try { GroupSortOrder.valueOf(sortOrder) } catch (_: Exception) { GroupSortOrder.MANUAL },
        items = emptyList()
    )

    companion object {
        fun fromDomain(group: AppGroup): AppGroupEntity = AppGroupEntity(
            id = group.id,
            name = group.name,
            iconName = group.iconName,
            colorHex = group.colorHex,
            orderIndex = group.orderIndex,
            isExpanded = group.isExpanded,
            viewType = group.viewType.name,
            columnCount = group.columnCount,
            sortOrder = group.sortOrder.name
        )
    }
}
