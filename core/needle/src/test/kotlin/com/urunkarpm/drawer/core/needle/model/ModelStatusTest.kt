package com.urunkarpm.drawer.core.needle.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelStatusTest {
    @Test
    fun testModelStatusTransitions() {
        val notDownloaded: ModelStatus = ModelStatus.NotDownloaded
        val downloading: ModelStatus = ModelStatus.Downloading(0.45f)
        val ready: ModelStatus = ModelStatus.Ready("/path/needle.bin", 10.2f)
        val error: ModelStatus = ModelStatus.Error("Checksum failed")

        assertTrue(notDownloaded is ModelStatus.NotDownloaded)
        assertEquals(0.45f, (downloading as ModelStatus.Downloading).progress)
        assertEquals(10.2f, (ready as ModelStatus.Ready).sizeMb)
        assertEquals("Checksum failed", (error as ModelStatus.Error).cause)
    }
}
