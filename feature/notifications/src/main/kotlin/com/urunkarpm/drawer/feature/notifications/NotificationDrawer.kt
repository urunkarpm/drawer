package com.urunkarpm.drawer.feature.notifications

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.NotificationItem
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
import com.urunkarpm.drawer.core.designsystem.theme.StealthColors
import com.urunkarpm.drawer.feature.notifications.component.NotificationRulesBottomSheet
import com.urunkarpm.drawer.feature.notifications.component.QuickMuteBottomSheet
import com.urunkarpm.drawer.core.designsystem.modifier.autoCloseOnInactivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDrawer(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    // Optional icon loader: given a package name, loads the app icon drawable.
    // Passed in from HomeScreen which already owns an AppRepository reference.
    iconLoader: (suspend (String) -> Drawable?)? = null,
    isDragging: Boolean = false,
    // ponytail: stealth colors applied only to notification cards; no new libraries; ceiling: no per-app masking; upgrade path: StealthColors(dark, light) factory.
    stealthTheme: Boolean = false,
    stealthContrastLevel: String = "LOW",
    stealthLimitBrightness: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val muteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rulesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 380f
        ),
        label = "notif_chevron"
    )

    val notifScrollState = rememberScrollState()
    val notifScrollBlocker = remember(isExpanded) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If scrolling up past top or bottom of notifications, collapse drawer
                if (isExpanded && available.y < -40f) {
                    onExpandedChange(false)
                }
                return Offset(0f, available.y)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = Velocity(0f, available.y)
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            notifScrollState.scrollTo(0)
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val shouldLimitBrightness = stealthTheme && stealthLimitBrightness && isExpanded

    DisposableEffect(shouldLimitBrightness) {
        val window = activity?.window
        val prevBrightness = window?.attributes?.screenBrightness
        if (shouldLimitBrightness && window != null) {
            val lp = window.attributes
            lp.screenBrightness = 0.25f
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = prevBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        val window = activity?.window
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = lp
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkPermission()
        viewModel.rebindService()
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermission()
        viewModel.rebindService()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .autoCloseOnInactivity(
                active = isExpanded,
                resetKey = uiState.activeNotifications.size,
                timeoutMs = 5000L,
                onClose = { onExpandedChange(false) }
            )
    ) {
        // ── Onboarding Banner ─────────────────────────────────────────────────
        if (!uiState.isPermissionGranted) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification Drawer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allow Drawer notification access to see alerts, swipe to dismiss, and quick mute apps right on your home screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.openNotificationAccessSettings() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Enable Access")
                    }
                }
            }

        // ── Notifications Card ────────────────────────────────────────────────
        } else {
            val hasNotifications = uiState.activeNotifications.isNotEmpty()
            val chevronBgColor by animateColorAsState(
                targetValue = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "chevronBgColor"
            )
            val cardBorderColor by animateColorAsState(
                targetValue = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                label = "notifBorderColor"
            )
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded)
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, cardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {

                    // ── Collapsed header (always visible) ─────────────────────
                    val headerInteractionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = headerInteractionSource,
                                indication = null
                            ) {
                                onExpandedChange(!isExpanded)
                            }
                            .pointerInput(isExpanded) {
                                if (isExpanded) {
                                    var dragY = 0f
                                    detectVerticalDragGestures(
                                        onDragStart = { dragY = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            dragY += dragAmount
                                            if (dragY < -30f) {
                                                onExpandedChange(false)
                                                change.consume()
                                            }
                                        }
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stacked app-icon avatars or subtle bell indicator
                        if (hasNotifications) {
                            NotificationIconStack(
                                groups = uiState.groupedNotifications,
                                iconLoader = iconLoader
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications",
                                fontFamily = LocalHeaderFontFamily.current,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val count = uiState.activeNotifications.size
                            Text(
                                text = if (!hasNotifications) "All caught up"
                                else if (isExpanded) "Tap to collapse"
                                else "$count alert${if (count != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Clear all (shown when expanded and there are alerts)
                        if (isExpanded && hasNotifications) {
                            IconButton(
                                onClick = {
                                    viewModel.clearAllNotifications()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Animated chevron
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(chevronBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = chevronRotation }
                            )
                        }
                    }

                    // ── Expanded body ─────────────────────────────────────────
                    AnimatedVisibility(
                        visible = isExpanded,
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
                            animationSpec = tween(
                                durationMillis = 260,
                                easing = FastOutSlowInEasing
                            )
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + slideOutVertically(
                            targetOffsetY = { -it / 8 },
                            animationSpec = tween(
                                durationMillis = 180,
                                easing = FastOutLinearInEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = 150,
                                easing = FastOutSlowInEasing
                            )
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            if (!hasNotifications) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No notifications\nYou're all caught up!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                val configuration = LocalConfiguration.current
                                val maxNotifHeight = if (configuration.screenHeightDp >= 800) 400.dp else 280.dp
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = maxNotifHeight)
                                        .nestedScroll(notifScrollBlocker)
                                        .verticalScroll(notifScrollState, enabled = !isDragging)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    uiState.groupedNotifications.forEach { appGroup ->
                                        key(appGroup.packageName) {
                                            AppNotificationGroupCard(
                                                group = appGroup,
                                                privacyMode = uiState.privacyMode,
                                                swipeRightAction = uiState.swipeRightAction,
                                                swipeLeftAction = uiState.swipeLeftAction,
                                                iconLoader = iconLoader,
                                                onOpen = { item ->
                                                    viewModel.openNotification(item.key)
                                                },
                                                onDismiss = { item ->
                                                    viewModel.dismissNotification(item.key)
                                                },
                                                onMarkAsRead = { item ->
                                                    viewModel.markNotificationAsRead(item.key)
                                                },
                                                onQuickMute = { item ->
                                                    viewModel.selectItemForMute(item)
                                                },
                                                stealthTheme = stealthTheme,
                                                stealthContrastLevel = stealthContrastLevel
                                            )
                                        }
                                    }
                                }
                            }

                            // Footer actions row: Privacy mode & Mute rules
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.togglePrivacyMode()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.privacyMode) Icons.Default.VisibilityOff
                                                      else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.privacyMode) "Privacy On" else "Privacy Off",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.setShowRulesSheet(true)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Rules (${uiState.mutedRules.size})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Mute Bottom Sheet
    uiState.selectedItemForMute?.let { item ->
        QuickMuteBottomSheet(
            item = item,
            sheetState = muteSheetState,
            onDismissRequest = { viewModel.dismissMuteSheet() },
            onMute = { ruleType, durationMillis, autoDismiss ->
                viewModel.muteApp(item.packageName, ruleType, durationMillis, autoDismiss)
            },
            onOpenSystemSettings = {
                viewModel.openAppNotificationSettings(item.packageName)
            }
        )
    }

    // Muted Rules Bottom Sheet
    if (uiState.showRulesSheet) {
        NotificationRulesBottomSheet(
            rules = uiState.mutedRules,
            sheetState = rulesSheetState,
            onDismissRequest = { viewModel.setShowRulesSheet(false) },
            onUnmute = { pkg -> viewModel.unmuteApp(pkg) }
        )
    }
}

// ── Private helpers ────────────────────────────────────────────────────────────

/**
 * Displays notification app icons in the notification card header.
 *
 * - If user has multiple notifications from similar/same apps, only one icon is shown
 *   for that app ("one app one icon even if there are multiple notifications").
 * - When there are multiple notifications from different apps, it keeps the app icons
 *   switching in a circular motion like an orbit showing all the icons.
 */
@Composable
private fun NotificationIconStack(
    groups: List<AppNotificationGroup>,
    iconLoader: (suspend (String) -> Drawable?)? = null,
    modifier: Modifier = Modifier
) {
    val distinctApps = remember(groups) {
        groups.distinctBy { it.packageName }
    }

    if (distinctApps.isEmpty()) return

    if (distinctApps.size == 1) {
        // One app one icon even if there are multiple notifications
        val group = distinctApps.first()
        val iconSize = 30.dp

        Box(
            modifier = modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            // ponytail: Borderless high-resolution app icon with subtle natural elevation replaces harsh grey border outline; ceiling: circular clip; upgrade path: match launcher adaptive shape.
            AppIconImage(
                key = group.packageName,
                size = iconSize,
                label = group.appName,
                iconShape = CircleShape,
                iconLoader = if (iconLoader != null) {
                    { iconLoader(group.packageName) }
                } else null,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = 2.dp.toPx()
                    shape = CircleShape
                    clip = true
                }
            )
        }
    } else {
        // Multiple notifications from different apps:
        // Keep the app icons switching in a circular motion like orbit showing all the icons
        NotificationIconOrbit(
            apps = distinctApps,
            iconLoader = iconLoader,
            modifier = modifier
        )
    }
}

/**
 * Renders distinct app icons cycling smoothly in a 3D elliptical orbit,
 * bringing each notifying app into the spotlight while supporting icons
 * glide along a depth-curved trajectory.
 */
// ponytail: continuous depth orbital projection calculates 2.5D elliptical (X, Y, depth) coordinates and dynamic zIndex/alpha/scale in draw & layout phase; ceiling: fixed semi-axes (rx, ry); upgrade path: interactive touch orbit scrubbing or 3D perspective distortion.
@Composable
private fun NotificationIconOrbit(
    apps: List<AppNotificationGroup>,
    iconLoader: (suspend (String) -> Drawable?)? = null,
    modifier: Modifier = Modifier
) {
    val count = apps.size
    val containerSize = 42.dp

    // Stable ordering so existing apps don't randomly shuffle positions when a new notification arrives
    val stableApps = remember(apps.map { it.packageName }.toSet()) { apps }

    // Orbital geometry
    val radiusXDp = when {
        count <= 2 -> 8.5.dp
        count <= 3 -> 10.dp
        else -> 11.dp
    }
    val radiusYDp = when {
        count <= 2 -> 3.8.dp
        count <= 3 -> 4.5.dp
        else -> 5.dp
    }
    val baseIconSize = when {
        count <= 2 -> 24.dp
        count <= 3 -> 22.dp
        else -> 20.5.dp
    }

    val rotationAngle = remember { Animatable(0f) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(count, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(3200L) // Stable dwell time so the spotlighted foreground app is crisp and readable
                val step = (2.0 * Math.PI / count).toFloat()
                rotationAngle.animateTo(
                    targetValue = rotationAngle.value + step,
                    animationSpec = tween(
                        durationMillis = 750,
                        easing = FastOutSlowInEasing
                    )
                )
                val twoPi = (2.0 * Math.PI).toFloat()
                if (rotationAngle.value >= twoPi) {
                    rotationAngle.snapTo(rotationAngle.value % twoPi)
                }
            }
        }
    }

    val stepAngle = (2.0 * Math.PI / count).toFloat()
    // Symmetrical offset for 2 icons so both overlap gracefully while resting; centered for 3+
    val baseAngle = if (count == 2) (Math.PI / 4.0).toFloat() else 0f

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        stableApps.forEachIndexed { index, group ->
            key(group.packageName) {
                val currentAngle = rotationAngle.value + baseAngle - (index * stepAngle)
                val cosVal = cos(currentAngle.toDouble()).toFloat()
                val sinVal = sin(currentAngle.toDouble()).toFloat()
                // Continuous depth: 1.0 (dead front) to 0.0 (far rear)
                val depth = ((cosVal + 1f) / 2f).coerceIn(0f, 1f)

                // ponytail: Borderless high-fidelity orbiting app icon with depth-scaled natural shadow replaces harsh grey border outline; ceiling: recomposition per frame during step animation; upgrade path: custom LayoutModifier with layout-phase zIndex.
                AppIconImage(
                    key = group.packageName,
                    size = baseIconSize,
                    label = group.appName,
                    iconShape = CircleShape,
                    iconLoader = if (iconLoader != null) {
                        { iconLoader(group.packageName) }
                    } else null,
                    modifier = Modifier
                        .zIndex(depth)
                        .graphicsLayer {
                            val scale = 0.70f + (0.30f * depth)
                            val rx = radiusXDp.toPx()
                            val ry = radiusYDp.toPx()

                            translationX = rx * sinVal
                            translationY = ry * cosVal // True 2.5D elliptical curve
                            scaleX = scale
                            scaleY = scale
                            this.alpha = when {
                                count <= 3 -> 0.58f + (0.42f * depth)
                                count == 4 -> 0.35f + (0.65f * depth)
                                else -> ((depth - 0.16f) / 0.84f).coerceIn(0f, 1f)
                            }
                            shadowElevation = (depth * 3.dp.toPx()).coerceAtLeast(0f)
                            shape = CircleShape
                            clip = true
                        }
                )
            }
        }
    }
}

