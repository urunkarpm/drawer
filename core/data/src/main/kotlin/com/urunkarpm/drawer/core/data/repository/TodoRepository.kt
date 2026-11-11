package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.database.dao.EventLogDao
import com.urunkarpm.drawer.core.database.dao.RecentSearchDao
import com.urunkarpm.drawer.core.database.dao.SavedPlaceDao
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.entity.EventLogEntity
import com.urunkarpm.drawer.core.database.entity.RecentSearchEntity
import com.urunkarpm.drawer.core.database.entity.SavedPlaceEntity
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.EventLog
import com.urunkarpm.drawer.core.model.RecentSearch
import com.urunkarpm.drawer.core.model.SavedPlace
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoSettings
import com.urunkarpm.drawer.core.model.TodoStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

interface TodoRepository {
    fun observeOpenTodos(): Flow<List<TodoItem>>
    fun observeDoneTodos(): Flow<List<TodoItem>>
    fun observeAllTodos(): Flow<List<TodoItem>>
    suspend fun getTodoById(id: Long): TodoItem?
    suspend fun createTodo(todo: TodoItem): Long
    suspend fun updateTodo(todo: TodoItem)
    suspend fun deleteTodo(id: Long)
    suspend fun completeTask(id: Long, now: Long = System.currentTimeMillis()): TodoItem?
    suspend fun restoreTask(id: Long)
    suspend fun clearAllDone(): Int
    suspend fun deleteOldDoneTasks(days: Int? = null): Int

    // Settings
    val settings: Flow<TodoSettings>
    suspend fun updateSettings(settings: TodoSettings)
    suspend fun setLocationEnabled(enabled: Boolean)
    suspend fun snoozeAllLocationAlerts(until: Long)
    val locationAlertsSnoozedUntil: Flow<Long>

    // Saved places
    fun observeSavedPlaces(): Flow<List<SavedPlace>>
    suspend fun getSavedPlacesList(): List<SavedPlace>
    suspend fun savePlace(place: SavedPlace): Long
    suspend fun deletePlace(id: Long)

    // Recent searches
    fun observeRecentSearches(limit: Int = 5): Flow<List<RecentSearch>>
    suspend fun addRecentSearch(query: String)

    // Event logs
    fun observeRecentEventLogs(limit: Int = 20): Flow<List<EventLog>>
    suspend fun logEvent(type: String, text: String): Long
    suspend fun pruneOldEventLogs()
}

