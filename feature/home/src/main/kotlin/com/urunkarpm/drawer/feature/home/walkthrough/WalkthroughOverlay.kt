package com.urunkarpm.drawer.feature.home.walkthrough

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class WalkthroughPageData(
    val badge: String,
    val title: String,
    val headline: String,
    val description: String,
    val timeSavedTitle: String,
    val timeSavedDesc: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun WalkthroughOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDefaultDialog by remember { mutableStateOf(false) }

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

    // ponytail: Dynamic page-count state linked directly to page list; ceiling: Pager only handles sequential slide steps; upgrade path: Deep-linking directly to specific walkthrough slide from Settings if desired.
    val pages = remember {
        listOf(
            WalkthroughPageData(
                badge = "EXECUTIVE SUITE",
                title = "Engineered for Focus",
                headline = "Zero distractions. Pure executive velocity.",
                description = "Designed for professionals who command their tools with precision. Drawer eliminates algorithm feeds, noisy promotions, and attention sinks in favor of a disciplined, high-performance workspace.",
                timeSavedTitle = "Reclaim Cognitive Bandwidth",
                timeSavedDesc = "Clean architecture, zero unsolicited feeds, and instantaneous sub-second app execution.",
                icon = Icons.Default.Security,
                accentColor = Color(0xFF1E3A8A) // Executive Navy
            ),
            WalkthroughPageData(
                badge = "WORKSPACE ARCHITECTURE",
                title = "Contextual Drawers",
                headline = "Structure your enterprise and personal apps effortlessly.",
                description = "Organize business tools, communications, finance, and productivity into discrete, collapsible workspace drawers. One tap expands; one tap collapses.",
                timeSavedTitle = "Unified Enterprise Hierarchy",
                timeSavedDesc = "Multi-drawer assignment with dedicated work-profile segregation and immediate context switching.",
                icon = Icons.Default.Folder,
                accentColor = Color(0xFF0F766E) // Deep Corporate Teal
            ),
            WalkthroughPageData(
                badge = "WIDGET ARCHITECTURE",
                title = "iPod CoverFlow Widgets",
                headline = "Stack and glide through live interactive data cards.",
                description = "Swipe left into your dedicated live widget feed. Group companion widgets into CoverFlow drawers with realistic 3D depth and inertia, or deploy standalone multi-span glance tiles.",
                timeSavedTitle = "Fluid Real-Time Awareness",
                timeSavedDesc = "Check calendars, market ticks, and system stats with continuous CoverFlow paging and zero home clutter.",
                icon = Icons.Default.Widgets,
                accentColor = Color(0xFF6D28D9) // Royal Violet
            ),
            WalkthroughPageData(
                badge = "INSTANT RETRIEVAL",
                title = "Executive Dock & Command Bar",
                headline = "Launch critical tools in under a second.",
                description = "Keep mission-critical productivity apps on the precision floating dock. Slide down the responsive alphabet index or invoke the ergonomic command bar for instant keyboard filtering.",
                timeSavedTitle = "Precision Muscle Memory",
                timeSavedDesc = "Ergonomic single-hand reach with zero animation delay and full keyboard-auto-open acceleration.",
                icon = Icons.Default.Search,
                accentColor = Color(0xFF065F46) // Financial Forest Green
            ),
            WalkthroughPageData(
                badge = "VISUAL DISCRETION",
                title = "Stealth Theme & Surfaces",
                headline = "Designed for boardroom discretion and battery longevity.",
                description = "Activate Stealth Mode for ultra-low-contrast typography, true OLED pitch-black backgrounds, and muted glyphs that shield your screen from wandering eyes in tight executive spaces.",
                timeSavedTitle = "Shoulder-Surfer Protection",
                timeSavedDesc = "Minimal visual footprint preserves OLED battery life and protects confidential workflows from side angles.",
                icon = Icons.Default.VisibilityOff,
                accentColor = Color(0xFF475569) // Stealth Slate
            ),
            WalkthroughPageData(
                badge = "CONFIDENTIALITY & CONTROL",
                title = "Notification Hub & Privacy",
                headline = "Take total ownership of your alerts.",
                description = "Consolidate active notifications cleanly by application. Apply confidentiality masking to keep sensitive client messages private during meetings, or quick-mute noisy threads on demand.",
                timeSavedTitle = "Meeting & Presentation Ready",
                timeSavedDesc = "Instant privacy masking prevents previews from exposing private discussions in professional environments.",
                icon = Icons.Default.NotificationsOff,
                accentColor = Color(0xFF334155) // Slate Charcoal
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Full-screen scrim backdrop
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 500.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
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
                    .padding(20.dp)
            ) {
                // Top Header Row with Badge & Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentPageData = pages[pagerState.currentPage]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = currentPageData.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, currentPageData.accentColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = currentPageData.badge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = currentPageData.accentColor,
                            letterSpacing = 1.2.sp
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Skip", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Skip walkthrough",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = page.headline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = page.accentColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual decorative banner
                        WalkthroughVisualCard(
                            pageIndex = pageIndex,
                            accentColor = page.accentColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Time saved callout box
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = page.accentColor.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = page.accentColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, page.accentColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = page.icon,
                                            contentDescription = null,
                                            tint = page.accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = page.timeSavedTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = page.timeSavedDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Row: Indicator Dots & Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pager Indicators
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(pages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 24.dp else 8.dp,
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
                                    .height(8.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pagerState.currentPage > 0) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                modifier = Modifier.size(40.dp)
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
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (isLastPage) {
                                Text(
                                    text = "Get Started",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "Next",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Make Drawer your default home app so your workspace drawers, executive dock, and live widget feed are always one tap away.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "QUICK GUIDE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When the system prompt appears, select Drawer and tap \"Always\" (or choose Drawer under Default Home App in Settings).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
private fun WalkthroughVisualCard(
    pageIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    accentColor.copy(alpha = 0.4f),
                    accentColor.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            when (pageIndex) {
                0 -> {
                    // Executive Suite preview: Minimalist metallic badge with crisp typography
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DRAWER WORKSPACE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.4.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zero Trackers • Enterprise Focus • Pure Velocity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                1 -> {
                    // Workspace Architecture preview: Discrete contextual drawers
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryPillMockup(
                            title = "Enterprise & Finance",
                            count = "4 apps",
                            icon = Icons.Default.Folder,
                            accentColor = accentColor,
                            isExpanded = true
                        )
                        CategoryPillMockup(
                            title = "Operations & Comms",
                            count = "6 apps",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            accentColor = accentColor,
                            isExpanded = false
                        )
                    }
                }
                2 -> {
                    // ponytail: Static visual representation of 3D CoverFlow deck; ceiling: Doesn't execute real gesture-driven carousel in walkthrough preview; upgrade path: Interactive mini-pager preview if users request live test drive.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((-12).dp)
                        ) {
                            // Left tucked card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.size(width = 64.dp, height = 52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Calendar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Center active CoverFlow card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(width = 112.dp, height = 68.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "LIVE FEED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            letterSpacing = 0.8.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = "Market Ticker",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "+2.4% • 4,892.1",
                                        fontSize = 9.sp,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Right tucked card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.size(width = 64.dp, height = 52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Battery",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Carousel indicator dots
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
                3 -> {
                    // Precision Dock & Search preview
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Search bar mockup
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Search workspace apps...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Precision dock slots with clean vector icons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dockIcons = listOf(
                                Icons.Default.Call,
                                Icons.AutoMirrored.Filled.Chat,
                                Icons.Default.Language,
                                Icons.Default.PhotoCamera
                            )
                            dockIcons.forEachIndexed { slotIndex, icon ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (slotIndex == 0) accentColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (slotIndex == 0) accentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (slotIndex == 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // ponytail: Simplified static mockup of low-contrast stealth surface; ceiling: Uses hardcoded dark palette rather than reactive dynamic theme engine in preview; upgrade path: Dynamically render user's chosen contrast tier if interactive theme preview needed.
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF09090B),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(76.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF18181B),
                                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color(0xFF71717A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "STEALTH SURFACE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.1.sp,
                                        color = Color(0xFF71717A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Subdued contrast • Side-shielded",
                                        fontSize = 11.sp,
                                        color = Color(0xFF52525B)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF18181B),
                                border = BorderStroke(1.dp, Color(0xFF27272A))
                            ) {
                                Text(
                                    text = "OLED 0%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF71717A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                5 -> {
                    // Confidentiality & Notification control preview
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Client Executive Brief",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            "Privacy mask enabled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accentColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "Mute 1h",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPillMockup(
    title: String,
    count: String,
    icon: ImageVector,
    accentColor: Color,
    isExpanded: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isExpanded) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(
            1.dp,
            if (isExpanded) accentColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isExpanded) accentColor else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = count.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
        }
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
