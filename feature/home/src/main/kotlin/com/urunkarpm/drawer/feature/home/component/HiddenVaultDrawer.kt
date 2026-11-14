package com.urunkarpm.drawer.feature.home.component

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.designsystem.animation.FluidSpringPhysics
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.rememberDeviceScreenCurve
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.notifications.NotificationDrawer
import com.urunkarpm.drawer.feature.notifications.NotificationsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenVaultDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    hiddenApps: List<AppInfo>,
    allInstalledApps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onUnhideApp: (String) -> Unit,
    onAddHiddenApp: (String) -> Unit = {},
    onAddHiddenApps: (Collection<String>) -> Unit = { pkgs -> pkgs.forEach(onAddHiddenApp) },
    notificationsViewModel: NotificationsViewModel? = null,
    drawerThemeMode: String = "SYSTEM",
    drawerLiquidGlass: Boolean = false,
    drawerLook: String = if (drawerLiquidGlass) "LIQUID_GLASS" else "SOLID",
    surfaceCornerRadiusDp: Float = 24f,
    matchDeviceScreenCurve: Boolean = true,
    deviceCurveOffsetDp: Float = 0f,
    iconLoader: (suspend (AppInfo) -> Drawable?)? = null,
    onOpenAppDetails: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isOpen, onBack = onClose)

    val isSystemDark = isSystemInDarkTheme()
    val isDrawerDark = when (drawerThemeMode.uppercase()) {
        "WHITE", "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val isLiquidGlassActive = (drawerLook == "LIQUID_GLASS" || (drawerLook == "SOLID" && drawerLiquidGlass))
    val isFrostedGlassActive = (drawerLook == "FROSTED_GLASS")
    val isAnyGlassActive = isLiquidGlassActive || isFrostedGlassActive

    var showAddAppsSheet by remember { mutableStateOf(false) }

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

    val drawerBackgroundColor = if (isDrawerDark) Color(0xFF141218) else Color(0xFFFFFFFF)
    val drawerBorder = remember(isDrawerDark) {
        BorderStroke(1.dp, if (isDrawerDark) Color(0xFF2B2930) else Color(0xFFE5E7EB))
    }

    // Liquid Glass styling
    val liquidGlassBackgroundBrush = remember(isDrawerDark) {
        if (isDrawerDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xCC1F1E2B),
                    Color(0xDD121118),
                    Color(0xF00A090F)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xDBFFFFFF),
                    Color(0xD0EEF1F6),
                    Color(0xDEE0E4EC)
                )
            )
        }
    }

    // Frosted Liquid Glass styling
    val frostedGlassBackgroundBrush = remember(isDrawerDark) {
        if (isDrawerDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xE8262433),
                    Color(0xF2181722),
                    Color(0xF80E0D14)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF2FFFFFF),
                    Color(0xEBF0F3F8),
                    Color(0xF4E4E8F0)
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

    val specularSheenProgress = remember { Animatable(0f) }
    LaunchedEffect(isOpen, isAnyGlassActive) {
        if (isOpen && isAnyGlassActive) {
            specularSheenProgress.snapTo(0f)
            specularSheenProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
            )
        }
    }

    // Spring enter/exit transitions matching AllAppsDrawer
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

    DrawerTheme(darkTheme = isDrawerDark) {
        AnimatedVisibility(
            visible = isOpen,
            enter = enterTransition,
            exit = exitTransition,
            modifier = modifier
        ) {
            val gridState = rememberLazyGridState()
            val coroutineScope = rememberCoroutineScope()
            val drawerTranslationY = remember { Animatable(0f) }
            var rawDragDistanceY by remember { mutableFloatStateOf(0f) }
            val handleVelocityTracker = remember { VelocityTracker() }
            var isVaultNotificationExpanded by remember { mutableStateOf(false) }
            var vaultNotificationBounds by remember { mutableStateOf<Rect?>(null) }
            var vaultBoxCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

            LaunchedEffect(isOpen) {
                if (isOpen) {
                    drawerTranslationY.snapTo(0f)
                    rawDragDistanceY = 0f
                }
            }

            // iOS-grade fluid spring & rubber-banding drag state with velocity handoff
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (available.y > 0f && !gridState.canScrollBackward && source == NestedScrollSource.UserInput) {
                            rawDragDistanceY += available.y
                            val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                            coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                            return available
                        } else if (available.y < 0f && rawDragDistanceY > 0f) {
                            rawDragDistanceY = (rawDragDistanceY + available.y).coerceAtLeast(0f)
                            val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                            coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
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

            Surface(
                shape = drawerShape,
                border = if (isAnyGlassActive) activeBorderStroke else drawerBorder,
                color = if (isAnyGlassActive) Color.Transparent else drawerBackgroundColor,
                shadowElevation = if (isAnyGlassActive) 16.dp else 0.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { vaultBoxCoordinates = it }
                    // ponytail: 0ms latency pointer-down collapse for vault notification drawer on outside touch; ceiling: coordinate hit-test against bounds; upgrade path: modal barrier scrim.
                    .pointerInput(isVaultNotificationExpanded) {
                        if (!isVaultNotificationExpanded) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            val rootPos = vaultBoxCoordinates?.takeIf { it.isAttached }?.localToRoot(down.position) ?: down.position
                            val isInside = vaultNotificationBounds?.contains(rootPos) == true
                            if (!isInside) {
                                isVaultNotificationExpanded = false
                                down.consume()
                            }
                        }
                    }
                    .graphicsLayer {
                        translationY = drawerTranslationY.value
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
                        // ponytail: defer sheen progress read to draw phase via drawBehind to prevent 60-120Hz recomposition churn during drawer entrance; ceiling: horizontal sweep only; upgrade path: shader-based caustics.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isFrostedGlassActive) 3.5.dp else 2.5.dp)
                                .drawBehind {
                                    val sheen = specularSheenProgress.value
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            0.0f to Color.Transparent,
                                            (sheen - 0.20f).coerceIn(0f, 1f) to Color.Transparent,
                                            (sheen - 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = if (isDrawerDark) 0.35f else 0.50f),
                                            sheen.coerceIn(0f, 1f) to Color.White.copy(alpha = if (isDrawerDark) 0.95f else 1.0f),
                                            (sheen + 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = if (isDrawerDark) 0.35f else 0.50f),
                                            (sheen + 0.20f).coerceIn(0f, 1f) to Color.Transparent,
                                            1.0f to Color.Transparent
                                        )
                                    )
                                }
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
                        // ── Top subtle drag handle indicator (effortless 1-swipe dismiss) ──
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
                                            if (change.positionChange().y != 0f) {
                                                val dragY = change.positionChange().y
                                                if (dragY > 0f || rawDragDistanceY > 0f) {
                                                    isDragging = true
                                                    change.consume()
                                                    rawDragDistanceY = (rawDragDistanceY + dragY).coerceAtLeast(0f)
                                                    val displacement = FluidSpringPhysics.rubberBand(rawDragDistanceY, 400f)
                                                    coroutineScope.launch { drawerTranslationY.snapTo(displacement) }
                                                    handleVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDrawerDark) Color.White.copy(alpha = 0.28f)
                                        else Color.Black.copy(alpha = 0.22f)
                                    )
                            )
                        }

                        // ── Header: Lock Title + Add Button Only ─────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Private Vault",
                                        fontFamily = LocalHeaderFontFamily.current,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${hiddenApps.size} hidden ${if (hiddenApps.size == 1) "app" else "apps"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showAddAppsSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // ── Vault-Exclusive Notification Drawer ─────────────────
                        if (notificationsViewModel != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                NotificationDrawer(
                                    viewModel = notificationsViewModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coords ->
                                            vaultNotificationBounds = coords.boundsInRoot()
                                        },
                                    isExpanded = isVaultNotificationExpanded,
                                    onExpandedChange = { isVaultNotificationExpanded = it },
                                    forVaultOnly = true,
                                    iconLoader = if (iconLoader != null) {
                                        { pkg ->
                                            val app = allInstalledApps.find { it.packageName == pkg }
                                            if (app != null) iconLoader(app) else null
                                        }
                                    } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Content: App Grid or Empty State ───────────────────
                        if (hiddenApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Your Vault is Empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Apps added to this vault are hidden from all app lists, drawers, dock, and searches. Notifications from them remain private.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showAddAppsSheet = true },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Hide Apps Now")
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = hiddenApps,
                                    key = { it.packageName },
                                    contentType = { "vault_app" }
                                ) { app ->
                                    VaultAppItem(
                                        app = app,
                                        iconLoader = iconLoader,
                                        onClick = { onLaunchApp(app) },
                                        onUnhide = { onUnhideApp(app.packageName) },
                                        onOpenAppDetails = { onOpenAppDetails(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Multi-Select Add Apps Sheet ─────────────────────────────────────────
    if (showAddAppsSheet) {
        val nonHiddenApps = remember(allInstalledApps, hiddenApps) {
            val hiddenPkgSet = hiddenApps.map { it.packageName }.toSet()
            allInstalledApps.filter { it.packageName !in hiddenPkgSet }.sortedBy { it.label.lowercase() }
        }
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = remember(searchQuery, nonHiddenApps) {
            if (searchQuery.isBlank()) nonHiddenApps
            else nonHiddenApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val selectedPackages = remember { mutableStateListOf<String>() }

        ModalBottomSheet(
            onDismissRequest = { showAddAppsSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Apps to Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedPackages.isEmpty()) "Select apps to hide from launcher and public alerts"
                            else "${selectedPackages.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedPackages.isNotEmpty()) {
                        TextButton(onClick = { selectedPackages.clear() }) {
                            Text("Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredList, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in selectedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedPackages.remove(app.packageName)
                                    } else {
                                        selectedPackages.add(app.packageName)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(
                                key = app.componentKey,
                                size = 38.dp,
                                label = app.label,
                                isWorkProfile = app.isWorkProfile,
                                iconLoader = if (iconLoader != null) { { iconLoader(app) } } else null
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (app.packageName !in selectedPackages) selectedPackages.add(app.packageName)
                                    } else {
                                        selectedPackages.remove(app.packageName)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (selectedPackages.isNotEmpty()) {
                            onAddHiddenApps(selectedPackages.toList())
                        }
                        showAddAppsSheet = false
                    },
                    enabled = selectedPackages.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedPackages.isEmpty()) "Select Apps to Hide"
                        else "Add ${selectedPackages.size} ${if (selectedPackages.size == 1) "App" else "Apps"} to Vault"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun VaultAppItem(
    app: AppInfo,
    iconLoader: (suspend (AppInfo) -> Drawable?)?,
    onClick: () -> Unit,
    onUnhide: () -> Unit,
    onOpenAppDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .padding(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIconImage(
                key = app.componentKey,
                size = 52.dp,
                label = app.label,
                isWorkProfile = app.isWorkProfile,
                iconLoader = if (iconLoader != null) { { iconLoader(app) } } else null
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Unhide from Vault") },
                    leadingIcon = { Icon(Icons.Outlined.Visibility, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onUnhide()
                    }
                )
                DropdownMenuItem(
                    text = { Text("App Info") },
                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onOpenAppDetails()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
