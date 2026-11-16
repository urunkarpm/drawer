package com.urunkarpm.drawer.feature.home.walkthrough

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.WeatherInfo

// ponytail: streamlined single-dialog welcome guide; replaces 1,450 lines of duplicate settings controls with essential gesture orientation.
@Composable
fun WalkthroughOverlay(
    onDismiss: () -> Unit,
    // Stage 1: Layout & Ergonomics
    twoDrawersSideBySide: Boolean = false,
    onSetTwoDrawersSideBySide: (Boolean) -> Unit = {},
    categoryAlignment: String = "BOTTOM",
    onSetCategoryAlignment: (String) -> Unit = {},
    enableWidgetsPage: Boolean = true,
    onSetEnableWidgetsPage: (Boolean) -> Unit = {},
    drawerTitleAnimation: String = "ACCENT_POP",
    onSetDrawerTitleAnimation: (String) -> Unit = {},
    openDrawerFromDockOnly: Boolean = false,
    onSetOpenDrawerFromDockOnly: (Boolean) -> Unit = {},
    autoArrangeApps: Boolean = true,
    onSetAutoArrangeApps: (Boolean) -> Unit = {},
    lockLayout: Boolean = false,
    onSetLockLayout: (Boolean) -> Unit = {},
    autoCategorizeNewApps: Boolean = true,
    onSetAutoCategorizeNewApps: (Boolean) -> Unit = {},

    // Stage 2: Colors, Surfaces & Palettes
    themeMode: String = "SYSTEM",
    onSetThemeMode: (String) -> Unit = {},
    drawerThemeMode: String = "SYSTEM",
    onSetDrawerThemeMode: (String) -> Unit = {},
    dynamicColor: Boolean = true,
    onSetDynamicColor: (Boolean) -> Unit = {},
    accentColor: String = "DYNAMIC",
    onSetAccentColor: (String) -> Unit = {},
    cinematicPalette: String = "DEFAULT",
    onSetCinematicPalette: (String) -> Unit = {},
    surfaceThemeStyle: String = "LIQUID_GLASS",
    onSetSurfaceThemeStyle: (String) -> Unit = {},
    surfaceOpacity: Float = 0.65f,
    onSetSurfaceOpacity: (Float) -> Unit = {},
    surfaceBlurRadius: Float = 25f,
    onSetSurfaceBlurRadius: (Float) -> Unit = {},
    surfaceStrokeOpacity: Float = 0.40f,
    onSetSurfaceStrokeOpacity: (Float) -> Unit = {},
    surfaceCornerRadius: Float = 24f,
    onSetSurfaceCornerRadius: (Float) -> Unit = {},

    // Stage 3: Wallpaper & Ambiance
    wallpaperBlur: Boolean = true,
    onSetWallpaperBlur: (Boolean) -> Unit = {},
    wallpaperBlurRadius: Float = 25f,
    onSetWallpaperBlurRadius: (Float) -> Unit = {},
    isDynamicWallpaper: Boolean = false,
    onSelectDynamicWallpaper: (Boolean) -> Unit = {},
    wallpaperGradientSpeedSeconds: Int = 3,
    onSetWallpaperGradientSpeedSeconds: (Int) -> Unit = {},
    wallpaperGradientPalette: String = "FULL_SPECTRUM",
    onSetWallpaperGradientPalette: (String) -> Unit = {},
    wallpaperGradientAngle: String = "DIAGONAL",
    onSetWallpaperGradientAngle: (String) -> Unit = {},
    wallpaperGradientBlurPercent: Int = 40,
    onSetWallpaperGradientBlurPercent: (Int) -> Unit = {},
    hideStatusBar: Boolean = true,
    onSetHideStatusBar: (Boolean) -> Unit = {},

    // Stage 4: Icons, Typography & Dock
    adaptiveIconShape: String = "CIRCLE",
    onSetAdaptiveIconShape: (String) -> Unit = {},
    headerFontFamily: String = "SYSTEM",
    onSetHeaderFontFamily: (String) -> Unit = {},
    appLabelScale: String = "NORMAL",
    onSetAppLabelScale: (String) -> Unit = {},
    hideDrawerIcons: Boolean = false,
    onSetHideDrawerIcons: (Boolean) -> Unit = {},
    dockMesmerizingGlow: Boolean = true,
    onSetDockMesmerizingGlow: (Boolean) -> Unit = {},
    dockShowLabels: Boolean = false,
    onSetDockShowLabels: (Boolean) -> Unit = {},
    dockIconSize: Float = 56f,
    onSetDockIconSize: (Float) -> Unit = {},

    // Stage 5: Header, Interactions & Privacy
    is24Hour: Boolean = true,
    onSetIs24Hour: (Boolean) -> Unit = {},
    showWeather: Boolean = true,
    onSetShowWeather: (Boolean) -> Unit = {},
    weatherUnit: String = "CELSIUS",
    onSetWeatherUnit: (String) -> Unit = {},
    showDuoStatusWidget: Boolean = true,
    onSetShowDuoStatusWidget: (Boolean) -> Unit = {},
    autoOpenKeyboardInDrawer: Boolean = false,
    onSetAutoOpenKeyboardInDrawer: (Boolean) -> Unit = {},
    blurAppListOnOpen: Boolean = false,
    onSetBlurAppListOnOpen: (Boolean) -> Unit = {},
    showDrawerSuggestions: Boolean = true,
    onSetShowDrawerSuggestions: (Boolean) -> Unit = {},
    stealthTheme: Boolean = false,
    onSetStealthTheme: (Boolean) -> Unit = {},
    stealthContrastLevel: String = "LOW",
    onSetStealthContrastLevel: (String) -> Unit = {},

    // Real App Data
    sampleApps: List<AppInfo> = emptyList(),
    iconLoader: (suspend (AppInfo) -> android.graphics.drawable.Drawable?)? = null,
    weatherInfo: WeatherInfo? = null,

    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onDismiss()
    }

    fun launchDefaultPrompt() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    roleLauncher.launch(intent)
                    return
                }
            }
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.70f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Drawer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Fast, minimal, and gesture-driven.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Gesture Guides
                GestureRow(
                    icon = Icons.Default.Home,
                    title = "Swipe Up",
                    description = "Open your app drawers"
                )
                Spacer(modifier = Modifier.height(10.dp))
                GestureRow(
                    icon = Icons.Default.Notifications,
                    title = "Swipe Down",
                    description = "View notifications & quick access"
                )
                Spacer(modifier = Modifier.height(10.dp))
                GestureRow(
                    icon = Icons.Default.TaskAlt,
                    title = "Swipe Right",
                    description = "To-Do tasks & reminders"
                )
                Spacer(modifier = Modifier.height(10.dp))
                GestureRow(
                    icon = Icons.Default.Tune,
                    title = "Long Press",
                    description = "Access Settings & customization"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { launchDefaultPrompt() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Default", maxLines = 1)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Started", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
