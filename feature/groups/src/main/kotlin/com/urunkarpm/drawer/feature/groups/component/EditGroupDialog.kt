package com.urunkarpm.drawer.feature.groups.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import android.graphics.drawable.Drawable
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.GroupSortOrder
import com.urunkarpm.drawer.core.model.GroupViewType
import com.urunkarpm.drawer.feature.groups.util.GroupIcons

import com.urunkarpm.drawer.core.model.AppInfo

@Composable
fun EditGroupDialog(
    group: AppGroup?,
    allApps: List<AppInfo> = emptyList(),
    iconLoader: (suspend (AppInfo) -> Drawable?)? = null,
    onDismissRequest: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String, viewType: GroupViewType, columnCount: Int, sortOrder: GroupSortOrder, selectedApps: List<AppInfo>) -> Unit,
    onDelete: (() -> Unit)? = null,
    twoDrawersSideBySide: Boolean = false
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(group?.iconName ?: "folder") }
    var selectedColor by remember {
        mutableStateOf(group?.colorHex ?: GroupIcons.availableColors.first())
    }
    var selectedViewType by remember {
        mutableStateOf(group?.viewType ?: if (twoDrawersSideBySide) GroupViewType.LIST else GroupViewType.GRID)
    }
    var columnCount by remember { mutableIntStateOf(group?.columnCount ?: 4) }
    var selectedSortOrder by remember { mutableStateOf(group?.sortOrder ?: GroupSortOrder.MANUAL) }

    val initialSelectedKeys = remember(group) {
        group?.items?.map { "${it.packageName}/${it.activityName}" }?.toSet() ?: emptySet()
    }
    var selectedApps by remember { mutableStateOf(initialSelectedKeys) }

    val isEditing = group != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Category" else "New Category",
                    style = MaterialTheme.typography.titleLarge
                )
                if (isEditing && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Category",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Picker
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupIcons.availableColors.forEach { colorHex ->
                        val color = GroupIcons.parseColor(colorHex)
                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Icon Picker
                Text(
                    text = "Category Icon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupIcons.availableIcons.forEach { (iconKey, vector) ->
                        val isSelected = selectedIcon == iconKey
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = iconKey,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Layout Style
                Text(
                    text = "Layout Style",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedViewType == GroupViewType.GRID,
                        onClick = { selectedViewType = GroupViewType.GRID },
                        label = { Text("Grid") }
                    )
                    FilterChip(
                        selected = selectedViewType == GroupViewType.LIST,
                        onClick = { selectedViewType = GroupViewType.LIST },
                        label = { Text("List") }
                    )
                }

                if (selectedViewType == GroupViewType.GRID) {
                    Text(
                        text = "Grid Columns",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 4, 5).forEach { count ->
                            FilterChip(
                                selected = columnCount == count,
                                onClick = { columnCount = count },
                                label = { Text("$count") }
                            )
                        }
                    }
                }

                // Sorting
                Text(
                    text = "Sort Apps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSortOrder == GroupSortOrder.MANUAL,
                        onClick = { selectedSortOrder = GroupSortOrder.MANUAL },
                        label = { Text("Manual") }
                    )
                    FilterChip(
                        selected = selectedSortOrder == GroupSortOrder.ALPHABETICAL,
                        onClick = { selectedSortOrder = GroupSortOrder.ALPHABETICAL },
                        label = { Text("A to Z") }
                    )
                }

                // ponytail: inline app selector with live icon rendering and instant search filter for bulk drawer assignment.
                if (allApps.isNotEmpty()) {
                    var appSearchQuery by remember { mutableStateOf("") }
                    val filteredApps = remember(allApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) allApps
                        else allApps.filter { it.label.contains(appSearchQuery.trim(), ignoreCase = true) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Apps (${selectedApps.size} selected)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedApps.isNotEmpty()) {
                            TextButton(
                                onClick = { selectedApps = emptySet() }
                            ) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Filter apps...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (appSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { appSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ponytail: inline app selector rendered directly inside dialog scroll column to prevent nested scroll traps; ceiling: 100-300 apps rendered without lazy recycling; upgrade path: LazyColumn in a full-screen sheet.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        filteredApps.forEach { app ->
                                val key = "${app.packageName}/${app.activityName}"
                                val isSelected = key in selectedApps
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedApps = if (isSelected) {
                                                selectedApps - key
                                            } else {
                                                selectedApps + key
                                            }
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AppIconImage(
                                        key = app.componentKey,
                                        size = 36.dp,
                                        label = app.label,
                                        isWorkProfile = app.isWorkProfile,
                                        iconLoader = { iconLoader?.invoke(app) }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalApps = allApps.filter { "${it.packageName}/${it.activityName}" in selectedApps }
                        onSave(name, selectedIcon, selectedColor, selectedViewType, columnCount, selectedSortOrder, finalApps)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (isEditing) "Save" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
