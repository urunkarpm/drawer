package com.urunkarpm.drawer.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.urunkarpm.drawer.core.model.AppGroupItem

@Entity(
    tableName = "app_group_items",
    foreignKeys = [
        ForeignKey(
            entity = AppGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["group_id"])]
)
data class AppGroupItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "group_id")
    val groupId: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "activity_name")
    val activityName: String,
    @ColumnInfo(name = "user_handle_id")
    val userHandleId: Int = 0,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    @ColumnInfo(name = "custom_label")
    val customLabel: String? = null
) {
    fun toDomain(): AppGroupItem = AppGroupItem(
        id = id,
        groupId = groupId,
        packageName = packageName,
        activityName = activityName,
        userHandleId = userHandleId,
        orderIndex = orderIndex,
        customLabel = customLabel
    )

    companion object {
        fun fromDomain(item: AppGroupItem): AppGroupItemEntity = AppGroupItemEntity(
            id = item.id,
            groupId = item.groupId,
            packageName = item.packageName,
            activityName = item.activityName,
            userHandleId = item.userHandleId,
            orderIndex = item.orderIndex,
            customLabel = item.customLabel
        )
    }
}
