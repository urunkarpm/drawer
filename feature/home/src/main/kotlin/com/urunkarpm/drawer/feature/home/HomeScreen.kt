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
import android.content.ActivityNotFoundException
import android.util.Log
import com.urunkarpm.drawer.core.needle.action.LauncherAction
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import com.urunkarpm.drawer.core.needle.util.SpokenTextNormalizer
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale
import android.provider.Settings
import android.provider.AlarmClock
import android.provider.MediaStore
import android.net.Uri
import android.media.AudioManager
import kotlin.math.pow

import kotlin.math.roundToInt
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.feature.home.component.WidgetsPage
import com.urunkarpm.drawer.feature.home.component.WidgetPickerBottomSheet
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
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
    onDoubleTapLock: () -> Unit = {},
    homeResetTrigger: Long = 0L
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val suggestedApps by viewModel.suggestedApps.collectAsStateWithLifecycle()
    val showDrawerSuggestions by viewModel.showDrawerSuggestions.collectAsStateWithLifecycle()
    val suggestedAction by viewModel.suggestedAction.collectAsStateWithLifecycle()
    val modelProcessingState by viewModel.modelProcessingState.collectAsStateWithLifecycle()
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

    val handleVoiceCommand = { spokenCandidates: List<String> ->
        coroutineScope.launch {
            if (spokenCandidates.isEmpty()) return@launch
            val primary = spokenCandidates.first().trim()
            if (primary.isBlank()) return@launch

            // 1. Check candidates to see if any matches high-confidence tool actions
            var selectedText = primary
            var resolvedToolCalls: List<ToolCallResult> = emptyList()

            for (cand in spokenCandidates) {
                val norm = SpokenTextNormalizer.normalize(cand.trim())
                val calls = viewModel.predictVoiceTools(norm)
                if (calls.isNotEmpty()) {
                    selectedText = norm
                    resolvedToolCalls = calls
                    break
                }
            }

            if (resolvedToolCalls.isEmpty()) {
                selectedText = SpokenTextNormalizer.normalize(primary)
                resolvedToolCalls = viewModel.predictVoiceTools(selectedText)
            }

            val trimmed = selectedText.trim()
            val lower = trimmed.lowercase()

            val toolCalls = resolvedToolCalls
            val drawerOrDockTools = setOf(
                "add_app_to_drawer",
                "remove_app_from_drawer",
                "move_app_between_drawers",
                "add_app_to_dock",
                "remove_app_from_dock",
                "create_drawer",
                "delete_drawer",
                "rename_drawer",
                "calculate"
            )

            if (toolCalls.any { it.toolName in drawerOrDockTools }) {
                fun resolveApp(name: String): AppInfo? {
                    val rawClean = name.trim()
                        .trim('"', '\'', '`', '“', '”')
                        .trimEnd('.', '?', '!', ',', ';')
                        .replaceFirst(Regex("(?i)^(?:the\\s+app\\s+|app\\s+|the\\s+)"), "")
                        .trim()
                    val clean = rawClean.lowercase()
                    val cleanNoSpaces = clean.replace(Regex("[^a-zA-Z0-9]"), "")

                    val candidateApps = (uiState.installedApps + groupsUiState.groups.flatMap { it.apps }).distinctBy { it.componentKey }

                    // 1. Exact match (case insensitive)
                    candidateApps.firstOrNull { it.label.equals(clean, ignoreCase = true) }?.let { return it }

                    // 2. Alphanumeric match (ignoring spaces, hyphens, e.g. "chat gpt" == "ChatGPT", "whats app" == "WhatsApp")
                    if (cleanNoSpaces.isNotEmpty()) {
                        candidateApps.firstOrNull {
                            it.label.lowercase().replace(Regex("[^a-zA-Z0-9]"), "") == cleanNoSpaces
                        }?.let { return it }
                    }

                    // 3. Substring match
                    candidateApps.firstOrNull { it.label.lowercase().contains(clean) }?.let { return it }

                    // 4. Package name match
                    if (cleanNoSpaces.length >= 3) {
                        candidateApps.firstOrNull {
                            it.packageName.lowercase().replace(Regex("[^a-zA-Z0-9]"), "").contains(cleanNoSpaces)
                        }?.let { return it }
                    }

                    // 5. Prefix match
                    if (clean.length >= 3) {
                        candidateApps.firstOrNull { it.label.startsWith(clean, ignoreCase = true) }?.let { return it }
                    }

                    return null
                }

                val successSummaries = mutableListOf<String>()
                val undoActions = mutableListOf<suspend () -> Unit>()

                for (call in toolCalls) {
                    when (call.toolName) {
                        "add_app_to_drawer" -> {
                            val appName = call.getString("app") ?: ""
                            val drawerName = call.getString("drawer") ?: ""
                            val app = resolveApp(appName)
                            if (app != null && drawerName.isNotBlank()) {
                                val targetGroup = groupsViewModel.getOrCreateGroupByName(drawerName)
                                val prevGroups = groupsUiState.groups.filter { g -> g.apps.any { a -> a.componentKey == app.componentKey } }
                                groupsViewModel.assignAppToGroupDirectly(targetGroup.id, app)
                                successSummaries.add("Added ${app.label} to ${targetGroup.name}")
                                undoActions.add {
                                    groupsViewModel.removeAppFromGroupDirectly(targetGroup.id, app.packageName, app.activityName)
                                    for (pg in prevGroups) {
                                        groupsViewModel.assignAppToGroupDirectly(pg.group.id, app)
                                    }
                                }
                            } else if (app == null) {
                                successSummaries.add("Could not find app \"$appName\"")
                            }
                        }
                        "remove_app_from_drawer" -> {
                            val appName = call.getString("app") ?: ""
                            val drawerName = call.getString("drawer")
                            val app = resolveApp(appName)
                            if (app != null) {
                                val targetGroup = if (!drawerName.isNullOrBlank()) {
                                    groupsViewModel.findGroupByName(drawerName)
                                } else null

                                if (targetGroup != null) {
                                    val isAppInGroup = groupsUiState.groups
                                        .find { it.group.id == targetGroup.id }
                                        ?.apps
                                        ?.any { it.componentKey == app.componentKey || it.packageName == app.packageName } == true

                                    if (isAppInGroup) {
                                        groupsViewModel.removeAppFromGroupDirectly(targetGroup.id, app.packageName, app.activityName)
                                        successSummaries.add("Removed ${app.label} from ${targetGroup.name}")
                                        undoActions.add {
                                            groupsViewModel.assignAppToGroupDirectly(targetGroup.id, app)
                                        }
                                    } else {
                                        successSummaries.add("${app.label} is not in ${targetGroup.name} folder")
                                    }
                                } else {
                                    val groupsToRemoveFrom = groupsUiState.groups
                                        .filter { it.apps.any { a -> a.componentKey == app.componentKey || a.packageName == app.packageName } }
                                        .map { it.group }

                                    if (groupsToRemoveFrom.isNotEmpty()) {
                                        for (g in groupsToRemoveFrom) {
                                            groupsViewModel.removeAppFromGroupDirectly(g.id, app.packageName, app.activityName)
                                            successSummaries.add("Removed ${app.label} from ${g.name}")
                                            undoActions.add {
                                                groupsViewModel.assignAppToGroupDirectly(g.id, app)
                                            }
                                        }
                                    } else {
                                        successSummaries.add("${app.label} is not in any folder")
                                    }
                                }
                            } else {
                                val fromText = if (!drawerName.isNullOrBlank()) " from $drawerName" else ""
                                successSummaries.add("Could not find app \"$appName\"$fromText")
                            }
                        }
                        "move_app_between_drawers" -> {
                            val appName = call.getString("app") ?: ""
                            val fromDrawer = call.getString("from_drawer") ?: ""
                            val toDrawer = call.getString("to_drawer") ?: ""
                            val app = resolveApp(appName)
                            if (app != null && toDrawer.isNotBlank()) {
                                val sourceGroup = if (fromDrawer.isNotBlank()) groupsViewModel.findGroupByName(fromDrawer) else null
                                val targetGroup = groupsViewModel.getOrCreateGroupByName(toDrawer)
                                val sourceGroupId = sourceGroup?.id
                                    ?: groupsUiState.groups.firstOrNull { it.apps.any { a -> a.componentKey == app.componentKey || a.packageName == app.packageName } }?.group?.id

                                if (sourceGroupId != null && sourceGroupId != targetGroup.id) {
                                    groupsViewModel.moveAppBetweenGroupsDirectly(sourceGroupId, targetGroup.id, app)
                                    successSummaries.add("Moved ${app.label} from ${sourceGroup?.name ?: "folder"} to ${targetGroup.name}")
                                    undoActions.add {
                                        groupsViewModel.moveAppBetweenGroupsDirectly(targetGroup.id, sourceGroupId, app)
                                    }
                                } else {
                                    groupsViewModel.assignAppToGroupDirectly(targetGroup.id, app)
                                    successSummaries.add("Added ${app.label} to ${targetGroup.name}")
                                    undoActions.add {
                                        groupsViewModel.removeAppFromGroupDirectly(targetGroup.id, app.packageName, app.activityName)
                                    }
                                }
                            } else if (app == null) {
                                successSummaries.add("Could not find app \"$appName\"")
                            }
                        }
                        "add_app_to_dock" -> {
                            val appName = call.getString("app") ?: ""
                            val app = resolveApp(appName)
                            if (app != null) {
                                val added = viewModel.addAppToDock(app)
                                if (added) {
                                    successSummaries.add("Added ${app.label} to dock")
                                    undoActions.add {
                                        viewModel.removeAppFromDock(app)
                                    }
                                }
                            }
                        }
                        "remove_app_from_dock" -> {
                            val appName = call.getString("app") ?: ""
                            val app = resolveApp(appName)
                            if (app != null) {
                                val removed = viewModel.removeAppFromDock(app)
                                if (removed) {
                                    successSummaries.add("Removed ${app.label} from dock")
                                    undoActions.add {
                                        viewModel.addAppToDock(app)
                                    }
                                }
                            }
                        }
                        "create_drawer" -> {
                            val drawerName = call.getString("drawer") ?: ""
                            if (drawerName.isNotBlank()) {
                                val targetGroup = groupsViewModel.getOrCreateGroupByName(drawerName)
                                successSummaries.add("Created folder ${targetGroup.name}")
                                undoActions.add {
                                    groupsViewModel.deleteGroupDirectly(targetGroup.id)
                                }
                            }
                        }
                        "delete_drawer" -> {
                            val drawerName = call.getString("drawer") ?: ""
                            if (drawerName.isNotBlank()) {
                                val targetGroup = groupsViewModel.findGroupByName(drawerName)
                                if (targetGroup != null) {
                                    val groupApps = groupsUiState.groups.find { it.group.id == targetGroup.id }?.apps ?: emptyList()
                                    groupsViewModel.deleteGroupDirectly(targetGroup.id)
                                    successSummaries.add("Deleted folder ${targetGroup.name}")
                                    undoActions.add {
                                        groupsViewModel.restoreGroupWithApps(targetGroup, groupApps)
                                    }
                                }
                            }
                        }
                        "rename_drawer" -> {
                            val fromName = call.getString("from_name") ?: ""
                            val toName = call.getString("to_name") ?: ""
                            if (fromName.isNotBlank() && toName.isNotBlank()) {
                                val targetGroup = groupsViewModel.findGroupByName(fromName)
                                if (targetGroup != null) {
                                    val oldName = targetGroup.name
                                    groupsViewModel.renameGroupDirectly(targetGroup.id, toName)
                                    successSummaries.add("Renamed folder $oldName to $toName")
                                    undoActions.add {
                                        groupsViewModel.renameGroupDirectly(targetGroup.id, oldName)
                                    }
                                }
                            }
                        }
                        "calculate" -> {
                            val expression = call.getString("expression") ?: ""
                            val result = call.getString("result") ?: ""
                            val summary = if (expression.isNotBlank() && result.isNotBlank()) "$expression = $result" else result
                            if (summary.isNotBlank()) {
                                successSummaries.add(summary)
                            }
                        }
                    }
                }

                if (successSummaries.isNotEmpty()) {
                    val summaryText = successSummaries.joinToString(", ")
                    viewModel.notifyCommandProcessed(summaryText)
                    return@launch
                }
            }

            // Direct launcher settings controls
            for (call in toolCalls.filter { it.toolName == "change_app_setting" }) {
                val setting = call.getString("setting") ?: ""
                val value = call.getString("value")
                val summary = viewModel.changeAppSetting(setting, value)
                viewModel.notifyCommandProcessed(summary)
                return@launch
            }

            // Direct brightness controls
            for (call in toolCalls.filter { it.toolName == "set_brightness" }) {
                val pct = call.getInt("percent")
                val action = call.getString("action") ?: "set"
                val canWrite = Settings.System.canWrite(context)
                if (!canWrite) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    viewModel.notifyCommandProcessed("Please allow 'Modify system settings' permission to let the assistant adjust brightness directly")
                    return@launch
                }

                val currentVal = try {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                } catch (_: Exception) { 128 }

                val targetVal = when {
                    pct != null -> (pct / 100f * 255).roundToInt().coerceIn(1, 255)
                    action == "max" -> 255
                    action == "min" -> 10
                    action == "increase" -> (currentVal + 51).coerceIn(1, 255)
                    action == "decrease" -> (currentVal - 51).coerceIn(1, 255)
                    else -> currentVal
                }

                try {
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetVal)
                    (context as? Activity)?.window?.let { w ->
                        val lp = w.attributes
                        lp.screenBrightness = targetVal / 255f
                        w.attributes = lp
                    }
                    val resultPct = (targetVal / 255f * 100).roundToInt()
                    viewModel.notifyCommandProcessed("Brightness set to $resultPct%")
                } catch (e: Exception) {
                    viewModel.notifyCommandFailed("Could not change brightness: ${e.message}")
                }
                return@launch
            }

            // Direct volume controls
            for (call in toolCalls.filter { it.toolName == "set_volume" }) {
                val streamStr = call.getString("stream") ?: "MEDIA"
                val pct = call.getInt("percent")
                val action = call.getString("action") ?: "set"
                val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager == null) {
                    viewModel.notifyCommandFailed("Audio service unavailable")
                    return@launch
                }

                val stream = when (streamStr.uppercase()) {
                    "RING" -> AudioManager.STREAM_RING
                    "ALARM" -> AudioManager.STREAM_ALARM
                    "NOTIFICATION" -> AudioManager.STREAM_NOTIFICATION
                    else -> AudioManager.STREAM_MUSIC
                }

                val maxVolume = audioManager.getStreamMaxVolume(stream)
                val currentVolume = audioManager.getStreamVolume(stream)

                when (action) {
                    "mute" -> {
                        audioManager.setStreamVolume(stream, 0, AudioManager.FLAG_SHOW_UI)
                        viewModel.notifyCommandProcessed("Volume muted")
                    }
                    "unmute" -> {
                        val target = (maxVolume * 0.5f).roundToInt().coerceAtLeast(1)
                        audioManager.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
                        viewModel.notifyCommandProcessed("Volume unmuted")
                    }
                    "increase" -> {
                        val target = (currentVolume + (maxVolume * 0.15f).roundToInt().coerceAtLeast(1)).coerceAtMost(maxVolume)
                        audioManager.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
                        val targetPct = (target.toFloat() / maxVolume * 100).roundToInt()
                        viewModel.notifyCommandProcessed("Volume raised to $targetPct%")
                    }
                    "decrease" -> {
                        val target = (currentVolume - (maxVolume * 0.15f).roundToInt().coerceAtLeast(1)).coerceAtLeast(0)
                        audioManager.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
                        val targetPct = (target.toFloat() / maxVolume * 100).roundToInt()
                        viewModel.notifyCommandProcessed("Volume lowered to $targetPct%")
                    }
                    else -> {
                        if (pct != null) {
                            val target = (pct / 100f * maxVolume).roundToInt().coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
                            viewModel.notifyCommandProcessed("Volume set to $pct%")
                        } else {
                            viewModel.notifyCommandProcessed("Current volume is ${(currentVolume.toFloat() / maxVolume * 100).roundToInt()}%")
                        }
                    }
                }
                return@launch
            }

            // Direct custom/private DNS controls
            for (call in toolCalls.filter { it.toolName == "set_dns" }) {
                val host = call.getString("host")
                val mode = call.getString("mode") ?: if (host != null) "hostname" else "off"

                var secureWriteSuccess = false
                try {
                    if (mode == "off") {
                        Settings.Global.putString(context.contentResolver, "private_dns_mode", "off")
                        Settings.Global.putString(context.contentResolver, "private_dns_specifier", "")
                        secureWriteSuccess = true
                        viewModel.notifyCommandProcessed("Private DNS turned off")
                    } else if (!host.isNullOrBlank()) {
                        Settings.Global.putString(context.contentResolver, "private_dns_mode", "hostname")
                        Settings.Global.putString(context.contentResolver, "private_dns_specifier", host)
                        secureWriteSuccess = true
                        viewModel.notifyCommandProcessed("Private DNS set to $host")
                    }
                } catch (_: SecurityException) {
                    secureWriteSuccess = false
                } catch (_: Throwable) {
                    secureWriteSuccess = false
                }

                if (!secureWriteSuccess) {
                    if (!host.isNullOrBlank()) {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Private DNS", host))
                    }
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {}

                    val msg = if (!host.isNullOrBlank()) {
                        "Copied $host to clipboard and opened Network Settings. Grant ADB permission ('adb shell pm grant com.urunkarpm.drawer android.permission.WRITE_SECURE_SETTINGS') for zero-tap DNS changes."
                    } else {
                        "Opened Network Settings. Grant ADB permission ('adb shell pm grant com.urunkarpm.drawer android.permission.WRITE_SECURE_SETTINGS') to manage Private DNS directly."
                    }
                    viewModel.notifyCommandProcessed(msg)
                }
                return@launch
            }

            // 2. Intent-based tool calls (Timer, Alarm, Settings, Camera, etc.) predicted by Needle
            for (call in toolCalls) {
                val action = viewModel.dispatchToolCall(call)
                if (action != null) {
                    val executed = executeLauncherAction(
                        context = context,
                        action = action,
                        toolCall = call,
                        onSuccess = { summary ->
                            viewModel.notifyCommandProcessed(summary)
                        },
                        onError = { error ->
                            viewModel.notifyCommandFailed("Failed: ${action.title}")
                        }
                    )
                    if (executed) {
                        return@launch
                    }
                }
            }

            val timerPattern = java.util.regex.Pattern.compile("(?i)(?:(?:set|start|create)\\s+(?:a\\s+)?)?timer\\s+(?:for\\s+)?(\\d+)\\s*(m|min|minute|minutes|s|sec|second|seconds)?")
            val timerMatcher = timerPattern.matcher(trimmed)
            val isTimer = timerMatcher.find()

            val alarmPattern = java.util.regex.Pattern.compile("(?i)(?:(?:set|create)\\s+(?:an?\\s+)?)?alarm\\s+(?:for\\s+)?(\\d{1,2})(?::|\\s+)?(\\d{2})?\\s*(am|pm)?")
            val alarmMatcher = alarmPattern.matcher(trimmed)
            val isAlarm = alarmMatcher.find()

            when {
            // 1. Flashlight / Torch
            lower.contains("flashlight") || lower.contains("torch") -> {
                val enable = if (lower.contains("off") || lower.contains("disable") || lower.contains("stop")) false else true
                val success = toggleTorch(context, enable)
                if (success) {
                    viewModel.notifyCommandProcessed(if (enable) "Flashlight turned on" else "Flashlight turned off")
                } else {
                    try {
                        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        viewModel.notifyCommandProcessed("Opening Camera")
                    } catch (_: Exception) {}
                }
            }

            // 2. Bluetooth
            lower.contains("bluetooth") -> {
                try {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Bluetooth Settings")
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                        viewModel.notifyCommandProcessed("Opening Settings")
                    } catch (_: Exception) {}
                }
            }

            // 3. Wi-Fi / Internet
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") -> {
                try {
                    val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        Intent("android.settings.panel.action.INTERNET").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else {
                        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Wi-Fi Panel")
                } catch (_: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                        viewModel.notifyCommandProcessed("Opening Wi-Fi Settings")
                    } catch (_: Exception) {}
                }
            }

            // 4. Hotspot / Tethering
            lower.contains("hotspot") || lower.contains("tethering") -> {
                try {
                    val intent = Intent().apply {
                        component = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Hotspot Settings")
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        viewModel.notifyCommandProcessed("Opening Wireless Settings")
                    } catch (_: Exception) {}
                }
            }

            // 5. Do Not Disturb
            lower.contains("do not disturb") || lower == "dnd" -> {
                try {
                    val intent = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Do Not Disturb Settings")
                } catch (_: Exception) {}
            }

            // 6. Sound / Volume
            lower.contains("volume") || lower.contains("sound") || lower.contains("mute") -> {
                try {
                    val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Sound Settings")
                } catch (_: Exception) {}
            }

            // 7. Display / Brightness
            lower.contains("brightness") || lower.contains("display setting") -> {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Display Settings")
                } catch (_: Exception) {}
            }

            // 8. Battery / Power
            lower.contains("battery") || lower.contains("power save") || lower.contains("battery saver") -> {
                try {
                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Battery Settings")
                } catch (_: Exception) {}
            }

            // 9. Airplane Mode
            lower.contains("airplane mode") || lower.contains("aeroplane mode") -> {
                try {
                    val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Airplane Mode Settings")
                } catch (_: Exception) {}
            }

            // 10. System Settings
            lower == "settings" || lower == "open settings" || lower == "system settings" -> {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Settings")
                } catch (_: Exception) {}
            }

            // 11. Camera
            lower.contains("camera") || lower.contains("take a picture") || lower.contains("take a photo") || lower.contains("take a selfie") -> {
                try {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    viewModel.notifyCommandProcessed("Opening Camera")
                } catch (_: Exception) {}
            }

            // 12. Timer
            isTimer -> {
                val count = timerMatcher.group(1)?.toIntOrNull() ?: 1
                val unit = timerMatcher.group(2)?.lowercase() ?: "m"
                val seconds = if (unit.startsWith("s")) count else count * 60
                val durationText = if (seconds >= 60) "${seconds / 60}m" else "${seconds}s"
                val success = launchTimerWithFallbacks(context, seconds, null)
                if (success) {
                    viewModel.notifyCommandProcessed("Set timer for $durationText")
                } else {
                    viewModel.notifyCommandFailed("Failed: Set timer")
                }
            }

            // 13. Alarm
            isAlarm -> {
                var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
                val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
                val amPm = alarmMatcher.group(3)?.lowercase()
                if (amPm == "pm" && hour < 12) hour += 12
                if (amPm == "am" && hour == 12) hour = 0
                val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
                val success = launchAlarmWithFallbacks(context, hour, minute, null)
                if (success) {
                    viewModel.notifyCommandProcessed("Set alarm for $timeStr")
                } else {
                    viewModel.notifyCommandFailed("Failed: Set alarm")
                }
            }

            // 14. Direct App Launch (e.g. "open YouTube", "WhatsApp", "Spotify")
            else -> {
                val installedApps = uiState.installedApps
                val matchedApp = spokenCandidates.asSequence().mapNotNull { cand ->
                    val clean = SpokenTextNormalizer.normalize(cand.trim())
                        .replaceFirst(Regex("^(?:open|launch|start|go to|run)\\s+", RegexOption.IGNORE_CASE), "").trim()
                    installedApps.firstOrNull { it.label.equals(clean, ignoreCase = true) }
                        ?: installedApps.firstOrNull { it.label.equals(cand.trim(), ignoreCase = true) }
                        ?: (if (clean.length >= 3) installedApps.firstOrNull { it.label.startsWith(clean, ignoreCase = true) } else null)
                        ?: (if (clean.length >= 4) installedApps.firstOrNull { it.label.contains(clean, ignoreCase = true) } else null)
                }.firstOrNull()

                if (matchedApp != null) {
                    viewModel.onAppClicked(matchedApp)
                    viewModel.notifyCommandProcessed("Opened ${matchedApp.label}")
                } else {
                    val cleanForSearch = trimmed.replaceFirst(Regex("^(?:open|launch|start|go to|run)\\s+", RegexOption.IGNORE_CASE), "").trim()
                    val isExplicitSearch = cleanForSearch.startsWith("search ", ignoreCase = true) ||
                                           cleanForSearch.startsWith("find ", ignoreCase = true) ||
                                           trimmed.startsWith("search ", ignoreCase = true) ||
                                           trimmed.startsWith("find ", ignoreCase = true)
                    if (isExplicitSearch) {
                        val query = trimmed.replaceFirst(Regex("^(?:search|find)\\s+", RegexOption.IGNORE_CASE), "").trim()
                        viewModel.openAllApps(activeNotificationPkgs)
                        viewModel.onSearchQueryChanged(query)
                        viewModel.notifyCommandProcessed("Searching for \"$query\"")
                    } else {
                        viewModel.notifyCommandFailed("No app or action found for \"$trimmed\"")
                    }
                }
            }
        }
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenCandidates = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            if (spokenCandidates.isNotEmpty()) {
                handleVoiceCommand(spokenCandidates)
            }
        }
    }

    val launchSpeechIntent = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak an action or app name...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizerLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VOICE_COMMAND)
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                viewModel.openAllApps(activeNotificationPkgs)
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechIntent()
        }
    }

    val handleVoiceSearch = {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            launchSpeechIntent()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
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

    val widgetHostManager = widgetHostManager ?: remember { DrawerWidgetHostManager(context.applicationContext) }
    var pendingAppWidgetId by rememberSaveable { mutableStateOf<Int?>(null) }
    var targetDrawerIdForAdd by rememberSaveable { mutableStateOf<String?>(null) }
    var showWidgetPickerSheet by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { 2 }
    )

    val homeScale = remember { Animatable(1f) }
    val homeAlpha = remember { Animatable(1f) }

    LaunchedEffect(uiState.isAllAppsOpen, isSettingsOpen) {
        val isOverlayOpen = uiState.isAllAppsOpen || isSettingsOpen
        if (isOverlayOpen) {
            launch {
                homeScale.animateTo(
                    targetValue = 0.94f,
                    animationSpec = FluidSpringPhysics.fluidSpring()
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
                    animationSpec = FluidSpringPhysics.criticallyDampedSpring(stiffness = 500f)
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
                            viewModel.openAllApps(activeNotificationPkgs)
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
                if (!actionTriggeredInScroll && !uiState.openDrawerFromDockOnly && !uiState.isAllAppsOpen) {
                    // Velocity handoff: quick upward flick directly triggers drawer opening
                    if (available.y < -750f) {
                        viewModel.openAllApps(activeNotificationPkgs)
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
                                            viewModel.openAllApps(activeNotificationPkgs)
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
                // ponytail: rightward swipe on home screen (page 1) consumes edge overscroll immediately to prevent stretchy rubber-band effect; ceiling: full edge overscroll suppression; upgrade path: customizable edge bounce physics.
                if (source == NestedScrollSource.UserInput && pagerState.currentPage == 1 && available.x < 0f) {
                    return Offset(available.x, 0f)
                }
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
        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(
            LocalOverscrollFactory provides null,
            LocalOverscrollConfiguration provides null
        ) {
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
                        drawerTitleAnimation = uiState.drawerTitleAnimation,
                        surfaceThemeStyle = uiState.surfaceThemeStyle,
                        isMesmerizingGlow = uiState.dockMesmerizingGlow,
                        surfaceOpacity = uiState.surfaceOpacity,
                        surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
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
        }
    } else {
        homeContent()
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
            surfaceCornerRadiusDp = uiState.surfaceCornerRadius,
            autoOpenKeyboard = uiState.autoOpenKeyboardInDrawer,
            blurAppListOnOpen = uiState.blurAppListOnOpen,
            onOpenSettings = onDrawerOpenSettings,
            suggestedAction = suggestedAction,
            onExecuteAction = { action ->
                try {
                    action.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(action.intent)
                } catch (_: Exception) {}
                viewModel.closeAllApps()
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
                onHideFromSuggestions = { viewModel.hideFromSuggestions(selectedApp.packageName) }
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

private fun toggleTorch(context: android.content.Context, enable: Boolean? = null): Boolean {
    return try {
        val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as? CameraManager ?: return false
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        val turnOn = enable ?: true
        cameraManager.setTorchMode(cameraId, turnOn)
        true
    } catch (_: Exception) {
        false
    }
}

private fun executeLauncherAction(
    context: android.content.Context,
    action: LauncherAction,
    toolCall: ToolCallResult?,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
): Boolean {
    // 1. Timer specialized execution with OEM fallbacks
    if (toolCall?.toolName == "set_timer") {
        val seconds = toolCall.getInt("seconds") ?: 60
        val label = toolCall.getString("label")
        val success = launchTimerWithFallbacks(context, seconds, label)
        return if (success) {
            onSuccess(action.title)
            true
        } else {
            onError("No clock or timer app available")
            false
        }
    }

    // 2. Alarm specialized execution with OEM fallbacks
    if (toolCall?.toolName == "set_alarm") {
        val hour = toolCall.getInt("hour") ?: 7
        val minute = toolCall.getInt("minute") ?: 0
        val message = toolCall.getString("message")
        val success = launchAlarmWithFallbacks(context, hour, minute, message)
        return if (success) {
            onSuccess(action.title)
            true
        } else {
            onError("No clock app available")
            false
        }
    }

    // 3. General action execution
    return try {
        action.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(action.intent)
        onSuccess(action.title)
        true
    } catch (e: ActivityNotFoundException) {
        Log.e("HomeScreen", "ActivityNotFoundException executing action: ${action.title}", e)
        onError("No app available for ${action.title}")
        false
    } catch (e: SecurityException) {
        Log.e("HomeScreen", "SecurityException executing action: ${action.title}", e)
        onError("Permission required for ${action.title}")
        false
    } catch (e: Exception) {
        Log.e("HomeScreen", "Exception executing action: ${action.title}", e)
        onError("Failed to execute: ${action.title}")
        false
    }
}

private fun launchTimerWithFallbacks(context: android.content.Context, seconds: Int, label: String?): Boolean {
    // 1. Direct AlarmClock.ACTION_SET_TIMER
    try {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            if (!label.isNullOrBlank()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    } catch (e: Exception) {
        Log.w("HomeScreen", "AlarmClock.ACTION_SET_TIMER failed (${e.javaClass.simpleName}): ${e.message}")
    }

    // 2. AlarmClock.ACTION_SHOW_TIMERS
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_TIMERS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    } catch (e: Exception) {
        Log.w("HomeScreen", "AlarmClock.ACTION_SHOW_TIMERS failed: ${e.message}")
    }

    // 3. Fallback to OEM Clock package
    return launchClockPackageFallback(context)
}

private fun launchAlarmWithFallbacks(context: android.content.Context, hour: Int, minute: Int, message: String?): Boolean {
    // 1. Direct AlarmClock.ACTION_SET_ALARM
    try {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            if (!message.isNullOrBlank()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
            }
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    } catch (e: Exception) {
        Log.w("HomeScreen", "AlarmClock.ACTION_SET_ALARM failed (${e.javaClass.simpleName}): ${e.message}")
    }

    // 2. AlarmClock.ACTION_SHOW_ALARMS
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    } catch (e: Exception) {
        Log.w("HomeScreen", "AlarmClock.ACTION_SHOW_ALARMS failed: ${e.message}")
    }

    // 3. Fallback to OEM Clock package
    return launchClockPackageFallback(context)
}

private fun launchClockPackageFallback(context: android.content.Context): Boolean {
    val pm = context.packageManager
    val clockPackages = listOf(
        "com.google.android.deskclock",      // Pixel, Motorola, AOSP
        "com.sec.android.app.clockpackage",  // Samsung One UI
        "com.coloros.alarmclock",            // OPPO, OnePlus, Realme (ColorOS / OxygenOS)
        "com.vivo.timer",                    // Vivo, iQOO
        "com.android.deskclock",             // Xiaomi HyperOS / MIUI / Generic AOSP
        "com.oneplus.deskclock",             // OnePlus legacy
        "com.miui.deskclock",                // Xiaomi MIUI legacy
        "com.huawei.deskclock",              // Huawei / Honor
        "com.asus.deskclock",                // ASUS
        "com.motorola.blur.alarmclock"       // Motorola legacy
    )
    for (pkg in clockPackages) {
        try {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        } catch (_: Exception) {}
    }
    return false
}
