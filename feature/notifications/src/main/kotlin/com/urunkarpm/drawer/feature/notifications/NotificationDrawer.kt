package com.urunkarpm.drawer.feature.notifications

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.NotificationItem
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
import com.urunkarpm.drawer.feature.notifications.component.NotificationRulesBottomSheet
import com.urunkarpm.drawer.feature.notifications.component.QuickMuteBottomSheet
import com.urunkarpm.drawer.core.designsystem.modifier.autoCloseOnInactivity
import kotlinx.coroutines.delay

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
    isDragging: Boolean = false
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
    val notifScrollBlocker = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(0f, available.y)

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
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                    .rotate(chevronRotation)
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
                                                }
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
 * Shows overlapping small app-icon circles (up to 4) in the collapsed notification
 * header, giving it an immersive, icon-rich look.
 */
@Composable
private fun NotificationIconStack(
    groups: List<AppNotificationGroup>,
    iconLoader: (suspend (String) -> Drawable?)? = null,
    modifier: Modifier = Modifier
) {
    val displayGroups = groups.take(4)
    val iconSize = 26.dp
    val overlap = 10.dp

    // Total width = iconSize + overlap*(n-1)
    val totalWidth = iconSize + overlap * (displayGroups.size - 1).coerceAtLeast(0)
    Box(modifier = modifier.height(iconSize).width(totalWidth)) {
        displayGroups.forEachIndexed { index, group ->
            val isAndroidSystem = group.packageName == "android"
            Box(
                modifier = Modifier
                    .offset(x = overlap * index)
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(
                        if (isAndroidSystem) androidx.compose.ui.graphics.Color(0xFF3DDC84)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                AppIconImage(
                    key = group.packageName,
                    size = iconSize,
                    label = group.appName,
                    iconLoader = if (iconLoader != null) {
                        { iconLoader(group.packageName) }
                    } else null
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
    onInteraction: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                    val isAndroidSystem = group.packageName == "android"
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAndroidSystem) androidx.compose.ui.graphics.Color(0xFF3DDC84)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        AppIconImage(
                            key = group.packageName,
                            size = 24.dp,
                            label = group.appName,
                            iconLoader = if (iconLoader != null) {
                                { iconLoader(group.packageName) }
                            } else null
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = group.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
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
                            onInteraction = onInteraction
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
    onInteraction: () -> Unit = {}
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
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
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
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatTimeAgo(item.postTimeMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
