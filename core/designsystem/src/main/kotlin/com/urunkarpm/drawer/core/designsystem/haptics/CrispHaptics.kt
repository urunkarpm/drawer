package com.urunkarpm.drawer.core.designsystem.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * High-definition crisp haptics engine delivering razor-sharp tactile feedback across all UI touch points.
 * Leverages modern Android VibrationEffect primitives (API 30+) where available, falling back
 * gracefully to predefined effects (API 29+) and native system View haptics.
 */
interface CrispHaptics {
    /** Sharp instantaneous micro-click (e.g., app icon launch, button tap, toggle tap). */
    fun click()

    /** Featherweight mechanical tick (e.g., alphabet index scrubber, slider step, page indicator). */
    fun tick()

    /** Substantial tactile pulse (e.g., long-press activation, drag lift). */
    fun heavyClick()

    /** Celebratory double-pulse or checkmark confirmation (e.g., completing a task, dropping into a slot). */
    fun confirm()

    /** Reject or error pulse (e.g., deleting a task, rejecting an action). */
    fun reject()

    /** Gesture start tactile trigger (e.g., pick-up on drag initiation). */
    fun dragStart()

    /** Gesture end release trigger (e.g., dropping or snapping into position). */
    fun dragEnd()
}

/**
 * No-op implementation for when haptic feedback is disabled in user settings.
 */
object NoOpCrispHaptics : CrispHaptics {
    override fun click() {}
    override fun tick() {}
    override fun heavyClick() {}
    override fun confirm() {}
    override fun reject() {}
    override fun dragStart() {}
    override fun dragEnd() {}
}

/**
 * Standard Android implementation blending hardware vibration primitives with View haptic constants.
 */
class DefaultCrispHaptics(
    private val context: Context?,
    private val view: View?,
    private val enabled: Boolean = true
) : CrispHaptics {

    private val vibrator: Vibrator? by lazy {
        if (!enabled || context == null) null
        else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    private val hasVibrator: Boolean by lazy {
        try {
            vibrator?.hasVibrator() == true
        } catch (_: Throwable) {
            false
        }
    }

    private val supportsPrimitives: Boolean by lazy {
        if (!hasVibrator || Build.VERSION.SDK_INT < Build.VERSION_CODES.R || vibrator == null) false
        else {
            try {
                vibrator?.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    VibrationEffect.Composition.PRIMITIVE_TICK,
                    VibrationEffect.Composition.PRIMITIVE_THUD
                ) == true
            } catch (_: Throwable) {
                false
            }
        }
    }

    override fun click() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (hasVibrator && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                return
            } catch (_: Throwable) {}
        }
        performViewHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun tick() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.65f)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (hasVibrator && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            } catch (_: Throwable) {}
        }
        val constant = if (Build.VERSION.SDK_INT >= 34) {
            26 // HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        performViewHaptic(constant)
    }

    override fun heavyClick() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (hasVibrator && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                return
            } catch (_: Throwable) {}
        }
        performViewHaptic(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun confirm() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 60)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performViewHaptic(16) // HapticFeedbackConstants.CONFIRM
            return
        }
        if (hasVibrator && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                return
            } catch (_: Throwable) {}
        }
        performViewHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun reject() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 90)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performViewHaptic(17) // HapticFeedbackConstants.REJECT
            return
        }
        performViewHaptic(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun dragStart() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.85f)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= 34) {
            performViewHaptic(25) // HapticFeedbackConstants.DRAG_START
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performViewHaptic(12) // HapticFeedbackConstants.GESTURE_START
            return
        }
        performViewHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun dragEnd() {
        if (!enabled) return
        if (supportsPrimitives && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f)
                    .compose()
                vibrator?.vibrate(effect)
                return
            } catch (_: Throwable) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performViewHaptic(13) // HapticFeedbackConstants.GESTURE_END
            return
        }
        performViewHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun performViewHaptic(constant: Int) {
        try {
            view?.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } catch (_: Throwable) {}
    }
}

val LocalCrispHaptics: ProvidableCompositionLocal<CrispHaptics> =
    staticCompositionLocalOf { NoOpCrispHaptics }

@Composable
fun rememberCrispHaptics(enabled: Boolean = true): CrispHaptics {
    if (!enabled) return NoOpCrispHaptics
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view, enabled) {
        DefaultCrispHaptics(context = context.applicationContext, view = view, enabled = enabled)
    }
}
