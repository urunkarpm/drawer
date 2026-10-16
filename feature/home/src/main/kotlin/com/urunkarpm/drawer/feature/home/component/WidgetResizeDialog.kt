package com.urunkarpm.drawer.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.model.WidgetItem

data class ResizePreset(
    val label: String,
    val spanX: Int,
    val spanY: Int,
    val heightDp: Int
)

val WIDGET_RESIZE_PRESETS = listOf(
    ResizePreset("2×2 Compact", 2, 2, 140),
    ResizePreset("2×3 Tall", 2, 3, 210),
    ResizePreset("4×2 Banner", 4, 2, 140),
    ResizePreset("4×3 Standard", 4, 3, 210),
    ResizePreset("4×4 Dashboard", 4, 4, 280)
)

@Composable
fun WidgetResizeDialog(
    widget: WidgetItem,
    onDismiss: () -> Unit,
    onApply: (spanX: Int, spanY: Int, heightDp: Int) -> Unit
) {
    var selectedSpanX by remember(widget) { mutableIntStateOf(widget.spanX.coerceIn(2, 4)) }
    var selectedSpanY by remember(widget) { mutableIntStateOf(widget.spanY.coerceIn(1, 5)) }
    var selectedHeightDp by remember(widget) { mutableIntStateOf(widget.heightDp.coerceIn(80, 500)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "Resize Widget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive 4x4 Grid Matrix Visualizer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Grid Occupancy: ${selectedSpanX}×${selectedSpanY} (${selectedSpanX} cols × ${selectedSpanY} rows)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 columns x 4 rows mini matrix
                    Column(
                        modifier = Modifier.width(160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (r in 1..4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 1..4) {
                                    val isOccupied = c <= selectedSpanX && r <= selectedSpanY
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isOccupied)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Presets
                Column {
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WIDGET_RESIZE_PRESETS.take(3).forEach { preset ->
                            val isSelected = selectedSpanX == preset.spanX && selectedSpanY == preset.spanY
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSpanX = preset.spanX
                                    selectedSpanY = preset.spanY
                                    selectedHeightDp = preset.heightDp
                                },
                                label = { Text(preset.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WIDGET_RESIZE_PRESETS.drop(3).forEach { preset ->
                            val isSelected = selectedSpanX == preset.spanX && selectedSpanY == preset.spanY
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSpanX = preset.spanX
                                    selectedSpanY = preset.spanY
                                    selectedHeightDp = preset.heightDp
                                },
                                label = { Text(preset.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Fine Adjust Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Columns Stepper
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Width (Columns)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedSpanX > 2) selectedSpanX--
                                },
                                enabled = selectedSpanX > 2,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Width", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$selectedSpanX",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (selectedSpanX < 4) selectedSpanX++
                                },
                                enabled = selectedSpanX < 4,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Width", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Rows Stepper
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Height (Rows)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedSpanY > 1) {
                                        selectedSpanY--
                                        selectedHeightDp = selectedSpanY * 70
                                    }
                                },
                                enabled = selectedSpanY > 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Height", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$selectedSpanY",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (selectedSpanY < 5) {
                                        selectedSpanY++
                                        selectedHeightDp = selectedSpanY * 70
                                    }
                                },
                                enabled = selectedSpanY < 5,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Height", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(selectedSpanX, selectedSpanY, selectedHeightDp)
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
