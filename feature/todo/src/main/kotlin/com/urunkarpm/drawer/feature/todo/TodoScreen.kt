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
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.feature.todo.component.AddTodoBottomSheet
import com.urunkarpm.drawer.feature.todo.component.DoneListPage
import com.urunkarpm.drawer.feature.todo.component.PlacePickerBottomSheet
import com.urunkarpm.drawer.feature.todo.component.TodoFilterChips
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
                        if (uiState.openTasks.isEmpty() && uiState.nearbyNowTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = when (uiState.filter) {
                                            TodoFilter.NEARBY -> "No tasks near your location"
                                            TodoFilter.DUE_TODAY -> "No tasks due today"
                                            else -> "All tasks completed!"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tap + to add a new task",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // "Nearby now" section for open tasks inside current place
                                if (uiState.nearbyNowTasks.isNotEmpty() && uiState.filter != TodoFilter.NEARBY) {
                                    item(key = "header_nearby_now") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Nearby now",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    items(uiState.nearbyNowTasks, key = { "nearby_${it.id}" }) { task ->
                                        TodoItemRow(
                                            task = task,
                                            isStale = staleTaskIds.contains(task.id),
                                            isNearbyNow = true,
                                            onComplete = { viewModel.completeTask(it) },
                                            onDelete = { viewModel.deleteTask(it) },
                                            onClick = { editingTask = task }
                                        )
                                    }

                                    item(key = "divider_after_nearby") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }

                                // Open tasks list (excluding those already shown in nearby now if applicable, or showing all sorted)
                                val remainingTasks = if (uiState.nearbyNowTasks.isNotEmpty() && uiState.filter != TodoFilter.NEARBY) {
                                    uiState.openTasks.filter { !nearbyNowTaskIds.contains(it.id) }
                                } else {
                                    uiState.openTasks
                                }

                                items(remainingTasks, key = { it.id }) { task ->
                                    TodoItemRow(
                                        task = task,
                                        isStale = staleTaskIds.contains(task.id),
                                        isNearbyNow = nearbyNowTaskIds.contains(task.id),
                                        onComplete = { viewModel.completeTask(it) },
                                        onDelete = { viewModel.deleteTask(it) },
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
