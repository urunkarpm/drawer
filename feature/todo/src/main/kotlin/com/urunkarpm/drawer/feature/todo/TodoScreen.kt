package com.urunkarpm.drawer.feature.todo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.feature.todo.component.AddTodoBottomSheet
import com.urunkarpm.drawer.feature.todo.component.DoneListPage
import com.urunkarpm.drawer.feature.todo.component.PlacePickerBottomSheet
import com.urunkarpm.drawer.feature.todo.component.TodoFilterChips
import com.urunkarpm.drawer.feature.todo.component.TodoHeroFocusCard
import com.urunkarpm.drawer.feature.todo.component.TodoItemRow
import com.urunkarpm.drawer.feature.todo.component.TodoStatsPage

@Composable
fun TodoScreen(
    onOpenPlacePicker: (() -> Unit)? = null,
    onOpenHealthScreen: (() -> Unit)? = null,
    isWallpaperBlurActive: Boolean = false,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Reset stats filter to the normal to-do list whenever navigating away from to-do page
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            viewModel.resetFilterIfStats()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetFilterIfStats()
        }
    }

    // Pressing back while viewing stats returns to the to-do list
    BackHandler(enabled = uiState.filter == TodoFilter.STATS) {
        viewModel.setFilter(TodoFilter.ALL)
    }

    val crispHaptics = LocalCrispHaptics.current
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TodoItem?>(null) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showHealthScreen by remember { mutableStateOf(false) }
    var pickedPlaceName by remember { mutableStateOf<String?>(null) }
    var pickedLat by remember { mutableStateOf<Double?>(null) }
    var pickedLon by remember { mutableStateOf<Double?>(null) }
    var pickedRadius by remember { mutableFloatStateOf(200f) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val horizontalPadding = if (screenWidth < 360) 12.dp else if (screenWidth >= 840) 24.dp else 16.dp

    val snackbarHostState = remember { SnackbarHostState() }

    // Fast O(1) ID lookups for jank-free LazyColumn rendering
    val staleTaskIds = remember(uiState.staleTasks) { uiState.staleTasks.map { it.id }.toSet() }
    val nearbyNowTaskIds = remember(uiState.nearbyNowTasks) { uiState.nearbyNowTasks.map { it.id }.toSet() }

    // Handle 5-second undo snackbar
    LaunchedEffect(uiState.undoAction) {
        val action = uiState.undoAction
        if (action != null) {
            val message = when (action.type) {
                UndoType.COMPLETED -> "Task completed"
                UndoType.DELETED -> "Task deleted"
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            } else {
                viewModel.dismissUndo()
            }
        }
    }

    if (showHealthScreen) {
        com.urunkarpm.drawer.feature.todo.health.ReminderHealthScreen(
            viewModel = viewModel,
            onBack = { showHealthScreen = false }
        )
        return
    }

    if (showPlacePicker) {
        PlacePickerBottomSheet(
            viewModel = viewModel,
            initialPlaceName = pickedPlaceName ?: editingTask?.placeName,
            initialLat = pickedLat ?: editingTask?.latitude,
            initialLon = pickedLon ?: editingTask?.longitude,
            initialRadiusM = if (pickedLat != null) pickedRadius else (editingTask?.radiusM ?: 200f),
            onConfirm = { name, lat, lon, rad, _ ->
                pickedPlaceName = name
                pickedLat = lat
                pickedLon = lon
                pickedRadius = rad
                showPlacePicker = false
            },
            onDismiss = { showPlacePicker = false }
        )
    }

    if (showAddSheet || editingTask != null) {
        AddTodoBottomSheet(
            editingTodo = editingTask,
            onDismiss = {
                showAddSheet = false
                editingTask = null
                pickedPlaceName = null
                pickedLat = null
                pickedLon = null
                pickedRadius = 200f
            },
            onAddTodo = { title, note, placeName, lat, lon, radius, deadlineUtc, deadlineZoneId, priority, repeatRule ->
                viewModel.createTask(
                    title = title,
                    note = note,
                    placeName = placeName,
                    latitude = lat,
                    longitude = lon,
                    radiusM = radius,
                    deadlineUtc = deadlineUtc,
                    deadlineZoneId = deadlineZoneId,
                    priority = priority,
                    repeatRule = repeatRule
                )
                showAddSheet = false
                pickedPlaceName = null
                pickedLat = null
                pickedLon = null
                pickedRadius = 200f
            },
            onUpdateTodo = { updatedTask ->
                viewModel.updateTodo(updatedTask)
                editingTask = null
                pickedPlaceName = null
                pickedLat = null
                pickedLon = null
                pickedRadius = 200f
            },
            onDeleteTodo = { taskToDelete ->
                viewModel.deleteTask(taskToDelete)
                editingTask = null
                pickedPlaceName = null
                pickedLat = null
                pickedLon = null
                pickedRadius = 200f
            },
            onPickPlaceClick = {
                showPlacePicker = true
                onOpenPlacePicker?.invoke()
            },
            presetPlaceName = pickedPlaceName ?: editingTask?.placeName,
            presetLatitude = pickedLat ?: editingTask?.latitude,
            presetLongitude = pickedLon ?: editingTask?.longitude,
            presetRadiusM = if (pickedLat != null) pickedRadius else (editingTask?.radiusM ?: 200f)
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Full-screen 50% frosted glass blur wash covering 100% of the display edge-to-edge
        // (including behind status bar, display cutout, and navigation bar/pill)
        if (!isWallpaperBlurActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.50f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                floatingActionButton = {
                    if (uiState.filter != TodoFilter.DONE && uiState.filter != TodoFilter.STATS) {
                        FloatingActionButton(
                            onClick = {
                                crispHaptics.click()
                                showAddSheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add task")
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "To-Do",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // If stale tasks exist, show a badge counter
                            if (uiState.staleTasks.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${uiState.staleTasks.size} stale",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    crispHaptics.click()
                                    showHealthScreen = true
                                    onOpenHealthScreen?.invoke()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Why no alert? Reminder health diagnostics",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Filter Chips
                    TodoFilterChips(
                        selectedFilter = uiState.filter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )

                    // Content Area: Open list vs Done list vs Stats
                    when (uiState.filter) {
                        TodoFilter.DONE -> {
                            DoneListPage(
                                doneTasks = uiState.doneTasks,
                                doneTodayCount = uiState.doneTodayCount,
                                onRestore = { viewModel.restoreTask(it) },
                                onDelete = { viewModel.deleteTask(it) },
                                onClearAll = { viewModel.clearAllDone() }
                            )
                        }
                        TodoFilter.STATS -> {
                            TodoStatsPage(
                                stats = uiState.stats,
                                openTasks = uiState.openTasks,
                                onCompleteTask = { viewModel.completeTask(it) }
                            )
                        }
                        else -> {
                            val now = remember { System.currentTimeMillis() }
                            val heroTask = remember(uiState.openTasks, uiState.filter, nearbyNowTaskIds) {
                                if (uiState.filter == TodoFilter.NEARBY) {
                                    uiState.openTasks.firstOrNull()
                                } else if (uiState.filter == TodoFilter.HIGH_PRIORITY) {
                                    uiState.openTasks.firstOrNull { it.priority == com.urunkarpm.drawer.core.model.TodoPriority.HIGH }
                                } else {
                                    uiState.openTasks.firstOrNull { nearbyNowTaskIds.contains(it.id) }
                                        ?: uiState.openTasks.firstOrNull()
                                }
                            }

                            if (uiState.openTasks.isEmpty() && uiState.nearbyNowTasks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(68.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = when (uiState.filter) {
                                                TodoFilter.NEARBY -> "No nearby tasks"
                                                TodoFilter.DUE_TODAY -> "Clear for today"
                                                TodoFilter.HIGH_PRIORITY -> "No high priority tasks"
                                                else -> "All caught up"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = when (uiState.filter) {
                                                TodoFilter.NEARBY -> "No tasks bound to places near you right now."
                                                TodoFilter.DUE_TODAY -> "Nothing due before midnight. Breathe easy."
                                                TodoFilter.HIGH_PRIORITY -> "No urgent high priority tasks. Breathe easy."
                                                else -> "Your mind is clear. Tap + to add a task."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )

                                        if (uiState.doneTodayCount > 0) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "${uiState.doneTodayCount} ${if (uiState.doneTodayCount == 1) "task" else "tasks"} completed today",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val remainingTasks = remember(uiState.openTasks, heroTask) {
                                    uiState.openTasks.filter { it.id != heroTask?.id }
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Hero Focus Card (single highest priority task anchor)
                                    if (heroTask != null) {
                                        item(key = "hero_focus_${heroTask.id}") {
                                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                                TodoHeroFocusCard(
                                                    task = heroTask,
                                                    isNearby = nearbyNowTaskIds.contains(heroTask.id),
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onClick = { editingTask = it },
                                                    onCyclePriority = { viewModel.cyclePriority(it) }
                                                )
                                            }
                                        }
                                    }

                                    // 2. Chrono-Stream Sections
                                    if (uiState.filter == TodoFilter.ALL) {
                                        // ponytail: chrono-flow stream partitioned in-memory via System.currentTimeMillis; ceiling: local partition without server sync; upgrade path: reactive database query with custom day-boundary triggers.
                                        val otherNearbyTasks = remainingTasks.filter { nearbyNowTaskIds.contains(it.id) }
                                        val otherNearbyIds = otherNearbyTasks.map { it.id }.toSet()
                                        val nonNearby = remainingTasks.filter { !otherNearbyIds.contains(it.id) }

                                        val todayAndOverdueTasks = nonNearby.filter { task ->
                                            val deadline = task.deadlineUtc
                                            deadline != null && deadline > 0 &&
                                                (TodoUtils.isOverdue(deadline, now) ||
                                                 TodoUtils.isDueToday(deadline, task.deadlineZoneId, now))
                                        }
                                        val todayAndOverdueIds = todayAndOverdueTasks.map { it.id }.toSet()

                                        val upcomingTasks = nonNearby.filter { task ->
                                            val deadline = task.deadlineUtc
                                            deadline != null && deadline > 0 &&
                                                !todayAndOverdueIds.contains(task.id)
                                        }
                                        val upcomingIds = upcomingTasks.map { it.id }.toSet()

                                        val anytimeTasks = nonNearby.filter { task ->
                                            !todayAndOverdueIds.contains(task.id) && !upcomingIds.contains(task.id)
                                        }

                                        // Nearby section (if any other nearby tasks beyond hero)
                                        if (otherNearbyTasks.isNotEmpty()) {
                                            item(key = "header_chrono_nearby") {
                                                ChronoSectionHeader(
                                                    title = "Nearby Now",
                                                    count = otherNearbyTasks.size,
                                                    icon = Icons.Outlined.LocationOn,
                                                    iconTint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            items(otherNearbyTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = true,
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }

                                        // Today & Overdue section
                                        if (todayAndOverdueTasks.isNotEmpty()) {
                                            item(key = "header_chrono_today") {
                                                ChronoSectionHeader(
                                                    title = "Today & Overdue",
                                                    count = todayAndOverdueTasks.size,
                                                    icon = Icons.Outlined.Schedule,
                                                    iconTint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            items(todayAndOverdueTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = nearbyNowTaskIds.contains(task.id),
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }

                                        // Upcoming section
                                        if (upcomingTasks.isNotEmpty()) {
                                            item(key = "header_chrono_upcoming") {
                                                ChronoSectionHeader(
                                                    title = "Upcoming",
                                                    count = upcomingTasks.size,
                                                    icon = Icons.Outlined.CalendarToday
                                                )
                                            }
                                            items(upcomingTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = nearbyNowTaskIds.contains(task.id),
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }

                                        // Anytime section
                                        if (anytimeTasks.isNotEmpty()) {
                                            item(key = "header_chrono_anytime") {
                                                ChronoSectionHeader(
                                                    title = "Anytime",
                                                    count = anytimeTasks.size,
                                                    icon = Icons.Outlined.Checklist
                                                )
                                            }
                                            items(anytimeTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = nearbyNowTaskIds.contains(task.id),
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }
                                    } else if (uiState.filter == TodoFilter.HIGH_PRIORITY) {
                                        if (remainingTasks.isNotEmpty()) {
                                            item(key = "header_chrono_high_priority") {
                                                ChronoSectionHeader(
                                                    title = "High Priority",
                                                    count = remainingTasks.size,
                                                    icon = Icons.Outlined.Checklist,
                                                    iconTint = Color(0xFFEF5350)
                                                )
                                            }
                                            items(remainingTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = nearbyNowTaskIds.contains(task.id),
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }
                                    } else if (uiState.filter == TodoFilter.DUE_TODAY) {
                                        if (remainingTasks.isNotEmpty()) {
                                            item(key = "header_chrono_duetoday") {
                                                ChronoSectionHeader(
                                                    title = "Due Today",
                                                    count = remainingTasks.size,
                                                    icon = Icons.Outlined.Schedule
                                                )
                                            }
                                            items(remainingTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = nearbyNowTaskIds.contains(task.id),
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }
                                    } else if (uiState.filter == TodoFilter.NEARBY) {
                                        if (remainingTasks.isNotEmpty()) {
                                            item(key = "header_chrono_nearby_only") {
                                                ChronoSectionHeader(
                                                    title = "Nearby Places",
                                                    count = remainingTasks.size,
                                                    icon = Icons.Outlined.LocationOn,
                                                    iconTint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            items(remainingTasks, key = { it.id }) { task ->
                                                TodoItemRow(
                                                    task = task,
                                                    isStale = staleTaskIds.contains(task.id),
                                                    isNearbyNow = true,
                                                    onComplete = { viewModel.completeTask(it) },
                                                    onDelete = { viewModel.deleteTask(it) },
                                                    onCyclePriority = { viewModel.cyclePriority(it) },
                                                    onClick = { editingTask = task }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChronoSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
