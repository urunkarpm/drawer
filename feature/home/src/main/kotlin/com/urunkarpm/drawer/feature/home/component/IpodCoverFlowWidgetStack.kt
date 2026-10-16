package com.urunkarpm.drawer.feature.home.component

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.core.model.WidgetItem
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun IpodCoverFlowWidgetStack(
    widgets: List<WidgetItem>,
    widgetHostManager: DrawerWidgetHostManager,
    heightDp: Int = 210,
    onDeleteWidget: (id: String, appWidgetId: Int) -> Unit,
    onExtractWidget: (id: String) -> Unit,
    onResizeWidget: (widget: WidgetItem) -> Unit,
    onReorderWidget: (fromIndex: Int, toIndex: Int) -> Unit,
    lockLayout: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (widgets.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No widgets in this stack yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { widgets.size }
    )

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    // Side padding lets previous and next cards peek into view on left and right
    val horizontalPadding = if (screenWidth < 360) 40.dp else 56.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                pageSpacing = 0.dp,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val widget = widgets.getOrNull(page) ?: return@HorizontalPager

                // Calculate offset relative to active center page
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset = abs(pageOffset)
                val isCenter = absOffset < 0.25f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            cameraDistance = 16f * density
                            // iPod Cover Flow 3D Perspective
                            rotationY = (pageOffset * 38f).coerceIn(-48f, 48f)
                            val scale = (1f - (absOffset * 0.14f)).coerceIn(0.72f, 1f)
                            scaleX = scale
                            scaleY = scale
                            // Tuck adjacent cards inwards so their edges stay beautifully visible
                            translationX = (pageOffset * 28.dp.toPx())
                            alpha = (1f - (absOffset * 0.08f)).coerceIn(0.75f, 1f)
                            shadowElevation = (16.dp * (1f - absOffset.coerceIn(0f, 1f))).toPx()
                        }
                        .zIndex(100f - absOffset * 10f)
                        .then(
                            if (!isCenter) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page)
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CoverFlowWidgetItemCard(
                        widget = widget,
                        page = page,
                        totalWidgets = widgets.size,
                        isCenter = isCenter,
                        absOffset = absOffset,
                        widgetHostManager = widgetHostManager,
                        onDelete = { onDeleteWidget(widget.id, widget.appWidgetId) },
                        onExtract = { onExtractWidget(widget.id) },
                        onResize = { onResizeWidget(widget) },
                        onMoveLeft = {
                            if (page > 0) {
                                onReorderWidget(page, page - 1)
                                coroutineScope.launch { pagerState.animateScrollToPage(page - 1) }
                            }
                        },
                        onMoveRight = {
                            if (page < widgets.size - 1) {
                                onReorderWidget(page, page + 1)
                                coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
                            }
                        },
                        lockLayout = lockLayout
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // iPod-style Carousel Dots & Counter
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                    shape = CircleShape
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            widgets.forEachIndexed { idx, _ ->
                val isSelected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 5.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                        }
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${pagerState.currentPage + 1}/${widgets.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CoverFlowWidgetItemCard(
    widget: WidgetItem,
    page: Int,
    totalWidgets: Int,
    isCenter: Boolean,
    absOffset: Float,
    widgetHostManager: DrawerWidgetHostManager,
    onDelete: () -> Unit,
    onExtract: () -> Unit,
    onResize: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    lockLayout: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = if (isCenter) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Widget Host View
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
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .padding(4.dp)
            )

            // Dynamic 3D lighting / scrim for side recessed cards
            if (absOffset > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = (absOffset * 0.25f).coerceIn(0f, 0.40f)),
                                    Color.Black.copy(alpha = (absOffset * 0.45f).coerceIn(0f, 0.55f))
                                )
                            )
                        )
                )
            }

            // Context Menu button on the active card
            if (isCenter && !lockLayout) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Surface(
                        onClick = { showMenu = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
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
                        DropdownMenuItem(
                            text = { Text("Resize Widget") },
                            leadingIcon = { Icon(Icons.Outlined.AspectRatio, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onResize()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Extract from Drawer") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                showMenu = false
                                onExtract()
                            }
                        )
                        if (page > 0) {
                            DropdownMenuItem(
                                text = { Text("Move Left") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onMoveLeft()
                                }
                            )
                        }
                        if (page < totalWidgets - 1) {
                            DropdownMenuItem(
                                text = { Text("Move Right") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onMoveRight()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove from Drawer", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
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
