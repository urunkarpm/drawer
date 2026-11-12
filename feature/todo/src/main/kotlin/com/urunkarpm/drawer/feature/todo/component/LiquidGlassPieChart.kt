package com.urunkarpm.drawer.feature.todo.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

data class PieSliceData(
    val value: Float,
    val brush: Brush,
    val rimColor: Color,
    val label: String
)

@Composable
fun LiquidGlassPieChart(
    completedCount: Int,
    pendingOnTrackCount: Int,
    overdueOrStaleCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 26.dp
) {
    val total = (completedCount + pendingOnTrackCount + overdueOrStaleCount).coerceAtLeast(0)
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(completedCount, pendingOnTrackCount, overdueOrStaleCount) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    // Emerald / Mint Glass for Completed
    val completedBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF34D399).copy(alpha = 0.90f),
            Color(0xFF059669).copy(alpha = 0.95f)
        )
    )
    val completedRim = Color(0xFFA7F3D0).copy(alpha = 0.85f)

    // Sapphire / Cyan Glass for Pending (On-Track)
    val pendingBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF38BDF8).copy(alpha = 0.90f),
            Color(0xFF0284C7).copy(alpha = 0.95f)
        )
    )
    val pendingRim = Color(0xFFBAE6FD).copy(alpha = 0.85f)

    // Amber / Radiant Coral Glass for Overdue or Stale
    val overdueBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFB923C).copy(alpha = 0.90f),
            Color(0xFFEA580C).copy(alpha = 0.95f)
        )
    )
    val overdueRim = Color(0xFFFED7AA).copy(alpha = 0.85f)

    val slices = remember(completedCount, pendingOnTrackCount, overdueOrStaleCount, total) {
        if (total == 0) {
            emptyList()
        } else {
            val list = mutableListOf<PieSliceData>()
            if (completedCount > 0) {
                list.add(
                    PieSliceData(
                        value = completedCount.toFloat() / total,
                        brush = completedBrush,
                        rimColor = completedRim,
                        label = "Completed"
                    )
                )
            }
            if (pendingOnTrackCount > 0) {
                list.add(
                    PieSliceData(
                        value = pendingOnTrackCount.toFloat() / total,
                        brush = pendingBrush,
                        rimColor = pendingRim,
                        label = "Pending"
                    )
                )
            }
            if (overdueOrStaleCount > 0) {
                list.add(
                    PieSliceData(
                        value = overdueOrStaleCount.toFloat() / total,
                        brush = overdueBrush,
                        rimColor = overdueRim,
                        label = "Overdue"
                    )
                )
            }
            list
        }
    }

    val completionPercent = if (total > 0) {
        ((completedCount.toFloat() / total) * 100f).roundToInt()
    } else {
        100
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glass back-glow
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.size(size)) {
            val canvasStrokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - canvasStrokePx, this.size.height - canvasStrokePx)
            val topLeft = Offset(canvasStrokePx / 2f, canvasStrokePx / 2f)

            if (slices.isEmpty()) {
                // Empty state track: Translucent glass track
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = canvasStrokePx, cap = StrokeCap.Round)
                )
            } else {
                // Background subtle glass groove
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = canvasStrokePx)
                )

                var currentStartAngle = -90f
                val gapDegree = if (slices.size > 1) 3f else 0f
                val totalAvailableAngle = 360f - (gapDegree * slices.size)

                slices.forEach { slice ->
                    val sweep = (slice.value * totalAvailableAngle * animatedProgress.value)
                        .coerceAtLeast(0f)

                    if (sweep > 0.5f) {
                        // Main liquid slice
                        drawArc(
                            brush = slice.brush,
                            startAngle = currentStartAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = canvasStrokePx, cap = StrokeCap.Round)
                        )

                        // Specular rim shine highlight along the outer edge
                        drawArc(
                            color = slice.rimColor.copy(alpha = 0.65f),
                            startAngle = currentStartAngle + 1f,
                            sweepAngle = (sweep - 2f).coerceAtLeast(0f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = canvasStrokePx * 0.22f, cap = StrokeCap.Round)
                        )
                    }
                    currentStartAngle += sweep + gapDegree
                }
            }
        }

        // Inner frosted core card
        Box(
            modifier = Modifier
                .size(size - (strokeWidth * 2.2f))
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$completionPercent%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (total == 1) "1 item" else "$total items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
