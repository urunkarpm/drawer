package com.urunkarpm.drawer.core.data.ai

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppGroupItem
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppCategorizerTest {

    private val needleEngine: NeedleEngine = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { needleEngine.modelStatus } returns MutableStateFlow(ModelStatus.NotDownloaded)
    }

    @Test
    fun categorizeApps_mapsFinanceAndSocialCorrectly() = runTest {
        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(id = "finance_group", name = "Finance", orderIndex = 0),
            AppGroup(id = "social_group", name = "Social", orderIndex = 1)
        )

        val apps = listOf(
            AppInfo(packageName = "com.splitwise.app", activityName = ".Main", label = "Splitwise"),
            AppInfo(packageName = "org.telegram.messenger", activityName = ".Main", label = "Telegram")
        )

        val suggestions = categorizer.categorizeApps(apps, groups)
        val financeSuggestion = suggestions.firstOrNull { it.group.id == "finance_group" }
        val socialSuggestion = suggestions.firstOrNull { it.group.id == "social_group" }

        assertNotNull("Finance suggestion should not be null", financeSuggestion)
        assertTrue(financeSuggestion!!.suggestedApps.any { it.label == "Splitwise" })

        assertNotNull("Social suggestion should not be null", socialSuggestion)
        assertTrue(socialSuggestion!!.suggestedApps.any { it.label == "Telegram" })
    }

    @Test
    fun categorizeApps_mapsProductivityAndMediaCorrectly() = runTest {
        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(id = "group_work", name = "Work", orderIndex = 0),
            AppGroup(id = "group_media", name = "Media", orderIndex = 1)
        )

        val apps = listOf(
            AppInfo(packageName = "com.google.android.apps.docs", activityName = ".Main", label = "Google Docs"),
            AppInfo(packageName = "com.spotify.music", activityName = ".Main", label = "Spotify"),
            AppInfo(packageName = "org.videolan.vlc", activityName = ".Main", label = "VLC"),
            AppInfo(packageName = "com.slack", activityName = ".Main", label = "Slack")
        )

        val suggestions = categorizer.categorizeApps(apps, groups)
        val workSuggestion = suggestions.firstOrNull { it.group.id == "group_work" }
        val mediaSuggestion = suggestions.firstOrNull { it.group.id == "group_media" }

        assertNotNull("Work suggestion should not be null", workSuggestion)
        val workLabels = workSuggestion!!.suggestedApps.map { it.label }
        assertTrue(workLabels.contains("Google Docs"))
        assertTrue(workLabels.contains("Slack"))

        assertNotNull("Media suggestion should not be null", mediaSuggestion)
        val mediaLabels = mediaSuggestion!!.suggestedApps.map { it.label }
        assertTrue(mediaLabels.contains("Spotify"))
        assertTrue(mediaLabels.contains("VLC"))
    }

    @Test
    fun categorizeApps_respectsUnassignedOnlyFlag() = runTest {
        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(
                id = "finance_group",
                name = "Finance",
                orderIndex = 0,
                items = listOf(
                    AppGroupItem(
                        id = "item_1",
                        groupId = "finance_group",
                        packageName = "com.splitwise.app",
                        activityName = ".Main",
                        userHandleId = 0
                    )
                )
            ),
            AppGroup(id = "social_group", name = "Social", orderIndex = 1)
        )

        val apps = listOf(
            AppInfo(packageName = "com.splitwise.app", activityName = ".Main", label = "Splitwise"),
            AppInfo(packageName = "org.telegram.messenger", activityName = ".Main", label = "Telegram")
        )

        // When unassignedOnly = true (default), Splitwise should be skipped as it's already assigned
        val suggestionsUnassigned = categorizer.categorizeApps(apps, groups, unassignedOnly = true)
        val financeSuggestion = suggestionsUnassigned.firstOrNull { it.group.id == "finance_group" }
        val socialSuggestion = suggestionsUnassigned.firstOrNull { it.group.id == "social_group" }

        assertTrue(financeSuggestion == null || financeSuggestion.suggestedApps.none { it.packageName == "com.splitwise.app" })
        assertNotNull(socialSuggestion)
        assertTrue(socialSuggestion!!.suggestedApps.any { it.label == "Telegram" })

        // When unassignedOnly = false, Splitwise should be included
        val suggestionsAll = categorizer.categorizeApps(apps, groups, unassignedOnly = false)
        val financeSuggestionAll = suggestionsAll.firstOrNull { it.group.id == "finance_group" }
        assertNotNull(financeSuggestionAll)
        assertTrue(financeSuggestionAll!!.suggestedApps.any { it.packageName == "com.splitwise.app" })
    }

    @Test
    fun categorizeApps_workProfileApp_assignedToProductivity() = runTest {
        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(id = "group_work", name = "Productivity", orderIndex = 0)
        )

        val apps = listOf(
            AppInfo(
                packageName = "com.custom.internal.crm",
                activityName = ".Main",
                label = "Internal CRM",
                isWorkProfile = true
            )
        )

        val suggestions = categorizer.categorizeApps(apps, groups)
        val workSuggestion = suggestions.firstOrNull { it.group.id == "group_work" }
        assertNotNull(workSuggestion)
        assertTrue(workSuggestion!!.suggestedApps.any { it.label == "Internal CRM" })
    }

    @Test
    fun categorizeApps_whenNeedleEngineReady_usesModelPrediction() = runTest {
        every { needleEngine.modelStatus } returns MutableStateFlow(
            ModelStatus.Ready(modelPath = "/data/model.bin", sizeMb = 120.0f)
        )
        coEvery { needleEngine.predictToolCall(any(), any()) } returns listOf(
            ToolCallResult(
                toolName = "categorize_app",
                arguments = buildJsonObject {
                    put("category", "games")
                }
            )
        )

        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(id = "games_group", name = "Games", orderIndex = 0)
        )

        val apps = listOf(
            AppInfo(packageName = "com.obscure.title", activityName = ".Main", label = "Mystery Game")
        )

        val suggestions = categorizer.categorizeApps(apps, groups)
        val gamesSuggestion = suggestions.firstOrNull { it.group.id == "games_group" }
        assertNotNull(gamesSuggestion)
        assertTrue(gamesSuggestion!!.suggestedApps.any { it.label == "Mystery Game" })
    }

    @Test
    fun categorizeApps_mapsUtilitiesGamesHealthShoppingAndFuzzyGroups() = runTest {
        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(id = "group_money", name = "Money", orderIndex = 0),
            AppGroup(id = "group_chat", name = "Chat", orderIndex = 1),
            AppGroup(id = "group_deep_work", name = "Deep Work", orderIndex = 2),
            AppGroup(id = "group_tools", name = "Tools", orderIndex = 3),
            AppGroup(id = "group_play", name = "Play", orderIndex = 4),
            AppGroup(id = "group_fitness", name = "Fitness", orderIndex = 5),
            AppGroup(id = "group_orders", name = "Orders", orderIndex = 6)
        )

        val apps = listOf(
            AppInfo(packageName = "com.google.android.apps.walletnfcrel", activityName = ".Main", label = "Google Pay"),
            AppInfo(packageName = "com.whatsapp", activityName = ".Main", label = "WhatsApp"),
            AppInfo(packageName = "notion.id", activityName = ".Main", label = "Notion"),
            AppInfo(packageName = "com.google.android.calculator", activityName = ".Main", label = "Calculator"),
            AppInfo(packageName = "com.mojang.minecraftpe", activityName = ".Main", label = "Minecraft"),
            AppInfo(packageName = "com.strava", activityName = ".Main", label = "Strava"),
            AppInfo(packageName = "in.amazon.mShop.android.shopping", activityName = ".Main", label = "Amazon")
        )

        val suggestions = categorizer.categorizeApps(apps, groups)

        val moneySuggestion = suggestions.firstOrNull { it.group.id == "group_money" }
        assertNotNull(moneySuggestion)
        assertTrue(moneySuggestion!!.suggestedApps.any { it.label == "Google Pay" })

        val chatSuggestion = suggestions.firstOrNull { it.group.id == "group_chat" }
        assertNotNull(chatSuggestion)
        assertTrue(chatSuggestion!!.suggestedApps.any { it.label == "WhatsApp" })

        val workSuggestion = suggestions.firstOrNull { it.group.id == "group_deep_work" }
        assertNotNull(workSuggestion)
        assertTrue(workSuggestion!!.suggestedApps.any { it.label == "Notion" })

        val toolsSuggestion = suggestions.firstOrNull { it.group.id == "group_tools" }
        assertNotNull(toolsSuggestion)
        assertTrue(toolsSuggestion!!.suggestedApps.any { it.label == "Calculator" })

        val playSuggestion = suggestions.firstOrNull { it.group.id == "group_play" }
        assertNotNull(playSuggestion)
        assertTrue(playSuggestion!!.suggestedApps.any { it.label == "Minecraft" })

        val fitnessSuggestion = suggestions.firstOrNull { it.group.id == "group_fitness" }
        assertNotNull(fitnessSuggestion)
        assertTrue(fitnessSuggestion!!.suggestedApps.any { it.label == "Strava" })

        val ordersSuggestion = suggestions.firstOrNull { it.group.id == "group_orders" }
        assertNotNull(ordersSuggestion)
        assertTrue(ordersSuggestion!!.suggestedApps.any { it.label == "Amazon" })
    }
}
