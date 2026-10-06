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
            val hideStatusBar by preferencesDataSource.hideStatusBar.collectAsStateWithLifecycle(initialValue = true)
            val wallpaperBlur by preferencesDataSource.wallpaperBlur.collectAsStateWithLifecycle(initialValue = false)
            val wallpaperBlurRadius by preferencesDataSource.wallpaperBlurRadius.collectAsStateWithLifecycle(initialValue = 25f)

            androidx.compose.runtime.LaunchedEffect(hideStatusBar) {
                isStatusBarHidden = hideStatusBar
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (hideStatusBar) {
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }

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

    private var isStatusBarHidden: Boolean = true

    override fun onResume() {
        super.onResume()
        enableHighRefreshRate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enableHighRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableHighRefreshRate()
            if (isStatusBarHidden) {
                runCatching {
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.systemBarsBehavior =
                        androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        try {
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = display?.supportedModes
            val highRefreshMode = modes?.filter { it.refreshRate >= 119.0f }
                ?.maxByOrNull { it.refreshRate }
                ?: modes?.maxByOrNull { it.refreshRate }
            val maxRate = highRefreshMode?.refreshRate ?: 120.0f
            val params = window.attributes
            if (highRefreshMode != null) {
                params.preferredDisplayModeId = highRefreshMode.modeId
            }
            params.preferredRefreshRate = maxRate

            // Lock display refresh rate bounds without barriers (Android 11+)
            runCatching {
                val minField = params.javaClass.getField("preferredMinDisplayRefreshRate")
                minField.set(params, maxRate)
            }
            runCatching {
                val maxField = params.javaClass.getField("preferredMaxDisplayRefreshRate")
                maxField.set(params, maxRate)
            }
            window.attributes = params

            // Set full-time frame rate on surface control without barriers
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                runCatching {
                    val rootSurfaceControl = window.decorView.rootSurfaceControl
                    if (rootSurfaceControl != null) {
                        val setFrameRateMethod = rootSurfaceControl.javaClass.getMethod(
                            "setFrameRate",
                            Float::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType
                        )
                        setFrameRateMethod.invoke(rootSurfaceControl, maxRate, 0)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
