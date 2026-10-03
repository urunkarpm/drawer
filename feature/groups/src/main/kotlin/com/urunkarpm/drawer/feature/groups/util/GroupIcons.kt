package com.urunkarpm.drawer.feature.groups.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object GroupIcons {
    val availableIcons: Map<String, ImageVector> = mapOf(
        "folder" to Icons.Default.Folder,
        "work" to Icons.Default.Work,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "account_balance" to Icons.Default.AccountBalance,
        "play_circle" to Icons.Default.PlayCircle,
        "build" to Icons.Default.Build,
        "gamepad" to Icons.Default.SportsEsports,
        "shopping" to Icons.Default.ShoppingCart,
        "fitness" to Icons.Default.FitnessCenter,
        "star" to Icons.Default.Star,
        "bookmark" to Icons.Default.Bookmark,
        "music" to Icons.Default.MusicNote,
        "code" to Icons.Default.Code
    )

    fun getIcon(name: String): ImageVector =
        availableIcons[name] ?: Icons.Default.Folder

    val availableColors: List<String> = listOf(
        "#1E88E5", // Blue
        "#E91E63", // Pink
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#9C27B0", // Purple
        "#009688", // Teal
        "#E53935", // Red
        "#5E35B1", // Deep Purple
        "#FDD835", // Yellow
        "#607D8B"  // Blue Grey
    )

    fun parseColor(hex: String, fallback: Color = Color(0xFF6750A4)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }
}
