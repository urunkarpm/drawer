package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class WeatherInfo(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val conditionDescription: String,
    val cityName: String,
    val lastUpdatedMillis: Long
)
