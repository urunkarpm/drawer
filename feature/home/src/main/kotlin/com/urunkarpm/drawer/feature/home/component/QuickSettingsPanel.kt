package com.urunkarpm.drawer.feature.home.component

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme

val DEFAULT_QUICK_TILES_ORDER = listOf(
    "rotate",
    "wifi",
    "bluetooth",
    "quick_share",
    "dnd",
    "auto_rotate",
    "location",
    "flashlight"
)

@Immutable
data class QuickTileData(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isActive: Boolean,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit
)

@Composable
fun QuickSettingsPanel(
    tileOrder: List<String>,
    hiddenTiles: Set<String>,
    onUpdateTileOrder: (List<String>) -> Unit,
    onUpdateHiddenTiles: (Set<String>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    drawerThemeMode: String = "SYSTEM",
    surfaceCornerRadiusDp: Float = 24f
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    // State observers
    val quickSettingsState = rememberQuickSettingsState(context)

    val isSystemDark = isSystemInDarkTheme()
    val isDrawerDark = when (drawerThemeMode.uppercase()) {
        "WHITE", "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DrawerTheme(darkTheme = isDrawerDark) {
        val drawerBackgroundColor = if (isDrawerDark) Color(0xFF141218) else Color(0xFFFFFFFF)
        val drawerBorder = BorderStroke(1.dp, if (isDrawerDark) Color(0xFF2B2930) else Color(0xFFE5E7EB))
        val panelShape = if (isLandscape) {
            RoundedCornerShape(
                topStart = surfaceCornerRadiusDp.dp,
                bottomStart = surfaceCornerRadiusDp.dp,
                topEnd = 0.dp,
                bottomEnd = 0.dp
            )
        } else {
            RoundedCornerShape(
                topStart = surfaceCornerRadiusDp.dp,
                topEnd = surfaceCornerRadiusDp.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        }

        // Construct master tile definitions
        val masterTiles = remember(quickSettingsState) {
            mapOf(
                "rotate" to QuickTileData(
                    id = "rotate",
                    title = "Rotate",
                    subtitle = if (quickSettingsState.isLandscape) "Landscape" else "Portrait",
                    icon = Icons.Default.ScreenRotation,
                    isActive = quickSettingsState.isLandscape,
                    onClick = { quickSettingsState.toggleRotate() },
                    onLongClick = { quickSettingsState.openDisplaySettings() }
                ),
                "wifi" to QuickTileData(
                    id = "wifi",
                    title = "Internet",
                    subtitle = when {
                        quickSettingsState.isWifiOn && quickSettingsState.wifiSsid != null -> quickSettingsState.wifiSsid
                        quickSettingsState.isWifiOn -> "Wi-Fi On"
                        quickSettingsState.isCellularOn || quickSettingsState.isInternetConnected -> "Mobile data"
                        else -> "Off"
                    },
                    icon = when {
                        quickSettingsState.isWifiOn -> Icons.Default.Wifi
                        quickSettingsState.isCellularOn || quickSettingsState.isInternetConnected -> Icons.Default.SignalCellularAlt
                        else -> Icons.Default.WifiOff
                    },
                    isActive = quickSettingsState.isWifiOn || quickSettingsState.isCellularOn || quickSettingsState.isInternetConnected,
                    onClick = { quickSettingsState.toggleWifi() },
                    onLongClick = { quickSettingsState.openWifiSettings() }
                ),
                "bluetooth" to QuickTileData(
                    id = "bluetooth",
                    title = "Bluetooth",
                    subtitle = if (quickSettingsState.isBtOn) "On" else "Off",
                    icon = if (quickSettingsState.isBtOn) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    isActive = quickSettingsState.isBtOn,
                    onClick = { quickSettingsState.toggleBluetooth() },
                    onLongClick = { quickSettingsState.openBluetoothSettings() }
                ),
                "quick_share" to QuickTileData(
                    id = "quick_share",
                    title = "Quick Share",
                    subtitle = "Nearby sharing",
                    icon = Icons.Default.NearMe,
                    isActive = false,
                    onClick = { quickSettingsState.launchQuickShare() },
                    onLongClick = { quickSettingsState.openQuickShareSettings() }
                ),
                "dnd" to QuickTileData(
                    id = "dnd",
                    title = "Do Not Disturb",
                    subtitle = if (quickSettingsState.isDndOn) "On" else "Off",
                    icon = Icons.Default.DoNotDisturb,
                    isActive = quickSettingsState.isDndOn,
                    onClick = { quickSettingsState.toggleDnd() },
                    onLongClick = { quickSettingsState.openDndSettings() }
                ),
                "auto_rotate" to QuickTileData(
                    id = "auto_rotate",
                    title = "Auto-rotate",
                    subtitle = if (quickSettingsState.isAutoRotateOn) "On" else "Off",
                    icon = Icons.Default.ScreenRotation,
                    isActive = quickSettingsState.isAutoRotateOn,
                    onClick = { quickSettingsState.toggleAutoRotate() },
                    onLongClick = { quickSettingsState.openAutoRotateSettings() }
                ),
                "location" to QuickTileData(
                    id = "location",
                    title = "Location",
                    subtitle = if (quickSettingsState.isLocationOn) "On" else "Off",
                    icon = if (quickSettingsState.isLocationOn) Icons.Default.LocationOn else Icons.Default.LocationOff,
                    isActive = quickSettingsState.isLocationOn,
                    onClick = { quickSettingsState.toggleLocation() },
                    onLongClick = { quickSettingsState.openLocationSettings() }
                ),
                "flashlight" to QuickTileData(
                    id = "flashlight",
                    title = "Flashlight",
                    subtitle = if (quickSettingsState.isTorchOn) "On" else "Off",
                    icon = if (quickSettingsState.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    isActive = quickSettingsState.isTorchOn,
                    onClick = { quickSettingsState.toggleFlashlight() },
                    onLongClick = { quickSettingsState.openCameraSettings() }
                )
            )
        }

        // Resolve ordered and visible tiles
        val orderedKeys = remember(tileOrder) {
            val list = tileOrder.filter { masterTiles.containsKey(it) }.toMutableList()
            // Append missing default keys if any
            for (k in DEFAULT_QUICK_TILES_ORDER) {
                if (!list.contains(k)) list.add(k)
            }
            list
        }

        val visibleTiles = remember(orderedKeys, hiddenTiles, masterTiles) {
            orderedKeys.filter { !hiddenTiles.contains(it) }.mapNotNull { masterTiles[it] }
        }

        Surface(
            modifier = modifier,
            shape = panelShape,
            color = drawerBackgroundColor,
            border = drawerBorder,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isLandscape) Modifier.statusBarsPadding() else Modifier)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Drag handle & Compact Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quick Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Manage quick tiles layout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable Grid of Stock Android Tiles (2 Columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleTiles, key = { it.id }) { tile ->
                        StockAndroidQuickTile(tile = tile)
                    }
                }
            }
        }

        if (showEditDialog) {
            ManageTilesDialog(
                currentOrder = orderedKeys,
                hiddenTiles = hiddenTiles,
                masterTiles = masterTiles,
                onDismiss = { showEditDialog = false },
                onSave = { newOrder, newHidden ->
                    onUpdateTileOrder(newOrder)
                    onUpdateHiddenTiles(newHidden)
                    showEditDialog = false
                },
                onReset = {
                    onUpdateTileOrder(DEFAULT_QUICK_TILES_ORDER)
                    onUpdateHiddenTiles(emptySet())
                    showEditDialog = false
                }
            )
        }
    }
}

