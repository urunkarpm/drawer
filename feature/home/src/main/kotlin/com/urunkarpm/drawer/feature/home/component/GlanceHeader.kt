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
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
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
    duoStatusPosition: String = "TEMP",
    isDuoStatusExpanded: Boolean = false,
    onToggleDuoStatusExpand: () -> Unit = {},
    onOpenVoiceSearch: (() -> Unit)? = null,
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

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val cornerRadiusDp = remember(context, density, configuration) {
        val cornerRadiusPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display
                } else null
                val corner = display?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_RIGHT)
                corner?.radius ?: 0
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }

        if (cornerRadiusPx > 0) {
            with(density) { cornerRadiusPx.toDp() }
        } else {
            when {
                configuration.screenWidthDp < 360 -> 16.dp
                configuration.screenWidthDp >= 600 -> 20.dp
                else -> 28.dp
            }
        }
    }

    // Status bar and corner-aware padding to give breathable spacing around the glance header
    val duoTopPadding = (cornerRadiusDp * 0.40f).coerceIn(10.dp, 16.dp)

    // Main Glance Row: Clock & Date (Left), Duo Status Icon (Center), and Temperature & Location (Right)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(100f)
            .padding(top = duoTopPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Live Clock & Date (2 Tiers)
        Column(
            modifier = Modifier
                .weight(1f)
                    .combinedClickable(
                        onClick = {
                            if (isDuoStatusExpanded) onToggleDuoStatusExpand() else launchClock(context)
                        },
                        onLongClick = onOpenSettings
                    ),
                horizontalAlignment = Alignment.Start
            ) {
                // Tier 1: Clock Display (Exact height 48.dp & matching baseline as Temperature)
                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeDigits,
                        fontFamily = LocalHeaderFontFamily.current,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Normal,
                        color = displayColor,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 44.sp,
                            shadow = textShadow
                        ),
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alignByBaseline()
                    )
                    if (amPm != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = amPm,
                            fontFamily = LocalHeaderFontFamily.current,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = subtextColor,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                shadow = textShadow
                            ),
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tier 2: Date Display (Matching 24.dp height & vertical center as weather location)
                Row(
                    modifier = Modifier.height(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontFamily = LocalHeaderFontFamily.current,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtextColor,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = textShadow
                        ),
                        letterSpacing = 0.1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (isDuoStatusExpanded) onToggleDuoStatusExpand() else launchCalendar(context)
                            },
                            onLongClick = onOpenSettings
                        )
                    )
                }
            }

        // Center: Duo Status Icon in between Time and Temp
        if (showDuoStatus) {
            Box(
                modifier = Modifier
                    .width(45.dp)
                    .height(48.dp)
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                // Collapsed Duo Status Icon in regular header layout flow
                DuoStatusIcon(
                    displayColor = displayColor,
                    sizeDp = 37,
                    isExpanded = false,
                    onToggleExpand = onToggleDuoStatusExpand,
                    duoStatusPosition = "CENTER"
                )

                if (isDuoStatusExpanded) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        onDismissRequest = onToggleDuoStatusExpand,
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true
                        )
                    ) {
                        DuoStatusIcon(
                            displayColor = displayColor,
                            sizeDp = 37,
                            isExpanded = true,
                            onToggleExpand = onToggleDuoStatusExpand,
                            duoStatusPosition = "CENTER"
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Right: Temperature & Location (2 Tiers) - Exactly matching left side alignment!
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {
                        if (isDuoStatusExpanded) onToggleDuoStatusExpand() else onRefreshWeather()
                    },
                    onLongClick = onOpenSettings
                ),
            horizontalAlignment = Alignment.End
        ) {
            // Tier 1: Temperature Display (Same height 48.dp, lineHeight 44.sp & exact baseline as Clock)
            if (showWeather) {
                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = tempText,
                        fontFamily = LocalHeaderFontFamily.current,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Normal,
                        color = displayColor,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 44.sp,
                            shadow = textShadow
                        ),
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .alignByBaseline()
                            .padding(end = 4.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tier 2: Weather Condition & Location Display (Same height 24.dp & baseline as Date)
            if (showWeather) {
                Row(
                    modifier = Modifier.height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = weatherLocationText,
                        fontFamily = LocalHeaderFontFamily.current,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtextColor,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = textShadow
                        ),
                        letterSpacing = 0.1.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (onOpenVoiceSearch != null) {
                                onOpenVoiceSearch()
                            } else {
                                launchSpeechRecognizer(context)
                            }
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search / AI Actions",
                            tint = subtextColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                Row(
                    modifier = Modifier.height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            if (onOpenVoiceSearch != null) {
                                onOpenVoiceSearch()
                            } else {
                                launchSpeechRecognizer(context)
                            }
                        },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search / AI Actions",
                            tint = subtextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            tint = subtextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

private fun launchSpeechRecognizer(context: Context) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback: don't crash if no speech recognizer found
    }
}