@Composable
private fun AppNotificationGroupCard(
    group: AppNotificationGroup,
    privacyMode: Boolean,
    swipeRightAction: NotificationSwipeAction,
    swipeLeftAction: NotificationSwipeAction,
    iconLoader: (suspend (String) -> Drawable?)?,
    onOpen: (NotificationItem) -> Unit,
    onDismiss: (NotificationItem) -> Unit,
    onMarkAsRead: (NotificationItem) -> Unit,
    onQuickMute: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
    stealthTheme: Boolean = false,
    stealthContrastLevel: String = "LOW"
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stealthTheme) StealthColors.cardBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            contentColor = if (stealthTheme) StealthColors.titleColor(stealthContrastLevel) else MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // App header row: icon + name + mute button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconImage(
                        key = group.packageName,
                        size = 24.dp,
                        label = group.appName,
                        iconShape = CircleShape,
                        iconLoader = if (iconLoader != null) {
                            { iconLoader(group.packageName) }
                        } else null,
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (stealthTheme) Modifier.graphicsLayer { alpha = StealthColors.iconAlpha }
                                else Modifier
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (stealthTheme) StealthColors.titleColor(stealthContrastLevel) else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (group.items.isNotEmpty()) {
                    IconButton(
                        onClick = { onQuickMute(group.items.first()) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Quick mute ${group.appName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Individual notification rows with distinct keys and proper spacing
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.items.forEach { item ->
                    key(item.key) {
                        DismissibleNotificationRow(
                            item = item,
                            appName = group.appName,
                            privacyMode = privacyMode,
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            onOpen = { onOpen(item) },
                            onDismiss = { onDismiss(item) },
                            onMarkAsRead = { onMarkAsRead(item) },
                            onInteraction = onInteraction,
                            stealthTheme = stealthTheme,
                            stealthContrastLevel = stealthContrastLevel
                        )
                    }
                }
            }
        }
    }
}

