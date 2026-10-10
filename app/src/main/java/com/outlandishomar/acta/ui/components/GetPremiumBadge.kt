package com.outlandishomar.acta.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.ui.theme.SfProText

enum class PremiumBadgeVariant {
    MetallicGold,    // Style 1: Subtasks in AddTaskDialog (Chic Metallic Gold Gradient with dark text)
    NeonAura,        // Style 2: Add Alert in AddTaskDialog (Glowing Neon Aura Halo background)
    Glassmorphic,    // Style 3: Starting Date in AddTaskDialog (Frosted Glass & Light-Catching Edge)
    ObsidianGold,    // Style 4: Repeat Interval in AddTaskDialog (Minimalist Dark Obsidian with Gold border)
    ChampagneHalo,   // Style 5: Hide Tasks in SettingsScreen (Champagne Gold Surface with Soft Glow)
    DiamondShimmer   // Style 6: App Lock in SettingsScreen (Dark Holographic Metallic Shimmer)
}

@Composable
fun GetPremiumBadge(
    modifier: Modifier = Modifier,
    variant: PremiumBadgeVariant = PremiumBadgeVariant.Glassmorphic,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "premiumScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        when (variant) {
            PremiumBadgeVariant.MetallicGold -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFDF7A), Color(0xFFD49A32))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFF1F1500),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Get Premium",
                        color = Color(0xFF1F1500),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = SfProText,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            PremiumBadgeVariant.NeonAura -> {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF007A).copy(alpha = 0.6f), Color(0xFFFFC107).copy(alpha = 0.6f))
                                ),
                                shape = RoundedCornerShape(100.dp)
                            )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF160F24))
                            .border(
                                BorderStroke(
                                    1.2.dp,
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF007A), Color(0xFFFFC107))
                                    )
                                ),
                                RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Get Premium",
                            color = Color(0xFFFFD766),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProText,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            PremiumBadgeVariant.Glassmorphic -> {
                val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val currentMode = com.outlandishomar.acta.ui.theme.ThemeState.currentThemeMode
                val isDark = when(currentMode) {
                    "DARK" -> true
                    "LIGHT", "ROSY_TAUPE", "SEASHELL", "SILVER", "ASH_GREY" -> false
                    else -> isSystemDark
                }

                val backgroundColor = if (isDark) Color(0x28DFB15B) else Color(0x1FF5C445)
                val strokeBrush = if (isDark) {
                    Brush.linearGradient(listOf(Color(0xFFFFF7E0), Color(0xFFC89838)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFE5A91A), Color(0xFFA67400)))
                }
                val contentColor = if (isDark) Color(0xFFF7D070) else Color(0xFF8E5C00)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(backgroundColor)
                        .border(
                            BorderStroke(1.dp, strokeBrush),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Get Premium",
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProText,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            PremiumBadgeVariant.ObsidianGold -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFF151517))
                        .border(
                            BorderStroke(1.dp, Color(0xFFE5B869).copy(alpha = 0.7f)),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFE5B869),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Get Premium",
                        color = Color(0xFFE5B869),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProText,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            PremiumBadgeVariant.ChampagneHalo -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF5CE7B), Color(0xFFCB932E))
                            )
                        )
                        .border(
                            BorderStroke(1.5.dp, Color(0xFFFFF3C9).copy(alpha = 0.8f)),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFF201502),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Get Premium",
                        color = Color(0xFF201502),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = SfProText
                    )
                }
            }

            PremiumBadgeVariant.DiamondShimmer -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2C223A), Color(0xFF4C3A62), Color(0xFF2C223A))
                            )
                        )
                        .border(
                            BorderStroke(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFE082), Color(0xFFE040FB), Color(0xFFFFE082))
                                )
                            ),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Get Premium",
                        color = Color(0xFFFFE082),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProText
                    )
                }
            }
        }
    }
}
