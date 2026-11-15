package com.urunkarpm.drawer.feature.todo.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
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

// ponytail: modern floating-overlay place picker sheet matching the AddTodoBottomSheet design philosophy.
// ceiling: osmdroid MapView without vector tile rotation;
// upgrade path: vector-based map renderer with 3D buildings and smooth camera bearing transitions.
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
    val crispHaptics = LocalCrispHaptics.current

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

    val radiusPresets = remember { listOf(100f, 200f, 500f, 1000f, 2000f) }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        val basePath = File(context.cacheDir, "osmdroid")
        val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)
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
                    override fun onMarkerDragStart(marker: Marker?) {
                        crispHaptics.tick()
                    }
                    override fun onMarkerDragEnd(marker: Marker?) {
                        crispHaptics.confirm()
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
            map.overlays.add(0, circle)
            circleRef = circle
        } else {
            circleRef?.points = Polygon.pointsAsCircle(point, rad.toDouble())
        }

        map.invalidate()
    }

    fun fetchCurrentLocation() {
        crispHaptics.click()
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pick Location",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Search address or tap map to set trigger",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Sleek Search Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isBlank()) {
                            searchResults = emptyList()
                            isOfflineError = false
                        }
                    },
                    placeholder = {
                        Text(
                            "Search place, address or shop...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
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
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Chips Row: Saved places & Recent searches
            if (savedPlaces.isNotEmpty() || recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    savedPlaces.forEach { place ->
                        Surface(
                            onClick = {
                                crispHaptics.click()
                                selectedName = place.name
                                selectedLat = place.latitude
                                selectedLon = place.longitude
                                radiusM = place.radiusM.coerceIn(100f, 3000f)
                                updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = true)
                                searchResults = emptyList()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = place.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    recentSearches.forEach { recent ->
                        Surface(
                            onClick = {
                                crispHaptics.click()
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
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = recent.query,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Map View with Floating Search Results Overlay & Floating Attribution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        RoundedCornerShape(16.dp)
                    )
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

                            val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let {
                                        crispHaptics.tick()
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
                                        crispHaptics.confirm()
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

                // Floating My Location Button
                Surface(
                    onClick = { fetchCurrentLocation() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = "Use my location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Attribution pill: "© OpenStreetMap contributors"
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topStart = 8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "© OpenStreetMap contributors",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Smooth Floating Search Results Overlay (Prevents MapView resizing jumps)
                androidx.compose.animation.AnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(220)) { -it / 3 },
                    exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(180)) { -it / 3 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            searchResults.forEachIndexed { index, result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            crispHaptics.click()
                                            selectedName = result.name
                                            selectedLat = result.latitude
                                            selectedLon = result.longitude
                                            radiusM = result.defaultRadiusM.coerceIn(100f, 3000f)
                                            searchResults = emptyList()
                                            updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = true)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
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
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Smooth Floating Offline Error Banner
                androidx.compose.animation.AnimatedVisibility(
                    visible = isOfflineError,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline. Drag or tap map to drop pin manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // 4. Unified Place Details & Trigger Radius Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Place Name Input
                    TextField(
                        value = selectedName,
                        onValueChange = { selectedName = it },
                        placeholder = {
                            Text(
                                "Location label (e.g. Office, Grocery)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 0.75.dp
                    )

                    // Alert Radius Controls
                    val radiusText = if (radiusM >= 1000f) {
                        "${"%.1f".format(radiusM / 1000f)} km"
                    } else {
                        "${radiusM.roundToInt()} m"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Adjust,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Alert radius",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = radiusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Quick Radius Preset Chips (Hardcoded distances, slider removed per design spec)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        radiusPresets.forEach { preset ->
                            val isPresetSelected = (radiusM - preset).let { kotlin.math.abs(it) < 10f }
                            val label = if (preset >= 1000f) "${(preset / 1000f).roundToInt()} km" else "${preset.roundToInt()} m"
                            Surface(
                                onClick = {
                                    crispHaptics.tick()
                                    radiusM = preset
                                    updateMapOverlays(selectedLat, selectedLon, radiusM, moveTo = false)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPresetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPresetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isPresetSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 0.75.dp
                    )

                    // Save to favorite places switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (saveToSavedPlaces) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (saveToSavedPlaces) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Save to favorite places",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = saveToSavedPlaces,
                            onCheckedChange = {
                                crispHaptics.tick()
                                saveToSavedPlaces = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // 5. Actions: Cancel and Confirm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        crispHaptics.tick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = {
                        crispHaptics.confirm()
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Confirm",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
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

