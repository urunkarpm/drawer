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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
import com.urunkarpm.drawer.feature.dock.component.DockActionBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockBar(
    modifier: Modifier = Modifier,
    iconShape: Shape = RoundedCornerShape(12.dp),
    viewModel: DockViewModel = hiltViewModel(),
    isDropTarget: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shape = RoundedCornerShape(uiState.cornerRadiusDp.dp)

    val backgroundColor = when (uiState.backgroundStyle) {
        "SOLID" -> MaterialTheme.colorScheme.surfaceContainerHigh
        "TRANSPARENT" -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isDropTarget) 2.5.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isDropTarget) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        label = "dockBorderColor"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isDropTarget) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background layer: applies shape, elevation, and backdrop tint without blurring child icons
        Surface(
            shape = shape,
            color = if (isDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else backgroundColor,
            shadowElevation = if (isDropTarget) 8.dp else if (uiState.backgroundStyle == "SOLID") 4.dp else 0.dp,
            border = if (isDropTarget || uiState.backgroundStyle != "TRANSPARENT") {
                BorderStroke(animatedBorderWidth, animatedBorderColor)
            } else null,
            modifier = Modifier.matchParentSize()
        ) {}

        // Foreground content: crisp, unblurred icons
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.resolvedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .height(uiState.iconSizeDp.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Long-press any app to pin here (max 5)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                uiState.resolvedApps.forEach { (dockItem, app) ->
                    DockAppSlot(
                        dockItem = dockItem,
                        app = app,
                        iconSizeDp = uiState.iconSizeDp,
                        showLabels = uiState.showLabels,
                        isEditMode = uiState.isEditMode,
                        iconShape = iconShape,
                        onClick = {
                            if (app != null) viewModel.onAppClicked(app)
                        },
                        onLongClick = {
                            viewModel.onItemLongClicked(dockItem, app)
                        },
                        onRemove = {
                            viewModel.removeItem(dockItem.position)
                        },
                        iconLoader = {
                            if (app != null) viewModel.getAppIcon(app) else null
                        }
                    )
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
                totalItems = uiState.resolvedApps.size,
                onDismissRequest = { viewModel.dismissMenu() },
                sheetState = sheetState,
                onRemoveFromDock = { viewModel.removeItem(dockItem.position) },
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
    iconSizeDp: Float,
    showLabels: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    iconLoader: suspend () -> android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier,
    iconShape: Shape = RoundedCornerShape(12.dp)
) {
    val label = app?.label ?: dockItem.customLabel ?: dockItem.packageName
    val slotWidth = (iconSizeDp + 16).dp

    Box(
        modifier = modifier.width(slotWidth),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .semantics {
                    contentDescription = "Dock item $label, slot ${dockItem.position + 1} of 5"
                }
        ) {
            AppIconImage(
                key = app?.componentKey ?: "${dockItem.packageName}/${dockItem.activityName}",
                size = iconSizeDp.dp,
                label = label,
                isWorkProfile = app?.isWorkProfile ?: false,
                iconShape = iconShape,
                iconLoader = iconLoader
            )

            if (showLabels) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
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
