package com.urunkarpm.drawer.feature.todo.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.urunkarpm.drawer.core.model.PlaceSearchResult
import com.urunkarpm.drawer.core.model.SavedPlace
import com.urunkarpm.drawer.feature.todo.BuildConfig
import com.urunkarpm.drawer.feature.todo.TodoViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerBottomSheet(
    viewModel: TodoViewModel,
    initialPlaceName: String? = null,
    initialLat: Double? = null,
    initialLon: Double? = null,
    initialRadiusM: Float = 200f,
    onConfirm: (placeName: String, latitude: Double, longitude: Double, radiusM: Float, shouldSavePlace: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    // Sheet height ~60-65% screen height, not full screen
    val sheetHeight = configuration.screenHeightDp.dp * 0.65f

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val savedPlaces by viewModel.savedPlaces.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var isOfflineError by remember { mutableStateOf(false) }

    // Selected place state
    var selectedName by remember { mutableStateOf(initialPlaceName ?: "Selected point") }
    var selectedLat by remember { mutableStateOf(initialLat ?: 37.7749) }
    var selectedLon by remember { mutableStateOf(initialLon ?: -122.4194) }
    var radiusM by remember { mutableFloatStateOf(initialRadiusM.coerceIn(100f, 3000f)) }
    var saveToSavedPlaces by remember { mutableStateOf(false) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var markerRef by remember { mutableStateOf<Marker?>(null) }
    var circleRef by remember { mutableStateOf<Polygon?>(null) }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        val basePath = File(context.cacheDir, "osmdroid")
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().osmdroidBasePath = basePath
        Configuration.getInstance().osmdroidTileCache = File(basePath, "tiles")
        Configuration.getInstance().userAgentValue = "Drawer-Launcher-Android/1.0 (com.urunkarpm.drawer; open-source-launcher)"
    }

    fun updateMapOverlays(lat: Double, lon: Double, rad: Float, moveTo: Boolean = false) {
        val map = mapViewRef ?: return
        val point = GeoPoint(lat, lon)

        if (moveTo) {
            map.controller.animateTo(point)
            map.controller.setZoom(16.0)
        }

        // Update or create Marker
        if (markerRef == null) {
            val marker = Marker(map).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                isDraggable = true
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(marker: Marker?) {}
                    override fun onMarkerDragStart(marker: Marker?) {}
                    override fun onMarkerDragEnd(marker: Marker?) {
                        marker?.position?.let { p ->
                            selectedLat = p.latitude
                            selectedLon = p.longitude
                            updateMapOverlays(p.latitude, p.longitude, radiusM, moveTo = false)
                            viewModel.reverseGeocode(p.latitude, p.longitude) { name ->
                                selectedName = name
                            }
                        }
                    }
                })
            }
            map.overlays.add(marker)
            markerRef = marker
        } else {
            markerRef?.position = point
        }

        // Update or create Circle Polygon
        if (circleRef == null) {
            val circle = Polygon(map).apply {
                points = Polygon.pointsAsCircle(point, rad.toDouble())
                fillPaint.color = 0x332196F3
                outlinePaint.color = 0xFF1976D2.toInt()
                outlinePaint.strokeWidth = 3f
            }
            map.overlays.add(0, circle) // circle beneath marker
            circleRef = circle
        } else {
            circleRef?.points = Polygon.pointsAsCircle(point, rad.toDouble())
        }

        map.invalidate()
    }

    fun fetchCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Toast.makeText(context, "Location permission not granted. Tap on map to set place.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm == null) {
                Toast.makeText(context, "Location service unavailable", Toast.LENGTH_SHORT).show()
                return
            }

            var bestLocation: Location? = null
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            for (p in providers) {
                if (lm.isProviderEnabled(p)) {
                    @SuppressLint("MissingPermission")
                    val l = lm.getLastKnownLocation(p)
                    if (l != null && (bestLocation == null || l.time > bestLocation.time)) {
                        bestLocation = l
                    }
                }
            }

            if (bestLocation != null) {
                selectedLat = bestLocation.latitude
                selectedLon = bestLocation.longitude
                updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = true)
                viewModel.reverseGeocode(selectedLat, selectedLon) { name ->
                    selectedName = name
                }
            } else {
                Toast.makeText(context, "Looking for location...", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pick Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Search Box (triggers only on IME Search action)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isBlank()) {
                        searchResults = emptyList()
                        isOfflineError = false
                    }
                },
                placeholder = { Text("Search address or place...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            keyboardController?.hide()
                            isSearching = true
                            isOfflineError = false
                            viewModel.searchPlaces(searchQuery) { results, failed ->
                                isSearching = false
                                if (failed) {
                                    isOfflineError = true
                                    searchResults = emptyList()
                                } else {
                                    isOfflineError = false
                                    searchResults = results.take(5)
                                }
                            }
                        }
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Chips Row: Saved places & Recent searches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                savedPlaces.forEach { place ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            selectedName = place.name
                            selectedLat = place.latitude
                            selectedLon = place.longitude
                            radiusM = place.radiusM.coerceIn(100f, 3000f)
                            updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = true)
                            searchResults = emptyList()
                        },
                        label = { Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                recentSearches.forEach { recent ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            searchQuery = recent.query
                            keyboardController?.hide()
                            isSearching = true
                            viewModel.searchPlaces(recent.query) { results, failed ->
                                isSearching = false
                                if (failed) {
                                    isOfflineError = true
                                    searchResults = emptyList()
                                } else {
                                    isOfflineError = false
                                    searchResults = results.take(5)
                                }
                            }
                        },
                        label = { Text(recent.query, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            // Search results list (up to 5 items)
            AnimatedVisibility(
                visible = searchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Column {
                        searchResults.forEachIndexed { index, result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedName = result.name
                                        selectedLat = result.latitude
                                        selectedLon = result.longitude
                                        radiusM = result.defaultRadiusM.coerceIn(100f, 3000f)
                                        searchResults = emptyList()
                                        updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = true)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = result.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (index < searchResults.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            // Offline / No Internet Warning Banner
            if (isOfflineError) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "No internet. Move the map and tap to drop a pin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // 3. Map View (osmdroid) with Attribution & Location button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            val tileSource = if (BuildConfig.DEBUG) {
                                TileSourceFactory.MAPNIK
                            } else {
                                TileSourceFactory.OpenTopo
                            }
                            setTileSource(tileSource)
                            setMultiTouchControls(true)
                            isTilesScaledToDpi = true
                            controller.setZoom(15.0)

                            val center = GeoPoint(selectedLat, selectedLon)
                            controller.setCenter(center)

                            // Add click & long press receiver
                            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        selectedLat = it.latitude
                                        selectedLon = it.longitude
                                        updateMapOverlays(it.latitude, it.longitude, radiusM, moveTo = false)
                                        viewModel.reverseGeocode(it.latitude, it.longitude) { name ->
                                            selectedName = name
                                        }
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        selectedLat = it.latitude
                                        selectedLon = it.longitude
                                        updateMapOverlays(it.latitude, it.longitude, radiusM, moveTo = false)
                                        viewModel.reverseGeocode(it.latitude, it.longitude) { name ->
                                            selectedName = name
                                        }
                                    }
                                    return true
                                }
                            })
                            overlays.add(eventsOverlay)

                            mapViewRef = this
                            updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = false)
                        }
                    },
                    update = { map ->
                        mapViewRef = map
                        updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = false)
                    }
                )

                // Optional "Use my location" button
                FloatingActionButton(
                    onClick = { fetchCurrentLocation() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(40.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Use my location",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Mandatory attribution: "© OpenStreetMap contributors"
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 6.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "© OpenStreetMap contributors",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Editable Place Name
            OutlinedTextField(
                value = selectedName,
                onValueChange = { selectedName = it },
                label = { Text("Place Name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Radius Slider (100m to 3000m)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val radiusText = if (radiusM >= 1000f) {
                        "${"%.1f".format(radiusM / 1000f)} km"
                    } else {
                        "${radiusM.roundToInt()} m"
                    }
                    Text(
                        text = "Alert Radius: $radiusText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Larger circle: earlier alert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = radiusM,
                    onValueChange = {
                        radiusM = it
                        updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = false)
                    },
                    valueRange = 100f..3000f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Save this place switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Save this place to favorites",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = saveToSavedPlaces,
                    onCheckedChange = { saveToSavedPlaces = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Actions: Cancel and Confirm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val finalName = selectedName.trim().ifBlank { "Selected point" }
                        if (saveToSavedPlaces) {
                            viewModel.savePlace(
                                SavedPlace(
                                    name = finalName,
                                    latitude = selectedLat,
                                    longitude = selectedLon,
                                    radiusM = radiusM
                                )
                            )
                        }
                        onConfirm(finalName, selectedLat, selectedLon, radiusM, saveToSavedPlaces)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }
}
