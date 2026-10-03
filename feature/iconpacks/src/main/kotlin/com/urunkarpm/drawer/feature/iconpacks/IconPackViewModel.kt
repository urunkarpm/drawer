package com.urunkarpm.drawer.feature.iconpacks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.drawer.core.data.repository.IconPackRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.IconOverride
import com.urunkarpm.drawer.core.model.IconPackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IconPackUiState(
    val installedPacks: List<IconPackInfo> = emptyList(),
    val activePackPackage: String? = null,
    val adaptiveIconShape: String = "SYSTEM",
    val overrides: List<IconOverride> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class IconPackViewModel @Inject constructor(
    private val iconPackRepository: IconPackRepository,
    private val preferencesDataSource: DrawerPreferencesDataSource
) : ViewModel() {

    val uiState: StateFlow<IconPackUiState> = combine(
        iconPackRepository.installedIconPacks,
        preferencesDataSource.activeIconPack,
        preferencesDataSource.adaptiveIconShape,
        iconPackRepository.overrides
    ) { packs, activePack, shape, overrides ->
        IconPackUiState(
            installedPacks = packs,
            activePackPackage = activePack,
            adaptiveIconShape = shape,
            overrides = overrides,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IconPackUiState()
    )

    fun selectIconPack(packageName: String?) {
        viewModelScope.launch {
            iconPackRepository.setActiveIconPack(packageName)
        }
    }

    fun setAdaptiveIconShape(shape: String) {
        viewModelScope.launch {
            preferencesDataSource.setAdaptiveIconShape(shape)
        }
    }

    fun setAppOverride(componentName: String, iconPackPackage: String, drawableName: String) {
        viewModelScope.launch {
            iconPackRepository.setAppOverride(componentName, iconPackPackage, drawableName)
        }
    }

    fun removeAppOverride(componentName: String) {
        viewModelScope.launch {
            iconPackRepository.removeAppOverride(componentName)
        }
    }
}
