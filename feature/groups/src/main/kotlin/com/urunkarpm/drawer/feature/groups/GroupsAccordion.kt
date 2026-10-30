package com.urunkarpm.drawer.feature.groups

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import com.urunkarpm.drawer.core.designsystem.theme.LocalAppLabelScale
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.designsystem.component.DrawerSurfaceBackground
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.GroupViewType
import com.urunkarpm.drawer.feature.groups.component.EditGroupDialog
import com.urunkarpm.drawer.feature.groups.component.GroupItemActionBottomSheet
import com.urunkarpm.drawer.feature.groups.util.GroupIcons

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import com.urunkarpm.drawer.core.designsystem.modifier.appDragSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsAccordion(
    viewModel: GroupsViewModel,
    modifier: Modifier = Modifier,
    twoDrawersSideBySide: Boolean = false,
    iconShape: Shape = CircleShape,
    hoveredGroupId: String? = null,
    notificationPackages: Set<String> = emptySet(),
    onGroupPositioned: ((String, Rect) -> Unit)? = null,
    onAppPositioned: ((String, AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((app: AppInfo, rootPosition: Offset, sourceGroupId: String) -> Unit)? = null,
    onAppDrag: ((dragAmount: Offset) -> Unit)? = null,
    onAppDragEnd: ((isDropped: Boolean) -> Unit)? = null,
    onMoveToCategory: ((AppInfo, String) -> Unit)? = null,
    onPinToDock: ((AppInfo) -> Unit)? = null,
    isDragging: Boolean = false,
    hideDrawerIcons: Boolean = false,
    surfaceThemeStyle: String = "LIQUID_GLASS",
    isMesmerizingGlow: Boolean = true,
    surfaceOpacity: Float = 0.65f,
    surfaceStrokeOpacity: Float = 0.40f,
    cinematicPaletteId: String = "DEFAULT",
    onToggleExpand: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasExpandedGroup = uiState.groups.any { it.group.isExpanded }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hasExpandedGroup, lastInteractionTime) {
        if (hasExpandedGroup) {
            delay(3000L)
            viewModel.collapseAllGroups()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompactEffective = twoDrawersSideBySide && screenWidthDp < 600

    // ponytail: Auto-collapse category app drawers after 3 seconds of inactivity; resets countdown on any touch interaction; ceiling: fixed 3s delay; upgrade path: customizable timeout in Settings.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(hasExpandedGroup) {
                if (!hasExpandedGroup) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        val now = android.os.SystemClock.uptimeMillis()
                        if (now - lastInteractionTime > 200L) {
                            lastInteractionTime = now
                        }
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (uiState.groups.isEmpty()) {
            Surface(
                onClick = { viewModel.setShowCreateDialog(true) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Category Drawer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to create your first category drawer and organize your apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

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
                        key(group.group.id) {
                            GroupCard(
                                resolvedGroup = group,
                                groupIndex = index,
                                totalGroups = uiState.groups.size,
                                isCompact = isCompactEffective,
                                twoDrawersSideBySide = true,
                                isDropTarget = hoveredGroupId == group.group.id,
                                hasNotification = group.apps.any { it.packageName in notificationPackages },
                                drawerNotificationGlow = uiState.drawerNotificationGlow,
                                notificationPackages = notificationPackages,
                                onPositioned = { rect -> onGroupPositioned?.invoke(group.group.id, rect) },
                                onAppPositioned = { app, rect -> onAppPositioned?.invoke(group.group.id, app, rect) },
                                onToggleExpand = { onToggleExpand?.invoke(group.group.id) ?: viewModel.toggleGroupExpanded(group.group.id) },
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
                                isDragging = isDragging,
                                hideDrawerIcons = hideDrawerIcons,
                                surfaceThemeStyle = surfaceThemeStyle,
                                isMesmerizingGlow = isMesmerizingGlow,
                                surfaceOpacity = surfaceOpacity,
                                surfaceStrokeOpacity = surfaceStrokeOpacity,
                                drawerTitleAnimation = uiState.drawerTitleAnimation,
                                cinematicPaletteId = cinematicPaletteId
                            )
                        }
                    }
                }

                // Right Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rightColumnGroups.forEach { (index, group) ->
                        key(group.group.id) {
                            GroupCard(
                                resolvedGroup = group,
                                groupIndex = index,
                                totalGroups = uiState.groups.size,
                                isCompact = isCompactEffective,
                                twoDrawersSideBySide = true,
                                isDropTarget = hoveredGroupId == group.group.id,
                                hasNotification = group.apps.any { it.packageName in notificationPackages },
                                drawerNotificationGlow = uiState.drawerNotificationGlow,
                                notificationPackages = notificationPackages,
                                onPositioned = { rect -> onGroupPositioned?.invoke(group.group.id, rect) },
                                onAppPositioned = { app, rect -> onAppPositioned?.invoke(group.group.id, app, rect) },
                                onToggleExpand = { onToggleExpand?.invoke(group.group.id) ?: viewModel.toggleGroupExpanded(group.group.id) },
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
                                isDragging = isDragging,
                                hideDrawerIcons = hideDrawerIcons,
                                surfaceThemeStyle = surfaceThemeStyle,
                                isMesmerizingGlow = isMesmerizingGlow,
                                surfaceOpacity = surfaceOpacity,
                                surfaceStrokeOpacity = surfaceStrokeOpacity,
                                drawerTitleAnimation = uiState.drawerTitleAnimation,
                                cinematicPaletteId = cinematicPaletteId
                            )
                        }
                    }
                }
            }
        } else {
            uiState.groups.forEachIndexed { index, resolvedGroup ->
                key(resolvedGroup.group.id) {
                    GroupCard(
                        resolvedGroup = resolvedGroup,
                        groupIndex = index,
                        totalGroups = uiState.groups.size,
                        isCompact = false,
                        twoDrawersSideBySide = false,
                        isDropTarget = hoveredGroupId == resolvedGroup.group.id,
                        hasNotification = resolvedGroup.apps.any { it.packageName in notificationPackages },
                        drawerNotificationGlow = uiState.drawerNotificationGlow,
                        notificationPackages = notificationPackages,
                        onPositioned = { rect -> onGroupPositioned?.invoke(resolvedGroup.group.id, rect) },
                        onAppPositioned = { app, rect -> onAppPositioned?.invoke(resolvedGroup.group.id, app, rect) },
                        onToggleExpand = {
                            onToggleExpand?.invoke(resolvedGroup.group.id) ?: viewModel.toggleGroupExpanded(resolvedGroup.group.id)
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
                        isDragging = isDragging,
                        hideDrawerIcons = hideDrawerIcons,
                        surfaceThemeStyle = surfaceThemeStyle,
                        isMesmerizingGlow = isMesmerizingGlow,
                        surfaceOpacity = surfaceOpacity,
                        surfaceStrokeOpacity = surfaceStrokeOpacity,
                        drawerTitleAnimation = uiState.drawerTitleAnimation
                    )
                }
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
    iconShape: Shape = CircleShape,
    isCompact: Boolean = false,
    twoDrawersSideBySide: Boolean = false,
    isDropTarget: Boolean = false,
    hasNotification: Boolean = false,
    drawerNotificationGlow: Boolean = true,
    notificationPackages: Set<String> = emptySet(),
    onPositioned: ((Rect) -> Unit)? = null,
    onAppPositioned: ((AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onAppDrag: ((Offset) -> Unit)? = null,
    onAppDragEnd: ((Boolean) -> Unit)? = null,
    lockLayout: Boolean = false,
    isDragging: Boolean = false,
    hideDrawerIcons: Boolean = false,
    surfaceThemeStyle: String = "LIQUID_GLASS",
    isMesmerizingGlow: Boolean = true,
    surfaceOpacity: Float = 0.65f,
    surfaceStrokeOpacity: Float = 0.40f,
    drawerTitleAnimation: String = "ACCENT_POP",
    cinematicPaletteId: String = "DEFAULT",
    modifier: Modifier = Modifier
) {
    val group = resolvedGroup.group
    val isExpanded = group.isExpanded
    val haptic = LocalHapticFeedback.current
    val groupColor = remember(group.colorHex, group.name, group.id) {
        if (!group.colorHex.isNullOrBlank() && group.colorHex != "#6750A4") {
            GroupIcons.parseColor(group.colorHex, fallback = GroupIcons.resolveCategoryColor(group.name, group.id))
        } else {
            GroupIcons.resolveCategoryColor(group.name, group.id)
        }
    }
    val groupIcon = GroupIcons.getIcon(group.iconName)

    // ponytail: Drawer title lateral slide animation options. When expanded, titles glide smoothly to the right edge.
    // Ceiling: fixed 5 animation modes; Upgrade path: custom user spring stiffness and physics sliders.
    val isTitleAnimated = drawerTitleAnimation != "OFF"
    val targetBias = if (isExpanded && isTitleAnimated) 1f else -1f

    val biasAnimationSpec: AnimationSpec<Float> = when (drawerTitleAnimation) {
        "FLUID_SPRING" -> spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        "KINETIC_DECEL" -> tween(durationMillis = 350, easing = CubicBezierEasing(0.05f, 0.9f, 0.1f, 1f))
        "SNAPPY" -> if (isExpanded) {
            spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        }
        "ACCENT_POP" -> spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        else -> snap()
    }

    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = biasAnimationSpec,
        label = "drawerTitleBias"
    )

    val titleColor by animateColorAsState(
        targetValue = if (isExpanded && drawerTitleAnimation == "ACCENT_POP") {
            groupColor
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 280),
        label = "drawerTitleColor"
    )

    val titleScale by animateFloatAsState(
        targetValue = if (isExpanded && drawerTitleAnimation == "ACCENT_POP") 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "drawerTitleScale"
    )

    val notifyingPkgs = remember(resolvedGroup.apps, notificationPackages) {
        resolvedGroup.apps.map { it.packageName }.filter { it in notificationPackages }.toSet()
    }
    // ponytail: in-memory drawer notification acknowledgment state; ceiling: resets across process restarts; upgrade path: persist in Room/DataStore.
    var acknowledgedPkgs by remember(group.id) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(notifyingPkgs) {
        if (notifyingPkgs.isEmpty()) {
            acknowledgedPkgs = emptySet()
        } else {
            acknowledgedPkgs = acknowledgedPkgs.intersect(notifyingPkgs)
        }
    }

    LaunchedEffect(isExpanded, notifyingPkgs) {
        if (isExpanded && notifyingPkgs.isNotEmpty()) {
            delay(1500L)
            acknowledgedPkgs = notifyingPkgs
        }
    }

    val hasUnacknowledgedNotification = notifyingPkgs.any { it !in acknowledgedPkgs }
    val isGlowing = drawerNotificationGlow && hasUnacknowledgedNotification

    val infiniteTransition = rememberInfiniteTransition(label = "drawerGlowPulse")
    val pulseScale by if (isGlowing) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    val pulseAlpha by if (isGlowing) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0.40f) }
    }

    val handleAppClick: (AppInfo) -> Unit = { app ->
        acknowledgedPkgs = notifyingPkgs
        onAppClick(app)
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 380f
        ),
        label = "chevron_rotation"
    )
    val chevronBgColor by animateColorAsState(
        targetValue = if (isExpanded) groupColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "chevronBgColor"
    )
    var showMenu by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "groupScale"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isDropTarget) 2.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "groupBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isDropTarget -> MaterialTheme.colorScheme.primary
            isExpanded -> groupColor.copy(alpha = 0.35f)
            hasNotification -> groupColor.copy(alpha = 0.30f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        },
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "groupBorderColor"
    )

    val listScrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(group.isExpanded) {
        if (!group.isExpanded) {
            listScrollState.scrollTo(0)
        } else {
            // ponytail: smoothly bring expanded category drawer into full view above dock without layout jitter; ceiling: double-settle delay (120ms/160ms); upgrade path: AnimatedVisibility progress listener.
            delay(120)
            bringIntoViewRequester.bringIntoView()
            delay(160)
            bringIntoViewRequester.bringIntoView()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
            .onGloballyPositioned { coords -> onPositioned?.invoke(coords.boundsInRoot()) }
    ) {
        DrawerSurfaceBackground(
            surfaceThemeStyle = surfaceThemeStyle,
            shape = RoundedCornerShape(16.dp),
            isExpanded = isExpanded,
            accentColor = if (isExpanded || hasNotification) groupColor else null,
            isDropTarget = isDropTarget,
            isMesmerizingGlow = isMesmerizingGlow,
            surfaceOpacity = surfaceOpacity,
            surfaceStrokeOpacity = surfaceStrokeOpacity,
            cinematicPaletteId = cinematicPaletteId,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (isExpanded) {
                            listOf(groupColor.copy(alpha = 0.04f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, Color.Transparent)
                        }
                    )
                )
        ) {
            // Header Row (Single tap to expand/collapse, long press to edit/reorder)
            val headerInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = headerInteractionSource,
                        indication = null,
                        onClick = {
                            // ponytail: crisp native tactile haptic feedback on category drawer snap/toggle; ceiling: TextHandleMove platform standard; upgrade path: custom vibration pattern.
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            acknowledgedPkgs = notifyingPkgs
                            onToggleExpand()
                        },
                        onLongClick = if (!lockLayout) { { showMenu = true } } else null
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isCompact) 10.dp else 14.dp,
                            vertical = if (isCompact) 10.dp else 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hideDrawerIcons) {
                        // Category Icon Badge with glowing pulse if unacknowledged notification is active
                        Box(
                            modifier = Modifier.size(if (isCompact) 30.dp else 34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(groupColor.copy(alpha = 0.12f))
                                    .border(
                                        width = if (hasNotification) 1.5.dp else 1.dp,
                                        color = if (hasNotification) groupColor.copy(alpha = 0.50f)
                                                else groupColor.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // ponytail: inner drawer icon glowing pulse with category color; ceiling: ambient/spot shadow and radial halo; upgrade path: RenderEffect blur.
                                if (isGlowing) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCompact) 28.dp else 32.dp)
                                            .drawBehind {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            groupColor.copy(alpha = pulseAlpha * 0.70f),
                                                            groupColor.copy(alpha = pulseAlpha * 0.25f),
                                                            Color.Transparent
                                                        ),
                                                        center = center,
                                                        radius = size.minDimension / 1.1f
                                                    )
                                                )
                                            }
                                    )
                                    Icon(
                                        imageVector = groupIcon,
                                        contentDescription = null,
                                        tint = groupColor,
                                        modifier = Modifier
                                            .size(if (isCompact) 16.dp else 18.dp)
                                            .graphicsLayer {
                                                scaleX = pulseScale
                                                scaleY = pulseScale
                                                alpha = pulseAlpha * 0.85f
                                                ambientShadowColor = groupColor
                                                spotShadowColor = groupColor
                                                shadowElevation = 8.dp.toPx()
                                            }
                                    )
                                }
                                Icon(
                                    imageVector = groupIcon,
                                    contentDescription = group.name,
                                    tint = groupColor,
                                    modifier = Modifier
                                        .size(if (isCompact) 16.dp else 18.dp)
                                        .graphicsLayer {
                                            if (isGlowing) {
                                                scaleX = 1f + 0.08f * (pulseScale - 1f) / 0.15f
                                                scaleY = 1f + 0.08f * (pulseScale - 1f) / 0.15f
                                            }
                                        }
                                )
                                // ponytail: Notification indicator dot displays only when drawer glowing is disabled to avoid visual clutter and duplicate indicator states; ceiling: static dot; upgrade path: numeric count badge.
                                if (hasNotification && !drawerNotificationGlow) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(if (isCompact) 2.dp else 2.5.dp)
                                            .size(if (isCompact) 6.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                    } else if (hasNotification && !drawerNotificationGlow) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 7.dp else 8.dp)
                                .clip(CircleShape)
                                .background(groupColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = BiasAlignment(horizontalBias = animatedBias, verticalBias = 0f)
                    ) {
                        Text(
                            text = group.name,
                            fontFamily = LocalHeaderFontFamily.current,
                            style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                                transformOrigin = TransformOrigin(if (animatedBias > 0f) 1f else 0f, 0.5f)
                            }
                        )
                    }

                    // Tactile circular touch backing for chevron
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 26.dp else 28.dp)
                            .clip(CircleShape)
                            .background(chevronBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(if (isCompact) 18.dp else 20.dp)
                                .graphicsLayer { rotationZ = chevronRotation },
                            tint = if (isExpanded) groupColor else MaterialTheme.colorScheme.onSurfaceVariant
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

            // Aesthetic open and close animation with organic spring physics
            // ponytail: clipToBounds + top vertical expansion prevents app icons from colliding into the drawer header
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 360f
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 260,
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
                ),
                modifier = Modifier.clipToBounds()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = if (isCompact) 10.dp else 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (resolvedGroup.apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCompact) "No applications in category\nLong-press app to assign" else "No applications assigned to this workspace category.\nLong-press any app in the drawer to assign it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val configuration = LocalConfiguration.current
                    val maxGroupHeight = if (configuration.screenHeightDp >= 800) 420.dp else if (isCompact) 240.dp else 280.dp
                    // ponytail: in single drawer mode, render apps directly in the parent vertical scroll container without height capping or inner scrolling to prevent nested scroll traps and dock occlusion; in two-column mode, constrain height to balance columns; ceiling: 420dp/280dp/240dp in side-by-side mode; upgrade path: dynamic viewport-aware height balancing.
                    val contentModifier = if (twoDrawersSideBySide) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxGroupHeight)
                            .verticalScroll(listScrollState, enabled = !isDragging)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    Box(
                        modifier = contentModifier
                    ) {
                        // When drawers are compact on small screens, default to LIST view mode
                        val effectiveViewType = if (isCompact) {
                            GroupViewType.LIST
                        } else {
                            group.viewType
                        }
                        when (effectiveViewType) {
                            GroupViewType.GRID -> {
                                GroupGridLayout(
                                    apps = resolvedGroup.apps,
                                    columnCount = group.columnCount,
                                    onAppClick = handleAppClick,
                                    onAppLongClick = onAppLongClick,
                                    iconLoader = iconLoader,
                                    iconShape = iconShape,
                                    isCompact = isCompact,
                                    notificationPackages = notificationPackages,
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
                                onAppClick = handleAppClick,
                                onAppLongClick = onAppLongClick,
                                iconLoader = iconLoader,
                                iconShape = iconShape,
                                isCompact = isCompact,
                                notificationPackages = notificationPackages,
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
}

@Composable
private fun GroupGridLayout(
    apps: List<AppInfo>,
    columnCount: Int,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    iconShape: Shape = CircleShape,
    isCompact: Boolean = false,
    notificationPackages: Set<String> = emptySet(),
    onAppPositioned: ((AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onAppDrag: ((Offset) -> Unit)? = null,
    onAppDragEnd: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val effectiveColumns = if (isCompact) 2 else columnCount.coerceIn(2, 6)
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
                                hasNotification = app.packageName in notificationPackages,
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
    hasNotification: Boolean = false,
    onPositioned: ((Rect) -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val labelScale = LocalAppLabelScale.current

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
        Box(
            contentAlignment = Alignment.Center
        ) {
            AppIconImage(
                key = app.componentKey,
                size = if (isCompact) 38.dp else 48.dp,
                label = app.label,
                isWorkProfile = app.isWorkProfile,
                iconShape = iconShape,
                iconLoader = iconLoader
            )
            if (hasNotification) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(1.dp)
                        .size(if (isCompact) 7.5.dp else 9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(1.2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize * labelScale),
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
    iconShape: Shape = CircleShape,
    isCompact: Boolean = false,
    notificationPackages: Set<String> = emptySet(),
    onAppPositioned: ((AppInfo, Rect) -> Unit)? = null,
    onAppDragStart: ((AppInfo, Offset) -> Unit)? = null,
    onAppDrag: ((Offset) -> Unit)? = null,
    onAppDragEnd: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val labelScale = LocalAppLabelScale.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        apps.forEach { app ->
            key(app.componentKey) {
                val hasNotification = app.packageName in notificationPackages
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
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        AppIconImage(
                            key = app.componentKey,
                            size = if (isCompact) 32.dp else 40.dp,
                            label = app.label,
                            isWorkProfile = app.isWorkProfile,
                            iconShape = iconShape,
                            iconLoader = { iconLoader(app) }
                        )
                        if (hasNotification) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(if (isCompact) 6.5.dp else 7.5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(1.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                    val baseStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                    Text(
                        text = app.label,
                        style = baseStyle.copy(fontSize = baseStyle.fontSize * labelScale),
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


