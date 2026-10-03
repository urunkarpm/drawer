package com.urunkarpm.drawer.feature.home.component

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.model.WeatherInfo
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun GlanceHeader(
    is24Hour: Boolean,
    showWeather: Boolean,
    weatherUnit: String,
    weatherInfo: WeatherInfo?,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    // Update time every 10 seconds to keep live clock accurate
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            currentDate = LocalDate.now()
            delay(10_000)
        }
    }

    val timeFormat = if (is24Hour) "HH:mm" else "h:mm a"
    val formattedTime = currentTime.format(DateTimeFormatter.ofPattern(timeFormat))
    val formattedDate = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        // Large Clock Display
        Text(
            text = formattedTime,
            fontSize = 60.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1).sp,
            modifier = Modifier.clickable {
                launchClock(context)
            }
        )

        // Date Display
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.clickable {
                launchCalendar(context)
            }
        )

        // Weather Pill / Widget
        AnimatedVisibility(
            visible = showWeather,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            if (weatherInfo != null) {
                val tempText = formatTemperature(weatherInfo.temperatureCelsius, weatherUnit)
                val icon = getWeatherIcon(weatherInfo.weatherCode)

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onRefreshWeather() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = weatherInfo.conditionDescription,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$tempText • ${weatherInfo.conditionDescription}, ${weatherInfo.cityName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onRefreshWeather() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tap to load weather",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap to load weather",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun getWeatherIcon(weatherCode: Int): ImageVector {
    return when (weatherCode) {
        0, 1 -> Icons.Default.WbSunny
        2 -> Icons.Default.CloudQueue
        3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.Dehaze
        51, 53, 55, 56, 57 -> Icons.Default.WaterDrop
        61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Default.WaterDrop
        71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
        95, 96, 99 -> Icons.Default.FlashOn
        else -> Icons.Default.WbSunny
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
