package com.urunkarpm.drawer.feature.home.component

import android.content.BroadcastReceiver
import android.content.Context
import kotlinx.coroutines.delay
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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.lerp
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
import androidx.compose.ui.draw.shadow
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
import android.os.SystemClock
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Snapshot of device system status: Battery, Wi-Fi, Cellular signal strength, and charging metrics.
 */
@Immutable
data class DuoSystemStatus(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val chargingWatts: Float? = null,
    val batteryTempCelsius: Float? = null,
    val isWifiConnected: Boolean = false,
    val wifiLevel: Int = 0, // 0..4
    val isCellularConnected: Boolean = false,
    val cellularLevel: Int = 0 // 0..4
)

// ponytail: Instantaneous wattage calculated via BatteryManager CURRENT_NOW * EXTRA_VOLTAGE with microamp/milliamp auto-scaling; ceiling: hardware driver support varies across OEM chipsets; upgrade path: Android 14+ BatteryState/Health HAL services.
private fun readBatteryChargingStats(
    context: Context,
    intent: Intent?,
    isCharging: Boolean
): Pair<Float?, Float?> {
    val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val tempCelsius = if (tempRaw > 0) tempRaw / 10f else null

    if (!isCharging) return null to tempCelsius

    val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    val currentNow = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
    val currentAvg = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: 0
    val current = if (currentNow != 0 && currentNow != Int.MIN_VALUE) currentNow else currentAvg
    val absCurrent = kotlin.math.abs(current)

    val amps = when {
        absCurrent > 20_000 -> absCurrent / 1_000_000f // microamperes (µA)
        absCurrent > 0 -> absCurrent / 1_000f         // milliamperes (mA) fallback
        else -> 0f
    }
    val volts = if (voltageMv > 0) voltageMv / 1000f else 0f
    val watts = volts * amps
    val chargingWatts = if (watts in 0.1f..240f) watts else null

    return chargingWatts to tempCelsius
}

