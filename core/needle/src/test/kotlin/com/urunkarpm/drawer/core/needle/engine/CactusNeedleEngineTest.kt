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
    fun testPredictToolCallReturnsEmptyWhenNotDownloaded() = runTest {
        val modelManager = mockk<NeedleModelManager>()
        val statusFlow = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
        every { modelManager.status } returns statusFlow

        val engine = CactusNeedleEngine(modelManager)
        val results = engine.predictToolCall("timer 10 minutes", StandardTools.allTools)

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
}
