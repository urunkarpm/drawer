package com.urunkarpm.drawer

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.designsystem.component.LocalMonochromeIcons
import com.urunkarpm.drawer.core.designsystem.haptics.LocalCrispHaptics
import com.urunkarpm.drawer.core.designsystem.haptics.rememberCrispHaptics
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

    private var homeResetTrigger by mutableLongStateOf(0L)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // ponytail: Android Home gesture/button triggers onNewIntent with ACTION_MAIN or CATEGORY_HOME; reset when activity is foregrounded to return to clean home page and collapse app drawer; ceiling is custom multi-instance task affinities; upgrade path is intent action dispatch routing.
        val isHomeIntent = intent.action == Intent.ACTION_MAIN ||
            intent.hasCategory(Intent.CATEGORY_HOME) ||
            intent.hasCategory(Intent.CATEGORY_DEFAULT)
        if (isHomeIntent) {
            homeResetTrigger = SystemClock.uptimeMillis()
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_HOME && event.action == android.view.KeyEvent.ACTION_UP) {
            homeResetTrigger = SystemClock.uptimeMillis()
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        com.urunkarpm.drawer.core.data.worker.AppUpdateCheckWorker.enqueuePeriodic(applicationContext)

        // ponytail: Home launcher is root of task stack; consume back press when no drawers/sheets open to prevent Android minimizing activity and exposing bare wallpaper; ceiling: no-op; upgrade path: scroll to home feed top.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No-op: Home launcher is root, never finish or minimize to desktop
            }
        })

        setContent {
            val dynamicColor by preferencesDataSource.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
            val accentColor by preferencesDataSource.accentColor.collectAsStateWithLifecycle(initialValue = "DYNAMIC")
            val hideStatusBar by preferencesDataSource.hideStatusBar.collectAsStateWithLifecycle(initialValue = true)
            val wallpaperBlur by preferencesDataSource.wallpaperBlur.collectAsStateWithLifecycle(initialValue = true)
            val wallpaperBlurRadius by preferencesDataSource.wallpaperBlurRadius.collectAsStateWithLifecycle(initialValue = 25f)
            val monochromeIcons by preferencesDataSource.monochromeIcons.collectAsStateWithLifecycle(initialValue = false)
            val hapticFeedbackEnabled by preferencesDataSource.hapticFeedbackEnabled.collectAsStateWithLifecycle(initialValue = true)
            val crispHaptics = rememberCrispHaptics(enabled = hapticFeedbackEnabled)

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
                        val params = window.attributes
                        // Keep FLAG_BLUR_BEHIND enabled on API 31+ so dynamic page transitions (such as To-Do page 20% blur) run at 120fps without relayout
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        params.blurBehindRadius = radius
                        window.attributes = params
                        window.setBackgroundBlurRadius(radius)
                        android.util.Log.i("MainActivity", "Applied wallpaper blur radius=$radius, blurBehindRadius=${params.blurBehindRadius}")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to apply wallpaper blur", e)
                    }
                }
            }

            var showAccessibilityPrompt by remember { mutableStateOf(false) }

            // ponytail: Launcher wallpaper overlay surfaces stay consistently dark; monochrome uses curated dark-grey palette; themeMode scopes only to SettingsScreen.
            DrawerTheme(
                darkTheme = true,
                isAmoled = false,
                dynamicColor = dynamicColor,
                accentColor = accentColor,
                monochromeMode = monochromeIcons
            ) {
                CompositionLocalProvider(
                    LocalCrispHaptics provides crispHaptics,
                    LocalMonochromeIcons provides monochromeIcons
                ) {
                    HomeScreen(
                        widgetHostManager = widgetHostManager,
                        homeResetTrigger = homeResetTrigger,
                        onDoubleTapLock = {
                            val locked = DrawerAccessibilityService.lockScreen()
                            if (!locked) {
                                showAccessibilityPrompt = true
                            }
                        }
                    )
                }

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
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        enableHighRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
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

    private var hasConfiguredRefreshRate = false

    private fun enableHighRefreshRate() {
        if (hasConfiguredRefreshRate) return
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

            // ponytail: update window attributes only once upon window attachment to prevent repeated relayout churn and LTPO mode renegotiation
            if (changed) {
                window.attributes = params
            }
            hasConfiguredRefreshRate = true
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHostManager.clearViewCache()
    }
}
