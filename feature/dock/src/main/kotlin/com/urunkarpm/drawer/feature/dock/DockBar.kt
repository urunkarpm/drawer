package com.urunkarpm.drawer.feature.dock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.data.util.DevicePostureUtils
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.designsystem.theme.LocalAppLabelScale
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
import com.urunkarpm.drawer.feature.dock.component.DockActionBottomSheet
import com.urunkarpm.drawer.core.designsystem.component.DockSurfaceBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockBar(
    modifier: Modifier = Modifier,
    iconShape: Shape = CircleShape,
    viewModel: DockViewModel = hiltViewModel(),
    isDropTarget: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // ponytail: Dynamic dock capacity dynamically adjusts based on foldable hinge posture and screen mode; ceiling: 5 apps when folded, up to dockExpandedMaxApps when unfolded/tablet; upgrade path: continuous multi-breakpoint dock layout.
    val isOpenFoldableOrTablet = remember(context, configuration) {
        DevicePostureUtils.isOpenFoldableOrTablet(context, configuration)
    }
    val isFoldableOrTablet = remember(context) {
        DevicePostureUtils.isFoldableOrTablet(context)
    }
    val maxCapacity = if (isFoldableOrTablet) uiState.dockExpandedMaxApps else 5
    val visibleApps = remember(uiState.resolvedApps, isOpenFoldableOrTablet, uiState.dockExpandedMaxApps) {
        if (!isOpenFoldableOrTablet) {
            uiState.resolvedApps.take(5)
        } else {
            uiState.resolvedApps.take(uiState.dockExpandedMaxApps)
        }
    }

    // ponytail: dock is 100% curved (pill/capsule) for sleek modern styling; ceiling: fixed capsule geometry; upgrade path: customizable dock corner slider in settings.
    val shape = CircleShape

    val animatedScale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (visibleApps.isEmpty()) {
            val emptyDockWidth = 280.dp.coerceAtMost(maxWidth)
            Box(
                modifier = Modifier
                    .width(emptyDockWidth)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    },
                contentAlignment = Alignment.Center
            ) {
                DockSurfaceBackground(
                    backgroundStyle = uiState.backgroundStyle,
                    shape = shape,
                    surfaceOpacity = uiState.surfaceOpacity,
                    surfaceBlurRadius = uiState.surfaceBlurRadius,
                    surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                    isDropTarget = isDropTarget,
                    isMesmerizingGlow = uiState.mesmerizingGlow,
                    cinematicPaletteId = uiState.cinematicPalette,
                    modifier = Modifier.matchParentSize()
                )

                Box(
                    modifier = Modifier
                        .height(uiState.iconSizeDp.dp.coerceAtLeast(48.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Long-press any app to pin here (max $maxCapacity)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val itemCount = visibleApps.size
            val targetIconSizeDp = uiState.iconSizeDp.dp
            val dockPaddingHorizontal = 12.dp
            val maxInnerWidth = (maxWidth - (dockPaddingHorizontal * 2)).coerceAtLeast(0.dp)

            // Slot horizontal breathing room around icon
            val slotPadding = 6.dp
            val idealSpacing = if (itemCount <= 3) 16.dp else 10.dp
            val minSpacing = 4.dp

            val totalIdealWidth = (targetIconSizeDp + slotPadding) * itemCount + idealSpacing * (itemCount - 1)

            val (effectiveIconSize, actualSpacing, rowWidth) = if (maxInnerWidth > 0.dp && totalIdealWidth <= maxInnerWidth) {
                // Fits at requested target size with ideal spacing
                Triple(targetIconSizeDp, idealSpacing, totalIdealWidth)
            } else if (maxInnerWidth > 0.dp) {
                // Need to fit within maxInnerWidth without any overflow or clipping
                val gapCount = (itemCount - 1).coerceAtLeast(1)
                val remainingForGaps = maxInnerWidth - ((targetIconSizeDp + slotPadding) * itemCount)
                val calculatedGap = if (gapCount > 0) remainingForGaps / gapCount else 0.dp

                if (calculatedGap >= minSpacing) {
                    // Can keep target icon size by reducing gap spacing
                    Triple(targetIconSizeDp, calculatedGap, maxInnerWidth)
                } else {
                    // Uniformly scale down icon size so ALL items fit with identical size and spacing
                    val spacingUsed = if (itemCount > 1) minSpacing else 0.dp
                    val totalGap = spacingUsed * (itemCount - 1)
                    val widthPerSlot = (maxInnerWidth - totalGap).coerceAtLeast(0.dp) / itemCount
                    val scaledIconSize = (widthPerSlot - slotPadding).coerceIn(24.dp, targetIconSizeDp)
                    Triple(scaledIconSize, spacingUsed, maxInnerWidth)
                }
            } else {
                Triple(targetIconSizeDp, idealSpacing, targetIconSizeDp * itemCount)
            }

            val dockWidth = (rowWidth + (dockPaddingHorizontal * 2)).coerceAtMost(maxWidth)

            val localDensity = LocalDensity.current
            val haptic = LocalHapticFeedback.current

            var draggedSlotIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffsetPx by remember { mutableFloatStateOf(0f) }
            var currentTargetIndex by remember { mutableStateOf<Int?>(null) }

            val slotWidthDp = (rowWidth - (actualSpacing * (itemCount - 1).coerceAtLeast(0))).coerceAtLeast(0.dp) / itemCount
            val slotPitchDp = slotWidthDp + actualSpacing
            val slotPitchPx = with(localDensity) { slotPitchDp.toPx() }

            // Real-time target slot calculation
            val activeTargetIndex = if (draggedSlotIndex != null && slotPitchPx > 0f) {
                val displacement = (dragOffsetPx / slotPitchPx).roundToInt()
                (draggedSlotIndex!! + displacement).coerceIn(0, itemCount - 1)
            } else {
                null
            }

            LaunchedEffect(activeTargetIndex) {
                if (activeTargetIndex != null && activeTargetIndex != currentTargetIndex) {
                    if (currentTargetIndex != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    currentTargetIndex = activeTargetIndex
                }
            }

            Box(
                modifier = Modifier
                    .width(dockWidth)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    },
                contentAlignment = Alignment.Center
            ) {
                DockSurfaceBackground(
                    backgroundStyle = uiState.backgroundStyle,
                    shape = shape,
                    surfaceOpacity = uiState.surfaceOpacity,
                    surfaceBlurRadius = uiState.surfaceBlurRadius,
                    surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                    isDropTarget = isDropTarget,
                    isMesmerizingGlow = uiState.mesmerizingGlow,
                    cinematicPaletteId = uiState.cinematicPalette,
                    modifier = Modifier.matchParentSize()
                )

                // Foreground content: crisp, unblurred icons uniformly scaled and distributed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dockPaddingHorizontal, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(actualSpacing, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleApps.forEachIndexed { index, (dockItem, app) ->
                    key(dockItem.position, dockItem.packageName) {
                        val isBeingDragged = (draggedSlotIndex == index)

                        // Auto-arrange animation shift:
                        val targetShiftPx = when {
                            draggedSlotIndex == null -> 0f
                            !uiState.autoArrangeApps -> 0f
                            isBeingDragged -> 0f
                            activeTargetIndex != null && activeTargetIndex > draggedSlotIndex!! && index > draggedSlotIndex!! && index <= activeTargetIndex -> -slotPitchPx
                            activeTargetIndex != null && activeTargetIndex < draggedSlotIndex!! && index < draggedSlotIndex!! && index >= activeTargetIndex -> slotPitchPx
                            else -> 0f
                        }

                        val animatedShiftPx by animateFloatAsState(
                            targetValue = targetShiftPx,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "dockSlotShift_$index"
                        )

                        // ponytail: dock slot shift reads translation lambda inside graphicsLayer; ceiling: slot shifts in X only; upgrade path: 2D fluid grid physics.
                        DockAppSlot(
                            dockItem = dockItem,
                            app = app,
                            iconSize = effectiveIconSize,
                            showLabels = uiState.showLabels,
                            isEditMode = uiState.isEditMode,
                            autoArrangeEnabled = uiState.autoArrangeApps && itemCount > 1,
                            isBeingDragged = isBeingDragged,
                            dragTranslationX = { if (isBeingDragged) dragOffsetPx else animatedShiftPx },
                            iconShape = iconShape,
                            maxCapacity = maxCapacity,
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (isBeingDragged) 10f else 1f),
                            onClick = {
                                if (uiState.isEditMode) {
                                    viewModel.setEditMode(false)
                                } else {
                                    viewModel.onAppClicked(dockItem, app)
                                }
                            },
                            onLongClick = {
                                viewModel.onItemLongClicked(dockItem, app)
                            },
                            onDragStart = {
                                draggedSlotIndex = index
                                dragOffsetPx = 0f
                                currentTargetIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { dx ->
                                dragOffsetPx += dx
                            },
                            onDragEnd = {
                                val fromIdx = draggedSlotIndex
                                val toIdx = currentTargetIndex
                                if (fromIdx != null && toIdx != null && fromIdx != toIdx && toIdx in 0 until itemCount) {
                                    val fromPos = visibleApps[fromIdx].first.position
                                    val toPos = visibleApps[toIdx].first.position
                                    viewModel.reorderDock(fromPos, toPos)
                                } else if (fromIdx != null && toIdx != null && fromIdx == toIdx) {
                                    visibleApps.getOrNull(fromIdx)?.let { (item, app) ->
                                        viewModel.onItemLongClicked(item, app)
                                    }
                                }
                                draggedSlotIndex = null
                                dragOffsetPx = 0f
                                currentTargetIndex = null
                            },
                            onRemove = {
                                viewModel.removeItem(dockItem.position)
                            },
                            iconLoader = {
                                if (app != null) {
                                    viewModel.getAppIcon(app)
                                } else {
                                    viewModel.getAppIcon(
                                        AppInfo(
                                            packageName = dockItem.packageName,
                                            activityName = dockItem.activityName,
                                            label = dockItem.customLabel ?: dockItem.packageName,
                                            userHandleId = dockItem.userHandleId
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

    // Context Sheet for Dock Item
    val selectedPair = uiState.selectedDockItemForMenu
    if (selectedPair != null) {
        val (dockItem, app) = selectedPair
        DockActionBottomSheet(
            dockItem = dockItem,
            app = app,
            totalItems = visibleApps.size,
            onDismissRequest = { viewModel.dismissMenu() },
            sheetState = sheetState,
            onRemoveFromDock = { viewModel.removeItem(dockItem.position) },
            onToggleEditMode = {
                viewModel.toggleEditMode()
                viewModel.dismissMenu()
            },
            onMoveLeft = { viewModel.moveItemLeft(dockItem.position) },
            onMoveRight = { viewModel.moveItemRight(dockItem.position) },
            onOpenDetails = {
                if (app != null) viewModel.openAppDetails(app.packageName)
            },
            onUninstall = {
                if (app != null) viewModel.uninstallApp(app.packageName)
            },
            iconLoader = {
                if (app != null) viewModel.getAppIcon(app) else null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockAppSlot(
    dockItem: DockItem,
    app: AppInfo?,
    iconSize: Dp,
    showLabels: Boolean,
    isEditMode: Boolean,
    autoArrangeEnabled: Boolean,
    isBeingDragged: Boolean,
    dragTranslationX: () -> Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onRemove: () -> Unit,
    iconLoader: suspend () -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
    iconShape: Shape = CircleShape,
    maxCapacity: Int = 5
) {
    val haptic = LocalHapticFeedback.current
    val labelScale = LocalAppLabelScale.current
    val label = app?.label ?: dockItem.customLabel ?: dockItem.packageName

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    // ponytail: Long-press drag enables native reordering across dock slots, while simple taps launch immediately; ceiling: 1D horizontal slot drag; upgrade path: freeform 2D gesture drag.
    val gestureModifier = if (autoArrangeEnabled) {
        Modifier
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectHorizontalDragGestures(
                        onDragStart = { currentOnDragStart() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount)
                        },
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() }
                    )
                } else {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { currentOnDragStart() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount.x)
                        },
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() }
                    )
                }
            }
            .clickable(onClick = currentOnClick)
    } else {
        Modifier.combinedClickable(
            onClick = currentOnClick,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnLongClick()
            }
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = dragTranslationX()
                if (isBeingDragged) {
                    scaleX = 1.15f
                    scaleY = 1.15f
                    shadowElevation = 16f
                }
            },
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .then(gestureModifier)
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .semantics {
                    contentDescription = "Dock item $label, slot ${dockItem.position + 1} of $maxCapacity"
                }
        ) {
            AppIconImage(
                key = app?.componentKey ?: "${dockItem.packageName}/${dockItem.activityName}",
                size = iconSize,
                label = label,
                isWorkProfile = app?.isWorkProfile ?: false,
                iconShape = iconShape,
                iconLoader = iconLoader
            )

            if (showLabels) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp * labelScale),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Edit Mode Remove Badge
        AnimatedVisibility(
            visible = isEditMode,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from dock",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
