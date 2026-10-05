package com.urunkarpm.drawer.core.designsystem.modifier

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Unified gesture detector combining instantaneous tap execution, list scroll passthrough,
 * and long-press drag-to-drawer / drag-to-dock.
 */
fun Modifier.appDragSource(
    key: Any?,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (isDropped: Boolean) -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    this
        .onGloballyPositioned { coords ->
            itemCoordinates = coords
        }
        .pointerInput(key) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                val touchSlop = viewConfiguration.touchSlop
                var isLongPressed = false

                // 1. Wait for long press or tap
                try {
                    withTimeout(viewConfiguration.longPressTimeoutMillis) {
                        val initialPosition = down.position
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            
                            if (change == null || change.changedToUp() || !change.pressed) {
                                // Tap released
                                val currentPos = change?.position ?: down.position
                                val dist = (currentPos - initialPosition).getDistance()
                                if (dist <= touchSlop) {
                                    change?.consume()
                                    onAppClick()
                                }
                                return@withTimeout
                            }
                            
                            val dist = (change.position - initialPosition).getDistance()
                            if (dist > touchSlop) {
                                return@withTimeout // Moved too far, let parent scroll
                            }
                        }
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    isLongPressed = true
                } catch (_: TimeoutCancellationException) {
                    isLongPressed = true
                }

                // 2. Handle long press drag or options menu
                if (isLongPressed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    var isDragStarted = false
                    var lastPosition: Offset? = null
                    var postLongPressDrag = Offset.Zero

                    while (true) {
                        // Use Initial pass to steal events from scrollable parents
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) {
                            if (isDragStarted) {
                                onDragEnd(false)
                            }
                            break
                        }

                        if (change.changedToUp() || !change.pressed) {
                            if (isDragStarted) {
                                onDragEnd(true)
                            } else {
                                onAppLongClick()
                            }
                            change.consume()
                            break
                        }

                        val currentPosition = change.position
                        val dragAmount = if (lastPosition != null) {
                            currentPosition - lastPosition
                        } else {
                            Offset.Zero
                        }
                        lastPosition = currentPosition
                        
                        postLongPressDrag += dragAmount

                        // Account for human finger micro-jiggle after long press
                        if (!isDragStarted) {
                            if (postLongPressDrag.getDistance() > touchSlop * 0.4f) {
                                isDragStarted = true
                                val rootPos = (itemCoordinates?.positionInRoot() ?: Offset.Zero) + change.position
                                onDragStart(rootPos)
                            }
                        } else {
                            if (dragAmount != Offset.Zero) {
                                onDrag(dragAmount)
                            }
                        }
                        
                        // Consume the event so parents don't scroll
                        change.consume()
                    }
                }
            }
        }
}
