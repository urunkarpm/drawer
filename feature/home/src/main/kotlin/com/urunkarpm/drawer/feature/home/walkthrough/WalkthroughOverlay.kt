package com.urunkarpm.drawer.feature.home.walkthrough

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.designsystem.component.DockSurfaceBackground
import com.urunkarpm.drawer.core.designsystem.component.DynamicPantoneWallpaper
import com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString
import com.urunkarpm.drawer.core.designsystem.theme.CinematicPalette
import com.urunkarpm.drawer.core.designsystem.theme.CinematicPalettes
import com.urunkarpm.drawer.core.designsystem.theme.VibrantAccentColors
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.WeatherInfo
import kotlinx.coroutines.launch

private val STUDIO_TABS = listOf(
    "Layout",
    "Surfaces",
    "Wallpaper",
    "Icons & Dock",
    "Behavior",
    "Ready"
)

@OptIn(ExperimentalLayoutApi::class)
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
    multiGroupApps: Boolean = false,
    onSetMultiGroupApps: (Boolean) -> Unit = {},
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
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { STUDIO_TABS.size })
    var isMinimized by remember { mutableStateOf(false) }
    var showDefaultDialog by remember { mutableStateOf(false) }

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
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            roleLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS)
                roleLauncher.launch(fallback)
            } catch (_: Exception) {
                onDismiss()
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (isMinimized) {
            // Floating pill to restore setup studio while inspecting full screen launcher
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .clickable { isMinimized = false },
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Customize Launcher",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Text(
                        text = "Done ✓",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
            }
        } else {
            // Touch-through / Tap-to-minimize transparent area in upper screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.44f)
                    .align(Alignment.TopCenter)
                    .clickable { isMinimized = true }
            )

            // Bottom Personalization Studio (56% height, anchored at bottom)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.56f)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 8.dp,
                shadowElevation = 20.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    // Studio Header: Drag Handle & Actions (Peek & Done)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 38.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                .clickable { isMinimized = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Personalize Launcher",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Changes apply live to your real homescreen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Peek Fullscreen button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable { isMinimized = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Peek Homescreen",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Peek",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable Category Tabs Bar
                        ScrollableTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            edgePadding = 12.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            STUDIO_TABS.forEachIndexed { index, title ->
                                val isSelected = pagerState.currentPage == index
                                Tab(
                                    selected = isSelected,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }

                        // Tab Page Contents
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { pageIndex ->
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                when (pageIndex) {
                                    0 -> {
                                        // ── TAB 0: LAYOUT & ERGONOMICS ─────────────────────
                                        StudioHeader("Drawer Position", "Stack category drawers up from the bottom or down from the header")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StudioChoiceCard(
                                                title = "Bottom (Down)",
                                                subtitle = "Thumb-friendly reach",
                                                isSelected = categoryAlignment.equals("BOTTOM", ignoreCase = true),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSetCategoryAlignment("BOTTOM") }
                                            )
                                            StudioChoiceCard(
                                                title = "Top (Up)",
                                                subtitle = "Classic top alignment",
                                                isSelected = categoryAlignment.equals("TOP", ignoreCase = true),
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSetCategoryAlignment("TOP") }
                                            )
                                        }

                                        StudioHeader("Drawer Columns", "Single column or dual columns side-by-side on home screen")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StudioChoiceCard(
                                                title = "1 Unified Column",
                                                subtitle = "Clean single list",
                                                isSelected = !twoDrawersSideBySide,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSetTwoDrawersSideBySide(false) }
                                            )
                                            StudioChoiceCard(
                                                title = "2 Dual Columns",
                                                subtitle = "Side-by-side multitasking",
                                                isSelected = twoDrawersSideBySide,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSetTwoDrawersSideBySide(true) }
                                            )
                                        }

                                        StudioHeader("Drawer Title Animation", "Animate title across the card when opening or closing")
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "ACCENT_POP" to "Accent Pop",
                                                "FLUID_SPRING" to "Elastic",
                                                "KINETIC_DECEL" to "Sleek Decel",
                                                "SNAPPY" to "Snappy",
                                                "OFF" to "Off"
                                            ).forEach { (animKey, label) ->
                                                StudioChip(
                                                    label = label,
                                                    isSelected = drawerTitleAnimation.equals(animKey, ignoreCase = true),
                                                    onClick = { onSetDrawerTitleAnimation(animKey) }
                                                )
                                            }
                                        }

                                        StudioHeader("Workspace Navigation", "Toggle gestures, sorting, and pages")
                                        StudioSwitch(
                                            title = "Dedicated Widgets Page",
                                            subtitle = "Swipe right to open dedicated full-page widgets screen",
                                            checked = enableWidgetsPage,
                                            onCheckedChange = onSetEnableWidgetsPage
                                        )
                                        StudioSwitch(
                                            title = "Open Drawer from Dock Only",
                                            subtitle = "Only open drawer when tapping the dock icon (disables swipe-up)",
                                            checked = openDrawerFromDockOnly,
                                            onCheckedChange = onSetOpenDrawerFromDockOnly
                                        )
                                        StudioSwitch(
                                            title = "Free Drag & Auto-Arrange",
                                            subtitle = "Automatically sort and snap apps alphabetically",
                                            checked = autoArrangeApps,
                                            onCheckedChange = onSetAutoArrangeApps
                                        )
                                        StudioSwitch(
                                            title = "Multi-Group Apps",
                                            subtitle = "Allow assigning a single app to multiple categories",
                                            checked = multiGroupApps,
                                            onCheckedChange = onSetMultiGroupApps
                                        )
                                        StudioSwitch(
                                            title = "Lock Home Layout",
                                            subtitle = "Prevent accidental dragging and category modifications",
                                            checked = lockLayout,
                                            onCheckedChange = onSetLockLayout
                                        )
                                        StudioSwitch(
                                            title = "Auto-Categorize New Apps",
                                            subtitle = "Automatically organize newly installed apps using on-device intelligence",
                                            checked = autoCategorizeNewApps,
                                            onCheckedChange = onSetAutoCategorizeNewApps
                                        )
                                    }

                                    1 -> {
                                        // ── TAB 1: THEMES, SURFACES & PALETTES ─────────────
                                        StudioHeader("Theme Palette Mode", "Light, Dark, AMOLED, or match device system theme")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "SYSTEM" to "System",
                                                "LIGHT" to "Light",
                                                "DARK" to "Dark",
                                                "AMOLED" to "AMOLED"
                                            ).forEach { (mode, label) ->
                                                StudioChoiceCard(
                                                    title = label,
                                                    subtitle = if (mode == "SYSTEM") "Auto" else mode,
                                                    isSelected = themeMode.equals(mode, ignoreCase = true),
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        onSetThemeMode(mode)
                                                        onSetDrawerThemeMode(if (mode == "LIGHT") "WHITE" else mode)
                                                    }
                                                )
                                            }
                                        }

                                        StudioSwitch(
                                            title = "Dynamic Color (Material You)",
                                            subtitle = "Derive accent highlights from your current wallpaper",
                                            checked = dynamicColor,
                                            onCheckedChange = onSetDynamicColor
                                        )

                                        StudioHeader("Vibrant Accent Color", "Pick an energetic, non-muted accent hue or sync with wallpaper")
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            VibrantAccentColors.palettes.forEach { palette ->
                                                val isSelected = accentColor.equals(palette.id, ignoreCase = true)
                                                val isDynamic = palette.id.equals(VibrantAccentColors.ID_DYNAMIC, ignoreCase = true)

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clickable {
                                                            onSetAccentColor(palette.id)
                                                            if (isDynamic) {
                                                                onSetDynamicColor(true)
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(CircleShape)
                                                            .then(
                                                                if (isDynamic) {
                                                                    Modifier.background(
                                                                        Brush.sweepGradient(
                                                                            listOf(
                                                                                Color(0xFF3B82F6),
                                                                                Color(0xFF10B981),
                                                                                Color(0xFFF59E0B),
                                                                                Color(0xFFEF4444),
                                                                                Color(0xFF8B5CF6),
                                                                                Color(0xFF3B82F6)
                                                                            )
                                                                        )
                                                                    )
                                                                } else {
                                                                    Modifier.background(palette.color)
                                                                }
                                                            )
                                                            .border(
                                                                width = if (isSelected) 3.dp else 1.5.dp,
                                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                                                shape = CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = palette.name,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }

                                        StudioHeader("Cinematic Color Grading", "Signature Hollywood film-stocks applied to glass rims and category cards")
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CinematicPalettes.allPalettes.forEach { palette ->
                                                val isSelected = cinematicPalette.equals(palette.id, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    border = BorderStroke(
                                                        if (isSelected) 2.dp else 1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onSetCinematicPalette(palette.id) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // 3-tone color swatch pills
                                                        Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                                                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(palette.primaryAccent).border(1.dp, Color.White, CircleShape))
                                                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(palette.secondaryAccent).border(1.dp, Color.White, CircleShape))
                                                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(palette.rimStart).border(1.dp, Color.White, CircleShape))
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(text = palette.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            Text(text = palette.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        if (isSelected) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        StudioHeader("Surface Aesthetic Finish", "Tactile finish for the dock and drawers")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "LIQUID_GLASS" to "Glass",
                                                "BLUR" to "Blur",
                                                "SOLID" to "Solid",
                                                "TRANSPARENT" to "Clear"
                                            ).forEach { (style, label) ->
                                                StudioChoiceCard(
                                                    title = label,
                                                    subtitle = when (style) {
                                                        "LIQUID_GLASS" -> "Specular"
                                                        "BLUR" -> "Frosted"
                                                        "SOLID" -> "OLED"
                                                        else -> "Invisible"
                                                    },
                                                    isSelected = surfaceThemeStyle.equals(style, ignoreCase = true),
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { onSetSurfaceThemeStyle(style) }
                                                )
                                            }
                                        }

                                        if (surfaceThemeStyle != "TRANSPARENT" && surfaceThemeStyle != "SOLID") {
                                            StudioSlider(
                                                title = "Surface Opacity",
                                                value = surfaceOpacity,
                                                onValueChange = onSetSurfaceOpacity,
                                                valueRange = 0.10f..1.0f,
                                                valueLabel = "${(surfaceOpacity * 100).toInt()}%"
                                            )
                                            StudioSlider(
                                                title = "Blur Depth Intensity",
                                                value = surfaceBlurRadius,
                                                onValueChange = onSetSurfaceBlurRadius,
                                                valueRange = 5f..80f,
                                                valueLabel = "${surfaceBlurRadius.toInt()} px"
                                            )
                                            if (surfaceThemeStyle == "LIQUID_GLASS") {
                                                StudioSlider(
                                                    title = "Glass Highlight (Specular Rim)",
                                                    value = surfaceStrokeOpacity,
                                                    onValueChange = onSetSurfaceStrokeOpacity,
                                                    valueRange = 0f..1.0f,
                                                    valueLabel = "${(surfaceStrokeOpacity * 100).toInt()}%"
                                                )
                                            }
                                        }

                                        if (surfaceThemeStyle != "TRANSPARENT") {
                                            StudioSlider(
                                                title = "Corner Curvature",
                                                value = surfaceCornerRadius,
                                                onValueChange = onSetSurfaceCornerRadius,
                                                valueRange = 0f..36f,
                                                valueLabel = "${surfaceCornerRadius.toInt()} dp"
                                            )
                                        }
                                    }

                                    2 -> {
                                        // ── TAB 2: WALLPAPER & AMBIANCE ────────────────────
                                        StudioHeader("Dynamic Pantone Wallpaper", "Smooth gradient transitions between authentic Pantone colors")
                                        StudioSwitch(
                                            title = "Enable Dynamic Pantone Wallpaper",
                                            subtitle = "Live morphing gradient with 40% blur, or keep device wallpaper",
                                            checked = isDynamicWallpaper,
                                            onCheckedChange = onSelectDynamicWallpaper
                                        )

                                        if (isDynamicWallpaper) {
                                            StudioHeader("Shift Speed", "Cycle duration for color morphing")
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(2 to "2s", 3 to "3s", 5 to "5s", 8 to "8s", 12 to "12s").forEach { (speed, label) ->
                                                    StudioChip(
                                                        label = label,
                                                        isSelected = wallpaperGradientSpeedSeconds == speed,
                                                        onClick = { onSetWallpaperGradientSpeedSeconds(speed) }
                                                    )
                                                }
                                            }

                                            StudioHeader("Pantone Palette", "Harmonized authentic color spectrums")
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    "FULL_SPECTRUM" to "Full Spectrum",
                                                    "PANTONE_COTY" to "Color of Year",
                                                    "AURORA_BOREALIS" to "Aurora",
                                                    "SUNSET_CHROMA" to "Sunset",
                                                    "DEEP_TWILIGHT" to "Twilight",
                                                    "PASTEL_DREAM" to "Pastel"
                                                ).forEach { (palKey, label) ->
                                                    StudioChip(
                                                        label = label,
                                                        isSelected = wallpaperGradientPalette.equals(palKey, ignoreCase = true),
                                                        onClick = { onSetWallpaperGradientPalette(palKey) }
                                                    )
                                                }
                                            }

                                            StudioHeader("Flow Direction", "Angle of gradient flow across screen")
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf("DIAGONAL" to "Diagonal", "VERTICAL" to "Vertical", "RADIAL" to "Radial").forEach { (angle, label) ->
                                                    StudioChoiceCard(
                                                        title = label,
                                                        subtitle = angle,
                                                        isSelected = wallpaperGradientAngle.equals(angle, ignoreCase = true),
                                                        modifier = Modifier.weight(1f),
                                                        onClick = { onSetWallpaperGradientAngle(angle) }
                                                    )
                                                }
                                            }

                                            StudioSlider(
                                                title = "Gradient Blur Diffusion",
                                                value = wallpaperGradientBlurPercent.toFloat(),
                                                onValueChange = { onSetWallpaperGradientBlurPercent(it.toInt()) },
                                                valueRange = 0f..100f,
                                                valueLabel = "$wallpaperGradientBlurPercent%"
                                            )
                                        }

                                        StudioHeader("Home Wallpaper Blur", "Frosted glass blur applied over system wallpaper")
                                        StudioSwitch(
                                            title = "Blur System Wallpaper",
                                            subtitle = "Apply soft frosted blur to home screen background",
                                            checked = wallpaperBlur,
                                            onCheckedChange = onSetWallpaperBlur
                                        )

                                        if (wallpaperBlur) {
                                            StudioSlider(
                                                title = "Wallpaper Blur Depth",
                                                value = wallpaperBlurRadius,
                                                onValueChange = onSetWallpaperBlurRadius,
                                                valueRange = 5f..100f,
                                                valueLabel = "${wallpaperBlurRadius.toInt()} px"
                                            )
                                        }

                                        StudioHeader("Immersive Fullscreen", "Hide status bar for distraction-free home screen")
                                        StudioSwitch(
                                            title = "Hide System Status Bar",
                                            subtitle = "Immersive edge-to-edge experience (battery/temp accessible via Duo Status)",
                                            checked = hideStatusBar,
                                            onCheckedChange = onSetHideStatusBar
                                        )
                                    }

                                    3 -> {
                                        // ── TAB 3: ICONS, TYPOGRAPHY & DOCK ────────────────
                                        StudioHeader("Adaptive Icon Mask", "Uniform geometric shape for system and third-party apps")
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(
                                                "CIRCLE" to "Circle",
                                                "SQUIRCLE" to "Squircle",
                                                "ROUNDED_SQUARE" to "Square",
                                                "TEARDROP" to "Teardrop",
                                                "SYSTEM" to "System"
                                            ).forEach { (shapeKey, label) ->
                                                val icShape = iconShapeFromString(shapeKey)
                                                val isSelected = adaptiveIconShape.equals(shapeKey, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    border = BorderStroke(
                                                        if (isSelected) 2.dp else 1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                    ),
                                                    modifier = Modifier
                                                        .width(76.dp)
                                                        .clickable { onSetAdaptiveIconShape(shapeKey) }
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(icShape)
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                                )
                                                        )
                                                        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }

                                        StudioHeader("Header & Clock Typography", "Select display font for clock, date, and group headers")
                                        var fontDropdownExpanded by remember { mutableStateOf(false) }
                                        val fontOptions = remember {
                                            listOf(
                                                "GOOGLE_SANS" to "Google Sans",
                                                "DS_DIGITAL" to "DS-Digital",
                                                "SYSTEM" to "System Sans",
                                                "ROBOTO" to "Roboto",
                                                "TEXAS_TROUPE" to "Texas Troupe",
                                                "MONOSPACE" to "Monospace",
                                                "SPOOKYY" to "Spookyy",
                                                "EVIL_EMPIRE" to "Evil Empire",
                                                "HYPIK" to "Hypik",
                                                "HELVETICA" to "Helvetica",
                                                "TIMES_NEW_ROMAN" to "Times New Roman",
                                                "ARIAL" to "Arial",
                                                "SERIF" to "Serif",
                                                "CURSIVE" to "Cursive"
                                            )
                                        }
                                        val currentFontName = fontOptions.find { it.first.equals(headerFontFamily, ignoreCase = true) }?.second ?: headerFontFamily

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { fontDropdownExpanded = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = currentFontName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "Sample: 12:45 • Social • Notes", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Icon(
                                                        imageVector = if (fontDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                        contentDescription = null
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = fontDropdownExpanded,
                                                onDismissRequest = { fontDropdownExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 380.dp)
                                            ) {
                                                fontOptions.forEach { (key, label) ->
                                                    val isSelected = headerFontFamily.equals(key, ignoreCase = true)
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(text = label, fontFamily = headerFontFamilyFromName(key), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                                if (isSelected) {
                                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            onSetHeaderFontFamily(key)
                                                            fontDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        StudioHeader("App Label Scale", "Scale text size for app names across drawer and dock")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("SMALL" to "Small", "NORMAL" to "Default", "LARGE" to "Large").forEach { (scaleKey, label) ->
                                                StudioChoiceCard(
                                                    title = label,
                                                    subtitle = scaleKey,
                                                    isSelected = appLabelScale.equals(scaleKey, ignoreCase = true),
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { onSetAppLabelScale(scaleKey) }
                                                )
                                            }
                                        }

                                        StudioSwitch(
                                            title = "Text-Only App List",
                                            subtitle = "Hide app icons in drawer for a clean minimalist text list",
                                            checked = hideDrawerIcons,
                                            onCheckedChange = onSetHideDrawerIcons
                                        )

                                        StudioHeader("Floating Quick-Launch Dock", "Customize dock icon scale, glow, and labels")
                                        StudioSlider(
                                            title = "Dock Icon Size",
                                            value = dockIconSize,
                                            onValueChange = onSetDockIconSize,
                                            valueRange = 40f..72f,
                                            valueLabel = "${dockIconSize.toInt()} dp"
                                        )
                                        StudioSwitch(
                                            title = "Show App Labels on Dock",
                                            subtitle = "Display app name underneath each dock icon",
                                            checked = dockShowLabels,
                                            onCheckedChange = onSetDockShowLabels
                                        )
                                        StudioSwitch(
                                            title = "Mesmerizing Refraction Glow",
                                            subtitle = "Iridescent chromatic sheen catching light along dock edge",
                                            checked = dockMesmerizingGlow,
                                            onCheckedChange = onSetDockMesmerizingGlow
                                        )
                                    }

                                    4 -> {
                                        // ── TAB 4: HEADER, INTERACTIONS & PRIVACY ──────────
                                        StudioHeader("Clock Formatting", "Choose 12-Hour AM/PM or 24-Hour Military format")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            StudioChoiceCard(
                                                title = "12-Hour (AM/PM)",
                                                subtitle = "Standard time",
                                                isSelected = !is24Hour,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSetIs24Hour(false) }
                                            )
                                            StudioChoiceCard(
                                                title = "24-Hour (Military)",
                                                subtitle = "24h format",
                                                isSelected = is24Hour,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSetIs24Hour(true) }
                                            )
                                        }

                                        StudioHeader("Live Weather & Telemetry", "Real-time conditions and device telemetry in header")
                                        StudioSwitch(
                                            title = "Show Live Weather",
                                            subtitle = "Display current temperature and weather forecast",
                                            checked = showWeather,
                                            onCheckedChange = onSetShowWeather
                                        )

                                        if (showWeather) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                StudioChoiceCard(
                                                    title = "Celsius (°C)",
                                                    subtitle = "Metric",
                                                    isSelected = weatherUnit.equals("CELSIUS", ignoreCase = true),
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { onSetWeatherUnit("CELSIUS") }
                                                )
                                                StudioChoiceCard(
                                                    title = "Fahrenheit (°F)",
                                                    subtitle = "Imperial",
                                                    isSelected = weatherUnit.equals("FAHRENHEIT", ignoreCase = true),
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { onSetWeatherUnit("FAHRENHEIT") }
                                                )
                                            }
                                        }

                                        StudioSwitch(
                                            title = "Show Duo Status Widget",
                                            subtitle = "Battery percentage & temperature pill in header",
                                            checked = showDuoStatusWidget,
                                            onCheckedChange = onSetShowDuoStatusWidget
                                        )

                                        StudioHeader("Interactions & Privacy", "Keyboard shortcuts and distraction-free options")
                                        StudioSwitch(
                                            title = "Auto-Open Keyboard in Drawer",
                                            subtitle = "Focus search bar immediately when opening All Apps drawer",
                                            checked = autoOpenKeyboardInDrawer,
                                            onCheckedChange = onSetAutoOpenKeyboardInDrawer
                                        )
                                        StudioSwitch(
                                            title = "Blur App List on Open",
                                            subtitle = "Obscure apps with frosted blur until tapped",
                                            checked = blurAppListOnOpen,
                                            onCheckedChange = onSetBlurAppListOnOpen
                                        )
                                        StudioSwitch(
                                            title = "Show Suggested Apps",
                                            subtitle = "Surface smart predictive apps based on your routine",
                                            checked = showDrawerSuggestions,
                                            onCheckedChange = onSetShowDrawerSuggestions
                                        )
                                        StudioSwitch(
                                            title = "Stealth Mode (OLED)",
                                            subtitle = "Pitch-black low-contrast mode for shoulder-proof discretion",
                                            checked = stealthTheme,
                                            onCheckedChange = onSetStealthTheme
                                        )
                                        if (stealthTheme) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf("LOW" to "Low", "MEDIUM" to "Medium", "HIGH" to "High").forEach { (lvl, lbl) ->
                                                    StudioChip(
                                                        label = "$lbl Contrast",
                                                        isSelected = stealthContrastLevel.equals(lvl, ignoreCase = true),
                                                        onClick = { onSetStealthContrastLevel(lvl) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    5 -> {
                                        // ── TAB 5: READY & DEFAULT LAUNCHER ────────────────
                                        StudioHeader("Configuration Overview", "Your launcher is live-configured to your personalized choices")
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    StudioPill(if (twoDrawersSideBySide) "2 Columns" else "1 Column")
                                                    StudioPill(if (categoryAlignment.equals("BOTTOM", ignoreCase = true)) "Drawers Down" else "Drawers Up")
                                                    StudioPill(if (is24Hour) "24h Clock" else "12h Clock")
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    StudioPill(themeMode)
                                                    StudioPill(surfaceThemeStyle)
                                                    StudioPill(cinematicPalette)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    StudioPill(adaptiveIconShape)
                                                    StudioPill(if (dockMesmerizingGlow) "Glow Active" else "Clean Dock")
                                                    StudioPill(if (showWeather) "Weather ($weatherUnit)" else "No Weather")
                                                }
                                            }
                                        }

                                        StudioHeader("Set as Default Launcher", "Make Drawer your default home app so swiping home returns here")
                                        OutlinedButton(
                                            onClick = { launchDefaultPrompt() },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Set as Default Home App", fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = onDismiss,
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Start Using Drawer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Navigation Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pagerState.currentPage > 0) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                                }
                            } else {
                                Spacer(modifier = Modifier.size(38.dp))
                            }

                            // Step indicator dots
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(STUDIO_TABS.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .height(5.dp)
                                            .width(if (isSelected) 16.dp else 5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }

                            val isLast = pagerState.currentPage == STUDIO_TABS.size - 1
                            Button(
                                onClick = {
                                    if (isLast) {
                                        if (!isDefaultLauncher(context)) {
                                            showDefaultDialog = true
                                        } else {
                                            onDismiss()
                                        }
                                    } else {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                if (isLast) {
                                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                } else {
                                    Text("Next", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

        if (showDefaultDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDefaultDialog = false
                    onDismiss()
                },
                icon = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) },
                title = { Text("Set as Default Launcher?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
                text = { Text("Make Drawer your default home app so your personalized drawers and dock are always one tap away.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(onClick = { showDefaultDialog = false; launchDefaultPrompt() }, shape = RoundedCornerShape(12.dp)) {
                        Text("Set as Default", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDefaultDialog = false; onDismiss() }) { Text("Maybe Later") }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun StudioHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StudioChoiceCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 9.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StudioSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun StudioSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = valueLabel, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().height(26.dp)
        )
    }
}

@Composable
private fun StudioChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StudioPill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun isDefaultLauncher(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        }
    }
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}
