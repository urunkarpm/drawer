package com.urunkarpm.drawer.core.data.ai

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo

data class CategorizationSuggestion(
    val group: AppGroup,
    val suggestedApps: List<AppInfo>
)

interface AppCategorizer {
    suspend fun categorizeApps(
        apps: List<AppInfo>,
        existingGroups: List<AppGroup>,
        unassignedOnly: Boolean = true
    ): List<CategorizationSuggestion>
}
