package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.WidgetItem

@Entity(
    tableName = "widgets",
    indices = [Index(value = ["drawer_id"])]
)
data class WidgetItemEntity(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "provider_class_name")
    val providerClassName: String,
    @ColumnInfo(name = "drawer_id")
    val drawerId: String? = null,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    @ColumnInfo(name = "span_x")
    val spanX: Int = 4,
    @ColumnInfo(name = "span_y")
    val spanY: Int = 2,
    @ColumnInfo(name = "height_dp")
    val heightDp: Int = 180
) {
    fun toDomain(): WidgetItem =
        WidgetItem(
            id = id,
            appWidgetId = appWidgetId,
            packageName = packageName,
            providerClassName = providerClassName,
            drawerId = drawerId,
            orderIndex = orderIndex,
            spanX = spanX,
            spanY = spanY,
            heightDp = heightDp
        )

    companion object {
        fun fromDomain(item: WidgetItem): WidgetItemEntity =
            WidgetItemEntity(
                id = item.id,
                appWidgetId = item.appWidgetId,
                packageName = item.packageName,
                providerClassName = item.providerClassName,
                drawerId = item.drawerId,
                orderIndex = item.orderIndex,
                spanX = item.spanX,
                spanY = item.spanY,
                heightDp = item.heightDp
            )
    }
}
