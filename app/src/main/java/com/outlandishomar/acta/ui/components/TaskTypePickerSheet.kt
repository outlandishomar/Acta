package com.outlandishomar.acta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.ui.theme.*

private val BlueBg = Color(0xFFE3F2FD)
private val BlueIcon = Color(0xFF2979FF)
private val PinkBg = Color(0xFFFCE4EC)
private val PinkIcon = Color(0xFFE57373)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTypePickerSheet(
    onDismiss: () -> Unit,
    onRegularTask: () -> Unit,
    onHighlightedTask: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = Divider,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Text(
                text = "ADD A TASK",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Regular Task option
            TaskTypeOption(
                iconBg = BlueBg,
                iconTint = BlueIcon,
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                title = "Regular Task",
                subtitle = "Added to your daily list",
                onClick = {
                    onDismiss()
                    onRegularTask()
                }
            )

            Spacer(Modifier.height(8.dp))

            // Highlighted Task option
            TaskTypeOption(
                iconBg = PinkBg,
                iconTint = PinkIcon,
                icon = Icons.Filled.StarOutline,
                title = "Highlighted Task",
                subtitle = "Pinned to the top row",
                onClick = {
                    onDismiss()
                    onHighlightedTask()
                }
            )
        }
    }
}

@Composable
private fun TaskTypeOption(
    iconBg: Color,
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        // Colored icon background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
        }

        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Divider,
            modifier = Modifier.size(24.dp)
        )
    }
}
