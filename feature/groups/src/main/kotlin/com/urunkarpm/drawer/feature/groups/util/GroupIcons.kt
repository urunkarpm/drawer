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
        "#1E3A8A", // Executive Navy
        "#0F766E", // Deep Corporate Teal
        "#065F46", // Financial Forest Green
        "#334155", // Slate / Charcoal
        "#475569", // Titanium Gray
        "#64748B", // Steel Blue
        "#831843", // Deep Bordeaux Wine
        "#92400E", // Warm Bronze / Amber
        "#3730A3", // Enterprise Indigo
        "#1E293B"  // Midnight Obsidian
    )

    fun parseColor(hex: String?, fallback: Color = Color(0xFF334155)): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val formatted = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
            Color(android.graphics.Color.parseColor(formatted))
        } catch (_: Exception) {
            fallback
        }
    }

    fun resolveCategoryColor(name: String, id: String = ""): Color {
        val lower = name.lowercase().trim()
        val hex = when {
            lower.contains("finance") || lower.contains("bank") || lower.contains("money") || lower.contains("pay") -> "#065F46" // Financial Forest Green
            lower.contains("media") || lower.contains("music") || lower.contains("audio") || lower.contains("song") -> "#831843" // Deep Bordeaux
            lower.contains("video") || lower.contains("stream") || lower.contains("movie") || lower.contains("tv") -> "#9F1239" // Refined Wine
            lower.contains("chat") || lower.contains("social") || lower.contains("message") || lower.contains("talk") -> "#0F766E" // Deep Corporate Teal
            lower.contains("work") || lower.contains("office") || lower.contains("business") || lower.contains("task") -> "#1E3A8A" // Executive Navy
            lower.contains("tool") || lower.contains("util") || lower.contains("system") -> "#475569" // Titanium Gray
            lower.contains("fit") || lower.contains("health") || lower.contains("gym") || lower.contains("run") -> "#0E7490" // Refined Petrol Cyan
            lower.contains("star") || lower.contains("fav") -> "#92400E" // Warm Bronze
            lower.contains("dev") || lower.contains("code") || lower.contains("git") -> "#3730A3" // Enterprise Indigo
            lower.contains("shop") || lower.contains("cart") || lower.contains("store") -> "#78350F" // Amber Earth
            lower.contains("game") || lower.contains("play") -> "#6D28D9" // Royal Amethyst
            else -> {
                val colors = availableColors
                val hash = if (id.isNotEmpty()) id.hashCode() else name.hashCode()
                val index = kotlin.math.abs(hash % colors.size)
                colors[index]
            }
        }
        return parseColor(hex)
    }
}
