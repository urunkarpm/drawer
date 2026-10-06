package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.model.AppUpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("published_at")
    val publishedAt: String = "",
    val assets: List<GitHubAssetDto> = emptyList(),
    val prerelease: Boolean = false,
    val draft: Boolean = false
)

@Serializable
private data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
    @SerialName("content_type")
    val contentType: String = ""
)

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : UpdateRepository {

    override suspend fun checkForUpdates(currentVersionName: String): Result<AppUpdateInfo> = withContext(ioDispatcher) {
        runCatching {
            val response: GitHubReleaseDto = httpClient.get("https://api.github.com/repos/urunkarpm/drawer/releases/latest") {
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "Drawer-Launcher-Android")
            }.body()

            val latestTag = response.tagName.trim()
            val cleanLatest = latestTag.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVersionName.removePrefix("v").removePrefix("V").trim()

            // Compare versions
            val isNewer = isVersionNewer(cleanLatest, cleanCurrent)

            // Look for .apk asset
            val apkAsset = response.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkAsset?.browserDownloadUrl ?: response.htmlUrl

            AppUpdateInfo(
                latestVersionTag = latestTag,
                releaseName = response.name ?: latestTag,
                releaseNotes = response.body ?: "No release notes provided.",
                apkDownloadUrl = downloadUrl,
                htmlUrl = response.htmlUrl,
                isUpdateAvailable = isNewer,
                publishedAt = response.publishedAt
            )
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        if (current.isBlank()) return true
        if (latest == current) return false

        val latestParts = latest.split('.').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
