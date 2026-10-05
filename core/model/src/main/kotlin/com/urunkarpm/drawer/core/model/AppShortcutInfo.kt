package com.urunkarpm.drawer.core.model

data class AppShortcutInfo(
    val id: String,
    val packageName: String,
    val shortLabel: String,
    val longLabel: String? = null,
    val isEnabled: Boolean = true
)
