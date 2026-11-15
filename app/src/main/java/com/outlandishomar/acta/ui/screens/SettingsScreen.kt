package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.GridView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.outlandishomar.acta.data.AppIconManager
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.saveFirstDayOfWeek
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
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
import com.outlandishomar.acta.ui.components.SETTINGS_BACKGROUND_ART_OPTIONS
import com.outlandishomar.acta.ui.components.SessionBackgroundArt
import com.outlandishomar.acta.ui.components.SettingsBackgroundArt
import com.outlandishomar.acta.ui.theme.*

import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
private fun ProfilePillCard(
    userName: String,
    formattedBirthday: String?,
    onClick: () -> Unit
) {
    val hasProfile = userName.isNotEmpty() || formattedBirthday != null
    val titleText = if (userName.isNotEmpty()) userName else "Profile"

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Grey50,
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasProfile && formattedBirthday != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedBirthday,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Grey100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open Profile",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

enum class SettingsSubPage {
    PROFILE,
    ABOUT,
    BACKUP,
    THEME,
    LOCK,
    HIDE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenSubPage: (SettingsSubPage) -> Unit = {},
    viewModel: TaskViewModel = viewModel()
) {
    var showStartDayDialog by remember { mutableStateOf(false) }
    var showArtPickerDialog by remember { mutableStateOf(false) }

    val currentMode = ThemeState.currentThemeMode
    val themeSubtext = when (currentMode) {
        "DARK" -> "Dark Theme"
        "LIGHT" -> "Light Theme"
        "ROSY_TAUPE" -> "Rosy Taupe"
        "DUST_GREY" -> "Dust Grey"
        "SILVER" -> "Silver"
        "ASH_GREY" -> "Ash Grey"
        else -> "Device Default"
    }

    val isDark = ThemeState.isThemeDark

    Scaffold(
        containerColor = TopBarBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showArtPickerDialog = true }
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TopBarBackground)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsBackgroundArt(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                val formattedBirthday = remember(AppSettings.userBirthday) {
                    AppSettings.userBirthday?.format(
                        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                    )
                }

                ProfilePillCard(
                    userName = AppSettings.userName.trim(),
                    formattedBirthday = formattedBirthday,
                    onClick = { onOpenSubPage(SettingsSubPage.PROFILE) }
                )

                val currentDayName = AppSettings.firstDayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

                SettingsItem(
                    icon = if (isDark) R.drawable.moonwhite else R.drawable.sundark,
                    title = "Appearance",
                    subtext = "$themeSubtext • ${AppIconManager.currentIcon.displayName}",
                    onClick = { onOpenSubPage(SettingsSubPage.THEME) }
                )

                SettingsItem(
                    icon = if (isDark) R.drawable.scheduelwhite else R.drawable.schedueldark,
                    title = "Start Day of Week",
                    subtext = currentDayName,
                    onClick = { showStartDayDialog = true }
                )

                val hideSubtext = if (AppSettings.isHideAllTasksEnabled) "All tasks hidden"
                    else if (AppSettings.isHideRegularTasksEnabled) "Regular tasks hidden"
                    else if (AppSettings.isHideTodayTasksEnabled) "Today's tasks hidden"
                    else if (AppSettings.isHideInboxTasksEnabled) "Inbox tasks hidden"
                    else "Disabled"

                SettingsItem(
                    icon = if (isDark) R.drawable.hidewhite else R.drawable.hidedark,
                    title = "Hide",
                    subtext = hideSubtext,
                    onClick = { onOpenSubPage(SettingsSubPage.HIDE) }
                )

                val lockSubtext = if (AppSettings.isLockScreenEnabled || AppSettings.isDevicePasscodeEnabled) "Enabled" else "Disabled"

                SettingsItem(
                    icon = if (isDark) R.drawable.lockscreenwhite else R.drawable.lockscreendark,
                    title = "App Lock",
                    subtext = lockSubtext,
                    onClick = { onOpenSubPage(SettingsSubPage.LOCK) }
                )

                SettingsItem(
                    icon = if (isDark) R.drawable.fileswhite else R.drawable.filesdark,
                    title = "Backup",
                    subtext = "Export or import data",
                    onClick = { onOpenSubPage(SettingsSubPage.BACKUP) }
                )

                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "About",
                    subtext = "App info",
                    onClick = { onOpenSubPage(SettingsSubPage.ABOUT) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showStartDayDialog) {
        StartDaySelectionDialog(onDismiss = { showStartDayDialog = false })
    }

    if (showArtPickerDialog) {
        BackgroundArtSelectionDialog(onDismiss = { showArtPickerDialog = false })
    }
}

@Composable
private fun BackgroundArtSelectionDialog(onDismiss: () -> Unit) {
    val selectedOverride = SessionBackgroundArt.manualOverrideResId

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Background Art Preview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            SessionBackgroundArt.manualOverrideResId = null
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOverride == null,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = Blue500)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Auto (Random)",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                SETTINGS_BACKGROUND_ART_OPTIONS.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                SessionBackgroundArt.manualOverrideResId = option.resId
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOverride == option.resId,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Blue500)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${index + 1}. ${option.displayName}",
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Blue500)
            }
        },
        containerColor = White
    )
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
