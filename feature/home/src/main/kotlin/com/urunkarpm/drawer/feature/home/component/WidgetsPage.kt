package com.urunkarpm.drawer.feature.home.component

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.core.model.WidgetItem

@Composable
fun WidgetsPage(
    widgets: List<WidgetItem>,
    widgetHostManager: DrawerWidgetHostManager,
    onAddWidgetClick: () -> Unit,
    onDeleteWidget: (String, Int) -> Unit,
    onMoveWidget: (Int, Boolean) -> Unit,
    onResizeWidget: (String, Int) -> Unit,
    lockLayout: Boolean = false,
    onBackToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        widgetHostManager.startListening()
        onDispose {
            widgetHostManager.stopListening()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (widgets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Widgets Feed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Place your favourite clock, weather, music, or app widgets here in a clean vertical feed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onAddWidgetClick,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Widget")
                    }

                    OutlinedButton(
                        onClick = onBackToHome,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Back to Home")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header row
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
                                fontWeight = FontWeight.Bold,
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!lockLayout) {
                                Surface(
                                    onClick = onAddWidgetClick,
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Widget",
                                            modifier = Modifier.size(16.dp),
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
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = widgets,
                    key = { _, item -> item.id }
                ) { index, widget ->
                    WidgetCardItem(
                        widget = widget,
                        index = index,
                        totalWidgets = widgets.size,
                        widgetHostManager = widgetHostManager,
                        onDelete = { onDeleteWidget(widget.id, widget.appWidgetId) },
                        onMoveUp = { onMoveWidget(index, true) },
                        onMoveDown = { onMoveWidget(index, false) },
                        onResize = { newHeight -> onResizeWidget(widget.id, newHeight) },
                        lockLayout = lockLayout
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCardItem(
    widget: WidgetItem,
    index: Int,
    totalWidgets: Int,
    widgetHostManager: DrawerWidgetHostManager,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onResize: (Int) -> Unit,
    lockLayout: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Widget Host View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(widget.heightDp.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            val providerInfo = widgetHostManager.appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                            widgetHostManager.createView(ctx, widget.appWidgetId, providerInfo)
                        } catch (e: Exception) {
                            android.widget.TextView(ctx).apply {
                                text = "Widget unavailable (${widget.packageName})"
                                setPadding(32, 32, 32, 32)
                            }
                        }
                    },
                    update = { view ->
                        if (view is AppWidgetHostView) {
                            view.layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            val density = view.resources.displayMetrics.density
                            val widthDp = (view.width / density).toInt().coerceAtLeast(150)
                            
                            val bundle = Bundle().apply {
                                putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, widget.heightDp)
                                putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, widget.heightDp)
                            }
                            view.updateAppWidgetOptions(bundle)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Menu button in top corner if layout is not locked
                if (!lockLayout) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { showMenu = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "⋮",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
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
                                text = { Text("Height: Small (120dp)") },
                                onClick = {
                                    showMenu = false
                                    onResize(120)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Height: Medium (180dp)") },
                                onClick = {
                                    showMenu = false
                                    onResize(180)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Height: Large (260dp)") },
                                onClick = {
                                    showMenu = false
                                    onResize(260)
                                }
                            )
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
