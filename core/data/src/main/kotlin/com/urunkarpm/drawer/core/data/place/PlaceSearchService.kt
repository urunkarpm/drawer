package com.urunkarpm.drawer.core.data.place

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.common.util.Haversine
import com.urunkarpm.drawer.core.model.PlaceSearchResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class NominatimResult(
    val place_id: Long? = null,
    val lat: String,
    val lon: String,
    val display_name: String? = null,
    val name: String? = null,
    val boundingbox: List<String>? = null
)

@Serializable
private data class NominatimReverseResult(
    val display_name: String? = null,
    val name: String? = null
)

@Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList()
)

@Serializable
private data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null
)

@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double> = emptyList() // [lon, lat]
)

@Serializable
private data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val extent: List<Double>? = null // [minLon, maxLat, maxLon, minLat]
)

@Singleton
class PlaceSearchService @Inject constructor(
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val rateLimitMutex = Mutex()
    private var lastRequestTimestamp = 0L

    private val cacheMutex = Mutex()
    private val searchCache = object : LinkedHashMap<String, List<PlaceSearchResult>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<PlaceSearchResult>>?): Boolean = size > 50
    }
    private val reverseCache = object : LinkedHashMap<String, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 50
    }

    companion object {
        private const val USER_AGENT = "Drawer-Launcher-Android/1.0 (com.urunkarpm.drawer; open-source-launcher)"
        private const val TIMEOUT_MS = 8000
    }

    private suspend fun throttle() = rateLimitMutex.withLock {
        val now = System.currentTimeMillis()
        val diff = now - lastRequestTimestamp
        if (diff < 1000L) {
            delay(1000L - diff)
        }
        lastRequestTimestamp = System.currentTimeMillis()
    }

    suspend fun search(query: String): Result<List<PlaceSearchResult>> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        // Check cache
        val cached = cacheMutex.withLock { searchCache[trimmed.lowercase()] }
        if (cached != null) return@withContext Result.success(cached)

        // 1. Try Nominatim
        val nominatimResult = runCatching {
            throttle()
            searchNominatim(trimmed)
        }

        if (nominatimResult.isSuccess && nominatimResult.getOrNull()?.isNotEmpty() == true) {
            val results = nominatimResult.getOrThrow()
            cacheMutex.withLock { searchCache[trimmed.lowercase()] = results }
            return@withContext Result.success(results)
        }

        // 2. Fallback to Photon
        val photonResult = runCatching {
            throttle()
            searchPhoton(trimmed)
        }

        if (photonResult.isSuccess) {
            val results = photonResult.getOrThrow()
            cacheMutex.withLock { searchCache[trimmed.lowercase()] = results }
            return@withContext Result.success(results)
        }

        // If Nominatim gave a non-empty error, return it or empty
        val exception = nominatimResult.exceptionOrNull() ?: photonResult.exceptionOrNull()
        if (exception != null) {
            Result.failure(exception)
        } else {
            Result.success(emptyList())
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<String> = withContext(ioDispatcher) {
        val cacheKey = "${"%.5f".format(lat)},${"%.5f".format(lon)}"
        val cached = cacheMutex.withLock { reverseCache[cacheKey] }
        if (cached != null) return@withContext Result.success(cached)

        // 1. Try Nominatim reverse
        val nomResult = runCatching {
            throttle()
            reverseNominatim(lat, lon)
        }

        if (nomResult.isSuccess && !nomResult.getOrNull().isNullOrBlank()) {
            val name = nomResult.getOrThrow()
            cacheMutex.withLock { reverseCache[cacheKey] = name }
            return@withContext Result.success(name)
        }

        // 2. Try Photon reverse
        val photonResult = runCatching {
            throttle()
            reversePhoton(lat, lon)
        }

        if (photonResult.isSuccess && !photonResult.getOrNull().isNullOrBlank()) {
            val name = photonResult.getOrThrow()
            cacheMutex.withLock { reverseCache[cacheKey] = name }
            return@withContext Result.success(name)
        }

        // 3. Fallback: "Selected point"
        Result.success("Selected point")
    }

    private fun searchNominatim(query: String): List<PlaceSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URI("https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&limit=5").toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<List<NominatimResult>>(jsonString)

        return parsed.map { item ->
            val lat = item.lat.toDoubleOrNull() ?: 0.0
            val lon = item.lon.toDoubleOrNull() ?: 0.0
            val name = item.name?.takeIf { it.isNotBlank() }
                ?: item.display_name?.split(",")?.firstOrNull()?.trim()
                ?: "Selected Place"
            val displayName = item.display_name ?: name

            val defaultRadius = calculateRadiusFromBoundingBox(lat, lon, item.boundingbox)

            PlaceSearchResult(
                name = name,
                displayName = displayName,
                latitude = lat,
                longitude = lon,
                defaultRadiusM = defaultRadius
            )
        }
    }

    private fun searchPhoton(query: String): List<PlaceSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URI("https://photon.komoot.io/api/?q=$encoded&limit=5").toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<PhotonResponse>(jsonString)

        return parsed.features.mapNotNull { feat ->
            val coords = feat.geometry?.coordinates ?: return@mapNotNull null
            if (coords.size < 2) return@mapNotNull null
            val lon = coords[0]
            val lat = coords[1]

            val props = feat.properties
            val name = props?.name ?: props?.street ?: props?.city ?: "Selected Place"
            val parts = listOfNotNull(props?.name, props?.street, props?.city, props?.country).filter { it.isNotBlank() }
            val displayName = if (parts.isNotEmpty()) parts.joinToString(", ") else name

            val defaultRadius = if (props?.extent != null && props.extent.size >= 4) {
                // extent: [minLon, maxLat, maxLon, minLat]
                val minLon = props.extent[0]
                val maxLat = props.extent[1]
                val maxLon = props.extent[2]
                val minLat = props.extent[3]
                calculateRadiusFromCoordinates(lat, lon, minLat, maxLat, minLon, maxLon)
            } else {
                200f
            }

            PlaceSearchResult(
                name = name,
                displayName = displayName,
                latitude = lat,
                longitude = lon,
                defaultRadiusM = defaultRadius
            )
        }
    }

    private fun reverseNominatim(lat: Double, lon: Double): String {
        val url = URI("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon").toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<NominatimReverseResult>(jsonString)
        val resolvedName = parsed.name?.takeIf { it.isNotBlank() }
            ?: parsed.display_name?.split(",")?.firstOrNull()?.trim()
        return resolvedName ?: "Selected point"
    }

    private fun reversePhoton(lat: Double, lon: Double): String {
        val url = URI("https://photon.komoot.io/reverse?lat=$lat&lon=$lon").toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<PhotonResponse>(jsonString)
        val feat = parsed.features.firstOrNull() ?: return "Selected point"
        val props = feat.properties
        return props?.name ?: props?.street ?: props?.city ?: "Selected point"
    }

    private fun calculateRadiusFromBoundingBox(lat: Double, lon: Double, bbox: List<String>?): Float {
        if (bbox == null || bbox.size < 4) return 200f
        val south = bbox[0].toDoubleOrNull() ?: lat
        val north = bbox[1].toDoubleOrNull() ?: lat
        val west = bbox[2].toDoubleOrNull() ?: lon
        val east = bbox[3].toDoubleOrNull() ?: lon
        return calculateRadiusFromCoordinates(lat, lon, south, north, west, east)
    }

    private fun calculateRadiusFromCoordinates(
        lat: Double,
        lon: Double,
        south: Double,
        north: Double,
        west: Double,
        east: Double
    ): Float {
        val dNorth = Haversine.distanceMeters(lat, lon, north, lon)
        val dSouth = Haversine.distanceMeters(lat, lon, south, lon)
        val dEast = Haversine.distanceMeters(lat, lon, lat, east)
        val dWest = Haversine.distanceMeters(lat, lon, lat, west)
        val maxDist = maxOf(dNorth, dSouth, dEast, dWest).toFloat()
        return maxDist.coerceIn(100f, 3000f)
    }
}
