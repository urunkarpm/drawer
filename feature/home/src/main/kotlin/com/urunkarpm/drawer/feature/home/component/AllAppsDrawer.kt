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
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.animation.core.AnimationVector1D
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
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import com.urunkarpm.drawer.core.designsystem.theme.rememberDeviceScreenCurve
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.home.dragdrop.AppDragDropState
import com.urunkarpm.drawer.feature.home.dragdrop.DragDropResult
import com.urunkarpm.drawer.core.designsystem.modifier.appDragSource
import kotlinx.coroutines.launch
import androidx.compose.runtime.key

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
    drawerLiquidGlass: Boolean = false,
    drawerLook: String = if (drawerLiquidGlass) "LIQUID_GLASS" else "SOLID",
    matchDeviceScreenCurve: Boolean = true,
    deviceCurveOffsetDp: Float = 0f,
    surfaceCornerRadiusDp: Float = 24f,
    autoOpenKeyboard: Boolean = false,
    blurAppListOnOpen: Boolean = true,
    appDrawerColumns: Int = 4,
    onOpenSettings: (() -> Unit)? = null,
    frequentApps: List<AppInfo> = emptyList()
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

    val isLiquidGlassActive = (drawerLook == "LIQUID_GLASS" || (drawerLook == "SOLID" && drawerLiquidGlass))
    val isFrostedGlassActive = (drawerLook == "FROSTED_GLASS")
    val isAnyGlassActive = isLiquidGlassActive || isFrostedGlassActive

    // Wrap the entire app drawer with DrawerTheme so all typography, search bars, and app labels use correct theme contrast
    DrawerTheme(darkTheme = isDrawerDark) {
        // ponytail: original fluid critically damped spring transition settling around 320ms; ceiling: vertical slide and fade; upgrade path: shared element morph.
        val enterTransition = if (isAnyGlassActive) {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = FluidSpringPhysics.fluidSpring(dampingRatio = 0.82f, stiffness = 380f)
            ) + fadeIn(
                animationSpec = FluidSpringPhysics.fluidSpring(dampingRatio = 0.90f, stiffness = 380f)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = FluidSpringPhysics.fluidSpring(dampingRatio = 0.82f, stiffness = 380f)
            )
        } else {
            slideInVertically(
                initialOffsetY = { it },
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = FluidSpringPhysics.FLUID_STIFFNESS)
            ) + fadeIn(
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 400f)
            )
        }

        val exitTransition = if (isAnyGlassActive) {
            slideOutVertically(
                targetOffsetY = { it },
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 420f)
            ) + fadeOut(
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 420f)
            ) + scaleOut(
                targetScale = 0.94f,
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 420f)
            )
        } else {
            slideOutVertically(
                targetOffsetY = { it },
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 450f)
            ) + fadeOut(
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 450f)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 450f)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = enterTransition,
            exit = exitTransition,
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

            LaunchedEffect(visible) {
                if (visible && blurAppListOnOpen) {
                    isAppListBlurred = true
                }
            }

            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotBlank() && isAppListBlurred) {
                    isAppListBlurred = false
                }
            }

            val specularSheenProgress = remember { Animatable(0f) }
            LaunchedEffect(visible, isAnyGlassActive) {
                if (visible && isAnyGlassActive) {
                    specularSheenProgress.snapTo(0f)
                    specularSheenProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
                    )
                }
            }

            val blurTransition by animateFloatAsState(
                targetValue = if (isAppListBlurred) 1f else 0f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "blurTransition"
            )

            // Apps are already filtered in HomeViewModel; use directly to prevent double-allocation jank
            val displayedApps = apps

            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp
            val selectedColumns = appDrawerColumns.coerceIn(4, 6)
            val gridColumns = when {
                screenWidth < 600 -> selectedColumns
                screenWidth < 840 -> maxOf(selectedColumns, 6)
                screenWidth < 1100 -> maxOf(selectedColumns, 7)
                else -> maxOf(selectedColumns, 8)
            }
            val horizontalPadding = when {
                screenWidth < 360 -> if (gridColumns >= 6) 6.dp else if (gridColumns == 5) 8.dp else 12.dp
                screenWidth >= 840 -> 24.dp
                gridColumns >= 6 -> 10.dp
                gridColumns == 5 -> 12.dp
                else -> 16.dp
            }
            val gridSpacing = when {
                gridColumns >= 6 -> 6.dp
                gridColumns == 5 -> 8.dp
                else -> 12.dp
            }
            val gridVerticalSpacing = when {
                gridColumns >= 6 -> 12.dp
                gridColumns == 5 -> 14.dp
                else -> 16.dp
            }
            val gridIconSize = when {
                gridColumns >= 6 -> if (screenWidth < 360) 36.dp else 40.dp
                gridColumns == 5 -> if (screenWidth < 360) 42.dp else 46.dp
                else -> if (screenWidth < 360) 48.dp else 54.dp
            }
            val gridItemPadding = when {
                gridColumns >= 6 -> 1.dp
                gridColumns == 5 -> 2.dp
                else -> 4.dp
            }
            val gridLabelFontSize = when {
                gridColumns >= 6 -> 10.sp * LocalAppLabelScale.current
                gridColumns == 5 -> 11.sp * LocalAppLabelScale.current
                else -> null
            }

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

            // 100% solid, crisp background in White, Dark, or System mode — or Apple Liquid Glass / Frosted Glass when enabled
            val drawerBackgroundColor = if (isDrawerDark) Color(0xFF141218) else Color(0xFFFFFFFF)
            val drawerBorder = remember(isDrawerDark) { BorderStroke(1.dp, if (isDrawerDark) Color(0xFF2B2930) else Color(0xFFE5E7EB)) }

            val detectedScreenCurve = rememberDeviceScreenCurve(fallbackDp = surfaceCornerRadiusDp)
            val drawerShape = remember(matchDeviceScreenCurve, detectedScreenCurve, deviceCurveOffsetDp, surfaceCornerRadiusDp) {
                if (matchDeviceScreenCurve) {
                    detectedScreenCurve.toRoundedCornerShape(offsetDp = deviceCurveOffsetDp, minDp = 0f)
                } else {
                    RoundedCornerShape(
                        topStart = surfaceCornerRadiusDp.dp,
                        topEnd = surfaceCornerRadiusDp.dp,
                        bottomStart = (surfaceCornerRadiusDp * 0.75f).dp,
                        bottomEnd = (surfaceCornerRadiusDp * 0.75f).dp
                    )
                }
            }

            // Apple Liquid Glass styling: crystalline translucent glass substrate with sharp specular sheen
            val liquidGlassBackgroundBrush = remember(isDrawerDark) {
                if (isDrawerDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC1F1E2B), // Specular smoky top
                            Color(0xDD121118), // Rich obsidian body
                            Color(0xF00A090F)  // Grounded deep base for crisp icon readability
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDBFFFFFF), // Crystalline pearl top
                            Color(0xD0EEF1F6), // Diffuse frosted mist
                            Color(0xDEE0E4EC)  // Clean grounded base
                        )
                    )
                }
            }

            // Frosted Liquid Glass styling: deeper velvety satin diffusion with soft frosted ambient glow
            val frostedGlassBackgroundBrush = remember(isDrawerDark) {
                if (isDrawerDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE8262433), // Velvety satin smoky top
                            Color(0xF2181722), // Soft obsidian frosted body
                            Color(0xF80E0D14)  // Grounded deep base
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF2FFFFFF), // High-opacity pearl top
                            Color(0xEBF0F3F8), // Soft misty frosted body
                            Color(0xF4E4E8F0)  // Grounded satin base
                        )
                    )
                }
            }

            val glassBorderStroke = remember(isDrawerDark) {
                BorderStroke(
                    width = 1.25.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDrawerDark) {
                            listOf(
                                Color.White.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        }
                    )
                )
            }

            val frostedBorderStroke = remember(isDrawerDark) {
                BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDrawerDark) {
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.90f),
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.30f)
                            )
                        }
                    )
                )
            }

            val activeBorderStroke = remember(isFrostedGlassActive, isDrawerDark) {
                if (isFrostedGlassActive) frostedBorderStroke else glassBorderStroke
            }

            val sheenPosition = specularSheenProgress.value
            val specularSheenBrush = remember(sheenPosition, isDrawerDark) {
                Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    (sheenPosition - 0.20f).coerceIn(0f, 1f) to Color.Transparent,
                    (sheenPosition - 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = if (isDrawerDark) 0.35f else 0.50f),
                    sheenPosition.coerceIn(0f, 1f) to Color.White.copy(alpha = if (isDrawerDark) 0.95f else 1.0f),
                    (sheenPosition + 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = if (isDrawerDark) 0.35f else 0.50f),
                    (sheenPosition + 0.20f).coerceIn(0f, 1f) to Color.Transparent,
                    1.0f to Color.Transparent
                )
            }

            // ponytail: translationY on the outer Surface moves the entire drawer background card together with content on pull-to-dismiss; ceiling: 1D vertical displacement without 2D tilt; upgrade path: AnchoredDraggableState.
            Surface(
                shape = drawerShape,
                border = if (isAnyGlassActive) activeBorderStroke else drawerBorder,
                color = if (isAnyGlassActive) Color.Transparent else drawerBackgroundColor,
                shadowElevation = if (isAnyGlassActive) 16.dp else 0.dp,
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = drawerTranslationY.value
                        alpha = animatedDrawerAlpha
                        if (isAnyGlassActive && drawerTranslationY.value > 0f) {
                            val compression = (drawerTranslationY.value / 3200f).coerceIn(0f, 0.035f)
                            scaleX = 1f - compression * 0.7f
                            scaleY = 1f - compression
                        }
                    }
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isAnyGlassActive) {
                    // 1. Translucent glass gradient fill
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isFrostedGlassActive) frostedGlassBackgroundBrush else liquidGlassBackgroundBrush)
                    )

                    // 2. Subtle chromatic refraction / frosted diffusion wash
                    val innerGlowBrush = remember(isFrostedGlassActive, isDrawerDark) {
                        Brush.radialGradient(
                            colors = if (isFrostedGlassActive) {
                                listOf(
                                    Color.White.copy(alpha = if (isDrawerDark) 0.12f else 0.20f),
                                    Color.White.copy(alpha = if (isDrawerDark) 0.03f else 0.06f),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = if (isDrawerDark) 0.06f else 0.12f),
                                    Color.Transparent
                                )
                            },
                            radius = if (isFrostedGlassActive) 1200f else 800f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(innerGlowBrush)
                    )

                    // 3. Specular light sweep line across the top rim curve
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isFrostedGlassActive) 3.5.dp else 2.5.dp)
                            .background(specularSheenBrush)
                            .align(Alignment.TopCenter)
                    )

                    // 4. Ambient top specular rim highlight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = if (isDrawerDark) 0.40f else 0.70f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .align(Alignment.TopCenter)
                    )
                }

            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            .background(
                                if (isAnyGlassActive) {
                                    if (isDrawerDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                            .then(
                                if (isAnyGlassActive) {
                                    Modifier.border(
                                        BorderStroke(0.5.dp, Color.White.copy(alpha = 0.40f)),
                                        RoundedCornerShape(2.dp)
                                    )
                                } else Modifier
                            )
                    )
                }

                // 1. App List Grid Area with privacy blur support
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    // ponytail: static blur when privacy mode is active; crossfade alpha overlay instead of dynamic radius recalculation to eliminate GPU pipeline stalls and Box recompositions during drawer open.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isAppListBlurred) Modifier.blur(18.dp) else Modifier)
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
                                                    modifier = Modifier.width(76.dp)
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
                                    verticalArrangement = Arrangement.spacedBy(gridVerticalSpacing),
                                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
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
                                            iconSize = gridIconSize,
                                            itemHorizontalPadding = gridItemPadding,
                                            labelFontSize = gridLabelFontSize,
                                            iconShape = iconShape,
                                            dragDropState = dragDropState,
                                            onDragDropResult = onDragDropResult,
                                            activeScrubLetterProvider = { scrubProvider.value }
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

                    // Frosted privacy scrim: cleanly obscures apps until tapped or searched; zero gesture clashing
                    if (blurTransition > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = blurTransition }
                                .background(
                                    if (isDrawerDark) Color(0x40000000) else Color(0x33FFFFFF)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isAppListBlurred = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDrawerDark) Color(0xD91E1B24) else Color(0xF0FFFFFF),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDrawerDark) Color(0x33FFFFFF) else Color(0x1F000000)
                                ),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Tap to reveal apps",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
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

                // Ergonomic Bottom Search Bar: Avoids camera cutout and provides easy one-hand thumb access
                SearchBarHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onClose = onClose,
                    onOpenSettings = onOpenSettings,
                    focusRequester = searchFocusRequester,
                    isLiquidGlass = isAnyGlassActive,
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
}

