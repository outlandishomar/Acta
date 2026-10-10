package com.outlandishomar.acta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material3.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.ui.theme.*
import androidx.compose.ui.res.painterResource
import com.outlandishomar.acta.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onMoveToInbox: ((Task) -> Unit)? = null,
    subTasks: List<SubTask> = emptyList(),
    onToggleSubTask: (SubTask) -> Unit = {},
    onTaskClick: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    TaskItemContent(
        task = task,
        onToggleComplete = onToggleComplete,
        subTasks = subTasks,
        onToggleSubTask = onToggleSubTask,
        onTaskClick = onTaskClick,
        modifier = modifier
    )
}

@Composable
private fun TaskItemContent(
    task: Task,
    onToggleComplete: (Task) -> Unit,
    subTasks: List<SubTask>,
    onToggleSubTask: (SubTask) -> Unit,
    onTaskClick: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated strikethrough progress
    val strikeProgress by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "strikethrough"
    )
    
    // Premium sinking effect when task is completed
    val cardAlpha by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.65f else 1f,
        animationSpec = tween(300),
        label = "cardAlpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) TextDisabled else TextPrimary,
        animationSpec = tween(200),
        label = "taskTextColor"
    )

    val (plainNotesText, notesSpans) = remember(task.description) {
        RichTextHelper.parseToRichSpans(task.description)
    }
    val formattedNotes = remember(plainNotesText, notesSpans) {
        RichTextHelper.buildVisualAnnotatedString(plainNotesText, notesSpans)
    }

    var showIconOnly by remember(task.id, task.description) {
        mutableStateOf(plainNotesText.count { it == '\n' } >= 3)
    }

    var mainRowHeightPx by remember { mutableFloatStateOf(0f) }
    var subTaskYCenters by remember(task.id, subTasks.size) { mutableStateOf(mapOf<Int, Float>()) }
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var titleLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    var isExpanded by remember(task.id, task.date) { mutableStateOf(!task.isCompleted) }

    // Automatically collapse subtasks when task is checked, and expand when unchecked
    LaunchedEffect(task.isCompleted) {
        isExpanded = !task.isCompleted
    }

    Surface(
        color = White,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha
                scaleX = cardScale
                scaleY = cardScale
            }
            .appleClickable { onTaskClick(task) }
    ) {
        Column {
            val hasNotes = plainNotesText.isNotBlank()
            Row(
                verticalAlignment = if (hasNotes) Alignment.Top else Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        mainRowHeightPx = size.height.toFloat()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Blue checkbox circle aligned to top to look neat with multiline notes
                Box(modifier = if (hasNotes) Modifier.padding(top = 2.dp) else Modifier) {
                    CheckCircle(
                        checked = task.isCompleted,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onToggleComplete(task) 
                        }
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Title with animated strikethrough
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { titleLayoutResult = it },
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .drawWithContent {
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
                        if (task.taskTimeHour != null && task.taskTimeMinute != null) {
                            Spacer(Modifier.width(8.dp))
                            val displayHour = if (task.taskTimeHour % 12 == 0) 12 else task.taskTimeHour % 12
                            val amPm = if (task.taskTimeHour < 12) "AM" else "PM"
                            Text(
                                text = String.format("%d:%02d %s", displayHour, task.taskTimeMinute, amPm),
                                fontSize = 12.sp,
                                color = TextSecondary.copy(alpha = 0.7f),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            val hasAnyAlert = task.alertEnabled || com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts).any { it.isEnabled }
                            if (hasAnyAlert) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Alert enabled",
                                    tint = TextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    if (hasNotes) {
                        Spacer(Modifier.height(4.dp))
                        if (showIconOnly) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Subject,
                                contentDescription = "Notes",
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = formattedNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                maxLines = 4,
                                onTextLayout = { textLayoutResult ->
                                    if (textLayoutResult.lineCount > 3) {
                                        showIconOnly = true
                                    }
                                }
                            )
                        }
                    }
                }

                // Repeat icon for recurring tasks
                if (task.repeatMode != "ONCE") {
                    Icon(
                        painter = painterResource(id = R.drawable.repeaticon),
                        contentDescription = "Recurring task",
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = if (hasNotes) 2.dp else 0.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }

                if (subTasks.isNotEmpty()) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp).padding(top = if (hasNotes) 2.dp else 0.dp)
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            animationSpec = tween(300),
                            label = "arrowRotation"
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            modifier = Modifier.rotate(rotation),
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Sub-tasks
            if (subTasks.isNotEmpty()) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val isDark = ThemeState.isThemeDark
                    val connectorColor = if (isDark) TextSecondary else TextPrimary
                    Column(
                        modifier = Modifier.drawBehind {
                            val lineX = 16.dp.toPx() + 12.dp.toPx()
                            val thresholdPx = 40.dp.toPx()
                            val defaultStartPx = -16.dp.toPx()
                            val lineStartY = if (mainRowHeightPx > thresholdPx) -(mainRowHeightPx - thresholdPx) else defaultStartPx
                            val radius = 10.dp.toPx()

                            val getMidY = { idx: Int ->
                                subTaskYCenters[idx] ?: (idx * 40.dp.toPx() + 20.dp.toPx())
                            }

                            val lastMidY = getMidY(subTasks.lastIndex)
                            
                            val path = androidx.compose.ui.graphics.Path().apply {
                                // Draw the main vertical line down to the last item's curve start
                                moveTo(lineX, lineStartY)
                                lineTo(lineX, lastMidY - radius)
                                
                                // Draw branches
                                subTasks.forEachIndexed { index, _ ->
                                    val midY = getMidY(index)
                                    
                                    moveTo(lineX, midY - radius)
                                    quadraticTo(lineX, midY, lineX + radius, midY)
                                    lineTo(lineX + 14.dp.toPx(), midY)
                                }
                            }
                            
                            drawPath(
                                path = path,
                                color = connectorColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    ) {
                        subTasks.forEachIndexed { index, subTask ->
                            SubTaskRow(
                                subTask = subTask,
                                isLast = index == subTasks.lastIndex,
                                onToggle = { onToggleSubTask(subTask) },
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val yInParent = coordinates.positionInParent().y
                                    val centerY = yInParent + with(density) { 16.dp.toPx() }
                                    if (subTaskYCenters[index] != centerY) {
                                        subTaskYCenters = subTaskYCenters.toMutableMap().apply { put(index, centerY) }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom divider
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp, end = 16.dp),
                color = Divider,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun SubTaskRow(
    subTask: SubTask,
    isLast: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (subTask.isCompleted) TextDisabled else TextPrimary,
        animationSpec = tween(200),
        label = "subTextColor"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val strikeProgress by animateFloatAsState(
        targetValue = if (subTask.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "subStrikethrough"
    )
    var titleLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .heightIn(min = 36.dp)
    ) {
        // Space reserved for the horizontal branch connector (now drawn by parent)
        Spacer(modifier = Modifier.width(26.dp))

        // Small check circle for sub-task
        Box(modifier = Modifier.padding(top = 2.dp)) {
            SmallCheckCircle(
                checked = subTask.isCompleted,
                onCheckedChange = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onToggle()
                }
            )
        }

        Spacer(Modifier.width(10.dp))

        // Sub-task title
        Text(
            text = subTask.title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = textColor,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { titleLayoutResult = it },
            modifier = Modifier
                .weight(1f)
                .drawWithContent {
                    drawContent()
                    if (strikeProgress > 0f && titleLayoutResult != null) {
                        val layout = titleLayoutResult!!
                        val strokeWidth = 1.25.dp.toPx()
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

@Composable
fun SmallCheckCircle(
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    val isDark = ThemeState.isThemeDark
    val uncheckedBorder = if (isDark) TextSecondary else TextPrimary

    val borderColor by animateColorAsState(
        targetValue = if (checked) Blue500 else uncheckedBorder,
        animationSpec = tween(200),
        label = "smallCheckBorder"
    )
    val fillColor by animateColorAsState(
        targetValue = if (checked) Blue500 else Color.Transparent,
        animationSpec = tween(200),
        label = "smallCheckFill"
    )
    val checkmarkColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E)

    Surface(
        shape = RoundedCornerShape(5.dp),
        color = fillColor,
        border = androidx.compose.foundation.BorderStroke(1.25.dp, borderColor),
        onClick = onCheckedChange,
        modifier = Modifier.size(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = checkmarkColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckCircle(
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    val isDark = ThemeState.isThemeDark
    val uncheckedBorder = if (isDark) TextSecondary else TextPrimary

    val borderColor by animateColorAsState(
        targetValue = if (checked) Blue500 else uncheckedBorder,
        animationSpec = tween(200),
        label = "checkBorder"
    )
    val fillColor by animateColorAsState(
        targetValue = if (checked) Blue500 else Color.Transparent,
        animationSpec = tween(200),
        label = "checkFill"
    )
    val checkmarkColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E)

    Surface(
        shape = RoundedCornerShape(7.dp),
        color = fillColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        onClick = onCheckedChange,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = checkmarkColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun Modifier.appleClickable(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1f,
        animationSpec = tween(150),
        label = "pressAlpha"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

