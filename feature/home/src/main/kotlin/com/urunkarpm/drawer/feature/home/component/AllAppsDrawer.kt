package com.urunkarpm.drawer.feature.home.component

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.home.dragdrop.AppDragDropState
import com.urunkarpm.drawer.feature.home.dragdrop.DragDropResult
import com.urunkarpm.drawer.feature.home.dragdrop.appDragSource
import kotlinx.coroutines.launch

import androidx.compose.foundation.isSystemInDarkTheme
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme

@Composable
fun AllAppsDrawer(
    visible: Boolean,
    apps: List<AppInfo>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onClose: () -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier,
    iconShape: Shape = RoundedCornerShape(12.dp),
    dragDropState: AppDragDropState? = null,
    onDragDropResult: ((DragDropResult) -> Unit)? = null,
    drawerThemeMode: String = "SYSTEM",
    surfaceCornerRadiusDp: Float = 24f,
    autoOpenKeyboard: Boolean = false,
    onOpenSettings: (() -> Unit)? = null
) {
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(visible) {
        if (visible && autoOpenKeyboard) {
            searchFocusRequester.requestFocus()
        }
    }
    val isSystemDark = isSystemInDarkTheme()
    val isDrawerDark = when (drawerThemeMode.uppercase()) {
        "WHITE", "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    // Wrap the entire app drawer with DrawerTheme so all typography, search bars, and app labels use correct theme contrast
    DrawerTheme(darkTheme = isDrawerDark) {
        // Smooth Material motion ensures a consistent, one-pace slide without overshoot bouncing or late adjustment
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutLinearInEasing
                )
            ),
            modifier = modifier
        ) {
            val gridState = rememberLazyGridState()
            val coroutineScope = rememberCoroutineScope()

            // NestedScrollConnection: when LazyGrid is scrolled to top, user can pull DOWN to dismiss.
            var overscrollDownAccumulator by remember { mutableFloatStateOf(0f) }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (available.y < 0) {
                            overscrollDownAccumulator = 0f
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        val shouldClose = overscrollDownAccumulator > 80f && available.y > 1200f
                        overscrollDownAccumulator = 0f
                        if (shouldClose && !gridState.canScrollBackward) {
                            onClose()
                        }
                        return super.onPostFling(consumed, available)
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (!gridState.canScrollBackward && available.y > 0f && source == NestedScrollSource.UserInput) {
                            overscrollDownAccumulator += available.y
                            if (overscrollDownAccumulator > 180f) {
                                overscrollDownAccumulator = 0f
                                onClose()
                            }
                        } else if (available.y < 0f) {
                            overscrollDownAccumulator = 0f
                        }
                        return Offset.Zero
                    }
                }
            }

            val animatedDrawerAlpha by animateFloatAsState(
                targetValue = if (dragDropState?.isDragging == true) 0f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "drawerAlphaDuringDrag"
            )

            // 100% solid, crisp background in White, Dark, or System mode — no liquid glass translucency
            val drawerBackgroundColor = if (isDrawerDark) Color(0xFF141218) else Color(0xFFFFFFFF)
            val drawerBorder = BorderStroke(1.dp, if (isDrawerDark) Color(0xFF2B2930) else Color(0xFFE5E7EB))

            val drawerShape = RoundedCornerShape(
                topStart = surfaceCornerRadiusDp.dp,
                topEnd = surfaceCornerRadiusDp.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )

            Surface(
                shape = drawerShape,
                border = drawerBorder,
                color = drawerBackgroundColor,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = animatedDrawerAlpha
                    }
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (overscrollDownAccumulator * 0.25f).coerceIn(0f, 60f)
                    }
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .navigationBarsPadding()
                    .imePadding()
                    .nestedScroll(nestedScrollConnection)
            ) {
                var handleDragAccumulator by remember { mutableFloatStateOf(0f) }
                // Top subtle drag handle indicator with pull-down gesture
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { handleDragAccumulator = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    handleDragAccumulator += dragAmount
                                    if (handleDragAccumulator > 80f) {
                                        handleDragAccumulator = 0f
                                        onClose()
                                    }
                                },
                                onDragEnd = { handleDragAccumulator = 0f },
                                onDragCancel = { handleDragAccumulator = 0f }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }

                // App Grid or Loading / Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (apps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No apps found for \"$searchQuery\"" else "No apps installed",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(4),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp,
                                    end = if (searchQuery.isBlank() && apps.size > 8) 32.dp else 16.dp,
                                    bottom = 12.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = apps,
                                    key = { it.componentKey },
                                    contentType = { "app_item" }
                                ) { app ->
                                    AppItemView(
                                        app = app,
                                        onAppClick = onAppClick,
                                        onAppLongClick = onAppLongClick,
                                        iconLoader = iconLoader,
                                        iconShape = iconShape,
                                        dragDropState = dragDropState,
                                        onDragDropResult = onDragDropResult
                                    )
                                }
                            }

                            if (searchQuery.isBlank() && apps.size > 8) {
                                AlphabetIndexBar(
                                    onLetterSelected = { letter ->
                                        val targetIndex = if (letter == "#") {
                                            0
                                        } else {
                                            val exact = apps.indexOfFirst { it.label.startsWith(letter, ignoreCase = true) }
                                            if (exact >= 0) {
                                                exact
                                            } else {
                                                val next = apps.indexOfFirst { it.label.uppercase() > letter }
                                                if (next >= 0) next else (apps.size - 1)
                                            }
                                        }
                                        if (targetIndex in apps.indices) {
                                            coroutineScope.launch {
                                                gridState.scrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }
                }

                // Ergonomic Bottom Search Bar: Avoids camera cutout and provides easy one-hand thumb access
                SearchBarHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onClose = onClose,
                    onOpenSettings = onOpenSettings,
                    focusRequester = searchFocusRequester
                )
            }
        }
    }
}
}