// ponytail: Lightweight HttpURLConnection speed test streaming 2MB from Cloudflare Edge without third-party SDKs; ceiling: single-threaded download burst test rather than multi-connection full-saturation test; upgrade path: full WebSocket/Multi-stream NDT7 or Ookla speed test engine.
private suspend fun executeQuickSpeedTest(): String = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://speed.cloudflare.com/__down?bytes=2000000")
        connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 3000
            readTimeout = 3500
            setRequestProperty("User-Agent", "DrawerLauncher/1.0")
            useCaches = false
            defaultUseCaches = false
        }
        connection.connect()
        val startTime = SystemClock.elapsedRealtime()
        var totalBytes = 0L
        val buffer = ByteArray(8192)
        connection.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                totalBytes += read
                if (SystemClock.elapsedRealtime() - startTime > 3500) break
            }
        }
        val durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(80L)
        val durationSec = durationMs / 1000.0
        val bits = totalBytes * 8.0
        val bps = bits / durationSec
        val mbps = bps / 1_000_000.0
        if (mbps >= 1.0) {
            String.format(Locale.US, "%.1f Mbps", mbps)
        } else if (bps > 0) {
            String.format(Locale.US, "%.0f Kbps", (bps / 1000.0).coerceAtLeast(0.0))
        } else {
            "Unavailable"
        }
    } catch (_: Exception) {
        "Failed"
    } finally {
        connection?.disconnect()
    }
}

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
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    duoStatusPosition: String = "TEMP"
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val status = rememberDuoSystemStatus(context)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isCenterPosition = duoStatusPosition.equals("CENTER", ignoreCase = true)
    val isTimePosition = duoStatusPosition.equals("TIME", ignoreCase = true)

    // Animated battery progress sweep angle & linear bar
    val animatedProgress by animateFloatAsState(
        targetValue = (status.batteryPercent.coerceIn(0, 100) / 100f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "duoBatteryProgress"
    )

    // Fluid spring physics for tactile morphing expansion
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "duoExpandProgress"
    )

    // Press scale micro-interaction
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "duoPressScale"
    )

    // Gentle breathing pulse animation for charging glow (active ONLY when actively charging)
    val isCharging = status.isCharging
    val chargingTransition = if (isCharging) rememberInfiniteTransition(label = "chargingPulse") else null
    val pulseAlpha = chargingTransition?.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulseAlpha"
    )

    val scope = rememberCoroutineScope()
    // ponytail: SharedPreferences used directly for instantaneous last recorded speed without DataStore flow overhead; ceiling: main-thread disk read on initial load; upgrade path: DataStore Preferences flow.
    val prefs = remember(context) { context.getSharedPreferences("duo_status_prefs", Context.MODE_PRIVATE) }
    var lastRecordedSpeed by remember { mutableStateOf(prefs.getString("last_speed", null)) }
    var isSpeedTesting by remember { mutableStateOf(false) }
    var speedTestTriggeredThisSession by remember { mutableStateOf(false) }
    var timerResetKey by remember { mutableIntStateOf(0) }

    var liveWatts by remember { mutableStateOf<Float?>(status.chargingWatts) }
    var liveTemp by remember { mutableStateOf<Float?>(status.batteryTempCelsius) }

    LaunchedEffect(status.chargingWatts, status.batteryTempCelsius) {
        liveWatts = status.chargingWatts
        liveTemp = status.batteryTempCelsius
    }

    LaunchedEffect(isExpanded, status.isCharging) {
        if (isExpanded && status.isCharging) {
            while (true) {
                val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val (w, t) = readBatteryChargingStats(context, sticky, true)
                if (w != null) liveWatts = w
                if (t != null) liveTemp = t
                delay(1500L)
            }
        }
    }

    fun triggerSpeedTest() {
        if (isSpeedTesting) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (!status.isWifiConnected && !status.isCellularConnected) {
            lastRecordedSpeed = "Offline"
            prefs.edit().putString("last_speed", "Offline").apply()
            return
        }
        isSpeedTesting = true
        speedTestTriggeredThisSession = true
        timerResetKey++
        scope.launch {
            val result = executeQuickSpeedTest()
            isSpeedTesting = false
            if (result != "Failed" && result != "Unavailable") {
                lastRecordedSpeed = result
                prefs.edit().putString("last_speed", result).apply()
            } else {
                lastRecordedSpeed = "Unavailable"
            }
            timerResetKey++
        }
    }

    // Auto-close expanded view: 15s when connected to power or running speed test; 3s otherwise
    LaunchedEffect(isExpanded, timerResetKey, status.isCharging, speedTestTriggeredThisSession) {
        if (isExpanded) {
            val durationMs = if (status.isCharging || speedTestTriggeredThisSession || isSpeedTesting) 15_000L else 3_000L
            delay(durationMs)
            if (!isSpeedTesting) {
                onToggleExpand()
            }
        } else {
            speedTestTriggeredThisSession = false
        }
    }

    val chargingGreen = Color(0xFF34C759)
    val activeColor = displayColor
    val inactiveColor = displayColor.copy(alpha = 0.22f)
    val activeBatteryColor = if (isCharging) chargingGreen else activeColor
    val inactiveBatteryColor = if (isCharging) chargingGreen.copy(alpha = 0.20f) else inactiveColor
    val alertRed = Color(0xFFFF3B30)

    // Morph container dimensions seamlessly
    val baseExpandedHeight = if (status.isCharging) 156.dp else 134.dp
    val currentWidth = lerp(sizeDp.dp, 268.dp, expandProgress)
    val currentHeight = lerp(sizeDp.dp, baseExpandedHeight, expandProgress)
    val currentCornerRadius = lerp((sizeDp / 2f).dp, 20.dp, expandProgress)

    Box(
        modifier = modifier
            .width(currentWidth)
            .height(currentHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                transformOrigin = when {
                    isCenterPosition -> TransformOrigin(0.5f, 0f)
                    isTimePosition -> TransformOrigin(0f, 0f)
                    else -> TransformOrigin(1f, 0f)
                }
            }
            .shadow(
                elevation = 8.dp * expandProgress,
                shape = RoundedCornerShape(currentCornerRadius)
            )
            .clip(RoundedCornerShape(currentCornerRadius))
            .background(
                MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.94f * expandProgress
                )
            )
            .then(
                if (expandProgress > 0.05f) {
                    Modifier.border(
                        width = 1.dp,
                        color = displayColor.copy(alpha = 0.16f * expandProgress),
                        shape = RoundedCornerShape(currentCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!isExpanded) {
                        onToggleExpand()
                    }
                }
            ),
        contentAlignment = when {
            isCenterPosition -> Alignment.TopCenter
            isTimePosition -> Alignment.TopStart
            else -> Alignment.TopEnd
        }
    ) {
        // ── Layer 1: Collapsed Unified Circular Duo Status Icon ──
        if (expandProgress < 0.95f) {
            val iconAlpha = (1f - (expandProgress / 0.40f)).coerceIn(0f, 1f)
            val iconScale = 1f - expandProgress * 0.12f
            val wedgePath = remember { Path() }
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .graphicsLayer {
                        alpha = iconAlpha
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(sizeDp.dp)) {
                    val canvasSize = size.minDimension
                    val s = canvasSize / 250.0f
                    val centerX = size.width / 2f
                    val centerY = size.height * 0.492f

                    // 1. BATTERY: Outer circular arc (sweeping 242° from 149° clockwise to 31°)
                    val outerRadius = 117.0f * s
                    val outerStroke = 18.0f * s
                    val startAngle = 149.0f
                    val totalBatteryArc = 242.0f
                    val arcTopLeft = Offset(centerX - outerRadius, centerY - outerRadius)
                    val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

                    drawArc(
                        color = inactiveBatteryColor,
                        startAngle = startAngle,
                        sweepAngle = totalBatteryArc,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round)
                    )

                    val activeSweep = (animatedProgress * totalBatteryArc).coerceIn(0f, totalBatteryArc)
                    if (activeSweep > 0f) {
                        if (isCharging) {
                            val currentPulse = pulseAlpha?.value ?: 1f
                            drawArc(
                                color = chargingGreen.copy(alpha = 0.25f * currentPulse),
                                startAngle = startAngle,
                                sweepAngle = activeSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = outerStroke * 2.2f, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = chargingGreen.copy(alpha = 0.45f * currentPulse),
                                startAngle = startAngle,
                                sweepAngle = activeSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = outerStroke * 1.5f, cap = StrokeCap.Round)
                            )
                        }
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

                    wedgePath.reset()
                    val startRad = Math.toRadians(226.0)
                    val endRad = Math.toRadians(314.0)
                    val pStartX = (wifiOriginX + rWedge * Math.cos(startRad)).toFloat()
                    val pStartY = (wifiOriginY + rWedge * Math.sin(startRad)).toFloat()

                    wedgePath.moveTo(pStartX, pStartY)
                    wedgePath.arcTo(
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
                    wedgePath.quadraticBezierTo(
                        x1 = wifiOriginX + 6.0f * s,
                        y1 = wifiOriginY + 2.0f * s,
                        x2 = wifiOriginX,
                        y2 = wifiOriginY + 7.5f * s
                    )
                    wedgePath.quadraticBezierTo(
                        x1 = wifiOriginX - 6.0f * s,
                        y1 = wifiOriginY + 2.0f * s,
                        x2 = pStartX,
                        y2 = pStartY
                    )
                    wedgePath.close()

                    if (!status.isWifiConnected) {
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
                        drawArc(
                            color = if (isTopActive) activeColor else inactiveColor,
                            startAngle = 226.0f,
                            sweepAngle = 88.0f,
                            useCenter = false,
                            topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop),
                            size = Size(rTop * 2f, rTop * 2f),
                            style = Stroke(width = strokeTop, cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = if (isMidActive) activeColor else inactiveColor,
                            startAngle = 225.0f,
                            sweepAngle = 90.0f,
                            useCenter = false,
                            topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid),
                            size = Size(rMid * 2f, rMid * 2f),
                            style = Stroke(width = strokeMid, cap = StrokeCap.Round)
                        )

                        drawPath(
                            path = wedgePath,
                            color = if (isBaseActive) activeColor else inactiveColor
                        )
                    }
                }
            }
        }

        // ── Layer 2: Expanded Breakdown 4-Step Card with Rich Staggered Motion ──
        if (expandProgress > 0.05f) {
            // ponytail: charging pulse and morph staggers run strictly in graphicsLayer; ceiling: simple alpha/translate lerp; upgrade path: vector path morphing.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer {
                        alpha = ((expandProgress - 0.20f) / 0.80f).coerceIn(0f, 1f)
                    },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Step 1: BATTERY — Live status with unrolling progress bar and charging metrics
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.20f) / 0.60f).coerceIn(0f, 1f)
                            translationY = (1f - ((ep - 0.20f) / 0.80f).coerceIn(0f, 1f)) * -10f
                        },
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Battery",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = displayColor.copy(alpha = 0.65f),
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "${status.batteryPercent}%${if (status.isCharging) " • Charging" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (status.isCharging) chargingGreen else displayColor
                        )
                    }

                    // Straight battery bar smoothly expanding and filling
                    val barProgress = animatedProgress * ((expandProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(inactiveBatteryColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isCharging) chargingGreen else activeBatteryColor
                                )
                                .graphicsLayer {
                                    if (isCharging) {
                                        val pulse = pulseAlpha?.value ?: 1f
                                        alpha = (0.85f + 0.15f * pulse).coerceIn(0f, 1f)
                                    }
                                }
                        )
                    }

                    if (status.isCharging) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val powerText = liveWatts?.let { String.format(Locale.US, "%.1f W", it) } ?: "Charging"
                            Text(
                                text = "⚡ $powerText",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = chargingGreen
                            )
                            val tempText = liveTemp?.let { String.format(Locale.US, "%.1f°C", it) }
                            if (tempText != null) {
                                Text(
                                    text = "🌡 $tempText",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = displayColor.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // Step 2: WI-FI — Signal waves with live network wave pulse (tap to speed test)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.28f) / 0.60f).coerceIn(0f, 1f)
                            translationX = (1f - ((ep - 0.28f) / 0.68f).coerceIn(0f, 1f)) * 18f
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { triggerSpeedTest() }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wi-Fi",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = displayColor.copy(alpha = 0.65f),
                        letterSpacing = 0.3.sp
                    )

                    Text(
                        text = if (status.isWifiConnected) {
                            when (status.wifiLevel) {
                                4, 3 -> "Excellent"
                                2 -> "Good"
                                1 -> "Fair"
                                else -> "Connected"
                            }
                        } else {
                            "Disconnected"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (status.isWifiConnected) displayColor else alertRed
                    )
                }

                // Step 3: CARRIER SIGNAL — 4 dots unfurling sequentially across horizontal meter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.36f) / 0.56f).coerceIn(0f, 1f)
                            translationX = (1f - ((ep - 0.36f) / 0.56f).coerceIn(0f, 1f)) * 18f
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carrier",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = displayColor.copy(alpha = 0.65f),
                        letterSpacing = 0.3.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 4 dots unfurl sequentially
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..4) {
                                val dotThreshold = 0.36f + (i * 0.06f)
                                val isFilled = status.isCellularConnected && status.cellularLevel >= i
                                val dotColor = if (!status.isCellularConnected) {
                                    alertRed
                                } else if (isFilled) {
                                    activeColor
                                } else {
                                    inactiveColor
                                }
                                Box(
                                    modifier = Modifier
                                        .size(5.5.dp)
                                        .graphicsLayer {
                                            val ep = expandProgress
                                            val dotAnimScale = ((ep - dotThreshold) / 0.28f).coerceIn(0f, 1f)
                                            scaleX = 0.35f + 0.65f * dotAnimScale
                                            scaleY = 0.35f + 0.65f * dotAnimScale
                                            alpha = dotAnimScale
                                        }
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }

                        Text(
                            text = if (status.isCellularConnected) {
                                when (status.cellularLevel) {
                                    4 -> "Strong"
                                    3 -> "Good"
                                    2 -> "Fair"
                                    1 -> "Poor"
                                    else -> "Connected"
                                }
                            } else {
                                "No Service"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (status.isCellularConnected) displayColor else alertRed
                        )
                    }
                }

                // Step 4: SPEED TEST — Quick latency/bandwidth measurement from connected source
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.44f) / 0.56f).coerceIn(0f, 1f)
                            translationX = (1f - ((ep - 0.44f) / 0.56f).coerceIn(0f, 1f)) * 18f
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { triggerSpeedTest() }
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = displayColor.copy(alpha = 0.65f),
                        letterSpacing = 0.3.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val speedText = when {
                            isSpeedTesting -> "Testing..."
                            !lastRecordedSpeed.isNullOrBlank() -> lastRecordedSpeed!!
                            !status.isWifiConnected && !status.isCellularConnected -> "Offline"
                            else -> "Tap to test"
                        }
                        val speedColor = when {
                            isSpeedTesting -> chargingGreen
                            speedText == "Offline" || speedText == "Error" || speedText == "Unavailable" -> alertRed
                            else -> displayColor
                        }

                        Text(
                            text = speedText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = speedColor
                        )
                        if (!isSpeedTesting) {
                            Text(
                                text = "↺",
                                fontSize = 11.sp,
                                color = displayColor.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * State holder that monitors live Battery, Wi-Fi, and Cellular conditions.
 * Tied directly to Activity lifecycle so all broadcast receivers and IPC callbacks
 * are completely unregistered when the screen is off or app is in background,
 * eliminating background battery drain.
 */
@Composable
fun rememberDuoSystemStatus(context: Context): DuoSystemStatus {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var chargingWatts by remember { mutableStateOf<Float?>(null) }
    var batteryTempCelsius by remember { mutableStateOf<Float?>(null) }
    var isWifiConnected by remember { mutableStateOf(false) }
    var wifiLevel by remember { mutableIntStateOf(0) }
    var isCellularConnected by remember { mutableStateOf(false) }
    var cellularLevel by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        var batteryReceiver: BroadcastReceiver? = null
        var networkCallback: ConnectivityManager.NetworkCallback? = null
        var telephonyCallback: Any? = null
        var phoneStateListener: Any? = null

        fun updateBattery(intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryPercent = if (level >= 0 && scale > 0) {
                ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
            } else 100

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val (w, t) = readBatteryChargingStats(context, intent, isCharging)
            chargingWatts = w
            batteryTempCelsius = t
        }

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

        fun updateFromSignal(signalStrength: SignalStrength?) {
            val level = signalStrength?.level?.coerceIn(0, 4) ?: 0
            cellularLevel = level
            if (level > 0) isCellularConnected = true
        }

        fun startListening() {
            runCatching {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        intent?.let { updateBattery(it) }
                    }
                }
                batteryReceiver = receiver
                val stickyIntent = context.registerReceiver(
                    receiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                stickyIntent?.let { updateBattery(it) }
            }

            runCatching {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                        queryWifi()
                    }

                    override fun onLost(network: Network) {
                        queryWifi()
                    }
                }
                networkCallback = callback
                cm?.registerDefaultNetworkCallback(callback)
                queryWifi()
            }

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                            updateFromSignal(signalStrength)
                        }
                    }
                    telephonyCallback = callback
                    tm?.registerTelephonyCallback(context.mainExecutor, callback)
                    updateFromSignal(tm?.signalStrength)
                } else {
                    val listener = object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                            updateFromSignal(signalStrength)
                        }
                    }
                    phoneStateListener = listener
                    @Suppress("DEPRECATION")
                    tm?.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        updateFromSignal(tm?.signalStrength)
                    }
                }
            }
        }

        fun stopListening() {
            runCatching {
                batteryReceiver?.let { context.unregisterReceiver(it) }
                batteryReceiver = null
            }
            runCatching {
                networkCallback?.let { cm?.unregisterNetworkCallback(it) }
                networkCallback = null
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (telephonyCallback as? TelephonyCallback)?.let { tm?.unregisterTelephonyCallback(it) }
                    telephonyCallback = null
                } else {
                    @Suppress("DEPRECATION")
                    (phoneStateListener as? PhoneStateListener)?.let { tm?.listen(it, PhoneStateListener.LISTEN_NONE) }
                    phoneStateListener = null
                }
            }
        }

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                startListening()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                stopListening()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            startListening()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopListening()
        }
    }

    return DuoSystemStatus(
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        chargingWatts = chargingWatts,
        batteryTempCelsius = batteryTempCelsius,
        isWifiConnected = isWifiConnected,
        wifiLevel = wifiLevel,
        isCellularConnected = isCellularConnected,
        cellularLevel = cellularLevel
    )
}
