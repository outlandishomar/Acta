package com.outlandishomar.acta.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.ui.components.AddTaskDialog
import com.outlandishomar.acta.ui.components.TaskActionSheet
import com.outlandishomar.acta.ui.components.TaskItem
import com.outlandishomar.acta.ui.theme.*
import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import java.time.ZoneId

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: TaskViewModel,
    onSettingsClick: () -> Unit,
    onAddInboxTaskClick: () -> Unit = {}
) {
    val inboxTasks by viewModel.inboxTasks.collectAsStateWithLifecycle()
    val inboxSubTasksMap by viewModel.inboxSubTasksMap.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    
    var taskForActionSheet by remember { mutableStateOf<Task?>(null) }
    var taskForEditDialog by remember { mutableStateOf<Task?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var localTasks by remember(inboxTasks) { mutableStateOf(inboxTasks) }
    val currentLocalTasks by rememberUpdatedState(localTasks)
    val displayTasks = localTasks.filter { task ->
        !com.outlandishomar.acta.data.shouldHideTask(task = task, isInboxTask = true)
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    var isInitialLoad by remember { mutableStateOf(true) }
    var previousTaskIds by remember { mutableStateOf(localTasks.map { it.id }.toSet()) }

    LaunchedEffect(localTasks) {
        if (localTasks.isNotEmpty()) {
            if (isInitialLoad) {
                isInitialLoad = false
                previousTaskIds = localTasks.map { it.id }.toSet()
            } else if (localTasks.size > previousTaskIds.size) {
                val newIndex = displayTasks.indexOfFirst { !previousTaskIds.contains(it.id) }.takeIf { it != -1 } ?: (displayTasks.size - 1)
                val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val firstVisible = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0

                if (newIndex > lastVisible) {
                    val itemsToScroll = newIndex - lastVisible
                    val avgHeight = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 180f
                    val scrollDistance = itemsToScroll * avgHeight + 250f

                    lazyListState.animateScrollBy(
                        value = scrollDistance,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                        )
                    )
                } else if (newIndex < firstVisible) {
                    lazyListState.animateScrollToItem(newIndex)
                }
                previousTaskIds = localTasks.map { it.id }.toSet()
            } else {
                previousTaskIds = localTasks.map { it.id }.toSet()
            }
        }
    }
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localTasks = localTasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    val isOverlayVisible = taskForActionSheet != null || taskForEditDialog != null
    val blurModifier = if (isOverlayVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = White,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TopBarBackground)
                ) {
                    TaskTopBar(
                        selectedDate = selectedDate,
                        onCalendarClick = { showDatePicker = true },
                        onSettingsClick = onSettingsClick
                    )
                }
            },
            modifier = blurModifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Section header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "INBOX",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${displayTasks.size} tasks",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)

                if (displayTasks.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Inbox,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Inbox is empty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                fontFamily = SfProText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Capture your thoughts and tasks as they come",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextSecondary.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                fontFamily = SfProText
                            )
                            Spacer(Modifier.height(20.dp))
                            Surface(
                                onClick = onAddInboxTaskClick,
                                shape = RoundedCornerShape(12.dp),
                                color = TextPrimary.copy(alpha = 0.08f),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Add an inbox task",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        fontFamily = SfProText
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(displayTasks, key = { it.id }) { task ->
                            ReorderableItem(reorderState, key = task.id) { isDragging ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            placementSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                        .longPressDraggableHandle(
                                            onDragStopped = { viewModel.reorderTasks(currentLocalTasks) }
                                        )
                                ) {
                                    TaskItem(
                                        task = task,
                                        onToggleComplete = viewModel::toggleComplete,
                                        onDelete = viewModel::deleteTask,
                                        subTasks = inboxSubTasksMap[task.id] ?: emptyList(),
                                        onToggleSubTask = viewModel::toggleSubTaskComplete,
                                        onTaskClick = { taskForActionSheet = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scrim for blur fallback on < API 31
        if (isOverlayVisible && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }

    taskForActionSheet?.let { task ->
        TaskActionSheet(
            task = task,
            subTasks = inboxSubTasksMap[task.id] ?: emptyList(),
            onDismiss = { taskForActionSheet = null },
            onDelete = {
                viewModel.deleteTask(task)
                taskForActionSheet = null
            },
            onToggleComplete = {
                viewModel.toggleComplete(task)
                taskForActionSheet = null
            },
            onToggleSubTask = { subTask ->
                viewModel.toggleSubTaskComplete(subTask)
            },
            onEdit = {
                taskForEditDialog = task
                taskForActionSheet = null
            },
            onDuplicate = { targetDate ->
                viewModel.duplicateTask(task, inboxSubTasksMap[task.id] ?: emptyList(), targetDate)
                taskForActionSheet = null
            }
        )
    }

    taskForEditDialog?.let { task ->
        val existingSubTasks = inboxSubTasksMap[task.id]?.map { it.title } ?: emptyList()
        AddTaskDialog(
            initialTitle = task.title,
            initialNotes = task.description,
            initialSubTasks = existingSubTasks,
            initialIconName = task.iconName,
            isHighlighted = task.isHighlighted,
            isEditMode = true,
            initialTimeHour = task.taskTimeHour,
            initialTimeMinute = task.taskTimeMinute,
            initialRepeatMode = task.repeatMode,
            initialRepeatInterval = task.repeatInterval,
            initialRepeatDays = task.repeatDays,
            initialStartDate = java.time.Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate(),
            initialEndDate = task.endDate?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
            initialAlertEnabled = task.alertEnabled,
            initialAdditionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts),
            enableAdvanced = false,
            onDismiss = { taskForEditDialog = null },
            onAdd = { title, notes, subTasksList, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, editType ->
                viewModel.updateTaskAndSubTasks(task, title, notes, subTasksList, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, editType)
                taskForEditDialog = null
            }
        )
    }

    // --- Material 3 Date Picker Dialog ---
    if (showDatePicker) {
        val currentYear = LocalDate.now().year
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
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
                        viewModel.selectDate(pickedDate)
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
