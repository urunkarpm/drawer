package com.urunkarpm.drawer.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.common.util.Haversine
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.data.place.PlaceSearchService
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.PlaceSearchResult
import com.urunkarpm.drawer.core.model.RecentSearch
import com.urunkarpm.drawer.core.model.SavedPlace
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoSettings
import com.urunkarpm.drawer.core.model.TodoStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val placeSearchService: PlaceSearchService
) : ViewModel() {

    init {
        viewModelScope.launch {
            val settings = todoRepository.settings.first()
            if (settings.doneAutoDeleteDays > 0) {
                todoRepository.deleteOldDoneTasks(settings.doneAutoDeleteDays)
            }
        }
    }

    private val _filter = MutableStateFlow(TodoFilter.ALL)
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _undoAction = MutableStateFlow<UndoAction?>(null)
    private var undoDismissJob: Job? = null

    private data class FilterState(
        val filter: TodoFilter,
        val location: Pair<Double, Double>?,
        val undoAction: UndoAction?
    )

    private val filterStateFlow = combine(_filter, _userLocation, _undoAction) { filter, location, undoAction ->
        FilterState(filter, location, undoAction)
    }

    val uiState: StateFlow<TodoUiState> = combine(
        todoRepository.observeOpenTodos(),
        todoRepository.observeDoneTodos(),
        todoRepository.settings,
        filterStateFlow
    ) { openList: List<TodoItem>, doneList: List<TodoItem>, settings: TodoSettings, filterState: FilterState ->
        val (filter, location, undoAction) = filterState
        val now = System.currentTimeMillis()

        // Count done today
        val today = LocalDate.now()
        val doneTodayCount = doneList.count { done ->
            val completed = done.completedAt ?: 0L
            if (completed > 0) {
                val completedDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(completed), ZoneId.systemDefault()).toLocalDate()
                completedDate.isEqual(today)
            } else false
        }

        // Identify nearby now tasks
        val nearbyNowTasks = if (location != null && settings.locationFeaturesEnabled) {
            val (userLat, userLon) = location
            openList.filter { task ->
                val taskLat = task.latitude
                val taskLon = task.longitude
                if (taskLat != null && taskLon != null) {
                    Haversine.isInsideCircle(userLat, userLon, taskLat, taskLon, task.radiusM)
                } else false
            }
        } else emptyList()

        // Identify stale tasks
        val staleTasks = openList.filter { TodoUtils.isStale(it, now) }

        // Filter and sort open tasks
        val filteredList = when (filter) {
            TodoFilter.ALL, TodoFilter.STATS -> openList
            TodoFilter.NEARBY -> {
                if (location != null) {
                    val (uLat, uLon) = location
                    openList.filter { it.latitude != null && it.longitude != null }
                        .sortedBy { Haversine.distanceMeters(uLat, uLon, it.latitude!!, it.longitude!!) }
                } else {
                    openList.filter { it.latitude != null && it.longitude != null }
                }
            }
            TodoFilter.DUE_TODAY -> openList.filter { TodoUtils.isDueToday(it.deadlineUtc, it.deadlineZoneId, now) }
            TodoFilter.DONE -> emptyList()
        }

        // Rule 4: Put stale tasks and tasks at current place at top
        val sortedOpenTasks = if (filter != TodoFilter.DONE) {
            filteredList.sortedWith(
                compareByDescending<TodoItem> { nearbyNowTasks.contains(it) }
                    .thenByDescending { staleTasks.contains(it) }
                    .thenByDescending { it.priority == TodoPriority.HIGH }
                    .thenBy { it.deadlineUtc ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
        } else emptyList()

        val stats = TodoStatsCalculator.calculate(openList, doneList, now)

        TodoUiState(
            filter = filter,
            openTasks = sortedOpenTasks,
            doneTasks = doneList,
            nearbyNowTasks = nearbyNowTasks,
            staleTasks = staleTasks,
            doneTodayCount = doneTodayCount,
            userLocation = location,
            undoAction = undoAction,
            settings = settings,
            stats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoUiState(isLoading = true)
    )

    val savedPlaces: StateFlow<List<SavedPlace>> = todoRepository.observeSavedPlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<RecentSearch>> = todoRepository.observeRecentSearches(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEventLogs: StateFlow<List<com.urunkarpm.drawer.core.model.EventLog>> = todoRepository.observeRecentEventLogs(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchPlaces(query: String, onResult: (List<PlaceSearchResult>, Boolean) -> Unit) {
        viewModelScope.launch {
            val result = placeSearchService.search(query)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                onResult(list, false)
                if (list.isNotEmpty()) {
                    todoRepository.addRecentSearch(query.trim())
                }
            } else {
                onResult(emptyList(), true)
            }
        }
    }

    fun reverseGeocode(lat: Double, lon: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = placeSearchService.reverseGeocode(lat, lon)
            onResult(result.getOrDefault("Selected point"))
        }
    }

    fun savePlace(place: SavedPlace) {
        viewModelScope.launch {
            todoRepository.savePlace(place)
        }
    }

    fun deleteSavedPlace(id: Long) {
        viewModelScope.launch {
            todoRepository.deletePlace(id)
        }
    }

    fun setFilter(filter: TodoFilter) {
        _filter.value = filter
    }

    fun resetFilterIfStats() {
        if (_filter.value == TodoFilter.STATS) {
            _filter.value = TodoFilter.ALL
        }
    }

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLocation.value = Pair(lat, lon)
    }

    /**
     * Single function to complete a task. Used by checkbox, swipe, and notification actions.
     */
    fun completeTask(todo: TodoItem) {
        viewModelScope.launch {
            val completed = todoRepository.completeTask(todo.id)
            if (completed != null) {
                scheduleUndo(UndoAction(task = todo, type = UndoType.COMPLETED))
            }
        }
    }

    fun deleteTask(todo: TodoItem) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo.id)
            scheduleUndo(UndoAction(task = todo, type = UndoType.DELETED))
        }
    }

    fun restoreTask(todo: TodoItem) {
        viewModelScope.launch {
            todoRepository.restoreTask(todo.id)
        }
    }

    fun undo() {
        val current = _undoAction.value ?: return
        undoDismissJob?.cancel()
        _undoAction.value = null

        viewModelScope.launch {
            when (current.type) {
                UndoType.COMPLETED -> {
                    todoRepository.restoreTask(current.task.id)
                }
                UndoType.DELETED -> {
                    // Recreate
                    todoRepository.createTodo(current.task.copy(id = 0L))
                }
            }
        }
    }

    fun dismissUndo() {
        undoDismissJob?.cancel()
        _undoAction.value = null
    }

    private fun scheduleUndo(action: UndoAction) {
        undoDismissJob?.cancel()
        _undoAction.value = action
        undoDismissJob = viewModelScope.launch {
            delay(5000L) // 5 seconds
            _undoAction.value = null
        }
    }

    fun clearAllDone() {
        viewModelScope.launch {
            todoRepository.clearAllDone()
        }
    }

    fun createTask(
        title: String,
        note: String? = null,
        placeName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusM: Float = 200f,
        deadlineUtc: Long? = null,
        deadlineZoneId: String? = null,
        priority: TodoPriority = TodoPriority.NORMAL,
        repeatRule: String? = null
    ) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val item = TodoItem(
                title = trimmed,
                note = note?.trim()?.takeIf { it.isNotBlank() },
                placeName = placeName?.trim()?.takeIf { it.isNotBlank() },
                latitude = latitude,
                longitude = longitude,
                radiusM = radiusM,
                deadlineUtc = deadlineUtc,
                deadlineZoneId = deadlineZoneId ?: ZoneId.systemDefault().id,
                priority = priority,
                status = TodoStatus.OPEN,
                repeatRule = repeatRule?.trim()?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis()
            )
            todoRepository.createTodo(item)
        }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            todoRepository.updateTodo(todo)
        }
    }

    fun updateSettings(settings: TodoSettings) {
        viewModelScope.launch {
            todoRepository.updateSettings(settings)
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            todoRepository.setLocationEnabled(enabled)
        }
    }

    fun snoozeAllLocationAlerts(until: Long) {
        viewModelScope.launch {
            todoRepository.snoozeAllLocationAlerts(until)
        }
    }
}
