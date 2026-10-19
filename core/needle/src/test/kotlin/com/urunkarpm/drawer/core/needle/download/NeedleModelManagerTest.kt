package com.urunkarpm.drawer.core.needle.download

import android.content.Context
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NeedleModelManagerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInitialStatusNotDownloadedWhenFileMissing() = runTest {
        val mockContext = mockk<Context>()
        val filesDir = tempFolder.newFolder("files")
        every { mockContext.filesDir } returns filesDir

        val manager = NeedleModelManager(mockContext)
        val initialStatus = manager.status.first()
        assertTrue(initialStatus is ModelStatus.NotDownloaded)
    }

    @Test
    fun testInitialStatusReadyWhenFileExists() = runTest {
        val mockContext = mockk<Context>()
        val filesDir = tempFolder.newFolder("files")
        val modelsDir = File(filesDir, "models").apply { mkdirs() }
        val modelFile = File(modelsDir, NeedleModelManager.MODEL_FILENAME).apply {
            writeBytes(ByteArray(1024 * 1024 * 5)) // 5 MB
        }
        every { mockContext.filesDir } returns filesDir

        val manager = NeedleModelManager(mockContext)
        val status = manager.status.first()
        assertTrue(status is ModelStatus.Ready)
    }

    @Test
    fun testInitialStatusReadyWhenLegacyFileExists() = runTest {
        val mockContext = mockk<Context>()
        val filesDir = tempFolder.newFolder("files")
        val modelsDir = File(filesDir, "models").apply { mkdirs() }
        val modelFile = File(modelsDir, "needle_3_cq2.bin").apply {
            writeBytes(ByteArray(1024 * 1024 * 5)) // 5 MB
        }
        every { mockContext.filesDir } returns filesDir

        val manager = NeedleModelManager(mockContext)
        val status = manager.status.first()
        assertTrue(status is ModelStatus.Ready)
    }

    @Test
    fun testDeleteModelRemovesFileAndUpdatesStatus() = runTest {
        val mockContext = mockk<Context>()
        val filesDir = tempFolder.newFolder("files")
        val modelsDir = File(filesDir, "models").apply { mkdirs() }
        val modelFile = File(modelsDir, NeedleModelManager.MODEL_FILENAME).apply {
            writeBytes(ByteArray(1024))
        }
        every { mockContext.filesDir } returns filesDir

        val manager = NeedleModelManager(mockContext)
        assertTrue(manager.status.first() is ModelStatus.Ready)
        val deleted = manager.deleteModel()
        assertTrue(deleted)
        assertTrue(manager.status.first() is ModelStatus.NotDownloaded)
        assertFalse(modelFile.exists())
    }
}
