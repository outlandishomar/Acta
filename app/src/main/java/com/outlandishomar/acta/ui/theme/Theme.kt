package com.outlandishomar.acta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TaskColorScheme = lightColorScheme(
    primary          = Blue500,
    onPrimary        = White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue500,
    secondary        = Blue100,
    onSecondary      = Blue500,
    background       = White,
    onBackground     = TextPrimary,
    surface          = White,
    onSurface        = TextPrimary,
    surfaceVariant   = Grey100,
    onSurfaceVariant = TextSecondary,
    outline          = Divider,
)

private object NoIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.node.DelegatableNode {
        return object : androidx.compose.ui.Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

@Composable
fun ActaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TaskColorScheme,
        typography  = Typography,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.foundation.LocalIndication provides NoIndication
            ) {
                content()
            }
        }
    )
}