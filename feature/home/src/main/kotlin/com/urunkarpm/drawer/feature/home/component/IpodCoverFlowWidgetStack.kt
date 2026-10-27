package com.urunkarpm.drawer.feature.home.component

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No widgets in this workspace drawer",
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

    var previousWidgetCount by remember { mutableStateOf(widgets.size) }

    // Animate to new widget if added, or clamp pager index if the active widget was removed
    LaunchedEffect(widgets.size) {
        if (widgets.size > previousWidgetCount) {
            pagerState.animateScrollToPage(widgets.size - 1)
        } else if (widgets.isNotEmpty() && pagerState.currentPage >= widgets.size) {
            pagerState.scrollToPage(widgets.size - 1)
        }
        previousWidgetCount = widgets.size
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    // Side padding lets previous and next cards peek into view on left and right
    val horizontalPadding = if (screenWidth < 360) 36.dp else 48.dp

    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .then(
                    if (widgets.size > 1) {
                        Modifier.pointerInput(pagerState.currentPage, widgets.size) {
                            detectVerticalDragGestures(
                                onDragStart = { verticalDragAccumulator = 0f },
                                onDragEnd = { verticalDragAccumulator = 0f },
                                onDragCancel = { verticalDragAccumulator = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    verticalDragAccumulator += dragAmount
                                    if (verticalDragAccumulator < -45f) {
                                        // Swiped up -> cycle to next widget
                                        if (pagerState.currentPage < widgets.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                        verticalDragAccumulator = 0f
                                        change.consume()
                                    } else if (verticalDragAccumulator > 45f) {
                                        // Swiped down -> cycle to previous widget
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                        verticalDragAccumulator = 0f
                                        change.consume()
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            HorizontalPager(
                state = pagerState,
                key = { page -> widgets.getOrNull(page)?.id ?: page },
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                pageSpacing = 10.dp,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val widget = widgets.getOrNull(page) ?: return@HorizontalPager

                val isCenter = page == pagerState.currentPage
                val pageDistance = abs(page - pagerState.currentPage)

                // ponytail: CoverFlow stack transforms computed purely in graphicsLayer draw phase; ceiling: constant background scrim alpha; upgrade path: shader-based gradient alpha.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Relative offset: 0 = active center, -1 = previous (left), +1 = next (right)
                            val pageOffset = (page - pagerState.currentPage) + pagerState.currentPageOffsetFraction
                            val absOffset = abs(pageOffset)

                            // Sleek executive card stack: zero 3D rotation distortion
                            rotationY = 0f

                            // Gentle horizontal tuck for adjacent cards
                            val translationAmount = if (pageOffset < 0) {
                                (absOffset * 14.dp.toPx())
                            } else if (pageOffset > 0) {
                                -(absOffset * 14.dp.toPx())
                            } else {
                                0f
                            }

                            val scale = (1f - (absOffset * 0.06f)).coerceIn(0.92f, 1f)
                            scaleX = scale
                            scaleY = scale
                            translationX = translationAmount
                            alpha = (1f - (absOffset * 0.20f)).coerceIn(0.70f, 1f)
                            shadowElevation = if (page == pagerState.currentPage) 6.dp.toPx() else 1.dp.toPx()
                        }
                        .zIndex(if (isCenter) 100f else (50f - pageDistance * 10f)),
                    contentAlignment = Alignment.Center
                ) {
                    key(widget.id, widget.appWidgetId) {
                        CoverFlowWidgetItemCard(
                            widget = widget,
                            page = page,
                            totalWidgets = widgets.size,
                            isCenter = isCenter,
                            widgetHostManager = widgetHostManager,
                            onBringToCenter = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            },
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Executive Segmented Pager Indicator
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                widgets.forEachIndexed { idx, _ ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (isSelected) 14.dp else 4.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                            }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%02d / %02d", pagerState.currentPage + 1, widgets.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CoverFlowWidgetItemCard(
    widget: WidgetItem,
    page: Int,
    totalWidgets: Int,
    isCenter: Boolean,
    widgetHostManager: DrawerWidgetHostManager,
    onBringToCenter: () -> Unit,
    onDelete: () -> Unit,
    onExtract: () -> Unit,
    onResize: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    lockLayout: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = if (isCenter) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle branded loading placeholder beneath the widget view so user NEVER sees a blank void
            val appLabel = remember(widget.packageName) {
                try {
                    val pm = context.packageManager
                    val app = pm.getApplicationInfo(widget.packageName, 0)
                    pm.getApplicationLabel(app).toString()
                } catch (_: Exception) {
                    widget.packageName
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Widget Host View (strictly isolated to widget.appWidgetId)
            AndroidView(
                factory = { ctx ->
                    try {
                        var providerInfo = widgetHostManager.appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                        var targetWidgetId = widget.appWidgetId
                        if (providerInfo == null) {
                            val reboundId = widgetHostManager.rebindOrRestoreWidget(
                                packageName = widget.packageName,
                                providerClassName = widget.providerClassName,
                                existingAppWidgetId = widget.appWidgetId,
                                spanX = widget.spanX,
                                heightDp = widget.heightDp
                            )
                            if (reboundId != widget.appWidgetId) {
                                providerInfo = widgetHostManager.appWidgetManager.getAppWidgetInfo(reboundId)
                                if (providerInfo != null) {
                                    targetWidgetId = reboundId
                                }
                            }
                        }
                        if (providerInfo == null) {
                            android.widget.TextView(ctx).apply {
                                text = "Widget unavailable (${widget.packageName})"
                                setPadding(32, 32, 32, 32)
                            }
                        } else {
                            val hostView = widgetHostManager.getOrCreateView(ctx, targetWidgetId)
                            hostView.layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            hostView
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
                        view.layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val density = view.resources.displayMetrics.density
                        val widthDp = if (view.width > 0) (view.width / density).toInt() else 280
                        val height = widget.heightDp
                        val bundle = widgetHostManager.createWidgetOptionsBundle(widthDp, height)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            view.updateAppWidgetSize(bundle, listOf(android.util.SizeF(widthDp.toFloat(), height.toFloat())))
                        } else {
                            @Suppress("DEPRECATION")
                            view.updateAppWidgetSize(bundle, widthDp, height, widthDp, height)
                        }
                        if (view.childCount == 0) {
                            widgetHostManager.triggerWidgetUpdate(view.appWidgetId)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(4.dp)
            )

            // Background Card: Scrim Overlay intercepts clicks to bring that card to center
            if (!isCenter) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.22f),
                                    Color.Black.copy(alpha = 0.40f)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onBringToCenter()
                        }
                )
            }

            // Context Menu button: ONLY on the active center card
            if (isCenter && !lockLayout) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Surface(
                        onClick = { showMenu = true },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Widget Options",
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
