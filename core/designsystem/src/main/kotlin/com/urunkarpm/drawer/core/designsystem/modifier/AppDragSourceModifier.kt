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
    onDragEnd: (isDropped: Boolean) -> Unit,
    longPressRequired: Boolean = false
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val coordsHolder = remember { arrayOfNulls<LayoutCoordinates>(1) }

    this
        .onGloballyPositioned { coords ->
            coordsHolder[0] = coords
        }
        .pointerInput(key, longPressRequired) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = true)
                val pointerId = down.id
                val touchSlop = viewConfiguration.touchSlop
                val initialPosition = down.position
                var isDragStarted = false
                var isLongPressed = false
                var currentPosition = initialPosition

                // When in app drawer (longPressRequired = true), require standard launcher long-press hold (350ms)
                // so scrolling down/up the list is 100% protected and never triggers accidental drag.
                // When on home grid (longPressRequired = false), quick 180ms hold or immediate horizontal flick.
                val dragInitiationTimeoutMillis = if (longPressRequired) 350L else 180L
                try {
                    withTimeout(dragInitiationTimeoutMillis) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId }

                            if (change == null || change.changedToUp() || !change.pressed) {
                                // Tap released quickly within touchSlop
                                val releasePos = change?.position ?: down.position
                                val dist = (releasePos - initialPosition).getDistance()
                                if (dist <= touchSlop) {
                                    change?.consume()
                                    onAppClick()
                                }
                                return@withTimeout
                            }

                            currentPosition = change.position
                            val deltaX = currentPosition.x - initialPosition.x
                            val deltaY = currentPosition.y - initialPosition.y
                            val dist = (currentPosition - initialPosition).getDistance()

                            if (!longPressRequired) {
                                // On grid: moving horizontally left/right past touchSlop initiates drag immediately without hesitation
                                if (kotlin.math.abs(deltaX) > touchSlop && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 1.2f) {
                                    isDragStarted = true
                                    change.consume()
                                    return@withTimeout
                                }
                            } else {
                                // In AllAppsDrawer (longPressRequired = true):
                                // If user moves past touchSlop before timeout, user is SCROLLING the list!
                                // Exit without consuming so LazyVerticalGrid / scroll container handles the scroll cleanly.
                                if (dist > touchSlop) {
                                    return@withTimeout
                                }
                            }
                        }
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    isLongPressed = true
                } catch (_: TimeoutCancellationException) {
                    isLongPressed = true
                }

                // If user released quickly (tap executed), end gesture
                if (!isLongPressed && !isDragStarted) {
                    return@awaitEachGesture
                }

                // Provide haptic feedback on drag or long-press hold
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                // If drag was initiated by quick movement in phase 1, trigger onDragStart right away!
                if (isDragStarted) {
                    val rootPos = (coordsHolder[0]?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero) + currentPosition
                    onDragStart(rootPos)
                }

                // Phase 2: Active pointer tracking (streaming drag deltas or waiting for long-press release)
                var lastPosition = currentPosition

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) {
                            if (isDragStarted) {
                                isDragStarted = false
                                onDragEnd(false)
                            }
                            break
                        }

                        if (change.changedToUp() || !change.pressed) {
                            if (isDragStarted) {
                                isDragStarted = false
                                onDragEnd(true)
                            } else {
                                onAppLongClick()
                            }
                            change.consume()
                            break
                        }

                        val newPosition = change.position
                        val dragAmount = newPosition - lastPosition
                        lastPosition = newPosition

                        if (!isDragStarted) {
                            // User held stationary for timeout, now moved finger slightly
                            val totalMovement = (newPosition - initialPosition).getDistance()
                            if (totalMovement > touchSlop * 0.3f) {
                                isDragStarted = true
                                val rootPos = (coordsHolder[0]?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero) + newPosition
                                onDragStart(rootPos)
                            }
                        } else {
                            if (dragAmount != Offset.Zero) {
                                onDrag(dragAmount)
                            }
                        }

                        change.consume()
                    }
                } finally {
                    if (isDragStarted) {
                        isDragStarted = false
                        onDragEnd(false)
                    }
                }
            }
        }
}
