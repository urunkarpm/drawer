package com.urunkarpm.drawer.core.data.location

import android.content.Context
import android.location.Location
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LocationEngineManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val todoDao: TodoDao = mockk(relaxed = true)
    private val todoRepository: TodoRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val notificationHelper: LocationNotificationHelper = mockk(relaxed = true)

    private val settingsFlow = MutableStateFlow(TodoSettings(locationFeaturesEnabled = true))
    private val snoozedUntilFlow = MutableStateFlow(0L)

    private lateinit var manager: LocationEngineManager

    @Before
    fun setUp() {
        every { preferencesDataSource.todoSettings } returns settingsFlow
        every { preferencesDataSource.todoLocationAlertsSnoozedUntil } returns snoozedUntilFlow

        manager = LocationEngineManager(
            context = context,
            todoDao = todoDao,
            todoRepository = todoRepository,
            preferencesDataSource = preferencesDataSource,
            notificationHelper = notificationHelper
        )
    }

    @Test
    fun handleNewLocation_ignoresFix_whenAccuracyWorseThan100m() {
        val badLocation = mockk<Location>(relaxed = true) {
            every { hasAccuracy() } returns true
            every { accuracy } returns 150f
            every { time } returns 1000L
        }

        manager.handleNewLocation(badLocation)

        assertNull(manager.getLastLocation())
    }

    @Test
    fun handleNewLocation_acceptsAccurateFix() {
        val goodLocation = mockk<Location>(relaxed = true) {
            every { hasAccuracy() } returns true
            every { accuracy } returns 25f
            every { time } returns 1000L
            every { latitude } returns 37.7749
            every { longitude } returns -122.4194
        }

        manager.handleNewLocation(goodLocation)

        assertEquals(goodLocation, manager.getLastLocation())
    }

    @Test
    fun handleNewLocation_ignoresFix_whenSpeedExceeds200kmh() {
        val fix1 = mockk<Location>(relaxed = true) {
            every { hasAccuracy() } returns true
            every { accuracy } returns 10f
            every { time } returns 1_000_000L
            every { latitude } returns 37.7749
            every { longitude } returns -122.4194
        }
        manager.handleNewLocation(fix1)
        assertEquals(fix1, manager.getLastLocation())

        // 1 second later, 100 km away (speed = 360,000 km/h)
        val teleportFix = mockk<Location>(relaxed = true) {
            every { hasAccuracy() } returns true
            every { accuracy } returns 10f
            every { time } returns 1_001_000L
            every { latitude } returns 38.7749
            every { longitude } returns -122.4194
        }
        manager.handleNewLocation(teleportFix)

        // Last valid fix must remain fix1
        assertEquals(fix1, manager.getLastLocation())
    }
}
