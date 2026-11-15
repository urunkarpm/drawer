package com.urunkarpm.drawer.feature.settings.subscreen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.urunkarpm.drawer.core.designsystem.theme.GoogleSansFontFamily
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class FeatureCardData(
    val id: String,
    val title: String,
    val shortChipTitle: String,
    val badge: String,
    val icon: ImageVector,
    val description: String,
    val howToOpen: String?,
    val capabilities: List<String>,
    val targetSection: SettingsSection?,
    val targetKey: String?,
    val actionLabel: String?
)

private val FEATURE_CARDS = listOf(
    FeatureCardData(
        id = "APP_VAULT",
        title = "App Vault",
        shortChipTitle = "App Vault",
        badge = "Private Storage",
        icon = Icons.Default.Lock,
        description = "The App Vault hides sensitive apps. Hidden apps do not show in app drawers, categories, dock bar, or search results.",
        howToOpen = "Swipe horizontally across the dock bar on your home screen. Then verify with biometric unlock or your device PIN.",
        capabilities = listOf(
            "Private Alerts: Notifications from hidden apps show only inside the vault.",
            "Discreet Caret Glow: The notification icon glows softly when private alerts arrive.",
            "Batch Selection: Select multiple apps with check boxes to hide them together.",
            "Unhide Apps: Press and hold an app icon inside the vault to restore it."
        ),
        targetSection = SettingsSection.PRIVACY,
        targetKey = "APP_VAULT",
        actionLabel = "Open App Vault Settings"
    ),
    FeatureCardData(
        id = "TODO",
        title = "To-Do & Location Alerts",
        shortChipTitle = "To-Do Tasks",
        badge = "Offline Tasks",
        icon = Icons.Default.Checklist,
        description = "This offline task manager works without internet access, user accounts, or background data tracking.",
        howToOpen = "Swipe right from your home screen.",
        capabilities = listOf(
            "Location Alerts: Set location reminders using offline maps.",
            "Smart Filters: Quickly filter tasks by Due Today, Nearby, or Done.",
            "Priority Levels: Organize tasks by low, medium, or high urgency.",
            "Task Statistics: Inspect punctuality scores and completion metrics."
        ),
        targetSection = null,
        targetKey = null,
        actionLabel = null
    ),
    FeatureCardData(
        id = "NOTIFICATIONS",
        title = "Notification Hub & Muting",
        shortChipTitle = "Notification Hub",
        badge = "Distraction Defense",
        icon = Icons.Default.NotificationsOff,
        description = "The Notification Hub groups incoming alerts by application to reduce distractions.",
        howToOpen = "Swipe up from your home screen.",
        capabilities = listOf(
            "Home-Screen Quick Mute: Mute notifications for 1 hour, 24 hours, or permanently without opening system settings.",
            "Privacy Mode: Hides notification previews and verification codes on the home screen.",
            "Instant Dismiss: Swipe to dismiss notifications immediately."
        ),
        targetSection = SettingsSection.PRIVACY,
        targetKey = "NOTIFICATIONS",
        actionLabel = "Open Notification Settings"
    ),
    FeatureCardData(
        id = "DUO_STATUS",
        title = "DuoStatus & Speed Tools",
        shortChipTitle = "DuoStatus",
        badge = "Live Telemetry",
        icon = Icons.Default.Bolt,
        description = "The DuoStatus widget displays live hardware metrics and network tools directly on your home screen.",
        howToOpen = "Tap the status pill between clock and weather in the glance header.",
        capabilities = listOf(
            "Power Telemetry: Inspect real-time charging power in watts, voltage, current in mA, and battery temperature.",
            "Protocol Detection: Detects fast charging protocols such as USB-PD, Warp, Dart, and Turbo.",
            "Built-in Speed Test: Run download speed tests powered directly by Cloudflare and Google Edge CDN.",
            "Network Status: View active Wi-Fi name, signal strength, and link speed."
        ),
        targetSection = SettingsSection.GLANCE,
        targetKey = "DUO_STATUS",
        actionLabel = "Open DuoStatus Settings"
    ),
    FeatureCardData(
        id = "CAMERA_MIRROR",
        title = "Front Camera Mirror",
        shortChipTitle = "Camera Mirror",
        badge = "Quick Tool",
        icon = Icons.Default.Face,
        description = "Check your appearance before video meetings without opening the camera application.",
        howToOpen = "Tap the camera punch hole or mirror icon in the glance header on your home screen.",
        capabilities = listOf(
            "Instant Mirror Glance: View a video preview of your front camera directly on the home screen.",
            "Fast Startup: Skips camera startup delay, shutter sounds, and gallery loading.",
            "Punch-Hole Alignment: Integrates right at your phone's front camera cutout."
        ),
        targetSection = SettingsSection.GLANCE,
        targetKey = "CAMERA_MIRROR",
        actionLabel = "Open Mirror Settings"
    ),
    FeatureCardData(
        id = "HOME_DRAWERS",
        title = "Category Drawers",
        shortChipTitle = "App Drawers",
        badge = "Intentional Layout",
        icon = Icons.Default.Folder,
        description = "Organize applications into expandable categories to remove screen clutter.",
        howToOpen = "Tap any category header on the home screen.",
        capabilities = listOf(
            "Task Groups: Group tools into Work, Finance, or Media. Only one drawer expands at a time.",
            "Multi-Group Assignment: Place apps in multiple drawers without creating duplicate shortcuts.",
            "Instant Outside-Tap Collapse: Tap outside an open drawer to close it immediately.",
            "Frecency Predictions: Suggests relevant applications based on launch frequency and time.",
            "Alphabet Scrub Rail: Drag your thumb down the right screen edge to jump directly to any letter."
        ),
        targetSection = SettingsSection.HOME_DRAWERS,
        targetKey = "HOME_DRAWERS",
        actionLabel = "Open Drawer Settings"
    ),
    FeatureCardData(
        id = "WIDGETS",
        title = "Widget Drawers & 3D Stack",
        shortChipTitle = "Widgets",
        badge = "Collapsible Widgets",
        icon = Icons.Default.Widgets,
        description = "Store standard interactive Android widgets in collapsible drawers without losing clean home screen space.",
        howToOpen = "Tap a widget drawer header on the home screen or swipe to the widget page.",
        capabilities = listOf(
            "Collapsible Storage: Place widgets into drawers that close after use.",
            "3D Cover Flow: Swipe horizontally through widgets with depth animation.",
            "Smart Auto-Collapse: Automatically closes drawers after interaction to keep your screen clean."
        ),
        targetSection = SettingsSection.GLANCE,
        targetKey = "WIDGETS",
        actionLabel = "Open Widget Settings"
    ),
    FeatureCardData(
        id = "MONOCHROME",
        title = "Monochrome Mode",
        shortChipTitle = "Monochrome",
        badge = "Focus & Calm",
        icon = Icons.Default.Palette,
        description = "Removes bright brand colors that cause distraction and compulsive checks.",
        howToOpen = null,
        capabilities = listOf(
            "Monochrome Style: Converts application icons and launcher accents to a uniform monochrome palette.",
            "Calm Interface: Removes bright colors so your device stays calm.",
            "High Contrast: Provides clear text on both light and AMOLED black backgrounds."
        ),
        targetSection = SettingsSection.THEMES,
        targetKey = "MONOCHROME",
        actionLabel = "Open Theme Settings"
    )
)

