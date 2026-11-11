package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchResult(
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val defaultRadiusM: Float = 200f
)
