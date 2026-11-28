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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    onDuplicate: (LocalDate?) -> Unit = {}
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
        containerColor = TopBarBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextPrimary) }
    ) {
        val isDarkBackground = ThemeState.isThemeDark || TopBarBackground.calculateLuminance() <= 0.5f
        val duplicateIconRes = if (isDarkBackground) {
            R.drawable.duplicatewhite
        } else {
            R.drawable.duplicateblack
        }
        val sheetDividerColor = TextPrimary.copy(alpha = if (isDarkBackground) 0.14f else 0.12f)
        val actionCardBg = if (isDarkBackground) {
            lerp(TopBarBackground, Color.White, 0.07f)
        } else {
            lerp(TopBarBackground, Color.Black, 0.06f)
        }
        val actionContentColor = Blue500

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // Main Task Title row with optional time, notification bell, and repeat icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (plainNotesText.isNotBlank()) 6.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (TaskIcons.hasIcon(task.iconName)) {
                        TaskIcon(
                            iconName = task.iconName,
                            tint = Blue500,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = task.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (task.taskTimeHour != null && task.taskTimeMinute != null) {
                        Spacer(Modifier.width(10.dp))
                        val displayHour = if (task.taskTimeHour % 12 == 0) 12 else task.taskTimeHour % 12
                        val amPm = if (task.taskTimeHour < 12) "AM" else "PM"
                        Text(
                            text = String.format("%d:%02d %s", displayHour, task.taskTimeMinute, amPm),
                            fontSize = 13.sp,
                            color = TextSecondary.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium
                        )
                        val hasAnyAlert = task.alertEnabled ||
                                com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts).any { it.isEnabled }
                        if (hasAnyAlert) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Alert enabled",
                                tint = TextSecondary.copy(alpha = 0.75f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                if (task.repeatMode != "ONCE") {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.repeaticon),
                        contentDescription = "Recurring task",
                        tint = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Description / Notes (directly under main task text without a divider between them)
            if (plainNotesText.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
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
                                .padding(top = 4.dp),
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
                    }
                }
            }

            // Visible full-width divider under the main text / notes block
            HorizontalDivider(
                color = sheetDividerColor,
                thickness = 1.dp
            )

            // Subtasks with our rounded checkboxes and Structured-style row dividers
            if (subTasks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    subTasks.forEach { subTask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubTask(subTask) }
                                .padding(vertical = 13.dp, horizontal = 2.dp)
                        ) {
                            SmallCheckCircle(
                                checked = subTask.isCompleted,
                                onCheckedChange = { onToggleSubTask(subTask) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = subTask.title,
                                fontSize = 15.sp,
                                color = if (subTask.isCompleted) TextDisabled else TextPrimary,
                                textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(
                            color = sheetDividerColor,
                            thickness = 1.dp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Structured-style 3 Action Cards Row: Delete | Duplicate | Complete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Delete Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(actionCardBg)
                        .clickable {
                            if (task.repeatMode != "ONCE") {
                                showDeleteDialog = true
                            } else {
                                onDelete(DeleteType.ALL)
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = actionContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Delete",
                        color = actionContentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. Duplicate Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(actionCardBg)
                        .clickable(onClick = { showDuplicateDialog = true })
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = duplicateIconRes),
                        contentDescription = "Duplicate Task",
                        tint = actionContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Duplicate",
                        color = actionContentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Complete / Incomplete Card
                val completeIcon = if (task.isCompleted) Icons.Filled.Close else Icons.Filled.CheckCircle
                val completeLabel = if (task.isCompleted) "Incomplete" else "Complete"

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(actionCardBg)
                        .clickable(onClick = onToggleComplete)
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = completeIcon,
                        contentDescription = completeLabel,
                        tint = actionContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = completeLabel,
                        color = actionContentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Full-width "Edit Task" Card at the bottom (like Structured)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(actionCardBg)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Task",
                        tint = actionContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Edit Task",
                        color = actionContentColor,
                        fontSize = 15.sp,
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
