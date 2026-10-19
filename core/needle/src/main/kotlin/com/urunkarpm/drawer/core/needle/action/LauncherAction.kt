package com.urunkarpm.drawer.core.needle.action

import android.content.Intent

data class LauncherAction(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val intent: Intent,
    val isDirectExecutionSafe: Boolean = false
)
