package com.urunkarpm.drawer.feature.groups.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object GroupIcons {
    // ponytail: unique 1:1 icon registry without duplicate alias keys in the picker; ceiling: fixed curated list of Material vectors; upgrade path: custom icon pack picker.
    val availableIcons: Map<String, ImageVector> = mapOf(
        "folder" to Icons.Default.Folder,
        "work" to Icons.Default.Work,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "account_balance" to Icons.Default.AccountBalance,
        "play_circle" to Icons.Default.PlayCircle,
        "camera" to Icons.Default.CameraAlt,
        "email" to Icons.Default.Email,
        "phone" to Icons.Default.Phone,
        "build" to Icons.Default.Build,
        "sports_esports" to Icons.Default.SportsEsports,
        "shopping" to Icons.Default.ShoppingCart,
        "fitness" to Icons.Default.FitnessCenter,
        "palette" to Icons.Default.Palette,
        "star" to Icons.Default.Star,
        "bookmark" to Icons.Default.Bookmark,
        "music" to Icons.Default.MusicNote,
        "code" to Icons.Default.Code
    )

    fun getIcon(name: String): ImageVector {
        availableIcons[name]?.let { return it }
        // Handle legacy alias names gracefully without showing duplicate icons in picker
        return when (name.lowercase().trim()) {
            "credit_card" -> Icons.Default.AccountBalance
            "gamepad" -> Icons.Default.SportsEsports
            "shopping_bag" -> Icons.Default.ShoppingCart
            "fitness_center" -> Icons.Default.FitnessCenter
            else -> Icons.Default.Folder
        }
    }

    // ponytail: Curated executive editorial palette (zero neon toy primaries; muted slate, sage, terracotta, sand, amethyst, mineral teal).
    val availableColors: List<String> = listOf(
        "#64748B", // Slate Steel
        "#5F8575", // Sage Green
        "#A35D6A", // Dusty Rose
        "#B8864E", // Warm Sand
        "#7E6E9B", // Muted Amethyst
        "#5A8288", // Mineral Teal
        "#B36A5E", // Terracotta
        "#8E6B88", // Muted Plum
        "#6B728E", // Nordic Indigo
        "#608078"  // Olive Slate
    )

    // ponytail: preserves user-selected color hex directly; only falls back to category-aware palette when unspecified; ceiling: hex string only; upgrade path: full Material ColorScheme integration.
    fun isDullOrGrey(hex: String?): Boolean = hex.isNullOrBlank()

    fun parseColor(hex: String?, fallback: Color = Color(0xFF64748B)): Color {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val formatted = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
            Color(android.graphics.Color.parseColor(formatted))
        } catch (_: Exception) {
            fallback
        }
    }

    fun resolveGroupColor(colorHex: String?, name: String, id: String = "", style: String = "NEUTRAL"): Color {
        return if (style == "EDITORIAL") {
            if (colorHex.isNullOrBlank()) {
                resolveEditorialCategoryColor(name, id)
            } else {
                parseColor(colorHex, fallback = resolveEditorialCategoryColor(name, id))
            }
        } else {
            if (colorHex.isNullOrBlank()) {
                resolveCategoryColor(name, id)
            } else {
                parseColor(colorHex, fallback = resolveCategoryColor(name, id))
            }
        }
    }

    fun resolveEditorialCategoryColor(name: String, id: String = ""): Color {
        val lower = name.lowercase().trim()
        val hex = when {
            lower.contains("finance") || lower.contains("bank") || lower.contains("money") || lower.contains("pay") -> "#5F8575" // Sage Green
            lower.contains("media") || lower.contains("music") || lower.contains("audio") || lower.contains("song") -> "#8E6B88" // Muted Plum
            lower.contains("video") || lower.contains("stream") || lower.contains("movie") || lower.contains("tv") -> "#A35D6A" // Dusty Rose
            lower.contains("chat") || lower.contains("social") || lower.contains("message") || lower.contains("talk") -> "#5A8288" // Mineral Teal
            lower.contains("work") || lower.contains("office") || lower.contains("business") || lower.contains("task") -> "#64748B" // Slate Steel
            lower.contains("tool") || lower.contains("util") || lower.contains("system") -> "#7E6E9B" // Muted Amethyst
            lower.contains("fit") || lower.contains("health") || lower.contains("gym") || lower.contains("run") -> "#608078" // Olive Slate
            lower.contains("star") || lower.contains("fav") -> "#B8864E" // Warm Sand
            lower.contains("dev") || lower.contains("code") || lower.contains("git") -> "#6B728E" // Nordic Indigo
            lower.contains("shop") || lower.contains("cart") || lower.contains("store") -> "#B36A5E" // Terracotta
            lower.contains("game") || lower.contains("play") -> "#7E6E9B" // Muted Amethyst
            lower.contains("photo") || lower.contains("camera") || lower.contains("image") -> "#A35D6A" // Dusty Rose
            lower.contains("mail") || lower.contains("email") -> "#64748B" // Slate Steel
            else -> {
                val colors = availableColors
                val hash = if (id.isNotEmpty()) id.hashCode() else name.hashCode()
                val index = kotlin.math.abs(hash % colors.size)
                colors[index]
            }
        }
        return parseColor(hex)
    }

    fun resolveCategoryColor(name: String, id: String = ""): Color {
        val lower = name.lowercase().trim()
        val hex = when {
            lower.contains("finance") || lower.contains("bank") || lower.contains("money") || lower.contains("pay") -> "#10B981" // Fresh Emerald
            lower.contains("media") || lower.contains("music") || lower.contains("audio") || lower.contains("song") -> "#EC4899" // Vibrant Pink
            lower.contains("video") || lower.contains("stream") || lower.contains("movie") || lower.contains("tv") -> "#F43F5E" // Vibrant Rose
            lower.contains("chat") || lower.contains("social") || lower.contains("message") || lower.contains("talk") -> "#06B6D4" // Vibrant Cyan
            lower.contains("work") || lower.contains("office") || lower.contains("business") || lower.contains("task") -> "#3B82F6" // Electric Blue
            lower.contains("tool") || lower.contains("util") || lower.contains("system") -> "#8B5CF6" // Electric Violet
            lower.contains("fit") || lower.contains("health") || lower.contains("gym") || lower.contains("run") -> "#14B8A6" // Luminous Teal
            lower.contains("star") || lower.contains("fav") -> "#F59E0B" // Solar Amber
            lower.contains("dev") || lower.contains("code") || lower.contains("git") -> "#6366F1" // Vivid Indigo
            lower.contains("shop") || lower.contains("cart") || lower.contains("store") -> "#F97316" // Vibrant Tangerine
            lower.contains("game") || lower.contains("play") -> "#D946EF" // Vibrant Fuchsia
            lower.contains("photo") || lower.contains("camera") || lower.contains("image") -> "#EC4899" // Vibrant Pink
            lower.contains("mail") || lower.contains("email") -> "#3B82F6" // Electric Blue
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
