package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.saveHideTasksSettings
import com.outlandishomar.acta.ui.theme.*
import com.outlandishomar.acta.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideTasksSettingsScreen(viewModel: TaskViewModel, onBackClick: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Hide Tasks", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centered Description Text at Top Center of page
            Text(
                text = "Hide your tasks from the application view whenever you want to demonstrate or present the app.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Toggle 1: Hide All Tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide All Tasks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hide regular, today, and inbox tasks",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = AppSettings.isHideAllTasksEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            saveHideTasksSettings(context, hideAll = true, hideRegular = true, hideToday = true, hideInbox = true)
                        } else {
                            saveHideTasksSettings(context, hideAll = false, hideRegular = false, hideToday = false, hideInbox = false)
                            viewModel.cleanupHideModeTasks()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Blue500,
                        uncheckedThumbColor = White,
                        uncheckedTrackColor = TextSecondary.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

            // Toggle 2: Hide Regular Tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Regular Tasks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hide calendar and daily tasks, keeping inbox intact",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = AppSettings.isHideRegularTasksEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val newHideAll = AppSettings.isHideInboxTasksEnabled && AppSettings.isHideTodayTasksEnabled
                            saveHideTasksSettings(
                                context,
                                hideAll = newHideAll,
                                hideRegular = true,
                                hideToday = AppSettings.isHideTodayTasksEnabled,
                                hideInbox = AppSettings.isHideInboxTasksEnabled
                            )
                        } else {
                            saveHideTasksSettings(
                                context,
                                hideAll = false,
                                hideRegular = false,
                                hideToday = AppSettings.isHideTodayTasksEnabled,
                                hideInbox = AppSettings.isHideInboxTasksEnabled
                            )
                            viewModel.cleanupHideModeTasks()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Blue500,
                        uncheckedThumbColor = White,
                        uncheckedTrackColor = TextSecondary.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

            // Toggle 3: Hide Today's Tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Today's Tasks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hide tasks for today only, keeping other days intact",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = AppSettings.isHideTodayTasksEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val newHideAll = AppSettings.isHideRegularTasksEnabled && AppSettings.isHideInboxTasksEnabled
                            saveHideTasksSettings(
                                context,
                                hideAll = newHideAll,
                                hideRegular = AppSettings.isHideRegularTasksEnabled,
                                hideToday = true,
                                hideInbox = AppSettings.isHideInboxTasksEnabled
                            )
                        } else {
                            saveHideTasksSettings(
                                context,
                                hideAll = false,
                                hideRegular = AppSettings.isHideRegularTasksEnabled,
                                hideToday = false,
                                hideInbox = AppSettings.isHideInboxTasksEnabled
                            )
                            viewModel.cleanupHideModeTasks()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Blue500,
                        uncheckedThumbColor = White,
                        uncheckedTrackColor = TextSecondary.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

            // Toggle 4: Hide Inbox Tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Inbox Tasks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hide inbox items, keeping regular tasks intact",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = AppSettings.isHideInboxTasksEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val newHideAll = AppSettings.isHideRegularTasksEnabled && AppSettings.isHideTodayTasksEnabled
                            saveHideTasksSettings(
                                context,
                                hideAll = newHideAll,
                                hideRegular = AppSettings.isHideRegularTasksEnabled,
                                hideToday = AppSettings.isHideTodayTasksEnabled,
                                hideInbox = true
                            )
                        } else {
                            saveHideTasksSettings(
                                context,
                                hideAll = false,
                                hideRegular = AppSettings.isHideRegularTasksEnabled,
                                hideToday = AppSettings.isHideTodayTasksEnabled,
                                hideInbox = false
                            )
                            viewModel.cleanupHideModeTasks()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Blue500,
                        uncheckedThumbColor = White,
                        uncheckedTrackColor = TextSecondary.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}
