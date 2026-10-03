package com.urunkarpm.drawer.core.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.WeatherInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val current: OpenMeteoCurrent? = null
)

@Serializable
data class OpenMeteoCurrent(
    val time: String? = null,
    @SerialName("temperature_2m")
    val temperature2m: Double = 0.0,
    @SerialName("weather_code")
    val weatherCode: Int = 0
)

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : WeatherRepository {

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    override val weatherInfo: Flow<WeatherInfo?> = _weatherInfo.asStateFlow()

    override suspend fun refreshWeather(): Result<WeatherInfo> = withContext(ioDispatcher) {
        try {
            val manualLat = preferencesDataSource.manualLat.first()
            val manualLon = preferencesDataSource.manualLon.first()
            val manualCity = preferencesDataSource.manualCityName.first()

            val (lat, lon, cityName) = if (manualLat != null && manualLon != null) {
                Triple(manualLat, manualLon, manualCity ?: "Custom Location")
            } else if (!manualCity.isNullOrBlank()) {
                val geocoded = try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val list = geocoder.getFromLocationName(manualCity, 1)
                    val addr = list?.firstOrNull()
                    if (addr != null) Pair(addr.latitude, addr.longitude) else null
                } catch (_: Exception) {
                    null
                }
                if (geocoded != null) {
                    Triple(geocoded.first, geocoded.second, manualCity)
                } else {
                    fetchLocationTriple()
                }
            } else {
                fetchLocationTriple()
            }

            val response: OpenMeteoResponse = httpClient.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current", "temperature_2m,weather_code")
            }.body()

            val current = response.current ?: throw IllegalStateException("Empty weather data returned")
            val (conditionDesc, _) = mapWmoCode(current.weatherCode)

            val info = WeatherInfo(
                temperatureCelsius = current.temperature2m,
                weatherCode = current.weatherCode,
                conditionDescription = conditionDesc,
                cityName = cityName,
                lastUpdatedMillis = System.currentTimeMillis()
            )

            _weatherInfo.value = info
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchLocationTriple(): Triple<Double, Double, String> {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val loc = if (hasLocationPermission) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                var l = fusedClient.lastLocation.awaitTask()
                if (l == null) {
                    l = fusedClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    ).awaitTask()
                }
                l
            } catch (_: Exception) {
                null
            }
        } else null

        return if (loc != null) {
            val resolvedCity = resolveCityName(loc.latitude, loc.longitude)
            Triple(loc.latitude, loc.longitude, resolvedCity)
        } else {
            // Default fallback coordinates (London)
            Triple(51.5074, -0.1278, "Local Weather")
        }
    }

    private fun resolveCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val address = addresses?.firstOrNull()
            address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "My Location"
        } catch (_: Exception) {
            "My Location"
        }
    }

    companion object {
        fun mapWmoCode(code: Int): Pair<String, String> {
            return when (code) {
                0 -> "Clear sky" to "sunny"
                1 -> "Mainly clear" to "sunny"
                2 -> "Partly cloudy" to "partly_cloudy"
                3 -> "Overcast" to "cloudy"
                45, 48 -> "Fog" to "fog"
                51, 53, 55 -> "Drizzle" to "drizzle"
                56, 57 -> "Freezing Drizzle" to "drizzle"
                61, 63, 65 -> "Rain" to "rain"
                66, 67 -> "Freezing Rain" to "rain"
                71, 73, 75 -> "Snow" to "snow"
                77 -> "Snow grains" to "snow"
                80, 81, 82 -> "Rain showers" to "rain"
                85, 86 -> "Snow showers" to "snow"
                95 -> "Thunderstorm" to "thunderstorm"
                96, 99 -> "Thunderstorm with hail" to "thunderstorm"
                else -> "Clear" to "sunny"
            }
        }
    }
}

suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { _ -> cont.resume(null) }
    addOnCanceledListener { cont.cancel() }
}
