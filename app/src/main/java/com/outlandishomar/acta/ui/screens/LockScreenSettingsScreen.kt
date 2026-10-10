package com.outlandishomar.acta.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.saveDevicePasscodeSettings
import com.outlandishomar.acta.data.saveLockScreenSettings
import com.outlandishomar.acta.data.saveRecoveryCode
import com.outlandishomar.acta.data.saveSecurityQuestion
import com.outlandishomar.acta.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenSettingsScreen(onBackClick: () -> Unit, onBackupClick: () -> Unit) {
    val context = LocalContext.current
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRecoveryCodeDialog by remember { mutableStateOf(false) }
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }
    var showSettingsForgotOptions by remember { mutableStateOf(false) }
    var pendingSwitchTarget by remember { mutableStateOf<String?>(null) }

    val devicePinLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showSettingsForgotOptions = false
            showSetPinDialog = true
        }
    }

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "App Lock",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Description (Centered at top)
            Text(
                text = "Protect your tasks and thoughts with a 4-digit PIN code or device passcode when opening the app.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 1. Main Enable App Lock Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable App Lock",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (AppSettings.isLockScreenEnabled) "App lock is currently enabled" else "App lock is currently disabled",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = AppSettings.isLockScreenEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (AppSettings.isDevicePasscodeEnabled) {
                                pendingSwitchTarget = "TO_APP_LOCK"
                            } else {
                                showSetPinDialog = true
                            }
                        } else {
                            saveLockScreenSettings(context, false, "")
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

            // Indented App Lock Options (Only visible when custom PIN app lock is enabled)
            if (AppSettings.isLockScreenEnabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp)
                ) {
                    // Change PIN Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangePinDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Change PIN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Update your 4-digit security PIN",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "Edit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue500
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Divider.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Recovery Code Option
                    val isRecoverySet = AppSettings.recoveryCode.isNotEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecoveryCodeDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recovery Code",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isRecoverySet) "8-digit recovery code active" else "Set an emergency recovery code",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isRecoverySet) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = if (isRecoverySet) "✓" else "!",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Divider.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Security Question Option
                    val isQuestionSet = AppSettings.securityQuestion.isNotEmpty() && AppSettings.securityAnswer.isNotEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSecurityQuestionDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security Question",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isQuestionSet) "Security question configured" else "Set secret question for PIN reset",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isQuestionSet) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = if (isQuestionSet) "✓" else "!",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

            // 2. Device Passcode Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Passcode",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (AppSettings.isDevicePasscodeEnabled) "Device passcode is currently enabled" else "Device passcode is currently disabled",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = AppSettings.isDevicePasscodeEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (AppSettings.isLockScreenEnabled) {
                                pendingSwitchTarget = "TO_DEVICE_PASSCODE"
                            } else {
                                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                if (keyguardManager.isKeyguardSecure) {
                                    saveDevicePasscodeSettings(context, true)
                                } else {
                                    Toast.makeText(context, "No device passcode set on this device", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            saveDevicePasscodeSettings(context, false)
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

            // Indented Options for Device Passcode (Recovery Code & Security Question - No Change PIN)
            if (AppSettings.isDevicePasscodeEnabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp)
                ) {
                    // Recovery Code Option
                    val isRecoverySet = AppSettings.recoveryCode.isNotEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecoveryCodeDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recovery Code",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isRecoverySet) "8-digit recovery code active" else "Set an emergency recovery code",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isRecoverySet) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = if (isRecoverySet) "✓" else "!",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Divider.copy(alpha = 0.4f), thickness = 0.5.dp)

                    // Security Question Option
                    val isQuestionSet = AppSettings.securityQuestion.isNotEmpty() && AppSettings.securityAnswer.isNotEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSecurityQuestionDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security Question",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isQuestionSet) "Security question configured" else "Set secret question for passcode reset",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isQuestionSet) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = if (isQuestionSet) "✓" else "!",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Backup Warning Block (shown when any app lock is enabled)
            if (AppSettings.isLockScreenEnabled || AppSettings.isDevicePasscodeEnabled) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Divider, thickness = 0.5.dp)

                Surface(
                    color = Red500.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Red500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Backup Recommended",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Red500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Please back up your data regularly. If you forget your PIN or passcode and lose all recovery options, you will have to use 'Clear App Data' in your device settings to access the app, which permanently deletes all your tasks and data.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBackupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue500, contentColor = White)
                ) {
                    Text("Go to Backup", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = {
                Text(text = "Set Lock PIN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("Enter a 4-digit PIN for the lock screen.", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4) newPin = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, letterSpacing = 8.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue500,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPin.length == 4) {
                            saveLockScreenSettings(context, true, newPin)
                            showSetPinDialog = false
                        }
                    }
                ) {
                    Text("Save", color = Blue500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var pinChangeError by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Text(text = "Change PIN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("Enter current PIN:", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 4) currentPinInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, letterSpacing = 6.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue500,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Enter new 4-digit PIN:", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, letterSpacing = 6.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue500,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            showChangePinDialog = false
                            showSettingsForgotOptions = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Your PIN?", color = Blue500, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (pinChangeError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = pinChangeError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentPinInput != AppSettings.lockScreenPin) {
                            pinChangeError = "Current PIN is incorrect"
                        } else if (newPinInput.length != 4) {
                            pinChangeError = "New PIN must be 4 digits"
                        } else {
                            saveLockScreenSettings(context, true, newPinInput)
                            showChangePinDialog = false
                        }
                    }
                ) {
                    Text("Update", color = Blue500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }

    // Settings Forgot PIN Recovery Options Dialog (Without Clear Data)
    if (showSettingsForgotOptions) {
        var activeSubView by remember { mutableStateOf<String?>(null) }
        var recoveryInput by remember { mutableStateOf("") }
        var securityAnswerInput by remember { mutableStateOf("") }
        var recoveryError by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(activeSubView) {
            if (activeSubView != null) {
                delay(100)
                focusRequester.requestFocus()
            }
        }

        AlertDialog(
            onDismissRequest = {
                showSettingsForgotOptions = false
                activeSubView = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeSubView != null) {
                        IconButton(
                            onClick = { activeSubView = null; recoveryError = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (activeSubView) {
                            "RECOVERY_CODE" -> "Recovery Code"
                            "SECURITY_QUESTION" -> "Security Question"
                            else -> "Unlock Options"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column {
                    when (activeSubView) {
                        "RECOVERY_CODE" -> {
                            Text(
                                text = "Enter your 8-digit Emergency Recovery Code:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = recoveryInput,
                                onValueChange = { recoveryInput = it },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, letterSpacing = 4.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Blue500,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                        "SECURITY_QUESTION" -> {
                            Text(
                                text = AppSettings.securityQuestion,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = securityAnswerInput,
                                onValueChange = { securityAnswerInput = it },
                                singleLine = true,
                                placeholder = { Text("Enter your secret answer", color = TextSecondary.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Blue500,
                                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 1. Device Passcode
                                Surface(
                                    onClick = {
                                        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                        if (keyguardManager.isKeyguardSecure) {
                                            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                                                "Unlock Acta",
                                                "Enter your device passcode or pattern to update your PIN"
                                            )
                                            if (intent != null) {
                                                devicePinLauncher.launch(intent)
                                            }
                                        } else {
                                            Toast.makeText(context, "No device passcode set on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    color = TextPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhonelinkLock,
                                            contentDescription = "Device Passcode",
                                            tint = Blue500,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Device Passcode", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text("Use your phone's passcode or pattern", fontSize = 12.sp, color = TextSecondary)
                                        }
                                    }
                                }

                                // 2. Recovery Code
                                val hasRecoveryCode = AppSettings.recoveryCode.isNotEmpty()
                                Surface(
                                    onClick = {
                                        if (!hasRecoveryCode) {
                                            Toast.makeText(context, "No recovery code set in settings", Toast.LENGTH_SHORT).show()
                                        } else {
                                            activeSubView = "RECOVERY_CODE"
                                            recoveryError = ""
                                        }
                                    },
                                    color = TextPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Recovery Code",
                                            tint = Blue500,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Recovery Code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text(
                                                if (hasRecoveryCode) "Enter your 8-digit code" else "Not configured in settings",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                // 3. Security Question
                                val hasSecurityQuestion = AppSettings.securityQuestion.isNotEmpty() && AppSettings.securityAnswer.isNotEmpty()
                                Surface(
                                    onClick = {
                                        if (!hasSecurityQuestion) {
                                            Toast.makeText(context, "No security question set in settings", Toast.LENGTH_SHORT).show()
                                        } else {
                                            activeSubView = "SECURITY_QUESTION"
                                            recoveryError = ""
                                        }
                                    },
                                    color = TextPrimary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Quiz,
                                            contentDescription = "Security Question",
                                            tint = Blue500,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Security Question", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text(
                                                if (hasSecurityQuestion) "Answer your secret question" else "Not configured in settings",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (recoveryError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = recoveryError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (activeSubView != null) {
                    TextButton(
                        onClick = {
                            if (activeSubView == "RECOVERY_CODE") {
                                if (recoveryInput.trim() == AppSettings.recoveryCode.trim()) {
                                    showSettingsForgotOptions = false
                                    showSetPinDialog = true
                                } else {
                                    recoveryError = "Incorrect Recovery Code"
                                }
                            } else if (activeSubView == "SECURITY_QUESTION") {
                                if (securityAnswerInput.trim().equals(AppSettings.securityAnswer.trim(), ignoreCase = true)) {
                                    showSettingsForgotOptions = false
                                    showSetPinDialog = true
                                } else {
                                    recoveryError = "Incorrect Answer"
                                }
                            }
                        }
                    ) {
                        Text("Verify", color = Blue500, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsForgotOptions = false
                        activeSubView = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }

    // Recovery Code Dialog
    if (showRecoveryCodeDialog) {
        val clipboardManager = LocalClipboardManager.current
        var currentCode by remember {
            mutableStateOf(
                if (AppSettings.recoveryCode.isNotEmpty()) AppSettings.recoveryCode
                else (10000000..99999999).random().toString()
            )
        }

        AlertDialog(
            onDismissRequest = { showRecoveryCodeDialog = false },
            title = {
                Text(text = "Recovery Code", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Save this 8-digit code. You can use it if you ever forget your PIN to unlock the app.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = TextPrimary.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = currentCode,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Blue500,
                                letterSpacing = 4.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        TextButton(
                            onClick = {
                                currentCode = (10000000..99999999).random().toString()
                            }
                        ) {
                            Text("Generate New", color = Blue500, fontSize = 13.sp)
                        }
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentCode))
                                android.widget.Toast.makeText(context, "Code copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy Code", color = Blue500, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveRecoveryCode(context, currentCode)
                        showRecoveryCodeDialog = false
                    }
                ) {
                    Text("Save Code", color = Blue500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryCodeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }

    // Security Question Dialog
    if (showSecurityQuestionDialog) {
        val presetQuestions = listOf(
            "What was the name of your first pet?",
            "What is your mother's maiden name?",
            "In what city were you born?",
            "What was the name of your first school?",
            "What is your favorite book or movie?"
        )

        var selectedQuestion by remember {
            mutableStateOf(
                if (AppSettings.securityQuestion.isNotEmpty()) AppSettings.securityQuestion
                else presetQuestions.first()
            )
        }
        var answerInput by remember {
            mutableStateOf(AppSettings.securityAnswer)
        }
        var errorMsg by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { showSecurityQuestionDialog = false },
            title = {
                Text(text = "Security Question", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Choose a question and set an answer to verify your identity if you forget your PIN.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("Select Question:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(
                                text = selectedQuestion,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(White)
                        ) {
                            presetQuestions.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, color = TextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        selectedQuestion = q
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Your Secret Answer:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue500,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (answerInput.trim().isEmpty()) {
                            errorMsg = "Please enter an answer"
                        } else {
                            saveSecurityQuestion(context, selectedQuestion, answerInput.trim())
                            showSecurityQuestionDialog = false
                        }
                    }
                ) {
                    Text("Save", color = Blue500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityQuestionDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }

    // Lock Method Switch Warning Dialog
    if (pendingSwitchTarget != null) {
        val isToAppLock = pendingSwitchTarget == "TO_APP_LOCK"
        AlertDialog(
            onDismissRequest = { pendingSwitchTarget = null },
            title = {
                Text(
                    text = "Switch Lock Method?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isToAppLock) {
                        "Enabling App Lock with a custom PIN will disable Device Passcode mode. Your saved recovery options will remain active for your App Lock PIN. Do you want to proceed?"
                    } else {
                        "Enabling Device Passcode will disable your custom App Lock PIN. Unlocking will rely on your phone's passcode or pattern. Do you want to proceed?"
                    },
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingSwitchTarget
                        pendingSwitchTarget = null
                        if (target == "TO_APP_LOCK") {
                            showSetPinDialog = true
                        } else if (target == "TO_DEVICE_PASSCODE") {
                            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                            if (keyguardManager.isKeyguardSecure) {
                                saveDevicePasscodeSettings(context, true)
                            } else {
                                Toast.makeText(context, "No device passcode set on this device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(
                        "Switch Method",
                        color = Blue500,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSwitchTarget = null }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }
}