@Composable
private fun SearchBarHeader(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close app drawer",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = {
                Text(
                    text = "Search apps...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        )

        if (onOpenSettings != null) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    onClose()
                    onOpenSettings()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Launcher Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItemView(
    app: AppInfo,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    modifier: Modifier = Modifier,
    iconShape: Shape = RoundedCornerShape(12.dp),
    dragDropState: AppDragDropState? = null,
    onDragDropResult: ((DragDropResult) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    // ponytail: Unified gesture detector combining instantaneous tap execution, list scroll passthrough, and long-press drag-to-drawer; ceiling is Compose pointer slop; upgrade path is native drag-and-drop transfer API.
    val itemModifier = if (dragDropState != null) {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .appDragSource(
                app = app,
                onAppClick = { onAppClick(app) },
                onAppLongClick = { onAppLongClick(app) },
                onDragStart = { rootPos ->
                    dragDropState.startDrag(app, rootPos, sourceGroupId = null)
                },
                onDrag = { dragAmount ->
                    dragDropState.updateDrag(dragAmount)
                },
                onDragEnd = { isDropped ->
                    if (isDropped) {
                        val result = dragDropState.endDrag()
                        onDragDropResult?.invoke(result)
                    } else {
                        dragDropState.cancelDrag()
                    }
                },
                longPressRequired = true
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
                contentDescription = "${app.label}${if (app.isWorkProfile) " (Work Profile)" else ""}"
            }
    } else {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onAppClick(app) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAppLongClick(app)
                }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics {
                contentDescription = "${app.label}${if (app.isWorkProfile) " (Work Profile)" else ""}"
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = itemModifier
    ) {
        AppIconImage(
            key = app.componentKey,
            size = 54.dp,
            label = app.label,
            isWorkProfile = app.isWorkProfile,
            iconShape = iconShape,
            iconLoader = { iconLoader(app) }
        )
        Spacer(modifier = Modifier.height(6.dp))
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
