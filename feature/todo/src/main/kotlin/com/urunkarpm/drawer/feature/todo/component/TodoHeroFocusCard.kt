package com.urunkarpm.drawer.feature.todo.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.designsystem.component.LocalMonochromeIcons
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoStatus

// ponytail: hero focus card prioritizing the single most urgent task; ceiling: statically bound hero calculation; upgrade path: ML contextual ranking.
@Composable
fun TodoHeroFocusCard(
    task: TodoItem,
    isNearby: Boolean,
    onComplete: (TodoItem) -> Unit,
    onClick: (TodoItem) -> Unit,
    onCyclePriority: ((TodoItem) -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val crispHaptics = LocalCrispHaptics.current
    val isMonochrome = LocalMonochromeIcons.current
    // ponytail: adapts priority indicators to subtle high-contrast tonal grayscale in Monochrome Vibe mode; ceiling: fixed luminance steps; upgrade path: custom dynamic tonal elevation.
    val priorityColor = if (isMonochrome) {
        when (task.priority) {
            TodoPriority.HIGH -> MaterialTheme.colorScheme.onSurface
            TodoPriority.NORMAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            TodoPriority.LOW -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        }
    } else {
        when (task.priority) {
            TodoPriority.HIGH -> Color(0xFFEF5350)   // Red
            TodoPriority.NORMAL -> Color(0xFF4CAF50) // Green
            TodoPriority.LOW -> Color(0xFF29B6F6)    // Blue
        }
    }

    val deadlineLabel = remember(task.deadlineUtc, task.deadlineZoneId) {
        task.deadlineUtc?.takeIf { it > 0 }?.let {
            TodoUtils.formatDeadline(it, task.deadlineZoneId, showOverdue = true)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMonochrome -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
                task.priority == TodoPriority.HIGH -> Color(0xFFEF5350).copy(alpha = 0.10f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
            }
        ),
        border = BorderStroke(
            1.5.dp,
            priorityColor.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .clickable {
                crispHaptics.click()
                onClick(task)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Priority Accent Stripe (uniform with all tasks)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Completion Checkmark (on the LEFT, uniform with all other tasks)
            IconButton(
                onClick = {
                    crispHaptics.confirm()
                    onComplete(task)
                },
                modifier = Modifier.size(36.dp)
            ) {
                // ponytail: simple check mark (red = open, green = done; in monochrome mode: onSurfaceVariant / primary); ceiling: binary status without custom iconography; upgrade path: multi-state custom check animation.
                val checkmarkTint = if (isMonochrome) {
                    if (task.status == TodoStatus.DONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    if (task.status == TodoStatus.DONE) Color(0xFF4CAF50) else Color(0xFFEF5350)
                }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete focus task",
                    tint = checkmarkTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Row: Focus Badge + Place/Deadline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Focus Badge (tap to cycle priority)
                    Surface(
                        onClick = {
                            crispHaptics.tick()
                            onCyclePriority?.invoke(task)
                        },
                        color = priorityColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.40f))
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
                                text = if (isNearby) "NEARBY FOCUS" else "NOW FOCUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = priorityColor
                            )
                        }
                    }

                    // Deadline or Place
                    if (isNearby && !task.placeName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = task.placeName.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (deadlineLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = deadlineLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Middle: Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!task.note.isNullOrBlank()) {
                    Text(
                        text = task.note.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onMoveDown != null) {
                IconButton(
                    onClick = {
                        crispHaptics.tick()
                        onMoveDown()
                    },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move focus task down",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


