package com.urunkarpm.drawer.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global icon cache invalidation bus.
 * Triggered whenever an icon pack or icon override changes.
 */
object IconCacheInvalidator {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun invalidate() {
        _version.value += 1
    }
}
