package com.outlandishomar.acta.ui.components

import android.content.ClipData
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.ui.theme.*

/**
 * Compact Squircle Chip Strip (Single horizontal row, minimal vertical space ~34dp)
 */
@Composable
fun UnscheduledChipsRow(
    tasks: List<Task>,
    onToggleComplete: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddClick: () -> Unit,
    draggingTaskId: Int? = null,
    onDragStart: (Task) -> Unit = {},
    isHovered: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rowBackground by animateColorAsState(
        targetValue = if (isHovered) Blue500.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "unscheduledRowBg"
    )
    val rowBorderColor by animateColorAsState(
        targetValue = if (isHovered) Blue500.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "unscheduledRowBorder"
    )
    val rowScale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "unscheduledRowScale"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .border(1.dp, rowBorderColor, RoundedCornerShape(12.dp))
    ) {
        if (tasks.isEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHovered) Blue500.copy(alpha = 0.15f) else AvatarGrey.copy(alpha = 0.25f))
                        .border(
                            width = if (isHovered) 1.dp else 0.5.dp,
                            color = if (isHovered) Blue500 else Divider.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onAddClick)
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isHovered) Icons.Filled.DateRange else Icons.Filled.Add,
                        contentDescription = "Add Unscheduled Task",
                        tint = if (isHovered) Blue500 else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isHovered) "Drop to make Unscheduled" else "Unscheduled Tasks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isHovered) Blue500 else TextSecondary,
                        fontFamily = SfProText
                    )
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                UnscheduledChipItem(
                    task = task,
                    isDragging = draggingTaskId == task.id,
                    onToggleComplete = { onToggleComplete(task) },
                    onClick = { onTaskClick(task) },
                    onDragStart = { onDragStart(task) }
                )
            }

            item {
                // Quick Add Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.5.dp, Divider.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddClick)
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Unscheduled Task",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontFamily = SfProText
                    )
                }
            }
        }
    }
}

/**
 * Individual Squircle Chip Item with pencil animation, bouncy checkbox, and fluid drag shadow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnscheduledChipItem(
    task: Task,
    isDragging: Boolean = false,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val hasNotes = remember(task.description) {
        RichTextHelper.parseToRichSpans(task.description).first.isNotBlank()
    }

    // Animated strikethrough pencil progress
    val strikeProgress by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "unscheduledStrikethrough"
    )

    // Sinking / opacity effect on completion & translucency while dragging
    val chipAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.35f else if (task.isCompleted) 0.65f else 1f,
        animationSpec = tween(250),
        label = "unscheduledChipAlpha"
    )

    val chipScale by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else if (task.isCompleted) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "unscheduledChipScale"
    )

    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) TextDisabled else TextPrimary,
        animationSpec = tween(200),
        label = "unscheduledTextColor"
    )

    var titleLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(34.dp)
            .graphicsLayer {
                alpha = chipAlpha
                scaleX = chipScale
                scaleY = chipScale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(AvatarGrey.copy(alpha = 0.7f))
            .border(0.5.dp, Divider.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .dragAndDropSource {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart()
                        startTransfer(
                            DragAndDropTransferData(
                                ClipData.newPlainText("taskId", task.id.toString())
                            )
                        )
                    }
                )
            }
            .padding(horizontal = 8.dp)
    ) {
        // Small squircle checkbox with scaleIn & fadeIn checkmark
        SmallCheckCircle(
            checked = task.isCompleted,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleComplete()
            }
        )

        Spacer(Modifier.width(8.dp))

        if (hasNotes) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Subject,
                contentDescription = "Notes",
                tint = TextSecondary.copy(alpha = if (task.isCompleted) 0.4f else 0.7f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
        }

        // Title with dynamic pencil strikethrough drawing
        Text(
            text = task.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = SfProText,
            onTextLayout = { titleLayoutResult = it },
            modifier = Modifier.drawWithContent {
                drawContent()
                if (strikeProgress > 0f && titleLayoutResult != null) {
                    val layout = titleLayoutResult!!
                    val strokeWidth = 1.5.dp.toPx()
                    for (i in 0 until layout.lineCount) {
                        val left = layout.getLineLeft(i)
                        val right = layout.getLineRight(i)
                        val lineWidth = right - left
                        val targetLineWidth = lineWidth * strikeProgress
                        val top = layout.getLineTop(i)
                        val bottom = layout.getLineBottom(i)
                        val y = top + (bottom - top) / 2

                        drawLine(
                            color = textColor,
                            start = Offset(left, y),
                            end = Offset(left + targetLineWidth, y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
        )
    }
}
