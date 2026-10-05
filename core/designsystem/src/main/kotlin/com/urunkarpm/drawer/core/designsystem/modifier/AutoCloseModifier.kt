package com.urunkarpm.drawer.core.designsystem.modifier

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * Automatically closes an expanded drawer after [timeoutMs] of inactivity.
 * Countdown pauses while any finger touches the screen, and restarts when fingers lift.
 */
// ponytail: auto-closes drawer after 2 seconds of inactivity, pausing countdown while finger is held on screen; ceiling is fixed 2s timeout; upgrade path is user-configurable timeout in Settings.
fun Modifier.autoCloseOnInactivity(
    active: Boolean,
    resetKey: Any? = null,
    timeoutMs: Long = 2000L,
    onClose: () -> Unit
): Modifier = composed {
    if (!active) return@composed this

    val isTouching = remember(active) { object { var value = false } }
    var timerTrigger by remember(active, resetKey) { mutableIntStateOf(0) }

    LaunchedEffect(active, timerTrigger, resetKey) {
        delay(timeoutMs)
        if (!isTouching.value) {
            onClose()
        }
    }

    this.pointerInput(active) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            isTouching.value = true
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.none { it.pressed }) break
            }
            isTouching.value = false
            timerTrigger++
        }
    }
}
