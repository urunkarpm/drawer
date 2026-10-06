package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.AppUpdateInfo

interface UpdateRepository {
    suspend fun checkForUpdates(currentVersionName: String): Result<AppUpdateInfo>
}
