package com.urunkarpm.drawer.feature.home.component

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.model.WeatherInfo
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Immutable
data class WallpaperContrastColors(
    val primaryText: Color,
    val secondaryText: Color,
    val textShadow: Shadow
)

@Composable
fun rememberWallpaperContrastColors(): WallpaperContrastColors {
    val context = LocalContext.current
    val fallbackIsLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    var isLightWallpaper by remember { mutableStateOf<Boolean?>(null) }

    val probeWallpaper = remember(context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                runCatching {
                    val wm = WallpaperManager.getInstance(context)
                    val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    if (colors != null) {
                        val hints = colors.colorHints
                        val supportsDarkText = (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                        if (supportsDarkText) {
                            true
                        } else {
                            val primaryLum = Color(colors.primaryColor.toArgb()).luminance()
                            val secondaryLum = colors.secondaryColor?.let { Color(it.toArgb()).luminance() }
                            val lum = if (secondaryLum != null) {
                                (primaryLum * 0.7f) + (secondaryLum * 0.3f)
                            } else {
                                primaryLum
                            }
                            lum > 0.48f
                        }
                    } else {
                        null
                    }
                }.getOrNull()
            } else {
                null
            }
        }
    }

    LaunchedEffect(Unit) {
        val detected = probeWallpaper()
        if (detected != null) {
            isLightWallpaper = detected
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val detected = probeWallpaper()
            if (detected != null) {
                isLightWallpaper = detected
            }
        }
    }

    DisposableEffect(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0 && colors != null) {
                    val hints = colors.colorHints
                    val supportsDarkText = (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                    isLightWallpaper = if (supportsDarkText) {
                        true
                    } else {
                        val primaryLum = Color(colors.primaryColor.toArgb()).luminance()
                        val secondaryLum = colors.secondaryColor?.let { Color(it.toArgb()).luminance() }
                        val lum = if (secondaryLum != null) {
                            (primaryLum * 0.7f) + (secondaryLum * 0.3f)
                        } else {
                            primaryLum
                        }
                        lum > 0.48f
                    }
                }
            }
            val handler = Handler(Looper.getMainLooper())
            runCatching {
                wm?.addOnColorsChangedListener(listener, handler)
            }
            onDispose {
                runCatching {
                    wm?.removeOnColorsChangedListener(listener)
                }
            }
        } else {
            onDispose {}
        }
    }

    val isLight = isLightWallpaper ?: fallbackIsLight
    return remember(isLight) {
        if (isLight) {
            WallpaperContrastColors(
                primaryText = Color(0xFF111827),
                secondaryText = Color(0xFF374151),
                textShadow = Shadow(
                    color = Color(0x66FFFFFF),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            )
        } else {
            WallpaperContrastColors(
                primaryText = Color(0xFFFFFFFF),
                secondaryText = Color(0xFFE5E7EB),
                textShadow = Shadow(
                    color = Color(0xCC000000),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlanceHeader(
    is24Hour: Boolean,
    showWeather: Boolean,
    weatherUnit: String,
    weatherInfo: WeatherInfo?,
    onRefreshWeather: () -> Unit,
    onOpenSettings: () -> Unit = {},
    showDuoStatus: Boolean = true,
    onDuoStatusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val now = LocalTime.now()
                currentTime = now
                currentDate = LocalDate.now()
                val millisUntilNextMinute = ((60 - now.second) * 1000L - (now.nano / 1_000_000L) + 50L)
                    .coerceIn(500L, 60_000L)
                delay(millisUntilNextMinute)
            }
        }
    }

    val timeDigits = currentTime.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm"))
    val amPm = if (!is24Hour) currentTime.format(DateTimeFormatter.ofPattern("a")) else null
    val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    val tempText = if (weatherInfo != null) {
        formatTemperature(weatherInfo.temperatureCelsius, weatherUnit)
    } else {
        "--°"
    }

    val weatherLocationText = if (weatherInfo != null) {
        val condition = weatherInfo.conditionDescription.trim()
        val city = weatherInfo.cityName.trim()
        if (city.isNotBlank() && !city.equals("Unknown", ignoreCase = true)) {
            "$condition, $city"
        } else {
            condition
        }
    } else {
        "Tap to load"
    }

    val wallpaperContrast = rememberWallpaperContrastColors()
    val displayColor = wallpaperContrast.primaryText
    val subtextColor = wallpaperContrast.secondaryText
    val textShadow = wallpaperContrast.textShadow

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Live Clock & Date (2 Tiers)
        Column(
            modifier = Modifier
                .weight(1.1f)
                .combinedClickable(
                    onClick = { launchClock(context) },
                    onLongClick = onOpenSettings
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Tier 1: Clock Display
            Row(
                modifier = Modifier.height(48.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeDigits,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    color = displayColor,
                    style = TextStyle(shadow = textShadow),
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    softWrap = false
                )
                if (amPm != null) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = amPm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = subtextColor,
                        style = TextStyle(shadow = textShadow),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tier 2: Date Display
            Text(
                text = formattedDate,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = subtextColor,
                style = TextStyle(shadow = textShadow),
                letterSpacing = 0.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.combinedClickable(
                    onClick = { launchCalendar(context) },
                    onLongClick = onOpenSettings
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right: DuoStatusIcon + Temperature (Side-by-Side) & Location (2 Tiers)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .combinedClickable(
                    onClick = onRefreshWeather,
                    onLongClick = onOpenSettings
                ),
            horizontalAlignment = Alignment.End
        ) {
            // Tier 1: DuoStatusIcon side-by-side with Temperature (Horizontally Centered)
            Row(
                modifier = Modifier.height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (showDuoStatus) {
                    DuoStatusIcon(
                        displayColor = displayColor,
                        sizeDp = 34,
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = onDuoStatusClick
                    )
                }

                if (showWeather) {
                    Text(
                        text = tempText,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        color = displayColor,
                        style = TextStyle(shadow = textShadow),
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tier 2: Weather Condition & Location Display
            if (showWeather) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = weatherLocationText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtextColor,
                        style = TextStyle(shadow = textShadow),
                        letterSpacing = 0.1.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = subtextColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = subtextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatTemperature(celsius: Double, unit: String): String {
    return if (unit.equals("FAHRENHEIT", ignoreCase = true)) {
        val fahrenheit = (celsius * 9.0 / 5.0) + 32.0
        "${fahrenheit.roundToInt()}°F"
    } else {
        "${celsius.roundToInt()}°C"
    }
}

private fun launchClock(context: Context) {
    try {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback: don't crash if no clock app found
    }
}

private fun launchCalendar(context: Context) {
    try {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        val intent = Intent(Intent.ACTION_VIEW, builder.build()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback: don't crash if no calendar app found
    }
}
