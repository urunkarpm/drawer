package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.DockItem

@Entity(tableName = "dock_items")
data class DockItemEntity(
    @PrimaryKey
    val position: Int, // 0 to 4
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "activity_name")
    val activityName: String,
    @ColumnInfo(name = "user_handle_id")
    val userHandleId: Int = 0,
    @ColumnInfo(name = "custom_label")
    val customLabel: String? = null
) {
    fun toDomain(): DockItem = DockItem(
        position = position,
        packageName = packageName,
        activityName = activityName,
        userHandleId = userHandleId,
        customLabel = customLabel
    )

    companion object {
        fun fromDomain(dockItem: DockItem): DockItemEntity = DockItemEntity(
            position = dockItem.position,
            packageName = dockItem.packageName,
            activityName = dockItem.activityName,
            userHandleId = dockItem.userHandleId,
            customLabel = dockItem.customLabel
        )
    }
}
