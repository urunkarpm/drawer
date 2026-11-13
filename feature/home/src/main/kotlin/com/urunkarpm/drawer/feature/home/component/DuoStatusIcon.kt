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
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
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
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import com.urunkarpm.drawer.core.designsystem.component.DrawerSurfaceBackground
import com.urunkarpm.drawer.core.designsystem.theme.CinematicPalettes
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import java.io.File
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * Snapshot of device system status: Battery, Wi-Fi, Cellular signal strength, and charging metrics.
 */
@Immutable
data class DuoSystemStatus(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val chargingWatts: Float? = null,
    val chargingMaxWatts: Float? = null,
    val chargingStandard: String? = null,
    val batteryTempCelsius: Float? = null,
    val isWifiConnected: Boolean = false,
    val wifiLevel: Int = 0, // 0..4
    val isCellularConnected: Boolean = false,
    val cellularLevel: Int = 0, // 0..4
    val networkType: String? = null // "5G", "5G+", "4G", "4G+", "3G", "2G"
)

@Immutable
private data class BatteryChargingDetails(
    val liveWatts: Float?,
    val maxWatts: Float?,
    val standardLabel: String?,
    val tempCelsius: Float?
)

// ponytail: Real-time battery charging telemetry engine with multi-OEM broadcast decoding (Samsung, OnePlus/Oppo/Realme, Xiaomi, Vivo, Motorola), dual-cell series pack calibration (2S), and EMA jitter suppression; ceiling: rootless HAL limitations on select non-standard kernel forks; upgrade path: Android 14+ BatteryState/Health HAL services.
private fun readBatteryChargingStats(
    context: Context,
    intent: Intent?,
    isCharging: Boolean
): BatteryChargingDetails {
    val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val tempCelsius = if (tempRaw > 0) tempRaw / 10f else null

    if (!isCharging) {
        return BatteryChargingDetails(
            liveWatts = null,
            maxWatts = null,
            standardLabel = null,
            tempCelsius = tempCelsius
        )
    }

    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val model = Build.MODEL.orEmpty()

    // 1. Raw Battery Fuel Gauge Voltage (Terminal Voltage)
    val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
    val rawVolts = when {
        voltageMv > 0 -> voltageMv / 1000f
        else -> 3.85f // Nominal Li-ion voltage fallback
    }

    // 2. Battery Fuel Gauge Instantaneous Current (Delivered into cells)
    val currentNow = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
    val currentAvg = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: 0
    val current = if (currentNow != 0 && currentNow != Int.MIN_VALUE) currentNow else currentAvg
    val absCurrent = kotlin.math.abs(current)

    // Normalize current across varying OEM units (µA, mA, A)
    var amps = when {
        absCurrent >= 100_000 -> absCurrent / 1_000_000f // microamperes (µA)
        absCurrent in 100..20_000 -> absCurrent / 1_000f  // milliamperes (mA)
        absCurrent in 20_001..99_999 -> absCurrent / 1_000_000f // µA fallback
        absCurrent in 1..99 -> absCurrent.toFloat()       // Amperes (rare OEM quirk)
        else -> 0f
    }

    // 3. Hardware Negotiated Charger Contract (Fallback / Supplemental)
    val maxCurrentUa = intent?.getIntExtra("max_charging_current", -1)?.takeIf { it > 0 }
    val maxVoltageUv = intent?.getIntExtra("max_charging_voltage", -1)?.takeIf { it > 0 }
    val negotiatedWatts: Float? = if (maxCurrentUa != null && maxVoltageUv != null) {
        val v = maxVoltageUv / 1_000_000f
        val a = maxCurrentUa / 1_000_000f
        (v * a).takeIf { it in 1f..300f }
    } else null

    // If gas gauge current is unsupported (0 or Int.MIN_VALUE), estimate from hardware extra
    if (amps == 0f && negotiatedWatts != null && negotiatedWatts > 0f) {
        amps = (negotiatedWatts / rawVolts).coerceIn(0.2f, 15f)
    }

    // 4. Dual-Cell Series (2S) Battery Calibration
    // High-wattage charge-pump architectures (SuperVOOC/Warp/HyperCharge 120W) wire two 4.2V-4.4V cells in series.
    // If the kernel driver only measures single-cell voltage (~4.2V) while charging at high currents,
    // true pack intake is 2x. If the kernel driver already reports pack voltage (> 6.0V), no scaling is needed.
    val isDualCell = isDualCellBattery(rawVolts, manufacturer, model, amps)
    val effectiveVolts = if (isDualCell) rawVolts * 2f else rawVolts

    val liveWatts = if (effectiveVolts > 0f && amps > 0f) {
        val calc = effectiveVolts * amps
        if (calc in 0.1f..250f) calc else null
    } else null

    // 5. Decode Official OEM Fast-Charging Protocol & Brand Name
    val standardLabel = resolveOemChargingBrand(context, intent, liveWatts, manufacturer, model)

    return BatteryChargingDetails(
        liveWatts = liveWatts,
        maxWatts = negotiatedWatts,
        standardLabel = standardLabel,
        tempCelsius = tempCelsius
    )
}

