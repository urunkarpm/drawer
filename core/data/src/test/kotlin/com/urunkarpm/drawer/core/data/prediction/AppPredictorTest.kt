package com.urunkarpm.drawer.core.data.prediction

import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppUsageProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AppPredictorTest {
    private lateinit var predictor: AppPredictor

    private fun createApp(
        packageName: String,
        label: String,
        installTimeMillis: Long = 0L
    ): AppInfo {
        return AppInfo(
            packageName = packageName,
            activityName = "MainActivity",
            label = label,
            installTimeMillis = installTimeMillis
        )
    }

    @Before
    fun setup() {
        predictor = AppPredictor()
    }

    @Test
    fun predictTopApps_excludesDockedApps() {
        val now = System.currentTimeMillis()
        val app1 = createApp("com.example.phone", "Phone")
        val app2 = createApp("com.example.browser", "Browser")
        val app3 = createApp("com.example.notes", "Notes")

        val profiles = mapOf(
            app1.componentKey to AppUsageProfile(totalScore = 10f, lastLaunchTime = now),
            app2.componentKey to AppUsageProfile(totalScore = 8f, lastLaunchTime = now),
            app3.componentKey to AppUsageProfile(totalScore = 5f, lastLaunchTime = now)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(app1, app2, app3),
            usageProfiles = profiles,
            dockedPackages = setOf("com.example.phone"),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet(),
            currentTimeMs = now
        )

        assertFalse(result.any { it.packageName == "com.example.phone" })
        assertTrue(result.any { it.packageName == "com.example.browser" })
    }

    @Test
    fun predictTopApps_boostsActiveNotifications() {
        val now = System.currentTimeMillis()
        val app1 = createApp("com.example.chat", "Chat")
        val app2 = createApp("com.example.game", "Game")

        // Chat has slightly lower base score, but active notification (1.3x) boosts it over Game
        // sqrt(5) * 10 * 1.30 = 29.07 > sqrt(6) * 10 = 24.49
        val profiles = mapOf(
            app1.componentKey to AppUsageProfile(totalScore = 5f, lastLaunchTime = now),
            app2.componentKey to AppUsageProfile(totalScore = 6f, lastLaunchTime = now)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(app2, app1), // Pass app2 first to verify score ordering over insertion order
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = setOf("com.example.chat"),
            currentTimeMs = now
        )

        assertEquals("com.example.chat", result.first().packageName)
    }

    @Test
    fun predictTopApps_favorsWeekdayMorningHabits() {
        // Tuesday 09:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 27, 9, 0, 0)
        }
        val targetTimeMs = cal.timeInMillis

        val workApp = createApp("com.work.slack", "Slack")
        val leisureApp = createApp("com.fun.netflix", "Netflix")

        // Work app launched exclusively weekday morning (slot 0)
        // Leisure app launched exclusively weekend evening (slot 2)
        val profiles = mapOf(
            workApp.componentKey to AppUsageProfile(
                totalScore = 10f,
                lastLaunchTime = targetTimeMs,
                weekdaySlots = listOf(20, 0, 0, 0)
            ),
            leisureApp.componentKey to AppUsageProfile(
                totalScore = 10f,
                lastLaunchTime = targetTimeMs,
                weekendSlots = listOf(0, 0, 20, 0)
            )
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(leisureApp, workApp), // leisureApp first to prove score ordering wins
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet(),
            currentTimeMs = targetTimeMs
        )

        assertEquals("com.work.slack", result.first().packageName)
    }

    @Test
    fun predictTopApps_respectsBlacklist() {
        val now = System.currentTimeMillis()
        val secretApp = createApp("com.example.secret", "Secret")
        val notesApp = createApp("com.example.notes", "Notes")

        val profiles = mapOf(
            secretApp.componentKey to AppUsageProfile(totalScore = 20f, lastLaunchTime = now),
            notesApp.componentKey to AppUsageProfile(totalScore = 5f, lastLaunchTime = now)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(secretApp, notesApp),
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = setOf("com.example.secret"),
            activeNotificationPackages = emptySet(),
            currentTimeMs = now
        )

        assertFalse(result.any { it.packageName == "com.example.secret" })
        assertTrue(result.any { it.packageName == "com.example.notes" })
    }

    @Test
    fun predictTopApps_fallsBackGracefully() {
        val dockedDialer = createApp("com.android.dialer", "Phone", installTimeMillis = 1000L)
        val browser = createApp("com.android.chrome", "Chrome", installTimeMillis = 2000L)
        val notes = createApp("com.example.notes", "Notes", installTimeMillis = 3000L)

        val result = predictor.predictTopApps(
            installedApps = listOf(dockedDialer, notes, browser),
            usageProfiles = emptyMap(),
            dockedPackages = setOf("com.android.dialer"),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet(),
            maxSuggestions = 2
        )

        // Excludes docked dialer
        assertFalse(result.any { it.packageName == "com.android.dialer" })
        // Chrome has priority keyword 'chrome'/'browser', so ranked ahead of notes
        assertEquals("com.android.chrome", result.first().packageName)
        assertEquals(2, result.size)
    }

    @Test
    fun predictTopApps_boostsCompanionSequence() {
        val now = System.currentTimeMillis()
        val galleryApp = createApp("com.example.gallery", "Gallery")
        val gameApp = createApp("com.example.game", "Game")

        // Game has slightly higher base score (11f vs 10f)
        // sqrt(10) * 10 * 1.25 companion boost = 39.52 > sqrt(11) * 10 = 33.17
        val profiles = mapOf(
            galleryApp.componentKey to AppUsageProfile(totalScore = 10f, lastLaunchTime = now),
            gameApp.componentKey to AppUsageProfile(totalScore = 11f, lastLaunchTime = now)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(gameApp, galleryApp), // Pass gameApp first to prove boost overrides order
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet(),
            lastExitedPackage = "com.android.camera",
            currentTimeMs = now
        )

        assertEquals("com.example.gallery", result.first().packageName)
    }
}
