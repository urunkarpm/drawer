package com.urunkarpm.drawer.feature.home.component

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlanceHeader(
    is24Hour: Boolean,
    showWeather: Boolean,
    weatherUnit: String,
    weatherInfo: WeatherInfo?,
    onRefreshWeather: () -> Unit,
    onOpenSettings: () -> Unit = {},
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

    val timeFormat = if (is24Hour) "HH:mm" else "h:mm a"
    val formattedTime = currentTime.format(DateTimeFormatter.ofPattern(timeFormat))
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

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val displayColor = if (isLight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    val subtextColor = if (isLight) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)

    // ponytail: 28.dp top padding positions the clock and weather safely below the display camera cut-out/punch hole; ceiling is static 28.dp offset; upgrade path is dynamic camera cutout bounding rect inspection.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Live Clock & Date
        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = { launchClock(context) },
                    onLongClick = onOpenSettings
                ),
            horizontalAlignment = Alignment.Start
        ) {
            // Large Clock Display
            Text(
                text = formattedTime,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                color = displayColor,
                letterSpacing = (-1).sp,
                maxLines = 1,
                softWrap = false
            )

            // Date Display with high-contrast text
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.combinedClickable(
                    onClick = { launchCalendar(context) },
                    onLongClick = onOpenSettings
                )
            )
        }

        // ponytail: right-side weather/location mirrors left-side time/date typography directly for visual symmetry; ceiling is 2-line summary; upgrade path is expanding a forecast card on tap.
        // ponytail: long-press on clock or weather opens settings to maintain clean symmetrical header without an intrusive gear icon; ceiling is hidden affordance; upgrade path is quick settings tile or dock shortcut.
        if (showWeather) {
            // Right: Temperature, Weather & Location (symmetric to Time & Date)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onRefreshWeather,
                        onLongClick = onOpenSettings
                    ),
                horizontalAlignment = Alignment.End
            ) {
                // Large Temperature Display
                Text(
                    text = tempText,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = displayColor,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )

                // Weather Condition & Location Display
                Text(
                    text = weatherLocationText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = subtextColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
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
