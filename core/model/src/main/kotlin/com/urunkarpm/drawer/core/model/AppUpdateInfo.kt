package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateInfo(
    val latestVersionTag: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val htmlUrl: String,
    val isUpdateAvailable: Boolean,
    val publishedAt: String = ""
)
