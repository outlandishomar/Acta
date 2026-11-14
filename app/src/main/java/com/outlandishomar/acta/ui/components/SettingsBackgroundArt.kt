package com.outlandishomar.acta.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.R
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.ThemeState
import com.outlandishomar.acta.ui.theme.TopBarBackground
import com.outlandishomar.acta.ui.theme.calculateLuminance

private val SETTINGS_BACKGROUND_ARTS = listOf(
    R.string.ascii_coolstars,
    R.string.ascii_coolflower,
    R.string.ascii_brain,
    R.string.ascii_pill,
    R.string.ascii_sharks,
    R.string.ascii_lotus,
    R.string.ascii_jellyfish,
    R.string.ascii_god,
    R.string.ascii_phone,
    R.string.angel_fallen,
    R.string.ascii_aim,
    R.string.ascii_key,
    R.string.ascii_space,
    R.string.ascii_miku,
    R.string.ascii_flower,
    R.string.ascii_meditate,
    R.string.ascii_star1,
    R.string.ascii_star2,
    R.string.ascii_blackhole,
    R.string.ascii_praying_angel,
    R.string.ascii_kitty,
    R.string.ascii_star3,
    R.string.ascii_star4,
    R.string.ascii_star5
)

private object SessionBackgroundArt {
    private var cachedArtResId: Int? = null

    fun getForSession(context: Context): Int {
        cachedArtResId?.let { return it }
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastIndex = prefs.getInt("last_settings_bg_art_index", -1)
        val candidates = SETTINGS_BACKGROUND_ARTS.indices.filter { it != lastIndex }
        val chosenIndex = candidates.random()
        prefs.edit().putInt("last_settings_bg_art_index", chosenIndex).apply()
        val resId = SETTINGS_BACKGROUND_ARTS[chosenIndex]
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
    val artResId = remember(context) { SessionBackgroundArt.getForSession(context) }
    val artText = stringResource(id = artResId).trim('\n')
    val isDark = ThemeState.isThemeDark
    val bgLum = TopBarBackground.calculateLuminance()

    val artColor = when {
        isDark && bgLum < 0.16f -> lerp(TextSecondary, Blue500, 0.22f).copy(alpha = 0.22f)
        isDark -> TextPrimary.copy(alpha = 0.25f)
        else -> TextPrimary.copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = artText,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            lineHeight = 8.2.sp,
            color = artColor,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .wrapContentWidth(align = Alignment.CenterHorizontally, unbounded = true)
                .offset(y = 22.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.24f to Color.Black,
                            0.78f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        )
    }
}
