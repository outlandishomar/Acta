package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.outlandishomar.acta.ui.components.AddTaskDialog
import com.outlandishomar.acta.ui.components.TaskTypePickerSheet
import com.outlandishomar.acta.ui.components.UndoFloatingBar
import com.outlandishomar.acta.ui.theme.BottomBarBackground
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.SfProText
import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import com.outlandishomar.acta.ui.theme.ThemeState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

private enum class BottomTab(
    val label: String
) {
    Tasks("Tasks"),
    Inbox("Inbox")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(openTaskId: Int = -1, onTaskOpened: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(BottomTab.Tasks) }
    val viewModel: TaskViewModel = viewModel()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showSettingsScreen by remember { mutableStateOf(false) }

    var showTaskAddDialog by remember { mutableStateOf(false) }
    var showTaskAddHighlightedDialog by remember { mutableStateOf(false) }
    var showInboxAdd by remember { mutableStateOf(false) }

    val isAddOverlayVisible = showTaskAddDialog || showTaskAddHighlightedDialog || showInboxAdd
    val blurModifier = if (isAddOverlayVisible && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else Modifier

    val isDark = ThemeState.isThemeDark
    
    val tasksIcon = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.taskswhite else com.outlandishomar.acta.R.drawable.tasksblack)
    val inboxIcon = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.boxwhite else com.outlandishomar.acta.R.drawable.boxblack)

    if (showSettingsScreen) {
        androidx.activity.compose.BackHandler { showSettingsScreen = false }
        SettingsScreen(onBackClick = { showSettingsScreen = false })
    } else {
        Scaffold(
            containerColor = White,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, clip = false)
                        .background(BottomBarBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .height(64.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tasks Tab
                        val isTasksSelected = selectedTab == BottomTab.Tasks
                        val tasksColor = if (isTasksSelected) Blue500 else TextSecondary
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedTab = BottomTab.Tasks },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = tasksIcon,
                                contentDescription = BottomTab.Tasks.label,
                                tint = tasksColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BottomTab.Tasks.label,
                                fontSize = 12.sp,
                                color = tasksColor,
                                fontFamily = SfProText
                            )
                        }

                        // Middle Add Button — plain image, no container
                        Box(
                            modifier = Modifier
                                .offset(y = (-8).dp)
                                .size(56.dp)
                                .combinedClickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    onClick = {
                                        if (selectedTab == BottomTab.Tasks) {
                                            showTaskAddDialog = true
                                        } else {
                                            showInboxAdd = true
                                        }
                                    },
                                    onLongClick = {
                                        if (selectedTab == BottomTab.Tasks) {
                                            showTaskAddHighlightedDialog = true
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add task",
                                tint = Blue500,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Inbox Tab
                        val isInboxSelected = selectedTab == BottomTab.Inbox
                        val inboxColor = if (isInboxSelected) Blue500 else TextSecondary
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedTab = BottomTab.Inbox },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = inboxIcon,
                                contentDescription = BottomTab.Inbox.label,
                                tint = inboxColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BottomTab.Inbox.label,
                                fontSize = 12.sp,
                                color = inboxColor,
                                fontFamily = SfProText
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    BottomTab.Tasks -> HomeScreen(viewModel = viewModel, onSettingsClick = { showSettingsScreen = true }, openTaskId = openTaskId, onTaskOpened = onTaskOpened)
                    BottomTab.Inbox -> InboxScreen(
                        viewModel = viewModel,
                        onSettingsClick = { showSettingsScreen = true },
                        onAddInboxTaskClick = { showInboxAdd = true }
                    )
                }

                val undoAction by viewModel.undoAction.collectAsStateWithLifecycle()
                UndoFloatingBar(
                    undoAction = undoAction,
                    onUndo = viewModel::undoDelete,
                    onDismiss = viewModel::dismissUndo,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    if (showTaskAddDialog) {
        AddTaskDialog(
            isHighlighted = false,
            initialStartDate = selectedDate,
            onDismiss = { showTaskAddDialog = false },
            onAdd = { title, notes, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, _ -> 
                 if (subTasks.isEmpty()) viewModel.addTask(title, notes, false, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 else viewModel.addTaskWithSubTasks(title, notes, false, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 showTaskAddDialog = false
            }
        )
    }
    if (showTaskAddHighlightedDialog) {
        AddTaskDialog(
            isHighlighted = true,
            initialStartDate = selectedDate,
            onDismiss = { showTaskAddHighlightedDialog = false },
            onAdd = { title, notes, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, _ -> 
                 if (subTasks.isEmpty()) viewModel.addTask(title, notes, true, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 else viewModel.addTaskWithSubTasks(title, notes, true, subTasks, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                 showTaskAddHighlightedDialog = false
            }
        )
    }
    if (showInboxAdd) {
        AddTaskDialog(
            isHighlighted = false,
            enableAdvanced = false,
            onDismiss = { showInboxAdd = false },
            onAdd = { title, notes, subTasks, _, _, _, _, _, _, _, _, _, _, _ -> 
                 viewModel.addInboxTask(title, notes, subTasks)
                 showInboxAdd = false
            }
        )
    }
}
