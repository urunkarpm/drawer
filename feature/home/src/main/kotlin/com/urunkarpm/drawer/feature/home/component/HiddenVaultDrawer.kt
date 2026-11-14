package com.urunkarpm.drawer.feature.home.component

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.designsystem.theme.rememberDeviceScreenCurve
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenVaultDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    hiddenApps: List<AppInfo>,
    allInstalledApps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onUnhideApp: (String) -> Unit,
    onAddHiddenApp: (String) -> Unit,
    surfaceCornerRadiusDp: Float = 24f,
    matchDeviceScreenCurve: Boolean = true,
    deviceCurveOffsetDp: Float = 0f,
    iconLoader: (suspend (AppInfo) -> Drawable?)? = null,
    onOpenAppDetails: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isOpen, onBack = onClose)

    var isDismissAllowed by remember(isOpen) { mutableStateOf(false) }
    LaunchedEffect(isOpen) {
        if (isOpen) {
            kotlinx.coroutines.delay(250)
            isDismissAllowed = true
        } else {
            isDismissAllowed = false
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    var showAddAppsSheet by remember { mutableStateOf(false) }

    val detectedScreenCurve = rememberDeviceScreenCurve(fallbackDp = surfaceCornerRadiusDp)
    val drawerShape = remember(surfaceCornerRadiusDp, matchDeviceScreenCurve, deviceCurveOffsetDp, detectedScreenCurve) {
        if (matchDeviceScreenCurve) {
            detectedScreenCurve.toRoundedCornerShape(offsetDp = deviceCurveOffsetDp, minDp = 12f)
        } else {
            RoundedCornerShape(surfaceCornerRadiusDp.dp)
        }
    }

    // Frosted glass atmospheric styling
    val frostedGlassBackground = remember(isSystemDark) {
        Brush.verticalGradient(
            colors = if (isSystemDark) {
                listOf(
                    Color(0xF012111A),
                    Color(0xE81A1826),
                    Color(0xF2161422)
                )
            } else {
                listOf(
                    Color(0xF5F0F0F8),
                    Color(0xEEFFFFFF),
                    Color(0xF5E8E8F5)
                )
            }
        )
    }

    val frostedBorderStroke = remember(isSystemDark) {
        BorderStroke(
            width = 1.5.dp,
            brush = Brush.verticalGradient(
                colors = if (isSystemDark) {
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.10f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.90f),
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.30f)
                    )
                }
            )
        )
    }

    val insets = WindowInsets.statusBars.union(WindowInsets.displayCutout).union(WindowInsets.navigationBars)
    val contentPadding = insets.asPaddingValues()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isDismissAllowed) {
                            onClose()
                        }
                    }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = drawerShape,
                border = frostedBorderStroke,
                color = Color.Transparent,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks to prevent dismiss on card click */ }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(frostedGlassBackground)
                        .padding(contentPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        // ── Header ─────────────────────────────────────────────
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Private Vault",
                                        fontFamily = LocalHeaderFontFamily.current,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${hiddenApps.size} hidden ${if (hiddenApps.size == 1) "app" else "apps"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { showAddAppsSheet = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", style = MaterialTheme.typography.labelMedium)
                                }

                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Content: App Grid or Empty State ───────────────────
                        if (hiddenApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Your Vault is Empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Apps added to this vault are hidden from all app lists, drawers, dock, and searches. Notifications from them remain private.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showAddAppsSheet = true },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Hide Apps Now")
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(hiddenApps, key = { it.packageName }) { app ->
                                    VaultAppItem(
                                        app = app,
                                        iconLoader = iconLoader,
                                        onClick = { onLaunchApp(app) },
                                        onUnhide = { onUnhideApp(app.packageName) },
                                        onOpenAppDetails = { onOpenAppDetails(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add Apps Sheet ─────────────────────────────────────────────────────────
    if (showAddAppsSheet) {
        val nonHiddenApps = remember(allInstalledApps, hiddenApps) {
            val hiddenPkgSet = hiddenApps.map { it.packageName }.toSet()
            allInstalledApps.filter { it.packageName !in hiddenPkgSet }.sortedBy { it.label.lowercase() }
        }
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = remember(searchQuery, nonHiddenApps) {
            if (searchQuery.isBlank()) nonHiddenApps
            else nonHiddenApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAddAppsSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Add Apps to Vault",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select an app to hide it completely from launcher drawers and notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredList, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAddHiddenApp(app.packageName)
                                    showAddAppsSheet = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(
                                key = app.componentKey,
                                size = 38.dp,
                                label = app.label,
                                isWorkProfile = app.isWorkProfile,
                                iconLoader = if (iconLoader != null) { { iconLoader(app) } } else null
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Hide App",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultAppItem(
    app: AppInfo,
    iconLoader: (suspend (AppInfo) -> Drawable?)?,
    onClick: () -> Unit,
    onUnhide: () -> Unit,
    onOpenAppDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .padding(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIconImage(
                key = app.componentKey,
                size = 52.dp,
                label = app.label,
                isWorkProfile = app.isWorkProfile,
                iconLoader = if (iconLoader != null) { { iconLoader(app) } } else null
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Unhide from Vault") },
                    leadingIcon = { Icon(Icons.Outlined.Visibility, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onUnhide()
                    }
                )
                DropdownMenuItem(
                    text = { Text("App Info") },
                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onOpenAppDetails()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
