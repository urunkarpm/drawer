package com.urunkarpm.drawer.feature.home.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.model.WidgetDrawer
import com.urunkarpm.drawer.core.model.WidgetItem
import com.urunkarpm.drawer.feature.groups.util.GroupIcons

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetDrawerCard(
    drawer: WidgetDrawer,
    widgetHostManager: DrawerWidgetHostManager,
    onToggleExpand: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onEditDrawer: () -> Unit = {},
    onDeleteDrawer: (deleteChildren: Boolean) -> Unit,
    onDeleteWidget: (id: String, appWidgetId: Int) -> Unit,
    onExtractWidget: (id: String) -> Unit,
    onResizeWidget: (widget: WidgetItem) -> Unit,
    onReorderWidget: (fromIndex: Int, toIndex: Int) -> Unit,
    drawerIndex: Int = 0,
    totalDrawers: Int = 1,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    lockLayout: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val drawerColor = remember(drawer.colorHex, drawer.title, drawer.id) {
        GroupIcons.parseColor(drawer.colorHex, fallback = resolveWidgetDrawerColor(drawer.title, drawer.id))
    }
    val drawerIcon = remember(drawer.iconName, drawer.title) {
        if (drawer.iconName.isNotBlank() && drawer.iconName != "folder") {
            GroupIcons.getIcon(drawer.iconName)
        } else {
            resolveWidgetDrawerIcon(drawer.title)
        }
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (drawer.isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 380f
        ),
        label = "chevron_rotation"
    )
    val chevronBgColor by animateColorAsState(
        targetValue = if (drawer.isExpanded) drawerColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "chevronBgColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (drawer.isExpanded) drawerColor.copy(alpha = 0.42f) else drawerColor.copy(alpha = 0.22f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "drawerBorderColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (drawer.isExpanded)
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, animatedBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            drawerColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            // Header Row (Single tap to expand/collapse, long press to open options)
            val headerInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = headerInteractionSource,
                        indication = null,
                        onClick = onToggleExpand,
                        onLongClick = if (!lockLayout) { { showMenu = true } } else null
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Icon Badge (squircle RoundedCornerShape(12.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(drawerColor.copy(alpha = 0.18f))
                            .border(
                                width = 1.dp,
                                color = drawerColor.copy(alpha = 0.30f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = drawerIcon,
                            contentDescription = drawer.title,
                            tint = drawerColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = drawer.title,
                            fontFamily = LocalHeaderFontFamily.current,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val widgetCount = drawer.widgets.size
                        Text(
                            text = if (widgetCount == 1) "1 widget" else "$widgetCount widgets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!lockLayout) {
                        // Quick Add button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(drawerColor.copy(alpha = 0.15f))
                                .clickable { onAddWidgetClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Widget to ${drawer.title}",
                                tint = drawerColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // More options button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Tactile circular touch backing for chevron
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(chevronBgColor)
                            .clickable { onToggleExpand() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (drawer.isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(chevronRotation),
                            tint = if (drawer.isExpanded) drawerColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!lockLayout) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Widget") },
                            onClick = {
                                showMenu = false
                                onAddWidgetClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEditDrawer()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                        )
                        if (drawerIndex > 0) {
                            DropdownMenuItem(
                                text = { Text("Move up") },
                                onClick = {
                                    showMenu = false
                                    onMoveUp()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) }
                            )
                        }
                        if (drawerIndex < totalDrawers - 1) {
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
                                showDeleteConfirmDialog = true
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

            // Aesthetic open and close animation with organic spring physics and parallax drift
            AnimatedVisibility(
                visible = drawer.isExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 360f
                    )
                ) + slideInVertically(
                    initialOffsetY = { -it / 6 },
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = 360f
                    )
                ) + fadeIn(
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = 0.90f,
                        stiffness = 420f
                    )
                ) + slideOutVertically(
                    targetOffsetY = { -it / 8 },
                    animationSpec = spring(
                        dampingRatio = 0.90f,
                        stiffness = 420f
                    )
                ) + fadeOut(
                    animationSpec = tween(140, easing = FastOutLinearInEasing)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    val coverFlowHeight = (drawer.spanY * 70).coerceIn(190, 380)
                    IpodCoverFlowWidgetStack(
                        widgets = drawer.widgets,
                        widgetHostManager = widgetHostManager,
                        heightDp = coverFlowHeight,
                        onDeleteWidget = onDeleteWidget,
                        onExtractWidget = onExtractWidget,
                        onResizeWidget = onResizeWidget,
                        onReorderWidget = onReorderWidget,
                        lockLayout = lockLayout,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Delete Drawer Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = "Delete \"${drawer.title}\"?",
                    fontFamily = LocalHeaderFontFamily.current,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("Do you want to keep the widgets in this drawer as standalone widgets or delete all widgets?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteDrawer(false) // Keep widgets as standalone
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Keep Widgets")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteDrawer(true) // Delete all widgets
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete All")
                    }
                }
            }
        )
    }
}

private fun resolveWidgetDrawerIcon(title: String): ImageVector {
    val lower = title.lowercase()
    return when {
        lower.contains("finance") || lower.contains("bank") || lower.contains("money") || lower.contains("wallet") || lower.contains("crypto") ->
            Icons.Default.AccountBalance
        lower.contains("media") || lower.contains("music") || lower.contains("audio") || lower.contains("sound") || lower.contains("song") ->
            Icons.Default.MusicNote
        lower.contains("video") || lower.contains("movie") || lower.contains("play") || lower.contains("stream") || lower.contains("tv") ->
            Icons.Default.PlayCircle
        lower.contains("chat") || lower.contains("social") || lower.contains("message") || lower.contains("talk") ->
            Icons.AutoMirrored.Filled.Chat
        lower.contains("work") || lower.contains("office") || lower.contains("task") || lower.contains("note") ->
            Icons.Default.Work
        lower.contains("fit") || lower.contains("health") || lower.contains("gym") || lower.contains("run") ->
            Icons.Default.FitnessCenter
        lower.contains("game") || lower.contains("play") ->
            Icons.Default.SportsEsports
        lower.contains("dev") || lower.contains("code") || lower.contains("git") ->
            Icons.Default.Code
        lower.contains("tool") || lower.contains("util") || lower.contains("system") || lower.contains("setting") ->
            Icons.Default.Build
        lower.contains("star") || lower.contains("fav") ->
            Icons.Default.Star
        lower.contains("shop") || lower.contains("cart") || lower.contains("store") ->
            Icons.Default.ShoppingCart
        lower.contains("book") || lower.contains("read") || lower.contains("news") ->
            Icons.Default.Bookmark
        else -> Icons.Default.Folder
    }
}

private fun resolveWidgetDrawerColor(title: String, drawerId: String): Color {
    val lower = title.lowercase()
    val hex = when {
        lower.contains("finance") || lower.contains("bank") || lower.contains("money") -> "#4CAF50" // Green
        lower.contains("media") || lower.contains("music") -> "#9C27B0" // Purple
        lower.contains("video") || lower.contains("stream") -> "#E53935" // Red
        lower.contains("chat") || lower.contains("social") -> "#009688" // Teal
        lower.contains("work") || lower.contains("office") -> "#1E88E5" // Blue
        lower.contains("tool") || lower.contains("util") -> "#FF9800" // Orange
        lower.contains("fit") || lower.contains("health") -> "#E91E63" // Pink
        lower.contains("star") || lower.contains("fav") -> "#FDD835" // Yellow
        lower.contains("dev") || lower.contains("code") -> "#5E35B1" // Deep Purple
        else -> {
            val colors = GroupIcons.availableColors
            val index = kotlin.math.abs(drawerId.hashCode() % colors.size)
            colors[index]
        }
    }
    return GroupIcons.parseColor(hex)
}
