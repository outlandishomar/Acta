package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.GridView
import com.outlandishomar.acta.data.AppIconManager
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.saveFirstDayOfWeek
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import androidx.compose.ui.res.painterResource
import com.outlandishomar.acta.R
import com.outlandishomar.acta.ui.theme.*

import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, viewModel: TaskViewModel = viewModel()) {
    var showThemeScreen by remember { mutableStateOf(false) }
    var showAboutAppScreen by remember { mutableStateOf(false) }
    var showStartDayDialog by remember { mutableStateOf(false) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var showLockScreenSettings by remember { mutableStateOf(false) }
    var showHideTasksSettings by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    val currentMode = ThemeState.currentThemeMode
    val themeSubtext = when(currentMode) {
        "DARK" -> "Dark Theme"
        "LIGHT" -> "Light Theme"
        "ROSY_TAUPE" -> "Rosy Taupe"
        "DUST_GREY" -> "Dust Grey"
        "SILVER" -> "Silver"
        "ASH_GREY" -> "Ash Grey"
        else -> "Device Default"
    }

    val isDark = ThemeState.isThemeDark

    if (showAboutAppScreen) {
        androidx.activity.compose.BackHandler { showAboutAppScreen = false }
        AboutAppScreen(onBackClick = { showAboutAppScreen = false })
    } else if (showBackupScreen) {
        androidx.activity.compose.BackHandler { showBackupScreen = false }
        BackupScreen(onBackClick = { showBackupScreen = false })
    } else if (showThemeScreen) {
        androidx.activity.compose.BackHandler { showThemeScreen = false }
        ThemeScreen(
            onBackClick = { showThemeScreen = false }
        )
    } else if (showLockScreenSettings) {
        androidx.activity.compose.BackHandler { showLockScreenSettings = false }
        LockScreenSettingsScreen(
            onBackClick = { showLockScreenSettings = false },
            onBackupClick = {
                showLockScreenSettings = false
                showBackupScreen = true
            }
        )
    } else if (showHideTasksSettings) {
        androidx.activity.compose.BackHandler { showHideTasksSettings = false }
        HideTasksSettingsScreen(viewModel = viewModel, onBackClick = { showHideTasksSettings = false })
    } else {
        Scaffold(
            containerColor = White,
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        // Empty spacer to balance the navigation icon and perfectly center the title
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val currentDayName = AppSettings.firstDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

            SettingsItem(
                icon = if (isDark) R.drawable.moonwhite else R.drawable.sundark,
                title = "Theme & Icon",
                subtext = "$themeSubtext • ${AppIconManager.currentIcon.displayName}",
                onClick = { showThemeScreen = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 16.dp), color = Divider, thickness = 0.5.dp)

            SettingsItem(
                icon = if (isDark) R.drawable.scheduelwhite else R.drawable.schedueldark,
                title = "Start Day of Week",
                subtext = currentDayName,
                onClick = { showStartDayDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 16.dp), color = Divider, thickness = 0.5.dp)

            val hideSubtext = if (AppSettings.isHideAllTasksEnabled) "All tasks hidden"
                else if (AppSettings.isHideRegularTasksEnabled) "Regular tasks hidden"
                else if (AppSettings.isHideTodayTasksEnabled) "Today's tasks hidden"
                else if (AppSettings.isHideInboxTasksEnabled) "Inbox tasks hidden"
                else "Disabled"

            SettingsItem(
                icon = if (isDark) R.drawable.hidewhite else R.drawable.hidedark,
                title = "Hide Tasks",
                subtext = hideSubtext,
                onClick = { showHideTasksSettings = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 16.dp), color = Divider, thickness = 0.5.dp)

            val lockSubtext = if (AppSettings.isLockScreenEnabled || AppSettings.isDevicePasscodeEnabled) "Enabled" else "Disabled"

            SettingsItem(
                icon = if (isDark) R.drawable.lockscreenwhite else R.drawable.lockscreendark,
                title = "App Lock",
                subtext = lockSubtext,
                onClick = { showLockScreenSettings = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 16.dp), color = Divider, thickness = 0.5.dp)

            SettingsItem(
                icon = if (isDark) R.drawable.fileswhite else R.drawable.filesdark,
                title = "Backup",
                subtext = "Export or import data",
                onClick = { showBackupScreen = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp, end = 16.dp), color = Divider, thickness = 0.5.dp)

            SettingsItem(
                icon = Icons.Filled.Info,
                title = "About App",
                subtext = "App info",
                onClick = { showAboutAppScreen = true }
            )
        }
    }
}

    if (showStartDayDialog) {
        StartDaySelectionDialog(onDismiss = { showStartDayDialog = false })
    }
}

@Composable
private fun SettingsItem(
    icon: Any,
    title: String,
    subtext: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon is ImageVector) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else if (icon is Int) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StartDaySelectionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentDay = AppSettings.firstDayOfWeek

    val daysOrder = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Start Day of Week", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                daysOrder.forEach { day ->
                    val dayName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                saveFirstDayOfWeek(context, day)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentDay == day,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Blue500)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = dayName, color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Blue500)
            }
        },
        containerColor = White
    )
}
