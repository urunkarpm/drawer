package com.urunkarpm.drawer.feature.todo.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ponytail: clean Compose Canvas donut chart for task breakdown; eliminates 260 lines of complex glass reflection math.
@Composable
fun LiquidGlassPieChart(
    completedCount: Int,
    pendingOnTrackCount: Int,
    overdueOrStaleCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 24.dp
) {
    val total = (completedCount + pendingOnTrackCount + overdueOrStaleCount).coerceAtLeast(0)
    val completedColor = Color(0xFF10B981) // Green
    val pendingColor = Color(0xFF3B82F6)   // Blue
    val overdueColor = Color(0xFFEF4444)   // Red
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val percent = if (total > 0) ((completedCount.toFloat() / total) * 100).roundToInt() else 0

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            if (total == 0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                val activeSegmentCount = listOf(completedCount, pendingOnTrackCount, overdueOrStaleCount).count { it > 0 }
                val strokeCap = if (activeSegmentCount > 1) StrokeCap.Butt else StrokeCap.Round
                var startAngle = -90f
                val sweepCompleted = (completedCount.toFloat() / total) * 360f
                val sweepPending = (pendingOnTrackCount.toFloat() / total) * 360f
                val sweepOverdue = (overdueOrStaleCount.toFloat() / total) * 360f

                if (sweepCompleted > 0f) {
                    drawArc(
                        color = completedColor,
                        startAngle = startAngle,
                        sweepAngle = sweepCompleted,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = strokeCap)
                    )
                    startAngle += sweepCompleted
                }

                if (sweepPending > 0f) {
                    drawArc(
                        color = pendingColor,
                        startAngle = startAngle,
                        sweepAngle = sweepPending,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = strokeCap)
                    )
                    startAngle += sweepPending
                }

                if (sweepOverdue > 0f) {
                    drawArc(
                        color = overdueColor,
                        startAngle = startAngle,
                        sweepAngle = sweepOverdue,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = strokeCap)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "completed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
