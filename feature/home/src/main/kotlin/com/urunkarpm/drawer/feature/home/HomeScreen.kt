package com.urunkarpm.drawer.feature.home

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import com.urunkarpm.drawer.feature.home.component.FrontCameraPreview
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.dock.DockBar
import com.urunkarpm.drawer.feature.groups.GroupsAccordion
import com.urunkarpm.drawer.feature.groups.GroupsViewModel
import com.urunkarpm.drawer.feature.groups.component.CategorySelectionBottomSheet
import com.urunkarpm.drawer.feature.home.component.AllAppsDrawer
import com.urunkarpm.drawer.feature.home.component.AppActionBottomSheet
import com.urunkarpm.drawer.feature.home.component.GlanceHeader
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.rememberCoroutineScope
import com.urunkarpm.drawer.feature.home.dragdrop.DragDropResult
import com.urunkarpm.drawer.feature.home.dragdrop.rememberAppDragDropState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    groupsViewModel: GroupsViewModel = hiltViewModel(),
    notificationsViewModel: com.urunkarpm.drawer.feature.notifications.NotificationsViewModel = hiltViewModel(),
    onDoubleTapLock: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val hasNotifications = notificationsUiState.activeNotifications.isNotEmpty()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val dragDropState = rememberAppDragDropState()
    val hoveredGroupId by remember(dragDropState) {
        derivedStateOf { dragDropState.hoveredGroupId() }
    }
    val isOverDock by remember(dragDropState) {
        derivedStateOf { dragDropState.isOverDock() }
    }
    var appForCategorySelection by remember { mutableStateOf<Pair<AppInfo, String?>?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var showHomeMenu by remember { mutableStateOf(false) }
    var isNotificationExpanded by remember { mutableStateOf(false) }
    var isCameraOpen by remember { mutableStateOf(false) }
    var glanceBounds by remember { mutableStateOf<Rect?>(null) }
    var notificationBounds by remember { mutableStateOf<Rect?>(null) }
    var rootColumnBounds by remember { mutableStateOf<Rect?>(null) }

    val handleDragDropResult: (DragDropResult) -> Unit = { result ->
        when (result) {
            is DragDropResult.DroppedOnDock -> {
                viewModel.pinToDock(result.app)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Pinned \"${result.app.label}\" to Dock")
                }
            }
            is DragDropResult.DroppedOnGroup -> {
                if (result.sourceGroupId != null && result.sourceGroupId != result.targetGroupId) {
                    groupsViewModel.moveAppBetweenGroups(
                        sourceGroupId = result.sourceGroupId,
                        targetGroupId = result.targetGroupId,
                        app = result.app,
                        targetIndex = result.targetIndex
                    )
                    val groupTitle = groupsUiState.groups.find { it.group.id == result.targetGroupId }?.group?.name ?: "Category"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Moved \"${result.app.label}\" to $groupTitle")
                    }
                } else {
                    groupsViewModel.assignAppToGroup(result.targetGroupId, result.app, result.targetIndex)
                    val groupTitle = groupsUiState.groups.find { it.group.id == result.targetGroupId }?.group?.name ?: "Category"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added \"${result.app.label}\" to $groupTitle")
                    }
                }
            }
            is DragDropResult.DroppedOnHome -> {
                appForCategorySelection = result.app to result.sourceGroupId
            }
            DragDropResult.None -> Unit
        }
        if (result !is DragDropResult.None) {
            viewModel.closeAllApps()
        }
    }

    // Auto-dismiss selfie camera mirror after 3 seconds
    LaunchedEffect(isCameraOpen) {
        if (isCameraOpen) {
            kotlinx.coroutines.delay(3000)
            isCameraOpen = false
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCameraOpen = true
        }
    }

    // When an app category group is opened, collapse notification drawer (if not in side-by-side mode)
    LaunchedEffect(groupsUiState.groups) {
        if (!uiState.twoDrawersSideBySide && groupsUiState.groups.any { it.group.isExpanded }) {
            isNotificationExpanded = false
        }
    }

    // When AllApps drawer opens, collapse notification drawer
    LaunchedEffect(uiState.isAllAppsOpen) {
        if (uiState.isAllAppsOpen) {
            isNotificationExpanded = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshWeather()
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    // Back: close camera preview, settings, popup menu, category picker, action sheet, all-apps, notification drawer, or group accordion
    BackHandler(enabled = isCameraOpen || isSettingsOpen || showHomeMenu || uiState.isAllAppsOpen || uiState.selectedAppForMenu != null || appForCategorySelection != null || isNotificationExpanded || groupsUiState.groups.any { it.group.isExpanded }) {
        when {
            isCameraOpen -> isCameraOpen = false
            isSettingsOpen -> isSettingsOpen = false
            showHomeMenu -> showHomeMenu = false
            appForCategorySelection != null -> appForCategorySelection = null
            uiState.selectedAppForMenu != null -> viewModel.dismissAppMenu()
            uiState.isAllAppsOpen -> viewModel.closeAllApps()
            isNotificationExpanded -> isNotificationExpanded = false
            groupsUiState.groups.any { it.group.isExpanded } -> groupsViewModel.collapseAllGroups()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState.hideStatusBar) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.hideStatusBar) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    val areOtherDrawersOpen = uiState.isAllAppsOpen || groupsUiState.groups.any { it.group.isExpanded }

    // Auto-collapse expanded notification state when any drawer opens
    LaunchedEffect(areOtherDrawersOpen) {
        if (areOtherDrawersOpen && isNotificationExpanded) {
            isNotificationExpanded = false
        }
    }

    // Track cumulative vertical drag on the home screen to:
    //  • drag UP (negative) → open All Apps (or collapse expanded notification drawer)
    //  • drag DOWN (positive) → expand notification drawer when notifications exist
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var scrollDownAccumulator by remember { mutableFloatStateOf(0f) }

    // ponytail: nested scroll captures downward drag when home scroll is at 0 to expand notifications and upward drag to open all apps; ceiling: doesn't animate partial drawer pull; upgrade path: AnchoredDraggableState.
    val homeNestedScrollConnection = remember(hasNotifications, isNotificationExpanded, areOtherDrawersOpen) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!areOtherDrawersOpen && hasNotifications && !isNotificationExpanded && available.y > 0f) {
                    scrollDownAccumulator += available.y
                    if (scrollDownAccumulator > 25f) {
                        isNotificationExpanded = true
                        if (!uiState.twoDrawersSideBySide) {
                            groupsViewModel.collapseAllGroups()
                        }
                        scrollDownAccumulator = 0f
                        return Offset(0f, available.y)
                    }
                } else if (available.y < -35f) {
                    if (isNotificationExpanded) {
                        isNotificationExpanded = false
                        return Offset(0f, available.y)
                    } else if (!areOtherDrawersOpen) {
                        viewModel.openAllApps()
                        return Offset(0f, available.y)
                    }
                } else if (available.y < 0f) {
                    scrollDownAccumulator = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                scrollDownAccumulator = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                scrollDownAccumulator = 0f
                return Velocity.Zero
            }
        }
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = modifier
                .fillMaxSize()
                // Transparent so the wallpaper shows through
                .background(Color.Transparent)
        ) {
            // Wallpaper blur effect overlay if enabled
            if (uiState.wallpaperBlur && uiState.wallpaperBlurRadius > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = (uiState.wallpaperBlurRadius / 250f).coerceIn(0.12f, 0.65f)
                            )
                        )
                )
            }

            // ── Home Content ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .onGloballyPositioned { coords ->
                        rootColumnBounds = coords.boundsInRoot()
                    }
                    // ponytail: double-tap on empty screen space locks phone via AccessibilityService; long-press opens quick popup menu (Settings & Add Category); ceiling: simple AlertDialog; upgrade path: customizable desktop widget menu.
                    .pointerInput(uiState.isAllAppsOpen, isSettingsOpen, isCameraOpen) {
                        if (!uiState.isAllAppsOpen && !isSettingsOpen && !isCameraOpen) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    val colBounds = rootColumnBounds ?: Rect.Zero
                                    val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                    val isOverGroup = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                    val isOverApp = dragDropState.appBounds.values.any { groupMap -> groupMap.values.any { it.contains(rootPos) } }
                                    val isOverDock = dragDropState.dockBounds?.contains(rootPos) == true
                                    val isOverGlance = glanceBounds?.contains(rootPos) == true
                                    val isOverNotification = notificationBounds?.contains(rootPos) == true
                                    if (!isOverGroup && !isOverApp && !isOverDock && !isOverGlance && !isOverNotification) {
                                        onDoubleTapLock()
                                    }
                                },
                                onLongPress = { offset ->
                                    val colBounds = rootColumnBounds ?: Rect.Zero
                                    val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                    val isOverGroup = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                    val isOverApp = dragDropState.appBounds.values.any { groupMap -> groupMap.values.any { it.contains(rootPos) } }
                                    val isOverDock = dragDropState.dockBounds?.contains(rootPos) == true
                                    val isOverGlance = glanceBounds?.contains(rootPos) == true
                                    val isOverNotification = notificationBounds?.contains(rootPos) == true
                                    if (!isOverGroup && !isOverApp && !isOverDock && !isOverGlance && !isOverNotification) {
                                        showHomeMenu = true
                                    }
                                }
                            )
                        }
                    },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Glance (Clock, Date & Weather)
                GlanceHeader(
                    is24Hour = uiState.is24Hour,
                    showWeather = uiState.showWeather,
                    weatherUnit = uiState.weatherUnit,
                    weatherInfo = uiState.weatherInfo,
                    onRefreshWeather = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        viewModel.refreshWeather()
                    },
                    onOpenSettings = { isSettingsOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            glanceBounds = coords.boundsInRoot()
                        }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Middle: Notifications (always full width at top) + Category Drawers (side-by-side or stacked vertically)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(homeNestedScrollConnection)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // 1. Notification Drawer: Auto-hides when any drawer is opened, reveals when all drawers close
                    // ponytail: Seamless auto-hide of notification drawer when category drawers or all-apps are open; ceiling is full-hide; upgrade path is persistent unread count badge.
                    AnimatedVisibility(
                        visible = !areOtherDrawersOpen,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(animationSpec = tween(durationMillis = 140))
                    ) {
                        com.urunkarpm.drawer.feature.notifications.NotificationDrawer(
                            viewModel = notificationsViewModel,
                            isExpanded = isNotificationExpanded,
                            onExpandedChange = { expanded ->
                                isNotificationExpanded = expanded
                                if (expanded && !uiState.twoDrawersSideBySide) {
                                    groupsViewModel.collapseAllGroups()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    notificationBounds = coords.boundsInRoot()
                                },
                            iconLoader = { packageName ->
                                val app = uiState.installedAppsByPackage[packageName]
                                if (app != null) viewModel.getAppIcon(app) else null
                            },
                            isDragging = dragDropState.isDragging
                        )
                    }

                    // 2. Rest of the Drawers (App Categories): Side by side (2 columns) if enabled, otherwise single column
                    GroupsAccordion(
                        viewModel = groupsViewModel,
                        twoDrawersSideBySide = uiState.twoDrawersSideBySide,
                        iconShape = com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(
                            uiState.adaptiveIconShape
                        ),
                        hoveredGroupId = hoveredGroupId,
                        onGroupPositioned = { groupId, rect ->
                            dragDropState.groupBounds[groupId] = rect
                        },
                        onAppPositioned = { groupId, app, rect ->
                            dragDropState.registerAppBounds(groupId, app.componentKey, rect)
                        },
                        onAppDragStart = { app, rootPos, sourceGroupId ->
                            dragDropState.startDrag(app, rootPos, sourceGroupId = sourceGroupId)
                        },
                        onAppDrag = { dragAmount ->
                            dragDropState.updateDrag(dragAmount)
                        },
                        onAppDragEnd = { isDropped ->
                            if (isDropped) {
                                val result = dragDropState.endDrag()
                                handleDragDropResult(result)
                            } else {
                                dragDropState.cancelDrag()
                            }
                        },
                        onMoveToCategory = { app, sourceGroupId ->
                            appForCategorySelection = app to sourceGroupId
                        },
                        onPinToDock = { app ->
                            viewModel.pinToDock(app)
                        },
                        isDragging = dragDropState.isDragging,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

            // Bottom: Dock (swiping up from dock opens app drawer instantaneously)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .onGloballyPositioned { coords ->
                        dragDropState.dockBounds = coords.boundsInRoot()
                    }
                    .pointerInput(uiState.isAllAppsOpen, hasNotifications, isNotificationExpanded) {
                        var dockDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dockDrag = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                dockDrag += dragAmount
                                if (!uiState.isAllAppsOpen && dockDrag < -20f) {
                                    if (isNotificationExpanded) {
                                        isNotificationExpanded = false
                                        dockDrag = 0f
                                    } else {
                                        viewModel.openAllApps()
                                        dockDrag = 0f
                                    }
                                } else if (!uiState.isAllAppsOpen && dockDrag > 20f) {
                                    if (hasNotifications && !isNotificationExpanded) {
                                        isNotificationExpanded = true
                                        if (!uiState.twoDrawersSideBySide) {
                                            groupsViewModel.collapseAllGroups()
                                        }
                                        dockDrag = 0f
                                    }
                                }
                            },
                            onDragEnd = { dockDrag = 0f },
                            onDragCancel = { dockDrag = 0f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                DockBar(
                    iconShape = com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(
                        uiState.adaptiveIconShape
                    ),
                    isDropTarget = isOverDock
                )
            }
        }

        // ── All Apps Drawer Overlay (slides up from bottom) ───────────────────
        AllAppsDrawer(
            visible = uiState.isAllAppsOpen,
            apps = uiState.filteredApps,
            searchQuery = uiState.searchQuery,
            isLoading = uiState.isLoading,
            iconShape = com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(
                uiState.adaptiveIconShape
            ),
            dragDropState = if (groupsUiState.lockLayout) null else dragDropState,
            onDragDropResult = handleDragDropResult,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onAppClick = { app -> viewModel.onAppClicked(app) },
            onAppLongClick = { app -> viewModel.onAppLongClicked(app) },
            onClose = { viewModel.closeAllApps() },
            iconLoader = { app -> viewModel.getAppIcon(app) }
        )

        // ── App Long-Press Action Sheet ────────────────────────────────────────
        val selectedApp = uiState.selectedAppForMenu
        if (selectedApp != null) {
            AppActionBottomSheet(
                app = selectedApp,
                onDismissRequest = { viewModel.dismissAppMenu() },
                sheetState = sheetState,
                onLaunch = { viewModel.onAppClicked(selectedApp) },
                onOpenDetails = { viewModel.openAppDetails(selectedApp) },
                onUninstall = { viewModel.uninstallApp(selectedApp) },
                onPinToDock = { viewModel.pinToDock(selectedApp) },
                onAddToGroup = {
                    appForCategorySelection = selectedApp to null
                    viewModel.dismissAppMenu()
                },
                iconLoader = { viewModel.getAppIcon(selectedApp) },
                shortcuts = uiState.shortcuts,
                onLaunchShortcut = { shortcutId -> viewModel.launchShortcut(selectedApp, shortcutId) },
                shortcutIconLoader = { shortcutId -> viewModel.getShortcutIcon(selectedApp, shortcutId) }
            )
        }

        // ── Category Selection Sheet ───────────────────────────────────────────
        val categorySelection = appForCategorySelection
        if (categorySelection != null) {
            val (targetApp, sourceGroupId) = categorySelection
            CategorySelectionBottomSheet(
                app = targetApp,
                groups = groupsUiState.groups.map { it.group },
                sheetState = categorySheetState,
                onDismissRequest = { appForCategorySelection = null },
                onSelectGroup = { group ->
                    if (sourceGroupId != null && sourceGroupId != group.id) {
                        groupsViewModel.moveAppBetweenGroups(sourceGroupId, group.id, targetApp)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Moved \"${targetApp.label}\" to ${group.name}")
                        }
                    } else {
                        groupsViewModel.assignAppToGroup(group.id, targetApp)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Added \"${targetApp.label}\" to ${group.name}")
                        }
                    }
                    appForCategorySelection = null
                },
                onCreateNewGroup = {
                    appForCategorySelection = null
                    groupsViewModel.setShowCreateDialog(true)
                }
            )
        }

        // ── Settings Screen Overlay (slide in from right) ──────────────────────
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            com.urunkarpm.drawer.feature.settings.SettingsScreen(
                onNavigateBack = { isSettingsOpen = false }
            )
        }

        // ── Camera Cutout Touch Target ────────────────────────────────────────
        // ponytail: transparent 110x48dp hit area directly over the top camera cut-out triggers the drop-down selfie mirror; ceiling is top-center cutouts; upgrade path is querying DisplayCutout bounding rects on Android 9+.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 110.dp, height = 48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                isCameraOpen = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
        )

        // ── Drop-Down Camera Window (Selfie Mirror) ───────────────────────────
        if (isCameraOpen) {
            // Outside-click scrim to vanish the camera window
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isCameraOpen = false })
                    }
            )
        }

        AnimatedVisibility(
            visible = isCameraOpen,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FrontCameraPreview(modifier = Modifier.fillMaxSize())

                    // 3-second animated countdown progress bar at bottom of camera view
                    val timerProgress = remember { Animatable(1f) }
                    LaunchedEffect(isCameraOpen) {
                        timerProgress.snapTo(1f)
                        timerProgress.animateTo(
                            0f,
                            animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { timerProgress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // ── Quick Screen Popup Menu (Settings & Add Category) ──────────────────
        if (showHomeMenu) {
            AlertDialog(
                onDismissRequest = { showHomeMenu = false },
                title = {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                showHomeMenu = false
                                isSettingsOpen = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                showHomeMenu = false
                                groupsViewModel.setShowCreateDialog(true)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Add Category",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showHomeMenu = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )

        // ── Drag Ghost Overlay (120 FPS hardware-accelerated follow-finger preview) ────
        if (dragDropState.isDragging && dragDropState.draggingApp != null) {
            val draggingApp = dragDropState.draggingApp!!
            val density = androidx.compose.ui.platform.LocalDensity.current
            val halfIconPx = with(density) { 27.dp.toPx() }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = dragDropState.dragPosition.x - halfIconPx
                            translationY = dragDropState.dragPosition.y - halfIconPx
                            scaleX = 1.15f
                            scaleY = 1.15f
                            shadowElevation = 16f
                        }
                        .size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    com.urunkarpm.drawer.core.designsystem.component.AppIconImage(
                        key = draggingApp.componentKey,
                        size = 54.dp,
                        label = draggingApp.label,
                        isWorkProfile = draggingApp.isWorkProfile,
                        iconShape = com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(
                            uiState.adaptiveIconShape
                        ),
                        iconLoader = { viewModel.getAppIcon(draggingApp) }
                    )
                }
            }
        }
    }
}
}
