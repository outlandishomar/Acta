package com.outlandishomar.acta.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape {
    Triangle,
    Sparkle,
    Star,
    Octagram
}

private class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val gravity: Float,
    val drag: Float,
    val sizePx: Float,
    val color: Color,
    val shape: ConfettiShape,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val tumbleSpeed: Float,
    val spawnDelayMs: Float,
    val timeToLiveMs: Float,
    val fadeDurationMs: Float
) {
    val totalLifeMs: Float get() = timeToLiveMs + fadeDurationMs
}

/**
 * Birday-inspired confetti stream overlay raining from the top edge using the 4 vector shapes
 * (triangle, sparkle, star, octagram) in 3 sizes with rotation and smooth fade-out.
 */
@Composable
fun BirthdayConfettiOverlay(
    triggerKey: Int,
    onFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (triggerKey <= 0) return

    val density = LocalDensity.current
    val palette = remember(Blue500, TextPrimary) {
        listOf(
            Blue500,
            TextPrimary,
            Color(0xFFFFB74D),
            Color(0xFFFF758F)
        )
    }

    val shapePaths = remember { buildNormalizedConfettiShapes() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val particles = remember(triggerKey, widthPx, palette) {
            if (widthPx <= 0f || heightPx <= 0f) emptyList()
            else createBirdayParticles(
                widthPx = widthPx,
                sizeOptionsPx = listOf(
                    with(density) { 8.dp.toPx() },
                    with(density) { 12.dp.toPx() },
                    with(density) { 16.dp.toPx() }
                ),
                driftScalePx = with(density) { 1.dp.toPx() },
                colors = palette
            )
        }

        var elapsedMs by remember(triggerKey) { mutableFloatStateOf(0f) }

        LaunchedEffect(triggerKey, particles) {
            if (particles.isEmpty()) return@LaunchedEffect
            elapsedMs = 0f
            val totalDurationMs = particles.maxOf { it.spawnDelayMs + it.totalLifeMs }
            var startNanos = -1L
            while (elapsedMs < totalDurationMs) {
                withFrameNanos { frameNanos ->
                    if (startNanos < 0L) startNanos = frameNanos
                    elapsedMs = (frameNanos - startNanos) / 1_000_000f
                }
            }
            onFinished()
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val nowMs = elapsedMs
            for (particle in particles) {
                val ageMs = nowMs - particle.spawnDelayMs
                if (ageMs < 0f || ageMs > particle.totalLifeMs) continue

                val tSec = ageMs / 1000f
                // Exponential air drag on initial burst velocity + gentle Konfetti gravity
                val dragFactor = if (particle.drag > 0f) {
                    (1f - kotlin.math.exp(-particle.drag * tSec)) / particle.drag
                } else tSec
                val x = particle.startX + particle.vx * dragFactor
                val y = particle.startY + particle.vy * dragFactor + 0.5f * particle.gravity * tSec * tSec

                if (y > size.height + particle.sizePx * 2f) continue

                // Konfetti behavior: full opacity during timeToLive (2000ms), then fade out smoothly
                val alpha = if (ageMs <= particle.timeToLiveMs) {
                    1f
                } else {
                    (1f - (ageMs - particle.timeToLiveMs) / particle.fadeDurationMs).coerceIn(0f, 1f)
                }

                val rotation = particle.initialRotation + particle.rotationSpeed * tSec
                val flutterScaleY = 0.35f + 0.65f * kotlin.math.abs(cos(particle.tumbleSpeed * tSec))
                val path = shapePaths.getValue(particle.shape)

                withTransform({
                    translate(left = x, top = y)
                    rotate(degrees = rotation, pivot = Offset.Zero)
                    scale(scaleX = particle.sizePx, scaleY = particle.sizePx * flutterScaleY, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = path,
                        color = particle.color.copy(alpha = alpha)
                    )
                }
            }
        }
    }
}

private fun createBirdayParticles(
    widthPx: Float,
    sizeOptionsPx: List<Float>,
    driftScalePx: Float,
    colors: List<Color>
): List<ConfettiParticle> {
    val shapes = ConfettiShape.entries
    val count = 200
    val emitterDurationMs = 2000f
    val random = Random(System.nanoTime())

    // Matches Birday's DisplayUtils.streamBirdayConfetti:
    // setDirection(0.0, 359.0), setSpeed(0.5f, 4f), setTimeToLive(2000L), setFadeOutEnabled(true), streamFor(200, 2000L)
    return List(count) { index ->
        val angleRad = random.nextDouble(0.0, Math.PI * 2.0)
        val konfettiSpeed = 0.5f + random.nextFloat() * 3.5f
        val launchSpeedPx = konfettiSpeed * 32f * driftScalePx
        val vx = (cos(angleRad) * launchSpeedPx).toFloat()
        val vy = (sin(angleRad) * launchSpeedPx).toFloat()
        val gravity = (115f + random.nextFloat() * 45f) * driftScalePx
        val extraMargin = 20f * driftScalePx
        val spawnX = -extraMargin + random.nextFloat() * (widthPx + extraMargin * 2f)
        val spawnDelay = (index.toFloat() / count) * emitterDurationMs + random.nextFloat() * 30f

        ConfettiParticle(
            startX = spawnX,
            startY = -18f * driftScalePx,
            vx = vx,
            vy = vy,
            gravity = gravity,
            drag = 1.6f,
            sizePx = sizeOptionsPx[random.nextInt(sizeOptionsPx.size)],
            color = colors[random.nextInt(colors.size)],
            shape = shapes[random.nextInt(shapes.size)],
            initialRotation = random.nextFloat() * 360f,
            rotationSpeed = (random.nextFloat() - 0.5f) * 240f,
            tumbleSpeed = random.nextFloat() * 5f + 2f,
            spawnDelayMs = spawnDelay,
            timeToLiveMs = 2000f,
            fadeDurationMs = 750f
        )
    }
}

private fun buildNormalizedConfettiShapes(): Map<ConfettiShape, Path> {
    fun norm(v: Float): Float = (v - 12f) / 24f

    fun pathFromPoints(vararg coords: Pair<Float, Float>): Path = Path().apply {
        coords.forEachIndexed { idx, (x, y) ->
            if (idx == 0) moveTo(norm(x), norm(y))
            else lineTo(norm(x), norm(y))
        }
        close()
    }

    return mapOf(
        ConfettiShape.Triangle to pathFromPoints(
            1f to 21f,
            23f to 21f,
            12f to 2f
        ),
        ConfettiShape.Sparkle to pathFromPoints(
            12f to 1f,
            9f to 9f,
            1f to 12f,
            9f to 15f,
            12f to 23f,
            15f to 15f,
            23f to 12f,
            15f to 9f
        ),
        ConfettiShape.Star to pathFromPoints(
            12f to 17.27f,
            18.18f to 21f,
            16.54f to 13.97f,
            22f to 9.24f,
            14.81f to 8.62f,
            12f to 2f,
            9.19f to 8.62f,
            2f to 9.24f,
            7.45f to 13.97f,
            5.82f to 21f
        ),
        ConfettiShape.Octagram to pathFromPoints(
            2.2f to 16.06f,
            3.88f to 12f,
            2.2f to 7.94f,
            6.26f to 6.26f,
            7.94f to 2.2f,
            12f to 3.88f,
            16.06f to 2.2f,
            17.74f to 6.26f,
            21.8f to 7.94f,
            20.12f to 12f,
            21.8f to 16.06f,
            17.74f to 17.74f,
            16.06f to 21.8f,
            12f to 20.12f,
            7.94f to 21.8f,
            6.26f to 17.74f
        )
    )
}
