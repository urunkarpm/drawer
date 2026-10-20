package com.urunkarpm.drawer.feature.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.needle.model.ModelStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeedleSettingsCard(
    status: ModelStatus,
    strictness: String = "BALANCED",
    onStrictnessChange: (String) -> Unit = {},
    categorizeAllApps: Boolean = false,
    onCategorizeAllAppsChange: (Boolean) -> Unit = {},
    forceWorkProfile: Boolean = true,
    onForceWorkProfileChange: (Boolean) -> Unit = {},
    autoCategorizeNewApps: Boolean = true,
    onAutoCategorizeNewAppsChange: (Boolean) -> Unit = {},
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "On-Device AI (Needle 3)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "100% offline intelligence & semantic categorization",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Model Status & Download/Delete
            when (status) {
                is ModelStatus.NotDownloaded -> {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Model (~33.7 MB)")
                    }
                }
                is ModelStatus.Downloading -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Downloading... ${(status.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                is ModelStatus.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ready (${status.sizeMb.toInt()} MB) • Fully Offline",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDelete) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Model")
                        }
                    }
                }
                is ModelStatus.Error -> {
                    Text(
                        text = "Download error: ${status.cause}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = onDownload) {
                        Text("Retry Download")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // AI Model Fine-Tuning Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Model Fine-Tuning & Strictness",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Categorization Strictness",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Strictness Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "STRICT" to "Strict",
                    "BALANCED" to "Balanced",
                    "AGGRESSIVE" to "Aggressive"
                ).forEach { (key, label) ->
                    val isSelected = strictness.equals(key, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStrictnessChange(key) },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic Strictness Description
            val strictnessDescription = when (strictness.uppercase()) {
                "STRICT" -> "Strict: Only high-confidence matches are suggested to prevent clutter."
                "AGGRESSIVE" -> "Aggressive: Eager matching. Categorizes maximum apps across all drawers."
                else -> "Balanced: Recommended balance between precision and category coverage."
            }
            Text(
                text = strictnessDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Re-evaluate All Apps Switch
            CardSwitchRow(
                title = "Re-evaluate already assigned apps",
                subtitle = "Scan all installed apps, not just unassigned ones",
                checked = categorizeAllApps,
                onCheckedChange = onCategorizeAllAppsChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Strict Work Profile Isolation Switch
            CardSwitchRow(
                title = "Strict Work Profile isolation",
                subtitle = "Automatically route enterprise & work apps to Productivity",
                checked = forceWorkProfile,
                onCheckedChange = onForceWorkProfileChange
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auto-Suggest on Install Switch
            CardSwitchRow(
                title = "Auto-suggest on new app install",
                subtitle = "Suggest category when a new app is installed from store",
                checked = autoCategorizeNewApps,
                onCheckedChange = onAutoCategorizeNewAppsChange
            )
        }
    }
}

@Composable
private fun CardSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
