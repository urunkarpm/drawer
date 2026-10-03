package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
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
}
