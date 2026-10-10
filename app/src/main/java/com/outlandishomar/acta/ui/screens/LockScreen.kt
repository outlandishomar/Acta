package com.outlandishomar.acta.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(onUnlock: (unlockedViaRecovery: Boolean) -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isUnlocking by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var activeSubView by remember { mutableStateOf<String?>(null) } // null = menu list, "RECOVERY_CODE", "SECURITY_QUESTION"

    val devicePinLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showForgotPinDialog = false
            onUnlock(AppSettings.isLockScreenEnabled)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    val isDark = ThemeState.isThemeDark

    val lockIconRes = if (isDark) R.drawable.lockscreenwhite else R.drawable.lockscreendark

    fun triggerShakeAnimation() {
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-18f) at 50
                    18f at 100
                    (-14f) at 150
                    14f at 200
                    (-8f) at 250
                    8f at 300
                    (-4f) at 350
                    0f at 400
                }
            )
        }
    }

    fun handleDigitPress(digit: String) {
        if (pin.length < 4 && !isUnlocking) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isError = false
            val newPin = pin + digit
            pin = newPin
            if (newPin.length == 4) {
                if (newPin == AppSettings.lockScreenPin) {
                    isUnlocking = true
                    coroutineScope.launch {
                        delay(140) // Allow 4th dot to render filled state smoothly
                        onUnlock(false)
                    }
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isError = true
                    triggerShakeAnimation()
                    coroutineScope.launch {
                        delay(250)
                        pin = ""
                        delay(1250)
                        if (isError) {
                            isError = false
                        }
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (pin.isNotEmpty() && !isUnlocking) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isError = false
            pin = pin.dropLast(1)
        }
    }

    fun launchDevicePasscode() {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Unlock Acta",
                "Enter your device passcode or pattern to unlock"
            )
            if (intent != null) {
                devicePinLauncher.launch(intent)
            }
        } else {
            Toast.makeText(context, "No device passcode set on this device", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (AppSettings.isDevicePasscodeEnabled && !AppSettings.isLockScreenEnabled) {
            launchDevicePasscode()
        }
    }

    Scaffold(
        containerColor = White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (AppSettings.isDevicePasscodeEnabled && !AppSettings.isLockScreenEnabled) {
                // Device Passcode Lock Screen View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(id = lockIconRes),
                        contentDescription = "Lock Icon",
                        tint = TextPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "App Locked",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = SfProText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Use your device passcode to continue",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontFamily = SfProText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { launchDevicePasscode() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue500, contentColor = White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhonelinkLock,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with Device Passcode", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(42.dp))

                Text(
                    text = "I Forgot My Passcode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue500,
                    fontFamily = SfProText,
                    modifier = Modifier
                        .clickable {
                            activeSubView = null
                            showForgotPinDialog = true
                        }
                        .padding(12.dp)
                )
            } else {
                // Custom 4-Digit PIN Lock Screen View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(id = lockIconRes),
                        contentDescription = "Lock Icon",
                        tint = TextPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "App Locked",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = SfProText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isError) "Incorrect PIN, try again" else "Enter your PIN to continue",
                        fontSize = 14.sp,
                        color = if (isError) MaterialTheme.colorScheme.error else TextSecondary,
                        fontFamily = SfProText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // PIN Dots Indicator with Shake Offset
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.offset(x = shakeOffset.value.dp)
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < pin.length
                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isFilled -> TextPrimary
                                    else -> Color.Transparent
                                },
                                animationSpec = tween(140),
                                label = "dotColor"
                            )

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(color = dotColor, shape = CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isError) MaterialTheme.colorScheme.error else if (isFilled) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Keypad Section (Digits 0-9 & Backspace)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val numberGrid = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "DEL")
                    )

                    numberGrid.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { item ->
                                if (item.isEmpty()) {
                                    Spacer(modifier = Modifier.size(68.dp))
                                } else if (item == "DEL") {
                                    KeypadButton(
                                        onClick = { handleBackspace() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    KeypadButton(
                                        onClick = { handleDigitPress(item) }
                                    ) {
                                        Text(
                                            text = item,
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            fontFamily = SfProText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(42.dp))

                // Forgot PIN link at bottom center with extra spacing
                Text(
                    text = "I Forgot My PIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue500,
                    fontFamily = SfProText,
                    modifier = Modifier
                        .clickable {
                            activeSubView = null
                            showForgotPinDialog = true
                        }
                        .padding(12.dp)
                )
            }
        }
    }

    // Forgot PIN Options Dialog
    if (showForgotPinDialog) {
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
                showForgotPinDialog = false
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
                                // 1. Device Passcode (Only shown when PIN lock is active, hidden when Device Passcode is primary lock)
                                if (!AppSettings.isDevicePasscodeEnabled) {
                                    Surface(
                                        onClick = {
                                            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                            if (keyguardManager.isKeyguardSecure) {
                                                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                                                    "Unlock Acta",
                                                    "Enter your device passcode or pattern to unlock"
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

                                // 4. Clear App Data
                                Surface(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
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
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Clear App Data",
                                            tint = Color(0xFFFF3B30),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Clear App Data", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text("Reset storage from device settings", fontSize = 12.sp, color = TextSecondary)
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
                                    showForgotPinDialog = false
                                    onUnlock(true)
                                } else {
                                    recoveryError = "Incorrect Recovery Code"
                                }
                            } else if (activeSubView == "SECURITY_QUESTION") {
                                if (securityAnswerInput.trim().equals(AppSettings.securityAnswer.trim(), ignoreCase = true)) {
                                    showForgotPinDialog = false
                                    onUnlock(true)
                                } else {
                                    recoveryError = "Incorrect Answer"
                                }
                            }
                        }
                    ) {
                        Text("Unlock", color = Blue500, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPinDialog = false
                        activeSubView = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = White
        )
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "keypadScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) TextPrimary.copy(alpha = 0.18f) else TextPrimary.copy(alpha = 0.07f),
        animationSpec = tween(durationMillis = 100),
        label = "keypadBg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(68.dp)
            .scale(scale)
            .background(color = bgColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        content()
    }
}
