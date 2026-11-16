package com.urunkarpm.drawer.core.designsystem.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

interface CrispHaptics {
    fun click()
    fun tick()
    fun heavyClick()
    fun confirm()
    fun reject()
    fun dragStart()
    fun dragEnd()
}

object NoOpCrispHaptics : CrispHaptics {
    override fun click() {}
    override fun tick() {}
    override fun heavyClick() {}
    override fun confirm() {}
    override fun reject() {}
    override fun dragStart() {}
    override fun dragEnd() {}
}

// ponytail: Native View-based haptic feedback engine; uses standard Android platform constants without reflection or vibrator service handles.
class DefaultCrispHaptics(
    private val context: android.content.Context? = null,
    private val view: View? = null,
    private val enabled: Boolean = true
) : CrispHaptics {

    private fun perform(constant: Int) {
        if (!enabled) return
        try {
            view?.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
        } catch (_: Throwable) {}
    }

    override fun click() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    override fun tick() {
        val constant = if (Build.VERSION.SDK_INT >= 34) 26 else HapticFeedbackConstants.CLOCK_TICK
        perform(constant)
    }

    override fun heavyClick() = perform(HapticFeedbackConstants.LONG_PRESS)

    override fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 16 else HapticFeedbackConstants.KEYBOARD_TAP
        perform(constant)
    }

    override fun reject() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 17 else HapticFeedbackConstants.LONG_PRESS
        perform(constant)
    }

    override fun dragStart() {
        val constant = when {
            Build.VERSION.SDK_INT >= 34 -> 25
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> 12
            else -> HapticFeedbackConstants.KEYBOARD_TAP
        }
        perform(constant)
    }

    override fun dragEnd() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 13 else HapticFeedbackConstants.KEYBOARD_TAP
        perform(constant)
    }
}

val LocalCrispHaptics: ProvidableCompositionLocal<CrispHaptics> =
    staticCompositionLocalOf { NoOpCrispHaptics }

@Composable
fun rememberCrispHaptics(enabled: Boolean = true): CrispHaptics {
    if (!enabled) return NoOpCrispHaptics
    val view = LocalView.current
    return remember(view, enabled) {
        DefaultCrispHaptics(view = view, enabled = enabled)
    }
}
