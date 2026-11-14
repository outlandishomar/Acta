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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.outlandishomar.acta.data.isTodayUserBirthday
import com.outlandishomar.acta.ui.components.AddTaskDialog
import com.outlandishomar.acta.ui.components.BirthdayConfettiOverlay
import com.outlandishomar.acta.ui.components.TaskTypePickerSheet
import com.outlandishomar.acta.ui.components.UndoFloatingBar
import com.outlandishomar.acta.ui.theme.BottomBarBackground
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.Divider
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.calculateLuminance
import com.outlandishomar.acta.ui.theme.InterText
import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import com.outlandishomar.acta.ui.theme.ThemeState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

private enum class BottomTab(
    val label: String
) {
    Inbox("Inbox"),
    Tasks("Tasks"),
    Settings("Settings")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(openTaskId: Int = -1, onTaskOpened: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(BottomTab.Tasks) }
    val viewModel: TaskViewModel = viewModel()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var isInSettingsSubScreen by remember { mutableStateOf(false) }

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
    val settingsIcon = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.settingswhite else com.outlandishomar.acta.R.drawable.settingsblack)

    var birthdayConfettiKey by remember { mutableIntStateOf(0) }
    var celebratedBirthdayThisSession by remember { mutableStateOf(false) }
    val isBirthdayToday = isTodayUserBirthday()
    LaunchedEffect(isBirthdayToday, isInSettingsSubScreen) {
        if (isBirthdayToday && !celebratedBirthdayThisSession && !isInSettingsSubScreen) {
            birthdayConfettiKey++
            celebratedBirthdayThisSession = true
        }
    }

    androidx.activity.compose.BackHandler(enabled = selectedTab == BottomTab.Settings && !isInSettingsSubScreen) {
        selectedTab = BottomTab.Tasks
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isInSettingsSubScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BottomBarBackground)
                    ) {
                        HorizontalDivider(
                            color = Divider.copy(alpha = 0.28f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .height(64.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavTab(
                                label = BottomTab.Inbox.label,
                                painter = inboxIcon,
                                selected = selectedTab == BottomTab.Inbox,
                                onClick = { selectedTab = BottomTab.Inbox },
                                modifier = Modifier.weight(1f)
                            )

                            BottomNavTab(
                                label = BottomTab.Tasks.label,
                                painter = tasksIcon,
                                selected = selectedTab == BottomTab.Tasks,
                                onClick = { selectedTab = BottomTab.Tasks },
                                modifier = Modifier.weight(1f)
                            )

                            BottomNavTab(
                                label = BottomTab.Settings.label,
                                painter = settingsIcon,
                                selected = selectedTab == BottomTab.Settings,
                                onClick = { selectedTab = BottomTab.Settings },
                                modifier = Modifier.weight(1f)
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
                    BottomTab.Tasks -> HomeScreen(
                        viewModel = viewModel,
                        openTaskId = openTaskId,
                        onTaskOpened = onTaskOpened
                    )
                    BottomTab.Inbox -> InboxScreen(
                        viewModel = viewModel,
                        onAddInboxTaskClick = { showInboxAdd = true }
                    )
                    BottomTab.Settings -> SettingsScreen(
                        onSubScreenChanged = { isInSettingsSubScreen = it },
                        viewModel = viewModel
                    )
                }

                if (selectedTab != BottomTab.Settings) {
                    // Floating circular '+' FAB at bottom-right (respects active tab)
                    val fabContentColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 18.dp)
                            .shadow(8.dp, CircleShape, clip = false)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Blue500)
                            .combinedClickable(
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
                            contentDescription = if (selectedTab == BottomTab.Tasks) "Add task" else "Add inbox task",
                            tint = fabContentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                val undoAction by viewModel.undoAction.collectAsStateWithLifecycle()
                UndoFloatingBar(
                    undoAction = undoAction,
                    onUndo = viewModel::undoDelete,
                    onDismiss = viewModel::dismissUndo,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 24.dp)
                )
            }
        }

        BirthdayConfettiOverlay(
            triggerKey = birthdayConfettiKey,
            onFinished = { birthdayConfettiKey = 0 },
            modifier = if (isInSettingsSubScreen) Modifier.alpha(0f) else Modifier
        )
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

@Composable
private fun BottomNavTab(
    label: String,
    painter: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (selected) Blue500 else TextSecondary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) Blue500.copy(alpha = 0.16f) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = tintColor,
            fontFamily = InterText
        )
    }
}
