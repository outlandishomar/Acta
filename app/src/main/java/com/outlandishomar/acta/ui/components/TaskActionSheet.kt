package com.outlandishomar.acta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.ui.theme.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset


enum class DeleteType { INSTANCE, FUTURE, ALL }
enum class EditType { INSTANCE, FUTURE, ALL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskActionSheet(
    task: Task,
    subTasks: List<SubTask>,
    onDismiss: () -> Unit,
    onDelete: (DeleteType) -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSubTask: (SubTask) -> Unit = {},
    onEdit: () -> Unit,
    onDuplicate: (LocalDate?) -> Unit = {},
    onToggleHighlight: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isNotesExpanded by remember { mutableStateOf(false) }
    val (plainNotesText, notesSpans) = remember(task.description) {
        RichTextHelper.parseToRichSpans(task.description)
    }
    val formattedNotes = remember(plainNotesText, notesSpans) {
        RichTextHelper.buildVisualAnnotatedString(plainNotesText, notesSpans)
    }
    var hasMoreThan10Lines by remember(task.description) {
        mutableStateOf(plainNotesText.count { it == '\n' } >= 10)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextPrimary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Top Bar: Title on the left, Actions (Duplicate, Edit) on the right in a single Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Top Right Actions: Duplicate on the left of Edit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isDarkBackground = ThemeState.isThemeDark || White.calculateLuminance() <= 0.5f
                    val duplicateIconRes = if (isDarkBackground) {
                        R.drawable.duplicatewhite
                    } else {
                        R.drawable.duplicateblack
                    }

                    // Duplicate Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = { showDuplicateDialog = true })
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Duplicate",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = duplicateIconRes),
                            contentDescription = "Duplicate Task",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Edit Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEdit)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Edit",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Task",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Description / Notes
            if (plainNotesText.isNotBlank()) {
                Text(
                    text = formattedNotes,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp,
                    maxLines = if (isNotesExpanded) Int.MAX_VALUE else 10,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (!isNotesExpanded && textLayoutResult.hasVisualOverflow) {
                            hasMoreThan10Lines = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasMoreThan10Lines) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isNotesExpanded = !isNotesExpanded }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(4.5.dp)
                                        .clip(CircleShape)
                                        .background(TextSecondary)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Divider between Notes and Subtasks
            if (plainNotesText.isNotBlank() && subTasks.isNotEmpty()) {
                HorizontalDivider(
                    color = Divider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Subtasks
            if (subTasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 4.dp)
                ) {
                    subTasks.forEach { subTask ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleSubTask(subTask) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Box(modifier = Modifier.padding(top = 2.dp)) {
                                SmallCheckCircle(
                                    checked = subTask.isCompleted,
                                    onCheckedChange = { onToggleSubTask(subTask) }
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = subTask.title,
                                fontSize = 15.sp,
                                color = if (subTask.isCompleted) TextDisabled else TextSecondary,
                                textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            // Move to Unscheduled / Scheduled Tasks Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (ThemeState.isThemeDark) Grey100.copy(alpha = 0.12f) else Grey50)
                    .clickable {
                        onToggleHighlight()
                        onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (task.isHighlighted) Icons.Filled.DateRange else Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Blue500,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (task.isHighlighted) "Move to Day Schedule" else "Move to Unscheduled",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        fontFamily = SfProText
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bottom Bar: Delete (Left), Complete/Incomplete (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete Button (Bottom Left, Crimson Red)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (task.repeatMode != "ONCE") {
                                showDeleteDialog = true
                            } else {
                                onDelete(DeleteType.ALL)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE63946),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Delete",
                        color = Color(0xFFE63946),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Complete / Incomplete Button (Bottom Right)
                val completeIcon = if (task.isCompleted) Icons.Filled.Close else Icons.Filled.Check
                val completeLabel = if (task.isCompleted) "Incomplete" else "Complete"
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleComplete)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = completeIcon,
                        contentDescription = completeLabel,
                        tint = Blue500,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = completeLabel,
                        color = Blue500,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        BasicAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Surface(
                color = White, // Theme-aware surface color
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) {
                    Text(
                        text = "Delete Task",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Are you sure you want to delete this task?\nThis is a repeating task.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    // Options
                    val crimsonRed = Color(0xFFE63946)
                    
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(DeleteType.INSTANCE)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Delete this task only", color = crimsonRed, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(DeleteType.FUTURE)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Delete all future tasks", color = crimsonRed, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(DeleteType.ALL)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Delete all tasks", color = crimsonRed, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showDuplicateDialog) {
        BasicAlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Surface(
                color = White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) {
                    Text(
                        text = "Duplicate Task",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Choose where you want to duplicate this task.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            onDuplicate(null)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            text = if (task.date == 0L) "Duplicate in Inbox" else "Duplicate for today",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            showDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            text = "Choose another day",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { showDuplicateDialog = false },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val currentYear = LocalDate.now().year
        val currentLocalDate = if (task.date != 0L) {
            Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            LocalDate.now()
        }
        val initialUtcMillis = currentLocalDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            yearRange = (currentYear - 10)..(currentYear + 10)
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDuplicate(pickedDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = Blue500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = White
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = White,
                    titleContentColor = Blue500,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    navigationContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = Blue500,
                    selectedYearContainerColor = Blue500,
                    selectedYearContentColor = White,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = Blue500,
                    selectedDayContentColor = White,
                    todayContentColor = Blue500,
                    todayDateBorderColor = Blue500,
                    dividerColor = Divider
                )
            )
        }
    }
}
