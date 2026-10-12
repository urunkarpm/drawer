package com.urunkarpm.drawer.feature.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ponytail: Alphabet jump strip calculates letter offset mathematically via drag position; ceiling: fixed # + A-Z list; upgrade path: dynamic list based only on installed initials.
private val ALPHABET = listOf(
    "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
    "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
    "V", "W", "X", "Y", "Z"
)

@Composable
fun AlphabetIndexBar(
    onLetterScrolled: (String) -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var draggingLetter by remember { mutableStateOf<String?>(null) }
    var dragYOffsetPx by remember { mutableFloatStateOf(0f) }

    fun updateLetter(y: Float) {
        if (barHeightPx <= 0f) return
        val clampedY = y.coerceIn(0f, barHeightPx - 1f)
        val itemHeight = barHeightPx / ALPHABET.size
        val index = (clampedY / itemHeight).toInt().coerceIn(0, ALPHABET.lastIndex)
        val letter = ALPHABET[index]
        dragYOffsetPx = clampedY
        if (letter != draggingLetter) {
            draggingLetter = letter
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterScrolled(letter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Active letter bubble indicator appearing to the left of the drag position
        val density = LocalDensity.current
        val bubbleOffsetXPx = with(density) { 72.dp.roundToPx() }
        val bubbleHalfHeightPx = with(density) { 28.dp.toPx() }

        AnimatedVisibility(
            visible = draggingLetter != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            val letter = draggingLetter ?: ""
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = -bubbleOffsetXPx,
                                y = (dragYOffsetPx - bubbleHalfHeightPx).roundToInt().coerceAtLeast(0)
                            )
                        }
                        .size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Alphabet letter column
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .padding(vertical = 4.dp)
                .onGloballyPositioned { coordinates ->
                    barHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(barHeightPx) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            updateLetter(down.position.y)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                change.consume()
                                updateLetter(change.position.y)
                            }
                            draggingLetter = null
                            onDragEnd()
                        }
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ALPHABET.forEach { letter ->
                AlphabetLetterItem(
                    letter = letter,
                    isSelected = letter == draggingLetter
                )
            }
        }
    }
}

@Composable
private fun AlphabetLetterItem(
    letter: String,
    isSelected: Boolean
) {
    Text(
        text = letter,
        fontSize = if (isSelected) 11.sp else 9.sp,
        lineHeight = 11.sp,
        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        },
        textAlign = TextAlign.Center
    )
}
