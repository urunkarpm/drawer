package com.urunkarpm.drawer.feature.todo

import com.urunkarpm.drawer.core.data.place.PlaceSearchService
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.model.PlaceSearchResult
import com.urunkarpm.drawer.core.model.SavedPlace
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoPriority
import com.urunkarpm.drawer.core.model.TodoSettings
import com.urunkarpm.drawer.core.model.TodoStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: TodoRepository = mockk(relaxed = true)
    private val placeSearchService: PlaceSearchService = mockk(relaxed = true)

    private val openTodosFlow = MutableStateFlow<List<TodoItem>>(emptyList())
    private val doneTodosFlow = MutableStateFlow<List<TodoItem>>(emptyList())
    private val settingsFlow = MutableStateFlow(TodoSettings())
    private val customTodoOrderFlow = MutableStateFlow<List<Long>>(emptyList())

    private lateinit var viewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeOpenTodos() } returns openTodosFlow
        every { repository.observeDoneTodos() } returns doneTodosFlow
        every { repository.settings } returns settingsFlow
        every { repository.customTodoOrder } returns customTodoOrderFlow
        every { repository.observeSavedPlaces() } returns MutableStateFlow(emptyList())
        every { repository.observeRecentSearches(any()) } returns MutableStateFlow(emptyList())

        viewModel = TodoViewModel(repository, placeSearchService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completeTask_callsRepositoryAndSetsUndo() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val task = TodoItem(id = 1L, title = "Task 1", status = TodoStatus.OPEN)
        coEvery { repository.completeTask(1L, any()) } returns task.copy(status = TodoStatus.DONE)

        viewModel.completeTask(task)
        testScheduler.runCurrent()

        coVerify { repository.completeTask(1L, any()) }
        val undo = viewModel.uiState.value.undoAction
        assertNotNull(undo)
        assertEquals(UndoType.COMPLETED, undo?.type)
        assertEquals(1L, undo?.task?.id)
    }

    @Test
    fun undo_restoresCompletedTask() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val task = TodoItem(id = 1L, title = "Task 1", status = TodoStatus.OPEN)
        coEvery { repository.completeTask(1L, any()) } returns task.copy(status = TodoStatus.DONE)

        viewModel.completeTask(task)
        testScheduler.runCurrent()

        viewModel.undo()
        testScheduler.runCurrent()

        coVerify { repository.restoreTask(1L) }
        assertEquals(null, viewModel.uiState.value.undoAction)
    }

    @Test
    fun setFilter_updatesStateFilter() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        viewModel.setFilter(TodoFilter.DUE_TODAY)
        testScheduler.runCurrent()

        assertEquals(TodoFilter.DUE_TODAY, viewModel.uiState.value.filter)
    }

    @Test
    fun cyclePriority_cyclesThroughNormalHighLow() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val task = TodoItem(id = 1L, title = "Task", priority = TodoPriority.NORMAL, status = TodoStatus.OPEN)
        viewModel.cyclePriority(task)
        testScheduler.runCurrent()
        coVerify { repository.updateTodo(match { it.priority == TodoPriority.HIGH }) }

        val highTask = task.copy(priority = TodoPriority.HIGH)
        viewModel.cyclePriority(highTask)
        testScheduler.runCurrent()
        coVerify { repository.updateTodo(match { it.priority == TodoPriority.LOW }) }

        val lowTask = task.copy(priority = TodoPriority.LOW)
        viewModel.cyclePriority(lowTask)
        testScheduler.runCurrent()
        coVerify { repository.updateTodo(match { it.priority == TodoPriority.NORMAL }) }
    }

    @Test
    fun highPriorityFilter_filtersOnlyHighPriorityTasks() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val normalTask = TodoItem(id = 1L, title = "Normal", priority = TodoPriority.NORMAL, status = TodoStatus.OPEN)
        val highTask = TodoItem(id = 2L, title = "High", priority = TodoPriority.HIGH, status = TodoStatus.OPEN)
        openTodosFlow.value = listOf(normalTask, highTask)
        testScheduler.runCurrent()

        viewModel.setFilter(TodoFilter.HIGH_PRIORITY)
        testScheduler.runCurrent()

        val openTasks = viewModel.uiState.value.openTasks
        assertEquals(1, openTasks.size)
        assertEquals(2L, openTasks[0].id)
    }

    @Test
    fun sorting_placesStaleAndNearbyAtTop() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val now = System.currentTimeMillis()
        val normalTask = TodoItem(id = 1L, title = "Normal", status = TodoStatus.OPEN, createdAt = now)
        val staleTask = TodoItem(id = 2L, title = "Stale", status = TodoStatus.OPEN, deadlineUtc = now - 50000L, createdAt = now)
        val highTask = TodoItem(id = 3L, title = "High", priority = TodoPriority.HIGH, status = TodoStatus.OPEN, deadlineUtc = now + 100000L, createdAt = now)

        openTodosFlow.value = listOf(normalTask, highTask, staleTask)
        testScheduler.runCurrent()

        val sorted = viewModel.uiState.value.openTasks
        assertEquals(3, sorted.size)
        // Stale task should be first
        assertEquals(2L, sorted[0].id)
        // High priority next
        assertEquals(3L, sorted[1].id)
        // Normal last
        assertEquals(1L, sorted[2].id)
    }

    @Test
    fun uiState_computesStatsAccurately() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val now = System.currentTimeMillis()
        val openTask = TodoItem(id = 1L, title = "Open Task", status = TodoStatus.OPEN, createdAt = now)
        val doneTask = TodoItem(id = 2L, title = "Done Task", status = TodoStatus.DONE, completedAt = now)

        openTodosFlow.value = listOf(openTask)
        doneTodosFlow.value = listOf(doneTask)
        testScheduler.runCurrent()

        val stats = viewModel.uiState.value.stats
        assertEquals(2, stats.totalAdded)
        assertEquals(1, stats.completedCount)
        assertEquals(1, stats.pendingOnTrackCount)
        assertEquals(0, stats.overdueOrStaleCount)
    }

    @Test
    fun resetFilterIfStats_resetsToAllOnlyWhenStats() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        viewModel.setFilter(TodoFilter.STATS)
        testScheduler.runCurrent()
        assertEquals(TodoFilter.STATS, viewModel.uiState.value.filter)

        viewModel.resetFilterIfStats()
        testScheduler.runCurrent()
        assertEquals(TodoFilter.ALL, viewModel.uiState.value.filter)

        // Does not reset non-stats filters
        viewModel.setFilter(TodoFilter.NEARBY)
        testScheduler.runCurrent()
        assertEquals(TodoFilter.NEARBY, viewModel.uiState.value.filter)

        viewModel.resetFilterIfStats()
        testScheduler.runCurrent()
        assertEquals(TodoFilter.NEARBY, viewModel.uiState.value.filter)
    }

    @Test
    fun reorderTasks_movesTaskUpAndDownCorrectly() {
        val tasks = listOf(
            TodoItem(id = 1L, title = "Task 1", status = TodoStatus.OPEN),
            TodoItem(id = 2L, title = "Task 2", status = TodoStatus.OPEN),
            TodoItem(id = 3L, title = "Task 3", status = TodoStatus.OPEN)
        )

        // Move 1 down to 2
        val movedDown = reorderTasks(tasks, fromId = 1L, toId = 2L)
        assertEquals(listOf(2L, 1L, 3L), movedDown)

        // Move 3 up to 2
        val movedUp = reorderTasks(tasks, fromId = 3L, toId = 2L)
        assertEquals(listOf(1L, 3L, 2L), movedUp)

        // Invalid ID returns original order
        val unchanged = reorderTasks(tasks, fromId = 99L, toId = 2L)
        assertEquals(listOf(1L, 2L, 3L), unchanged)
    }

    @Test
    fun moveTask_persistsNewOrderToRepository() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val task1 = TodoItem(id = 1L, title = "Task 1", status = TodoStatus.OPEN)
        val task2 = TodoItem(id = 2L, title = "Task 2", status = TodoStatus.OPEN)
        val task3 = TodoItem(id = 3L, title = "Task 3", status = TodoStatus.OPEN)
        openTodosFlow.value = listOf(task1, task2, task3)
        testScheduler.runCurrent()

        viewModel.moveTask(fromId = 1L, toId = 2L)
        testScheduler.runCurrent()

        coVerify { repository.updateCustomTodoOrder(listOf(2L, 1L, 3L)) }
    }

    @Test
    fun customTodoOrder_respectsSavedOrderInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testScheduler.runCurrent()

        val task1 = TodoItem(id = 1L, title = "Task 1", status = TodoStatus.OPEN)
        val task2 = TodoItem(id = 2L, title = "Task 2", status = TodoStatus.OPEN)
        val task3 = TodoItem(id = 3L, title = "Task 3", status = TodoStatus.OPEN)
        openTodosFlow.value = listOf(task1, task2, task3)
        customTodoOrderFlow.value = listOf(3L, 1L, 2L)
        testScheduler.runCurrent()

        val tasks = viewModel.uiState.value.openTasks
        assertEquals(3, tasks.size)
        assertEquals(3L, tasks[0].id)
        assertEquals(1L, tasks[1].id)
        assertEquals(2L, tasks[2].id)
    }
}
