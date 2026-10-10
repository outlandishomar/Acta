package com.outlandishomar.acta

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.applyAppSettings
import com.outlandishomar.acta.data.saveLockScreenSettings
import com.outlandishomar.acta.ui.screens.LockScreen
import com.outlandishomar.acta.ui.screens.MainScreen
import com.outlandishomar.acta.ui.theme.ActaTheme
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.ThemeState
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.applyThemeMode
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val openTaskIdState = mutableStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openTaskIdState.value = intent.getIntExtra("open_task_id", -1)

        createNotificationChannel()

        // Synchronously load preferences prior to composing UI
        applyAppSettings(this)
        val isSysDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        applyThemeMode(this, isSysDark)

        enableEdgeToEdge()
        setContent {
            val isSysDarkCompose = isSystemInDarkTheme()
            val context = LocalContext.current

            LaunchedEffect(isSysDarkCompose) {
                applyThemeMode(context, isSysDarkCompose)
            }

            val currentMode = ThemeState.currentThemeMode
            val isResolvedDark = when (currentMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSysDark
            }

            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !isResolvedDark

            ActaTheme {
                var isUnlocked by remember { 
                    mutableStateOf(!AppSettings.isLockScreenEnabled && !AppSettings.isDevicePasscodeEnabled)
                }
                var showUpdatePinPrompt by remember {
                    mutableStateOf(false)
                }
                var showSetNewPinDialog by remember {
                    mutableStateOf(false)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = White
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainScreen(
                            openTaskId = openTaskIdState.value,
                            onTaskOpened = {
                                openTaskIdState.value = -1
                                intent.removeExtra("open_task_id")
                            }
                        )

                        AnimatedVisibility(
                            visible = !isUnlocked,
                            enter = fadeIn(animationSpec = tween(150)),
                            exit = fadeOut(animationSpec = tween(150))
                        ) {
                            LockScreen(
                                onUnlock = { unlockedViaRecovery ->
                                    isUnlocked = true
                                    if (unlockedViaRecovery && !AppSettings.isDevicePasscodeEnabled) {
                                        showUpdatePinPrompt = true
                                    }
                                }
                            )
                        }

                        if (isUnlocked && showUpdatePinPrompt) {
                            AlertDialog(
                                onDismissRequest = { showUpdatePinPrompt = false },
                                title = {
                                    Text(
                                        text = "Want to update your PIN?",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                },
                                text = {
                                    Text(
                                        text = "You just unlocked the app using a recovery method. Would you like to set a new PIN code now?",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        lineHeight = 20.sp
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showUpdatePinPrompt = false
                                            showSetNewPinDialog = true
                                        }
                                    ) {
                                        Text(
                                            "Update PIN",
                                            color = Blue500,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showUpdatePinPrompt = false }
                                    ) {
                                        Text(
                                            "Not Now",
                                            color = TextSecondary
                                        )
                                    }
                                },
                                containerColor = White
                            )
                        }

                        if (isUnlocked && showSetNewPinDialog) {
                            var newPin by remember { mutableStateOf("") }
                            val focusRequester = remember { FocusRequester() }

                            LaunchedEffect(Unit) {
                                delay(100)
                                focusRequester.requestFocus()
                            }

                            AlertDialog(
                                onDismissRequest = { showSetNewPinDialog = false },
                                title = {
                                    Text(
                                        text = "Set New PIN",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                },
                                text = {
                                    Column {
                                        Text("Enter a new 4-digit PIN for your app lock.", color = TextSecondary, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(
                                            value = newPin,
                                            onValueChange = { if (it.length <= 4) newPin = it },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            textStyle = TextStyle(fontSize = 18.sp, letterSpacing = 8.sp),
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
                                                showSetNewPinDialog = false
                                            }
                                        }
                                    ) {
                                        Text(
                                            "Save",
                                            color = Blue500,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSetNewPinDialog = false }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                },
                                containerColor = White
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Alerts"
            val descriptionText = "Notifications for scheduled tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("task_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTaskIdState.value = intent.getIntExtra("open_task_id", -1)
    }
}