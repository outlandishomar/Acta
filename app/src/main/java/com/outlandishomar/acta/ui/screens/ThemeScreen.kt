package com.outlandishomar.acta.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.outlandishomar.acta.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SavedCustomTheme(
    val hex: String,
    val name: String = hex
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentMode = ThemeState.currentThemeMode
    val isSystemDark = isSystemInDarkTheme()

    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val themePrefs = remember { context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) }
    var savedCustomThemes by remember { mutableStateOf(loadSavedCustomThemes(themePrefs)) }
    var selectedThemeForOptions by remember { mutableStateOf<SavedCustomTheme?>(null) }
    var themeToRename by remember { mutableStateOf<SavedCustomTheme?>(null) }
    var themeToDelete by remember { mutableStateOf<SavedCustomTheme?>(null) }
    var activePrimary by remember { mutableStateOf(themePrefs.getString("custom_primary", null)) }

    var lastPickedHex by remember {
        mutableStateOf(
            themePrefs.getString("last_picked_hex", null)
                ?: themePrefs.getString("custom_primary", null)
                ?: themePrefs.getString("custom_secondary", null)
                ?: "#FF5733"
        )
    }
    var hexCode by remember { mutableStateOf("") }
    val parsedColor = remember(hexCode) { parseHexColor(hexCode) }

    BackHandler(enabled = selectedTab == 1) {
        selectedTab = 0
    }

    fun updateTheme(mode: String) {
        activePrimary = null
        themePrefs.edit()
            .remove("custom_primary")
            .remove("custom_secondary")
            .apply()
        saveThemeMode(context, mode, isSystemDark)
        coroutineScope.launch {
            try {
                com.outlandishomar.acta.widget.TaskWidget().updateAll(context)
            } catch (e: Exception) {}
        }
    }

    fun applyCustomTheme(hexStr: String) {
        activePrimary = hexStr
        themePrefs.edit()
            .putString("custom_primary", hexStr)
            .putString("last_picked_hex", hexStr)
            .putString("mode", "CUSTOM")
            .apply()
        ThemeState.currentThemeMode = "CUSTOM"
        lastPickedHex = hexStr
        hexCode = hexStr
        updateThemeColors(context, "CUSTOM", isSystemDark)
        coroutineScope.launch {
            try {
                com.outlandishomar.acta.widget.TaskWidget().updateAll(context)
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        containerColor = White,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedTab == 1) {
                            selectedTab = 0
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            // Theme Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedTab = 0 }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Theme",
                                    fontSize = 20.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) TextPrimary else TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val themeIndicatorWidth by animateDpAsState(
                                    targetValue = if (selectedTab == 0) 44.dp else 0.dp,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                    label = "theme_indicator"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width(themeIndicatorWidth)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(if (selectedTab == 0) Blue500 else Color.Transparent)
                                )
                            }

                            // Icon Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedTab = 1 }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Icon",
                                    fontSize = 20.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) TextPrimary else TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val iconIndicatorWidth by animateDpAsState(
                                    targetValue = if (selectedTab == 1) 40.dp else 0.dp,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                    label = "icon_indicator"
                                )
                                Box(
                                    modifier = Modifier
                                        .height(3.dp)
                                        .width(iconIndicatorWidth)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(if (selectedTab == 1) Blue500 else Color.Transparent)
                                )
                            }
                        }
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
            // Top 3 Default Themes Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // System Default
                ThemeBox(
                    label = "Device Default",
                    boxColor = if (isSystemDark) Color(0xFF38383A) else Color(0xFFEEE2DF),
                    isSelected = activePrimary == null && currentMode == "SYSTEM",
                    onClick = { updateTheme("SYSTEM") },
                    modifier = Modifier.weight(1f)
                )

                // Light Theme
                ThemeBox(
                    label = "Light",
                    boxColor = Color(0xFFEEE2DF),
                    isSelected = activePrimary == null && currentMode == "LIGHT",
                    onClick = { updateTheme("LIGHT") },
                    modifier = Modifier.weight(1f)
                )

                // Dark Theme
                ThemeBox(
                    label = "Dark",
                    boxColor = Color(0xFF38383A),
                    isSelected = activePrimary == null && currentMode == "DARK",
                    onClick = { updateTheme("DARK") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(color = Divider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // Preset Themes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rosy Taupe #C89F9C
                ThemeBox(
                    label = "Rosy Taupe",
                    boxColor = Color(0xFFC89F9C),
                    isSelected = activePrimary == null && currentMode == "ROSY_TAUPE",
                    onClick = { updateTheme("ROSY_TAUPE") },
                    modifier = Modifier.weight(1f)
                )

                // Dust Grey #D1D1D1
                ThemeBox(
                    label = "Dust Grey",
                    boxColor = Color(0xFFD1D1D1),
                    isSelected = activePrimary == null && currentMode == "DUST_GREY",
                    onClick = { updateTheme("DUST_GREY") },
                    modifier = Modifier.weight(1f)
                )

                // Silver #B0BBBF
                ThemeBox(
                    label = "Silver",
                    boxColor = Color(0xFFB0BBBF),
                    isSelected = activePrimary == null && currentMode == "SILVER",
                    onClick = { updateTheme("SILVER") },
                    modifier = Modifier.weight(1f)
                )

                // Ash Grey #A1A499
                ThemeBox(
                    label = "Ash Grey",
                    boxColor = Color(0xFFA1A499),
                    isSelected = activePrimary == null && currentMode == "ASH_GREY",
                    onClick = { updateTheme("ASH_GREY") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Saved Custom Themes & Add '+' Box Grid
            val allCustomItems: List<SavedCustomTheme?> = savedCustomThemes.map<SavedCustomTheme, SavedCustomTheme?> { it } + listOf<SavedCustomTheme?>(null)
            val customRows = allCustomItems.chunked(4)

            customRows.forEachIndexed { rowIndex, rowItems ->
                if (rowIndex > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 0 until 4) {
                        if (i < rowItems.size) {
                            val item = rowItems[i]
                            if (item == null) {
                                // '+' Add Theme Box
                                AddThemeBox(
                                    onClick = {
                                        val colorToAdd = if (hexCode.isNotBlank() && parsedColor != null) {
                                            hexCode
                                        } else {
                                            lastPickedHex
                                        }
                                        if (colorToAdd.isNotBlank()) {
                                            if (!savedCustomThemes.any { it.hex.equals(colorToAdd, ignoreCase = true) }) {
                                                val newTheme = SavedCustomTheme(hex = colorToAdd, name = colorToAdd)
                                                val updated = savedCustomThemes + newTheme
                                                savedCustomThemes = updated
                                                saveCustomThemesToPrefs(themePrefs, updated)
                                            }
                                            applyCustomTheme(colorToAdd)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                // Saved Custom Theme Box
                                val color = parseHexColor(item.hex) ?: Color.Gray
                                val isSelected = (activePrimary != null && activePrimary.equals(item.hex, ignoreCase = true))
                                ThemeBox(
                                    label = item.name,
                                    boxColor = color,
                                    isSelected = isSelected,
                                    onClick = {
                                        applyCustomTheme(item.hex)
                                    },
                                    onLongClick = {
                                        selectedThemeForOptions = item
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            HorizontalDivider(color = Divider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Color Tool & Interactive Palette Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Color Picker",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Drag your finger on the palette or type a HEX code to test custom colors live",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Dual-Bar Color Palette (Hue + Shade)
                ColorPalettePicker(
                    onColorSelected = { newHex ->
                        hexCode = newHex
                        lastPickedHex = newHex
                        themePrefs.edit().putString("last_picked_hex", newHex).apply()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Color Preview Box
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(parsedColor ?: Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = Divider,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (parsedColor == null && hexCode.isNotBlank()) {
                            Text("?", color = Red500, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // HEX Input Field
                    OutlinedTextField(
                        value = hexCode,
                        onValueChange = { input ->
                            if (input.length <= 9) {
                                hexCode = input.uppercase()
                                if (hexCode.isNotBlank()) {
                                    lastPickedHex = hexCode
                                    themePrefs.edit().putString("last_picked_hex", hexCode).apply()
                                }
                            }
                        },
                        placeholder = { Text(lastPickedHex, color = TextSecondary.copy(alpha = 0.4f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue500,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Blue500,
                            disabledBorderColor = Divider,
                            disabledTextColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Set Secondary Button (was Set Accent)
                    Button(
                        onClick = {
                            parsedColor?.let { color ->
                                val hexStr = colorToHex(color)
                                themePrefs.edit()
                                    .putString("custom_secondary", hexStr)
                                    .putString("last_picked_hex", hexStr)
                                    .apply()
                                lastPickedHex = hexStr
                                updateThemeColors(context, currentMode, isSystemDark)
                            }
                        },
                        enabled = parsedColor != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue500,
                            contentColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E),
                            disabledContainerColor = Divider,
                            disabledContentColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Secondary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Set Primary Button (was Set Background)
                    Button(
                        onClick = {
                            parsedColor?.let { color ->
                                val hexStr = colorToHex(color)
                                activePrimary = hexStr
                                themePrefs.edit()
                                    .putString("custom_primary", hexStr)
                                    .putString("last_picked_hex", hexStr)
                                    .apply()
                                lastPickedHex = hexStr
                                updateThemeColors(context, currentMode, isSystemDark)
                            }
                        },
                        enabled = parsedColor != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue500,
                            contentColor = if (Blue500.calculateLuminance() <= 0.5f) Color.White else Color(0xFF1E1E1E),
                            disabledContainerColor = Divider,
                            disabledContentColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set Primary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset Button
                OutlinedButton(
                    onClick = {
                        hexCode = ""
                        activePrimary = null
                        themePrefs.edit()
                            .remove("custom_primary")
                            .remove("custom_secondary")
                            .apply()
                        updateThemeColors(context, currentMode, isSystemDark)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset", fontSize = 13.sp, color = TextPrimary)
                }
            }
        }
    } else {
        AppIconsContent(modifier = Modifier.padding(innerPadding))
    }
    }

    val isBgDark = ThemeState.isThemeDark || White.calculateLuminance() <= 0.5f
    val isReddishBg = White.red > 0.55f && (White.green < 0.45f || White.blue < 0.45f)
    val deleteActionColor = if (isReddishBg) {
        if (isBgDark) Color(0xFFFFFFFF) else Color(0xFF1E1E1E)
    } else {
        if (isBgDark) Color(0xFFFF6B6B) else Red500
    }

    // Long-press Action Sheet / Options Dialog
    if (selectedThemeForOptions != null) {
        val target = selectedThemeForOptions!!
        AlertDialog(
            onDismissRequest = { selectedThemeForOptions = null },
            containerColor = White,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = target.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Rename Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TextPrimary.copy(alpha = 0.06f))
                            .clickable {
                                val t = selectedThemeForOptions
                                selectedThemeForOptions = null
                                themeToRename = t
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Rename",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Rename",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    // Delete Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isReddishBg) {
                                    if (isBgDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                                } else {
                                    Red500.copy(alpha = 0.1f)
                                }
                            )
                            .clickable {
                                val t = selectedThemeForOptions
                                selectedThemeForOptions = null
                                themeToDelete = t
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = deleteActionColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Delete",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = deleteActionColor
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedThemeForOptions = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Rename Dialog
    if (themeToRename != null) {
        val target = themeToRename!!
        var newName by remember(target) { mutableStateOf(target.name) }

        AlertDialog(
            onDismissRequest = { themeToRename = null },
            containerColor = White,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Rename Theme",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a name for theme (${target.hex}):",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= 25) newName = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Blue500,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Blue500
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalName = if (newName.isNotBlank()) newName.trim() else target.hex
                        val updated = savedCustomThemes.map { item ->
                            if (item.hex.equals(target.hex, ignoreCase = true)) {
                                item.copy(name = finalName)
                            } else item
                        }
                        savedCustomThemes = updated
                        saveCustomThemesToPrefs(themePrefs, updated)
                        themeToRename = null
                    }
                ) {
                    Text("Save", color = Blue500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { themeToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (themeToDelete != null) {
        val target = themeToDelete!!
        AlertDialog(
            onDismissRequest = { themeToDelete = null },
            containerColor = White,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Delete Custom Theme",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.name}\" (${target.hex})?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = savedCustomThemes.filterNot { it.hex.equals(target.hex, ignoreCase = true) }
                        savedCustomThemes = updated
                        saveCustomThemesToPrefs(themePrefs, updated)
                        if (activePrimary?.equals(target.hex, ignoreCase = true) == true) {
                            activePrimary = null
                            themePrefs.edit().remove("custom_primary").apply()
                            updateThemeColors(context, currentMode, isSystemDark)
                        }
                        themeToDelete = null
                    }
                ) {
                    Text("Delete", color = deleteActionColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { themeToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ColorPalettePicker(
    onColorSelected: (String) -> Unit
) {
    var selectedHue by remember { mutableFloatStateOf(0f) }
    var selectedShadeFraction by remember { mutableFloatStateOf(0.5f) }

    fun updateColor(hue: Float, shade: Float) {
        val f = shade.coerceIn(0f, 1f)
        val color = if (f < 0.5f) {
            val value = f * 2f
            Color.hsv(hue, 1f, value)
        } else {
            val saturation = (1f - f) * 2f
            Color.hsv(hue, saturation, 1f)
        }
        onColorSelected(colorToHex(color))
    }

    val hueGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF0000), // Red
                Color(0xFFFFFF00), // Yellow
                Color(0xFF00FF00), // Green
                Color(0xFF00FFFF), // Cyan
                Color(0xFF0000FF), // Blue
                Color(0xFFFF00FF), // Magenta
                Color(0xFFFF0000)  // Red
            )
        )
    }

    val pureColor = remember(selectedHue) { Color.hsv(selectedHue, 1f, 1f) }
    val shadeGradient = remember(pureColor) {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Black,
                pureColor,
                Color.White
            )
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Bar 1: Hue Picker ---
        var canvasWidth1 by remember { mutableFloatStateOf(1f) }
        var selectedX1 by remember { mutableFloatStateOf(-1f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(hueGradient)
                .border(1.dp, Divider.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .onGloballyPositioned { coordinates ->
                    canvasWidth1 = coordinates.size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        selectedX1 = offset.x.coerceIn(0f, canvasWidth1)
                        val fraction = (selectedX1 / canvasWidth1).coerceIn(0f, 1f)
                        selectedHue = fraction * 360f
                        updateColor(selectedHue, selectedShadeFraction)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        selectedX1 = change.position.x.coerceIn(0f, canvasWidth1)
                        val fraction = (selectedX1 / canvasWidth1).coerceIn(0f, 1f)
                        selectedHue = fraction * 360f
                        updateColor(selectedHue, selectedShadeFraction)
                    }
                }
        ) {
            if (selectedX1 >= 0f) {
                val density = LocalDensity.current
                val thumbOffsetDp = with(density) { (selectedX1 - 12.dp.toPx()).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetDp)
                        .size(24.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, Color(0xFF1E1E1E), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Bar 2: Shade / Lightness Picker (Black -> Color -> White) ---
        var canvasWidth2 by remember { mutableFloatStateOf(1f) }
        var selectedX2 by remember { mutableFloatStateOf(-1f) }

        LaunchedEffect(canvasWidth2) {
            if (selectedX2 < 0f && canvasWidth2 > 1f) {
                selectedX2 = canvasWidth2 * 0.5f
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shadeGradient)
                .border(1.dp, Divider.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .onGloballyPositioned { coordinates ->
                    canvasWidth2 = coordinates.size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        selectedX2 = offset.x.coerceIn(0f, canvasWidth2)
                        selectedShadeFraction = (selectedX2 / canvasWidth2).coerceIn(0f, 1f)
                        updateColor(selectedHue, selectedShadeFraction)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        selectedX2 = change.position.x.coerceIn(0f, canvasWidth2)
                        selectedShadeFraction = (selectedX2 / canvasWidth2).coerceIn(0f, 1f)
                        updateColor(selectedHue, selectedShadeFraction)
                    }
                }
        ) {
            if (selectedX2 >= 0f) {
                val density = LocalDensity.current
                val thumbOffsetDp = with(density) { (selectedX2 - 12.dp.toPx()).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetDp)
                        .size(24.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, Color(0xFF1E1E1E), CircleShape)
                )
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeBox(
    label: String,
    boxColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(boxColor)
                .border(
                    width = if (isSelected) 3.dp else 2.dp,
                    color = if (isSelected) Blue500 else TextSecondary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                val isLight = boxColor.calculateLuminance() > 0.5f
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = if (isLight) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddThemeBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Grey100.copy(alpha = 0.4f))
                .border(
                    width = 1.5.dp,
                    color = TextSecondary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Theme",
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

private fun loadSavedCustomThemes(prefs: SharedPreferences): List<SavedCustomTheme> {
    val raw = prefs.getString("saved_custom_themes", null) ?: return emptyList()
    if (raw.isBlank()) return emptyList()

    return try {
        if (raw.trim().startsWith("[")) {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i)
                if (obj != null) {
                    val hex = obj.optString("hex", "")
                    val name = obj.optString("name", hex)
                    if (hex.isNotBlank()) SavedCustomTheme(hex = hex, name = if (name.isNotBlank()) name else hex) else null
                } else {
                    val hex = arr.optString(i, "")
                    if (hex.isNotBlank()) SavedCustomTheme(hex = hex, name = hex) else null
                }
            }
        } else {
            raw.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { SavedCustomTheme(hex = it, name = it) }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveCustomThemesToPrefs(prefs: SharedPreferences, themes: List<SavedCustomTheme>) {
    val arr = JSONArray()
    themes.forEach { theme ->
        val obj = JSONObject().apply {
            put("hex", theme.hex)
            put("name", theme.name)
        }
        arr.put(obj)
    }
    prefs.edit().putString("saved_custom_themes", arr.toString()).apply()
}