@Composable
private fun SearchBarHeader(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isLiquidGlass: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val crispHaptics = LocalCrispHaptics.current
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            crispHaptics.click()
            onClose()
        }) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close app drawer",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
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
                    IconButton(onClick = {
                        crispHaptics.click()
                        onSearchQueryChanged("")
                    }) {
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
            shape = if (isLiquidGlass) RoundedCornerShape(18.dp) else RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (isLiquidGlass) {
                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                unfocusedContainerColor = if (isLiquidGlass) {
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
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
                .heightIn(min = 52.dp)
                .then(
                    if (isLiquidGlass) {
                        Modifier.border(
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.45f else 0.70f),
                                        Color.White.copy(alpha = if (isDark) 0.10f else 0.20f)
                                    )
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                )
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        )

        if (onOpenSettings != null) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    crispHaptics.click()
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
    val itemsToShow = remember(frequentApps, gridColumns) { frequentApps.take(gridColumns) }
    if (itemsToShow.isEmpty()) return

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360
    val (iconSize, suggestedFontSize, suggestedSpacing) = when {
        gridColumns <= 4 -> Triple(
            if (isSmallScreen) 38.dp else 42.dp,
            11.sp * LocalAppLabelScale.current,
            12.dp
        )
        gridColumns == 5 -> Triple(
            if (isSmallScreen) 34.dp else 38.dp,
            10.5.sp * LocalAppLabelScale.current,
            8.dp
        )
        else -> Triple(
            if (isSmallScreen) 30.dp else 34.dp,
            10.sp * LocalAppLabelScale.current,
            6.dp
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 760.dp)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 4.dp,
                bottom = 6.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(suggestedSpacing, Alignment.CenterHorizontally),
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
                    itemHorizontalPadding = if (gridColumns >= 6) 1.dp else 2.dp,
                    labelFontSize = suggestedFontSize,
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
    val crispHaptics = LocalCrispHaptics.current
    val labelScale = LocalAppLabelScale.current
    val firstChar = remember(app.label) { app.label.firstOrNull()?.uppercaseChar() }

    // ponytail: Unified gesture detector combining instantaneous tap execution, list scroll passthrough, and long-press drag-to-drawer; ceiling is Compose pointer slop; upgrade path is native drag-and-drop transfer API.
    val itemModifier = if (dragDropState != null) {
        modifier
            .clip(RoundedCornerShape(12.dp))
            .appDragSource(
                key = app.componentKey,
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
                        crispHaptics.confirm()
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
                onClick = {
                    crispHaptics.click()
                    onAppClick(app)
                },
                onLongClick = {
                    crispHaptics.heavyClick()
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
