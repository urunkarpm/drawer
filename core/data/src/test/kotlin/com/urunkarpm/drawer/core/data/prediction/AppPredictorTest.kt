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

    private fun createApp(packageName: String, label: String): AppInfo {
        return AppInfo(
            packageName = packageName,
            activityName = "MainActivity",
            label = label
        )
    }

    @Before
    fun setup() {
        predictor = AppPredictor()
    }

    @Test
    fun predictTopApps_excludesDockedApps() {
        val app1 = createApp("com.example.phone", "Phone")
        val app2 = createApp("com.example.browser", "Browser")
        val app3 = createApp("com.example.notes", "Notes")

        val profiles = mapOf(
            app1.componentKey to AppUsageProfile(totalScore = 10f),
            app2.componentKey to AppUsageProfile(totalScore = 8f),
            app3.componentKey to AppUsageProfile(totalScore = 5f)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(app1, app2, app3),
            usageProfiles = profiles,
            dockedPackages = setOf("com.example.phone"),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet()
        )

        assertFalse(result.any { it.packageName == "com.example.phone" })
        assertTrue(result.any { it.packageName == "com.example.browser" })
    }

    @Test
    fun predictTopApps_boostsActiveNotifications() {
        val app1 = createApp("com.example.chat", "Chat")
        val app2 = createApp("com.example.game", "Game")

        // Chat has slightly lower base score, but active notification boosts it over Game
        val profiles = mapOf(
            app1.componentKey to AppUsageProfile(totalScore = 5f),
            app2.componentKey to AppUsageProfile(totalScore = 6f)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(app1, app2),
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = setOf("com.example.chat")
        )

        assertEquals("com.example.chat", result.first().packageName)
    }

    @Test
    fun predictTopApps_favorsWeekdayMorningHabits() {
        val workApp = createApp("com.work.slack", "Slack")
        val leisureApp = createApp("com.fun.netflix", "Netflix")

        // Work app launched exclusively weekday morning (slot 0)
        // Leisure app launched exclusively weekend evening (slot 2)
        val profiles = mapOf(
            workApp.componentKey to AppUsageProfile(
                totalScore = 10f,
                weekdaySlots = listOf(20, 0, 0, 0)
            ),
            leisureApp.componentKey to AppUsageProfile(
                totalScore = 10f,
                weekendSlots = listOf(0, 0, 20, 0)
            )
        )

        // Tuesday 09:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 27, 9, 0, 0)
        }

        val result = predictor.predictTopApps(
            installedApps = listOf(workApp, leisureApp),
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet(),
            currentTimeMs = cal.timeInMillis
        )

        assertEquals("com.work.slack", result.first().packageName)
    }
}