@Singleton
class DefaultTodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val savedPlaceDao: SavedPlaceDao,
    private val recentSearchDao: RecentSearchDao,
    private val eventLogDao: EventLogDao,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val deadlineAlarmScheduler: com.urunkarpm.drawer.core.data.reminder.DeadlineAlarmScheduler,
    private val deadlineNotificationHelper: com.urunkarpm.drawer.core.data.reminder.DeadlineNotificationHelper,
    private val locationEngineManager: javax.inject.Provider<com.urunkarpm.drawer.core.data.location.LocationEngineManager>
) : TodoRepository {

    override fun observeOpenTodos(): Flow<List<TodoItem>> =
        todoDao.getOpenTodos().map { list -> list.map { it.toDomain() } }

    override fun observeDoneTodos(): Flow<List<TodoItem>> =
        todoDao.getDoneTodos().map { list -> list.map { it.toDomain() } }

    override fun observeAllTodos(): Flow<List<TodoItem>> =
        todoDao.getAllTodos().map { list -> list.map { it.toDomain() } }

    override suspend fun getTodoById(id: Long): TodoItem? =
        todoDao.getTodoById(id)?.toDomain()

    override suspend fun createTodo(todo: TodoItem): Long {
        val entity = TodoEntity.fromDomain(todo)
        val id = todoDao.insert(entity)
        val saved = todo.copy(id = id)
        deadlineAlarmScheduler.scheduleAlarms(saved)
        locationEngineManager.get().updateTargetsAndEvaluate()
        logEvent("TODO_CREATED", "Created task: ${todo.title} (id: $id)")
        return id
    }

    override suspend fun updateTodo(todo: TodoItem) {
        todoDao.update(TodoEntity.fromDomain(todo))
        deadlineAlarmScheduler.cancelAlarms(todo.id)
        if (todo.status == TodoStatus.OPEN && todo.deadlineUtc != null) {
            deadlineAlarmScheduler.scheduleAlarms(todo)
        }
        locationEngineManager.get().updateTargetsAndEvaluate()
        logEvent("TODO_UPDATED", "Updated task: ${todo.title} (id: ${todo.id})")
    }

    override suspend fun deleteTodo(id: Long) {
        val existing = todoDao.getTodoById(id)
        if (existing != null) {
            todoDao.deleteById(id)
            deadlineAlarmScheduler.cancelAlarms(id)
            deadlineNotificationHelper.cancelDeadlineNotification(id)
            locationEngineManager.get().updateTargetsAndEvaluate()
            logEvent("TODO_DELETED", "Deleted task: ${existing.title} (id: $id)")
        }
    }

    override suspend fun completeTask(id: Long, now: Long): TodoItem? {
        val existing = todoDao.getTodoById(id) ?: return null
        if (existing.status == TodoStatus.DONE.name) return existing.toDomain()

        // 1. Mark DONE
        todoDao.updateStatus(id, TodoStatus.DONE.name, now)
        deadlineAlarmScheduler.cancelAlarms(id)
        deadlineNotificationHelper.cancelDeadlineNotification(id)
        locationEngineManager.get().updateTargetsAndEvaluate()
        logEvent("TODO_COMPLETED", "Completed task: ${existing.title} (id: $id)")

        // 2. If repeat_rule is set, create the next recurring task
        val rule = existing.repeatRule?.trim()
        if (!rule.isNullOrBlank()) {
            val nextDeadline = calculateNextDeadline(existing.deadlineUtc, existing.deadlineZoneId, rule, now)
            val nextTask = TodoItem(
                id = 0L,
                title = existing.title,
                note = existing.note,
                placeName = existing.placeName,
                latitude = existing.latitude,
                longitude = existing.longitude,
                radiusM = existing.radiusM,
                deadlineUtc = nextDeadline,
                deadlineZoneId = existing.deadlineZoneId,
                priority = try { com.urunkarpm.drawer.core.model.TodoPriority.valueOf(existing.priority) } catch (_: Exception) { com.urunkarpm.drawer.core.model.TodoPriority.NORMAL },
                status = TodoStatus.OPEN,
                repeatRule = existing.repeatRule,
                createdAt = now
            )
            val newId = todoDao.insert(TodoEntity.fromDomain(nextTask))
            val savedNext = nextTask.copy(id = newId)
            deadlineAlarmScheduler.scheduleAlarms(savedNext)
            logEvent("TODO_REPEATED", "Scheduled next recurring task: ${nextTask.title} (id: $newId)")
        }

        return todoDao.getTodoById(id)?.toDomain()
    }

    override suspend fun restoreTask(id: Long) {
        val existing = todoDao.getTodoById(id) ?: return
        val restored = existing.copy(
            status = TodoStatus.OPEN.name,
            completedAt = null,
            staleSince = null,
            lastNudgeAt = null,
            nudgeCount = 0
        )
        todoDao.update(restored)
        deadlineAlarmScheduler.scheduleAlarms(restored.toDomain())
        locationEngineManager.get().updateTargetsAndEvaluate()
        logEvent("TODO_RESTORED", "Restored task: ${existing.title} (id: $id)")
    }

    override suspend fun clearAllDone(): Int {
        val deleted = todoDao.clearAllDone()
        logEvent("TODO_CLEARED_ALL_DONE", "Cleared $deleted completed tasks")
        return deleted
    }

    override suspend fun deleteOldDoneTasks(days: Int?): Int {
        val autoDeleteDays = days ?: preferencesDataSource.todoSettings.first().doneAutoDeleteDays
        if (autoDeleteDays <= 0) return 0 // <=0 means Never

        val cutoff = System.currentTimeMillis() - (autoDeleteDays.toLong() * 24L * 60L * 60L * 1000L)
        val deleted = todoDao.deleteOldDone(cutoff)
        if (deleted > 0) {
            logEvent("TODO_AUTO_PRUNED", "Pruned $deleted completed tasks older than $autoDeleteDays days")
        }
        return deleted
    }

    override val settings: Flow<TodoSettings> = preferencesDataSource.todoSettings

    override suspend fun updateSettings(settings: TodoSettings) {
        preferencesDataSource.updateTodoSettings(settings)
    }

    override suspend fun setLocationEnabled(enabled: Boolean) {
        preferencesDataSource.setTodoLocationFeaturesEnabled(enabled)
        locationEngineManager.get().updateTargetsAndEvaluate()
    }

    override suspend fun snoozeAllLocationAlerts(until: Long) {
        preferencesDataSource.snoozeAllLocationAlerts(until)
    }

    override val locationAlertsSnoozedUntil: Flow<Long> =
        preferencesDataSource.todoLocationAlertsSnoozedUntil

    override fun observeSavedPlaces(): Flow<List<SavedPlace>> =
        savedPlaceDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getSavedPlacesList(): List<SavedPlace> =
        savedPlaceDao.getAllList().map { it.toDomain() }

    override suspend fun savePlace(place: SavedPlace): Long =
        savedPlaceDao.insert(SavedPlaceEntity.fromDomain(place))

    override suspend fun deletePlace(id: Long) =
        savedPlaceDao.deleteById(id)

    override fun observeRecentSearches(limit: Int): Flow<List<RecentSearch>> =
        recentSearchDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            recentSearchDao.saveAndPrune(RecentSearchEntity(query = query.trim(), createdAt = System.currentTimeMillis()))
        }
    }

    override fun observeRecentEventLogs(limit: Int): Flow<List<EventLog>> =
        eventLogDao.getRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun logEvent(type: String, text: String): Long {
        return eventLogDao.insert(EventLogEntity(type = type, text = text))
    }

    override suspend fun pruneOldEventLogs() {
        val cutoff = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L) // 7 days
        eventLogDao.pruneOlderThan(cutoff)
    }

    companion object {
        fun calculateNextDeadline(
            currentDeadlineUtc: Long?,
            zoneIdString: String?,
            repeatRule: String,
            nowUtc: Long
        ): Long {
            val zoneId = try {
                if (!zoneIdString.isNullOrBlank()) ZoneId.of(zoneIdString) else ZoneId.systemDefault()
            } catch (_: Exception) {
                ZoneId.systemDefault()
            }

            val baseInstant = if (currentDeadlineUtc != null && currentDeadlineUtc > 0) {
                Instant.ofEpochMilli(currentDeadlineUtc)
            } else {
                Instant.ofEpochMilli(nowUtc)
            }

            var nextZdt = ZonedDateTime.ofInstant(baseInstant, zoneId)
            val upperRule = repeatRule.trim().uppercase()

            when (upperRule) {
                "DAILY" -> {
                    nextZdt = nextZdt.plusDays(1)
                }
                "WEEKDAYS" -> {
                    do {
                        nextZdt = nextZdt.plusDays(1)
                    } while (nextZdt.dayOfWeek == DayOfWeek.SATURDAY || nextZdt.dayOfWeek == DayOfWeek.SUNDAY)
                }
                "WEEKLY" -> {
                    nextZdt = nextZdt.plusWeeks(1)
                }
                "BIWEEKLY" -> {
                    nextZdt = nextZdt.plusWeeks(2)
                }
                "MONTHLY" -> {
                    nextZdt = nextZdt.plusMonths(1)
                }
                "YEARLY" -> {
                    nextZdt = nextZdt.plusYears(1)
                }
                else -> {
                    nextZdt = nextZdt.plusDays(1)
                }
            }

            // If nextZdt is still in the past compared to now, advance until it is in the future
            val nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowUtc), zoneId)
            while (nextZdt.isBefore(nowZdt)) {
                nextZdt = when (upperRule) {
                    "DAILY" -> nextZdt.plusDays(1)
                    "WEEKDAYS" -> {
                        var temp = nextZdt.plusDays(1)
                        while (temp.dayOfWeek == DayOfWeek.SATURDAY || temp.dayOfWeek == DayOfWeek.SUNDAY) {
                            temp = temp.plusDays(1)
                        }
                        temp
                    }
                    "WEEKLY" -> nextZdt.plusWeeks(1)
                    "BIWEEKLY" -> nextZdt.plusWeeks(2)
                    "MONTHLY" -> nextZdt.plusMonths(1)
                    "YEARLY" -> nextZdt.plusYears(1)
                    else -> nextZdt.plusDays(1)
                }
            }

            return nextZdt.toInstant().toEpochMilli()
        }
    }
}
