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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.urunkarpm.drawer.core.designsystem.theme.LocalAppLabelScale
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.designsystem.theme.labelScaleFromName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    groupsViewModel: GroupsViewModel = hiltViewModel(),
    notificationsViewModel: com.urunkarpm.drawer.feature.notifications.NotificationsViewModel = hiltViewModel(),
    widgetHostManager: DrawerWidgetHostManager? = null,
    onDoubleTapLock: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val launchCounts by viewModel.appLaunchCounts.collectAsStateWithLifecycle()
    val launchTimes by viewModel.appLastLaunchedTimes.collectAsStateWithLifecycle()

    // ponytail: Smart Frecency suggestion algorithm combining exponential recency decay, launch frequency, and time-of-day affinity with smart cold-start fallback; ceiling is local heuristic ranking without deep neural network; upgrade path is on-device AppSearch / ML suggestion engine.
    val suggestedApps = remember(uiState.installedApps, launchCounts, launchTimes) {
        if (uiState.installedApps.isEmpty()) return@remember emptyList()

        val now = System.currentTimeMillis()
        val currentCalendar = java.util.Calendar.getInstance()
        val currentHour = currentCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val appCalendar = java.util.Calendar.getInstance()

        fun scoreApp(app: AppInfo): Float {
            // Count matching: exact componentKey, or sum of all components starting with packageName/
            val count = launchCounts[app.componentKey]
                ?: launchCounts.entries.filter { it.key.startsWith("${app.packageName}/") }.sumOf { it.value }
                    .takeIf { it > 0 }
                ?: launchCounts[app.packageName]
                ?: 0

            // Last launch time matching: exact componentKey, or max timestamp of package
            val lastLaunch = launchTimes[app.componentKey]
                ?: launchTimes.entries.filter { it.key.startsWith("${app.packageName}/") }.maxOfOrNull { it.value }
                ?: launchTimes[app.packageName]
                ?: 0L

            if (count > 0 || lastLaunch > 0L) {
                val frequencyScore = count * 15f

                val diffMs = (now - lastLaunch).coerceAtLeast(0L)
                val recencyBonus = when {
                    diffMs < 30 * 60 * 1000L -> 120f       // Opened within last 30 mins -> immediate top priority
                    diffMs < 2 * 60 * 60 * 1000L -> 80f     // Last 2 hours
                    diffMs < 12 * 60 * 60 * 1000L -> 50f    // Last 12 hours
                    diffMs < 24 * 60 * 60 * 1000L -> 30f    // Today
                    diffMs < 3 * 24 * 60 * 60 * 1000L -> 18f// Last 3 days
                    diffMs < 7 * 24 * 60 * 60 * 1000L -> 8f // Last 7 days
                    else -> 2f
                }

                val timeBonus = if (lastLaunch > 0L) {
                    appCalendar.timeInMillis = lastLaunch
                    val lastHour = appCalendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val hourDiff = kotlin.math.abs(currentHour - lastHour)
                    val circularDiff = kotlin.math.min(hourDiff, 24 - hourDiff)
                    if (circularDiff <= 2) 20f else if (circularDiff <= 4) 10f else 0f
                } else 0f

                return frequencyScore + recencyBonus + timeBonus
            }

            // Cold-start fallback: prioritize recently installed or updated apps
            val recencyTime = app.lastUpdateTimeMillis.coerceAtLeast(app.installTimeMillis)
            val baseline = (recencyTime / 1_000_000_000_000.0).toFloat().coerceIn(0f, 3f)
            return baseline
        }

        uiState.installedApps
            .distinctBy { it.packageName }
            .sortedByDescending { scoreApp(it) }
            .take(5)
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isSmallScreen = screenWidthDp < 360
    val isFoldableOrTablet = screenWidthDp >= 600
    val isLargeTablet = screenWidthDp >= 840
    val effectiveTwoDrawersSideBySide = uiState.twoDrawersSideBySide || isFoldableOrTablet
    val horizontalPadding = when {
        isSmallScreen -> 12.dp
        isLargeTablet -> 24.dp
        else -> 16.dp
    }
    val hasNotifications = notificationsUiState.activeNotifications.isNotEmpty()
    val activeNotificationPackages = remember(notificationsUiState.activeNotifications) {
        notificationsUiState.activeNotifications.map { it.packageName }.toSet()
    }
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
    var showManualWalkthrough by remember { mutableStateOf(false) }
    var isDuoStatusExpanded by remember { mutableStateOf(false) }
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
        if (!effectiveTwoDrawersSideBySide && groupsUiState.groups.any { it.group.isExpanded }) {
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

    // Back: close camera preview, settings, duo status breakdown, popup menu, category picker, action sheet, all-apps, notification drawer, or group accordion
    BackHandler(enabled = isDuoStatusExpanded || isCameraOpen || isSettingsOpen || showHomeMenu || uiState.isAllAppsOpen || uiState.selectedAppForMenu != null || appForCategorySelection != null || isNotificationExpanded || groupsUiState.groups.any { it.group.isExpanded }) {
        when {
            isDuoStatusExpanded -> isDuoStatusExpanded = false
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
    val widgetHostManager = widgetHostManager ?: remember { DrawerWidgetHostManager(context.applicationContext) }
    var pendingAppWidgetId by rememberSaveable { mutableStateOf<Int?>(null) }
    var targetDrawerIdForAdd by rememberSaveable { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 2 }
    )

    fun completeWidgetAdd(appWidgetId: Int) {
        val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info != null) {
            val pkg = info.provider.packageName
            val cls = info.provider.className
            val targetDrawerId = targetDrawerIdForAdd ?: viewModel.pendingAddTargetDrawerId
            val spanX = if (info.minWidth in 1..160) 2 else 4
            val spanY = ((info.minHeight + 69) / 70).coerceIn(2, 4)
            val heightDp = (spanY * 70).coerceAtLeast(140)

            // ponytail: push initial options to AppWidgetManager on add so providers receive dimensional constraints immediately; ceiling is static init options; upgrade path is reactive orientation-aware options.
            val density = context.resources.displayMetrics.density
            val widthDp = if (spanX <= 2) 160 else 340
            val options = android.os.Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            }
            try {
                widgetHostManager.appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
            } catch (_: Exception) {}

            viewModel.addWidget(
                appWidgetId = appWidgetId,
                packageName = pkg,
                providerClassName = cls,
                drawerId = targetDrawerId,
                spanX = spanX,
                spanY = spanY,
                heightDp = heightDp
            )
            coroutineScope.launch {
                if (uiState.enableWidgetsPage && pagerState.currentPage != 0) {
                    pagerState.animateScrollToPage(0)
                }
                val dest = if (targetDrawerId != null) "drawer" else "feed"
                snackbarHostState.showSnackbar("Widget added to $dest")
            }
        }
        targetDrawerIdForAdd = null
        viewModel.pendingAddTargetDrawerId = null
        pendingAppWidgetId = null
        viewModel.pendingAddAppWidgetId = null
    }

    fun checkAndConfigureWidget(appWidgetId: Int) {
        val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info?.configure != null) {
            val activity = context as? Activity
            if (activity != null) {
                widgetHostManager.onWidgetConfiguredListener = { resultCode, data ->
                    val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId) ?: appWidgetId
                    if (resultCode == Activity.RESULT_OK && id != -1) {
                        completeWidgetAdd(id)
                    } else {
                        widgetHostManager.deleteAppWidgetId(appWidgetId)
                        targetDrawerIdForAdd = null
                        viewModel.pendingAddTargetDrawerId = null
                        pendingAppWidgetId = null
                        viewModel.pendingAddAppWidgetId = null
                    }
                }
                // ponytail: startAppWidgetConfigureActivityForResult delegates to system AppWidgetService so private, unexported configure activities (e.g. Pinterest) start without SecurityException; ceiling: requires Activity context; upgrade path: custom config fallback.
                val started = widgetHostManager.startConfigure(activity, appWidgetId)
                if (!started) {
                    completeWidgetAdd(appWidgetId)
                }
            } else {
                completeWidgetAdd(appWidgetId)
            }
        } else {
            completeWidgetAdd(appWidgetId)
        }
    }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingAppWidgetId ?: viewModel.pendingAddAppWidgetId
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            checkAndConfigureWidget(id)
        } else if (id != null) {
            widgetHostManager.deleteAppWidgetId(id)
            pendingAppWidgetId = null
            viewModel.pendingAddAppWidgetId = null
            targetDrawerIdForAdd = null
            viewModel.pendingAddTargetDrawerId = null
        }
    }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val fallbackId = pendingAppWidgetId ?: viewModel.pendingAddAppWidgetId
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fallbackId ?: -1)
            ?: fallbackId
        if (result.resultCode == Activity.RESULT_OK && id != null && id != -1) {
            pendingAppWidgetId = id
            viewModel.pendingAddAppWidgetId = id
            val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(id)
            if (info != null) {
                // ponytail: widget picked from platform picker is already bound to this id; proceed straight to configure; ceiling: assumes single user profile; upgrade path: multi-profile cross-user inspection.
                checkAndConfigureWidget(id)
            } else {
                val provider = result.data?.component
                    ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, ComponentName::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER)
                    }
                if (provider != null) {
                    val isAllowed = widgetHostManager.appWidgetManager.bindAppWidgetIdIfAllowed(id, provider)
                    if (isAllowed) {
                        checkAndConfigureWidget(id)
                    } else {
                        val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                        }
                        try {
                            bindWidgetLauncher.launch(bindIntent)
                        } catch (e: Exception) {
                            widgetHostManager.deleteAppWidgetId(id)
                            pendingAppWidgetId = null
                            viewModel.pendingAddAppWidgetId = null
                            targetDrawerIdForAdd = null
                            viewModel.pendingAddTargetDrawerId = null
                        }
                    }
                } else {
                    widgetHostManager.deleteAppWidgetId(id)
                    pendingAppWidgetId = null
                    viewModel.pendingAddAppWidgetId = null
                    targetDrawerIdForAdd = null
                    viewModel.pendingAddTargetDrawerId = null
                }
            }
        } else if (fallbackId != null) {
            widgetHostManager.deleteAppWidgetId(fallbackId)
            pendingAppWidgetId = null
            viewModel.pendingAddAppWidgetId = null
            targetDrawerIdForAdd = null
            viewModel.pendingAddTargetDrawerId = null
        }
    }

    fun launchAddWidgetFlow(targetDrawerId: String? = null) {
        targetDrawerIdForAdd = targetDrawerId
        viewModel.pendingAddTargetDrawerId = targetDrawerId
        val allocatedId = widgetHostManager.allocateAppWidgetId()
        pendingAppWidgetId = allocatedId
        viewModel.pendingAddAppWidgetId = allocatedId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
            // ponytail: empty custom info/extras avoids NPE on custom OEM picker activities (Samsung/Xiaomi); ceiling: no custom launcher shortcuts mixed in system picker; upgrade path: custom in-app widget gallery.
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, arrayListOf<AppWidgetProviderInfo>())
            putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, arrayListOf<Bundle>())
        }
        try {
            pickWidgetLauncher.launch(pickIntent)
        } catch (e: Exception) {
            widgetHostManager.deleteAppWidgetId(allocatedId)
            pendingAppWidgetId = null
            viewModel.pendingAddAppWidgetId = null
            targetDrawerIdForAdd = null
            viewModel.pendingAddTargetDrawerId = null
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Widget picker not available on this device")
            }
        }
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

    // ponytail: nested scroll captures downward drag when home scroll is at 0 to expand notifications and upward drag to open all apps; ceiling: doesn't animate partial drawer pull; upgrade path: AnchoredDraggableState.
    val homeNestedScrollConnection = remember(hasNotifications, isNotificationExpanded, areOtherDrawersOpen, dragDropState.isDragging, uiState.openDrawerFromDockOnly) {
        object : NestedScrollConnection {
            private var scrollDownAccumulator = 0f
            private var scrollUpAccumulator = 0f
            private var actionTriggeredInScroll = false

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
                        if (!effectiveTwoDrawersSideBySide) {
                            groupsViewModel.collapseAllGroups()
                        }
                        scrollDownAccumulator = 0f
                        actionTriggeredInScroll = true
                        return Offset(0f, available.y)
                    }
                } else if (!areOtherDrawersOpen && !isNotificationExpanded && available.y < 0f) {
                    scrollDownAccumulator = 0f
                    // Apply resistance to upward drag so normal scrolling does not unintentionally trigger app drawer
                    scrollUpAccumulator += available.y * 0.55f
                    if (!uiState.openDrawerFromDockOnly && !uiState.isAllAppsOpen) {
                        // Sane resistance threshold: requires deliberate upward pull (~180f accumulated)
                        if (scrollUpAccumulator < -180f) {
                            viewModel.openAllApps()
                            scrollUpAccumulator = 0f
                            actionTriggeredInScroll = true
                            return Offset(0f, available.y)
                        }
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

    val headerFont = remember(uiState.headerFontFamily) { headerFontFamilyFromName(uiState.headerFontFamily) }
    val labelScale = remember(uiState.appLabelScale) { labelScaleFromName(uiState.appLabelScale) }

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        LocalHeaderFontFamily provides headerFont,
        LocalAppLabelScale provides labelScale
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                // Transparent so the wallpaper shows through cleanly with zero theme tint overlay
                // ponytail: native window.setBackgroundBlurRadius blurs wallpaper on Android 12+ without flat surface scrims covering wallpaper; ceiling: Android < 12 shows clean transparent wallpaper; upgrade path: RenderEffect-based RenderNode blur.
                .background(Color.Transparent)
        ) {
            if (uiState.dynamicWallpaperEnabled) {
                com.urunkarpm.drawer.core.designsystem.component.DynamicPantoneWallpaper(
                    speedSeconds = uiState.wallpaperGradientSpeedSeconds,
                    blurPercent = uiState.wallpaperGradientBlurPercent,
                    paletteId = uiState.wallpaperGradientPalette,
                    angleStyle = uiState.wallpaperGradientAngle,
                    modifier = Modifier.fillMaxSize()
                )
            }
            val homeContent: @Composable () -> Unit = {
                val homeScrollState = rememberScrollState()

                LaunchedEffect(isNotificationExpanded) {
                    if (isNotificationExpanded) {
                        homeScrollState.scrollTo(0)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isDuoStatusExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(90f)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        isDuoStatusExpanded = false
                                    }
                                }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding)
                            .onGloballyPositioned { coords ->
                                rootColumnBounds = coords.boundsInRoot()
                            }
                            // ponytail: double-tap on empty screen space locks phone via AccessibilityService; long-press opens context menu (Settings & Add Category & Add Widget); single-tap closes open drawers.
                            .pointerInput(uiState.isAllAppsOpen, isSettingsOpen, isCameraOpen, isNotificationExpanded, groupsUiState.groups, isDuoStatusExpanded) {
                                if (!uiState.isAllAppsOpen && !isSettingsOpen && !isCameraOpen) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val colBounds = rootColumnBounds ?: Rect.Zero
                                            val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                            val isOverDock = dragDropState.dockBounds?.contains(rootPos) == true
                                            val isOverGlance = glanceBounds?.contains(rootPos) == true
                                            val isOverGroup = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                            val isOverNotification = notificationBounds?.contains(rootPos) == true
                                            if (isDuoStatusExpanded) {
                                                isDuoStatusExpanded = false
                                            } else if (!isOverDock && !isOverGlance && !isOverGroup && !isOverNotification) {
                                                if (isNotificationExpanded) {
                                                    isNotificationExpanded = false
                                                }
                                                if (groupsUiState.groups.any { it.group.isExpanded }) {
                                                    groupsViewModel.collapseAllGroups()
                                                }
                                            }
                                        },
                                        onDoubleTap = { offset ->
                                            if (isDuoStatusExpanded) {
                                                isDuoStatusExpanded = false
                                                return@detectTapGestures
                                            }
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
                                            if (isDuoStatusExpanded) {
                                                isDuoStatusExpanded = false
                                                return@detectTapGestures
                                            }
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
                            }
                            // ponytail: outside swipe listener collapses notification drawer on upward drag outside or swipe on other category drawers; ceiling: fixed 24f drag threshold; upgrade path: velocity tracker fling dismiss.
                            .pointerInput(isNotificationExpanded) {
                                if (!isNotificationExpanded) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                    val colBounds = rootColumnBounds ?: Rect.Zero
                                    val rootPos = Offset(colBounds.left + down.position.x, colBounds.top + down.position.y)
                                    val isInsideNotification = notificationBounds?.contains(rootPos) == true
                                    if (isInsideNotification) {
                                        return@awaitEachGesture
                                    }
                                    val isOverOtherDrawers = dragDropState.groupBounds.values.any { it.contains(rootPos) }
                                    var totalDrag = Offset.Zero
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) break
                                        val delta = change.position - change.previousPosition
                                        totalDrag += delta
                                        if (totalDrag.y < -24f || (isOverOtherDrawers && (kotlin.math.abs(totalDrag.y) > 24f || kotlin.math.abs(totalDrag.x) > 24f))) {
                                            isNotificationExpanded = false
                                            break
                                        }
                                    }
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
                        duoStatusPosition = uiState.duoStatusPosition,
                        isDuoStatusExpanded = isDuoStatusExpanded,
                        onToggleDuoStatusExpand = { isDuoStatusExpanded = !isDuoStatusExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(100f)
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
                            .verticalScroll(homeScrollState, enabled = !dragDropState.isDragging && !isNotificationExpanded),
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
                                if (expanded && !effectiveTwoDrawersSideBySide) {
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
                                if (app != null) {
                                    viewModel.getAppIcon(app)
                                } else {
                                    try {
                                        context.packageManager.getApplicationIcon(packageName)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            },
                            isDragging = dragDropState.isDragging,
                            stealthTheme = uiState.stealthTheme,
                            stealthContrastLevel = uiState.stealthContrastLevel,
                            stealthLimitBrightness = uiState.stealthLimitBrightness
                        )

                        // 2. Rest of the Drawers (App Categories): Side by side (2 columns) if enabled, otherwise single column
                        GroupsAccordion(
                            viewModel = groupsViewModel,
                            twoDrawersSideBySide = effectiveTwoDrawersSideBySide,
                            iconShape = com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(
                                uiState.adaptiveIconShape
                            ),
                            hoveredGroupId = hoveredGroupId,
                            notificationPackages = activeNotificationPackages,
                            hideDrawerIcons = uiState.hideDrawerIcons,
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
                                    if (!uiState.isAllAppsOpen && dockDrag < -55f) {
                                        if (isNotificationExpanded) {
                                            isNotificationExpanded = false
                                        } else {
                                            viewModel.openAllApps()
                                        }
                                        actionTriggered = true
                                    } else if (!uiState.isAllAppsOpen && dockDrag > 50f) {
                                        if (hasNotifications && !isNotificationExpanded) {
                                            isNotificationExpanded = true
                                            if (!effectiveTwoDrawersSideBySide) {
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
    }

    // ── Left Widgets Page and Home Page (only active when enabled in Settings) ──
    // Resistance connection for WidgetsPage pull from Home (page 1 -> page 0)
    // ponytail: initial pull gate absorbs accidental micro-swipes (<72px) without dampening swipe velocity so an intentional gesture opens the widget drawer in a single swipe; ceiling is fixed ~24dp deadband; upgrade path is user-configurable resistance threshold in Settings.
    val widgetPageResistanceConnection = remember(pagerState) {
        object : NestedScrollConnection {
            private var pullAccumulator = 0f
            private val resistanceGatePx = 72f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && pagerState.currentPage == 1 && available.x > 0f) {
                    if (pullAccumulator < resistanceGatePx) {
                        val needed = resistanceGatePx - pullAccumulator
                        val consumed = available.x.coerceAtMost(needed)
                        pullAccumulator += consumed
                        return Offset(consumed, 0f)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.x < 0f && pagerState.currentPage == 1) {
                    pullAccumulator = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pullAccumulator = 0f
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.targetPage) {
        if (pagerState.currentPage == 1 && pagerState.targetPage == 1) {
            viewModel.collapseAllWidgetDrawers()
            if (isNotificationExpanded) isNotificationExpanded = false
            if (groupsUiState.groups.any { it.group.isExpanded }) {
                groupsViewModel.collapseAllGroups()
            }
        }
    }

    // Back button: If user is on Widgets feed (page 0), pressing Back returns to main Home (page 1)
    BackHandler(enabled = uiState.enableWidgetsPage && pagerState.currentPage == 0 && !isCameraOpen && !isSettingsOpen && !showHomeMenu && !uiState.isAllAppsOpen && uiState.selectedAppForMenu == null && appForCategorySelection == null && !isNotificationExpanded && !groupsUiState.groups.any { it.group.isExpanded }) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    if (uiState.enableWidgetsPage) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !dragDropState.isDragging &&
                !uiState.isAllAppsOpen &&
                !isSettingsOpen &&
                !isCameraOpen &&
                !isNotificationExpanded &&
                !groupsUiState.groups.any { it.group.isExpanded } &&
                !uiState.widgetDrawers.any { it.isExpanded },
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.35f
            ),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(widgetPageResistanceConnection)
        ) { page ->
            if (page == 0) {
                WidgetsPage(
                    drawers = uiState.widgetDrawers,
                    standaloneWidgets = uiState.standaloneWidgets,
                    widgetHostManager = widgetHostManager,
                    onAddWidgetClick = { targetDrawerId -> launchAddWidgetFlow(targetDrawerId) },
                    onCreateDrawer = { title, iconName, colorHex -> viewModel.createWidgetDrawer(title, iconName, colorHex) },
                    onUpdateDrawer = { drawer -> viewModel.updateWidgetDrawer(drawer) },
                    onToggleDrawerExpand = { drawerId -> viewModel.toggleWidgetDrawerExpand(drawerId) },
                    onCollapseAllDrawers = { viewModel.collapseAllWidgetDrawers() },
                    onDeleteDrawer = { drawerId, deleteChildren ->
                        if (deleteChildren) {
                            val drawer = uiState.widgetDrawers.find { it.id == drawerId }
                            drawer?.widgets?.forEach { w ->
                                widgetHostManager.deleteAppWidgetId(w.appWidgetId)
                            }
                        }
                        viewModel.deleteWidgetDrawer(drawerId, deleteChildren)
                    },
                    onDeleteWidget = { id, appWidgetId ->
                        widgetHostManager.deleteAppWidgetId(appWidgetId)
                        viewModel.deleteWidget(id, appWidgetId)
                    },
                    onMoveWidget = { index, up -> viewModel.moveWidget(index, up) },
                    onMoveDrawer = { index, up -> viewModel.moveWidgetDrawer(index, up) },
                    onUpdateWidgetSpan = { id, spanX, spanY, heightDp -> viewModel.updateWidgetSpan(id, spanX, spanY, heightDp) },
                    onMoveWidgetToDrawer = { widgetId, drawerId -> viewModel.moveWidgetToDrawer(widgetId, drawerId) },
                    onReorderWidgetInDrawer = { drawerId, fromIdx, toIdx -> viewModel.reorderWidgetsInDrawer(drawerId, fromIdx, toIdx) },
                    lockLayout = groupsUiState.lockLayout,
                    hideDrawerIcons = uiState.hideDrawerIcons,
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
            frequentApps = suggestedApps,
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
                onNavigateBack = { isSettingsOpen = false },
                onOpenWalkthrough = {
                    isSettingsOpen = false
                    showManualWalkthrough = true
                }
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
                    .widthIn(max = 520.dp)
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

        // ── Walkthrough Tour Overlay (First Install & Revisit) ───────────────
        val showWalkthrough = (!uiState.hasCompletedWalkthrough && !uiState.isLoading) || showManualWalkthrough
        if (showWalkthrough) {
            BackHandler {
                showManualWalkthrough = false
                viewModel.completeWalkthrough()
            }
        }
        AnimatedVisibility(
            visible = showWalkthrough,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            com.urunkarpm.drawer.feature.home.walkthrough.WalkthroughOverlay(
                onDismiss = {
                    showManualWalkthrough = false
                    viewModel.completeWalkthrough()
                },
                twoDrawersSideBySide = uiState.twoDrawersSideBySide,
                onSetTwoDrawersSideBySide = viewModel::setTwoDrawersSideBySide,
                is24Hour = uiState.is24Hour,
                onSetIs24Hour = viewModel::setIs24Hour,
                showWeather = uiState.showWeather,
                onSetShowWeather = viewModel::setShowWeather,
                adaptiveIconShape = uiState.adaptiveIconShape,
                onSetAdaptiveIconShape = viewModel::setAdaptiveIconShape,
                drawerThemeMode = uiState.drawerThemeMode,
                onSetDrawerThemeMode = viewModel::setDrawerThemeMode,
                surfaceThemeStyle = uiState.surfaceThemeStyle,
                onSetSurfaceThemeStyle = viewModel::setSurfaceThemeStyle,
                dockMesmerizingGlow = uiState.dockMesmerizingGlow,
                onSetDockMesmerizingGlow = viewModel::setDockMesmerizingGlow,
                headerFontFamily = uiState.headerFontFamily,
                onSetHeaderFontFamily = viewModel::setHeaderFontFamily,
                stealthTheme = uiState.stealthTheme,
                onSetStealthTheme = viewModel::setStealthTheme,
                isDynamicWallpaper = uiState.dynamicWallpaperEnabled,
                onSelectDynamicWallpaper = viewModel::setDynamicWallpaperEnabled
            )
        }
    }
}
}
