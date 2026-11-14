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
    val sizePx: Float,
    val color: Color,
    val shape: ConfettiShape,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val tumbleSpeed: Float,
    val spawnDelayMs: Float,
    val lifeMs: Float
)

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

        val particles = remember(triggerKey, widthPx, heightPx, palette) {
            if (widthPx <= 0f || heightPx <= 0f) emptyList()
            else createBirdayParticles(
                widthPx = widthPx,
                heightPx = heightPx,
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
            val totalDurationMs = particles.maxOf { it.spawnDelayMs + it.lifeMs }
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
                if (ageMs < 0f || ageMs > particle.lifeMs) continue

                val tSec = ageMs / 1000f
                val progress = (ageMs / particle.lifeMs).coerceIn(0f, 1f)
                val x = particle.startX + particle.vx * tSec
                val y = particle.startY + particle.vy * tSec + 0.5f * particle.gravity * tSec * tSec

                if (y > size.height + particle.sizePx * 2f) continue

                // Smooth fade out across the final 35% of particle lifetime
                val alpha = if (progress > 0.65f) {
                    (1f - (progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
                } else {
                    1f
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
    heightPx: Float,
    sizeOptionsPx: List<Float>,
    driftScalePx: Float,
    colors: List<Color>
): List<ConfettiParticle> {
    val shapes = ConfettiShape.entries
    val count = 200
    val emitterDurationMs = 2000f
    val random = Random(System.nanoTime())

    return List(count) { index ->
        val angleRad = random.nextDouble(0.0, Math.PI * 2.0)
        val initialSpeed = random.nextFloat() * 110f * driftScalePx + 20f * driftScalePx
        val vx = (cos(angleRad) * initialSpeed).toFloat()
        val vy = (sin(angleRad) * initialSpeed * 0.45f).toFloat() + random.nextFloat() * 140f * driftScalePx + 120f * driftScalePx
        val gravity = random.nextFloat() * 140f * driftScalePx + 240f * driftScalePx
        val extraMargin = 40f * driftScalePx
        val spawnX = -extraMargin + random.nextFloat() * (widthPx + extraMargin * 2f)
        val spawnDelay = (index.toFloat() / count) * emitterDurationMs + random.nextFloat() * 40f
        val lifeMs = ((heightPx / (320f * driftScalePx)).coerceIn(1.8f, 3.2f) * 1000f) + random.nextFloat() * 500f

        ConfettiParticle(
            startX = spawnX,
            startY = -24f * driftScalePx,
            vx = vx,
            vy = vy,
            gravity = gravity,
            sizePx = sizeOptionsPx[random.nextInt(sizeOptionsPx.size)],
            color = colors[random.nextInt(colors.size)],
            shape = shapes[random.nextInt(shapes.size)],
            initialRotation = random.nextFloat() * 360f,
            rotationSpeed = (random.nextFloat() - 0.5f) * 320f,
            tumbleSpeed = random.nextFloat() * 6f + 2f,
            spawnDelayMs = spawnDelay,
            lifeMs = lifeMs
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
