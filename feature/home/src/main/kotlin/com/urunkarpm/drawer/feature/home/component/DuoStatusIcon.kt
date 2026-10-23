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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import android.os.SystemClock
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
                val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val details = readBatteryChargingStats(context, sticky, true)
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
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        val standardSuffix = if (status.isCharging) {
                            val isFull = status.batteryPercent >= 100
                            val label = liveStandard ?: status.chargingStandard
                            when {
                                isFull -> " • Fully Charged"
                                !label.isNullOrBlank() -> " • $label"
                                else -> " • Charging"
                            }
                        } else ""
                        Text(
                            text = "${status.batteryPercent}%$standardSuffix",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (status.isCharging) chargingGreen else displayColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            val isFull = status.batteryPercent >= 100
                            val powerText = buildString {
                                append("⚡ ")
                                val currentW = liveWatts ?: status.chargingWatts
                                if (isFull) {
                                    append("Fully Charged")
                                } else if (currentW != null && currentW > 0.5f) {
                                    append(String.format(Locale.US, "%.1f W", currentW))
                                } else if (currentW != null && currentW > 0.05f) {
                                    append(String.format(Locale.US, "%.1f W (Trickle)", currentW))
                                } else {
                                    val fallbackW = liveMaxWatts ?: status.chargingMaxWatts
                                    if (fallbackW != null && fallbackW > 1.0f) {
                                        append(String.format(Locale.US, "%.0f W", fallbackW))
                                    } else {
                                        append("Charging")
                                    }
                                }
                            }
                            Text(
                                text = powerText,
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
                    val carrierSuffix = if (status.isCellularConnected && !status.networkType.isNullOrBlank()) {
                        " • ${status.networkType}"
                    } else ""
                    Text(
                        text = "Carrier$carrierSuffix",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = displayColor.copy(alpha = 0.65f),
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                            isSpeedTesting -> liveTestingSpeed ?: "Testing..."
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
                        Text(
                            text = "↺",
                            fontSize = 11.sp,
                            color = if (isSpeedTesting) chargingGreen else displayColor.copy(alpha = 0.45f),
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
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var chargingWatts by remember { mutableStateOf<Float?>(null) }
    var chargingMaxWatts by remember { mutableStateOf<Float?>(null) }
    var chargingStandard by remember { mutableStateOf<String?>(null) }
    var batteryTempCelsius by remember { mutableStateOf<Float?>(null) }
    var isWifiConnected by remember { mutableStateOf(false) }
    var wifiLevel by remember { mutableIntStateOf(0) }
    var isCellularConnected by remember { mutableStateOf(false) }
    var cellularLevel by remember { mutableIntStateOf(0) }
    var networkType by remember { mutableStateOf<String?>(null) }

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

                val hasCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                isCellularConnected = hasCell || isCellularConnected
            }
        }

        fun updateFromSignal(signalStrength: SignalStrength?) {
            val level = signalStrength?.level?.coerceIn(0, 4) ?: 0
            cellularLevel = level
            if (level > 0) isCellularConnected = true
        }

        fun updateFromDisplayInfo(telephonyDisplayInfo: TelephonyDisplayInfo) {
            val resolved = resolveNetworkTypeLabel(
                telephonyDisplayInfo.networkType,
                telephonyDisplayInfo.overrideNetworkType
            )
            networkType = resolved
            if (!resolved.isNullOrBlank()) {
                isCellularConnected = true
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

            runCatching {
                val initialType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    tm?.dataNetworkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                } else {
                    @Suppress("DEPRECATION")
                    tm?.networkType ?: TelephonyManager.NETWORK_TYPE_UNKNOWN
                }
                val resolved = resolveNetworkTypeLabel(initialType, 0)
                if (!resolved.isNullOrBlank()) {
                    networkType = resolved
                }
            }

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val callback = object : TelephonyCallback(),
                        TelephonyCallback.SignalStrengthsListener,
                        TelephonyCallback.DisplayInfoListener {
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                            updateFromSignal(signalStrength)
                        }

                        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                            updateFromDisplayInfo(telephonyDisplayInfo)
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
