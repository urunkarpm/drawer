package com.urunkarpm.drawer.feature.groups.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.data.ai.CategorizationSuggestion
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.groups.util.GroupIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCategorizationPreviewBottomSheet(
    suggestions: List<CategorizationSuggestion>,
    isLoading: Boolean,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    onApplySuggestions: (List<CategorizationSuggestion>) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier
) {
    var selectedAppKeysByGroup by remember(suggestions) {
        mutableStateOf(
            suggestions.associate { suggestion ->
                suggestion.group.id to suggestion.suggestedApps.map { it.componentKey }.toSet()
            }
        )
    }

    val totalSelectedCount = selectedAppKeysByGroup.values.sumOf { it.size }
    val totalSuggestedCount = suggestions.sumOf { it.suggestedApps.size }
    val allSelected = totalSuggestedCount > 0 && totalSelectedCount == totalSuggestedCount

    fun toggleApp(groupId: String, componentKey: String) {
        val currentSet = selectedAppKeysByGroup[groupId].orEmpty()
        val newSet = if (componentKey in currentSet) {
            currentSet - componentKey
        } else {
            currentSet + componentKey
        }
        selectedAppKeysByGroup = selectedAppKeysByGroup + (groupId to newSet)
    }

    fun toggleSelectAll() {
        selectedAppKeysByGroup = if (allSelected) {
            suggestions.associate { it.group.id to emptySet<String>() }
        } else {
            suggestions.associate { it.group.id to it.suggestedApps.map { app -> app.componentKey }.toSet() }
        }
    }

    fun applyApproved() {
        val approved = suggestions.mapNotNull { suggestion ->
            val approvedKeys = selectedAppKeysByGroup[suggestion.group.id].orEmpty()
            val filteredApps = suggestion.suggestedApps.filter { it.componentKey in approvedKeys }
            if (filteredApps.isNotEmpty()) {
                suggestion.copy(suggestedApps = filteredApps)
            } else {
                null
            }
        }
        onApplySuggestions(approved)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Organize Drawers with AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Suggested categorization based on on-device intelligence. Select the apps you'd like to assign.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Analyzing installed apps on-device...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (suggestions.isEmpty() || totalSuggestedCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No apps to organize",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "All installed apps are already assigned to drawers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalSuggestedCount suggestions across ${suggestions.size} drawers",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { toggleSelectAll() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(if (allSelected) "Deselect all" else "Select all")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suggestions, key = { it.group.id }) { suggestion ->
                        SuggestionGroupCard(
                            suggestion = suggestion,
                            selectedAppKeys = selectedAppKeysByGroup[suggestion.group.id].orEmpty(),
                            onToggleApp = { key -> toggleApp(suggestion.group.id, key) },
                            iconLoader = iconLoader
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { applyApproved() },
                    enabled = !isLoading && totalSelectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (totalSelectedCount > 0) "Apply ($totalSelectedCount apps)" else "Apply")
                }
            }
        }
    }
}

@Composable
private fun SuggestionGroupCard(
    suggestion: CategorizationSuggestion,
    selectedAppKeys: Set<String>,
    onToggleApp: (String) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier
) {
    val group = suggestion.group
    val groupColor = remember(group.colorHex, group.name, group.id) {
        if (!group.colorHex.isNullOrBlank() && group.colorHex != "#6750A4") {
            GroupIcons.parseColor(group.colorHex, fallback = GroupIcons.resolveCategoryColor(group.name, group.id))
        } else {
            GroupIcons.resolveCategoryColor(group.name, group.id)
        }
    }
    val icon = GroupIcons.getIcon(group.iconName)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(groupColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${selectedAppKeys.size}/${suggestion.suggestedApps.size} apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(4.dp))

            suggestion.suggestedApps.forEach { app ->
                val isChecked = app.componentKey in selectedAppKeys
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleApp(app.componentKey) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(
                        key = app.componentKey,
                        size = 36.dp,
                        label = app.label,
                        isWorkProfile = app.isWorkProfile,
                        iconLoader = { iconLoader(app) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleApp(app.componentKey) }
                    )
                }
            }
        }
    }
}
