package com.urunkarpm.drawer.core.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

    init {
        // Hydrate from cached weather info on startup for instantaneous rendering without London flicker
        CoroutineScope(ioDispatcher).launch {
            try {
                val cached = preferencesDataSource.cachedWeatherInfo.firstOrNull()
                if (cached != null && _weatherInfo.value == null) {
                    _weatherInfo.value = cached
                }
            } catch (_: Throwable) {}
        }
    }

    override suspend fun refreshWeather(): Result<WeatherInfo> = withContext(ioDispatcher) {
        try {
            val manualLat = preferencesDataSource.manualLat.first()
            val manualLon = preferencesDataSource.manualLon.first()
            val manualCity = preferencesDataSource.manualCityName.first()

            val (lat, lon, cityName) = if (manualLat != null && manualLon != null && isValidCoordinate(manualLat, manualLon)) {
                Triple(manualLat, manualLon, manualCity ?: "Custom Location")
            } else if (!manualCity.isNullOrBlank()) {
                val geocoded = try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val list = geocoder.getFromLocationName(manualCity, 1)
                    val addr = list?.firstOrNull()
                    if (addr != null && isValidCoordinate(addr.latitude, addr.longitude)) Pair(addr.latitude, addr.longitude) else null
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
            preferencesDataSource.setCachedWeatherInfo(info)
            Result.success(info)
        } catch (e: Exception) {
            // Keep existing weather intact on transient error; never blow away known weather
            if (_weatherInfo.value == null) {
                try {
                    preferencesDataSource.cachedWeatherInfo.firstOrNull()?.let { cached ->
                        _weatherInfo.value = cached
                    }
                } catch (_: Throwable) {}
            }
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

        val savedLat = preferencesDataSource.lastKnownWeatherLat.first()
        val savedLon = preferencesDataSource.lastKnownWeatherLon.first()
        val savedCity = preferencesDataSource.lastKnownWeatherCity.first()

        // 1. Try to acquire the best location from Android LocationManager providers
        val loc = if (hasLocationPermission) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                var bestLocation: Location? = null
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER
                )
                for (provider in providers) {
                    try {
                        val l = lm?.getLastKnownLocation(provider)
                        if (l != null && isValidCoordinate(l.latitude, l.longitude)) {
                            if (bestLocation == null || l.time > bestLocation.time) {
                                bestLocation = l
                            }
                        }
                    } catch (_: SecurityException) {
                        // ignore permission issue on specific provider
                    }
                }

                // If passive cache is stale (> 30 min) or missing, attempt an active single-shot network fix on Android 11+
                if ((bestLocation == null || System.currentTimeMillis() - bestLocation.time > 30 * 60 * 1000L) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && lm != null
                ) {
                    val fresh = requestCurrentLocation(lm)
                    if (fresh != null && isValidCoordinate(fresh.latitude, fresh.longitude)) {
                        bestLocation = fresh
                    }
                }

                bestLocation
            } catch (_: Exception) {
                null
            }
        } else null

        // 2. If fresh location is available and valid, resolve city and persist
        if (loc != null && isValidCoordinate(loc.latitude, loc.longitude)) {
            val resolvedCity = resolveCityName(loc.latitude, loc.longitude, fallbackCity = savedCity)
            preferencesDataSource.setLastKnownWeatherLocation(loc.latitude, loc.longitude, resolvedCity)
            return Triple(loc.latitude, loc.longitude, resolvedCity)
        }

        // 3. ponytail: Never bounce to arbitrary default coordinates if a valid location was ever resolved; ceiling: relies on DataStore last known location; upgrade path: manual multi-city picker.
        if (savedLat != null && savedLon != null && isValidCoordinate(savedLat, savedLon)) {
            return Triple(savedLat, savedLon, savedCity ?: "My Location")
        }

        // 4. In-memory fallback if _weatherInfo already has valid non-placeholder data
        val current = _weatherInfo.value
        if (current != null && !current.cityName.equals("Local Weather", ignoreCase = true)) {
            if (savedLat != null && savedLon != null) {
                return Triple(savedLat, savedLon, current.cityName)
            }
        }

        // 5. Ultimate initial fallback only before any location fix has ever been granted
        return Triple(51.5074, -0.1278, "Local Weather")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestCurrentLocation(lm: LocationManager): Location? = withTimeoutOrNull(3000L) {
        suspendCancellableCoroutine { continuation ->
            val signal = android.os.CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val executor = ContextCompat.getMainExecutor(context)
                val provider = when {
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> null
                }
                if (provider != null) {
                    lm.getCurrentLocation(provider, signal, executor) { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                } else {
                    if (continuation.isActive) continuation.resume(null)
                }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private fun resolveCityName(lat: Double, lon: Double, fallbackCity: String? = null): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val address = addresses?.firstOrNull()
            val resolved = address?.locality ?: address?.subAdminArea ?: address?.adminArea
            if (!resolved.isNullOrBlank() && !resolved.equals("Unknown", ignoreCase = true)) {
                resolved
            } else if (!fallbackCity.isNullOrBlank() && !fallbackCity.equals("My Location", ignoreCase = true) && !fallbackCity.equals("Local Weather", ignoreCase = true)) {
                fallbackCity
            } else {
                resolved ?: "My Location"
            }
        } catch (_: Exception) {
            if (!fallbackCity.isNullOrBlank() && !fallbackCity.equals("My Location", ignoreCase = true) && !fallbackCity.equals("Local Weather", ignoreCase = true)) {
                fallbackCity
            } else {
                "My Location"
            }
        }
    }

    companion object {
        fun isValidCoordinate(lat: Double, lon: Double): Boolean {
            if (kotlin.math.abs(lat) < 0.0001 && kotlin.math.abs(lon) < 0.0001) return false
            return lat in -90.0..90.0 && lon in -180.0..180.0
        }

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
