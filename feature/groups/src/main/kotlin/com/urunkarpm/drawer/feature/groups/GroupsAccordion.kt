package com.urunkarpm.drawer.feature.groups

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.urunkarpm.drawer.core.designsystem.theme.TexasTroupeFontFamily
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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

import androidx.compose.ui.graphics.Shape
import com.urunkarpm.drawer.core.designsystem.modifier.appDragSource
import com.urunkarpm.drawer.core.designsystem.modifier.autoCloseOnInactivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsAccordion(
    viewModel: GroupsViewModel,
    modifier: Modifier = Modifier,
    twoDrawersSideBySide: Boolean = false,
    iconShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    hoveredGroupId: String? = null,
    onGroupPositioned: ((String, Rect) -> Unit)? = null,
    onAppPositioned: ((String, AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((app: AppInfo, rootPosition: Offset, sourceGroupId: String) -> Unit)? = null,
    onAppDrag: ((dragAmount: Offset) -> Unit)? = null,
    onAppDragEnd: ((isDropped: Boolean) -> Unit)? = null,
    onMoveToCategory: ((AppInfo, String) -> Unit)? = null,
    onPinToDock: ((AppInfo) -> Unit)? = null,
    isDragging: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasExpandedGroup = uiState.groups.any { it.group.isExpanded }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .autoCloseOnInactivity(
                active = hasExpandedGroup,
                resetKey = uiState.groups.find { it.group.isExpanded }?.group?.id,
                timeoutMs = 5000L,
                onClose = { viewModel.collapseAllGroups() }
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (twoDrawersSideBySide) {
            val leftColumnGroups = remember(uiState.groups) {
                uiState.groups.mapIndexed { index, group -> index to group }.filterIndexed { idx, _ -> idx % 2 == 0 }
            }
            val rightColumnGroups = remember(uiState.groups) {
                uiState.groups.mapIndexed { index, group -> index to group }.filterIndexed { idx, _ -> idx % 2 == 1 }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    leftColumnGroups.forEach { (index, group) ->
                        GroupCard(
                            resolvedGroup = group,
                            groupIndex = index,
                            totalGroups = uiState.groups.size,
                            isCompact = true,
                            twoDrawersSideBySide = true,
                            isDropTarget = hoveredGroupId == group.group.id,
                            onPositioned = { rect -> onGroupPositioned?.invoke(group.group.id, rect) },
                            onAppPositioned = { app, rect -> onAppPositioned?.invoke(group.group.id, app, rect) },
                            onToggleExpand = { viewModel.toggleGroupExpanded(group.group.id) },
                            onEditGroup = { viewModel.startEditingGroup(group.group) },
                            onMoveUp = { viewModel.moveGroupUp(index) },
                            onMoveDown = { viewModel.moveGroupDown(index, uiState.groups.size) },
                            onDeleteGroup = { viewModel.deleteGroup(group.group.id) },
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onAppLongClick = { app -> viewModel.selectAppForAction(app, group.group) },
                            iconLoader = { app -> viewModel.getAppIcon(app) },
                            iconShape = iconShape,
                            onAppDragStart = if (uiState.lockLayout) null else onAppDragStart?.let { cb -> { app: AppInfo, rootPos: Offset -> cb(app, rootPos, group.group.id) } },
                            onAppDrag = if (uiState.lockLayout) null else onAppDrag,
                            onAppDragEnd = if (uiState.lockLayout) null else onAppDragEnd,
                            lockLayout = uiState.lockLayout,
                            isDragging = isDragging
                        )
                    }
                }

                // Right Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rightColumnGroups.forEach { (index, group) ->
                        GroupCard(
                            resolvedGroup = group,
                            groupIndex = index,
                            totalGroups = uiState.groups.size,
                            isCompact = true,
                            twoDrawersSideBySide = true,
                            isDropTarget = hoveredGroupId == group.group.id,
                            onPositioned = { rect -> onGroupPositioned?.invoke(group.group.id, rect) },
                            onAppPositioned = { app, rect -> onAppPositioned?.invoke(group.group.id, app, rect) },
                            onToggleExpand = { viewModel.toggleGroupExpanded(group.group.id) },
                            onEditGroup = { viewModel.startEditingGroup(group.group) },
                            onMoveUp = { viewModel.moveGroupUp(index) },
                            onMoveDown = { viewModel.moveGroupDown(index, uiState.groups.size) },
                            onDeleteGroup = { viewModel.deleteGroup(group.group.id) },
                            onAppClick = { app -> viewModel.launchApp(app) },
                            onAppLongClick = { app -> viewModel.selectAppForAction(app, group.group) },
                            iconLoader = { app -> viewModel.getAppIcon(app) },
                            iconShape = iconShape,
                            onAppDragStart = if (uiState.lockLayout) null else onAppDragStart?.let { cb -> { app: AppInfo, rootPos: Offset -> cb(app, rootPos, group.group.id) } },
                            onAppDrag = if (uiState.lockLayout) null else onAppDrag,
                            onAppDragEnd = if (uiState.lockLayout) null else onAppDragEnd,
                            lockLayout = uiState.lockLayout,
                            isDragging = isDragging
                        )
                    }
                }
            }
        } else {
            uiState.groups.forEachIndexed { index, resolvedGroup ->
                GroupCard(
                    resolvedGroup = resolvedGroup,
                    groupIndex = index,
                    totalGroups = uiState.groups.size,
                    isCompact = false,
                    twoDrawersSideBySide = false,
                    isDropTarget = hoveredGroupId == resolvedGroup.group.id,
                    onPositioned = { rect -> onGroupPositioned?.invoke(resolvedGroup.group.id, rect) },
                    onAppPositioned = { app, rect -> onAppPositioned?.invoke(resolvedGroup.group.id, app, rect) },
                    onToggleExpand = {
                        viewModel.toggleGroupExpanded(resolvedGroup.group.id)
                    },
                    onEditGroup = { viewModel.startEditingGroup(resolvedGroup.group) },
                    onMoveUp = { viewModel.moveGroupUp(index) },
                    onMoveDown = { viewModel.moveGroupDown(index, uiState.groups.size) },
                    onDeleteGroup = { viewModel.deleteGroup(resolvedGroup.group.id) },
                    onAppClick = { app ->
                        viewModel.launchApp(app)
                    },
                    onAppLongClick = { app ->
                        viewModel.selectAppForAction(app, resolvedGroup.group)
                    },
                    iconLoader = { app -> viewModel.getAppIcon(app) },
                    iconShape = iconShape,
                    onAppDragStart = if (uiState.lockLayout) null else onAppDragStart?.let { cb -> { app: AppInfo, rootPos: Offset -> cb(app, rootPos, resolvedGroup.group.id) } },
                    onAppDrag = if (uiState.lockLayout) null else onAppDrag,
                    onAppDragEnd = if (uiState.lockLayout) null else onAppDragEnd,
                    lockLayout = uiState.lockLayout,
                    isDragging = isDragging
                )
            }
        }
    }

    // Create Group Dialog
    if (uiState.showCreateDialog) {
        EditGroupDialog(
            group = null,
            allApps = uiState.allApps,
            twoDrawersSideBySide = twoDrawersSideBySide,
            onDismissRequest = { viewModel.setShowCreateDialog(false) },
            onSave = { name, iconName, colorHex, viewType, columnCount, sortOrder, selectedApps ->
                viewModel.createGroup(name, iconName, colorHex, viewType, columnCount, sortOrder, selectedApps)
            }
        )
    }

    // Edit Group Dialog
    uiState.groupBeingEdited?.let { groupToEdit ->
        EditGroupDialog(
            group = groupToEdit,
            allApps = uiState.allApps,
            twoDrawersSideBySide = twoDrawersSideBySide,
            onDismissRequest = { viewModel.dismissEditGroup() },
            onSave = { name, iconName, colorHex, viewType, columnCount, sortOrder, selectedApps ->
                viewModel.updateGroup(
                    groupToEdit.copy(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex,
                        viewType = viewType,
                        columnCount = columnCount,
                        sortOrder = sortOrder
                    ),
                    selectedApps
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
            onMoveEarlier = if (uiState.lockLayout) null else {
                { viewModel.moveAppEarlier(group.id, app) }
            },
            onMoveLater = if (uiState.lockLayout) null else {
                { viewModel.moveAppLater(group.id, app) }
            },
            onMoveToCategory = if (uiState.lockLayout) null else onMoveToCategory?.let { cb ->
                {
                    val srcGroupId = group.id
                    viewModel.dismissAppAction()
                    cb(app, srcGroupId)
                }
            },
            onPinToDock = onPinToDock?.let { cb ->
                {
                    viewModel.dismissAppAction()
                    cb(app)
                }
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
    iconShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    isCompact: Boolean = false,
    twoDrawersSideBySide: Boolean = false,
    isDropTarget: Boolean = false,
    onPositioned: ((Rect) -> Unit)? = null,
    onAppPositioned: ((AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onAppDrag: ((Offset) -> Unit)? = null,
    onAppDragEnd: ((Boolean) -> Unit)? = null,
    lockLayout: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val group = resolvedGroup.group
    val groupColor = GroupIcons.parseColor(group.colorHex)
    val groupIcon = GroupIcons.getIcon(group.iconName)
    val chevronRotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "chevron_rotation"
    )
    var showMenu by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "groupScale"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isDropTarget) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "groupBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isDropTarget) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "groupBorderColor"
    )

    val listScrollState = rememberScrollState()
    val scrollBlocker = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(0f, available.y)

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = Velocity(0f, available.y)
        }
    }

    LaunchedEffect(group.isExpanded) {
        if (!group.isExpanded) {
            listScrollState.scrollTo(0)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
            .onGloballyPositioned { coords -> onPositioned?.invoke(coords.boundsInRoot()) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDropTarget)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (isDropTarget) BorderStroke(animatedBorderWidth, animatedBorderColor) else null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Single tap to expand/collapse, long press to edit/reorder)
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = onToggleExpand,
                            onLongClick = if (!lockLayout) { { showMenu = true } } else null
                        ),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isCompact) 10.dp else 16.dp,
                                vertical = if (isCompact) 10.dp else 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 28.dp else 36.dp)
                                .clip(CircleShape)
                                .background(groupColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = groupIcon,
                                contentDescription = group.name,
                                tint = groupColor,
                                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 12.dp))

                        Text(
                            text = group.name,
                            fontFamily = TexasTroupeFontFamily,
                            style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Count badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = if (isCompact) 2.dp else 4.dp)
                        ) {
                            Text(
                                text = "${resolvedGroup.apps.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = if (isCompact) 6.dp else 8.dp,
                                    vertical = 2.dp
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(if (isCompact) 18.dp else 24.dp)
                                .rotate(chevronRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!lockLayout) {
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

            // ponytail: Silky smooth drawer expansion anchored from top with physics spring; ceiling: fixed spring constants; upgrade path: customizable motion duration.
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                if (resolvedGroup.apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCompact) "No apps in category\nLong-press app to add" else "No apps in this category.\nLong-press any app in the drawer to add it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // ponytail: bounded drawer list height with scroll blocker ensures drawer cards stay static while large lists scroll smoothly; ceiling: fixed max height (280dp/240dp); upgrade path: dynamic height calculation based on available viewport space.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isCompact) 240.dp else 280.dp)
                            .nestedScroll(scrollBlocker)
                            .verticalScroll(listScrollState, enabled = !isDragging)
                    ) {
                        // When twoDrawersSideBySide is enabled, default to LIST view mode
                        val effectiveViewType = if (twoDrawersSideBySide) {
                            GroupViewType.LIST
                        } else {
                            group.viewType
                        }
                        when (effectiveViewType) {
                            GroupViewType.GRID -> {
                                GroupGridLayout(
                                    apps = resolvedGroup.apps,
                                    columnCount = group.columnCount,
                                    onAppClick = onAppClick,
                                    onAppLongClick = onAppLongClick,
                                    iconLoader = iconLoader,
                                    iconShape = iconShape,
                                    isCompact = isCompact,
                                    onAppPositioned = onAppPositioned,
                                    onAppDragStart = onAppDragStart,
                                    onAppDrag = onAppDrag,
                                    onAppDragEnd = onAppDragEnd,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = if (isCompact) 4.dp else 8.dp,
                                            vertical = if (isCompact) 6.dp else 8.dp
                                        )
                                )
                            }
                            GroupViewType.LIST -> {
                                GroupListLayout(
                                    apps = resolvedGroup.apps,
                                    onAppClick = onAppClick,
                                    onAppLongClick = onAppLongClick,
                                    iconLoader = iconLoader,
                                    iconShape = iconShape,
                                    isCompact = isCompact,
                                    onAppPositioned = onAppPositioned,
                                    onAppDragStart = onAppDragStart,
                                    onAppDrag = onAppDrag,
                                    onAppDragEnd = onAppDragEnd,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = if (isCompact) 4.dp else 8.dp,
                                            vertical = if (isCompact) 6.dp else 8.dp
                                        )
                                )
                            }
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
    iconShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    isCompact: Boolean = false,
    onAppPositioned: ((AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onAppDrag: ((Offset) -> Unit)? = null,
    onAppDragEnd: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // ponytail: when drawers are side-by-side (isCompact), constrain columns to max 2 so app icons and labels have comfortable breathing room without crunching; ceiling is fixed 2-col; upgrade path is responsive minSize calculation.
    val effectiveColumns = if (isCompact) 2 else columnCount.coerceAtLeast(1)
    val chunkedApps = remember(apps, effectiveColumns) { apps.chunked(effectiveColumns) }
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
                    key(app.componentKey) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            GroupGridItem(
                                app = app,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) },
                                iconLoader = { iconLoader(app) },
                                iconShape = iconShape,
                                isCompact = isCompact,
                                onPositioned = onAppPositioned?.let { cb -> { rect -> cb(app, rect) } },
                                onDragStart = onAppDragStart?.let { cb -> { rootPos: Offset -> cb(app, rootPos) } },
                                onDrag = onAppDrag,
                                onDragEnd = onAppDragEnd,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                // Fill empty slots in the row
                val emptySlots = effectiveColumns - rowApps.size
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
    iconShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    isCompact: Boolean = false,
    onPositioned: ((Rect) -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val baseModifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .then(
            if (onPositioned != null) {
                Modifier.onGloballyPositioned { coords -> onPositioned(coords.boundsInRoot()) }
            } else {
                Modifier
            }
        )

    val itemModifier = if (onDragStart != null && onDrag != null && onDragEnd != null) {
        baseModifier
            .appDragSource(
                key = app.componentKey,
                onAppClick = onClick,
                onAppLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd
            )
            .padding(horizontal = if (isCompact) 2.dp else 4.dp, vertical = if (isCompact) 4.dp else 6.dp)
    } else {
        baseModifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = if (isCompact) 2.dp else 4.dp, vertical = if (isCompact) 4.dp else 6.dp)
    }

    Column(
        modifier = itemModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconImage(
            key = app.componentKey,
            size = if (isCompact) 38.dp else 48.dp,
            label = app.label,
            isWorkProfile = app.isWorkProfile,
            iconShape = iconShape,
            iconLoader = iconLoader
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
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
    iconShape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    isCompact: Boolean = false,
    onAppPositioned: ((AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onAppDrag: ((Offset) -> Unit)? = null,
    onAppDragEnd: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        apps.forEach { app ->
            key(app.componentKey) {
                val baseModifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (onAppPositioned != null) {
                            Modifier.onGloballyPositioned { coords -> onAppPositioned(app, coords.boundsInRoot()) }
                        } else {
                            Modifier
                        }
                    )

                val rowModifier = if (onAppDragStart != null && onAppDrag != null && onAppDragEnd != null) {
                    baseModifier
                        .appDragSource(
                            key = app.componentKey,
                            onAppClick = { onAppClick(app) },
                            onAppLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppLongClick(app)
                            },
                            onDragStart = { rootPos -> onAppDragStart(app, rootPos) },
                            onDrag = onAppDrag,
                            onDragEnd = onAppDragEnd
                        )
                        .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = if (isCompact) 4.dp else 6.dp)
                } else {
                    baseModifier
                        .combinedClickable(
                            onClick = { onAppClick(app) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppLongClick(app)
                            }
                        )
                        .padding(horizontal = if (isCompact) 4.dp else 8.dp, vertical = if (isCompact) 4.dp else 6.dp)
                }

                Row(
                    modifier = rowModifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(
                        key = app.componentKey,
                        size = if (isCompact) 32.dp else 40.dp,
                        label = app.label,
                        isWorkProfile = app.isWorkProfile,
                        iconShape = iconShape,
                        iconLoader = { iconLoader(app) }
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                    Text(
                        text = app.label,
                        style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


