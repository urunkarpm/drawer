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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.designsystem.component.DynamicPantoneWallpaper
import com.urunkarpm.drawer.core.designsystem.component.PantoneColors
import com.urunkarpm.drawer.core.designsystem.component.iconShapeFromString
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import kotlinx.coroutines.launch

private data class WalkthroughPageData(
    val badge: String,
    val title: String,
    val headline: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun WalkthroughOverlay(
    onDismiss: () -> Unit,
    twoDrawersSideBySide: Boolean = false,
    onSetTwoDrawersSideBySide: (Boolean) -> Unit = {},
    is24Hour: Boolean = true,
    onSetIs24Hour: (Boolean) -> Unit = {},
    showWeather: Boolean = true,
    onSetShowWeather: (Boolean) -> Unit = {},
    adaptiveIconShape: String = "CIRCLE",
    onSetAdaptiveIconShape: (String) -> Unit = {},
    drawerThemeMode: String = "SYSTEM",
    onSetDrawerThemeMode: (String) -> Unit = {},
    surfaceThemeStyle: String = "LIQUID_GLASS",
    onSetSurfaceThemeStyle: (String) -> Unit = {},
    dockMesmerizingGlow: Boolean = true,
    onSetDockMesmerizingGlow: (Boolean) -> Unit = {},
    headerFontFamily: String = "SYSTEM",
    onSetHeaderFontFamily: (String) -> Unit = {},
    stealthTheme: Boolean = false,
    onSetStealthTheme: (Boolean) -> Unit = {},
    isDynamicWallpaper: Boolean = false,
    onSelectDynamicWallpaper: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDefaultDialog by remember { mutableStateOf(false) }
    var isPeeking by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onDismiss()
    }

    fun launchDefaultPrompt() {
        // ponytail: RoleManager on Q+, fallback to ACTION_HOME_SETTINGS; ceiling: relies on system activity result; upgrade path: custom animated in-app visual guide.
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

    val pages = remember {
        listOf(
            WalkthroughPageData(
                badge = "WORKSPACE LAYOUT",
                title = "Drawer Architecture",
                headline = "Single column or dual side-by-side multitasking?",
                description = "Choose 1 unified drawer for classic focus, or 2 side-by-side drawers for separate work and personal context.",
                icon = Icons.Default.Folder,
                accentColor = Color(0xFF0F766E)
            ),
            WalkthroughPageData(
                badge = "EXECUTIVE GLANCE",
                title = "Clock & Weather Header",
                headline = "Tailor your home time format and live forecasts.",
                description = "Switch between 12-hour AM/PM and 24-hour military time, with optional real-time weather conditions.",
                icon = Icons.Default.Widgets,
                accentColor = Color(0xFF2563EB)
            ),
            WalkthroughPageData(
                badge = "ICON GEOMETRY",
                title = "App Icon Masking",
                headline = "Sculpt a uniform adaptive shape for all apps.",
                description = "Harmonize system and third-party app icons with modern geometric masking.",
                icon = Icons.Default.Security,
                accentColor = Color(0xFF7C3AED)
            ),
            WalkthroughPageData(
                badge = "THEME & SURFACES",
                title = "Theme & Glass Atmosphere",
                headline = "Light, Dark, or System — with Liquid Glass.",
                description = "Choose your baseline color temperature and tactile frosted glass or solid OLED finish.",
                icon = Icons.Default.Palette,
                accentColor = Color(0xFFD97706)
            ),
            WalkthroughPageData(
                badge = "PRECISION DOCK",
                title = "Dock & Mesmerizing Glow",
                headline = "Iridescent chromatic refraction or clean glass.",
                description = "Infuse the floating quick-launch dock with Peach Fuzz and Very Peri specular edge shimmer.",
                icon = Icons.Outlined.ColorLens,
                accentColor = Color(0xFFE11D48)
            ),
            WalkthroughPageData(
                badge = "TYPOGRAPHY",
                title = "Header Typographic Voice",
                headline = "Select your signature headline font family.",
                description = "Set the tone of your launcher with bespoke typography, from modern sans to executive display fonts.",
                icon = Icons.Default.Language,
                accentColor = Color(0xFF0D9488)
            ),
            WalkthroughPageData(
                badge = "VISUAL DISCRETION",
                title = "Stealth Theme & Privacy",
                headline = "OLED pitch black with muted contrast.",
                description = "Boardroom-ready discretion: eliminate visual distractions and preserve maximum battery life.",
                icon = Icons.Default.VisibilityOff,
                accentColor = Color(0xFF475569)
            ),
            WalkthroughPageData(
                badge = "AESTHETIC AMBIANCE",
                title = "Pantone Gradient Wallpaper",
                headline = "Smooth 3s morphing gradient or original wallpaper.",
                description = "Enjoy smooth 3-second cycle transitions with authentic Pantone colors and 40% blur, or keep your wallpaper.",
                icon = Icons.Outlined.ColorLens,
                accentColor = Color(0xFFBE3455)
            ),
            WalkthroughPageData(
                badge = "SETUP COMPLETE",
                title = "Your Launcher is Ready!",
                headline = "Live-configured precisely to your taste.",
                description = "Every setting you chose is already active. Launch Drawer now or make it your default home app.",
                icon = Icons.Default.Home,
                accentColor = Color(0xFF059669)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPeeking) 0.08f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "cardAlpha"
    )

    // Full-screen backdrop with see-through peek capability
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isPeeking) 0.05f else 0.58f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(enabled = isPeeking) { isPeeking = false },
        contentAlignment = Alignment.Center
    ) {
        // Floating prompt shown while peeking
        if (isPeeking) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .clickable { isPeeking = false }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Viewing live launcher • Tap anywhere to return",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Main Walkthrough Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 500.dp)
                .alpha(cardAlpha)
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top Header Row with Badge, Peek, & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentPageData = pages[pagerState.currentPage]
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = currentPageData.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, currentPageData.accentColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = currentPageData.badge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = currentPageData.accentColor,
                            letterSpacing = 1.1.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Peek at Live Launcher Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { isPeeking = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Peek at launcher",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Peek",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Skip", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Skip walkthrough",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = page.headline,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = page.accentColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Real-Time Live Preview & Configuration Area
                        WalkthroughLiveStep(
                            pageIndex = pageIndex,
                            accentColor = page.accentColor,
                            twoDrawersSideBySide = twoDrawersSideBySide,
                            onSetTwoDrawersSideBySide = onSetTwoDrawersSideBySide,
                            is24Hour = is24Hour,
                            onSetIs24Hour = onSetIs24Hour,
                            showWeather = showWeather,
                            onSetShowWeather = onSetShowWeather,
                            adaptiveIconShape = adaptiveIconShape,
                            onSetAdaptiveIconShape = onSetAdaptiveIconShape,
                            drawerThemeMode = drawerThemeMode,
                            onSetDrawerThemeMode = onSetDrawerThemeMode,
                            surfaceThemeStyle = surfaceThemeStyle,
                            onSetSurfaceThemeStyle = onSetSurfaceThemeStyle,
                            dockMesmerizingGlow = dockMesmerizingGlow,
                            onSetDockMesmerizingGlow = onSetDockMesmerizingGlow,
                            headerFontFamily = headerFontFamily,
                            onSetHeaderFontFamily = onSetHeaderFontFamily,
                            stealthTheme = stealthTheme,
                            onSetStealthTheme = onSetStealthTheme,
                            isDynamicWallpaper = isDynamicWallpaper,
                            onSelectDynamicWallpaper = onSelectDynamicWallpaper
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pager Indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 22.dp else 6.dp,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "indicatorWidth"
                            )
                            val color by animateColorAsState(
                                targetValue = if (isSelected) {
                                    pages[pagerState.currentPage].accentColor
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                label = "indicatorColor"
                            )

                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (pagerState.currentPage > 0) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous slide"
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == pages.size - 1

                        Button(
                            onClick = {
                                if (isLastPage) {
                                    if (!isDefaultLauncher(context)) {
                                        showDefaultDialog = true
                                    } else {
                                        onDismiss()
                                    }
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pages[pagerState.currentPage].accentColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            if (isLastPage) {
                                Text(
                                    text = "Start Launcher",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "Next",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
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
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Set as Default Launcher?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Make Drawer your default home app so your personalized drawers, executive dock, and live widgets are always one tap away.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDefaultDialog = false
                            launchDefaultPrompt()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set as Default", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDefaultDialog = false
                            onDismiss()
                        }
                    ) {
                        Text("Maybe Later")
                    }
                },
                shape = RoundedCornerShape(22.dp)
            )
        }
    }
}

/**
 * Renders the live interactive preview and configuration options for each walkthrough step.
 */
@Composable
private fun WalkthroughLiveStep(
    pageIndex: Int,
    accentColor: Color,
    twoDrawersSideBySide: Boolean,
    onSetTwoDrawersSideBySide: (Boolean) -> Unit,
    is24Hour: Boolean,
    onSetIs24Hour: (Boolean) -> Unit,
    showWeather: Boolean,
    onSetShowWeather: (Boolean) -> Unit,
    adaptiveIconShape: String,
    onSetAdaptiveIconShape: (String) -> Unit,
    drawerThemeMode: String,
    onSetDrawerThemeMode: (String) -> Unit,
    surfaceThemeStyle: String,
    onSetSurfaceThemeStyle: (String) -> Unit,
    dockMesmerizingGlow: Boolean,
    onSetDockMesmerizingGlow: (Boolean) -> Unit,
    headerFontFamily: String,
    onSetHeaderFontFamily: (String) -> Unit,
    stealthTheme: Boolean,
    onSetStealthTheme: (Boolean) -> Unit,
    isDynamicWallpaper: Boolean,
    onSelectDynamicWallpaper: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (pageIndex) {
            0 -> {
                // ── STEP 0: 1 vs 2 DRAWERS ──────────────────────────────────────────────
                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().height(96.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                        if (!twoDrawersSideBySide) {
                            // 1 Unified Column mockup
                            Column(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Primary Apps (1 Drawer)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accentColor)
                                    Text("12 apps", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    repeat(4) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = accentColor.copy(alpha = 0.15f),
                                            modifier = Modifier.size(28.dp)
                                        ) {}
                                    }
                                }
                            }
                        } else {
                            // 2 Side-by-Side Drawers mockup
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Drawer 1", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = accentColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(2) { Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.2f), modifier = Modifier.size(24.dp)) {} }
                                    }
                                }
                                Box(modifier = Modifier.width(1.dp).height(50.dp).background(accentColor.copy(alpha = 0.4f)))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Drawer 2", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF2563EB))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(2) { Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF2563EB).copy(alpha = 0.2f), modifier = Modifier.size(24.dp)) {} }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = "1 Unified Drawer",
                        subtitle = "Classic focus",
                        isSelected = !twoDrawersSideBySide,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSetTwoDrawersSideBySide(false) }
                    )
                    OptionCard(
                        title = "2 Dual Drawers",
                        subtitle = "Side-by-side split",
                        isSelected = twoDrawersSideBySide,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSetTwoDrawersSideBySide(true) }
                    )
                }
            }

            1 -> {
                // ── STEP 1: AT A GLANCE (24h/12h & Weather) ─────────────────────────────
                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (is24Hour) "15:45" else "03:45 PM",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Thursday, Oct 18",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (showWeather) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("24°C", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sunny", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Interactive controls
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Clock Format", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipOption(title = "12-Hour AM/PM", isSelected = !is24Hour, accentColor = accentColor) { onSetIs24Hour(false) }
                        ChipOption(title = "24-Hour Military", isSelected = is24Hour, accentColor = accentColor) { onSetIs24Hour(true) }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Weather Feed", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipOption(title = "Show Weather", isSelected = showWeather, accentColor = accentColor) { onSetShowWeather(true) }
                        ChipOption(title = "Hide Weather", isSelected = !showWeather, accentColor = accentColor) { onSetShowWeather(false) }
                    }
                }
            }

            2 -> {
                // ── STEP 2: ICON SHAPE GEOMETRY ─────────────────────────────────────────
                val shape = remember(adaptiveIconShape) { iconShapeFromString(adaptiveIconShape) }

                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icons = listOf(Icons.Default.Call, Icons.AutoMirrored.Filled.Chat, Icons.Default.Language, Icons.Default.PhotoCamera)
                        val colors = listOf(Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFFF97316), Color(0xFFA855F7))
                        icons.forEachIndexed { i, ic ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(shape)
                                    .background(colors[i]),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = ic, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                // Interactive chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shapes = listOf(
                        "SQUIRCLE" to "Squircle",
                        "CIRCLE" to "Circle",
                        "ROUNDED_SQUARE" to "Square",
                        "TEARDROP" to "Teardrop",
                        "SYSTEM" to "System"
                    )
                    shapes.forEach { (key, label) ->
                        ChipOption(
                            title = label,
                            isSelected = adaptiveIconShape.equals(key, ignoreCase = true),
                            accentColor = accentColor
                        ) { onSetAdaptiveIconShape(key) }
                    }
                }
            }

            3 -> {
                // ── STEP 3: THEME & GLASS SURFACES ──────────────────────────────────────
                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when (drawerThemeMode.uppercase()) {
                        "DARK" -> Color(0xFF121212)
                        "LIGHT" -> Color(0xFFF4F4F5)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        if (surfaceThemeStyle == "LIQUID_GLASS") 1.5.dp else 1.dp,
                        accentColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mode: $drawerThemeMode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Surface: $surfaceThemeStyle",
                                fontSize = 11.sp,
                                color = accentColor
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier.size(height = 34.dp, width = 74.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Preview", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                    }
                }

                // Interactive controls
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Theme Palette", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("SYSTEM" to "System", "DARK" to "Dark", "LIGHT" to "Light").forEach { (mode, label) ->
                            ChipOption(title = label, isSelected = drawerThemeMode.equals(mode, ignoreCase = true), accentColor = accentColor) {
                                onSetDrawerThemeMode(mode)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Surface Texture", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LIQUID_GLASS" to "Liquid Glass", "BLUR" to "Frosted Blur", "SOLID" to "Solid OLED").forEach { (style, label) ->
                            ChipOption(title = label, isSelected = surfaceThemeStyle.equals(style, ignoreCase = true), accentColor = accentColor) {
                                onSetSurfaceThemeStyle(style)
                            }
                        }
                    }
                }
            }

            4 -> {
                // ── STEP 4: PRECISION DOCK & MESMERIZING GLOW ────────────────────────────
                // Live visual mockup
                val glowBorder = if (dockMesmerizingGlow) {
                    Brush.horizontalGradient(
                        listOf(PantoneColors.PeachFuzz, PantoneColors.VeryPeri, PantoneColors.VivaMagenta)
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(if (dockMesmerizingGlow) 2.dp else 1.dp, glowBorder),
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (dockMesmerizingGlow) "✨ MESMERIZING GLOW ACTIVE" else "CLEAN GLASS DOCK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (dockMesmerizingGlow) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icons = listOf(Icons.Default.Call, Icons.AutoMirrored.Filled.Chat, Icons.Default.Language, Icons.Default.PhotoCamera)
                            icons.forEach { ic ->
                                Surface(
                                    shape = CircleShape,
                                    color = if (dockMesmerizingGlow) accentColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    border = BorderStroke(1.dp, if (dockMesmerizingGlow) accentColor.copy(alpha = 0.5f) else Color.Transparent),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = ic, contentDescription = null, tint = if (dockMesmerizingGlow) accentColor else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = "Mesmerizing Glow",
                        subtitle = "Iridescent refraction",
                        isSelected = dockMesmerizingGlow,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSetDockMesmerizingGlow(true) }
                    )
                    OptionCard(
                        title = "Clean Glass",
                        subtitle = "Subtle frosted dock",
                        isSelected = !dockMesmerizingGlow,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSetDockMesmerizingGlow(false) }
                    )
                }
            }

            5 -> {
                // ── STEP 5: TYPOGRAPHY ──────────────────────────────────────────────────
                val resolvedFont = remember(headerFontFamily) { headerFontFamilyFromName(headerFontFamily) }

                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "12:30 • EXECUTIVE",
                            fontFamily = resolvedFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Bespoke Workspace Typography",
                            fontFamily = resolvedFont,
                            fontSize = 11.sp,
                            color = accentColor
                        )
                    }
                }

                // Interactive chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fonts = listOf(
                        "GOOGLE_SANS" to "Google Sans",
                        "SPOOKYY" to "Spookyy",
                        "DS_DIGITAL" to "DS-Digital",
                        "EVIL_EMPIRE" to "Evil Empire",
                        "HYPIK" to "Hypik",
                        "HELVETICA" to "Helvetica",
                        "ROBOTO" to "Roboto",
                        "TIMES_NEW_ROMAN" to "Times",
                        "ARIAL" to "Arial",
                        "TEXAS_TROUPE" to "Texas Troupe",
                        "SYSTEM" to "System Sans",
                        "MONOSPACE" to "Monospace",
                        "SERIF" to "Serif",
                        "CURSIVE" to "Cursive"
                    )
                    fonts.forEach { (key, label) ->
                        ChipOption(
                            title = label,
                            isSelected = headerFontFamily.equals(key, ignoreCase = true),
                            accentColor = accentColor
                        ) { onSetHeaderFontFamily(key) }
                    }
                }
            }

            6 -> {
                // ── STEP 6: STEALTH THEME & PRIVACY ─────────────────────────────────────
                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (stealthTheme) Color(0xFF09090B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, if (stealthTheme) Color(0xFF27272A) else accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (stealthTheme) Color(0xFF18181B) else accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (stealthTheme) Icons.Default.VisibilityOff else Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (stealthTheme) Color(0xFF71717A) else accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (stealthTheme) "STEALTH OLED 0%" else "EXECUTIVE NOTIFICATIONS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (stealthTheme) Color(0xFF71717A) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (stealthTheme) "Low contrast • Shoulder proof" else "Vibrant previews & accents",
                                    fontSize = 10.sp,
                                    color = if (stealthTheme) Color(0xFF52525B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Interactive selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = "Stealth Mode",
                        subtitle = "OLED pitch black",
                        isSelected = stealthTheme,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSetStealthTheme(true) }
                    )
                    OptionCard(
                        title = "Standard Mode",
                        subtitle = "Full dynamic contrast",
                        isSelected = !stealthTheme,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSetStealthTheme(false) }
                    )
                }
            }

            7 -> {
                // ── STEP 7: PANTONE GRADIENT WALLPAPER ──────────────────────────────────
                // Live visual mockup
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(92.dp)
                ) {
                    if (isDynamicWallpaper) {
                        DynamicPantoneWallpaper(speedSeconds = 3, blurPercent = 40)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Original System Wallpaper Active", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Interactive selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionCard(
                        title = "Pantone Gradient",
                        subtitle = "3s cycle • 40% blur",
                        isSelected = isDynamicWallpaper,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectDynamicWallpaper(true) }
                    )
                    OptionCard(
                        title = "Original Wallpaper",
                        subtitle = "System wallpaper",
                        isSelected = !isDynamicWallpaper,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectDynamicWallpaper(false) }
                    )
                }
            }

            8 -> {
                // ── STEP 8: FINAL SUMMARY ────────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ACTIVE CONFIGURATION SUMMARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 0.8.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SummaryPill(if (twoDrawersSideBySide) "2 Drawers" else "1 Drawer")
                            SummaryPill(if (is24Hour) "24h" else "12h")
                            SummaryPill(adaptiveIconShape)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SummaryPill(drawerThemeMode)
                            SummaryPill(surfaceThemeStyle)
                            SummaryPill(if (dockMesmerizingGlow) "Mesmerizing Glow" else "Clean Dock")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SummaryPill(if (isDynamicWallpaper) "Pantone Gradient" else "System Wallpaper")
                            SummaryPill(if (stealthTheme) "Stealth On" else "Standard")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(text: String) {
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

@Composable
private fun OptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .height(68.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChipOption(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun isDefaultLauncher(context: Context): Boolean {
    // ponytail: standard platform check via RoleManager on Q+ and PackageManager fallback; ceiling: relies on MATCH_DEFAULT_ONLY filter; upgrade path: BroadcastReceiver on ACTION_PREFERRED_ACTIVITY_CHANGED.
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
