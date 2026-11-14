package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.saveUserProfile
import com.outlandishomar.acta.ui.components.BirthdayConfettiOverlay
import com.outlandishomar.acta.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    var nameInput by remember { mutableStateOf(AppSettings.userName) }
    var selectedBirthday by remember { mutableStateOf(AppSettings.userBirthday) }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var previewTriggerKey by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            saveUserProfile(context, nameInput, selectedBirthday)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = White,
            topBar = {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Profile", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { updated ->
                        nameInput = updated
                        saveUserProfile(context, updated, selectedBirthday)
                    },
                    placeholder = {
                        Text("Enter your name", color = TextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue500,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.45f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Blue500
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Birthday",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val formattedBirthday = remember(selectedBirthday) {
                    selectedBirthday?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Grey50)
                        .clickable { showBirthdayPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cake,
                        contentDescription = null,
                        tint = if (selectedBirthday != null) Blue500 else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formattedBirthday ?: "Select your birthday",
                        fontSize = 15.sp,
                        color = if (formattedBirthday != null) TextPrimary else TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedBirthday != null) {
                        IconButton(
                            onClick = {
                                selectedBirthday = null
                                saveUserProfile(context, nameInput, null)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear birthday",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val previewContentColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E)
                Button(
                    onClick = { previewTriggerKey++ },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue500,
                        contentColor = previewContentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Celebration,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Celebration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        BirthdayConfettiOverlay(triggerKey = previewTriggerKey)
    }

    if (showBirthdayPicker) {
        val today = remember { LocalDate.now() }
        val todayMaxMillis = remember(today) {
            today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val initialMillis = remember(selectedBirthday) {
            (selectedBirthday ?: today.minusYears(20))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = 1920..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMaxMillis
                }
            }
        )

        val selectedDateTextColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E)
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        selectedBirthday = pickedDate
                        saveUserProfile(context, nameInput, pickedDate)
                    }
                    showBirthdayPicker = false
                }) {
                    Text("OK", color = Blue500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = White)
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
                    selectedYearContentColor = selectedDateTextColor,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = Blue500,
                    selectedDayContentColor = selectedDateTextColor,
                    todayContentColor = Blue500,
                    todayDateBorderColor = Blue500,
                    dividerColor = Divider
                )
            )
        }
    }
}
