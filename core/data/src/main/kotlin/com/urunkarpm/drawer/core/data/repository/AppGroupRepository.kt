package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import kotlinx.coroutines.flow.Flow

interface AppGroupRepository {
    val groups: Flow<List<AppGroup>>

    suspend fun createGroup(
        name: String,
        iconName: String = "folder",
        colorHex: String = "#6750A4",
        viewType: GroupViewType = GroupViewType.GRID,
        columnCount: Int = 4,
        sortOrder: GroupSortOrder = GroupSortOrder.MANUAL
    ): AppGroup

    suspend fun updateGroup(group: AppGroup)
    suspend fun deleteGroup(groupId: String)
    suspend fun toggleGroupExpanded(groupId: String)
    suspend fun reorderGroups(fromIndex: Int, toIndex: Int)

    suspend fun assignAppToGroup(
        groupId: String,
        app: AppInfo,
        allowMultiGroup: Boolean = false,
        targetIndex: Int? = null
    )

    suspend fun moveAppBetweenGroups(
        sourceGroupId: String,
        targetGroupId: String,
        app: AppInfo,
        targetIndex: Int? = null
    )

    suspend fun removeAppFromGroup(groupId: String, packageName: String, activityName: String)
    suspend fun removeGroupItemById(itemId: String)
    suspend fun collapseAllGroups()
    suspend fun autoPopulateGroups()
}
