package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widgets")
data class WidgetItemEntity(
    @PrimaryKey
    val id: String, // UUID
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "provider_class_name")
    val providerClassName: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "height_dp")
    val heightDp: Int = 180
) {
    fun toDomain(): com.urunkarpm.drawer.core.model.WidgetItem =
        com.urunkarpm.drawer.core.model.WidgetItem(
            id = id,
            appWidgetId = appWidgetId,
            packageName = packageName,
            providerClassName = providerClassName,
            orderIndex = orderIndex,
            heightDp = heightDp
        )

    companion object {
        fun fromDomain(item: com.urunkarpm.drawer.core.model.WidgetItem): WidgetItemEntity =
            WidgetItemEntity(
                id = item.id,
                appWidgetId = item.appWidgetId,
                packageName = item.packageName,
                providerClassName = item.providerClassName,
                orderIndex = item.orderIndex,
                heightDp = item.heightDp
            )
    }
}
