package com.outlandishomar.acta.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.ThemeState
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.calculateLuminance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val currentIcon = AppIconManager.currentIcon
    val isDarkScheme = ThemeState.isThemeDark || White.calculateLuminance() <= 0.5f
    val logoRes = when (currentIcon) {
        com.outlandishomar.acta.data.AppIcon.DEFAULT -> R.drawable.subttasks
        com.outlandishomar.acta.data.AppIcon.THREE_DOTS -> R.drawable.logo_3dots
        com.outlandishomar.acta.data.AppIcon.CURRENT_2 -> R.drawable.logo_current2
        com.outlandishomar.acta.data.AppIcon.ACTA_WORD -> R.drawable.logo_actaword
    }
    val noveltyRes = if (isDarkScheme) R.drawable.noveltylight else R.drawable.noveltydark

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

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "v1.0",
                        fontSize = 12.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
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
                }
            }
        }
    }
}
