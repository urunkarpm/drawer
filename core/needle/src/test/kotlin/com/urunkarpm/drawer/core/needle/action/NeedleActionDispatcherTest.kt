package com.urunkarpm.drawer.core.needle.action

import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NeedleActionDispatcherTest {
    private val dispatcher = NeedleActionDispatcher()

    @Test
    fun testSetTimerToolDispatch() {
        val toolCall = ToolCallResult(
            toolName = "set_timer",
            arguments = buildJsonObject {
                put("seconds", 600)
                put("label", "Tea timer")
            }
        )

        val action = dispatcher.dispatch(toolCall)
        assertNotNull(action)
        assertNotNull(action?.intent)
        assertEquals("Set timer for 10 min", action?.title)
        assertEquals("Tea timer", action?.subtitle)
        assertEquals(true, action?.isDirectExecutionSafe)
    }

    @Test
    fun testSetAlarmToolDispatch() {
        val toolCall = ToolCallResult(
            toolName = "set_alarm",
            arguments = buildJsonObject {
                put("hour", 7)
                put("minute", 30)
                put("message", "Wake up")
            }
        )

        val action = dispatcher.dispatch(toolCall)
        assertNotNull(action)
        assertNotNull(action?.intent)
        assertEquals("Set alarm for 07:30", action?.title)
        assertEquals("Wake up", action?.subtitle)
        assertEquals(true, action?.isDirectExecutionSafe)
    }

    @Test
    fun testUnknownToolReturnsNull() {
        val toolCall = ToolCallResult(
            toolName = "unknown_tool",
            arguments = buildJsonObject {}
        )

        val action = dispatcher.dispatch(toolCall)
        assertNull(action)
    }
}
