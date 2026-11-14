package com.urunkarpm.drawer.core.designsystem.theme

import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

data class DeviceScreenCurve(
    val topStartDp: Float,
    val topEndDp: Float,
    val bottomStartDp: Float,
    val bottomEndDp: Float,
    val isHardwareDetected: Boolean,
    val rawDetectedDp: Float
) {
    /**
     * Produces a [RoundedCornerShape] combining the detected curves with an optional user offset.
     */
    fun toRoundedCornerShape(offsetDp: Float = 0f, minDp: Float = 0f): RoundedCornerShape {
        val ts = (topStartDp + offsetDp).coerceAtLeast(minDp)
        val te = (topEndDp + offsetDp).coerceAtLeast(minDp)
        val bs = (bottomStartDp + offsetDp).coerceAtLeast(minDp)
        val be = (bottomEndDp + offsetDp).coerceAtLeast(minDp)
        return RoundedCornerShape(
            topStart = ts.dp,
            topEnd = te.dp,
            bottomStart = bs.dp,
            bottomEnd = be.dp
        )
    }
}

object DeviceScreenCurveDetector {

    fun detect(view: View, fallbackDp: Float = 24f): DeviceScreenCurve {
        val density = view.resources.displayMetrics.density
        if (density <= 0f) return DeviceScreenCurve(fallbackDp, fallbackDp, fallbackDp, fallbackDp, false, fallbackDp)

        // 1. Android 12+ (API 31+) hardware insets query
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                val tl = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
                val tr = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0
                val bl = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
                val br = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0

                if (tl > 0 || tr > 0 || bl > 0 || br > 0) {
                    val maxTop = maxOf(tl, tr)
                    val maxBottom = maxOf(bl, br)
                    val tlDp = (if (tl > 0) tl else maxTop).toFloat() / density
                    val trDp = (if (tr > 0) tr else maxTop).toFloat() / density
                    val blDp = (if (bl > 0) bl else maxBottom).toFloat() / density
                    val brDp = (if (br > 0) br else maxBottom).toFloat() / density
                    val rawMax = maxOf(tl, tr, bl, br).toFloat() / density
                    return DeviceScreenCurve(
                        topStartDp = tlDp,
                        topEndDp = trDp,
                        bottomStartDp = blDp,
                        bottomEndDp = brDp,
                        isHardwareDetected = true,
                        rawDetectedDp = rawMax
                    )
                }
            }
        }

        // 2. System internal resource fallback (common across Samsung, Pixel, Xiaomi, OnePlus AOSP ROMs)
        val systemRadiusPx = getSystemDimensionPixelSize(view.context, "rounded_corner_radius")
        if (systemRadiusPx > 0) {
            val radiusDp = systemRadiusPx.toFloat() / density
            return DeviceScreenCurve(
                topStartDp = radiusDp,
                topEndDp = radiusDp,
                bottomStartDp = radiusDp,
                bottomEndDp = radiusDp,
                isHardwareDetected = true,
                rawDetectedDp = radiusDp
            )
        }

        return DeviceScreenCurve(
            topStartDp = fallbackDp,
            topEndDp = fallbackDp,
            bottomStartDp = fallbackDp,
            bottomEndDp = fallbackDp,
            isHardwareDetected = false,
            rawDetectedDp = fallbackDp
        )
    }

    private fun getSystemDimensionPixelSize(context: Context, resName: String): Int {
        return try {
            val resId = context.resources.getIdentifier(resName, "dimen", "android")
            if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        } catch (_: Throwable) {
            0
        }
    }
}

@Composable
fun rememberDeviceScreenCurve(fallbackDp: Float = 24f): DeviceScreenCurve {
    val view = LocalView.current
    var curve by remember(view, fallbackDp) {
        mutableStateOf(DeviceScreenCurveDetector.detect(view, fallbackDp))
    }

    DisposableEffect(view) {
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                curve = DeviceScreenCurveDetector.detect(v, fallbackDp)
            }
            override fun onViewDetachedFromWindow(v: View) {}
        }
        view.addOnAttachStateChangeListener(listener)

        if (!curve.isHardwareDetected && view.isAttachedToWindow) {
            view.post {
                val updated = DeviceScreenCurveDetector.detect(view, fallbackDp)
                if (updated.isHardwareDetected) {
                    curve = updated
                }
            }
        }

        onDispose {
            view.removeOnAttachStateChangeListener(listener)
        }
    }

    return curve
}
