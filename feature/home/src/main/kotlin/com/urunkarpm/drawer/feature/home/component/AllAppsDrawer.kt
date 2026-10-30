package com.urunkarpm.drawer.feature.home.component

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.urunkarpm.drawer.core.designsystem.animation.FluidSpringPhysics
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.needle.action.LauncherAction
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.home.dragdrop.AppDragDropState
import com.urunkarpm.drawer.feature.home.dragdrop.DragDropResult
import com.urunkarpm.drawer.feature.home.dragdrop.appDragSource
import kotlinx.coroutines.launch
import androidx.compose.runtime.key
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.foundation.isSystemInDarkTheme
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme
import com.urunkarpm.drawer.core.designsystem.theme.LocalAppLabelScale

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
    iconShape: Shape = CircleShape,
    dragDropState: AppDragDropState? = null,
    onDragDropResult: ((DragDropResult) -> Unit)? = null,
    drawerThemeMode: String = "SYSTEM",
    surfaceCornerRadiusDp: Float = 24f,
    autoOpenKeyboard: Boolean = false,
    blurAppListOnOpen: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
    frequentApps: List<AppInfo> = emptyList(),
    suggestedAction: LauncherAction? = null,
    onExecuteAction: (LauncherAction) -> Unit = {}
) {
    val context = LocalContext.current
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
        // iOS-grade fluid critically damped spring transition settling around 320ms without artificial cubic beziers
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = FluidSpringPhysics.FLUID_STIFFNESS)
            ) + fadeIn(
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 400f)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 450f)
            ) + fadeOut(
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 450f)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 450f)
            ),
            modifier = modifier
        ) {
            val gridState = rememberLazyGridState()
            val coroutineScope = rememberCoroutineScope()
            var activeScrubLetter by remember { mutableStateOf<String?>(null) }
            var scrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            // Reset active scrub letter when drawer closes or search is active
            LaunchedEffect(visible, searchQuery) {
                if (!visible || searchQuery.isNotBlank()) {
                    activeScrubLetter = null
                }
            }

            var isAppListBlurred by remember { mutableStateOf(blurAppListOnOpen) }
            val blurCurtainTranslationY = remember { Animatable(0f) }

            LaunchedEffect(visible, blurAppListOnOpen) {
                if (visible) {
                    isAppListBlurred = blurAppListOnOpen
                    blurCurtainTranslationY.snapTo(0f)
                }
            }

            // Apps are already filtered in HomeViewModel; use directly to prevent double-allocation jank
            val displayedApps = apps

            val appPositions = remember { mutableMapOf<String, Offset>() }
            val dustState = rememberDustParticleState()
            var previousApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
            val particleColor = MaterialTheme.colorScheme.onSurfaceVariant

            LaunchedEffect(displayedApps, searchQuery) {
                if (searchQuery.isNotBlank() && previousApps.isNotEmpty() && displayedApps.size < previousApps.size) {
                    val currentKeys = displayedApps.map { it.componentKey }.toSet()
                    val removed = previousApps.filter { it.componentKey !in currentKeys }
                    for (app in removed) {
                        val pos = appPositions[app.componentKey]
                        if (pos != null) {
                            dustState.spawnPuff(pos, particleColor)
                            appPositions.remove(app.componentKey)
                        }
                    }
                }
                previousApps = displayedApps
            }

            LaunchedEffect(visible) {
                if (!visible) {
                    appPositions.clear()
                    previousApps = emptyList()
                }
            }

            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val gridColumns = when {
                screenWidth < 600 -> 4
                screenWidth < 840 -> 6
                screenWidth < 1100 -> 7
                else -> 8
            }
            val horizontalPadding = if (screenWidth < 360) 12.dp else if (screenWidth >= 840) 24.dp else 16.dp

            val letterIndexMap = remember(displayedApps) {
                val map = HashMap<String, Int>(32)
                map["#"] = 0
                for ((index, app) in displayedApps.withIndex()) {
                    val fc = app.label.firstOrNull()?.uppercaseChar()
                    if (fc != null && fc in 'A'..'Z') {
                        val s = fc.toString()
                        if (!map.containsKey(s)) {
                            map[s] = index
                        }
                    }
                }
                var lastIdx = (displayedApps.size - 1).coerceAtLeast(0)
                for (c in 'Z' downTo 'A') {
                    val s = c.toString()
                    val idx = map[s]
                    if (idx != null) {
                        lastIdx = idx
                    } else {
                        map[s] = lastIdx
                    }
                }
                map
            }

            // iOS-grade fluid spring & rubber-banding drag state with velocity handoff
            val drawerTranslationY = remember { Animatable(0f) }
            var rawDragDistanceY by remember { mutableFloatStateOf(0f) }
            val handleVelocityTracker = remember { VelocityTracker() }

            LaunchedEffect(visible) {
                if (visible) {
                    drawerTranslationY.snapTo(0f)
                    rawDragDistanceY = 0f
                }
            }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        // Immediately intercept downward pull from stroke 1 when at top of list
                        if (available.y > 0f && !gridState.canScrollBackward && source == NestedScrollSource.UserInput) {
                            rawDragDistanceY += available.y
                            val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                            coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                            return available
                        } else if (available.y < 0f && rawDragDistanceY > 0f) {
                            // User pushing back upward while rubber-banded downward
                            rawDragDistanceY = (rawDragDistanceY + available.y).coerceAtLeast(0f)
                            val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                            coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        // Downward flick anywhere near top closes immediately on 1 swipe
                        if (!gridState.canScrollBackward && available.y > 250f) {
                            onClose()
                            return available
                        }
                        return super.onPreFling(available)
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        val vY = available.y
                        if (rawDragDistanceY > 0f) {
                            val shouldClose = rawDragDistanceY > 60f || vY > 250f
                            if (shouldClose) {
                                onClose()
                            } else {
                                coroutineScope.launch {
                                    drawerTranslationY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = FluidSpringPhysics.fluidSpring(),
                                        initialVelocity = vY
                                    )
                                    rawDragDistanceY = 0f
                                }
                            }
                        }
                        return super.onPostFling(consumed, available)
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (!gridState.canScrollBackward && available.y > 0f && source == NestedScrollSource.UserInput) {
                            rawDragDistanceY += available.y
                            val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                            coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                            return Offset(0f, available.y)
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
            val drawerBorder = remember(isDrawerDark) { BorderStroke(1.dp, if (isDrawerDark) Color(0xFF2B2930) else Color(0xFFE5E7EB)) }

            val drawerShape = remember(surfaceCornerRadiusDp) {
                RoundedCornerShape(
                    topStart = surfaceCornerRadiusDp.dp,
                    topEnd = surfaceCornerRadiusDp.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            }

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
                        translationY = drawerTranslationY.value
                    }
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .nestedScroll(nestedScrollConnection)
            ) {
                // Top subtle drag handle indicator with generous touch target & effortless 1-swipe dismiss
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                handleVelocityTracker.resetTracking()
                                handleVelocityTracker.addPosition(down.uptimeMillis, down.position)
                                var isDragging = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.changedToUp()) {
                                        if (isDragging) {
                                            val vY = handleVelocityTracker.calculateVelocity().y
                                            val shouldClose = rawDragDistanceY > 60f || vY > 250f
                                            if (shouldClose) {
                                                onClose()
                                            } else {
                                                coroutineScope.launch {
                                                    drawerTranslationY.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = FluidSpringPhysics.fluidSpring(),
                                                        initialVelocity = vY
                                                    )
                                                    rawDragDistanceY = 0f
                                                }
                                            }
                                        }
                                        break
                                    }
                                    val dragY = change.position.y - change.previousPosition.y
                                    if (dragY > 0f || isDragging) {
                                        change.consume()
                                        isDragging = true
                                        rawDragDistanceY = (rawDragDistanceY + dragY).coerceAtLeast(0f)
                                        handleVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                        val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                                        coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }

                val containerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

                // 1. App List Grid Area with continuous fluid frosted glass blur curtain
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onGloballyPositioned { containerCoordinates.value = it }
                ) {
                    val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                    LaunchedEffect(searchQuery) {
                        if (searchQuery.isNotBlank() && isAppListBlurred) {
                            coroutineScope.launch {
                                blurCurtainTranslationY.animateTo(
                                    targetValue = containerHeightPx,
                                    animationSpec = FluidSpringPhysics.fluidSpring()
                                )
                                isAppListBlurred = false
                            }
                        }
                    }

                    val curtainProgress = (blurCurtainTranslationY.value / containerHeightPx).coerceIn(0f, 1f)
                    val effectiveBlurRadius = if (isAppListBlurred && curtainProgress < 0.99f) {
                        (28.dp * (1f - curtainProgress))
                    } else {
                        0.dp
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (effectiveBlurRadius > 0.5.dp) Modifier.blur(effectiveBlurRadius) else Modifier)
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
                        } else if (displayedApps.isEmpty()) {
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
                        } else if (searchQuery.isNotBlank() && displayedApps.size in 1..3) {
                            // Centered Thumb Row: 1-3 matching apps anchored right above the keyboard/search bar
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = horizontalPadding, vertical = 16.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val thumbRowProgress = remember { Animatable(0f) }
                                LaunchedEffect(displayedApps.size) {
                                    thumbRowProgress.snapTo(0f)
                                    thumbRowProgress.animateTo(1f, animationSpec = FluidSpringPhysics.fluidSpring())
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .graphicsLayer {
                                            alpha = thumbRowProgress.value
                                            translationY = (1f - thumbRowProgress.value) * 40f
                                        }
                                ) {
                                        for (app in displayedApps) {
                                            key(app.componentKey) {
                                                AppItemView(
                                                    app = app,
                                                    onAppClick = onAppClick,
                                                    onAppLongClick = onAppLongClick,
                                                    iconLoader = iconLoader,
                                                    iconShape = iconShape,
                                                    dragDropState = dragDropState,
                                                    onDragDropResult = onDragDropResult,
                                                    modifier = Modifier
                                                        .width(76.dp)
                                                        .onGloballyPositioned { coords ->
                                                            val container = containerCoordinates.value
                                                            if (container != null && container.isAttached && coords.isAttached) {
                                                                appPositions[app.componentKey] = container.localPositionOf(
                                                                    coords,
                                                                    Offset(coords.size.width / 2f, coords.size.height / 2f)
                                                                )
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val scrubProvider = rememberUpdatedState(activeScrubLetter)

                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(gridColumns),
                                    contentPadding = PaddingValues(
                                        start = horizontalPadding,
                                        top = 8.dp,
                                        end = horizontalPadding,
                                        bottom = 12.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = displayedApps,
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
                                            onDragDropResult = onDragDropResult,
                                            activeScrubLetterProvider = { scrubProvider.value },
                                            modifier = Modifier.onGloballyPositioned { coords ->
                                                val container = containerCoordinates.value
                                                if (container != null && container.isAttached && coords.isAttached) {
                                                    appPositions[app.componentKey] = container.localPositionOf(
                                                        coords,
                                                        Offset(coords.size.width / 2f, coords.size.height / 2f)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                var lastTargetIndex by remember { mutableIntStateOf(-1) }

                                if (searchQuery.isBlank() && displayedApps.size > 8) {
                                    AlphabetIndexBar(
                                        onLetterScrolled = { letter ->
                                            activeScrubLetter = letter
                                            val targetIndex = letterIndexMap[letter] ?: 0
                                            if (targetIndex != lastTargetIndex) {
                                                lastTargetIndex = targetIndex
                                                scrollJob?.cancel()
                                                scrollJob = coroutineScope.launch {
                                                    gridState.scrollToItem(targetIndex)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            activeScrubLetter = null
                                            lastTargetIndex = -1
                                        },
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    )
                                }
                            }
                        }
                    }

                    // Frosted Glass Blur Curtain: slides down continuously on upward swipe or dismisses drawer on downward swipe
                    if (isAppListBlurred && curtainProgress < 1f) {
                        val frostedCurtainBrush = remember(isDrawerDark) {
                            if (isDrawerDark) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xE6141218),
                                        Color(0xF21E1B24),
                                        Color(0xEA141218)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xE6FFFFFF),
                                        Color(0xF2F3F4F6),
                                        Color(0xEAFFFFFF)
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = blurCurtainTranslationY.value
                                    alpha = (1f - curtainProgress * 0.4f).coerceIn(0f, 1f)
                                }
                                .background(frostedCurtainBrush)
                                .pointerInput(isAppListBlurred) {
                                    if (!isAppListBlurred) return@pointerInput
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val curtainVelocityTracker = VelocityTracker()
                                        curtainVelocityTracker.addPosition(down.uptimeMillis, down.position)
                                        var isDraggingDown = false
                                        var isDraggingUp = false
                                        var totalDragY = 0f

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: break
                                            if (change.changedToUp()) {
                                                val velocityY = curtainVelocityTracker.calculateVelocity().y
                                                if (isDraggingDown) {
                                                    val shouldClose = rawDragDistanceY > 60f || velocityY > 250f
                                                    if (shouldClose) {
                                                        onClose()
                                                    } else {
                                                        coroutineScope.launch {
                                                            drawerTranslationY.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = FluidSpringPhysics.fluidSpring(),
                                                                initialVelocity = velocityY
                                                            )
                                                            rawDragDistanceY = 0f
                                                        }
                                                    }
                                                } else if (isDraggingUp) {
                                                    val shouldClear = blurCurtainTranslationY.value > containerHeightPx * 0.25f || velocityY < -250f
                                                    coroutineScope.launch {
                                                        if (shouldClear) {
                                                            blurCurtainTranslationY.animateTo(
                                                                targetValue = containerHeightPx,
                                                                animationSpec = FluidSpringPhysics.fluidSpring(),
                                                                initialVelocity = -velocityY
                                                            )
                                                            isAppListBlurred = false
                                                        } else {
                                                            blurCurtainTranslationY.animateTo(
                                                                targetValue = 0f,
                                                                animationSpec = FluidSpringPhysics.fluidSpring(),
                                                                initialVelocity = -velocityY
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    // Tap: smoothly slide curtain down and clear blur
                                                    coroutineScope.launch {
                                                        blurCurtainTranslationY.animateTo(
                                                            targetValue = containerHeightPx,
                                                            animationSpec = FluidSpringPhysics.fluidSpring()
                                                        )
                                                        isAppListBlurred = false
                                                    }
                                                }
                                                break
                                            }

                                            val dragDeltaY = change.position.y - change.previousPosition.y
                                            curtainVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                            totalDragY += dragDeltaY

                                            if (!isDraggingDown && !isDraggingUp) {
                                                if (totalDragY > 12f) {
                                                    isDraggingDown = true
                                                } else if (totalDragY < -12f) {
                                                    isDraggingUp = true
                                                }
                                            }

                                            if (isDraggingDown) {
                                                change.consume()
                                                rawDragDistanceY = (rawDragDistanceY + dragDeltaY).coerceAtLeast(0f)
                                                val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                                                coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                                            } else if (isDraggingUp) {
                                                change.consume()
                                                val newCurtainY = (blurCurtainTranslationY.value - dragDeltaY).coerceIn(0f, containerHeightPx)
                                                coroutineScope.launch { blurCurtainTranslationY.snapTo(newCurtainY) }
                                            }
                                        }
                                    }
                                }
                        ) {
                            // Luminous specular rim across the top of the frosted glass sheet
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                if (isDrawerDark) Color(0x66FFFFFF) else Color(0x40000000),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    DustParticleOverlay(
                        state = dustState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Suggested Apps Row (NOT blurred, always visible, interactive, and aligned below app grid)
                if (frequentApps.isNotEmpty() && searchQuery.isBlank() && activeScrubLetter == null) {
                    FrequentAppsRow(
                        frequentApps = frequentApps,
                        horizontalPadding = horizontalPadding,
                        gridColumns = gridColumns,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        iconLoader = iconLoader,
                        iconShape = iconShape,
                        dragDropState = dragDropState,
                        onDragDropResult = onDragDropResult
                    )
                }

                ActionSuggestionChip(
                    action = suggestedAction,
                    onExecuteAction = { action ->
                        onExecuteAction(action)
                        try {
                            context.startActivity(action.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {}
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 760.dp)
                )

                // Ergonomic Bottom Search Bar: Avoids camera cutout and provides easy one-hand thumb access
                SearchBarHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onClose = onClose,
                    onOpenSettings = onOpenSettings,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 760.dp)
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
                    text = "Search workspace apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                .height(48.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(14.dp)
                )
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

@Composable
private fun FrequentAppsRow(
    frequentApps: List<AppInfo>,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    gridColumns: Int,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    iconLoader: suspend (AppInfo) -> Drawable?,
    iconShape: Shape,
    dragDropState: AppDragDropState?,
    onDragDropResult: ((DragDropResult) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val itemsToShow = remember(frequentApps) { frequentApps.take(6) }
    if (itemsToShow.isEmpty()) return

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val iconSize = if (isSmallScreen) 36.dp else 40.dp
    val effectiveHorizontalPadding = if (isSmallScreen) 8.dp else horizontalPadding.coerceAtLeast(12.dp)

    // ponytail: 6 suggested apps row with proper padding and equal weight per slot, always visible and unblurred; ceiling: single row 6 apps; upgrade path: continuous responsive row.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 760.dp)
            .padding(
                start = effectiveHorizontalPadding,
                end = effectiveHorizontalPadding,
                top = 4.dp,
                bottom = 6.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (app in itemsToShow) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AppItemView(
                    app = app,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    iconLoader = iconLoader,
                    iconSize = iconSize,
                    itemHorizontalPadding = 2.dp,
                    labelFontSize = 10.sp * LocalAppLabelScale.current,
                    iconShape = iconShape,
                    dragDropState = dragDropState,
                    onDragDropResult = onDragDropResult
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
    iconSize: androidx.compose.ui.unit.Dp = 54.dp,
    itemHorizontalPadding: androidx.compose.ui.unit.Dp = 4.dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit? = null,
    iconShape: Shape = CircleShape,
    dragDropState: AppDragDropState? = null,
    onDragDropResult: ((DragDropResult) -> Unit)? = null,
    activeScrubLetterProvider: () -> String? = { null }
) {
    val haptic = LocalHapticFeedback.current
    val labelScale = LocalAppLabelScale.current
    val firstChar = remember(app.label) { app.label.firstOrNull()?.uppercaseChar() }

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
            .padding(vertical = 6.dp, horizontal = itemHorizontalPadding)
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
            .padding(vertical = 6.dp, horizontal = itemHorizontalPadding)
            .semantics {
                contentDescription = "${app.label}${if (app.isWorkProfile) " (Work Profile)" else ""}"
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = itemModifier.graphicsLayer {
            val scrub = activeScrubLetterProvider()
            if (scrub != null) {
                val matches = if (scrub == "#") !(firstChar?.isLetter() == true) else (firstChar == scrub.firstOrNull())
                alpha = if (matches) 1f else 0.35f
                scaleX = if (matches) 1.05f else 0.98f
                scaleY = if (matches) 1.05f else 0.98f
            } else {
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
            }
        }
    ) {
        AppIconImage(
            key = app.componentKey,
            size = iconSize,
            label = app.label,
            isWorkProfile = app.isWorkProfile,
            iconShape = iconShape,
            iconLoader = { iconLoader(app) }
        )
        Spacer(modifier = Modifier.height(4.dp))
        val resolvedFontSize = labelFontSize ?: (MaterialTheme.typography.bodySmall.fontSize * labelScale)
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = resolvedFontSize),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
