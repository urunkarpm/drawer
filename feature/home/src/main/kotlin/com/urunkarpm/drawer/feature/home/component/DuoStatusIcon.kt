package com.urunkarpm.drawer.feature.home.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * Snapshot of device system status: Battery, Wi-Fi, and Cellular signal strength.
 */
@Immutable
data class DuoSystemStatus(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val isWifiConnected: Boolean = false,
    val wifiLevel: Int = 0, // 0..4
    val isCellularConnected: Boolean = false,
    val cellularLevel: Int = 0 // 0..4
)

/**
 * Unified Duo Status Icon inspired by iOS 27 on iPhone Duo.
 * Consolidates battery (upper circle arc), Wi-Fi (central signal waves), and cellular signal
 * (bottom curved status dots) into a single cohesive, circular graphic matching reference design.
 * All elements render in the same uniform contrast color.
 */
@Composable
fun DuoStatusIcon(
    displayColor: Color,
    modifier: Modifier = Modifier,
    sizeDp: Int = 46,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val status = rememberDuoSystemStatus(context)

    // Animated battery progress sweep angle
    val animatedProgress by animateFloatAsState(
        targetValue = (status.batteryPercent.coerceIn(0, 100) / 100f),
        label = "batteryProgress"
    )

    // Gentle breathing pulse animation for charging glow
    val isCharging = status.isCharging
    val infiniteTransition = rememberInfiniteTransition(label = "chargingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulseAlpha"
    )

    // Disconnected Alert Red Glow cycle:
    // Glow blooms and gently fades over 16 seconds ("takes its sweet time to vanish"),
    // followed by a 30 second rest period before the next glow cycle begins (total duration = 46,000 ms).
    val redGlowTransition = rememberInfiniteTransition(label = "disconnectedRedGlow")
    val redGlowFactor by redGlowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 46000 // 16s animation + 30s idle delay
                0f at 0 using FastOutSlowInEasing
                1f at 2500 using LinearEasing // Smoothly swells to peak glow in 2.5s
                0.60f at 7000 using LinearEasing // Lingering rich glow
                0.20f at 12000 using LinearEasing // Soft fading presence
                0f at 16000 using LinearEasing // Completely vanished at 16s
                0f at 46000 // Silent for next 30s until next cycle
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "redGlowFactor"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val canvasSize = size.minDimension
            val s = canvasSize / 250.0f
            val centerX = size.width / 2f
            val centerY = size.height * 0.492f

            // All elements share the exact same uniform contrast color when active, except battery glows green when charging
            val activeColor = displayColor
            val inactiveColor = displayColor.copy(alpha = 0.22f)

            // Luminous alert red for disconnected states:
            // Crisp solid red base (slightly dim when glow is off, full vibrant when glowing)
            // Plus an expansive, slow-fading luminous aura driven by redGlowFactor (16s fade, 30s rest)
            val baseRedAlpha = 0.55f + 0.45f * redGlowFactor
            val alertRed = Color(0xFFFF3B30).copy(alpha = baseRedAlpha)
            val glowRedOuter = Color(0xFFFF3B30).copy(alpha = 0.45f * redGlowFactor)
            val glowRedInner = Color(0xFFFF453A).copy(alpha = 0.65f * redGlowFactor)

            val chargingGreen = Color(0xFF34C759)
            val activeBatteryColor = if (isCharging) chargingGreen else activeColor
            val inactiveBatteryColor = if (isCharging) chargingGreen.copy(alpha = 0.20f) else inactiveColor

            // 1. BATTERY: Outer circular arc (sweeping 242° from 149° clockwise to 31°)
            val outerRadius = 117.0f * s
            val outerStroke = 18.0f * s
            val startAngle = 149.0f
            val totalBatteryArc = 242.0f
            val arcTopLeft = Offset(centerX - outerRadius, centerY - outerRadius)
            val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

            // Inactive battery track
            drawArc(
                color = inactiveBatteryColor,
                startAngle = startAngle,
                sweepAngle = totalBatteryArc,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )

            // Active battery level arc (with multi-layer luminous green glow when charging)
            val activeSweep = (animatedProgress * totalBatteryArc).coerceIn(0f, totalBatteryArc)
            if (activeSweep > 0f) {
                if (isCharging) {
                    // Outer diffuse glow halo
                    drawArc(
                        color = chargingGreen.copy(alpha = 0.25f * pulseAlpha),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = outerStroke * 2.2f, cap = StrokeCap.Round)
                    )
                    // Inner focused glow halo
                    drawArc(
                        color = chargingGreen.copy(alpha = 0.45f * pulseAlpha),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = outerStroke * 1.5f, cap = StrokeCap.Round)
                    )
                }

                // Crisp foreground battery arc
                drawArc(
                    color = activeBatteryColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                )
            }

            // 2. CELLULAR: 4 reception dots following the bottom curve of the circle
            // When disconnected, show in glowing red; when connected, show signal levels normally
            val dotOffsets = listOf(
                Offset(-58.0f * s, 92.5f * s),
                Offset(-19.0f * s, 108.5f * s),
                Offset(20.0f * s, 109.5f * s),
                Offset(60.5f * s, 97.5f * s)
            )
            val dotRadius = 12.0f * s

            for (i in dotOffsets.indices) {
                val offset = dotOffsets[i]
                val dotCenter = Offset(centerX + offset.x, centerY + offset.y)
                if (!status.isCellularConnected) {
                    // Cellular disconnected: Render all dots in glowing red
                    if (redGlowFactor > 0f) {
                        // Outer expansive glow halo
                        drawCircle(
                            color = glowRedOuter,
                            radius = dotRadius * 1.9f,
                            center = dotCenter
                        )
                        // Inner focused glow halo
                        drawCircle(
                            color = glowRedInner,
                            radius = dotRadius * 1.4f,
                            center = dotCenter
                        )
                    }
                    drawCircle(
                        color = alertRed,
                        radius = dotRadius,
                        center = dotCenter
                    )
                } else {
                    val isFilled = status.cellularLevel >= (i + 1)
                    drawCircle(
                        color = if (isFilled) activeColor else inactiveColor,
                        radius = dotRadius,
                        center = dotCenter
                    )
                }
            }

            // 3. WI-FI: Concentric waves and wedge
            // When disconnected: show full waves and wedge in rich glowing red instead of disappearing.
            // When connected: render active/inactive levels based on signal strength.
            val wifiOriginX = centerX - 0.5f * s
            val wifiOriginY = centerY + 40.0f * s

            val isBaseActive = status.isWifiConnected && (status.wifiLevel >= 1)
            val isMidActive = status.isWifiConnected && (status.wifiLevel >= 2)
            val isTopActive = status.isWifiConnected && (status.wifiLevel >= 3)

            val rTop = 69.0f * s
            val strokeTop = 15.0f * s
            val rMid = 40.5f * s
            val strokeMid = 14.0f * s
            val rWedge = 18.0f * s

            val wedgePath = Path().apply {
                val startRad = Math.toRadians(226.0)
                val endRad = Math.toRadians(314.0)
                val pStartX = (wifiOriginX + rWedge * Math.cos(startRad)).toFloat()
                val pStartY = (wifiOriginY + rWedge * Math.sin(startRad)).toFloat()
                val pEndX = (wifiOriginX + rWedge * Math.cos(endRad)).toFloat()
                val pEndY = (wifiOriginY + rWedge * Math.sin(endRad)).toFloat()

                moveTo(pStartX, pStartY)
                arcTo(
                    rect = Rect(
                        left = wifiOriginX - rWedge,
                        top = wifiOriginY - rWedge,
                        right = wifiOriginX + rWedge,
                        bottom = wifiOriginY + rWedge
                    ),
                    startAngleDegrees = 226f,
                    sweepAngleDegrees = 88f,
                    forceMoveTo = false
                )
                quadraticBezierTo(
                    x1 = wifiOriginX + 6.0f * s,
                    y1 = wifiOriginY + 2.0f * s,
                    x2 = wifiOriginX,
                    y2 = wifiOriginY + 7.5f * s
                )
                quadraticBezierTo(
                    x1 = wifiOriginX - 6.0f * s,
                    y1 = wifiOriginY + 2.0f * s,
                    x2 = pStartX,
                    y2 = pStartY
                )
                close()
            }

            if (!status.isWifiConnected) {
                // Wi-Fi disconnected / off: Multi-layer luminous glow halos (16s linger, 30s pause)
                if (redGlowFactor > 0f) {
                    // Outer diffuse glow halo
                    drawArc(
                        color = glowRedOuter,
                        startAngle = 226.0f,
                        sweepAngle = 88.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop),
                        size = Size(rTop * 2f, rTop * 2f),
                        style = Stroke(width = strokeTop * 2.2f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = glowRedOuter,
                        startAngle = 225.0f,
                        sweepAngle = 90.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid),
                        size = Size(rMid * 2f, rMid * 2f),
                        style = Stroke(width = strokeMid * 2.2f, cap = StrokeCap.Round)
                    )
                    // Inner focused glow halo
                    drawArc(
                        color = glowRedInner,
                        startAngle = 226.0f,
                        sweepAngle = 88.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop),
                        size = Size(rTop * 2f, rTop * 2f),
                        style = Stroke(width = strokeTop * 1.5f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = glowRedInner,
                        startAngle = 225.0f,
                        sweepAngle = 90.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid),
                        size = Size(rMid * 2f, rMid * 2f),
                        style = Stroke(width = strokeMid * 1.5f, cap = StrokeCap.Round)
                    )
                }

                // Crisp glowing alert red foreground Wi-Fi arcs & wedge
                drawArc(
                    color = alertRed,
                    startAngle = 226.0f,
                    sweepAngle = 88.0f,
                    useCenter = false,
                    topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop),
                    size = Size(rTop * 2f, rTop * 2f),
                    style = Stroke(width = strokeTop, cap = StrokeCap.Round)
                )
                drawArc(
                    color = alertRed,
                    startAngle = 225.0f,
                    sweepAngle = 90.0f,
                    useCenter = false,
                    topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid),
                    size = Size(rMid * 2f, rMid * 2f),
                    style = Stroke(width = strokeMid, cap = StrokeCap.Round)
                )
                drawPath(
                    path = wedgePath,
                    color = alertRed
                )
            } else {
                // Top wave arc: r=69, stroke=15, sweep=88° (226° to 314°)
                drawArc(
                    color = if (isTopActive) activeColor else inactiveColor,
                    startAngle = 226.0f,
                    sweepAngle = 88.0f,
                    useCenter = false,
                    topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop),
                    size = Size(rTop * 2f, rTop * 2f),
                    style = Stroke(width = strokeTop, cap = StrokeCap.Round)
                )

                // Middle wave arc: r=40.5, stroke=14, sweep=90° (225° to 315°)
                drawArc(
                    color = if (isMidActive) activeColor else inactiveColor,
                    startAngle = 225.0f,
                    sweepAngle = 90.0f,
                    useCenter = false,
                    topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid),
                    size = Size(rMid * 2f, rMid * 2f),
                    style = Stroke(width = strokeMid, cap = StrokeCap.Round)
                )

                // Base wedge / dot
                drawPath(
                    path = wedgePath,
                    color = if (isBaseActive) activeColor else inactiveColor
                )
            }
        }
    }

}

