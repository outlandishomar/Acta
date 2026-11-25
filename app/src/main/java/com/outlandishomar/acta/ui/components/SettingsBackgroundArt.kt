package com.outlandishomar.acta.ui.components

import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.R
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.ThemeState
import com.outlandishomar.acta.ui.theme.TopBarBackground
import com.outlandishomar.acta.ui.theme.calculateLuminance

data class BackgroundArtOption(
    val resId: Int,
    val displayName: String
)

private data class ArtLayoutConfig(
    val widthFraction: Float = 0f, // 0f preserves legacy 9.5.sp sizing for existing arts
    val heightFraction: Float = 0f, // 0f keeps 8.2/9.5 legacy lineHeight ratio
    val alignment: Alignment = Alignment.BottomCenter,
    val offsetY: Dp = 22.dp,
    val trimBlankColumns: Boolean = false,
    val fadeTopEnd: Float = 0f,
    val fadeBottomStart: Float = 0.78f,
    val fadeHorizontal: Float = 0f,
    val fullBleedTop: Boolean = false,
    val repeatFirstLineCount: Int = 0
)

private val NEW_ART_LAYOUT_OVERRIDES = mapOf(
    R.string.ascii_pulp_fiction_dance to ArtLayoutConfig(
        widthFraction = 0.82f,
        heightFraction = 0.51f,
        alignment = Alignment.BottomCenter,
        offsetY = 6.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.92f
    ),
    R.string.ascii_kill_bill to ArtLayoutConfig(
        widthFraction = 0.94f,
        heightFraction = 0.41f,
        alignment = Alignment.BottomCenter,
        offsetY = 14.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.88f
    ),
    R.string.ascii_se7en to ArtLayoutConfig(
        widthFraction = 0.92f,
        heightFraction = 0.44f,
        alignment = Alignment.BottomCenter,
        offsetY = 6.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.92f
    ),
    R.string.ascii_the_thing to ArtLayoutConfig(
        widthFraction = 0.88f,
        heightFraction = 0.60f,
        alignment = Alignment.BottomCenter,
        offsetY = 10.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_spiderman_3 to ArtLayoutConfig(
        widthFraction = 0.84f,
        heightFraction = 0.62f,
        alignment = Alignment.BottomEnd,
        offsetY = 8.dp,
        trimBlankColumns = false,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_dead_space to ArtLayoutConfig(
        widthFraction = 1.05f,
        heightFraction = 0.99f,
        alignment = Alignment.BottomCenter,
        offsetY = 6.dp,
        trimBlankColumns = false,
        fadeBottomStart = 0.94f,
        fullBleedTop = true,
        repeatFirstLineCount = 35
    ),
    R.string.ascii_assassins_creed to ArtLayoutConfig(
        widthFraction = 0.88f,
        heightFraction = 0.56f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = true,
        fadeTopEnd = 0.14f,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_kevin_hart_cat to ArtLayoutConfig(
        widthFraction = 0.78f,
        heightFraction = 0.45f,
        alignment = Alignment.BottomEnd,
        offsetY = 6.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_nokias_bond to ArtLayoutConfig(
        widthFraction = 0.94f,
        heightFraction = 0.19f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f,
        fadeHorizontal = 0.14f
    ),
    R.string.ascii_scarface to ArtLayoutConfig(
        widthFraction = 0.76f,
        heightFraction = 0.46f,
        alignment = Alignment.BottomStart,
        offsetY = 6.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_akira to ArtLayoutConfig(
        widthFraction = 0.84f,
        heightFraction = 0.36f,
        alignment = Alignment.BottomCenter,
        offsetY = (-95).dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_niko_bellic to ArtLayoutConfig(
        widthFraction = 0.88f,
        heightFraction = 0.56f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_superman to ArtLayoutConfig(
        widthFraction = 0.96f,
        heightFraction = 0.89f,
        alignment = Alignment.BottomCenter,
        offsetY = 6.dp,
        trimBlankColumns = false,
        fadeTopEnd = 0.03f,
        fadeBottomStart = 0.94f,
        fadeHorizontal = 0.03f
    ),
    R.string.ascii_john_marston to ArtLayoutConfig(
        widthFraction = 0.78f,
        heightFraction = 0.66f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_arthur_morgan to ArtLayoutConfig(
        widthFraction = 0.90f,
        heightFraction = 0.52f,
        alignment = Alignment.BottomStart,
        offsetY = 6.dp,
        trimBlankColumns = true,
        fadeBottomStart = 0.90f
    ),
    R.string.ascii_cod_pose to ArtLayoutConfig(
        widthFraction = 0.92f,
        heightFraction = 0.46f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = false,
        fadeTopEnd = 0.07f,
        fadeBottomStart = 0.93f,
        fadeHorizontal = 0.03f
    ),
    R.string.ascii_cod_pose_2 to ArtLayoutConfig(
        widthFraction = 0.92f,
        heightFraction = 0.38f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = false,
        fadeTopEnd = 0.12f,
        fadeBottomStart = 0.90f,
        fadeHorizontal = 0.06f
    ),
    R.string.ascii_cod_pose_3 to ArtLayoutConfig(
        widthFraction = 0.92f,
        heightFraction = 0.44f,
        alignment = Alignment.BottomCenter,
        offsetY = 8.dp,
        trimBlankColumns = false,
        fadeTopEnd = 0.12f,
        fadeBottomStart = 0.90f,
        fadeHorizontal = 0.06f
    )
)

private fun trimHorizontalBlankColumns(text: String): String {
    val lines = text.split('\n')
    var minCol = Int.MAX_VALUE
    var maxCol = -1
    for (line in lines) {
        for (i in line.indices) {
            val ch = line[i]
            if (ch != ' ' && ch != '\u2800') {
                if (i < minCol) minCol = i
                if (i > maxCol) maxCol = i
            }
        }
    }
    if (maxCol < minCol) return text
    return lines.joinToString("\n") { line ->
        if (line.length <= minCol) ""
        else line.substring(minCol, (maxCol + 1).coerceAtMost(line.length))
    }
}

val SETTINGS_BACKGROUND_ART_OPTIONS = listOf(
    BackgroundArtOption(R.string.ascii_god, "Creation"),
    BackgroundArtOption(R.string.ascii_miku, "Miku"),
    BackgroundArtOption(R.string.ascii_blackhole, "Black Hole"),
    BackgroundArtOption(R.string.ascii_pulp_fiction_dance, "Pulp Fiction Dance"),
    BackgroundArtOption(R.string.ascii_kill_bill, "Kill Bill"),
    BackgroundArtOption(R.string.ascii_se7en, "Se7en"),
    BackgroundArtOption(R.string.ascii_the_thing, "The Thing"),
    BackgroundArtOption(R.string.ascii_spiderman_3, "Spider-Man 3"),
    BackgroundArtOption(R.string.ascii_dead_space, "Dead Space"),
    BackgroundArtOption(R.string.ascii_assassins_creed, "Assassin's Creed"),
    BackgroundArtOption(R.string.ascii_kevin_hart_cat, "Kevin Hart Cat"),
    BackgroundArtOption(R.string.ascii_nokias_bond, "Nokia's Bond"),
    BackgroundArtOption(R.string.ascii_scarface, "Scarface"),
    BackgroundArtOption(R.string.ascii_akira, "Akira"),
    BackgroundArtOption(R.string.ascii_niko_bellic, "Niko Bellic"),
    BackgroundArtOption(R.string.ascii_superman, "Superman"),
    BackgroundArtOption(R.string.ascii_john_marston, "John Marston"),
    BackgroundArtOption(R.string.ascii_arthur_morgan, "Arthur Morgan"),
    BackgroundArtOption(R.string.ascii_cod_pose, "COD Pose"),
    BackgroundArtOption(R.string.ascii_cod_pose_2, "COD Pose 2"),
    BackgroundArtOption(R.string.ascii_cod_pose_3, "COD Pose 3")
)

object SessionBackgroundArt {
    private var cachedArtResId: Int? = null
    var manualOverrideResId by mutableStateOf<Int?>(null)

    fun isFullBleedTop(context: Context): Boolean {
        val resId = manualOverrideResId ?: getForSession(context)
        return NEW_ART_LAYOUT_OVERRIDES[resId]?.fullBleedTop == true
    }

    fun getForSession(context: Context): Int {
        manualOverrideResId?.let { return it }
        cachedArtResId?.let { return it }
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastIndex = prefs.getInt("last_settings_bg_art_index", -1)
        val candidates = SETTINGS_BACKGROUND_ART_OPTIONS.indices.filter { it != lastIndex }
        val chosenIndex = candidates.random()
        prefs.edit().putInt("last_settings_bg_art_index", chosenIndex).apply()
        val resId = SETTINGS_BACKGROUND_ART_OPTIONS[chosenIndex].resId
        cachedArtResId = resId
        return resId
    }
}

/**
 * Displays a session-randomized Braille ASCII background art anchored in the lower area
 * between the last Settings item and the bottom bar, with soft alpha-masked fades on the art
 * itself so there are no sharp horizontal cutoffs and Settings text remains easy to read.
 */
@Composable
fun SettingsBackgroundArt(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overrideResId = SessionBackgroundArt.manualOverrideResId
    val artResId = overrideResId ?: remember(context) { SessionBackgroundArt.getForSession(context) }
    val rawArtText = stringResource(id = artResId).trimStart('\n').trimEnd('\n', ' ')
    val layout = NEW_ART_LAYOUT_OVERRIDES[artResId] ?: ArtLayoutConfig()
    val displayText = remember(rawArtText, layout.trimBlankColumns, layout.repeatFirstLineCount) {
        val extended = if (layout.repeatFirstLineCount > 0) {
            val firstLine = rawArtText.substringBefore('\n')
            val prefix = buildString {
                repeat(layout.repeatFirstLineCount) {
                    append(firstLine)
                    append('\n')
                }
            }
            prefix + rawArtText
        } else {
            rawArtText
        }
        if (layout.trimBlankColumns) trimHorizontalBlankColumns(extended) else extended
    }

    val isDark = ThemeState.isThemeDark
    val bgLum = TopBarBackground.calculateLuminance()

    val artColor = when {
        isDark && bgLum < 0.16f -> lerp(TextSecondary, Blue500, 0.22f).copy(alpha = 0.22f)
        isDark -> TextPrimary.copy(alpha = 0.25f)
        else -> TextPrimary.copy(alpha = 0.22f)
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = layout.alignment
    ) {
        val sizing = remember(displayText, constraints.maxWidth, constraints.maxHeight, layout.widthFraction, layout.heightFraction, density) {
            if (layout.widthFraction <= 0f || constraints.maxWidth <= 0) {
                Triple(9.5.sp, 8.2.sp, 1f)
            } else {
                val referenceSp = 10f
                val measured = textMeasurer.measure(
                    text = displayText,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = referenceSp.sp
                    ),
                    softWrap = false,
                    maxLines = Int.MAX_VALUE
                )
                val measuredWidthPx = measured.size.width.toFloat().coerceAtLeast(1f)
                val targetWidthPx = constraints.maxWidth * layout.widthFraction
                val computedSp = (referenceSp * (targetWidthPx / measuredWidthPx)).coerceIn(3.5f, 9.5f)

                if (layout.heightFraction > 0f && constraints.maxHeight > 0) {
                    val lineCount = (displayText.count { it == '\n' } + 1).coerceAtLeast(1)
                    val targetHeightPx = constraints.maxHeight * layout.heightFraction
                    val desiredLineHeightSp = with(density) { (targetHeightPx / lineCount).toSp() }.value
                    val maxDenseLineHeightSp = computedSp * 1.32f
                    if (desiredLineHeightSp <= maxDenseLineHeightSp) {
                        Triple(computedSp.sp, desiredLineHeightSp.coerceAtLeast(computedSp * 0.9f).sp, 1f)
                    } else {
                        val scaleY = (desiredLineHeightSp / maxDenseLineHeightSp).coerceIn(1f, 1.8f)
                        Triple(computedSp.sp, maxDenseLineHeightSp.sp, scaleY)
                    }
                } else {
                    Triple(computedSp.sp, (computedSp * (8.2f / 9.5f)).sp, 1f)
                }
            }
        }
        val fontSize = sizing.first
        val lineHeight = sizing.second
        val extraScaleY = sizing.third

        val horizontalWrapAlign = when (layout.alignment) {
            Alignment.BottomEnd -> Alignment.End
            Alignment.BottomStart -> Alignment.Start
            else -> Alignment.CenterHorizontally
        }

        val textStyle = if (layout.heightFraction > 0f) {
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                lineHeight = lineHeight,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        } else {
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                lineHeight = lineHeight
            )
        }

        Text(
            text = displayText,
            style = textStyle,
            color = artColor,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .wrapContentWidth(align = horizontalWrapAlign, unbounded = true)
                .wrapContentHeight(align = Alignment.Bottom, unbounded = true)
                .offset(y = layout.offsetY)
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen,
                    scaleY = extraScaleY,
                    transformOrigin = TransformOrigin(
                        pivotFractionX = when (layout.alignment) {
                            Alignment.BottomEnd -> 1f
                            Alignment.BottomStart -> 0f
                            else -> 0.5f
                        },
                        pivotFractionY = 1f
                    )
                )
                .drawWithContent {
                    drawContent()
                    val verticalBrush = if (layout.fadeTopEnd > 0f) {
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            layout.fadeTopEnd to Color.Black,
                            layout.fadeBottomStart to Color.Black,
                            1.0f to Color.Transparent
                        )
                    } else {
                        Brush.verticalGradient(
                            0.0f to Color.Black,
                            layout.fadeBottomStart to Color.Black,
                            1.0f to Color.Transparent
                        )
                    }
                    drawRect(
                        brush = verticalBrush,
                        blendMode = BlendMode.DstIn
                    )
                    if (layout.fadeHorizontal > 0f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                layout.fadeHorizontal to Color.Black,
                                (1f - layout.fadeHorizontal) to Color.Black,
                                1.0f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
        )
    }
}
