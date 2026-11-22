package com.urunkarpm.drawer.feature.home.component

import com.urunkarpm.drawer.core.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenVaultDrawerTest {

    @Test
    fun filterNonHiddenAppsForVault_deduplicatesMultiActivityPackages() {
        // Multi-activity package (e.g. Google Pixel system app with SearchActivity & VoiceSearchActivity)
        val pixelSearchMain = AppInfo(
            packageName = "com.google.android.googlequicksearchbox",
            activityName = "com.google.android.googlequicksearchbox.SearchActivity",
            label = "Google",
            userHandleId = 0,
            isWorkProfile = false
        )
        val pixelSearchVoice = AppInfo(
            packageName = "com.google.android.googlequicksearchbox",
            activityName = "com.google.android.googlequicksearchbox.VoiceSearchActivity",
            label = "Voice Search",
            userHandleId = 0,
            isWorkProfile = false
        )
        val chrome = AppInfo(
            packageName = "com.android.chrome",
            activityName = "com.google.android.apps.chrome.Main",
            label = "Chrome",
            userHandleId = 0,
            isWorkProfile = false
        )

        val allApps = listOf(pixelSearchMain, pixelSearchVoice, chrome)
        val hiddenApps = emptyList<AppInfo>()

        val result = filterNonHiddenAppsForVault(allApps, hiddenApps)

        // Must contain exactly 2 unique packages (Google and Chrome), not 3 items
        assertEquals(2, result.size)
        val packageCounts = result.groupingBy { it.packageName }.eachCount()
        assertEquals(1, packageCounts["com.google.android.googlequicksearchbox"])
        assertEquals(1, packageCounts["com.android.chrome"])

        // Guaranteed unique keys for Compose LazyColumn
        val componentKeys = result.map { it.componentKey }.toSet()
        assertEquals(result.size, componentKeys.size)
    }

    @Test
    fun filterNonHiddenAppsForVault_excludesAlreadyHiddenPackages() {
        val chrome = AppInfo(
            packageName = "com.android.chrome",
            activityName = "com.google.android.apps.chrome.Main",
            label = "Chrome"
        )
        val youtube = AppInfo(
            packageName = "com.google.android.youtube",
            activityName = "com.google.android.youtube.app.honeycomb.Shell\$HomeActivity",
            label = "YouTube"
        )

        val allApps = listOf(chrome, youtube)
        val hiddenApps = listOf(youtube)

        val result = filterNonHiddenAppsForVault(allApps, hiddenApps)

        assertEquals(1, result.size)
        assertEquals("com.android.chrome", result.first().packageName)
    }

    @Test
    fun filterNonHiddenAppsForVault_prioritizesPersonalProfileOverWorkProfile() {
        val workSlack = AppInfo(
            packageName = "com.Slack",
            activityName = "com.slack.ui.MainActivity",
            label = "Slack",
            userHandleId = 10,
            isWorkProfile = true
        )
        val personalSlack = AppInfo(
            packageName = "com.Slack",
            activityName = "com.slack.ui.MainActivity",
            label = "Slack",
            userHandleId = 0,
            isWorkProfile = false
        )

        val allApps = listOf(workSlack, personalSlack)
        val result = filterNonHiddenAppsForVault(allApps, emptyList())

        assertEquals(1, result.size)
        assertFalse(result.first().isWorkProfile)
        assertEquals(0, result.first().userHandleId)
    }

    @Test
    fun filterNonHiddenAppsForVault_sortsAlphabeticallyByLabel() {
        val zebra = AppInfo(packageName = "com.zebra", activityName = "Act", label = "Zebra")
        val alpha = AppInfo(packageName = "com.alpha", activityName = "Act", label = "Alpha")
        val beta = AppInfo(packageName = "com.beta", activityName = "Act", label = "Beta")

        val result = filterNonHiddenAppsForVault(listOf(zebra, alpha, beta), emptyList())

        assertEquals(listOf("Alpha", "Beta", "Zebra"), result.map { it.label })
    }

    @Test
    fun componentKeys_areUniqueForMultiActivityAndWorkProfileApps() {
        val app1 = AppInfo("com.google.android.googlequicksearchbox", "Act1", "Google", userHandleId = 0)
        val app2 = AppInfo("com.google.android.googlequicksearchbox", "Act2", "Voice", userHandleId = 0)
        val app3 = AppInfo("com.google.android.googlequicksearchbox", "Act1", "Google", userHandleId = 10, isWorkProfile = true)

        val keys = listOf(app1, app2, app3).map { it.componentKey }.toSet()
        assertEquals(3, keys.size)
    }
}
