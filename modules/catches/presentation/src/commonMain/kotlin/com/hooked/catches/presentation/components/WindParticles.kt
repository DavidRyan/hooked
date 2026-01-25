package com.hooked.catches.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private data class WindParticle(
    var pos: Offset,
    var vel: Offset,
    var lenPx: Float,
    var alpha: Float,
    var thicknessPx: Float,
)

@Composable
fun WindParticles(
    modifier: Modifier = Modifier,
    windFromDegrees: Float,
    windSpeed: Float,
    particleCount: Int = 120,
    color: Color = Color.White,
    baseDriftPxPerSec: Float = 40f,
    speedScale: Float = 18f,
    turbulence: Float = 0.15f,
) {
    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val windToDegrees = (windFromDegrees + 180f) % 360f
        val theta = windToDegrees * (PI / 180.0)
        val dir = Offset(
            x = sin(theta).toFloat(),
            y = -cos(theta).toFloat(),
        ).let { if (it.getDistance() == 0f) Offset(0f, 1f) else it / it.getDistance() }

        val rng = remember(w, h, particleCount) { Random(12345) }
        val particles = remember(w, h, particleCount) {
            MutableList(particleCount) {
                val speedJitter = 0.7f + rng.nextFloat() * 0.6f
                val vMag = baseDriftPxPerSec + (windSpeed * speedScale) * speedJitter
                WindParticle(
                    pos = Offset(rng.nextFloat() * w, rng.nextFloat() * h),
                    vel = dir * vMag,
                    lenPx = 10f + rng.nextFloat() * 36f,
                    alpha = 0.08f + rng.nextFloat() * 0.28f,
                    thicknessPx = 0.5f + rng.nextFloat() * 1.4f,
                )
            }
        }
        var frameTime by remember { mutableStateOf(0L) }

        LaunchedEffect(dir, windSpeed, baseDriftPxPerSec, speedScale) {
            for (p in particles) {
                val speedJitter = 0.85f + (p.lenPx / 32f) * 0.3f
                val vMag = baseDriftPxPerSec + (windSpeed * speedScale) * speedJitter
                p.vel = dir * vMag
            }
        }

        LaunchedEffect(w, h, particles) {
            var lastT = 0L
            while (true) {
                withFrameNanos { t ->
                    if (lastT == 0L) {
                        lastT = t
                        return@withFrameNanos
                    }
                    val dt = (t - lastT) / 1_000_000_000f
                    lastT = t

                    val perp = Offset(-dir.y, dir.x)
                    val jitterMag = turbulence * (10f + windSpeed * 2.5f)

                    for (p in particles) {
                        val jitter = (rng.nextFloat() - 0.5f) * jitterMag
                        val v = p.vel + perp * jitter
                        p.pos = p.pos + v * dt

                        val margin = 40f
                        var x = p.pos.x
                        var y = p.pos.y

                        if (x < -margin) x = w + margin
                        if (x > w + margin) x = -margin
                        if (y < -margin) y = h + margin
                        if (y > h + margin) y = -margin

                        p.pos = Offset(x, y)
                    }
                    frameTime = t
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            frameTime
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            )
            for (p in particles) {
                val tail = p.pos - (p.vel / (baseDriftPxPerSec + windSpeed * speedScale + 1f)) * p.lenPx * 25f
                drawLine(
                    color = color.copy(alpha = p.alpha),
                    start = tail,
                    end = p.pos,
                    strokeWidth = p.thicknessPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun Offset.getDistance(): Float = hypot(x, y)
private operator fun Offset.times(s: Float) = Offset(x * s, y * s)
private operator fun Offset.div(s: Float) = Offset(x / s, y / s)
private operator fun Offset.plus(o: Offset) = Offset(x + o.x, y + o.y)
private operator fun Offset.minus(o: Offset) = Offset(x - o.x, y - o.y)
