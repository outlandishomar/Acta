package com.outlandishomar.acta.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.data.AppIcon
import com.outlandishomar.acta.data.AppIconManager
import com.outlandishomar.acta.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val currentIcon = AppIconManager.currentIcon

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "App Icons",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White
                )
            )
        }
    ) { innerPadding ->
        AppIconsContent(modifier = Modifier.padding(innerPadding))
    }
}

@Composable
fun AppIconsContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentIcon = AppIconManager.currentIcon

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Choose an icon for your home screen. The selected icon will update across your launcher.",
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val icons = AppIcon.entries
        val chunkedIcons = icons.chunked(2)

        chunkedIcons.forEach { rowIcons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowIcons.forEach { iconItem ->
                    val isSelected = currentIcon == iconItem
                    AppIconCard(
                        icon = iconItem,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!isSelected) {
                                AppIconManager.setAppIcon(context, iconItem)
                                Toast.makeText(
                                    context,
                                    "${iconItem.displayName} icon applied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                if (rowIcons.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notice / footnote
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Divider.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Tip: Some Android launchers may take a few seconds to refresh the home screen icon.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AppIconCard(
    icon: AppIcon,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "press_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Blue500 else Divider,
        label = "border_color"
    )

    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Column(
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (ThemeState.isThemeDark) Color(0xFF2C2C2E) else Color(0xFFEBEBEB))
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon squircle preview
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(if (icon.isDarkBackground) Color(0xFF000000) else Color(0xFFFFFFFF))
                .border(
                    width = 0.5.dp,
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon.previewDrawableRes),
                contentDescription = icon.displayName,
                tint = Color.Unspecified,
                modifier = Modifier.requiredSize(114.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = icon.displayName,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Selected indicator
        if (isSelected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Blue500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = if (ThemeState.isThemeDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue500
                )
            }
        } else {
            Text(
                text = "Tap to apply",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