/**
 * Stock Android Quick Settings Tile:
 * Rectangle with rounded/curved edges, icon on left, title & state on right.
 * Short tap: activates or deactivates with tactile press & toggle animations.
 * Long press: opens system settings page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StockAndroidQuickTile(
    tile: QuickTileData,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press scale bounce animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tilePressScale"
    )

    // Animated icon scale and rotation pop when active state toggles
    val iconScale by animateFloatAsState(
        targetValue = if (tile.isActive) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tileIconScale"
    )

    val activeContainerColor = MaterialTheme.colorScheme.primary
    val inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    val containerColor by animateColorAsState(
        targetValue = if (tile.isActive) activeContainerColor else inactiveContainerColor,
        animationSpec = tween(durationMillis = 180),
        label = "tileContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (tile.isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 180),
        label = "tileContentColor"
    )

    val subtitleColor by animateColorAsState(
        targetValue = if (tile.isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 180),
        label = "tileSubtitleColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = tile.onClick,
                onLongClick = tile.onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.title,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tile.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = tile.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Dialog allowing user to customize and manage Quick Settings tiles layout:
 * reorder tiles up/down, toggle visibility, or reset to default.
 */
@Composable
private fun ManageTilesDialog(
    currentOrder: List<String>,
    hiddenTiles: Set<String>,
    masterTiles: Map<String, QuickTileData>,
    onDismiss: () -> Unit,
    onSave: (List<String>, Set<String>) -> Unit,
    onReset: () -> Unit
) {
    var order by remember { mutableStateOf(currentOrder.toMutableList()) }
    var hidden by remember { mutableStateOf(hiddenTiles.toMutableSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Tiles Layout", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(order) { index, key ->
                    val tile = masterTiles[key]
                    if (tile != null) {
                        val isVisible = !hidden.contains(key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isVisible,
                                onCheckedChange = { checked ->
                                    val newHidden = hidden.toMutableSet()
                                    if (checked) newHidden.remove(key) else newHidden.add(key)
                                    hidden = newHidden
                                }
                            )

                            Icon(
                                imageVector = tile.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = tile.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            // Move Up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newOrder = order.toMutableList()
                                        val temp = newOrder[index]
                                        newOrder[index] = newOrder[index - 1]
                                        newOrder[index - 1] = temp
                                        order = newOrder
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Move Down
                            IconButton(
                                onClick = {
                                    if (index < order.size - 1) {
                                        val newOrder = order.toMutableList()
                                        val temp = newOrder[index]
                                        newOrder[index] = newOrder[index + 1]
                                        newOrder[index + 1] = temp
                                        order = newOrder
                                    }
                                },
                                enabled = index < order.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(order, hidden) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun Context.findActivity(): Activity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Controller holder that monitors and controls hardware & system features:
 * Flashlight, Auto-rotate, Screen Orientation, Wi-Fi, Bluetooth, DND, Location, Quick Share.
 */
class QuickSettingsState(
    val context: Context,
    val isLandscape: Boolean,
    val isAutoRotateOn: Boolean,
    val isTorchOn: Boolean,
    val isWifiOn: Boolean,
    val wifiSsid: String?,
    val isCellularOn: Boolean,
    val isInternetConnected: Boolean,
    val isBtOn: Boolean,
    val isDndOn: Boolean,
    val isLocationOn: Boolean,
    private val onToggleAutoRotate: () -> Unit,
    private val onToggleTorch: () -> Unit,
    private val onToggleWifi: () -> Unit,
    private val onToggleBt: () -> Unit,
    private val onToggleDnd: () -> Unit,
    private val onToggleRotate: () -> Unit,
    private val onToggleLocation: () -> Unit
) {
    fun toggleAutoRotate() = onToggleAutoRotate()
    fun toggleFlashlight() = onToggleTorch()
    fun toggleWifi() = onToggleWifi()
    fun toggleBluetooth() = onToggleBt()
    fun toggleDnd() = onToggleDnd()
    fun toggleRotate() = onToggleRotate()
    fun toggleLocation() = onToggleLocation()

    fun openDisplaySettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun openWifiSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun openBluetoothSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun launchQuickShare() {
        val shareIntents = listOf(
            Intent("com.google.android.gms.nearby.sharing.ACTION_QUICK_SHARE"),
            Intent("android.settings.QUICK_SHARE_SETTINGS"),
            Intent().setComponent(ComponentName("com.google.android.gms", "com.google.android.gms.nearby.sharing.ShareSheetActivity")),
            Intent().setComponent(ComponentName("com.google.android.gms", "com.google.android.gms.nearby.sharing.DirectShareActivity")),
            Intent("com.google.android.gms.settings.NEARBY_SHARING"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )
        var launched = false
        for (intent in shareIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                launched = true
                break
            } catch (_: Exception) {}
        }
        if (!launched) {
            Toast.makeText(context, "Quick Share not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun openQuickShareSettings() {
        val settingsIntents = listOf(
            Intent("android.settings.QUICK_SHARE_SETTINGS"),
            Intent("com.google.android.gms.settings.NEARBY_SHARING"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        )
        for (intent in settingsIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                break
            } catch (_: Exception) {}
        }
    }

    fun openDndSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.onFailure {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    fun openAutoRotateSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_AUTO_ROTATE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.onFailure {
            openDisplaySettings()
        }
    }

    fun openLocationSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun openCameraSettings() {
        runCatching {
            context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}

@Composable
fun rememberQuickSettingsState(context: Context): QuickSettingsState {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 1. Auto Rotate
    var isAutoRotateOn by remember {
        mutableStateOf(
            runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
            }.getOrDefault(false)
        )
    }

    DisposableEffect(context) {
        val uri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isAutoRotateOn = runCatching {
                    Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                }.getOrDefault(false)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(uri, false, observer)
        }
        onDispose {
            runCatching { context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    // 2. Flashlight (Torch)
    val cameraManager = remember {
        runCatching { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }.getOrNull()
    }
    val cameraId = remember(cameraManager) {
        runCatching {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: "0"
        }.getOrDefault("0")
    }
    var isTorchOn by remember { mutableStateOf(false) }

    DisposableEffect(cameraManager, cameraId) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    isTorchOn = enabled
                }
            }
        }
        runCatching {
            cameraManager?.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
        }
        onDispose {
            runCatching { cameraManager?.unregisterTorchCallback(callback) }
        }
    }

    // 3. Connectivity (Wi-Fi & Mobile Data / Internet)
    val wifiManager = remember {
        runCatching { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }.getOrNull()
    }
    val connectivityManager = remember {
        runCatching { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }.getOrNull()
    }
    var isWifiOn by remember { mutableStateOf(wifiManager?.isWifiEnabled == true) }
    var wifiSsid by remember { mutableStateOf<String?>(null) }
    var isCellularOn by remember {
        mutableStateOf(
            runCatching {
                val caps = connectivityManager?.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            }.getOrDefault(false)
        )
    }
    var isInternetConnected by remember {
        mutableStateOf(
            runCatching {
                val caps = connectivityManager?.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            }.getOrDefault(false)
        )
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                isWifiOn = wifiManager?.isWifiEnabled == true
            }
        }
        val filter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        runCatching { context.registerReceiver(receiver, filter) }

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isWifiOn = wifiManager?.isWifiEnabled == true
                val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val hasCell = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                isCellularOn = hasCell
                isInternetConnected = hasInternet

                if (hasWifi) {
                    val info = wifiManager?.connectionInfo
                    val rawSsid = info?.ssid?.replace("\"", "")
                    wifiSsid = if (rawSsid != null && rawSsid != "<unknown ssid>" && rawSsid.isNotBlank()) rawSsid else null
                } else {
                    wifiSsid = null
                }
            }

            override fun onLost(network: Network) {
                isWifiOn = wifiManager?.isWifiEnabled == true
                val activeCaps = connectivityManager?.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                isCellularOn = activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                isInternetConnected = activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) {
                    wifiSsid = null
                }
            }
        }
        runCatching { connectivityManager?.registerDefaultNetworkCallback(networkCallback) }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        }
    }

    // 4. Bluetooth
    val bluetoothAdapter = remember {
        runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
    }
    var isBtOn by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                isBtOn = bluetoothAdapter?.isEnabled == true
            }
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // 5. DND
    val notificationManager = remember {
        runCatching { context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager }.getOrNull()
    }
    var isDndOn by remember {
        mutableStateOf(notificationManager?.let { it.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL } ?: false)
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                isDndOn = notificationManager?.let { it.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL } ?: false
            }
        }
        val filter = IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // 6. Location
    val locationManager = remember {
        runCatching { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }.getOrNull()
    }
    var isLocationOn by remember {
        mutableStateOf(locationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false)
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                isLocationOn = locationManager?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return QuickSettingsState(
        context = context,
        isLandscape = isLandscape,
        isAutoRotateOn = isAutoRotateOn,
        isTorchOn = isTorchOn,
        isWifiOn = isWifiOn,
        wifiSsid = wifiSsid,
        isCellularOn = isCellularOn,
        isInternetConnected = isInternetConnected,
        isBtOn = isBtOn,
        isDndOn = isDndOn,
        isLocationOn = isLocationOn,
        onToggleAutoRotate = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
                Toast.makeText(context, "Grant 'Modify system settings' (long press tile) to toggle", Toast.LENGTH_SHORT).show()
            } else {
                val next = if (isAutoRotateOn) 0 else 1
                val success = runCatching {
                    Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, next)
                }.isSuccess
                if (success) {
                    isAutoRotateOn = next == 1
                }
            }
        },
        onToggleTorch = {
            if (cameraManager != null) {
                runCatching {
                    val next = !isTorchOn
                    cameraManager.setTorchMode(cameraId, next)
                    isTorchOn = next
                }.onFailure {
                    Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onToggleWifi = {
            var toggled: Boolean = runCatching {
                wifiManager?.setWifiEnabled(!isWifiOn) ?: false
            }.getOrDefault(false)

            if (!toggled) {
                toggled = runCatching {
                    val cr = context.contentResolver
                    val next = if (isWifiOn) 0 else 1
                    Settings.Global.putInt(cr, "wifi_on", next)
                }.getOrDefault(false)
            }

            if (toggled) {
                isWifiOn = !isWifiOn
            } else {
                // If modern Android restricted direct toggle without root/secure permission, show in-place panel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val internetPanel = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val chosen = if (panelIntent.resolveActivity(context.packageManager) != null) panelIntent else internetPanel
                    runCatching { context.startActivity(chosen) }
                } else {
                    Toast.makeText(context, "Cannot change Wi-Fi state", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onToggleBt = {
            val bt = bluetoothAdapter
            if (bt != null) {
                val currentlyOn = bt.isEnabled
                var success = false

                // Try direct call
                try {
                    @Suppress("DEPRECATION")
                    success = if (currentlyOn) bt.disable() else bt.enable()
                } catch (_: Exception) {}

                // Try reflection
                if (!success) {
                    try {
                        val methodName = if (currentlyOn) "disable" else "enable"
                        val method = bt.javaClass.getMethod(methodName)
                        val res = method.invoke(bt) as? Boolean
                        success = res == true
                    } catch (_: Exception) {}
                }

                if (success) {
                    isBtOn = !currentlyOn
                } else {
                    if (!currentlyOn) {
                        // Prompt in-place system dialog (does NOT open Settings)
                        runCatching {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(enableBtIntent)
                        }.onFailure {
                            Toast.makeText(context, "Cannot toggle Bluetooth", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Cannot disable Bluetooth without system permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onToggleDnd = {
            val nm = notificationManager
            if (nm != null) {
                if (nm.isNotificationPolicyAccessGranted) {
                    val current = nm.currentInterruptionFilter
                    val next = if (current == NotificationManager.INTERRUPTION_FILTER_ALL) {
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    } else {
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    }
                    nm.setInterruptionFilter(next)
                    isDndOn = next != NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    Toast.makeText(context, "Grant DND permission (long press tile) to toggle", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onToggleRotate = {
            val activity = context.findActivity()
            if (activity != null) {
                val target = if (isLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                activity.requestedOrientation = target
            } else {
                Toast.makeText(context, "Cannot rotate screen", Toast.LENGTH_SHORT).show()
            }
        },
        onToggleLocation = {
            val cr = context.contentResolver
            val nextState = !isLocationOn
            var toggled = false

            // Try LocationManagerCompat / LocationManager reflection
            try {
                if (locationManager != null) {
                    val method = locationManager.javaClass.getMethod("setLocationEnabledForUser", Boolean::class.javaPrimitiveType, android.os.UserHandle::class.java)
                    val myUserHandle = android.os.Process.myUserHandle()
                    method.invoke(locationManager, nextState, myUserHandle)
                    toggled = true
                }
            } catch (_: Exception) {}

            // Try Secure Settings LOCATION_MODE
            if (!toggled) {
                try {
                    val mode = if (nextState) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF
                    toggled = Settings.Secure.putInt(cr, Settings.Secure.LOCATION_MODE, mode)
                } catch (_: Exception) {}
            }

            // Try providers allowed
            if (!toggled) {
                try {
                    val providerStr = if (nextState) "+gps,+network" else "-gps,-network"
                    toggled = Settings.Secure.putString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED, providerStr)
                } catch (_: Exception) {}
            }

            if (toggled) {
                isLocationOn = nextState
            } else {
                Toast.makeText(context, "Location requires system settings (long press tile)", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
