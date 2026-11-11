package com.outlandishomar.acta.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.AppIconManager
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.Divider
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.ThemeState
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.calculateLuminance

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog

private data class UsedLibrary(
    val name: String,
    val version: String,
    val dependency: String,
    val url: String,
    val licenseName: String,
    val licenseUrl: String
)

private val usedLibraries = listOf(
    UsedLibrary(
        name = "Jetpack Compose UI",
        version = "2024.09.00",
        dependency = "androidx.compose.ui:ui",
        url = "https://developer.android.com/jetpack/compose",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Compose Material 3",
        version = "2024.09.00",
        dependency = "androidx.compose.material3:material3",
        url = "https://developer.android.com/jetpack/androidx/releases/compose-material3",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Material Icons Extended",
        version = "2024.09.00",
        dependency = "androidx.compose.material:material-icons-extended",
        url = "https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Room Database",
        version = "2.7.0",
        dependency = "androidx.room:room-runtime:2.7.0",
        url = "https://developer.android.com/training/data-storage/room",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Jetpack Glance",
        version = "1.1.1",
        dependency = "androidx.glance:glance-appwidget:1.1.1",
        url = "https://developer.android.com/jetpack/androidx/releases/glance",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Calvin Reorderable",
        version = "2.4.1",
        dependency = "sh.calvin.reorderable:reorderable:2.4.1",
        url = "https://github.com/Calvin-LL/Reorderable",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://github.com/Calvin-LL/Reorderable/blob/main/LICENSE"
    ),
    UsedLibrary(
        name = "Lifecycle & ViewModel Compose",
        version = "2.10.0",
        dependency = "androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0",
        url = "https://developer.android.com/topic/libraries/architecture/viewmodel",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Activity Compose",
        version = "1.13.0",
        dependency = "androidx.activity:activity-compose:1.13.0",
        url = "https://developer.android.com/jetpack/androidx/releases/activity",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Core KTX",
        version = "1.18.0",
        dependency = "androidx.core:core-ktx:1.18.0",
        url = "https://developer.android.com/kotlin/ktx",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Desugar JDK Libs",
        version = "2.0.4",
        dependency = "com.android.tools:desugar_jdk_libs:2.0.4",
        url = "https://developer.android.com/studio/write/java8-support#library-desugaring",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Navigation Compose",
        version = "2.8.9",
        dependency = "androidx.navigation:navigation-compose:2.8.9",
        url = "https://developer.android.com/jetpack/compose/navigation",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "KSP - Kotlin Symbol Processing",
        version = "2.0.21-1.0.26",
        dependency = "com.google.devtools.ksp:2.0.21-1.0.26",
        url = "https://github.com/google/ksp",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "JUnit 4",
        version = "4.13.2",
        dependency = "junit:junit:4.13.2",
        url = "https://junit.org/junit4/",
        licenseName = "Eclipse Public License, Version 1.0",
        licenseUrl = "https://www.eclipse.org/legal/epl-v10.html"
    ),
    UsedLibrary(
        name = "Espresso Core",
        version = "3.7.0",
        dependency = "androidx.test.espresso:espresso-core:3.7.0",
        url = "https://developer.android.com/training/testing/espresso",
        licenseName = "The Apache Software License, Version 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
    ),
    UsedLibrary(
        name = "Inter Font Family",
        version = "4.1",
        dependency = "rsms:inter:4.1",
        url = "https://github.com/rsms/inter",
        licenseName = "SIL Open Font License, Version 1.1",
        licenseUrl = "https://raw.githubusercontent.com/rsms/inter/master/LICENSE.txt"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var showUsedLibrariesDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

    val currentIcon = AppIconManager.currentIcon
    val isDarkScheme = ThemeState.isThemeDark || White.calculateLuminance() <= 0.5f
    val logoRes = when (currentIcon) {
        com.outlandishomar.acta.data.AppIcon.DEFAULT -> R.drawable.subttasks
        com.outlandishomar.acta.data.AppIcon.THREE_DOTS -> R.drawable.logo_3dots
        com.outlandishomar.acta.data.AppIcon.CURRENT_2 -> R.drawable.logo_current2
        com.outlandishomar.acta.data.AppIcon.ACTA_WORD -> R.drawable.logo_actaword
    }
    val noveltyRes = if (isDarkScheme) R.drawable.noveltylight else R.drawable.noveltydark
    val linkColor = if (isDarkScheme) Color(0xFF64B5F6) else Color(0xFF1976D2)

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("About App", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDonateDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolunteerActivism,
                            contentDescription = "Donate",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                painter = painterResource(id = logoRes),
                contentDescription = "App Logo",
                tint = TextPrimary,
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Acta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Acta is a minimalist, offline, and privacy-focused task manager designed to help you organize your day without worrying about whether your data is being collected or used for AI training.",
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Start,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val bullets = listOf(
                    "Powerful Task Style" to "Break down complex goals into simple, actionable subtasks to achieve your plans effortlessly without confusion.",
                    "Simple Design" to "Built to be clean and intuitive. Every feature is easily accessible at a glance, so you never feel overwhelmed or hesitant to use the app.",
                    "Privacy First" to "Acta operates locally and offline. No ads, no tracking, no data collection, no account sign-ups, no cloud servers, and no unnecessary permissions."
                )

                bullets.forEach { (title, description) ->
                    val annotatedString = buildAnnotatedString {
                        append("• ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) {
                            append(title)
                            append(": ")
                        }
                        append(description)
                    }
                    Text(
                        text = annotatedString,
                        fontSize = 13.5.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Start,
                        lineHeight = 19.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "All of your data stays strictly on your device, with full control to export backup copies anytime from the settings.",
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Start,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_github),
                    contentDescription = "GitHub Repository",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/outlandishomar/Acta"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                )

                Spacer(modifier = Modifier.height(35.dp))

                Icon(
                    painter = painterResource(id = noveltyRes),
                    contentDescription = "Novelty Designs",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .height(30.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://noveltydesigns.net/"))
                            context.startActivity(intent)
                        }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Google Play & GitHub versions on the left
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play_store),
                                contentDescription = "Google Play",
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "v2.1",
                                fontSize = 11.5.sp,
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = "GitHub",
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "v2.1.1",
                                fontSize = 11.5.sp,
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Privacy Policy in the center
                    Text(
                        text = "Privacy Policy",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://acta-app-policy.oasamir-post.workers.dev/"))
                                context.startActivity(intent)
                            }
                    )

                    // Used libraries on the right
                    Text(
                        text = "Used libraries",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clickable { showUsedLibrariesDialog = true }
                    )
                }
            }
        }
    }

    if (showUsedLibrariesDialog) {
        Dialog(onDismissRequest = { showUsedLibrariesDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = White,
                border = BorderStroke(1.dp, Divider.copy(alpha = 0.2f)),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp, start = 22.dp, end = 22.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "Used libraries",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(usedLibraries) { lib ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${lib.name}, ${lib.version}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lib.dependency,
                                    fontSize = 12.5.sp,
                                    color = linkColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lib.url,
                                    fontSize = 12.5.sp,
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)))
                                        } catch (_: Exception) {}
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lib.licenseName,
                                    fontSize = 12.5.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lib.licenseUrl,
                                    fontSize = 12.5.sp,
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lib.licenseUrl)))
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showUsedLibrariesDialog = false },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CLOSE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = linkColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDonateDialog) {
        Dialog(onDismissRequest = { showDonateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = White,
                border = BorderStroke(1.dp, Divider.copy(alpha = 0.2f)),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = TextPrimary.copy(alpha = 0.08f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolunteerActivism,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Support Acta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Acta is free, open-source and keeps your data for you only. This project has been and is still being made by a single developer. You can either help financially or by leaving a review on the store, starring the repo, or contributing on GitHub. Your help will definitely keep me able to maintain and keep this project alive.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val donateBtnContainerColor = Blue500
                    val donateBtnContentColor = if (donateBtnContainerColor.calculateLuminance() > 0.5f) Color(0xFF1E1E1E) else Color.White

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://ko-fi.com/outlandishomar")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            showDonateDialog = false
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = donateBtnContainerColor,
                            contentColor = donateBtnContentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolunteerActivism,
                            contentDescription = null,
                            tint = donateBtnContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Donate",
                            color = donateBtnContentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showDonateDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Maybe later",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
