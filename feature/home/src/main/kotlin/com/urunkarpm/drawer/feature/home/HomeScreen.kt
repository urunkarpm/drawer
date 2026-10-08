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
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.feature.home.component.WidgetsPage
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
import com.urunkarpm.drawer.feature.home.component.HomeContextMenuSheet
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
    val homeMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    var isQuickSettingsOpen by remember { mutableStateOf(false) }
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
                } else if (result.sourceGroupId != null && result.sourceGroupId == result.targetGroupId) {
                    // Reorder within the same category grid freely without hesitation
                    groupsViewModel.moveAppBetweenGroups(
                        sourceGroupId = result.sourceGroupId,
                        targetGroupId = result.targetGroupId,
                        app = result.app,
                        targetIndex = result.targetIndex
                    )
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
            // Collapse groups unless reordering within the same group
            if (result !is DragDropResult.DroppedOnGroup || result.sourceGroupId != result.targetGroupId) {
                groupsViewModel.collapseAllGroups()
            }
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

    // Auto-collapse quick settings when other major sheets or drawers open
    LaunchedEffect(uiState.isAllAppsOpen, isSettingsOpen, isCameraOpen) {
        if (uiState.isAllAppsOpen || isSettingsOpen || isCameraOpen) {
            isQuickSettingsOpen = false
        }
    }

    // Back: close quick settings, camera preview, settings, popup menu, category picker, action sheet, all-apps, notification drawer, or group accordion
    BackHandler(enabled = isQuickSettingsOpen || isCameraOpen || isSettingsOpen || showHomeMenu || uiState.isAllAppsOpen || uiState.selectedAppForMenu != null || appForCategorySelection != null || isNotificationExpanded || groupsUiState.groups.any { it.group.isExpanded }) {
        when {
            isQuickSettingsOpen -> isQuickSettingsOpen = false
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
    val widgetHostManager = remember { DrawerWidgetHostManager(context.applicationContext) }
    var pendingAppWidgetId by remember { mutableStateOf<Int?>(null) }

    fun completeWidgetAdd(appWidgetId: Int) {
        val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info != null) {
            val pkg = info.provider.packageName
            val cls = info.provider.className
            viewModel.addWidget(appWidgetId, pkg, cls)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Widget added to feed")
            }
        }
    }

    val configureWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingAppWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            completeWidgetAdd(id)
        } else if (id != null) {
            widgetHostManager.deleteAppWidgetId(id)
        }
        pendingAppWidgetId = null
    }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingAppWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(id)
            if (info?.configure != null) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                configureWidgetLauncher.launch(intent)
            } else {
                completeWidgetAdd(id)
                pendingAppWidgetId = null
            }
        } else if (id != null) {
            widgetHostManager.deleteAppWidgetId(id)
            pendingAppWidgetId = null
        }
    }

    fun launchAddWidgetFlow() {
        val allocatedId = widgetHostManager.allocateAppWidgetId()
        pendingAppWidgetId = allocatedId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
        }
        pickWidgetLauncher.launch(pickIntent)
    }

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
    //  • drag UP (negative) → open All Apps (or collapse expanded notification drawer / category drawer)
    //  • drag DOWN (positive) → expand notification drawer when notifications exist
    var scrollDownAccumulator by remember { mutableFloatStateOf(0f) }
    var scrollUpAccumulator by remember { mutableFloatStateOf(0f) }
    var actionTriggeredInScroll by remember { mutableStateOf(false) }

    // ponytail: nested scroll captures downward drag when home scroll is at 0 to expand notifications and upward drag to open all apps; ceiling: doesn't animate partial drawer pull; upgrade path: AnchoredDraggableState.
    val homeNestedScrollConnection = remember(hasNotifications, isNotificationExpanded, areOtherDrawersOpen, dragDropState.isDragging) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (actionTriggeredInScroll) {
                    if (available.y > 0f && scrollUpAccumulator < 0f) {
                        actionTriggeredInScroll = false
                    } else if (available.y < 0f && scrollDownAccumulator > 0f) {
                        actionTriggeredInScroll = false
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (dragDropState.isDragging) return Offset.Zero
                if (actionTriggeredInScroll) return Offset.Zero

                // ponytail: nested scroll gestures only active on empty home screen when no category drawers are open; ceiling: fixed 60f scroll accumulator; upgrade path: velocity-aware gesture router.
                if (!areOtherDrawersOpen && hasNotifications && !isNotificationExpanded && available.y > 0f) {
                    scrollDownAccumulator += available.y
                    if (scrollDownAccumulator > 60f) {
                        isNotificationExpanded = true
                        if (!uiState.twoDrawersSideBySide) {
                            groupsViewModel.collapseAllGroups()
                        }
                        scrollDownAccumulator = 0f
                        actionTriggeredInScroll = true
                        return Offset(0f, available.y)
                    }
                } else if (!areOtherDrawersOpen && available.y < 0f) {
                    scrollDownAccumulator = 0f
                    scrollUpAccumulator += available.y
                    if (scrollUpAccumulator < -60f) {
                        if (isNotificationExpanded) {
                            isNotificationExpanded = false
                        } else if (!uiState.isAllAppsOpen) {
                            viewModel.openAllApps()
                        }
                        scrollUpAccumulator = 0f
                        actionTriggeredInScroll = true
                        return Offset(0f, available.y)
                    }
                } else {
                    scrollDownAccumulator = 0f
                    scrollUpAccumulator = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                scrollDownAccumulator = 0f
                scrollUpAccumulator = 0f
                actionTriggeredInScroll = false
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                scrollDownAccumulator = 0f
                scrollUpAccumulator = 0f
                actionTriggeredInScroll = false
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

            val homeContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .onGloballyPositioned { coords ->
                            rootColumnBounds = coords.boundsInRoot()
                        }
                        // ponytail: double-tap on empty screen space locks phone via AccessibilityService; long-press opens context menu (Settings & Add Category & Add Widget); single-tap closes open drawers.
                        .pointerInput(isQuickSettingsOpen, uiState.isAllAppsOpen, isSettingsOpen, isCameraOpen, isNotificationExpanded, groupsUiState.groups) {
                            if (!isQuickSettingsOpen && !uiState.isAllAppsOpen && !isSettingsOpen && !isCameraOpen) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val colBounds = rootColumnBounds ?: Rect.Zero
                                        val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                        val isOverDock = dragDropState.dockBounds?.contains(rootPos) == true
                                        val isOverGlance = glanceBounds?.contains(rootPos) == true
                                        val isOverGroup = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                        val isOverNotification = notificationBounds?.contains(rootPos) == true
                                        if (!isOverDock && !isOverGlance && !isOverGroup && !isOverNotification) {
                                            if (isNotificationExpanded) {
                                                isNotificationExpanded = false
                                            }
                                            if (groupsUiState.groups.any { it.group.isExpanded }) {
                                                groupsViewModel.collapseAllGroups()
                                            }
                                        }
                                    },
                                    onDoubleTap = { offset ->
                                        val colBounds = rootColumnBounds ?: Rect.Zero
                                        val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                        val isOverDock = dragDropState.dockBounds?.contains(rootPos) == true
                                        val isOverGlance = glanceBounds?.contains(rootPos) == true
                                        val isOverGroup = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                        val isOverNotification = notificationBounds?.contains(rootPos) == true
                                        if (!isOverDock && !isOverGlance && !isOverGroup && !isOverNotification) {
                                            onDoubleTapLock()
                                        }
                                    },
                                    onLongPress = { offset ->
                                        val colBounds = rootColumnBounds ?: Rect.Zero
                                        val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                        val isOverDock = dragDropState.dockBounds?.contains(rootPos) == true
                                        val isOverGlance = glanceBounds?.contains(rootPos) == true
                                        val isOverGroup = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                        val isOverNotification = notificationBounds?.contains(rootPos) == true
                                        if (!isOverDock && !isOverGlance && !isOverGroup && !isOverNotification) {
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
                    showDuoStatus = uiState.showDuoStatusWidget && uiState.hideStatusBar,
                    onDuoStatusClick = { isQuickSettingsOpen = true },
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
                        .verticalScroll(rememberScrollState(), enabled = !dragDropState.isDragging),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // 1. Notification Drawer: Stays fixed at top as stable accordion item; no layout shifts when drawers open
                    // ponytail: keep notification header persistent to eliminate jarring layout shifts on category drawer expansion; ceiling: full drawer card always present; upgrade path: customizable collapsed view.
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
                        var actionTriggered = false
                        detectVerticalDragGestures(
                            onDragStart = { 
                                dockDrag = 0f 
                                actionTriggered = false
                            },
                            onVerticalDrag = { _, dragAmount ->
                                if (actionTriggered) return@detectVerticalDragGestures
                                dockDrag += dragAmount
                                if (!uiState.isAllAppsOpen && dockDrag < -20f) {
                                    if (isNotificationExpanded) {
                                        isNotificationExpanded = false
                                    } else {
                                        viewModel.openAllApps()
                                    }
                                    actionTriggered = true
                                } else if (!uiState.isAllAppsOpen && dockDrag > 20f) {
                                    if (hasNotifications && !isNotificationExpanded) {
                                        isNotificationExpanded = true
                                        if (!uiState.twoDrawersSideBySide) {
                                            groupsViewModel.collapseAllGroups()
                                        }
                                    }
                                    actionTriggered = true
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
    }

    // ── Left Widgets Page and Home Page (only active when enabled in Settings) ──
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 2 }
    )

    // Back button: If user is on Widgets feed (page 0), pressing Back returns to main Home (page 1)
    BackHandler(enabled = uiState.enableWidgetsPage && pagerState.currentPage == 0 && !isCameraOpen && !isSettingsOpen && !showHomeMenu && !uiState.isAllAppsOpen && uiState.selectedAppForMenu == null && appForCategorySelection == null && !isNotificationExpanded && !groupsUiState.groups.any { it.group.isExpanded }) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    if (uiState.enableWidgetsPage) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !dragDropState.isDragging && !uiState.isAllAppsOpen && !isSettingsOpen && !isCameraOpen,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                WidgetsPage(
                    widgets = uiState.widgets,
                    widgetHostManager = widgetHostManager,
                    onAddWidgetClick = { launchAddWidgetFlow() },
                    onDeleteWidget = { id, appWidgetId -> viewModel.deleteWidget(id, appWidgetId) },
                    onMoveWidget = { index, up -> viewModel.moveWidget(index, up) },
                    onResizeWidget = { id, newHeight -> viewModel.updateWidgetHeight(id, newHeight) },
                    lockLayout = groupsUiState.lockLayout,
                    onBackToHome = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                        .navigationBarsPadding()
                )
            } else {
                homeContent()
            }
        }
    } else {
        homeContent()
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
            iconLoader = { app -> viewModel.getAppIcon(app) },
            drawerThemeMode = uiState.drawerThemeMode,
            surfaceCornerRadiusDp = uiState.surfaceCornerRadius,
            autoOpenKeyboard = uiState.autoOpenKeyboardInDrawer,
            onOpenSettings = { isSettingsOpen = true }
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
        if (uiState.enableCameraMirror) {
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
        }

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

        // ── Home Screen Context Menu Sheet (Settings, Add Category, Wallpaper, System Settings) ──
        if (showHomeMenu) {
            HomeContextMenuSheet(
                onDismissRequest = { showHomeMenu = false },
                sheetState = homeMenuSheetState,
                onOpenSettings = {
                    showHomeMenu = false
                    isSettingsOpen = true
                },
                onAddCategory = {
                    showHomeMenu = false
                    groupsViewModel.setShowCreateDialog(true)
                },
                onAddWidget = {
                    showHomeMenu = false
                    launchAddWidgetFlow()
                }
            )
        }

        // ── Quick Settings Dark Scrim (covers remaining screen with dark shadow) ──
        AnimatedVisibility(
            visible = isQuickSettingsOpen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isQuickSettingsOpen = false })
                    }
            )
        }

        // ── Quick Settings Panel (Rich animated presentation: 35% bottom in portrait, right 50% in landscape) ──
        val screenConfig = LocalConfiguration.current
        val isLandscape = screenConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

        AnimatedVisibility(
            visible = isQuickSettingsOpen,
            enter = if (isLandscape) {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(220))
            } else {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(220))
            },
            exit = if (isLandscape) {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(180))
            } else {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(180))
            },
            modifier = if (isLandscape) Modifier.align(Alignment.CenterEnd) else Modifier.align(Alignment.BottomCenter)
        ) {
            com.urunkarpm.drawer.feature.home.component.QuickSettingsPanel(
                tileOrder = uiState.quickSettingsTileOrder,
                hiddenTiles = uiState.quickSettingsHiddenTiles,
                onUpdateTileOrder = viewModel::updateQuickSettingsTileOrder,
                onUpdateHiddenTiles = viewModel::updateQuickSettingsHiddenTiles,
                onClose = { isQuickSettingsOpen = false },
                drawerThemeMode = uiState.drawerThemeMode,
                surfaceCornerRadiusDp = uiState.surfaceCornerRadius,
                modifier = if (isLandscape) {
                    Modifier
                        .fillMaxWidth(0.50f)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.35f)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp, start = 24.dp, end = 24.dp),
            snackbar = { snackbarData ->
                androidx.compose.material3.Snackbar(
                    snackbarData = snackbarData,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
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