@Composable
fun FeaturesSubScreen(
    initialPageIndex: Int,
    onNavigateToSetting: (SettingsSection, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, FEATURE_CARDS.size - 1),
        pageCount = { FEATURE_CARDS.size }
    )
    val chipListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val centerOffsetPx = with(density) { -(configuration.screenWidthDp / 3).dp.roundToPx() }

    // Ensure we scroll to the exact card when coming back from settings
    LaunchedEffect(initialPageIndex) {
        if (initialPageIndex in 0 until FEATURE_CARDS.size) {
            pagerState.scrollToPage(initialPageIndex)
        }
    }

    // Ensure the active highlighted chip moves into view as the user scrolls through the cards
    LaunchedEffect(pagerState.currentPage) {
        chipListState.animateScrollToItem(
            index = pagerState.currentPage,
            scrollOffset = centerOffsetPx
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Navigation Chips (Quick Jump) - Auto-scrolls as user scrolls through cards
        LazyRow(
            state = chipListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = FEATURE_CARDS,
                key = { _, card -> card.id }
            ) { index, card ->
                val isSelected = pagerState.currentPage == index
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    label = {
                        Text(
                            text = card.shortChipTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Feature Index Progress
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FEATURE ${pagerState.currentPage + 1} OF ${FEATURE_CARDS.size}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = FEATURE_CARDS[pagerState.currentPage].badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stacked Cards Pager
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val card = FEATURE_CARDS[page]

            // Stacked depth effect: scale and subtle alpha based on distance from center
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(0.93f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(0.60f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Scrollable content area inside card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header: Icon + Title + Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = card.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSansFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = card.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )

                        // Description (ASD-STE100)
                        Text(
                            text = card.description,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // How to Open / Access (If applicable)
                        if (card.howToOpen != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "How to Open:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = card.howToOpen,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Key Capabilities
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Capabilities:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            card.capabilities.forEach { capability ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = capability,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Direct Action Button (Anchor Link)
                    if (card.actionLabel != null && card.targetSection != null && card.targetKey != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onNavigateToSetting(card.targetSection, card.targetKey, page)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = card.actionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (card.id == "TODO") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gesture: Swipe right on Home screen to access",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Carousel Controls (Prev, Dots, Next)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Feature"
                )
            }

            // Animated dot indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(FEATURE_CARDS.size) { i ->
                    val isSelected = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        animationSpec = tween(250),
                        label = "dotWidth"
                    )
                    val color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                enabled = pagerState.currentPage < FEATURE_CARDS.size - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Feature"
                )
            }
        }
    }
}
