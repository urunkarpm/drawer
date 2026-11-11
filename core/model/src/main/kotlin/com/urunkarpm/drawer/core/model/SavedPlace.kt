package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedPlace(
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusM: Float = 200f
)
