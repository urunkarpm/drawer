package com.urunkarpm.drawer.core.needle.model

sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data class Downloading(val progress: Float) : ModelStatus
    data class Ready(val modelPath: String, val sizeMb: Float) : ModelStatus
    data class Error(val cause: String) : ModelStatus
}
