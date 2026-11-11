package com.urunkarpm.drawer.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.urunkarpm.drawer.core.common.util.Haversine
import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class TaskDwellState(
    val firstFixInsideTime: Long = 0L,
    val consecutiveFixCount: Int = 0,
    val isInside: Boolean = false,
    val hasAlerted: Boolean = false
)

@Singleton
class LocationEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val todoDao: TodoDao,
    private val todoRepository: TodoRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val notificationHelper: LocationNotificationHelper
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sigMotionSensor = sm?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)

    private var lastFix: Location? = null
    private var isTracking = false
    private var isForegroundActive = false
    private var nearestDistanceMeters: Double = Double.MAX_VALUE

    // Active nearest targets (kept to 20-30 places)
    private var activeTargets: List<TodoItem> = emptyList()
    private val dwellMap = mutableMapOf<Long, TaskDwellState>()

    var isNetworkProviderAvailable: Boolean = false
        private set
    var isGpsProviderAvailable: Boolean = false
        private set

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleNewLocation(location)
        }

        override fun onProviderEnabled(provider: String) {
            refreshProviderStatus()
        }

        override fun onProviderDisabled(provider: String) {
            refreshProviderStatus()
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            refreshProviderStatus()
        }
    }

    private val triggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            // Significant motion woke the phone up from stationary
            scope.launch {
                todoRepository.logEvent("SIGNIFICANT_MOTION", "Phone motion detected; requesting single location fix")
                requestSingleFix()
                rearmSignificantMotion()
            }
        }
    }

    init {
        refreshProviderStatus()
    }

    private fun refreshProviderStatus() {
        val manager = lm ?: return
        isGpsProviderAvailable = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isNetworkProviderAvailable = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Updates target list from database, prunes to nearest 25 open tasks with places,
     * and evaluates engine duty cycling.
     */
    suspend fun updateTargetsAndEvaluate(): Unit = mutex.withLock {
        val settings = preferencesDataSource.todoSettings.first()
        if (!settings.locationFeaturesEnabled) {
            stopEngine()
            return
        }

        val allOpen = todoDao.getOpenTodosList().map { it.toDomain() }
        val withPlaces = allOpen.filter {
            it.status == TodoStatus.OPEN && it.latitude != null && it.longitude != null
        }

        if (withPlaces.isEmpty()) {
            activeTargets = emptyList()
            nearestDistanceMeters = Double.MAX_VALUE
            stopEngine()
            return
        }

        val refLocation = lastFix
        activeTargets = if (refLocation != null) {
            withPlaces.sortedBy {
                Haversine.distanceMeters(refLocation.latitude, refLocation.longitude, it.latitude!!, it.longitude!!)
            }.take(25)
        } else {
            withPlaces.take(25)
        }

        if (refLocation != null && activeTargets.isNotEmpty()) {
            nearestDistanceMeters = activeTargets.minOf {
                Haversine.distanceMeters(refLocation.latitude, refLocation.longitude, it.latitude!!, it.longitude!!)
            }
            evaluateDutyCycle(nearestDistanceMeters)
        } else {
            // Trigger an initial single fix to compute distance to nearest place
            requestSingleFix()
        }
    }

    /**
     * Requirement 4: Distance duty cycling:
     * a. More than 5 km: duty-cycle checking (10-15 min).
     * b. Less than 5 km: foreground service with frequent checks (30-120 sec).
     * Stop service when > 6 km from all places or when no places remain.
     */
    private fun evaluateDutyCycle(nearestDistance: Double) {
        if (nearestDistance <= 5000.0) {
            startForegroundService()
        } else if (nearestDistance > 6000.0) {
            stopForegroundService()
        }
    }

    private fun startForegroundService() {
        if (!isForegroundActive) {
            try {
                LocationEngineService.start(context)
                isForegroundActive = true
                registerListeners(intervalMs = 45_000L) // 45 seconds polling
                scope.launch {
                    todoRepository.logEvent("LOCATION_FGS_STARTED", "Entered < 5km from task place; started Foreground Service")
                }
            } catch (e: Exception) {
                scope.launch {
                    todoRepository.logEvent("LOCATION_FGS_ERROR", "Failed to start FGS: ${e.message}")
                }
            }
        }
    }

    private fun stopForegroundService() {
        if (isForegroundActive) {
            LocationEngineService.stop(context)
            isForegroundActive = false
            registerListeners(intervalMs = 12 * 60 * 1000L) // 12 minutes polling
            scope.launch {
                todoRepository.logEvent("LOCATION_FGS_STOPPED", "Moved > 6km from all task places; stopped Foreground Service")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun registerListeners(intervalMs: Long) {
        val manager = lm ?: return
        if (!hasLocationPermission()) return

        try {
            manager.removeUpdates(locationListener)
            val minDistanceM = 15f

            if (manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, intervalMs, minDistanceM, locationListener)
            }
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, intervalMs, minDistanceM, locationListener)
            }
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intervalMs, minDistanceM, locationListener)
            }
            isTracking = true
            rearmSignificantMotion()
        } catch (_: SecurityException) {
            isTracking = false
        }
    }

    private fun rearmSignificantMotion() {
        if (sigMotionSensor != null) {
            try {
                sm?.requestTriggerSensor(triggerListener, sigMotionSensor)
            } catch (_: Exception) {}
        }
    }

    fun stopEngine() {
        lm?.removeUpdates(locationListener)
        LocationEngineService.stop(context)
        isForegroundActive = false
        isTracking = false
        try {
            sm?.cancelTriggerSensor(triggerListener, sigMotionSensor)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun requestSingleFix() {
        val manager = lm ?: return
        if (!hasLocationPermission()) return

        var best: Location? = null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (p in providers) {
            if (manager.isProviderEnabled(p)) {
                try {
                    val l = manager.getLastKnownLocation(p)
                    if (l != null && (best == null || l.time > best.time)) {
                        best = l
                    }
                } catch (_: SecurityException) {}
            }
        }

        best?.let { handleNewLocation(it) }
    }

    /**
     * Requirement 6:
     * Ignore fix if accuracy > 100m.
     * Ignore fix if implied speed > 200 km/h.
     */
    fun handleNewLocation(location: Location) {
        // Filter 1: Accuracy > 100m
        if (location.hasAccuracy() && location.accuracy > 100f) {
            return
        }

        // Filter 2: Speed > 200 km/h (55.56 m/s)
        val prev = lastFix
        val now = System.currentTimeMillis()
        if (prev != null) {
            val dtSec = (location.time - prev.time).coerceAtLeast(1L) / 1000.0
            val distM = Haversine.distanceMeters(prev.latitude, prev.longitude, location.latitude, location.longitude)
            val speedKmh = (distM / dtSec) * 3.6
            if (speedKmh > 200.0) {
                scope.launch {
                    todoRepository.logEvent("LOCATION_GLITCH_IGNORED", "Ignored glitch fix: speed $speedKmh km/h, dist $distM m")
                }
                return
            }
        }

        lastFix = location
        scope.launch {
            processGeofences(location, now)
        }
    }

    private suspend fun processGeofences(location: Location, now: Long) {
        val settings = preferencesDataSource.todoSettings.first()
        if (!settings.locationFeaturesEnabled) return

        // Global 1-day snooze check
        val globalSnoozeUntil = preferencesDataSource.todoLocationAlertsSnoozedUntil.first()
        if (now < globalSnoozeUntil) return

        val placesInsideNow = mutableListOf<TodoItem>()

        for (task in activeTargets) {
            val taskLat = task.latitude ?: continue
            val taskLon = task.longitude ?: continue
            val radiusM = task.radiusM

            val dist = Haversine.distanceMeters(location.latitude, location.longitude, taskLat, taskLon)
            val enterCircle = radiusM
            val exitCircle = radiusM * 1.3f

            val currentState = dwellMap[task.id] ?: TaskDwellState()
            val isInside = if (currentState.isInside) dist <= exitCircle else dist <= enterCircle

            if (isInside) {
                val updatedState = if (!currentState.isInside) {
                    TaskDwellState(firstFixInsideTime = now, consecutiveFixCount = 1, isInside = true, hasAlerted = false)
                } else {
                    currentState.copy(consecutiveFixCount = currentState.consecutiveFixCount + 1)
                }
                dwellMap[task.id] = updatedState

                // Requirement 8: Dwell rule: inside circle for >= 90s with 2 valid consecutive fixes
                val dwellDurationMs = now - updatedState.firstFixInsideTime
                if (dwellDurationMs >= 90_000L && updatedState.consecutiveFixCount >= 2) {
                    placesInsideNow.add(task)
                }
            } else {
                // Left exit circle
                dwellMap[task.id] = TaskDwellState(0L, 0, isInside = false, hasAlerted = false)
            }
        }

        if (placesInsideNow.isEmpty()) {
            // Update nearest distance for duty cycle
            if (activeTargets.isNotEmpty()) {
                val minD = activeTargets.minOf {
                    Haversine.distanceMeters(location.latitude, location.longitude, it.latitude!!, it.longitude!!)
                }
                nearestDistanceMeters = minD
                evaluateDutyCycle(minD)
            }
            return
        }

        // Smart Rule: If user is near >= 3 task places simultaneously, dispatch grouped notification
        if (placesInsideNow.size >= 3) {
            val names = placesInsideNow.mapNotNull { it.placeName }.distinct()
            notificationHelper.showGroupedLocationAlert(
                placesCount = placesInsideNow.size,
                placeNames = names,
                hideTextOnLockScreen = settings.hideTaskTextOnLockScreen
            )
            todoRepository.logEvent("LOCATION_GROUPED_ALERT", "Dispatched grouped alert for ${placesInsideNow.size} nearby places")
            return
        }

        // Single alerts for tasks meeting dwell rule
        for (task in placesInsideNow) {
            val state = dwellMap[task.id] ?: continue
            if (state.hasAlerted) continue

            // Check quiet hours
            if (TodoUtils.isInQuietHours(now, settings.quietHoursStart, settings.quietHoursEnd)) {
                todoRepository.logEvent("LOCATION_ALERT_DEFERRED", "Deferred location alert for ${task.title}: Quiet hours")
                continue
            }

            // Check task snooze
            val taskSnoozedUntil = task.snoozedUntil
            if (taskSnoozedUntil != null && now < taskSnoozedUntil) continue

            // Check 3-hour cooldown
            val lastAlert = task.lastLocationAlertAt
            if (lastAlert != null && (now - lastAlert) < 3 * 3600 * 1000L) {
                continue
            }

            // Check place suppression from 3x 'Not now'
            val placeName = task.placeName ?: "Place"
            val prefs = context.getSharedPreferences("todo_location_prefs", Context.MODE_PRIVATE)
            val suppressUntil = prefs.getLong("suppress_place_${placeName.lowercase()}", 0L)
            if (now < suppressUntil) {
                continue
            }

            // Dispatch alert!
            notificationHelper.showLocationAlert(
                taskId = task.id,
                placeName = placeName,
                taskTitle = task.title,
                hideTextOnLockScreen = settings.hideTaskTextOnLockScreen
            )

            // Update task lastLocationAlertAt
            todoDao.getTodoById(task.id)?.let { entity ->
                todoDao.update(entity.copy(lastLocationAlertAt = now))
            }
            dwellMap[task.id] = state.copy(hasAlerted = true)
            todoRepository.logEvent("LOCATION_ALERT_SENT", "Dispatched nearby alert for ${task.title} at $placeName")
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun getLastLocation(): Location? = lastFix
    fun getNearestDistance(): Double = nearestDistanceMeters
    fun isServiceRunning(): Boolean = isForegroundActive
}