/**
 * State holder that monitors live Battery, Wi-Fi, and Cellular conditions.
 */
@Composable
fun rememberDuoSystemStatus(context: Context): DuoSystemStatus {
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var isWifiConnected by remember { mutableStateOf(false) }
    var wifiLevel by remember { mutableIntStateOf(0) }
    var isCellularConnected by remember { mutableStateOf(false) }
    var cellularLevel by remember { mutableIntStateOf(0) }

    // 1. Observe Battery
    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let { updateBattery(it) }
            }

            private fun updateBattery(intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPercent = if (level >= 0 && scale > 0) {
                    ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
                } else 100

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }

        val stickyIntent = context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        stickyIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryPercent = if (level >= 0 && scale > 0) {
                ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
            } else 100
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }

        onDispose {
            runCatching { context.unregisterReceiver(batteryReceiver) }
        }
    }

    // 2. Observe Wi-Fi and Connectivity
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        fun queryWifi() {
            runCatching {
                val activeNet = cm?.activeNetwork
                val caps = cm?.getNetworkCapabilities(activeNet)
                val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                isWifiConnected = hasWifi

                if (hasWifi && wm != null) {
                    val rssi = wm.connectionInfo?.rssi ?: -127
                    wifiLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        wm.calculateSignalLevel(rssi).coerceIn(0, 4)
                    } else {
                        @Suppress("DEPRECATION")
                        WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
                    }
                } else {
                    wifiLevel = 0
                }

                val hasCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                isCellularConnected = hasCell || isCellularConnected
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                queryWifi()
            }

            override fun onLost(network: Network) {
                queryWifi()
            }
        }

        runCatching {
            cm?.registerDefaultNetworkCallback(callback)
            queryWifi()
        }

        onDispose {
            runCatching { cm?.unregisterNetworkCallback(callback) }
        }
    }

    // 3. Observe Cellular Signal Quality
    DisposableEffect(context) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        fun updateFromSignal(signalStrength: SignalStrength?) {
            val level = signalStrength?.level?.coerceIn(0, 4) ?: 0
            cellularLevel = level
            if (level > 0) isCellularConnected = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    updateFromSignal(signalStrength)
                }
            }
            runCatching {
                tm?.registerTelephonyCallback(context.mainExecutor, callback)
                updateFromSignal(tm?.signalStrength)
            }
            onDispose {
                runCatching { tm?.unregisterTelephonyCallback(callback) }
            }
        } else {
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    updateFromSignal(signalStrength)
                }
            }
            runCatching {
                @Suppress("DEPRECATION")
                tm?.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    updateFromSignal(tm?.signalStrength)
                }
            }
            onDispose {
                runCatching {
                    @Suppress("DEPRECATION")
                    tm?.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
    }

    return DuoSystemStatus(
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        isWifiConnected = isWifiConnected,
        wifiLevel = wifiLevel,
        isCellularConnected = isCellularConnected,
        cellularLevel = cellularLevel
    )
}