private fun isDualCellBattery(
    rawVolts: Float,
    manufacturer: String,
    model: String,
    amps: Float
): Boolean {
    // If voltage already reads > 6.0V (e.g. 7.4V - 8.8V), the driver is ALREADY reporting total series pack voltage
    if (rawVolts > 6.0f) return false

    // Dual-cell series pack multiplication only applies during active charging (amps >= 1.2A)
    // When plugged into slow 5V USB ports, dual-cell devices charge in parallel or standard 5V bypass
    if (amps < 1.2f) return false

    // Hardware sysfs indicator probe
    val hasSysfsSecondary = runCatching {
        File("/sys/class/power_supply/battery/sub_batt_volt").exists() ||
        File("/sys/class/power_supply/battery-cg2").exists() ||
        File("/sys/class/power_supply/bms2").exists()
    }.getOrDefault(false)
    if (hasSysfsSecondary) return true

    val m = manufacturer.lowercase(Locale.ROOT)
    val mod = model.lowercase(Locale.ROOT)

    // OnePlus dual-cell series fast-charging lineup (8T, 9, 10, 11, 12, Nord 2/3, Ace, Open)
    // Exclude single-cell series: Nord CE, Nord N-series, OnePlus 7/8 series
    if (m.contains("oneplus")) {
        if (mod.contains("ce") || mod.contains("n10") || mod.contains("n20") || mod.contains("n30") ||
            mod.contains("n100") || mod.contains("n200") || mod.contains("n300")
        ) {
            return false
        }
        val dualSeries = listOf("8t", "9", "10", "11", "12", "nord 2", "nord 3", "ace", "open")
        return dualSeries.any { mod.contains(it) }
    }

    // OPPO Find X and Reno Pro+ series with SuperVOOC (65W+)
    if (m.contains("oppo")) {
        if (mod.contains("find x") || (mod.contains("reno") && (mod.contains("pro+") || mod.contains("pro plus")))) {
            return true
        }
    }

    // Realme GT and Pro+ series with SuperDart / UltraDart (65W - 240W)
    if (m.contains("realme")) {
        if (mod.contains("gt") || mod.contains("pro+") || mod.contains("pro plus") || mod.contains("master explorer")) {
            return true
        }
    }

    // Xiaomi ONLY 120W+ HyperCharge devices (Note: 33W and 67W are single-cell MMT!)
    if (m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")) {
        val isHyperCharge120W = mod.contains("120w") || mod.contains("hyper") ||
                mod.contains("pro+") || mod.contains("pro plus") ||
                (mod.contains("11t") && mod.contains("pro")) ||
                (mod.contains("12") && mod.contains("pro")) ||
                (mod.contains("13") && mod.contains("pro")) ||
                (mod.contains("14") && mod.contains("pro")) ||
                mod.contains("f4 gt") || mod.contains("f5 pro") || mod.contains("k50") || mod.contains("k60 pro")
        if (isHyperCharge120W) return true
    }

    // Vivo / iQOO FlashCharge series (80W / 120W / 200W)
    if (m.contains("vivo") || m.contains("iqoo")) {
        if (mod.contains("pro+") || mod.contains("legend") ||
            (m.contains("iqoo") && (mod.contains("7") || mod.contains("8") || mod.contains("9") ||
                    mod.contains("10") || mod.contains("11") || mod.contains("12") || mod.contains("neo 7") ||
                    mod.contains("neo 8") || mod.contains("neo 9")))
        ) {
            return true
        }
    }

    return false
}

private fun resolveOemChargingBrand(
    context: Context,
    intent: Intent?,
    liveWatts: Float?,
    manufacturer: String,
    model: String
): String {
    val m = manufacturer.lowercase(Locale.ROOT)
    val mod = model.lowercase(Locale.ROOT)
    val watts = liveWatts ?: 0f

    // ── OEM Specific Broadcast Intent Extras (100% Hardware Accurate) ──
    if (intent != null) {
        // 1. Samsung One UI charger detection
        if (m.contains("samsung")) {
            val chargerType = intent.getIntExtra("charger_type", -1)
            val chargeType = intent.getIntExtra("charge_type", -1)
            val secPlugType = intent.getIntExtra("sec_plug_type", -1)
            val isFast = intent.getBooleanExtra("fast_charger", false)

            when {
                chargerType == 6 || chargeType == 4 || secPlugType == 6 -> return "Super Fast 2.0 (45W)"
                chargerType == 5 || chargeType == 3 || secPlugType == 5 -> return "Super Fast (25W)"
                chargerType == 4 || chargeType == 2 || secPlugType == 4 || isFast -> return "Fast Charging (15W)"
                chargerType == 1 || chargeType == 1 -> return "Slow Charging"
                chargerType == 2 -> return "Cable Charging"
            }
        }

        // 2. OnePlus / OPPO / Realme VOOC & SuperVOOC mode detection
        if (m.contains("oneplus") || m.contains("oppo") || m.contains("realme")) {
            val voocMode = intent.getIntExtra("vooc_charge_mode", 0)
                .takeIf { it > 0 } ?: intent.getIntExtra("fastcharge_status", 0)
                .takeIf { it > 0 } ?: intent.getIntExtra("charge_technology", 0)

            if (voocMode > 0) {
                return when {
                    voocMode >= 4 -> {
                        if (m.contains("oneplus") || m.contains("oppo")) "SuperVOOC 100W+" else "UltraDart 150W+"
                    }
                    voocMode in 2..3 -> {
                        when {
                            m.contains("oneplus") -> if (mod.contains("8t") || mod.contains("9")) "Warp Charge 65" else "SuperVOOC 80W"
                            m.contains("realme") -> "SuperDart 65W+"
                            else -> "SuperVOOC 80W"
                        }
                    }
                    else -> {
                        when {
                            m.contains("oneplus") -> if (mod.contains("8t") || mod.contains("9") || mod.contains("10") || mod.contains("11") || mod.contains("12") || mod.contains("nord 2") || mod.contains("nord 3")) "SuperVOOC" else "Warp Charge 30"
                            m.contains("realme") -> "Dart Charge"
                            else -> "VOOC Flash Charge"
                        }
                    }
                }
            }
        }

        // 3. Xiaomi / Redmi / POCO MIUI/HyperOS charge speed detection
        if (m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")) {
            val chargeSpeed = intent.getIntExtra("charge_speed", -1).takeIf { it >= 0 }
                ?: intent.getIntExtra("miui_charge_type", -1).takeIf { it >= 0 }
            val isQuick = intent.getBooleanExtra("is_quick_charge", false)

            when {
                chargeSpeed != null && chargeSpeed >= 4 -> return "HyperCharge 120W"
                chargeSpeed != null && chargeSpeed == 3 -> return "Mi Turbo Charge 67W"
                chargeSpeed != null && chargeSpeed == 2 -> return "Mi Turbo Charge 33W"
                (chargeSpeed != null && chargeSpeed == 1) || isQuick -> return "Quick Charge"
            }
        }

        // 4. Motorola TurboPower detection
        if (m.contains("motorola")) {
            val isTurbo = intent.getBooleanExtra("turbo_charger", false) ||
                    intent.getIntExtra("turbo_charge", 0) > 0
            if (isTurbo) {
                return when {
                    watts >= 28f -> "TurboPower 68W+"
                    watts >= 14f -> "TurboPower 30W"
                    else -> "TurboPower"
                }
            }
        }
    }

    // ── Telemetry & Model Capability Fallback ──
    // Note: Live power hitting battery during screen-on usage is naturally ~40-60% of charger rating
    return when {
        m.contains("oneplus") -> when {
            watts >= 32f -> "SuperVOOC 80W+"
            watts >= 20f -> if (mod.contains("8t") || mod.contains("9")) "Warp Charge 65" else "SuperVOOC 65W"
            watts >= 12f -> if (mod.contains("7") || mod.contains("8")) "Warp Charge 30" else "SuperVOOC"
            watts >= 7.5f -> "Fast Charging"
            else -> "Charging"
        }
        m.contains("oppo") -> when {
            watts >= 32f -> "SuperVOOC 80W+"
            watts >= 20f -> "SuperVOOC 65W"
            watts >= 12f -> "SuperVOOC 33W"
            watts >= 7.5f -> "VOOC Flash Charge"
            else -> "Charging"
        }
        m.contains("realme") -> when {
            watts >= 32f -> "UltraDart / GT 150W+"
            watts >= 20f -> "SuperDart 65W"
            watts >= 12f -> "Dart Charge 33W"
            watts >= 7.5f -> "Quick Charge"
            else -> "Charging"
        }
        m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> when {
            watts >= 38f -> "HyperCharge 120W"
            watts >= 20f -> "Mi Turbo Charge 67W"
            watts >= 12f -> "Mi Turbo Charge 33W"
            watts >= 7.5f -> "Quick Charge"
            else -> "Charging"
        }
        m.contains("samsung") -> when {
            watts >= 22f -> "Super Fast 2.0 (45W)"
            watts >= 13f -> "Super Fast (25W)"
            watts >= 7.5f -> "Fast Charging (15W)"
            else -> "Cable Charging"
        }
        m.contains("google") -> when {
            watts >= 17f -> "Rapidly Charging (30W)"
            watts >= 10f -> "Rapidly Charging"
            watts >= 5.0f -> "Charging"
            else -> "Charging slowly"
        }
        m.contains("motorola") -> when {
            watts >= 28f -> "TurboPower 68W+"
            watts >= 14f -> "TurboPower 30W"
            watts >= 7.5f -> "TurboPower"
            else -> "Charging"
        }
        m.contains("vivo") || m.contains("iqoo") -> when {
            watts >= 30f -> "FlashCharge 80W+"
            watts >= 16f -> "FlashCharge 44W"
            watts >= 8f -> "FlashCharge"
            else -> "Charging"
        }
        m.contains("huawei") || m.contains("honor") -> when {
            watts >= 32f -> "SuperCharge 66W+"
            watts >= 18f -> "SuperCharge 40W"
            watts >= 10f -> "SuperCharge 22.5W"
            else -> "Charging"
        }
        m.contains("nothing") -> when {
            watts >= 20f -> "Super Fast (45W)"
            watts >= 12f -> "Fast Charging (33W)"
            else -> "Charging"
        }
        else -> when {
            watts >= 25f -> "Ultra Fast Charging"
            watts >= 12f -> "Fast Charging"
            watts >= 5.5f -> "Standard Charging"
            else -> "Charging slowly"
        }
    }
}

