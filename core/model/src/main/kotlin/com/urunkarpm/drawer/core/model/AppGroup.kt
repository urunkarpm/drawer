package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class GroupViewType {
    GRID, LIST
}

@Serializable
enum class GroupSortOrder {
    MANUAL, ALPHABETICAL, USAGE
}

@Serializable
data class AppGroup(
    val id: String,
    val name: String,
    val iconName: String = "folder",
    val colorHex: String = "#6750A4",
    val orderIndex: Int = 0,
    val isExpanded: Boolean = true,
    val viewType: GroupViewType = GroupViewType.GRID,
    val columnCount: Int = 4,
    val sortOrder: GroupSortOrder = GroupSortOrder.MANUAL,
    val items: List<AppGroupItem> = emptyList()
)

@Serializable
data class AppGroupItem(
    val id: String,
    val groupId: String,
    val packageName: String,
    val activityName: String,
    val userHandleId: Int = 0,
    val orderIndex: Int = 0,
    val customLabel: String? = null
)
