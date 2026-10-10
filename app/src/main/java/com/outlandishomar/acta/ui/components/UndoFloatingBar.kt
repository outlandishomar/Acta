package com.outlandishomar.acta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.ui.theme.Red500
import com.outlandishomar.acta.ui.theme.SfProText
import com.outlandishomar.acta.ui.theme.ThemeState
import com.outlandishomar.acta.ui.viewmodel.UndoAction

@Composable
fun UndoFloatingBar(
    undoAction: UndoAction?,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = undoAction != null,
        enter = slideInVertically(
            initialOffsetY = { it * 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it * 2 },
            animationSpec = tween(180)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        val isDark = ThemeState.isThemeDark
        val containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFF1E1E1E)
        val borderColor = if (isDark) Color(0xFF3E3E42) else Color(0xFF333333)
        val messageColor = Red500
        val undoTextColor = Color.White

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = containerColor,
            border = BorderStroke(0.5.dp, borderColor),
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.25f)
                )
                .height(44.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 16.dp, end = 6.dp)
            ) {
                Text(
                    text = undoAction?.message ?: "Task deleted",
                    color = messageColor,
                    fontSize = 14.sp,
                    fontFamily = SfProText,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(14.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .appleClickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onUndo()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = undoTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Undo",
                            color = undoTextColor,
                            fontSize = 13.sp,
                            fontFamily = SfProText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
