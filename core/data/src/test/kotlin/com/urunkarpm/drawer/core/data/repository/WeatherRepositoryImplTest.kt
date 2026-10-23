package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val manualLatFlow = MutableStateFlow<Double?>(null)
    private val manualLonFlow = MutableStateFlow<Double?>(null)
    private val manualCityFlow = MutableStateFlow<String?>(null)

    @Before
    fun setUp() {
        every { preferencesDataSource.manualLat } returns manualLatFlow
        every { preferencesDataSource.manualLon } returns manualLonFlow
        every { preferencesDataSource.manualCityName } returns manualCityFlow
    }

    @Test
    fun mapWmoCode_mapsSunnyAndClearConditions() {
        val (desc0, icon0) = WeatherRepositoryImpl.mapWmoCode(0)
        assertEquals("Clear sky", desc0)
        assertEquals("sunny", icon0)

        val (desc1, icon1) = WeatherRepositoryImpl.mapWmoCode(1)
        assertEquals("Mainly clear", desc1)
        assertEquals("sunny", icon1)
    }

    @Test
    fun mapWmoCode_mapsCloudyAndOvercastConditions() {
        val (desc2, icon2) = WeatherRepositoryImpl.mapWmoCode(2)
        assertEquals("Partly cloudy", desc2)
        assertEquals("partly_cloudy", icon2)

        val (desc3, icon3) = WeatherRepositoryImpl.mapWmoCode(3)
        assertEquals("Overcast", desc3)
        assertEquals("cloudy", icon3)
    }

    @Test
    fun mapWmoCode_mapsPrecipitationAndStormConditions() {
        val (descRain, iconRain) = WeatherRepositoryImpl.mapWmoCode(61)
        assertEquals("Rain", descRain)
        assertEquals("rain", iconRain)

        val (descSnow, iconSnow) = WeatherRepositoryImpl.mapWmoCode(71)
        assertEquals("Snow", descSnow)
        assertEquals("snow", iconSnow)

        val (descStorm, iconStorm) = WeatherRepositoryImpl.mapWmoCode(95)
        assertEquals("Thunderstorm", descStorm)
        assertEquals("thunderstorm", iconStorm)
    }

    @Test
    fun isValidCoordinate_validatesRealWorldCoordinates() {
        assertTrue(WeatherRepositoryImpl.isValidCoordinate(37.7749, -122.4194))
        assertTrue(WeatherRepositoryImpl.isValidCoordinate(51.5074, -0.1278))
        assertTrue(WeatherRepositoryImpl.isValidCoordinate(19.0760, 72.8777))
        assertTrue(WeatherRepositoryImpl.isValidCoordinate(-33.8688, 151.2093))
    }

    @Test
    fun isValidCoordinate_rejectsNullIslandAndOutOfRange() {
        assertFalse(WeatherRepositoryImpl.isValidCoordinate(0.0, 0.0))
        assertFalse(WeatherRepositoryImpl.isValidCoordinate(0.00005, -0.00005))
        assertFalse(WeatherRepositoryImpl.isValidCoordinate(95.0, 10.0))
        assertFalse(WeatherRepositoryImpl.isValidCoordinate(-91.0, 10.0))
        assertFalse(WeatherRepositoryImpl.isValidCoordinate(40.0, 195.0))
        assertFalse(WeatherRepositoryImpl.isValidCoordinate(40.0, -185.0))
    }
}
