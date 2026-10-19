package com.urunkarpm.drawer.core.needle.engine

import com.urunkarpm.drawer.core.needle.action.StandardTools
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {
    @Test
    fun testPromptContainsToolDefinitionsAndQuery() {
        val prompt = PromptBuilder.buildToolCallPrompt(
            query = "timer 10 minutes",
            tools = StandardTools.allTools
        )
        assertTrue(prompt.contains("set_timer"))
        assertTrue(prompt.contains("timer 10 minutes"))
    }
}
