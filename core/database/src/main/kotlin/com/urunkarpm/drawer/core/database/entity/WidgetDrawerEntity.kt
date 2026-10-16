package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.WidgetDrawer
import com.urunkarpm.drawer.core.model.WidgetItem

@Entity(tableName = "widget_drawers")
data class WidgetDrawerEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "span_x")
    val spanX: Int = 4,
    @ColumnInfo(name = "span_y")
    val spanY: Int = 3,
    @ColumnInfo(name = "is_expanded")
    val isExpanded: Boolean = true,
    @ColumnInfo(name = "icon_name")
    val iconName: String = "folder",
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#1E88E5"
) {
    fun toDomain(widgets: List<WidgetItem> = emptyList()): WidgetDrawer =
        WidgetDrawer(
            id = id,
            title = title,
            orderIndex = orderIndex,
            spanX = spanX,
            spanY = spanY,
            isExpanded = isExpanded,
            iconName = iconName,
            colorHex = colorHex,
            widgets = widgets
        )

    companion object {
        fun fromDomain(drawer: WidgetDrawer): WidgetDrawerEntity =
            WidgetDrawerEntity(
                id = drawer.id,
                title = drawer.title,
                orderIndex = drawer.orderIndex,
                spanX = drawer.spanX,
                spanY = drawer.spanY,
                isExpanded = drawer.isExpanded,
                iconName = drawer.iconName,
                colorHex = drawer.colorHex
            )
    }
}
