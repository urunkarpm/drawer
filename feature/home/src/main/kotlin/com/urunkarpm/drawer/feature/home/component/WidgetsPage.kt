package com.urunkarpm.drawer.feature.home.component

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.core.model.WidgetDrawer
import com.urunkarpm.drawer.core.model.WidgetItem

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.MoreVert
import com.urunkarpm.drawer.core.designsystem.modifier.autoCloseOnInactivity
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily

sealed class StandaloneRowItem {
    data class Single(val widget: WidgetItem, val index: Int) : StandaloneRowItem()
    data class PairRow(val left: WidgetItem, val leftIndex: Int, val right: WidgetItem, val rightIndex: Int) : StandaloneRowItem()
}

@Composable
fun WidgetsPage(
    drawers: List<WidgetDrawer>,
    standaloneWidgets: List<WidgetItem>,
    widgetHostManager: DrawerWidgetHostManager,
    onAddWidgetClick: (targetDrawerId: String?) -> Unit,
    onCreateDrawer: (title: String, iconName: String, colorHex: String) -> Unit,
    onUpdateDrawer: (drawer: WidgetDrawer) -> Unit,
    onToggleDrawerExpand: (drawerId: String) -> Unit,
    onCollapseAllDrawers: () -> Unit = {},
    onDeleteDrawer: (drawerId: String, deleteChildren: Boolean) -> Unit,
    onDeleteWidget: (id: String, appWidgetId: Int) -> Unit,
    onMoveWidget: (index: Int, up: Boolean) -> Unit,
    onMoveDrawer: (index: Int, up: Boolean) -> Unit = { _, _ -> },
    onUpdateWidgetSpan: (id: String, spanX: Int, spanY: Int, heightDp: Int) -> Unit,
    onMoveWidgetToDrawer: (widgetId: String, drawerId: String?) -> Unit,
    onReorderWidgetInDrawer: (drawerId: String, fromIndex: Int, toIndex: Int) -> Unit,
    lockLayout: Boolean = false,
    hideDrawerIcons: Boolean = false,
    onBackToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onCollapseAllDrawers()
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val horizontalPadding = if (screenWidth < 360) 12.dp else if (screenWidth >= 840) 24.dp else 16.dp

    var showCreateDrawerDialog by remember { mutableStateOf(false) }
    var drawerBeingEdited by remember { mutableStateOf<WidgetDrawer?>(null) }
    var widgetToResize by remember { mutableStateOf<WidgetItem?>(null) }
    var widgetToMoveDrawer by remember { mutableStateOf<WidgetItem?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    // Group standalone 2-column widgets into pairs
    val rowItems = remember(standaloneWidgets) {
        val rows = mutableListOf<StandaloneRowItem>()
        var i = 0
        while (i < standaloneWidgets.size) {
            val curr = standaloneWidgets[i]
            if (curr.spanX <= 2 && i + 1 < standaloneWidgets.size && standaloneWidgets[i + 1].spanX <= 2) {
                rows.add(StandaloneRowItem.PairRow(curr, i, standaloneWidgets[i + 1], i + 1))
                i += 2
            } else {
                rows.add(StandaloneRowItem.Single(curr, i))
                i++
            }
        }
        rows
    }

    val isEmpty = drawers.isEmpty() && standaloneWidgets.isEmpty()
    val hasExpandedDrawer = drawers.any { it.isExpanded }

    Box(
        modifier = modifier
            .fillMaxSize()
            .autoCloseOnInactivity(
                active = hasExpandedDrawer,
                resetKey = drawers.find { it.isExpanded }?.id,
                timeoutMs = 5000L,
                onClose = { onCollapseAllDrawers() }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            if (isEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Workspace Widgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Organize widgets into structured categorical drawers or pin standalone workspace metrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onAddWidgetClick(null) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Widget")
                        }

                        OutlinedButton(
                            onClick = { showCreateDrawerDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Drawer")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onBackToHome() }
                            ) {
                                Text(
                                    text = "Widgets Feed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = "Return to Home",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (!lockLayout) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // New Drawer Button
                                    Surface(
                                        onClick = { showCreateDrawerDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CreateNewFolder,
                                                contentDescription = "New Drawer",
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Drawer",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Add Widget Button
                                    Box {
                                        Surface(
                                            onClick = {
                                                if (drawers.isEmpty()) {
                                                    onAddWidgetClick(null)
                                                } else {
                                                    showAddMenu = true
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Widget",
                                                    modifier = Modifier.size(15.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Add",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showAddMenu,
                                            onDismissRequest = { showAddMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Add as Standalone Widget") },
                                                leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null) },
                                                onClick = {
                                                    showAddMenu = false
                                                    onAddWidgetClick(null)
                                                }
                                            )
                                            drawers.forEach { drawer ->
                                                DropdownMenuItem(
                                                    text = { Text("Add to \"${drawer.title}\"") },
                                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                                    onClick = {
                                                        showAddMenu = false
                                                        onAddWidgetClick(drawer.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1. Widget Drawers (iPod Cover Flow stacks)
                    itemsIndexed(
                        items = drawers,
                        key = { _, it -> it.id }
                    ) { index, drawer ->
                        WidgetDrawerCard(
                            drawer = drawer,
                            drawerIndex = index,
                            totalDrawers = drawers.size,
                            widgetHostManager = widgetHostManager,
                            onToggleExpand = { onToggleDrawerExpand(drawer.id) },
                            onAddWidgetClick = { onAddWidgetClick(drawer.id) },
                            onEditDrawer = { drawerBeingEdited = drawer },
                            onMoveUp = { onMoveDrawer(index, true) },
                            onMoveDown = { onMoveDrawer(index, false) },
                            onDeleteDrawer = { deleteChildren -> onDeleteDrawer(drawer.id, deleteChildren) },
                            onDeleteWidget = onDeleteWidget,
                            onExtractWidget = { wId -> onMoveWidgetToDrawer(wId, null) },
                            onResizeWidget = { w -> widgetToResize = w },
                            onReorderWidget = { fromIdx, toIdx -> onReorderWidgetInDrawer(drawer.id, fromIdx, toIdx) },
                            lockLayout = lockLayout,
                            hideDrawerIcons = hideDrawerIcons
                        )
                    }

                    // 2. Standalone Widgets (4-column responsive grid flow)
                    itemsIndexed(
                        items = rowItems,
                        key = { index, item ->
                            when (item) {
                                is StandaloneRowItem.Single -> "single_${item.widget.id}"
                                is StandaloneRowItem.PairRow -> "pair_${item.left.id}_${item.right.id}"
                            }
                        }
                    ) { _, rowItem ->
                        when (rowItem) {
                            is StandaloneRowItem.Single -> {
                                val widget = rowItem.widget
                                val isCompact = widget.spanX <= 2
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isCompact) Alignment.CenterStart else Alignment.Center
                                ) {
                                    Box(
                                        modifier = if (isCompact)
                                            Modifier.fillMaxWidth(0.5f).padding(end = 6.dp)
                                        else
                                            Modifier.fillMaxWidth()
                                    ) {
                                        StandaloneWidgetCardItem(
                                            widget = widget,
                                            index = rowItem.index,
                                            totalWidgets = standaloneWidgets.size,
                                            drawers = drawers,
                                            widgetHostManager = widgetHostManager,
                                            onDelete = { onDeleteWidget(widget.id, widget.appWidgetId) },
                                            onMoveUp = { onMoveWidget(rowItem.index, true) },
                                            onMoveDown = { onMoveWidget(rowItem.index, false) },
                                            onResize = { widgetToResize = widget },
                                            onMoveToDrawer = { widgetToMoveDrawer = widget },
                                            lockLayout = lockLayout
                                        )
                                    }
                                }
                            }
                            is StandaloneRowItem.PairRow -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        StandaloneWidgetCardItem(
                                            widget = rowItem.left,
                                            index = rowItem.leftIndex,
                                            totalWidgets = standaloneWidgets.size,
                                            drawers = drawers,
                                            widgetHostManager = widgetHostManager,
                                            onDelete = { onDeleteWidget(rowItem.left.id, rowItem.left.appWidgetId) },
                                            onMoveUp = { onMoveWidget(rowItem.leftIndex, true) },
                                            onMoveDown = { onMoveWidget(rowItem.leftIndex, false) },
                                            onResize = { widgetToResize = rowItem.left },
                                            onMoveToDrawer = { widgetToMoveDrawer = rowItem.left },
                                            lockLayout = lockLayout
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        StandaloneWidgetCardItem(
                                            widget = rowItem.right,
                                            index = rowItem.rightIndex,
                                            totalWidgets = standaloneWidgets.size,
                                            drawers = drawers,
                                            widgetHostManager = widgetHostManager,
                                            onDelete = { onDeleteWidget(rowItem.right.id, rowItem.right.appWidgetId) },
                                            onMoveUp = { onMoveWidget(rowItem.rightIndex, true) },
                                            onMoveDown = { onMoveWidget(rowItem.rightIndex, false) },
                                            onResize = { widgetToResize = rowItem.right },
                                            onMoveToDrawer = { widgetToMoveDrawer = rowItem.right },
                                            lockLayout = lockLayout
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

    // Interactive Widget Resize Dialog
    widgetToResize?.let { widget ->
        WidgetResizeDialog(
            widget = widget,
            onDismiss = { widgetToResize = null },
            onApply = { spanX, spanY, heightDp ->
                onUpdateWidgetSpan(widget.id, spanX, spanY, heightDp)
                widgetToResize = null
            }
        )
    }

    // Move Widget to Drawer Dialog
    widgetToMoveDrawer?.let { widget ->
        AlertDialog(
            onDismissRequest = { widgetToMoveDrawer = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Move into Drawer",
                    fontFamily = LocalHeaderFontFamily.current,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a drawer to add this widget to:")
                    drawers.forEach { d ->
                        Surface(
                            onClick = {
                                onMoveWidgetToDrawer(widget.id, d.id)
                                widgetToMoveDrawer = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(d.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    widgetToMoveDrawer = null
                    showCreateDrawerDialog = true
                }) {
                    Text("+ Create New Drawer")
                }
            },
            dismissButton = {
                TextButton(onClick = { widgetToMoveDrawer = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Drawer Dialog
    if (showCreateDrawerDialog) {
        EditWidgetDrawerDialog(
            drawer = null,
            onDismissRequest = { showCreateDrawerDialog = false },
            onSave = { title, iconName, colorHex ->
                onCreateDrawer(title, iconName, colorHex)
                showCreateDrawerDialog = false
            }
        )
    }

    // Edit Drawer Dialog
    drawerBeingEdited?.let { drawerToEdit ->
        EditWidgetDrawerDialog(
            drawer = drawerToEdit,
            onDismissRequest = { drawerBeingEdited = null },
            onSave = { title, iconName, colorHex ->
                onUpdateDrawer(drawerToEdit.copy(title = title, iconName = iconName, colorHex = colorHex))
                drawerBeingEdited = null
            },
            onDelete = {
                onDeleteDrawer(drawerToEdit.id, false)
                drawerBeingEdited = null
            }
        )
    }
}

@Composable
private fun StandaloneWidgetCardItem(
    widget: WidgetItem,
    index: Int,
    totalWidgets: Int,
    drawers: List<WidgetDrawer>,
    widgetHostManager: DrawerWidgetHostManager,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onResize: () -> Unit,
    onMoveToDrawer: () -> Unit,
    lockLayout: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(widget.heightDp.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            val providerInfo = widgetHostManager.appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                            if (providerInfo == null) {
                                android.widget.TextView(ctx).apply {
                                    text = "Widget unavailable (${widget.packageName})"
                                    setPadding(32, 32, 32, 32)
                                }
                            } else {
                                widgetHostManager.getOrCreateView(ctx, widget.appWidgetId)
                            }
                        } catch (e: Exception) {
                            android.widget.TextView(ctx).apply {
                                text = "Widget unavailable (${widget.packageName})"
                                setPadding(32, 32, 32, 32)
                            }
                        }
                    },
                    update = { view ->
                        if (view is AppWidgetHostView) {
                            val density = view.resources.displayMetrics.density
                            val widthDp = if (view.width > 0) (view.width / density).toInt() else (if (widget.spanX <= 2) 160 else 340)
                            val heightDp = widget.heightDp
                            val lastTag = view.tag as? Pair<Int, Int>
                            if (lastTag?.first != widthDp || lastTag.second != heightDp) {
                                view.tag = Pair(widthDp, heightDp)
                                view.layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val bundle = Bundle().apply {
                                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                                    putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                                }
                                view.updateAppWidgetSize(bundle, widthDp, heightDp, widthDp, heightDp)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Menu button in top corner if layout is not locked
                if (!lockLayout) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Surface(
                            onClick = { showMenu = true },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Resize Widget") },
                                leadingIcon = { Icon(Icons.Outlined.AspectRatio, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onResize()
                                }
                            )
                            if (drawers.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Move into Drawer...") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onMoveToDrawer()
                                    }
                                )
                            }
                            if (index > 0) {
                                DropdownMenuItem(
                                    text = { Text("Move Up") },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onMoveUp()
                                    }
                                )
                            }
                            if (index < totalWidgets - 1) {
                                DropdownMenuItem(
                                    text = { Text("Move Down") },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onMoveDown()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Widget", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