// ponytail: Dual-stream warm-up filtered throughput engine using Google Global Edge CDN (153MB) with Cloudflare (25MB) fallback; ceiling: 2-stream 2.4s HTTP GET window instead of multi-gigabit socket daemon; upgrade path: NDT7 or Ookla speedtest-cli daemon.
private suspend fun executeQuickSpeedTest(
    context: Context? = null,
    onProgress: ((String) -> Unit)? = null
): String = withContext(Dispatchers.IO) {
    val primaryUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    val fallbackUrl = "https://speed.cloudflare.com/__down?bytes=25000000"
    val userAgent = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    suspend fun runDualStream(targetUrl: String): Double? {
        val sharedMeasuredBytes = AtomicLong(0L)
        var sharedMeasureStartTime = 0L
        val lock = Any()
        var lastProgressEmitTime = 0L

        suspend fun streamWorker(): Boolean {
            var connection: HttpURLConnection? = null
            return try {
                val url = URL(targetUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3500
                    setRequestProperty("User-Agent", userAgent)
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                    setRequestProperty("Pragma", "no-cache")
                    useCaches = false
                    defaultUseCaches = false
                }
                connection.connect()
                if (connection.responseCode !in 200..299) return false

                var firstByteTime = 0L
                var warmupDone = false
                val buffer = ByteArray(65536)

                connection.inputStream.use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        val now = SystemClock.elapsedRealtime()
                        if (firstByteTime == 0L) firstByteTime = now

                        val elapsedTotal = now - firstByteTime
                        if (!warmupDone) {
                            if (elapsedTotal >= 400L) { // 400ms TCP slow-start warmup discard
                                warmupDone = true
                                synchronized(lock) {
                                    if (sharedMeasureStartTime == 0L) {
                                        sharedMeasureStartTime = now
                                    }
                                }
                            }
                        } else {
                            val currentTotal = sharedMeasuredBytes.addAndGet(read.toLong())
                            val dt = now - sharedMeasureStartTime
                            if (dt > 250L && now - lastProgressEmitTime > 200L && onProgress != null) {
                                lastProgressEmitTime = now
                                val liveBps = (currentTotal * 8.0) / (dt / 1000.0)
                                val liveMbps = liveBps / 1_000_000.0
                                val progressText = if (liveMbps >= 100.0) {
                                    String.format(Locale.US, "%.0f Mbps", liveMbps)
                                } else if (liveMbps >= 1.0) {
                                    String.format(Locale.US, "%.1f Mbps", liveMbps)
                                } else {
                                    String.format(Locale.US, "%.0f Kbps", (liveMbps * 1000.0).coerceAtLeast(1.0))
                                }
                                withContext(Dispatchers.Main) {
                                    onProgress(progressText)
                                }
                            }
                            if (elapsedTotal >= 2400L) break
                        }
                    }
                }
                true
            } catch (_: Exception) {
                false
            } finally {
                connection?.disconnect()
            }
        }

        coroutineScope {
            val s1 = async { streamWorker() }
            val s2 = async { streamWorker() }
            s1.await() || s2.await()
        }

        val totalBytes = sharedMeasuredBytes.get()
        val now = SystemClock.elapsedRealtime()
        return@runDualStream if (sharedMeasureStartTime > 0L && totalBytes > 0L) {
            val durationSec = (now - sharedMeasureStartTime).coerceAtLeast(200L) / 1000.0
            (totalBytes * 8.0) / (durationSec * 1_000_000.0)
        } else {
            null
        }
    }

    // Try primary (Google Global Edge CDN)
    var mbpsResult = runDualStream(primaryUrl)

    // Fallback to Cloudflare if primary failed (e.g. region where Google is blocked)
    if (mbpsResult == null || mbpsResult <= 0.01) {
        mbpsResult = runDualStream(fallbackUrl)
    }

    if (mbpsResult != null && mbpsResult > 0.01) {
        return@withContext when {
            mbpsResult >= 100.0 -> String.format(Locale.US, "%.0f Mbps", mbpsResult)
            mbpsResult >= 1.0 -> String.format(Locale.US, "%.1f Mbps", mbpsResult)
            else -> String.format(Locale.US, "%.0f Kbps", (mbpsResult * 1000.0).coerceAtLeast(1.0))
        }
    }

    // Native Platform Link Speed Fallback if completely offline / blocked
    if (context != null) {
        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val linkSpeed = wm?.connectionInfo?.linkSpeed ?: -1
                if (linkSpeed > 0) return@withContext "$linkSpeed Mbps"
            } else if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                val kbps = caps.linkDownstreamBandwidthKbps
                if (kbps > 1000) return@withContext "${kbps / 1000} Mbps"
            }
        }
    }

    "Unavailable"
}

