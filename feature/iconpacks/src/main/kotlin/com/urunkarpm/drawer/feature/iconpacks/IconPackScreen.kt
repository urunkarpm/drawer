package com.urunkarpm.drawer.feature.iconpacks

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.feature.iconpacks.component.IconPackPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackScreen(
    viewModel: IconPackViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    IconPackPickerSheet(
        installedPacks = uiState.installedPacks,
        activePackPackage = uiState.activePackPackage,
        adaptiveShape = uiState.adaptiveIconShape,
        sheetState = sheetState,
        onDismissRequest = onNavigateBack,
        onSelectPack = { pkg -> viewModel.selectIconPack(pkg) },
        onSelectShape = { shape -> viewModel.setAdaptiveIconShape(shape) },
        modifier = modifier
    )
}
