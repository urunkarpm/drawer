package com.urunkarpm.drawer

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.urunkarpm.drawer.core.common.widget.DrawerWidgetHostManager
import com.urunkarpm.drawer.feature.home.HomeScreen
import com.urunkarpm.drawer.service.DrawerAccessibilityService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataSource: DrawerPreferencesDataSource

    @Inject
    lateinit var widgetHostManager: DrawerWidgetHostManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        com.urunkarpm.drawer.core.data.worker.WeatherRefreshWorker.enqueuePeriodic(this)
        setContent {
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

            var showAccessibilityPrompt by remember { mutableStateOf(false) }

            // ponytail: Launcher wallpaper overlay surfaces stay consistently dark; themeMode scopes only to SettingsScreen.
            DrawerTheme(
                darkTheme = true,
                isAmoled = false,
                dynamicColor = dynamicColor
            ) {
                HomeScreen(
                    widgetHostManager = widgetHostManager,
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
                            Text(
                                "To lock your screen with a double-tap, enable 'Drawer Screen Lock' in Accessibility settings.\n\n" +
                                "Drawer uses the AccessibilityService API solely to perform the screen-lock action (GLOBAL_ACTION_LOCK_SCREEN). " +
                                "No personal information or screen content is accessed, collected, or stored."
                            )
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

    override fun onStart() {
        super.onStart()
        widgetHostManager.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHostManager.stopListening()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DrawerWidgetHostManager.REQUEST_CONFIGURE_APPWIDGET) {
            widgetHostManager.onWidgetConfigured(resultCode, data)
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
            // Target native 120Hz mode (closest to 120fps) for butter-smooth 120Hz frame pacing
            val targetRate = 120.0f
            val targetMode = modes?.minByOrNull { kotlin.math.abs(it.refreshRate - targetRate) }
                ?: modes?.maxByOrNull { it.refreshRate }
            val refreshRate = targetMode?.refreshRate ?: 120.0f
            val params = window.attributes
            var changed = false
            if (targetMode != null && params.preferredDisplayModeId != targetMode.modeId) {
                params.preferredDisplayModeId = targetMode.modeId
                changed = true
            }
            if (params.preferredRefreshRate != refreshRate) {
                params.preferredRefreshRate = refreshRate
                changed = true
            }

            // ponytail: update window attributes only when mode/rate actually changes to prevent repeated relayout churn
            if (changed) {
                window.attributes = params
            }
        } catch (_: Exception) {}
    }
}
