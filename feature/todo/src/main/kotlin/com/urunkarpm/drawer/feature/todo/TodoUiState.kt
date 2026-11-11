package com.urunkarpm.drawer.feature.todo

import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoSettings

enum class TodoFilter {
    ALL,
    NEARBY,
    DUE_TODAY,
    DONE
}

enum class UndoType {
    COMPLETED,
    DELETED
}

data class UndoAction(
    val task: TodoItem,
    val type: UndoType,
    val expiresAt: Long = System.currentTimeMillis() + 5000L
)

data class TodoUiState(
    val filter: TodoFilter = TodoFilter.ALL,
    val openTasks: List<TodoItem> = emptyList(),
    val doneTasks: List<TodoItem> = emptyList(),
    val nearbyNowTasks: List<TodoItem> = emptyList(),
    val staleTasks: List<TodoItem> = emptyList(),
    val doneTodayCount: Int = 0,
    val userLocation: Pair<Double, Double>? = null, // lat, lon
    val currentPlaceName: String? = null,
    val undoAction: UndoAction? = null,
    val settings: TodoSettings = TodoSettings(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
