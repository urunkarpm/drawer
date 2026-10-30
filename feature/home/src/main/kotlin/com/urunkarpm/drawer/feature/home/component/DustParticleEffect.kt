package com.urunkarpm.drawer.feature.home.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * State holder and animator for canvas dust particle bursts.
 */
class DustParticleState {
    private class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val radius: Float,
        val color: Color,
        var age: Float,
        val maxAge: Float
    )

    private val particles = ArrayList<Particle>(64)
    var hasParticles by mutableStateOf(false)
        private set
    var frameTick by mutableLongStateOf(0L)
        private set

    // ponytail: 2D Canvas dust burst using simple radial velocity integration and a hard cap of 64 particles; ceiling is no collision physics or 3D depth; upgrade path is an AGSL RuntimeShader with fragment noise on Android 13+.
    fun spawnPuff(center: Offset, particleColor: Color, count: Int = 16) {
        if (particles.size >= 64) return
        val random = Random(System.nanoTime())
        val actualCount = count.coerceAtMost(64 - particles.size)
        for (i in 0 until actualCount) {
            val angle = random.nextDouble(0.0, Math.PI * 2)
            val speed = random.nextDouble(40.0, 160.0).toFloat()
            val radius = random.nextDouble(2.0, 4.5).toFloat()
            val maxAge = random.nextDouble(0.24, 0.34).toFloat()

            // Subtle color jitter for organic dust texture
            val alphaFactor = random.nextDouble(0.6, 0.95).toFloat()
            val finalColor = particleColor.copy(alpha = alphaFactor)

            particles.add(
                Particle(
                    x = center.x,
                    y = center.y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat() - 25f, // slight upward chimney drift
                    radius = radius,
                    color = finalColor,
                    age = 0f,
                    maxAge = maxAge
                )
            )
        }
        hasParticles = true
    }

    fun update(dtSeconds: Float, now: Long) {
        val iterator = particles.listIterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age += dtSeconds
            if (p.age >= p.maxAge) {
                iterator.remove()
            } else {
                p.x += p.vx * dtSeconds
                p.y += p.vy * dtSeconds
            }
        }
        hasParticles = particles.isNotEmpty()
        frameTick = now
    }

    fun draw(drawScope: DrawScope) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick // Register draw-phase dependency
        for (i in 0 until particles.size) {
            val p = particles[i]
            val progress = (p.age / p.maxAge).coerceIn(0f, 1f)
            val alpha = (1f - progress) * p.color.alpha
            drawScope.drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius * (1f + progress * 0.4f),
                center = Offset(p.x, p.y)
            )
        }
    }
}

@Composable
fun rememberDustParticleState(): DustParticleState {
    return remember { DustParticleState() }
}

@Composable
fun DustParticleOverlay(
    state: DustParticleState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.hasParticles) {
        if (state.hasParticles) {
            var lastTime = withFrameNanos { it }
            while (state.hasParticles) {
                withFrameNanos { now ->
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastTime = now
                    state.update(dt, now)
                }
            }
        }
    }

    if (state.hasParticles) {
        Canvas(modifier = modifier.fillMaxSize()) {
            state.draw(this)
        }
    }
}
