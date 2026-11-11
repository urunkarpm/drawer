package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentSearch(
    val query: String,
    val createdAt: Long = System.currentTimeMillis()
)