// ponytail: Clean, battery-efficient swipe-to-dismiss with threshold haptic, dynamic bidirectional reveal, and hardware-accelerated collapse before committing action to prevent animation cut-off; ceiling: fixed 180ms delay matching tween duration; upgrade path: AnchoredDraggable with custom SettleVelocityTracker.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleNotificationRow(
    item: NotificationItem,
    appName: String,
    privacyMode: Boolean,
    swipeRightAction: NotificationSwipeAction,
    swipeLeftAction: NotificationSwipeAction,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
    stealthTheme: Boolean = false,
    stealthContrastLevel: String = "LOW"
) {
    var pendingAction by remember { mutableStateOf<NotificationSwipeAction?>(null) }
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.38f },
        confirmValueChange = { value ->
            val action = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> swipeRightAction
                SwipeToDismissBoxValue.EndToStart -> swipeLeftAction
                else -> null
            }
            if (item.isClearable && action != null && action != NotificationSwipeAction.NONE) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pendingAction = action
                true
            } else false
        }
    )

    // Keep notification drawer open while user is interacting
    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            onInteraction()
        }
    }

    // Smooth exit: allow the swipe settle and vertical height collapse to finish completely before executing callback
    LaunchedEffect(pendingAction) {
        val action = pendingAction ?: return@LaunchedEffect
        onInteraction()
        delay(180)
        when (action) {
            NotificationSwipeAction.MARK_AS_READ -> onMarkAsRead()
            NotificationSwipeAction.DISMISS -> onDismiss()
            NotificationSwipeAction.OPEN -> onOpen()
            NotificationSwipeAction.NONE -> {}
        }
    }

    AnimatedVisibility(
        visible = pendingAction == null,
        enter = fadeIn(),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 130)
        )
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = item.isClearable && swipeRightAction != NotificationSwipeAction.NONE,
            enableDismissFromEndToStart = item.isClearable && swipeLeftAction != NotificationSwipeAction.NONE,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val action = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> swipeRightAction
                    SwipeToDismissBoxValue.EndToStart -> swipeLeftAction
                    else -> null
                }
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
                val bgColor = when (action) {
                    NotificationSwipeAction.MARK_AS_READ -> MaterialTheme.colorScheme.primaryContainer
                    NotificationSwipeAction.DISMISS -> MaterialTheme.colorScheme.errorContainer
                    NotificationSwipeAction.OPEN -> MaterialTheme.colorScheme.secondaryContainer
                    NotificationSwipeAction.NONE, null -> Color.Transparent
                }
                val contentColor = when (action) {
                    NotificationSwipeAction.MARK_AS_READ -> MaterialTheme.colorScheme.onPrimaryContainer
                    NotificationSwipeAction.DISMISS -> MaterialTheme.colorScheme.onErrorContainer
                    NotificationSwipeAction.OPEN -> MaterialTheme.colorScheme.onSecondaryContainer
                    NotificationSwipeAction.NONE, null -> Color.Transparent
                }
                val icon = when (action) {
                    NotificationSwipeAction.MARK_AS_READ -> Icons.Default.Check
                    NotificationSwipeAction.DISMISS -> Icons.Default.Delete
                    NotificationSwipeAction.OPEN -> Icons.AutoMirrored.Filled.OpenInNew
                    NotificationSwipeAction.NONE, null -> null
                }
                val label = when (action) {
                    NotificationSwipeAction.MARK_AS_READ -> "Mark as read"
                    NotificationSwipeAction.DISMISS -> "Delete"
                    NotificationSwipeAction.OPEN -> "Open"
                    NotificationSwipeAction.NONE, null -> ""
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = alignment
                ) {
                    if (icon != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (direction == SwipeToDismissBoxValue.EndToStart) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor
                                )
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor
                                )
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            },
            content = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (stealthTheme) StealthColors.cardBackground else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 1.dp,
                    modifier = modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpen() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (privacyMode) appName else item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (stealthTheme) StealthColors.titleColor(stealthContrastLevel) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatTimeAgo(item.postTimeMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (stealthTheme) StealthColors.bodyColor(stealthContrastLevel) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.isClearable) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        pendingAction = NotificationSwipeAction.DISMISS
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = if (stealthTheme) StealthColors.bodyColor(stealthContrastLevel) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        if (!privacyMode && item.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (stealthTheme) StealthColors.bodyColor(stealthContrastLevel) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        )
    }
}

private fun formatTimeAgo(timeMillis: Long): String {
    val diff = (System.currentTimeMillis() - timeMillis).coerceAtLeast(0L)
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${hours / 24}d"
    }
}
