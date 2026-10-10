package com.outlandishomar.acta.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.saveIsPro
import com.outlandishomar.acta.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPlanScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val isProActive = AppSettings.isPro

    val isDark = com.outlandishomar.acta.ui.theme.ThemeState.isThemeDark

    val premiumColor = if (isDark) Color(0xFFFFB300) else Color(0xFFC58200)

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Premium Plan",
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Feature Comparison Table
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Divider.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                color = White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Feature",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }

                        VerticalDivider(color = Divider.copy(alpha = 0.5f), thickness = 1.dp)

                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Free Plan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        VerticalDivider(color = Divider.copy(alpha = 0.5f), thickness = 1.dp)

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Premium Plan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = premiumColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    HorizontalDivider(color = Divider.copy(alpha = 0.5f), thickness = 1.dp)

                    val comparisonRows = listOf(
                        ComparisonRow("Subtasks", "Up to 5", "Unlimited", isBool = false),
                        ComparisonRow("Alerts & Reminders", "1 Default Alert", "Unlimited Alerts", isBool = false),
                        ComparisonRow("Repeating Tasks", "Basic Daily", "Advanced Daily, Weekly And Monthly", isBool = false),
                        ComparisonRow("Themes & Colors", "Light & Dark", "Premium Themes + Custom Color Picker Tool", isBool = false),
                        ComparisonRow("Hide Tasks", "", "", isBool = true, freeAvailable = false, proAvailable = true),
                        ComparisonRow("App Lock", "", "", isBool = true, freeAvailable = false, proAvailable = true)
                    )

                    comparisonRows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Feature Column
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 14.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = row.feature,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            VerticalDivider(color = Divider.copy(alpha = 0.5f), thickness = 1.dp)

                            // Free Plan Column
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .fillMaxHeight()
                                    .padding(vertical = 14.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (row.isBool) {
                                    Text(
                                        text = "—",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = row.freeValue,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            VerticalDivider(color = Divider.copy(alpha = 0.5f), thickness = 1.dp)

                            // Premium Plan Column
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight()
                                    .padding(vertical = 14.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (row.isBool) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Included",
                                        tint = premiumColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = row.proValue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = premiumColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (index < comparisonRows.size - 1) {
                            HorizontalDivider(color = Divider.copy(alpha = 0.5f), thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Activate / Deactivate Premium Button
            Button(
                onClick = {
                    saveIsPro(context, !isProActive)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProActive) Color(0xFFFF3B30) else Blue500,
                    contentColor = White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isProActive) "Deactivate Premium Plan" else "Activate Premium Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Toggle for testing and development purposes",
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class ComparisonRow(
    val feature: String,
    val freeValue: String,
    val proValue: String,
    val isBool: Boolean = false,
    val freeAvailable: Boolean = false,
    val proAvailable: Boolean = true
)
