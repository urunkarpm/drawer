package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.model.WeatherInfo
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    val weatherInfo: Flow<WeatherInfo?>
    suspend fun refreshWeather(): Result<WeatherInfo>
}
