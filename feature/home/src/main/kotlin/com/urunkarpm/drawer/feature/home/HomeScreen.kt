package com.urunkarpm.drawer.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.feature.dock.DockBar
import com.urunkarpm.drawer.feature.groups.GroupsAccordion
import com.urunkarpm.drawer.feature.groups.GroupsViewModel
import com.urunkarpm.drawer.feature.groups.component.CategorySelectionBottomSheet
import com.urunkarpm.drawer.feature.home.component.AllAppsDrawer
import com.urunkarpm.drawer.feature.home.component.AppActionBottomSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    groupsViewModel: GroupsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupsUiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var appForCategorySelection by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    // Handle back button for launcher:
    // If a menu, all apps, or category selection is open, back closes it.
    BackHandler(enabled = uiState.isAllAppsOpen || uiState.selectedAppForMenu != null || appForCategorySelection != null) {
        if (appForCategorySelection != null) {
            appForCategorySelection = null
        } else if (uiState.selectedAppForMenu != null) {
            viewModel.dismissAppMenu()
        } else if (uiState.isAllAppsOpen) {
            viewModel.closeAllApps()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState.hideStatusBar) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (uiState.hideStatusBar) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Home View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (uiState.hideStatusBar) Modifier else Modifier.statusBarsPadding())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (delta < -20f && !uiState.isAllAppsOpen) {
                            viewModel.openAllApps()
                        }
                    },
                    orientation = Orientation.Vertical
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Glance (Clock, Date & Weather)
            com.urunkarpm.drawer.feature.home.component.GlanceHeader(
                is24Hour = uiState.is24Hour,
                showWeather = uiState.showWeather,
                weatherUnit = uiState.weatherUnit,
                weatherInfo = uiState.weatherInfo,
                onRefreshWeather = { viewModel.refreshWeather() }
            )

            // Middle Section: App Groups / Categories
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                GroupsAccordion(
                    viewModel = groupsViewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom Section: All Apps Trigger & Dock placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dock Bar (0-5 apps)
                DockBar()

                Spacer(modifier = Modifier.height(8.dp))

                // All Apps handle / search bar pill
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { viewModel.openAllApps() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "  Search apps (${uiState.installedApps.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Swipe up for apps",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // All Apps Drawer Overlay (Slide up)
        AllAppsDrawer(
            visible = uiState.isAllAppsOpen,
            apps = uiState.filteredApps,
            searchQuery = uiState.searchQuery,
            isLoading = uiState.isLoading,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onAppClick = { app -> viewModel.onAppClicked(app) },
            onAppLongClick = { app -> viewModel.onAppLongClicked(app) },
            onClose = { viewModel.closeAllApps() },
            iconLoader = { app -> viewModel.getAppIcon(app) }
        )

        // App Long-Press Action Sheet
        val selectedApp = uiState.selectedAppForMenu
        if (selectedApp != null) {
            AppActionBottomSheet(
                app = selectedApp,
                onDismissRequest = { viewModel.dismissAppMenu() },
                sheetState = sheetState,
                onLaunch = { viewModel.onAppClicked(selectedApp) },
                onOpenDetails = { viewModel.openAppDetails(selectedApp) },
                onUninstall = { viewModel.uninstallApp(selectedApp) },
                onPinToDock = {
                    viewModel.pinToDock(selectedApp)
                },
                onAddToGroup = {
                    appForCategorySelection = selectedApp
                    viewModel.dismissAppMenu()
                },
                iconLoader = { viewModel.getAppIcon(selectedApp) }
            )
        }

        val targetApp = appForCategorySelection
        if (targetApp != null) {
            CategorySelectionBottomSheet(
                app = targetApp,
                groups = groupsUiState.groups.map { it.group },
                sheetState = categorySheetState,
                onDismissRequest = { appForCategorySelection = null },
                onSelectGroup = { group ->
                    groupsViewModel.assignAppToGroup(group.id, targetApp)
                    appForCategorySelection = null
                },
                onCreateNewGroup = {
                    appForCategorySelection = null
                    groupsViewModel.setShowCreateDialog(true)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }
}
