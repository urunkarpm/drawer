package com.urunkarpm.drawer.feature.todo.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ponytail: direct card without horizontal swipe gestures to avoid launcher drawer page-drag conflicts; ceiling: tap-to-complete only; upgrade path: long-press contextual action menu.
@Composable
fun TodoItemRow(
    task: TodoItem,
    isStale: Boolean,
    isNearbyNow: Boolean,
    onComplete: (TodoItem) -> Unit,
    onDelete: ((TodoItem) -> Unit)? = null,
    onCyclePriority: ((TodoItem) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val crispHaptics = LocalCrispHaptics.current
    val now = System.currentTimeMillis()
    val isOverdue = TodoUtils.isOverdue(task.deadlineUtc, now)

    val priorityColor = when (task.priority) {
        TodoPriority.HIGH -> Color(0xFFEF5350)   // Red
        TodoPriority.NORMAL -> Color(0xFF4CAF50) // Green
        TodoPriority.LOW -> Color(0xFF29B6F6)    // Blue
    }

    val formattedDeadline = remember(task.deadlineUtc, task.deadlineZoneId) {
        val utc = task.deadlineUtc ?: return@remember null
        if (utc <= 0L) return@remember null
        formatDeadline(utc, task.deadlineZoneId)
    }

    Card(
        onClick = {
            crispHaptics.click()
            onClick?.invoke()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isNearbyNow -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                isStale || isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.20f)
                task.priority == TodoPriority.HIGH && task.status == TodoStatus.OPEN -> Color(0xFFEF5350).copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.70f)
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isNearbyNow -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                isStale || isOverdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.40f)
                task.status == TodoStatus.DONE -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                task.priority == TodoPriority.HIGH -> priorityColor.copy(alpha = 0.45f)
                else -> priorityColor.copy(alpha = 0.28f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Priority Accent Stripe
                    if (task.status == TodoStatus.OPEN) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Completion Checkmark
                    IconButton(
                        onClick = {
                            crispHaptics.confirm()
                            onComplete(task)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        // ponytail: simple check mark (red = open, green = done); ceiling: binary status without custom iconography; upgrade path: multi-state custom check animation.
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete task",
                            tint = if (task.status == TodoStatus.DONE) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Title + Badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = when (task.priority) {
                                        TodoPriority.HIGH -> FontWeight.Bold
                                        TodoPriority.NORMAL -> FontWeight.SemiBold
                                        TodoPriority.LOW -> FontWeight.Medium
                                    },
                                    textDecoration = if (task.status == TodoStatus.DONE) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = when {
                                    task.status == TodoStatus.DONE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    isOverdue -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (isStale && task.status == TodoStatus.OPEN) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Stale",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (isNearbyNow && task.status == TodoStatus.OPEN) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Nearby",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Interactive Priority Pill for all open tasks
                            // ponytail: interactive priority pill cycling Normal -> High -> Low on tap; ceiling: in-row toggle; upgrade path: long-press priority picker menu.
                            if (task.status == TodoStatus.OPEN) {
                                Surface(
                                    onClick = {
                                        crispHaptics.tick()
                                        onCyclePriority?.invoke(task)
                                    },
                                    color = priorityColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(priorityColor)
                                        )
                                        Text(
                                            text = when (task.priority) {
                                                TodoPriority.HIGH -> "High"
                                                TodoPriority.NORMAL -> "Normal"
                                                TodoPriority.LOW -> "Low"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = priorityColor
                                        )
                                    }
                                }
                            }
                        }

                        // Note if present
                        val note = task.note
                        if (!note.isNullOrBlank()) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Place name and Deadline Row
                        val placeName = task.placeName
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!placeName.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = if (isNearbyNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = placeName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isNearbyNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (formattedDeadline != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formattedDeadline,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

private fun formatDeadline(utc: Long, zoneIdStr: String?): String {
    val zoneId = zoneIdStr?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(utc), zoneId)
    val now = ZonedDateTime.now(zoneId)

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return when {
        zdt.toLocalDate().isEqual(now.toLocalDate()) -> "Today ${zdt.format(timeFormatter)}"
        zdt.toLocalDate().isEqual(now.toLocalDate().plusDays(1)) -> "Tomorrow ${zdt.format(timeFormatter)}"
        else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }
}
