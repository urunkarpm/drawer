package com.urunkarpm.drawer.feature.groups

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupViewType
import com.urunkarpm.drawer.feature.groups.component.EditGroupDialog
import com.urunkarpm.drawer.feature.groups.component.GroupItemActionBottomSheet
import com.urunkarpm.drawer.feature.groups.util.GroupIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsAccordion(
    viewModel: GroupsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.groups.forEachIndexed { index, resolvedGroup ->
            GroupCard(
                resolvedGroup = resolvedGroup,
                groupIndex = index,
                totalGroups = uiState.groups.size,
                onToggleExpand = { viewModel.toggleGroupExpanded(resolvedGroup.group.id) },
                onEditGroup = { viewModel.startEditingGroup(resolvedGroup.group) },
                onMoveUp = { viewModel.moveGroupUp(index) },
                onMoveDown = { viewModel.moveGroupDown(index, uiState.groups.size) },
                onDeleteGroup = { viewModel.deleteGroup(resolvedGroup.group.id) },
                onAppClick = { app -> viewModel.launchApp(app) },
                onAppLongClick = { app -> viewModel.selectAppForAction(app, resolvedGroup.group) },
                iconLoader = { app -> viewModel.getAppIcon(app) }
            )
        }

        // Add Category Button
        FilledTonalButton(
            onClick = { viewModel.setShowCreateDialog(true) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Category",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Category")
        }
    }

    // Create Group Dialog
    if (uiState.showCreateDialog) {
        EditGroupDialog(
            group = null,
            onDismissRequest = { viewModel.setShowCreateDialog(false) },
            onSave = { name, iconName, colorHex, viewType, columnCount, sortOrder ->
                viewModel.createGroup(name, iconName, colorHex, viewType, columnCount, sortOrder)
            }
        )
    }

    // Edit Group Dialog
    uiState.groupBeingEdited?.let { groupToEdit ->
        EditGroupDialog(
            group = groupToEdit,
            onDismissRequest = { viewModel.dismissEditGroup() },
            onSave = { name, iconName, colorHex, viewType, columnCount, sortOrder ->
                viewModel.updateGroup(
                    groupToEdit.copy(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        viewType = viewType,
                        columnCount = columnCount,
                        sortOrder = sortOrder
                    )
                )
            },
            onDelete = { viewModel.deleteGroup(groupToEdit.id) }
        )
    }

    // App Action Bottom Sheet
    uiState.selectedAppForAction?.let { (app, group) ->
        GroupItemActionBottomSheet(
            app = app,
            group = group,
            sheetState = actionSheetState,
            onDismissRequest = { viewModel.dismissAppAction() },
            onLaunchApp = {
                viewModel.dismissAppAction()
                viewModel.launchApp(app)
            },
            onRemoveFromGroup = {
                viewModel.removeAppFromGroup(group.id, app.packageName, app.activityName)
            },
            onOpenDetails = {
                viewModel.dismissAppAction()
                viewModel.openAppDetails(app.packageName)
            },
            onUninstall = {
                viewModel.dismissAppAction()
                viewModel.uninstallApp(app.packageName)
            },
            iconLoader = { viewModel.getAppIcon(app) }
        )
    }
}

@Composable
private fun GroupCard(
    resolvedGroup: ResolvedAppGroup,
    groupIndex: Int,
    totalGroups: Int,
    onToggleExpand: () -> Unit,
    onEditGroup: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier
) {
    val group = resolvedGroup.group
    val groupColor = GroupIcons.parseColor(group.colorHex)
    val groupIcon = GroupIcons.getIcon(group.iconName)
    val chevronRotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 180f else 0f,
        label = "chevron_rotation"
    )
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Surface(
                onClick = onToggleExpand,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(groupColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = groupIcon,
                            contentDescription = group.name,
                            tint = groupColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Count badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${resolvedGroup.apps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Category options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEditGroup()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                            )
                            if (groupIndex > 0) {
                                DropdownMenuItem(
                                    text = { Text("Move up") },
                                    onClick = {
                                        showMenu = false
                                        onMoveUp()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) }
                                )
                            }
                            if (groupIndex < totalGroups - 1) {
                                DropdownMenuItem(
                                    text = { Text("Move down") },
                                    onClick = {
                                        showMenu = false
                                        onMoveDown()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteGroup()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Expanded Apps Content
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (resolvedGroup.apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps in this category.\nLong-press any app in the drawer to add it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    when (group.viewType) {
                        GroupViewType.GRID -> {
                            GroupGridLayout(
                                apps = resolvedGroup.apps,
                                columnCount = group.columnCount,
                                onAppClick = onAppClick,
                                onAppLongClick = onAppLongClick,
                                iconLoader = iconLoader,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                        GroupViewType.LIST -> {
                            GroupListLayout(
                                apps = resolvedGroup.apps,
                                onAppClick = onAppClick,
                                onAppLongClick = onAppLongClick,
                                iconLoader = iconLoader,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupGridLayout(
    apps: List<AppInfo>,
    columnCount: Int,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier
) {
    val chunkedApps = apps.chunked(columnCount.coerceAtLeast(1))
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedApps.forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                rowApps.forEach { app ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GroupGridItem(
                            app = app,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) },
                            iconLoader = { iconLoader(app) }
                        )
                    }
                }
                // Fill empty slots in the row
                val emptySlots = columnCount - rowApps.size
                repeat(emptySlots) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupGridItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    iconLoader: suspend () -> Drawable?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconImage(
            size = 48.dp,
            label = app.label,
            isWorkProfile = app.isWorkProfile,
            iconLoader = iconLoader
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupListLayout(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        apps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onAppClick(app) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAppLongClick(app)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    size = 40.dp,
                    label = app.label,
                    isWorkProfile = app.isWorkProfile,
                    iconLoader = { iconLoader(app) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
