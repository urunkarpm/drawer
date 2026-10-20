package com.urunkarpm.drawer.core.needle.engine

import com.urunkarpm.drawer.core.needle.action.StandardTools
import com.urunkarpm.drawer.core.needle.download.NeedleModelManager
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CactusNeedleEngineTest {

    @Test
    fun testPredictToolCallWorksWithFastPathWhenNotDownloaded() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val results = engine.predictToolCall("timer 10 minutes", StandardTools.allTools)

        assertEquals(1, results.size)
        assertEquals("set_timer", results.first().toolName)
    }

    @Test
    fun testPredictToolCallAmbiguousReturnsEmptyWhenNotDownloaded() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val results = engine.predictToolCall("can you help me organize my life", StandardTools.allTools)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testPredictToolCallReturnsToolWhenReady() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.Ready("/models/needle.bin", 10.3f))
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val results = engine.predictToolCall("timer 10 minutes", StandardTools.allTools)

        assertEquals(1, results.size)
        val toolCall = results.first()
        assertEquals("set_timer", toolCall.toolName)
        assertEquals(600, toolCall.arguments["seconds"]?.jsonPrimitive?.int)
    }

    @Test
    fun testPredictToolCallAlarmWhenReady() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.Ready("/models/needle.bin", 10.3f))
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val results = engine.predictToolCall("set alarm for 6:30 am", StandardTools.allTools)

        assertEquals(1, results.size)
        val toolCall = results.first()
        assertEquals("set_alarm", toolCall.toolName)
        assertEquals(6, toolCall.arguments["hour"]?.jsonPrimitive?.int)
        assertEquals(30, toolCall.arguments["minute"]?.jsonPrimitive?.int)
    }

    @Test
    fun testPredictToolCallReturnsEmptyWhenNoMatch() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.Ready("/models/needle.bin", 10.3f))
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val results = engine.predictToolCall("what is the weather today", StandardTools.allTools)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testGetEmbeddingWhenReady() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.Ready("/models/needle.bin", 10.3f))
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val embedding = engine.getEmbedding("test query")

        assertNotNull(embedding)
        assertEquals(768, embedding?.size)
    }

    @Test
    fun testGetEmbeddingWhenNotDownloaded() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val embedding = engine.getEmbedding("test query")

        assertNull(embedding)
    }

    @Test
    fun testDownloadAndDeleteModelDelegateToManager() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
        every { modelManager.status } returns statusFlow
        coEvery { modelManager.downloadModel(any()) } returns Result.success(Unit)
        every { modelManager.deleteModel() } returns true

        val engine = CactusNeedleEngine(modelManager)
        val downloadResult = engine.downloadModel()
        assertTrue(downloadResult.isSuccess)

        val deleteResult = engine.deleteModel()
        assertTrue(deleteResult)
    }

    @Test
    fun testPredictToolCallCategorizeApp() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.Ready("/models/needle.bin", 10.3f))
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val categorizeTool = com.urunkarpm.drawer.core.needle.model.ToolDefinition(
            name = "categorize_app",
            description = "Categorize an Android application",
            parameters = kotlinx.serialization.json.buildJsonObject {}
        )

        val results = engine.predictToolCall("Categorize application: Minecraft (com.mojang.minecraftpe)", listOf(categorizeTool))
        assertEquals(1, results.size)
        assertEquals("categorize_app", results.first().toolName)
        assertEquals("games", results.first().arguments["category"]?.jsonPrimitive?.content)
    }

    @Test
    fun testPredictToolCallDrawerAndDockActions() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.Ready("/models/needle.bin", 10.3f))
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)

        // 1. Add to drawer
        val addResults = engine.predictToolCall("put instagram in Social drawer", StandardTools.allTools)
        assertEquals(1, addResults.size)
        assertEquals("add_app_to_drawer", addResults[0].toolName)
        assertEquals("instagram", addResults[0].getString("app"))
        assertEquals("Social", addResults[0].getString("drawer"))

        // 2. Remove from drawer
        val removeResults = engine.predictToolCall("remove slack from Work drawer", StandardTools.allTools)
        assertEquals(1, removeResults.size)
        assertEquals("remove_app_from_drawer", removeResults[0].toolName)
        assertEquals("slack", removeResults[0].getString("app"))
        assertEquals("Work", removeResults[0].getString("drawer"))

        // 3. Move between drawers
        val moveResults = engine.predictToolCall("move spotify from Tools to Media drawer", StandardTools.allTools)
        assertEquals(1, moveResults.size)
        assertEquals("move_app_between_drawers", moveResults[0].toolName)
        assertEquals("spotify", moveResults[0].getString("app"))
        assertEquals("Tools", moveResults[0].getString("from_drawer"))
        assertEquals("Media", moveResults[0].getString("to_drawer"))

        // 4. Add to dock
        val addDockResults = engine.predictToolCall("add Telegram to the dock", StandardTools.allTools)
        assertEquals(1, addDockResults.size)
        assertEquals("add_app_to_dock", addDockResults[0].toolName)
        assertEquals("Telegram", addDockResults[0].getString("app"))

        // 5. Remove from dock
        val removeDockResults = engine.predictToolCall("remove WhatsApp from dock", StandardTools.allTools)
        assertEquals(1, removeDockResults.size)
        assertEquals("remove_app_from_dock", removeDockResults[0].toolName)
        assertEquals("WhatsApp", removeDockResults[0].getString("app"))

        // 6. Compound chained commands
        val chainedResults = engine.predictToolCall("remove WhatsApp from dock and add Telegram to the dock", StandardTools.allTools)
        assertEquals(2, chainedResults.size)
        assertEquals("remove_app_from_dock", chainedResults[0].toolName)
        assertEquals("WhatsApp", chainedResults[0].getString("app"))
        assertEquals("add_app_to_dock", chainedResults[1].toolName)
        assertEquals("Telegram", chainedResults[1].getString("app"))

        // 7. Verify Instant Intent Router state on fast-path execution
        assertEquals("Instant Intent Router", engine.processingState.value.modelName)
        assertEquals(com.urunkarpm.drawer.core.needle.model.ProcessingStep.Executing, engine.processingState.value.step)

        // 8. Natural phrasing and reverse syntax
        val reverseResults = engine.predictToolCall("please move spotify to Media drawer from Tools drawer", StandardTools.allTools)
        assertEquals(1, reverseResults.size)
        assertEquals("move_app_between_drawers", reverseResults[0].toolName)
        assertEquals("spotify", reverseResults[0].getString("app"))
        assertEquals("Tools", reverseResults[0].getString("from_drawer"))
        assertEquals("Media", reverseResults[0].getString("to_drawer"))
        assertEquals("Instant Intent Router", engine.processingState.value.modelName)

        // 9. Spoken words timer (e.g. "thirty seconds", "half an hour", compound "1 minute and 30 seconds")
        val spokenTimerResults = engine.predictToolCall("set a timer for thirty seconds", StandardTools.allTools)
        assertEquals(1, spokenTimerResults.size)
        assertEquals("set_timer", spokenTimerResults[0].toolName)
        assertEquals(30, spokenTimerResults[0].arguments["seconds"]?.jsonPrimitive?.int)

        val compoundTimerResults = engine.predictToolCall("set timer for 1 minute and 30 seconds", StandardTools.allTools)
        assertEquals(1, compoundTimerResults.size)
        assertEquals("set_timer", compoundTimerResults[0].toolName)
        assertEquals(90, compoundTimerResults[0].arguments["seconds"]?.jsonPrimitive?.int)

        val spokenAlarmResults = engine.predictToolCall("set alarm for seven thirty am", StandardTools.allTools)
        assertEquals(1, spokenAlarmResults.size)
        assertEquals("set_alarm", spokenAlarmResults[0].toolName)
        assertEquals(7, spokenAlarmResults[0].arguments["hour"]?.jsonPrimitive?.int)
        assertEquals(30, spokenAlarmResults[0].arguments["minute"]?.jsonPrimitive?.int)

        // 10. Folder CRUD operations
        val createFolderResults = engine.predictToolCall("create folder Work", StandardTools.allTools)
        assertEquals(1, createFolderResults.size)
        assertEquals("create_drawer", createFolderResults[0].toolName)
        assertEquals("Work", createFolderResults[0].getString("drawer"))

        val deleteFolderResults = engine.predictToolCall("delete Social folder", StandardTools.allTools)
        assertEquals(1, deleteFolderResults.size)
        assertEquals("delete_drawer", deleteFolderResults[0].toolName)
        assertEquals("Social", deleteFolderResults[0].getString("drawer"))

        val renameFolderResults = engine.predictToolCall("rename folder Work to Office", StandardTools.allTools)
        assertEquals(1, renameFolderResults.size)
        assertEquals("rename_drawer", renameFolderResults[0].toolName)
        assertEquals("Work", renameFolderResults[0].getString("from_name"))
        assertEquals("Office", renameFolderResults[0].getString("to_name"))

        val addAppFolderResults = engine.predictToolCall("put app WhatsApp in Chat folder", StandardTools.allTools)
        assertEquals(1, addAppFolderResults.size)
        assertEquals("add_app_to_drawer", addAppFolderResults[0].toolName)
        assertEquals("WhatsApp", addAppFolderResults[0].getString("app"))
        assertEquals("Chat", addAppFolderResults[0].getString("drawer"))

        val moveAppFolderResults = engine.predictToolCall("move Spotify from Tools folder to Media folder", StandardTools.allTools)
        assertEquals(1, moveAppFolderResults.size)
        assertEquals("move_app_between_drawers", moveAppFolderResults[0].toolName)
        assertEquals("Spotify", moveAppFolderResults[0].getString("app"))
        assertEquals("Tools", moveAppFolderResults[0].getString("from_drawer"))
        assertEquals("Media", moveAppFolderResults[0].getString("to_drawer"))

        val removeAppFolderResults = engine.predictToolCall("remove Slack from Work folder", StandardTools.allTools)
        assertEquals(1, removeAppFolderResults.size)
        assertEquals("remove_app_from_drawer", removeAppFolderResults[0].toolName)
        assertEquals("Slack", removeAppFolderResults[0].getString("app"))
        assertEquals("Work", removeAppFolderResults[0].getString("drawer"))

        val removeChatGptResults = engine.predictToolCall("remove chatgpt from social folder.", StandardTools.allTools)
        assertEquals(1, removeChatGptResults.size)
        assertEquals("remove_app_from_drawer", removeChatGptResults[0].toolName)
        assertEquals("chatgpt", removeChatGptResults[0].getString("app"))
        assertEquals("social", removeChatGptResults[0].getString("drawer"))

        // 11. Mathematical calculations
        val calcResults = engine.predictToolCall("calculate 25 * 4", StandardTools.allTools)
        assertEquals(1, calcResults.size)
        assertEquals("calculate", calcResults[0].toolName)
        assertEquals("100", calcResults[0].getString("result"))

        val nlCalcResults = engine.predictToolCall("what is 15 plus 30", StandardTools.allTools)
        assertEquals(1, nlCalcResults.size)
        assertEquals("calculate", nlCalcResults[0].toolName)
        assertEquals("45", nlCalcResults[0].getString("result"))

        val percentResults = engine.predictToolCall("20% of 150", StandardTools.allTools)
        assertEquals(1, percentResults.size)
        assertEquals("calculate", percentResults[0].toolName)
        assertEquals("30", percentResults[0].getString("result"))

        // 12. App and System Settings Controls
        val stealthResults = engine.predictToolCall("turn on stealth theme", StandardTools.allTools)
        assertEquals(1, stealthResults.size)
        assertEquals("change_app_setting", stealthResults[0].toolName)
        assertEquals("STEALTH_THEME", stealthResults[0].getString("setting"))
        assertEquals("true", stealthResults[0].getString("value"))

        val lockLayoutResults = engine.predictToolCall("lock layout", StandardTools.allTools)
        assertEquals(1, lockLayoutResults.size)
        assertEquals("change_app_setting", lockLayoutResults[0].toolName)
        assertEquals("LOCK_LAYOUT", lockLayoutResults[0].getString("setting"))
        assertEquals("true", lockLayoutResults[0].getString("value"))

        val brightnessResults = engine.predictToolCall("set brightness to 70%", StandardTools.allTools)
        assertEquals(1, brightnessResults.size)
        assertEquals("set_brightness", brightnessResults[0].toolName)
        assertEquals(70, brightnessResults[0].getInt("percent"))

        val volumeResults = engine.predictToolCall("mute volume", StandardTools.allTools)
        assertEquals(1, volumeResults.size)
        assertEquals("set_volume", volumeResults[0].toolName)
        assertEquals("mute", volumeResults[0].getString("action"))

        val dnsResults = engine.predictToolCall("change custom dns to dns.adguard.com", StandardTools.allTools)
        assertEquals(1, dnsResults.size)
        assertEquals("set_dns", dnsResults[0].toolName)
        assertEquals("dns.adguard.com", dnsResults[0].getString("host"))
    }
}
