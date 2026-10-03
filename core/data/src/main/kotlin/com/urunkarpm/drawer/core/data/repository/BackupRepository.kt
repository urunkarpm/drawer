package com.urunkarpm.drawer.core.data.repository

interface BackupRepository {
    suspend fun createBackupJson(): String
    suspend fun restoreBackupJson(jsonString: String): Result<Unit>
}
