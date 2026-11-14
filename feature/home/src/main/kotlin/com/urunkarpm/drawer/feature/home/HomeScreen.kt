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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.urunkarpm.drawer.core.designsystem.animation.FluidSpringPhysics
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.util.VelocityTracker
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.snapshotFlow
import kotlin.math.roundToInt
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale
import android.provider.Settings
import android.net.Uri
import kotlin.math.pow
import kotlin.math.roundToInt
import android.os.Bundle
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.feature.home.component.WidgetsPage
import com.urunkarpm.drawer.feature.home.component.WidgetPickerBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.urunkarpm.drawer.feature.home.component.MindfulPauseDialog
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    groupsViewModel: GroupsViewModel = hiltViewModel(),
    notificationsViewModel: com.urunkarpm.drawer.feature.notifications.NotificationsViewModel = hiltViewModel(),
    widgetHostManager: DrawerWidgetHostManager? = null,
    onDoubleTapLock: () -> Unit = {},
    homeResetTrigger: Long = 0L
) {
    val crispHaptics = LocalCrispHaptics.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val suggestedApps by viewModel.suggestedApps.collectAsStateWithLifecycle()
    val showDrawerSuggestions by viewModel.showDrawerSuggestions.collectAsStateWithLifecycle()
    val autoArrangeApps by viewModel.autoArrangeApps.collectAsStateWithLifecycle()
    val dockShowLabels by viewModel.dockShowLabels.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()
    val dockIconSize by viewModel.dockIconSize.collectAsStateWithLifecycle()
    val multiGroupApps by viewModel.multiGroupApps.collectAsStateWithLifecycle()
    val lockLayout by viewModel.lockLayout.collectAsStateWithLifecycle()
    val autoCategorizeNewApps by viewModel.autoCategorizeNewApps.collectAsStateWithLifecycle()
    val staleTodoCount by viewModel.staleTodoCount.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isSmallScreen = screenWidthDp < 360
    val activeNotificationPkgs = remember(notificationsUiState.activeNotifications) {
        notificationsUiState.activeNotifications.map { it.packageName }.toSet()
    }
    val isFoldableOrTablet = screenWidthDp >= 600
    val isLargeTablet = screenWidthDp >= 840
    val effectiveTwoDrawersSideBySide = uiState.twoDrawersSideBySide || isFoldableOrTablet
    val horizontalPadding = when {
        isSmallScreen -> 12.dp
        isLargeTablet -> 24.dp
        else -> 16.dp
    }
    val hasNotifications = notificationsUiState.activeNotifications.isNotEmpty()
    val activeNotificationPackages = activeNotificationPkgs
    val currentActiveNotificationPkgs by rememberUpdatedState(activeNotificationPkgs)
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
            }
            is DragDropResult.DroppedOnGroup -> {
                if (result.sourceGroupId != null && result.sourceGroupId != result.targetGroupId) {
                    groupsViewModel.moveAppBetweenGroups(
                        sourceGroupId = result.sourceGroupId,
                        targetGroupId = result.targetGroupId,
                        app = result.app,
                        targetIndex = result.targetIndex
                    )
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
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        val installedVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        }.getOrDefault("0.0.0")
        viewModel.checkForUpdateOnStartup(installedVersion)
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
            viewModel.clearUserMessage()
        }
    }

    // Back: close camera preview, settings, duo status breakdown, popup menu, category picker, action sheet, all-apps, notification drawer, vault, or group accordion
    BackHandler(enabled = uiState.isVaultOpen || isDuoStatusExpanded || isCameraOpen || isSettingsOpen || showHomeMenu || uiState.isAllAppsOpen || uiState.selectedAppForMenu != null || appForCategorySelection != null || isNotificationExpanded || groupsUiState.groups.any { it.group.isExpanded }) {
        when {
            uiState.isVaultOpen -> viewModel.closeVault()
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

    val widgetHostManager = widgetHostManager ?: remember { DrawerWidgetHostManager(context.applicationContext) }
    var pendingAppWidgetId by rememberSaveable { mutableStateOf<Int?>(null) }
    var targetDrawerIdForAdd by rememberSaveable { mutableStateOf<String?>(null) }
    var showWidgetPickerSheet by rememberSaveable { mutableStateOf(false) }

    val homePageIndex = if (uiState.enableWidgetsPage) 1 else 0
    val todoPageIndex = if (uiState.enableWidgetsPage) 2 else 1
    val totalPages = if (uiState.enableWidgetsPage) 3 else 2

    val pagerState = rememberPagerState(
        initialPage = if (uiState.enableWidgetsPage) 1 else 0,
        pageCount = { totalPages }
    )

    val homeScale = remember { Animatable(1f) }
    val homeAlpha = remember { Animatable(1f) }

    LaunchedEffect(uiState.isAllAppsOpen, isSettingsOpen) {
        val isOverlayOpen = uiState.isAllAppsOpen || isSettingsOpen
        if (isOverlayOpen) {
            launch {
                homeScale.animateTo(
                    targetValue = 0.94f,
                    animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 500f)
                )
            }
            launch {
                homeAlpha.animateTo(
                    targetValue = 0.75f,
                    animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 500f)
                )
            }
        } else {
            launch {
                homeScale.animateTo(
                    targetValue = 1f,
                    animationSpec = FluidSpringPhysics.fluidSpring()
                )
            }
            launch {
                homeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = FluidSpringPhysics.fluidSpring()
                )
            }
        }
    }

    // ponytail: Android Home gesture/button returns to clean home page by collapsing all open sheets/drawers and returning to main page with an elastic rebound; ceiling is uniform spring physics; upgrade path is gesture-velocity-driven handoff.
    LaunchedEffect(homeResetTrigger) {
        if (homeResetTrigger > 0L) {
            homeScale.snapTo(0.93f)
            launch {
                homeScale.animateTo(
                    targetValue = 1f,
                    animationSpec = FluidSpringPhysics.fluidSpring()
                )
            }
            isSettingsOpen = false
            viewModel.closeAllApps()
            isCameraOpen = false
            isDuoStatusExpanded = false
            showHomeMenu = false
            appForCategorySelection = null
            viewModel.dismissAppMenu()
            isNotificationExpanded = false
            showManualWalkthrough = false
            groupsViewModel.collapseAllGroups()
            if (uiState.enableWidgetsPage && pagerState.currentPage != 1) {
                pagerState.animateScrollToPage(1)
            }
        }
    }

    fun completeWidgetAdd(appWidgetId: Int) {
        val info = widgetHostManager.appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info != null) {
            val pkg = info.provider.packageName
            val cls = info.provider.className
            val targetDrawerId = targetDrawerIdForAdd ?: viewModel.pendingAddTargetDrawerId
            val spanX = if (info.minWidth in 1..160) 2 else 4
            val spanY = ((info.minHeight + 69) / 70).coerceIn(2, 4)
            val heightDp = (spanY * 70).coerceAtLeast(140)
            val widthDp = if (spanX <= 2) 160 else 340
            val options = widgetHostManager.createWidgetOptionsBundle(widthDp, heightDp)

            try {
                widgetHostManager.appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
            } catch (_: Exception) {}

            widgetHostManager.triggerWidgetUpdate(appWidgetId)

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
                    val spanX = 4
                    val spanY = 2
                    val heightDp = 140
                    val widthDp = 340
                    val options = widgetHostManager.createWidgetOptionsBundle(widthDp, heightDp)
                    val isAllowed = widgetHostManager.appWidgetManager.bindAppWidgetIdIfAllowed(id, provider, options)
                    if (isAllowed) {
                        checkAndConfigureWidget(id)
                    } else {
                        val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
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

    fun handleSelectedWidget(info: AppWidgetProviderInfo) {
        val allocatedId = widgetHostManager.allocateAppWidgetId()
        pendingAppWidgetId = allocatedId
        viewModel.pendingAddAppWidgetId = allocatedId

        val spanX = if (info.minWidth in 1..160) 2 else 4
        val spanY = ((info.minHeight + 69) / 70).coerceIn(2, 4)
        val heightDp = (spanY * 70).coerceAtLeast(140)
        val widthDp = if (spanX <= 2) 160 else 340
        val options = widgetHostManager.createWidgetOptionsBundle(widthDp, heightDp)

        val isAllowed = try {
            widgetHostManager.appWidgetManager.bindAppWidgetIdIfAllowed(
                allocatedId,
                info.provider,
                options
            )
        } catch (_: Exception) {
            false
        }

        if (isAllowed) {
            checkAndConfigureWidget(allocatedId)
        } else {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options)
            }
            try {
                bindWidgetLauncher.launch(bindIntent)
            } catch (_: Exception) {
                widgetHostManager.deleteAppWidgetId(allocatedId)
                pendingAppWidgetId = null
                viewModel.pendingAddAppWidgetId = null
                targetDrawerIdForAdd = null
                viewModel.pendingAddTargetDrawerId = null
            }
        }
    }

    fun launchSystemWidgetPicker() {
        val allocatedId = widgetHostManager.allocateAppWidgetId()
        pendingAppWidgetId = allocatedId
        viewModel.pendingAddAppWidgetId = allocatedId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
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
                snackbarHostState.showSnackbar("System widget picker not available on this device")
            }
        }
    }

    fun launchAddWidgetFlow(targetDrawerId: String? = null) {
        targetDrawerIdForAdd = targetDrawerId
        viewModel.pendingAddTargetDrawerId = targetDrawerId
        showWidgetPickerSheet = true
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

    // ponytail: Dynamic 50% wallpaper blur when swiping to/from the To-Do page, active only when global wallpaper blur is OFF
    LaunchedEffect(uiState.wallpaperBlur, todoPageIndex) {
        if (!uiState.wallpaperBlur && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val activity = context as? android.app.Activity
            val window = activity?.window ?: return@LaunchedEffect
            var lastRadius = -1
            snapshotFlow {
                val rawOffset = (pagerState.currentPage - todoPageIndex) + pagerState.currentPageOffsetFraction
                val p = (1f - kotlin.math.abs(rawOffset)).coerceIn(0f, 1f)
                // Smoothstep easing: 3p^2 - 2p^3 provides seamless, jank-free 120fps acceleration & deceleration
                p * p * (3f - 2f * p)
            }.collect { progress ->
                val targetBlur = (50f * progress).roundToInt()
                if (targetBlur != lastRadius) {
                    lastRadius = targetBlur
                    try {
                        if (targetBlur > 0) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        }
                        window.setBackgroundBlurRadius(targetBlur)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    DisposableEffect(uiState.wallpaperBlur) {
        onDispose {
            if (!uiState.wallpaperBlur && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                try {
                    val activity = context as? android.app.Activity
                    activity?.window?.setBackgroundBlurRadius(0)
                } catch (_: Exception) {}
            }
        }
    }

    // Discreet notification cue: listen for device unlock events to decrement counter
    DisposableEffect(context) {
        val receiver = UnlockReceiver {
            viewModel.onUserUnlocked()
        }
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_STOP) {
        viewModel.closeVault()
    }

    val areOtherDrawersOpen = uiState.isAllAppsOpen || groupsUiState.groups.any { it.group.isExpanded }

    var lastDrawerCollapseTime by remember { mutableLongStateOf(0L) }

    // Auto-collapse expanded notification state when any drawer opens
    LaunchedEffect(areOtherDrawersOpen) {
        if (areOtherDrawersOpen && isNotificationExpanded) {
            isNotificationExpanded = false
            lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
        }
    }

    LaunchedEffect(isNotificationExpanded) {
        if (!isNotificationExpanded) {
            lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
        }
    }

    LaunchedEffect(groupsUiState.groups.any { it.group.isExpanded }) {
        if (!groupsUiState.groups.any { it.group.isExpanded }) {
            lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
        }
    }

    val currentAnyCategoryExpanded by rememberUpdatedState(groupsUiState.groups.any { it.group.isExpanded })
    val currentHasNotifications by rememberUpdatedState(hasNotifications)
    val currentIsNotificationExpanded by rememberUpdatedState(isNotificationExpanded)
    val currentIsOpenDrawerFromDockOnly by rememberUpdatedState(uiState.openDrawerFromDockOnly)
    val currentIsAllAppsOpen by rememberUpdatedState(uiState.isAllAppsOpen)
    val currentIsDragging by rememberUpdatedState(dragDropState.isDragging)
    val currentEffectiveTwoDrawersSideBySide by rememberUpdatedState(effectiveTwoDrawersSideBySide)

    // ponytail: nested scroll captures downward drag when home scroll is at 0 to expand notifications and upward drag to open all apps; ignores upward drag/fling while category drawer or notification drawer is expanded so scrolling down drawer lists does not activate main app drawer; ceiling: doesn't animate partial drawer pull; upgrade path: AnchoredDraggableState.
    val homeNestedScrollConnection = remember {
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
                if (currentIsDragging) return Offset.Zero
                if (actionTriggeredInScroll) return Offset.Zero

                val timeSinceCollapse = android.os.SystemClock.uptimeMillis() - lastDrawerCollapseTime
                val isRecentCollapse = timeSinceCollapse < 500L

                // ponytail: downward pull expands notifications when at top and no category accordions are open
                if (!currentAnyCategoryExpanded && currentHasNotifications && !currentIsNotificationExpanded && !isRecentCollapse && available.y > 0f) {
                    scrollDownAccumulator += available.y
                    if (scrollDownAccumulator > 60f) {
                        isNotificationExpanded = true
                        if (!currentEffectiveTwoDrawersSideBySide) {
                            groupsViewModel.collapseAllGroups()
                        }
                        scrollDownAccumulator = 0f
                        actionTriggeredInScroll = true
                        return Offset(0f, available.y)
                    }
                } else if (!currentAnyCategoryExpanded && !currentIsAllAppsOpen && !currentIsNotificationExpanded && !isRecentCollapse && available.y < 0f) {
                    scrollDownAccumulator = 0f
                    // Apply resistance to upward drag so normal scrolling does not unintentionally trigger app drawer
                    scrollUpAccumulator += available.y * 0.55f
                    if (!currentIsOpenDrawerFromDockOnly) {
                        // Sane resistance threshold: requires deliberate upward pull (~180f accumulated)
                        if (scrollUpAccumulator < -180f) {
                            viewModel.openAllApps(currentActiveNotificationPkgs)
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
                val timeSinceCollapse = android.os.SystemClock.uptimeMillis() - lastDrawerCollapseTime
                val isRecentCollapse = timeSinceCollapse < 500L

                if (!currentAnyCategoryExpanded && !currentIsNotificationExpanded && !isRecentCollapse && !actionTriggeredInScroll && !currentIsOpenDrawerFromDockOnly && !currentIsAllAppsOpen) {
                    // Velocity handoff: quick upward flick directly triggers drawer opening
                    if (available.y < -750f) {
                        viewModel.openAllApps(currentActiveNotificationPkgs)
                        actionTriggeredInScroll = true
                        scrollUpAccumulator = 0f
                        return available
                    }
                }
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
                val dynamicBlur = if (!uiState.wallpaperBlur) {
                    val rawOffset = (pagerState.currentPage - todoPageIndex) + pagerState.currentPageOffsetFraction
                    val p = (1f - kotlin.math.abs(rawOffset)).coerceIn(0f, 1f)
                    val smooth = p * p * (3f - 2f * p)
                    (uiState.wallpaperGradientBlurPercent + (50f * smooth).roundToInt()).coerceIn(0, 100)
                } else {
                    uiState.wallpaperGradientBlurPercent
                }
                com.urunkarpm.drawer.core.designsystem.component.DynamicPantoneWallpaper(
                    speedSeconds = uiState.wallpaperGradientSpeedSeconds,
                    blurPercent = dynamicBlur,
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

                LaunchedEffect(groupsUiState.groups.any { it.group.isExpanded }) {
                    if (!groupsUiState.groups.any { it.group.isExpanded } && homeScrollState.value > 0) {
                        homeScrollState.animateScrollTo(0, animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
                    }
                }

                var homeRootBoxCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                val currentGroups by rememberUpdatedState(groupsUiState.groups)
                val currentIsNotificationExpanded by rememberUpdatedState(isNotificationExpanded)
                val currentIsDuoStatusExpanded by rememberUpdatedState(isDuoStatusExpanded)
                val currentIsAllAppsOpen by rememberUpdatedState(uiState.isAllAppsOpen)
                val currentIsSettingsOpen by rememberUpdatedState(isSettingsOpen)
                val currentIsCameraOpen by rememberUpdatedState(isCameraOpen)
                val currentAnyCategoryExpanded = currentGroups.any { it.group.isExpanded }
                val currentAnyDrawerExpanded = currentAnyCategoryExpanded || currentIsNotificationExpanded

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { homeRootBoxCoordinates = it }
                        // ponytail: 0ms latency pointer-down collapse for expanded category & notification drawers on outside touch; ceiling: coordinate hit-test against bounds; upgrade path: modal barrier scrim.
                        .pointerInput(currentAnyCategoryExpanded, currentIsNotificationExpanded) {
                            if (!currentAnyCategoryExpanded && !currentIsNotificationExpanded) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                val rootPos = homeRootBoxCoordinates?.takeIf { it.isAttached }?.localToRoot(down.position) ?: down.position
                                val expandedGroupIds = currentGroups.filter { it.group.isExpanded }.map { it.group.id }.toSet()
                                val isInsideAnyExpandedGroup = expandedGroupIds.any { id ->
                                    dragDropState.groupBounds[id]?.contains(rootPos) == true
                                }
                                val isInsideNotification = notificationBounds?.contains(rootPos) == true

                                val isOutsideNotification = currentIsNotificationExpanded && !isInsideNotification
                                val isOutsideGroup = currentAnyCategoryExpanded && !isInsideAnyExpandedGroup

                                if (isOutsideNotification || isOutsideGroup) {
                                    if (isOutsideNotification) {
                                        isNotificationExpanded = false
                                        lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                    }
                                    if (isOutsideGroup) {
                                        groupsViewModel.collapseAllGroups()
                                        lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                    }
                                    val isOverOtherGroup = dragDropState.groupBounds.filterKeys { !expandedGroupIds.contains(it) }.values.any { it.contains(rootPos) }
                                    if (!isOverOtherGroup) {
                                        down.consume()
                                    }
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = homeScale.value
                            scaleY = homeScale.value
                            alpha = homeAlpha.value
                        }
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
                            // pointerInput(Unit) with rememberUpdatedState prevents gesture detector coroutine restarts and dropped touches on list/state changes.
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        if (currentIsAllAppsOpen || currentIsSettingsOpen || currentIsCameraOpen) return@detectTapGestures
                                        val colBounds = rootColumnBounds ?: Rect.Zero
                                        val rootPos = Offset(colBounds.left + offset.x, colBounds.top + offset.y)
                                        val expandedGroup = currentGroups.firstOrNull { it.group.isExpanded }
                                        val isInsideExpandedGroup = expandedGroup?.let { dragDropState.groupBounds[it.group.id]?.contains(rootPos) } == true
                                        val isInsideNotification = notificationBounds?.contains(rootPos) == true

                                        if (currentIsDuoStatusExpanded) {
                                            isDuoStatusExpanded = false
                                        } else {
                                            if (currentIsNotificationExpanded && !isInsideNotification) {
                                                isNotificationExpanded = false
                                                lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                            }
                                            if (expandedGroup != null && !isInsideExpandedGroup) {
                                                groupsViewModel.collapseAllGroups()
                                                lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                            }
                                        }
                                    },
                                    onDoubleTap = { offset ->
                                        if (currentIsAllAppsOpen || currentIsSettingsOpen || currentIsCameraOpen) return@detectTapGestures
                                        if (currentIsDuoStatusExpanded) {
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
                                        if (currentIsAllAppsOpen || currentIsSettingsOpen || currentIsCameraOpen) return@detectTapGestures
                                        if (currentIsDuoStatusExpanded) {
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
                                    var collapsed = false
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) break
                                        val delta = change.position - change.previousPosition
                                        totalDrag += delta
                                        if (!collapsed && (totalDrag.y < -24f || (isOverOtherDrawers && (kotlin.math.abs(totalDrag.y) > 24f || kotlin.math.abs(totalDrag.x) > 24f)))) {
                                            isNotificationExpanded = false
                                            lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                            collapsed = true
                                        }
                                        if (collapsed) {
                                            change.consume()
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
                        surfaceThemeStyle = uiState.surfaceThemeStyle,
                        cinematicPaletteId = uiState.cinematicPalette,
                        surfaceOpacity = uiState.surfaceOpacity,
                        surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                        isMesmerizingGlow = uiState.dockMesmerizingGlow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(100f)
                            .onGloballyPositioned { coords ->
                                glanceBounds = coords.boundsInRoot()
                            }
                    )

                    if (staleTodoCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable {
                                    crispHaptics.click()
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(todoPageIndex)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$staleTodoCount ${if (staleTodoCount == 1) "task" else "tasks"} open for more than a day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "View",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Middle: Notifications (always full width at top) + Category Drawers (side-by-side or stacked vertically)
                    // ponytail: Category alignment respects user toggle between Bottom (thumb-zone) and Top (stacked under glance header).
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val minContentHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = minContentHeight)
                                .nestedScroll(homeNestedScrollConnection)
                                .verticalScroll(homeScrollState, enabled = !dragDropState.isDragging && !isNotificationExpanded),
                            verticalArrangement = if (uiState.categoryAlignment.equals("BOTTOM", ignoreCase = true)) {
                                Arrangement.spacedBy(10.dp, Alignment.Bottom)
                            } else {
                                Arrangement.spacedBy(10.dp, Alignment.Top)
                            }
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
                                    withContext(Dispatchers.IO) {
                                        try {
                                            context.packageManager.getApplicationIcon(packageName)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }
                            },
                            isDragging = dragDropState.isDragging,
                            stealthTheme = uiState.stealthTheme,
                            stealthContrastLevel = uiState.stealthContrastLevel,
                            stealthLimitBrightness = uiState.stealthLimitBrightness,
                            surfaceThemeStyle = uiState.surfaceThemeStyle,
                            isMesmerizingGlow = uiState.dockMesmerizingGlow,
                            surfaceOpacity = uiState.surfaceOpacity,
                            surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                            cinematicPaletteId = uiState.cinematicPalette
                        )

                        // ponytail: dropped AI assistant drawer completely; ceiling: zero on-screen assistant layout clutter; upgrade path: restore AiAssistantDrawer if on-device model is reintroduced.

                        // 2. Rest of the Drawers (App Categories): Side by side (2 columns) if enabled, otherwise single column
                        GroupsAccordion(
                            viewModel = groupsViewModel,
                            onAppClick = { app -> viewModel.onAppClicked(app) },
                            twoDrawersSideBySide = effectiveTwoDrawersSideBySide,
                            surfaceThemeStyle = uiState.surfaceThemeStyle,
                            isMesmerizingGlow = uiState.dockMesmerizingGlow,
                            surfaceOpacity = uiState.surfaceOpacity,
                            surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                            cinematicPaletteId = uiState.cinematicPalette,
                            iconShape = com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(
                                uiState.adaptiveIconShape
                            ),
                            hoveredGroupId = hoveredGroupId,
                            notificationPackages = activeNotificationPackages,
                            hideDrawerIcons = uiState.hideDrawerIcons,
                            onToggleExpand = { groupId ->
                                val group = groupsUiState.groups.find { it.group.id == groupId }
                                if (group != null && !group.group.isExpanded) {
                                    if (!effectiveTwoDrawersSideBySide) {
                                        isNotificationExpanded = false
                                    }
                                }
                                groupsViewModel.toggleGroupExpanded(groupId)
                            },
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

                        // Sane bottom spacing so expanded lower drawers can be scrolled comfortably above the dock
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                val currentHasNotifications by rememberUpdatedState(hasNotifications)
                val currentIsNotificationExpandedDock by rememberUpdatedState(isNotificationExpanded)
                val currentAnyCategoryExpandedDock by rememberUpdatedState(groupsUiState.groups.any { it.group.isExpanded })
                val currentIsAllAppsOpenDock by rememberUpdatedState(uiState.isAllAppsOpen)

                // Pager Indicator with red badge for stale tasks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (pageIdx in 0 until totalPages) {
                        val isSelected = pagerState.currentPage == pageIdx
                        val isTodoPage = pageIdx == todoPageIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clickable {
                                    crispHaptics.tick()
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pageIdx)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                            )
                            if (isTodoPage && staleTodoCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = (-8).dp)
                                        .size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (staleTodoCount > 9) "9+" else "$staleTodoCount",
                                            color = MaterialTheme.colorScheme.onError,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom: Dock (swiping up from dock opens app drawer 1:1 with finger)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .onGloballyPositioned { coords ->
                            dragDropState.dockBounds = coords.boundsInRoot()
                        }
                        // ponytail: stable drag gesture recognizer with rememberUpdatedState; won't restart/drop drags when notification badges update mid-drag.
                        .pointerInput(Unit) {
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
                                    if (!currentIsAllAppsOpenDock && !uiState.isVaultOpen && dockDrag < -55f) {
                                        if (currentIsNotificationExpandedDock) {
                                            isNotificationExpanded = false
                                            lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                        } else if (currentAnyCategoryExpandedDock) {
                                            groupsViewModel.collapseAllGroups()
                                            lastDrawerCollapseTime = android.os.SystemClock.uptimeMillis()
                                        } else {
                                            viewModel.openAllApps(currentActiveNotificationPkgs)
                                        }
                                        actionTriggered = true
                                    } else if (!currentIsAllAppsOpenDock && !uiState.isVaultOpen && dockDrag > 50f) {
                                        if (currentHasNotifications && !currentIsNotificationExpandedDock) {
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
                        isDropTarget = isOverDock,
                        onAppClick = { app -> viewModel.onAppClicked(app) },
                        onTriggerHiddenVault = {
                            if (!uiState.isAppVaultEnabled) return@DockBar
                            com.urunkarpm.drawer.feature.home.auth.VaultAuthenticator.authenticate(
                                context = context,
                                onSuccess = {
                                    viewModel.openVault()
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // ── Left Widgets Page, Center Home, and Right To-Do Page ──
    // Resistance connection for WidgetsPage pull from Home
    val widgetPageResistanceConnection = remember(pagerState, homePageIndex, todoPageIndex) {
        object : NestedScrollConnection {
            private var pullAccumulator = 0f
            private val resistanceGatePx = 72f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Rightmost page edge overscroll suppression
                if (source == NestedScrollSource.UserInput && pagerState.currentPage == todoPageIndex && available.x < 0f) {
                    return Offset(available.x, 0f)
                }
                // Resistance when pulling towards Widgets page from Home
                if (source == NestedScrollSource.UserInput && uiState.enableWidgetsPage && pagerState.currentPage == homePageIndex && available.x > 0f) {
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
                if (available.x < 0f && pagerState.currentPage == homePageIndex) {
                    pullAccumulator = 0f
                }
                // When on Widgets page (page 0), dragging or swiping right navigates back to Home
                if (source == NestedScrollSource.UserInput && uiState.enableWidgetsPage && pagerState.currentPage == 0 && available.x > 30f) {
                    coroutineScope.launch {
                        viewModel.collapseAllWidgetDrawers()
                        pagerState.animateScrollToPage(homePageIndex)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullAccumulator = 0f
                if (uiState.enableWidgetsPage && pagerState.currentPage == 0 && available.x > 250f) {
                    coroutineScope.launch {
                        viewModel.collapseAllWidgetDrawers()
                        pagerState.animateScrollToPage(homePageIndex)
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.targetPage, homePageIndex) {
        if (pagerState.currentPage == homePageIndex && pagerState.targetPage == homePageIndex) {
            viewModel.collapseAllWidgetDrawers()
            if (isNotificationExpanded) isNotificationExpanded = false
            if (groupsUiState.groups.any { it.group.isExpanded }) {
                groupsViewModel.collapseAllGroups()
            }
        }
    }

    // Back button: If user is on Widgets feed or To-Do feed, pressing Back returns to main Home
    val isNonHomePage = (uiState.enableWidgetsPage && pagerState.currentPage == 0) || (pagerState.currentPage == todoPageIndex)
    BackHandler(enabled = isNonHomePage && !isCameraOpen && !isSettingsOpen && !showHomeMenu && !uiState.isAllAppsOpen && uiState.selectedAppForMenu == null && appForCategorySelection == null && !isNotificationExpanded && !groupsUiState.groups.any { it.group.isExpanded }) {
        coroutineScope.launch {
            viewModel.collapseAllWidgetDrawers()
            pagerState.animateScrollToPage(homePageIndex)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(
        LocalOverscrollFactory provides null,
        LocalOverscrollConfiguration provides null
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !dragDropState.isDragging &&
                !uiState.isAllAppsOpen &&
                !uiState.isVaultOpen &&
                !isSettingsOpen &&
                !isCameraOpen &&
                !isNotificationExpanded &&
                !groupsUiState.groups.any { it.group.isExpanded },
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.35f
            ),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(widgetPageResistanceConnection)
        ) { page ->
            when {
                uiState.enableWidgetsPage && page == 0 -> {
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
                        drawerTitleAnimation = uiState.drawerTitleAnimation,
                        surfaceThemeStyle = uiState.surfaceThemeStyle,
                        isMesmerizingGlow = uiState.dockMesmerizingGlow,
                        surfaceOpacity = uiState.surfaceOpacity,
                        surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                        onBackToHome = {
                            coroutineScope.launch {
                                viewModel.collapseAllWidgetDrawers()
                                pagerState.animateScrollToPage(homePageIndex)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                            .navigationBarsPadding()
                    )
                }
                page == homePageIndex -> {
                    homeContent()
                }
                else -> {
                    com.urunkarpm.drawer.feature.todo.TodoScreen(
                        isWallpaperBlurActive = uiState.wallpaperBlur,
                        isVisible = pagerState.currentPage == todoPageIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

        // ── All Apps Drawer Overlay (slides up from bottom) ───────────────────
        val drawerIconShape = remember(uiState.adaptiveIconShape) {
            com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString(uiState.adaptiveIconShape)
        }
        val onDrawerAppClick: (AppInfo) -> Unit = remember(viewModel) { { app: AppInfo -> viewModel.onAppClicked(app); Unit } }
        val onDrawerAppLongClick: (AppInfo) -> Unit = remember(viewModel) { { app: AppInfo -> viewModel.onAppLongClicked(app); Unit } }
        val onDrawerClose = remember(viewModel) { { viewModel.closeAllApps() } }
        val onDrawerIconLoader: suspend (AppInfo) -> android.graphics.drawable.Drawable? = remember(viewModel) {
            { app: AppInfo -> viewModel.getAppIcon(app) }
        }
        val onDrawerOpenSettings = remember { { isSettingsOpen = true } }

        AllAppsDrawer(
            visible = uiState.isAllAppsOpen,
            apps = uiState.filteredApps,
            frequentApps = if (showDrawerSuggestions) suggestedApps else emptyList(),
            searchQuery = uiState.searchQuery,
            isLoading = uiState.isLoading,
            iconShape = drawerIconShape,
            dragDropState = if (groupsUiState.lockLayout) null else dragDropState,
            onDragDropResult = handleDragDropResult,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onAppClick = onDrawerAppClick,
            onAppLongClick = onDrawerAppLongClick,
            onClose = onDrawerClose,
            iconLoader = onDrawerIconLoader,
            drawerThemeMode = uiState.drawerThemeMode,
            drawerLiquidGlass = uiState.drawerLiquidGlass,
            drawerLook = uiState.drawerLook,
            matchDeviceScreenCurve = uiState.matchDeviceScreenCurve,
            deviceCurveOffsetDp = uiState.deviceCurveOffsetDp,
            surfaceCornerRadiusDp = uiState.surfaceCornerRadius,
            autoOpenKeyboard = uiState.autoOpenKeyboardInDrawer,
            blurAppListOnOpen = uiState.blurAppListOnOpen,
            appDrawerColumns = uiState.appDrawerColumns,
            onOpenSettings = onDrawerOpenSettings
        )

        // ── Hidden Drawer Vault Overlay ──────────────────────────────────────
        com.urunkarpm.drawer.feature.home.component.HiddenVaultDrawer(
            isOpen = uiState.isVaultOpen && uiState.isAppVaultEnabled,
            onClose = { viewModel.closeVault() },
            hiddenApps = uiState.hiddenApps,
            allInstalledApps = uiState.rawInstalledApps,
            onLaunchApp = { app -> viewModel.onAppClicked(app) },
            onUnhideApp = { pkg -> viewModel.removeHiddenApp(pkg) },
            onAddHiddenApp = { pkg -> viewModel.addHiddenApp(pkg) },
            onAddHiddenApps = { pkgs -> viewModel.addHiddenApps(pkgs) },
            notificationsViewModel = notificationsViewModel,
            drawerThemeMode = uiState.drawerThemeMode,
            drawerLiquidGlass = uiState.drawerLiquidGlass,
            drawerLook = uiState.drawerLook,
            surfaceCornerRadiusDp = uiState.surfaceCornerRadius,
            matchDeviceScreenCurve = uiState.matchDeviceScreenCurve,
            deviceCurveOffsetDp = uiState.deviceCurveOffsetDp,
            iconLoader = { app -> viewModel.getAppIcon(app) },
            onOpenAppDetails = { pkg ->
                val app = uiState.rawInstalledApps.find { it.packageName == pkg }
                if (app != null) viewModel.openAppDetails(app)
            }
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
                shortcutIconLoader = { shortcutId -> viewModel.getShortcutIcon(selectedApp, shortcutId) },
                onHideFromSuggestions = { viewModel.hideFromSuggestions(selectedApp.packageName) },
                isMindful = selectedApp.packageName in uiState.mindfulApps,
                onToggleMindful = { viewModel.toggleMindfulApp(selectedApp.packageName) }
            )
        }

        // ── Mindful Pause Dialog (3-second anti-doomscroll breathing prompt) ───
        val pendingMindful = uiState.pendingMindfulApp
        if (pendingMindful != null) {
            MindfulPauseDialog(
                app = pendingMindful,
                onDismiss = { viewModel.cancelMindfulLaunch() },
                onConfirm = { viewModel.confirmMindfulLaunch() },
                iconLoader = { viewModel.getAppIcon(pendingMindful) }
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
                    } else {
                        groupsViewModel.assignAppToGroup(group.id, targetApp)
                    }
                    appForCategorySelection = null
                },
                onCreateNewGroup = {
                    appForCategorySelection = null
                    groupsViewModel.setShowCreateDialog(true)
                }
            )
        }

        // ── New App Install Suggestion Dialog ─────────────────────────────────
        val installSuggestion = groupsUiState.installSuggestion
        if (installSuggestion != null) {
            val (app, group) = installSuggestion
            AlertDialog(
                onDismissRequest = { groupsViewModel.dismissInstallSuggestion() },
                title = { Text("Add to ${group.name}?") },
                text = { Text("Would you like to add ${app.label} to the ${group.name} drawer?") },
                confirmButton = {
                    TextButton(onClick = { groupsViewModel.acceptInstallSuggestion(app, group) }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { groupsViewModel.dismissInstallSuggestion() }) {
                        Text("Not now")
                    }
                }
            )
        }

        // ── In-App Update Prompt Dialog ─────────────────────────────────────────
        val updateToPrompt = availableUpdate
        if (updateToPrompt != null) {
            AlertDialog(
                onDismissRequest = {
                    crispHaptics.tick()
                    viewModel.dismissUpdate(updateToPrompt.latestVersionTag)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "Update Available",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = updateToPrompt.releaseName.ifBlank { updateToPrompt.latestVersionTag },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A new version of Drawer (${updateToPrompt.latestVersionTag}) is available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (updateToPrompt.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = updateToPrompt.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 8,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            crispHaptics.confirm()
                            val downloadUrl = updateToPrompt.apkDownloadUrl ?: updateToPrompt.htmlUrl
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            viewModel.dismissUpdate(updateToPrompt.latestVersionTag)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (updateToPrompt.apkDownloadUrl?.endsWith(".apk", ignoreCase = true) == true) "Download APK" else "Open Release")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            crispHaptics.tick()
                            viewModel.dismissUpdate(updateToPrompt.latestVersionTag)
                        }
                    ) {
                        Text("Later")
                    }
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
                onOrganizeWithAi = {
                    showHomeMenu = false
                    groupsViewModel.startAiCategorization()
                },
                onAddWidget = {
                    showHomeMenu = false
                    launchAddWidgetFlow()
                }
            )
        }

        if (showWidgetPickerSheet) {
            val targetTitle = uiState.widgetDrawers.find { it.id == targetDrawerIdForAdd }?.title
            WidgetPickerBottomSheet(
                widgetHostManager = widgetHostManager,
                targetDrawerTitle = targetTitle,
                onWidgetSelected = { info ->
                    showWidgetPickerSheet = false
                    handleSelectedWidget(info)
                },
                onLaunchSystemPicker = {
                    showWidgetPickerSheet = false
                    launchSystemWidgetPicker()
                },
                onDismissRequest = {
                    showWidgetPickerSheet = false
                    targetDrawerIdForAdd = null
                    viewModel.pendingAddTargetDrawerId = null
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
                categoryAlignment = uiState.categoryAlignment,
                onSetCategoryAlignment = viewModel::setCategoryAlignment,
                enableWidgetsPage = uiState.enableWidgetsPage,
                onSetEnableWidgetsPage = viewModel::setEnableWidgetsPage,
                drawerTitleAnimation = uiState.drawerTitleAnimation,
                onSetDrawerTitleAnimation = viewModel::setDrawerTitleAnimation,
                openDrawerFromDockOnly = uiState.openDrawerFromDockOnly,
                onSetOpenDrawerFromDockOnly = viewModel::setOpenDrawerFromDockOnly,
                autoArrangeApps = autoArrangeApps,
                onSetAutoArrangeApps = viewModel::setAutoArrangeApps,
                multiGroupApps = multiGroupApps,
                onSetMultiGroupApps = viewModel::setMultiGroupApps,
                lockLayout = lockLayout,
                onSetLockLayout = viewModel::setLockLayout,
                autoCategorizeNewApps = autoCategorizeNewApps,
                onSetAutoCategorizeNewApps = viewModel::setAutoCategorizeNewApps,
                themeMode = themeMode,
                onSetThemeMode = viewModel::setThemeMode,
                drawerThemeMode = uiState.drawerThemeMode,
                onSetDrawerThemeMode = viewModel::setDrawerThemeMode,
                dynamicColor = dynamicColor,
                onSetDynamicColor = viewModel::setDynamicColor,
                accentColor = accentColor,
                onSetAccentColor = viewModel::setAccentColor,
                cinematicPalette = uiState.cinematicPalette,
                onSetCinematicPalette = viewModel::setCinematicPalette,
                surfaceThemeStyle = uiState.surfaceThemeStyle,
                onSetSurfaceThemeStyle = viewModel::setSurfaceThemeStyle,
                surfaceOpacity = uiState.surfaceOpacity,
                onSetSurfaceOpacity = viewModel::setSurfaceOpacity,
                surfaceBlurRadius = uiState.surfaceBlurRadius,
                onSetSurfaceBlurRadius = viewModel::setSurfaceBlurRadius,
                surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                onSetSurfaceStrokeOpacity = viewModel::setSurfaceStrokeOpacity,
                surfaceCornerRadius = uiState.surfaceCornerRadius,
                onSetSurfaceCornerRadius = viewModel::setSurfaceCornerRadius,
                wallpaperBlur = uiState.wallpaperBlur,
                onSetWallpaperBlur = viewModel::setWallpaperBlur,
                wallpaperBlurRadius = uiState.wallpaperBlurRadius,
                onSetWallpaperBlurRadius = viewModel::setWallpaperBlurRadius,
                isDynamicWallpaper = uiState.dynamicWallpaperEnabled,
                onSelectDynamicWallpaper = viewModel::setDynamicWallpaperEnabled,
                wallpaperGradientSpeedSeconds = uiState.wallpaperGradientSpeedSeconds,
                onSetWallpaperGradientSpeedSeconds = viewModel::setWallpaperGradientSpeedSeconds,
                wallpaperGradientPalette = uiState.wallpaperGradientPalette,
                onSetWallpaperGradientPalette = viewModel::setWallpaperGradientPalette,
                wallpaperGradientAngle = uiState.wallpaperGradientAngle,
                onSetWallpaperGradientAngle = viewModel::setWallpaperGradientAngle,
                wallpaperGradientBlurPercent = uiState.wallpaperGradientBlurPercent,
                onSetWallpaperGradientBlurPercent = viewModel::setWallpaperGradientBlurPercent,
                hideStatusBar = uiState.hideStatusBar,
                onSetHideStatusBar = viewModel::setHideStatusBar,
                adaptiveIconShape = uiState.adaptiveIconShape,
                onSetAdaptiveIconShape = viewModel::setAdaptiveIconShape,
                headerFontFamily = uiState.headerFontFamily,
                onSetHeaderFontFamily = viewModel::setHeaderFontFamily,
                appLabelScale = uiState.appLabelScale,
                onSetAppLabelScale = viewModel::setAppLabelScale,
                hideDrawerIcons = uiState.hideDrawerIcons,
                onSetHideDrawerIcons = viewModel::setHideDrawerIcons,
                dockMesmerizingGlow = uiState.dockMesmerizingGlow,
                onSetDockMesmerizingGlow = viewModel::setDockMesmerizingGlow,
                dockShowLabels = dockShowLabels,
                onSetDockShowLabels = viewModel::setDockShowLabels,
                dockIconSize = dockIconSize,
                onSetDockIconSize = viewModel::setDockIconSize,
                is24Hour = uiState.is24Hour,
                onSetIs24Hour = viewModel::setIs24Hour,
                showWeather = uiState.showWeather,
                onSetShowWeather = viewModel::setShowWeather,
                weatherUnit = uiState.weatherUnit,
                onSetWeatherUnit = viewModel::setWeatherUnit,
                showDuoStatusWidget = uiState.showDuoStatusWidget,
                onSetShowDuoStatusWidget = viewModel::setShowDuoStatusWidget,
                autoOpenKeyboardInDrawer = uiState.autoOpenKeyboardInDrawer,
                onSetAutoOpenKeyboardInDrawer = viewModel::setAutoOpenKeyboardInDrawer,
                blurAppListOnOpen = uiState.blurAppListOnOpen,
                onSetBlurAppListOnOpen = viewModel::setBlurAppListOnOpen,
                showDrawerSuggestions = showDrawerSuggestions,
                onSetShowDrawerSuggestions = viewModel::setShowDrawerSuggestions,
                stealthTheme = uiState.stealthTheme,
                onSetStealthTheme = viewModel::setStealthTheme,
                stealthContrastLevel = uiState.stealthContrastLevel,
                onSetStealthContrastLevel = viewModel::setStealthContrastLevel,
                sampleApps = uiState.installedApps.take(8),
                iconLoader = { app -> viewModel.getAppIcon(app) },
                weatherInfo = uiState.weatherInfo
            )
        }
    }
}
}