// ponytail: Cellular network generation decoder resolving 5G NSA/SA, 5G+ (mmWave/Advanced), 4G+ (LTE CA), 4G, 3G, and 2G; ceiling: rootless TelephonyDisplayInfo branding; upgrade path: 3GPP ARFCN / band aggregation telemetry.
internal fun resolveNetworkTypeLabel(networkType: Int, overrideType: Int): String? {
    when (overrideType) {
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> return "5G+"
        @Suppress("DEPRECATION")
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> return "5G+"
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> return "5G"
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> return "4G+"
    }

    return when (networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN,
        TelephonyManager.NETWORK_TYPE_GSM -> "2G"
        else -> null
    }
}

// ponytail: Multi-RAT cellular signal strength extractor resolving 5G NR, LTE CA on Q+, single level on P+, and safe 0 fallback on API 26-27 where getLevel() is absent; ceiling: no raw dBm extraction on Oreo; upgrade path: CarrierConfigManager signal threshold parsing.
internal fun extractSignalLevel(signalStrength: SignalStrength?): Int {
    if (signalStrength == null) return 0
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cellLevels = signalStrength.cellSignalStrengths.map { it.level }.filter { it in 0..4 }
            val maxCell = cellLevels.maxOrNull() ?: 0
            maxOf(signalStrength.level.coerceIn(0, 4), maxCell)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || Build.VERSION.SDK_INT == 0) {
            signalStrength.level.coerceIn(0, 4)
        } else {
            0
        }
    }.getOrDefault(0)
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
    duoStatusPosition: String = "TEMP",
    surfaceThemeStyle: String = "LIQUID_GLASS",
    cinematicPaletteId: String = "DEFAULT",
    surfaceOpacity: Float = 0.85f,
    surfaceStrokeOpacity: Float = 0.60f,
    isMesmerizingGlow: Boolean = true
) {
    val context = LocalContext.current
    val crispHaptics = LocalCrispHaptics.current
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
    var liveTestingSpeed by remember { mutableStateOf<String?>(null) }
    var speedTestTriggeredThisSession by remember { mutableStateOf(false) }
    var timerResetKey by remember { mutableIntStateOf(0) }

    val retestRotationTransition = rememberInfiniteTransition(label = "retestRotation")
    val retestRotation by retestRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "retestSpin"
    )

    var liveWatts by remember { mutableStateOf<Float?>(status.chargingWatts) }
    var liveMaxWatts by remember { mutableStateOf<Float?>(status.chargingMaxWatts) }
    var liveStandard by remember { mutableStateOf<String?>(status.chargingStandard) }
    var liveTemp by remember { mutableStateOf<Float?>(status.batteryTempCelsius) }

    LaunchedEffect(status.chargingWatts, status.chargingMaxWatts, status.chargingStandard, status.batteryTempCelsius) {
        liveWatts = status.chargingWatts
        liveMaxWatts = status.chargingMaxWatts
        liveStandard = status.chargingStandard
        liveTemp = status.batteryTempCelsius
    }

    LaunchedEffect(isExpanded, status.isCharging) {
        if (isExpanded && status.isCharging) {
            while (true) {
                val details = withContext(Dispatchers.IO) {
                    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    readBatteryChargingStats(context, sticky, true)
                }
                if (details.liveWatts != null) {
                    val prev = liveWatts
                    liveWatts = if (prev != null && prev > 0.1f) {
                        (prev * 0.70f + details.liveWatts * 0.30f)
                    } else {
                        details.liveWatts
                    }
                }
                if (details.maxWatts != null) liveMaxWatts = details.maxWatts
                if (details.standardLabel != null) liveStandard = details.standardLabel
                if (details.tempCelsius != null) liveTemp = details.tempCelsius
                delay(1500L)
            }
        }
    }

    fun triggerSpeedTest() {
        if (isSpeedTesting) return
        crispHaptics.click()
        if (!status.isWifiConnected && !status.isCellularConnected) {
            lastRecordedSpeed = "Offline"
            prefs.edit().putString("last_speed", "Offline").apply()
            return
        }
        isSpeedTesting = true
        liveTestingSpeed = "Testing..."
        speedTestTriggeredThisSession = true
        timerResetKey++
        scope.launch {
            val result = executeQuickSpeedTest(context) { liveMbps ->
                liveTestingSpeed = liveMbps
            }
            isSpeedTesting = false
            liveTestingSpeed = null
            if (result != "Failed" && result != "Unavailable") {
                lastRecordedSpeed = result
                prefs.edit().putString("last_speed", result).apply()
                crispHaptics.confirm()
            } else {
                lastRecordedSpeed = "Unavailable"
            }
            timerResetKey++
        }
    }

    // Auto-close expanded view: 20s when connected to power or running speed test; 10s otherwise
    LaunchedEffect(isExpanded, timerResetKey, status.isCharging, speedTestTriggeredThisSession) {
        if (isExpanded) {
            val durationMs = if (status.isCharging || speedTestTriggeredThisSession || isSpeedTesting) 20_000L else 10_000L
            delay(durationMs)
            if (!isSpeedTesting) {
                onToggleExpand()
            }
        } else {
            speedTestTriggeredThisSession = false
        }
    }

    val isLightDisplay = displayColor.luminance() < 0.5f
    val chargingGreen = if (isLightDisplay) Color(0xFF16A34A) else Color(0xFF34C759)
    val activeColor = displayColor
    val inactiveColor = if (isLightDisplay) displayColor.copy(alpha = 0.38f) else displayColor.copy(alpha = 0.24f)
    val activeBatteryColor = if (isCharging) chargingGreen else activeColor
    val inactiveBatteryColor = if (isCharging) chargingGreen.copy(alpha = if (isLightDisplay) 0.35f else 0.20f) else inactiveColor
    val alertRed = if (isLightDisplay) Color(0xFFDC2626) else Color(0xFFFF3B30)

    // ponytail: Surface-aware contrast engine for expanded telemetry card; ceiling: two-tier dark/light token derivation; upgrade path: full Material 3 dynamic color scheme harmonizer.
    val palette = remember(cinematicPaletteId) { CinematicPalettes.find(cinematicPaletteId) }
    val isCinematic = palette.id != CinematicPalettes.ID_DEFAULT
    val cardSurfaceBase = if (isCinematic) palette.surfaceColor else MaterialTheme.colorScheme.surface
    val isDarkSurface = cardSurfaceBase.luminance() < 0.5f

    // High-contrast content colors guaranteeing WCAG AAA readability against glass/solid card surfaces
    val cardTitleColor = if (isDarkSurface) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardSubtextColor = if (isDarkSurface) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardTertiaryColor = if (isDarkSurface) Color.White.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.50f)
    val cardTileBackground = if (isDarkSurface) Color(0xFF131B2E).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.85f)
    val cardTileBorder = if (isDarkSurface) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.09f)
    val cardAccentColor = if (isCinematic) palette.primaryAccent else (if (isDarkSurface) Color(0xFF818CF8) else Color(0xFF4F46E5))
    val cardChargingGreen = if (isDarkSurface) Color(0xFF34C759) else Color(0xFF16A34A)
    val cardAlertRed = if (isDarkSurface) Color(0xFFFF453A) else Color(0xFFDC2626)
    val cardInactiveTrack = if (isDarkSurface) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.14f)

    // Morph container dimensions seamlessly
    val configuration = LocalConfiguration.current
    val maxTargetWidth = (configuration.screenWidthDp.dp - 32.dp).coerceIn(264.dp, 320.dp)
    val baseExpandedHeight = if (status.isCharging) 256.dp else 230.dp
    val currentWidth = lerp(sizeDp.dp, maxTargetWidth, expandProgress)
    val currentHeight = lerp(sizeDp.dp, baseExpandedHeight, expandProgress)
    val currentCornerRadius = lerp((sizeDp / 2f).dp, 22.dp, expandProgress)

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
                elevation = if (expandProgress > 0.05f) 12.dp * expandProgress else 0.dp,
                shape = RoundedCornerShape(currentCornerRadius)
            )
            .clip(RoundedCornerShape(currentCornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    crispHaptics.click()
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
        if (expandProgress > 0.05f) {
            // Ground glass finishes with a protective high-contrast backing scrim so light or patterned wallpapers never bleed through and degrade legibility
            val backingAlpha = when (surfaceThemeStyle) {
                "SOLID" -> 0f
                "TRANSPARENT" -> 0.55f
                else -> 0.94f // LIQUID_GLASS, BLUR: Opaque backing eliminates wallpaper bleed-through while glass sheen layers on top
            } * ((expandProgress - 0.05f) / 0.95f).coerceIn(0f, 1f)

            if (backingAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(currentCornerRadius))
                        .background(
                            if (isDarkSurface) Color(0xFF090D16).copy(alpha = backingAlpha)
                            else Color(0xFFF1F5F9).copy(alpha = backingAlpha)
                        )
                )
            }

            DrawerSurfaceBackground(
                surfaceThemeStyle = surfaceThemeStyle,
                shape = RoundedCornerShape(currentCornerRadius),
                isExpanded = true,
                accentColor = if (status.isCharging) cardChargingGreen else null,
                isMesmerizingGlow = isMesmerizingGlow,
                surfaceOpacity = surfaceOpacity,
                surfaceStrokeOpacity = surfaceStrokeOpacity,
                cinematicPaletteId = cinematicPaletteId,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // Collapsed capsule surface backing ensuring high contrast on white and light wallpapers
            val collapsedFade = (1f - (expandProgress / 0.30f)).coerceIn(0f, 1f)
            val collapsedBackingColor = if (isLightDisplay) {
                Color(0xFF0F172A).copy(alpha = 0.09f * collapsedFade)
            } else {
                Color.Black.copy(alpha = 0.22f * collapsedFade)
            }
            val collapsedBorderColor = if (isLightDisplay) {
                Color.Black.copy(alpha = 0.14f * collapsedFade)
            } else {
                Color.White.copy(alpha = 0.16f * collapsedFade)
            }

            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .background(collapsedBackingColor)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, collapsedBorderColor), CircleShape)
            )
        }
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

                    // Shadow halo pass guaranteeing crisp visibility on white or busy wallpapers
                    val shadowColor = if (isLightDisplay) {
                        Color.White.copy(alpha = 0.65f)
                    } else {
                        Color.Black.copy(alpha = 0.55f)
                    }
                    val shadowStrokeOffset = 0.8f * s

                    // 1. BATTERY: Outer circular arc (sweeping 242° from 149° clockwise to 31°)
                    val outerRadius = 117.0f * s
                    val outerStroke = 18.0f * s
                    val startAngle = 149.0f
                    val totalBatteryArc = 242.0f
                    val arcTopLeft = Offset(centerX - outerRadius, centerY - outerRadius)
                    val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

                    drawArc(
                        color = shadowColor,
                        startAngle = startAngle,
                        sweepAngle = totalBatteryArc,
                        useCenter = false,
                        topLeft = Offset(arcTopLeft.x, arcTopLeft.y + shadowStrokeOffset),
                        size = arcSize,
                        style = Stroke(width = outerStroke + 1.2f * s, cap = StrokeCap.Round)
                    )

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
                            color = shadowColor,
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = Offset(arcTopLeft.x, arcTopLeft.y + shadowStrokeOffset),
                            size = arcSize,
                            style = Stroke(width = outerStroke + 1.2f * s, cap = StrokeCap.Round)
                        )
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
                        drawCircle(
                            color = shadowColor,
                            radius = dotRadius + 0.8f * s,
                            center = Offset(dotCenter.x, dotCenter.y + shadowStrokeOffset)
                        )
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
                    wedgePath.quadraticTo(
                        x1 = wifiOriginX + 6.0f * s,
                        y1 = wifiOriginY + 2.0f * s,
                        x2 = wifiOriginX,
                        y2 = wifiOriginY + 7.5f * s
                    )
                    wedgePath.quadraticTo(
                        x1 = wifiOriginX - 6.0f * s,
                        y1 = wifiOriginY + 2.0f * s,
                        x2 = pStartX,
                        y2 = pStartY
                    )
                    wedgePath.close()

                    val topWifiColor = if (!status.isWifiConnected) alertRed else if (isTopActive) activeColor else inactiveColor
                    val midWifiColor = if (!status.isWifiConnected) alertRed else if (isMidActive) activeColor else inactiveColor
                    val baseWifiColor = if (!status.isWifiConnected) alertRed else if (isBaseActive) activeColor else inactiveColor

                    drawArc(
                        color = shadowColor,
                        startAngle = 226.0f,
                        sweepAngle = 88.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop + shadowStrokeOffset),
                        size = Size(rTop * 2f, rTop * 2f),
                        style = Stroke(width = strokeTop + 1.2f * s, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = topWifiColor,
                        startAngle = 226.0f,
                        sweepAngle = 88.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rTop, wifiOriginY - rTop),
                        size = Size(rTop * 2f, rTop * 2f),
                        style = Stroke(width = strokeTop, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = shadowColor,
                        startAngle = 225.0f,
                        sweepAngle = 90.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid + shadowStrokeOffset),
                        size = Size(rMid * 2f, rMid * 2f),
                        style = Stroke(width = strokeMid + 1.2f * s, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = midWifiColor,
                        startAngle = 225.0f,
                        sweepAngle = 90.0f,
                        useCenter = false,
                        topLeft = Offset(wifiOriginX - rMid, wifiOriginY - rMid),
                        size = Size(rMid * 2f, rMid * 2f),
                        style = Stroke(width = strokeMid, cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = wedgePath,
                        color = baseWifiColor
                    )
                }
            }
        }

        // ── Layer 2: Expanded Breakdown Telemetry Card ──
        if (expandProgress > 0.05f) {
            // ponytail: Expanded status telemetry card uses DrawerSurfaceBackground and LocalHeaderFontFamily to match the launcher's glassmorphic design language; ceiling: fixed height telemetry card without expandable history logs; upgrade path: scrollable diagnostics / battery health sub-page.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 13.dp, vertical = 10.dp)
                    .graphicsLayer {
                        alpha = ((expandProgress - 0.20f) / 0.80f).coerceIn(0f, 1f)
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: Title and tactile Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.20f) / 0.60f).coerceIn(0f, 1f)
                            translationY = (1f - ((ep - 0.20f) / 0.80f).coerceIn(0f, 1f)) * -8f
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (status.isCharging) cardChargingGreen else cardAccentColor)
                        )
                        Text(
                            text = "Device Status",
                            fontFamily = LocalHeaderFontFamily.current,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cardTitleColor,
                            letterSpacing = 0.4.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkSurface) Color.White.copy(alpha = 0.12f)
                                else Color.Black.copy(alpha = 0.08f)
                            )
                            .border(
                                width = 0.8.dp,
                                color = cardTileBorder,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    crispHaptics.click()
                                    onToggleExpand()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = cardTitleColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Section 1: Battery tile with charging metrics & live progress
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.22f) / 0.60f).coerceIn(0f, 1f)
                            translationY = (1f - ((ep - 0.22f) / 0.80f).coerceIn(0f, 1f)) * -6f
                        }
                        .clip(RoundedCornerShape(13.dp))
                        .background(cardTileBackground)
                        .border(
                            width = 0.8.dp,
                            color = cardTileBorder,
                            shape = RoundedCornerShape(13.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                                imageVector = if (status.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (status.isCharging) cardChargingGreen else cardTitleColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Battery",
                                fontFamily = LocalHeaderFontFamily.current,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cardTitleColor
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (status.isCharging && liveTemp != null) {
                                Text(
                                    text = String.format(Locale.US, "%.1f°C", liveTemp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = cardSubtextColor
                                )
                            }
                            Text(
                                text = "${status.batteryPercent}%",
                                fontFamily = LocalHeaderFontFamily.current,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isCharging) cardChargingGreen else cardTitleColor
                            )
                        }
                    }

                    // Progress bar
                    val barProgress = animatedProgress * ((expandProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(cardInactiveTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isCharging) cardChargingGreen else cardAccentColor
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isFull = status.batteryPercent >= 100
                            val label = liveStandard ?: status.chargingStandard
                            val standardText = when {
                                isFull -> "Fully Charged"
                                !label.isNullOrBlank() -> label
                                else -> "Charging"
                            }
                            Text(
                                text = standardText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = cardChargingGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val powerText = buildString {
                                val currentW = liveWatts ?: status.chargingWatts
                                if (isFull) {
                                    append("Complete")
                                } else if (currentW != null && currentW > 0.5f) {
                                    append(String.format(Locale.US, "⚡ %.1f W", currentW))
                                } else if (currentW != null && currentW > 0.05f) {
                                    append(String.format(Locale.US, "⚡ %.1f W", currentW))
                                } else {
                                    val fallbackW = liveMaxWatts ?: status.chargingMaxWatts
                                    if (fallbackW != null && fallbackW > 1.0f) {
                                        append(String.format(Locale.US, "⚡ %.0f W", fallbackW))
                                    } else {
                                        append("⚡ Active")
                                    }
                                }
                            }
                            Text(
                                text = powerText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cardChargingGreen
                            )
                        }
                    }
                }

                // Section 2: Wi-Fi and Cellular side-by-side tiles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.28f) / 0.58f).coerceIn(0f, 1f)
                            translationY = (1f - ((ep - 0.28f) / 0.70f).coerceIn(0f, 1f)) * -4f
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wi-Fi Tile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(cardTileBackground)
                            .border(
                                width = 0.8.dp,
                                color = cardTileBorder,
                                shape = RoundedCornerShape(13.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (status.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (status.isWifiConnected) cardTitleColor else cardAlertRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Wi-Fi",
                                    fontFamily = LocalHeaderFontFamily.current,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cardSubtextColor
                                )
                            }
                            val wifiStatusText = if (status.isWifiConnected) {
                                when (status.wifiLevel) {
                                    4, 3 -> "Excellent"
                                    2 -> "Good"
                                    1 -> "Fair"
                                    else -> "Connected"
                                }
                            } else {
                                "Disconnected"
                            }
                            Text(
                                text = wifiStatusText,
                                fontFamily = LocalHeaderFontFamily.current,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (status.isWifiConnected) cardTitleColor else cardAlertRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Cellular Tile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .background(cardTileBackground)
                            .border(
                                width = 0.8.dp,
                                color = cardTileBorder,
                                shape = RoundedCornerShape(13.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCell,
                                    contentDescription = null,
                                    tint = if (status.isCellularConnected) cardTitleColor else cardAlertRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                val carrierLabel = status.networkType ?: "Cellular"
                                Text(
                                    text = carrierLabel,
                                    fontFamily = LocalHeaderFontFamily.current,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cardSubtextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val cellStatusText = if (status.isCellularConnected) {
                                    when (status.cellularLevel) {
                                        4 -> "Strong"
                                        3 -> "Good"
                                        2 -> "Fair"
                                        1 -> "Poor"
                                        else -> "Connected"
                                    }
                                } else {
                                    "No Service"
                                }
                                Text(
                                    text = cellStatusText,
                                    fontFamily = LocalHeaderFontFamily.current,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status.isCellularConnected) cardTitleColor else cardAlertRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 1..4) {
                                        val isFilled = status.isCellularConnected && status.cellularLevel >= i
                                        val dotColor = if (!status.isCellularConnected) {
                                            cardAlertRed
                                        } else if (isFilled) {
                                            cardAccentColor
                                        } else {
                                            cardInactiveTrack
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Speed test pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val ep = expandProgress
                            alpha = ((ep - 0.35f) / 0.55f).coerceIn(0f, 1f)
                            translationY = (1f - ((ep - 0.35f) / 0.65f).coerceIn(0f, 1f)) * -4f
                        }
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (isSpeedTesting) cardChargingGreen.copy(alpha = 0.18f)
                            else cardTileBackground
                        )
                        .border(
                            width = 0.8.dp,
                            color = if (isSpeedTesting) cardChargingGreen.copy(alpha = 0.50f)
                            else cardTileBorder,
                            shape = RoundedCornerShape(13.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { triggerSpeedTest() }
                        )
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isSpeedTesting) cardChargingGreen else cardTitleColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Speed Test",
                            fontFamily = LocalHeaderFontFamily.current,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cardTitleColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val speedText = when {
                            isSpeedTesting -> liveTestingSpeed ?: "Testing..."
                            !lastRecordedSpeed.isNullOrBlank() -> lastRecordedSpeed!!
                            !status.isWifiConnected && !status.isCellularConnected -> "Offline"
                            else -> "Tap to test"
                        }
                        val speedColor = when {
                            isSpeedTesting -> cardChargingGreen
                            speedText == "Offline" || speedText == "Error" || speedText == "Unavailable" -> cardAlertRed
                            else -> cardTitleColor
                        }

                        Text(
                            text = speedText,
                            fontFamily = LocalHeaderFontFamily.current,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = speedColor
                        )
                        Text(
                            text = "↺",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpeedTesting) cardChargingGreen else cardTertiaryColor,
                            modifier = Modifier.graphicsLayer {
                                if (isSpeedTesting) {
                                    rotationZ = retestRotation
                                }
                            }
                        )
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

    val cm = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    val wm = remember(context) { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }
    val tm = remember(context) { context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager }

    // Resolve primary TelephonyManager instance pinned to active data/default SIM for multi-SIM / Dual-SIM
    val activeTm = remember(context, tm) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && tm != null) {
                val dataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (dataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    tm.createForSubscriptionId(dataSubId)
                } else {
                    val defaultSubId = SubscriptionManager.getDefaultSubscriptionId()
                    if (defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        tm.createForSubscriptionId(defaultSubId)
                    } else tm
                }
            } else tm
        }.getOrNull() ?: tm
    }

    val initialSignal = remember(activeTm, tm) {
        runCatching {
            // TelephonyManager.getSignalStrength is API 28+; minSdk is 26.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                0
            } else {
                val s1 = activeTm?.signalStrength
                val s2 = if (activeTm != tm) tm?.signalStrength else null
                maxOf(extractSignalLevel(s1), extractSignalLevel(s2))
            }
        }.getOrDefault(0)
    }

    val initialCellularConnected = remember(context, activeTm, tm, cm, initialSignal) {
        runCatching {
            val isAirplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            if (isAirplane) false
            else {
                initialSignal > 0 ||
                activeTm?.simState == TelephonyManager.SIM_STATE_READY ||
                tm?.simState == TelephonyManager.SIM_STATE_READY ||
                cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            }
        }.getOrDefault(false)
    }

    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var chargingWatts by remember { mutableStateOf<Float?>(null) }
    var chargingMaxWatts by remember { mutableStateOf<Float?>(null) }
    var chargingStandard by remember { mutableStateOf<String?>(null) }
    var batteryTempCelsius by remember { mutableStateOf<Float?>(null) }
    var isWifiConnected by remember { mutableStateOf(false) }
    var wifiLevel by remember { mutableIntStateOf(0) }
    var isCellularConnected by remember { mutableStateOf(initialCellularConnected) }
    var cellularLevel by remember { mutableIntStateOf(initialSignal) }
    var networkType by remember { mutableStateOf<String?>(null) }

    fun refreshCellularConnected() {
        runCatching {
            val isAirplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            if (isAirplane) {
                isCellularConnected = false
                return
            }

            val hasCell = cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isSimReady = activeTm?.simState == TelephonyManager.SIM_STATE_READY ||
                    tm?.simState == TelephonyManager.SIM_STATE_READY
            val hasOperator = !activeTm?.networkOperatorName.isNullOrBlank() || !tm?.networkOperatorName.isNullOrBlank()

            isCellularConnected = hasCell || cellularLevel > 0 || !networkType.isNullOrBlank() || (isSimReady && hasOperator)
        }
    }

    fun updateFromSignal(signalStrength: SignalStrength?) {
        val level = extractSignalLevel(signalStrength)
        cellularLevel = level
        refreshCellularConnected()
    }

    fun queryCellularSignal() {
        runCatching {
            // TelephonyManager.getSignalStrength is API 28+; minSdk is 26.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
            val s1 = activeTm?.signalStrength
            val s2 = if (activeTm != tm) tm?.signalStrength else null
            val bestSignal = when {
                s1 == null -> s2
                s2 == null -> s1
                else -> {
                    val l1 = extractSignalLevel(s1)
                    val l2 = extractSignalLevel(s2)
                    if (l2 > l1) s2 else s1
                }
            }
            updateFromSignal(bestSignal)
        }
    }

    fun updateFromDisplayInfo(telephonyDisplayInfo: TelephonyDisplayInfo) {
        // TelephonyDisplayInfo accessors are API 30+; the listener that calls this is
        // registered behind a Build.VERSION_CODES.S check, but guard here too.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val resolved = resolveNetworkTypeLabel(
            telephonyDisplayInfo.networkType,
            telephonyDisplayInfo.overrideNetworkType
        )
        networkType = resolved
        refreshCellularConnected()
    }

    // Periodic telemetry sync loop preventing stale/unresponsive cellular icons when stationary
    LaunchedEffect(lifecycleOwner, activeTm, tm) {
        while (true) {
            delay(5000L)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                queryCellularSignal()
            }
        }
    }

    DisposableEffect(lifecycleOwner, context, activeTm) {
        var batteryReceiver: BroadcastReceiver? = null
        var networkCallback: ConnectivityManager.NetworkCallback? = null
        var signalCallback: Any? = null
        var displayCallback: Any? = null
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

            val stats = readBatteryChargingStats(context, intent, isCharging)
            chargingWatts = stats.liveWatts
            chargingMaxWatts = stats.maxWatts
            chargingStandard = stats.standardLabel
            batteryTempCelsius = stats.tempCelsius
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

                refreshCellularConnected()
            }
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

            queryCellularSignal()
            refreshCellularConnected()

            runCatching {
                val initialType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    activeTm?.dataNetworkType?.takeIf { it != TelephonyManager.NETWORK_TYPE_UNKNOWN }
                        ?: tm?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                } else {
                    @Suppress("DEPRECATION")
                    activeTm?.networkType?.takeIf { it != TelephonyManager.NETWORK_TYPE_UNKNOWN }
                        ?: tm?.networkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                }
                val resolved = resolveNetworkTypeLabel(initialType, 0)
                if (!resolved.isNullOrBlank()) {
                    networkType = resolved
                    refreshCellularConnected()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // 1. Signal strength listener (Does NOT require READ_PHONE_STATE)
                runCatching {
                    val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                            updateFromSignal(signalStrength)
                        }
                    }
                    signalCallback = callback
                    activeTm?.registerTelephonyCallback(context.mainExecutor, callback)
                    if (activeTm != tm) {
                        tm?.registerTelephonyCallback(context.mainExecutor, callback)
                    }
                }

                // 2. Display info listener (isolated in case READ_PHONE_STATE is missing on strict OEMs)
                runCatching {
                    val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                            updateFromDisplayInfo(telephonyDisplayInfo)
                        }
                    }
                    displayCallback = callback
                    activeTm?.registerTelephonyCallback(context.mainExecutor, callback)
                }
            } else {
                runCatching {
                    val listener = object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                            updateFromSignal(signalStrength)
                        }
                    }
                    phoneStateListener = listener
                    @Suppress("DEPRECATION")
                    activeTm?.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                    if (activeTm != tm) {
                        @Suppress("DEPRECATION")
                        tm?.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
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
                    (signalCallback as? TelephonyCallback)?.let {
                        activeTm?.unregisterTelephonyCallback(it)
                        if (activeTm != tm) tm?.unregisterTelephonyCallback(it)
                    }
                    signalCallback = null
                    (displayCallback as? TelephonyCallback)?.let {
                        activeTm?.unregisterTelephonyCallback(it)
                    }
                    displayCallback = null
                } else {
                    (phoneStateListener as? PhoneStateListener)?.let {
                        @Suppress("DEPRECATION")
                        activeTm?.listen(it, PhoneStateListener.LISTEN_NONE)
                        if (activeTm != tm) {
                            @Suppress("DEPRECATION")
                            tm?.listen(it, PhoneStateListener.LISTEN_NONE)
                        }
                    }
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
        chargingMaxWatts = chargingMaxWatts,
        chargingStandard = chargingStandard,
        batteryTempCelsius = batteryTempCelsius,
        isWifiConnected = isWifiConnected,
        wifiLevel = wifiLevel,
        isCellularConnected = isCellularConnected,
        cellularLevel = cellularLevel,
        networkType = networkType
    )
}
