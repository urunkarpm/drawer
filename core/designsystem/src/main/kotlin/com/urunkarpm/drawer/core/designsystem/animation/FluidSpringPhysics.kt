package com.urunkarpm.drawer.core.designsystem.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationResult
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.abs
import kotlin.math.sign

/**
 * iOS-grade fluid physics and spring animation system.
 *
 * Core Principles:
 * 1. Fluid Spring Timings: Apple's core design language uses critically damped and underdamped
 *    spring systems with settling duration around 320ms.
 * 2. Velocity Interruption & Momentum Handoff: Mid-motion interruptions capture the exact
 *    pixel-per-second momentum of the touch interaction and inject it as the initialVelocity
 *    of the concluding animation.
 * 3. Rubber-Banding: When dragged past limits, structural limits are enforced using Apple's
 *    non-linear curve: d = (x * c) / (x + c).
 */
object FluidSpringPhysics {

    /**
     * Target settling duration around 320ms.
     */
    const val FLUID_SETTLING_DURATION_MS = 320

    /**
     * Stiffness calculated for ~320ms settling time in second-order differential equation.
     */
    const val FLUID_STIFFNESS = 380f

    /**
     * Underdamped ratio providing natural Apple-like fluid feel with minimal settling bounce.
     */
    const val FLUID_DAMPING_RATIO = 0.82f

    /**
     * Critically damped ratio (zeta = 1.0f) for 100% bounce-free, crisp settling.
     */
    const val CRITICALLY_DAMPED_RATIO = Spring.DampingRatioNoBouncy // 1.0f

    /**
     * Creates a fluid underdamped spring spec settling around 320ms.
     */
    fun <T> fluidSpring(
        dampingRatio: Float = FLUID_DAMPING_RATIO,
        stiffness: Float = FLUID_STIFFNESS,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold
    )

    /**
     * Creates a critically damped spring spec settling around 320ms without overshoot.
     */
    fun <T> criticallyDampedSpring(
        stiffness: Float = FLUID_STIFFNESS,
        visibilityThreshold: T? = null
    ): SpringSpec<T> = spring(
        dampingRatio = CRITICALLY_DAMPED_RATIO,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold
    )

    /**
     * Apple's non-linear rubber-band displacement formula:
     * d = (x * c) / (x + c)
     *
     * Properties:
     * - At x = 0, d = 0.
     * - As x -> infinity, d -> c (asymptote / structural limit).
     * - Near 0, derivative is 1 (feels 1:1 to finger touch, then progressively resists).
     *
     * @param distance Actual finger distance past the boundary (x >= 0)
     * @param coefficient Structural limit coefficient (c > 0)
     * @return Visible rubber-band displacement (d)
     */
    fun rubberBand(distance: Float, coefficient: Float = 360f): Float {
        if (distance <= 0f) return 0f
        return (distance * coefficient) / (distance + coefficient)
    }

    /**
     * Bidirectional signed rubber-banding for a single displacement offset from 0.
     */
    fun signedRubberBand(distance: Float, coefficient: Float = 360f): Float {
        val magnitude = abs(distance)
        val rb = rubberBand(magnitude, coefficient)
        return sign(distance) * rb
    }

    /**
     * Clamps displacement within [minLimit, maxLimit] with rubber-banding resistance past the limits.
     *
     * @param value Current raw position
     * @param minLimit Lower boundary
     * @param maxLimit Upper boundary
     * @param coefficient Resistance coefficient
     */
    fun boundedRubberBand(
        value: Float,
        minLimit: Float,
        maxLimit: Float,
        coefficient: Float = 360f
    ): Float {
        return when {
            value < minLimit -> minLimit - rubberBand(minLimit - value, coefficient)
            value > maxLimit -> maxLimit + rubberBand(value - maxLimit, coefficient)
            else -> value
        }
    }
}

/**
 * Concludes an animation with seamless velocity handoff, injecting finger momentum (px/sec)
 * as the initialVelocity of the spring.
 */
suspend fun Animatable<Float, AnimationVector1D>.animateWithVelocityHandoff(
    targetValue: Float,
    initialVelocity: Float = 0f,
    dampingRatio: Float = FluidSpringPhysics.FLUID_DAMPING_RATIO,
    stiffness: Float = FluidSpringPhysics.FLUID_STIFFNESS
): AnimationResult<Float, AnimationVector1D> {
    return animateTo(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        initialVelocity = initialVelocity
    )
}
