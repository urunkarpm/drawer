package com.urunkarpm.drawer

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme
import com.urunkarpm.drawer.feature.home.HomeScreen
import com.urunkarpm.drawer.service.DrawerAccessibilityService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataSource: DrawerPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        com.urunkarpm.drawer.core.data.worker.WeatherRefreshWorker.enqueuePeriodic(this)
        setContent {
            val themeMode by preferencesDataSource.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val dynamicColor by preferencesDataSource.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
            val wallpaperBlur by preferencesDataSource.wallpaperBlur.collectAsStateWithLifecycle(initialValue = false)
            val wallpaperBlurRadius by preferencesDataSource.wallpaperBlurRadius.collectAsStateWithLifecycle(initialValue = 25f)

            androidx.compose.runtime.LaunchedEffect(wallpaperBlur, wallpaperBlurRadius) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    try {
                        val radius = if (wallpaperBlur) wallpaperBlurRadius.toInt().coerceAtLeast(1) else 0
                        window.setBackgroundBlurRadius(radius)
                    } catch (_: Exception) {}
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK", "AMOLED" -> true
                else -> isSystemDark
            }
            val isAmoled = themeMode == "AMOLED"

            var showAccessibilityPrompt by remember { mutableStateOf(false) }

            DrawerTheme(
                darkTheme = isDark,
                isAmoled = isAmoled,
                dynamicColor = dynamicColor
            ) {
                HomeScreen(
                    onDoubleTapLock = {
                        val locked = DrawerAccessibilityService.lockScreen()
                        if (!locked) {
                            showAccessibilityPrompt = true
                        }
                    }
                )

                if (showAccessibilityPrompt) {
                    AlertDialog(
                        onDismissRequest = { showAccessibilityPrompt = false },
                        title = { Text("Double-Tap to Lock") },
                        text = {
                            Text("To lock your screen with a double-tap, enable 'Drawer Screen Lock' in Accessibility settings.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showAccessibilityPrompt = false
                                    try {
                                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAccessibilityPrompt = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        try {
            // Setting preferredRefreshRate and displayModeId to 0 lets the system dynamic refresh rate policy (LTPO VRR)
            // scale down when idle to conserve battery and scale up during touches/animations
            val params = window.attributes
            params.preferredRefreshRate = 0f
            params.preferredDisplayModeId = 0
            window.attributes = params
        } catch (_: Exception) {}
    }
}
