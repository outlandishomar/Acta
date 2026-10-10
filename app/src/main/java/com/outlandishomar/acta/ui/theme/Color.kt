package com.outlandishomar.acta.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var Blue500        by mutableStateOf(Color(0xFF1E1E1E))
var Blue100        by mutableStateOf(Color(0xFFBBDEFB))
var Blue50         by mutableStateOf(Color(0xFFE3F2FD))
var White          by mutableStateOf(Color(0xFFD1D1D1))
var Grey50         by mutableStateOf(Color(0xFFFAFAFA))
var Grey100        by mutableStateOf(Color(0xFFDCDCE0))
var TextPrimary    by mutableStateOf(Color(0xFF212121))
var TextSecondary  by mutableStateOf(Color(0xFF757575))
var TextDisabled   by mutableStateOf(Color(0xFFBDBDBD))
var Divider        by mutableStateOf(Color(0xFF1E1E1E))
var Completed      by mutableStateOf(Color(0xFFBDBDBD))
var AvatarGrey     by mutableStateOf(Color(0xFFE0E0E0))
var Red500         by mutableStateOf(Color(0xFFE63946))

var TopBarBackground    by mutableStateOf(Color(0xFFD1D1D1))
var BottomBarBackground by mutableStateOf(Color(0xFFD1D1D1))

fun Color.calculateLuminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

fun parseHexColor(hexStr: String): Color? {
    return try {
        val cleanHex = hexStr.trim().removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> android.graphics.Color.parseColor("#FF$cleanHex")
            8 -> android.graphics.Color.parseColor("#$cleanHex")
            3 -> {
                val expanded = cleanHex.map { "$it$it" }.joinToString("")
                android.graphics.Color.parseColor("#FF$expanded")
            }
            else -> null
        }
        colorInt?.let { Color(it) }
    } catch (e: Exception) {
        null
    }
}

fun updateThemeColors(context: Context, mode: String, isSystemDark: Boolean) {
    var isDark = when (mode) {
        "DARK" -> true
        "LIGHT", "ROSY_TAUPE", "SEASHELL", "DUST_GREY", "SILVER", "ASH_GREY" -> false
        else -> isSystemDark
    }

    when (mode) {
        "ROSY_TAUPE" -> {
            Blue500        = Color(0xFF1E1E1E)
            White          = Color(0xFFC89F9C)
            Grey100        = Color(0xFFD8B2AF)
            Grey50         = Color(0xFFDFC0BD)
            TextPrimary    = Color(0xFF1E1E1E)
            TextSecondary  = Color(0xFF5A4341)
            TextDisabled   = Color(0xFF8A6E6C)
            Divider        = Color(0xFF1E1E1E).copy(alpha = 0.2f)
            Completed      = Color(0xFF8A6E6C)
            TopBarBackground    = Color(0xFFC89F9C)
            BottomBarBackground = Color(0xFFC89F9C)
            isDark = false
        }
        "DUST_GREY" -> {
            Blue500        = Color(0xFF1E1E1E)
            White          = Color(0xFFD1D1D1)
            Grey100        = Color(0xFFDCDCE0)
            Grey50         = Color(0xFFFAFAFA)
            TextPrimary    = Color(0xFF212121)
            TextSecondary  = Color(0xFF757575)
            TextDisabled   = Color(0xFFBDBDBD)
            Divider        = Color(0xFF1E1E1E)
            Completed      = Color(0xFFBDBDBD)
            AvatarGrey     = Color(0xFFE0E0E0)
            Red500         = Color(0xFFE63946)
            TopBarBackground    = Color(0xFFD1D1D1)
            BottomBarBackground = Color(0xFFD1D1D1)
            isDark = false
        }
        "SILVER" -> {
            Blue500        = Color(0xFF212121)
            White          = Color(0xFFB0BBBF)
            Grey100        = Color(0xFFC0CBCE)
            Grey50         = Color(0xFFCCD6D9)
            TextPrimary    = Color(0xFF212121)
            TextSecondary  = Color(0xFF4F575B)
            TextDisabled   = Color(0xFF7C8589)
            Divider        = Color(0xFF212121).copy(alpha = 0.2f)
            Completed      = Color(0xFF7C8589)
            TopBarBackground    = Color(0xFFB0BBBF)
            BottomBarBackground = Color(0xFFB0BBBF)
            isDark = false
        }
        "ASH_GREY" -> {
            Blue500        = Color(0xFF212121)
            White          = Color(0xFFA1A499)
            Grey100        = Color(0xFFB1B4A9)
            Grey50         = Color(0xFFBDBFA6)
            TextPrimary    = Color(0xFF212121)
            TextSecondary  = Color(0xFF4A4D43)
            TextDisabled   = Color(0xFF75786E)
            Divider        = Color(0xFF212121).copy(alpha = 0.2f)
            Completed      = Color(0xFF75786E)
            TopBarBackground    = Color(0xFFA1A499)
            BottomBarBackground = Color(0xFFA1A499)
            isDark = false
        }
        "SEASHELL" -> {
            Blue500        = Color(0xFF212121)
            White          = Color(0xFFEEE2DF)
            Grey100        = Color(0xFFF8EFEF)
            Grey50         = Color(0xFFFAF3F3)
            TextPrimary    = Color(0xFF212121)
            TextSecondary  = Color(0xFF6E6462)
            TextDisabled   = Color(0xFFA09694)
            Divider        = Color(0xFF212121).copy(alpha = 0.2f)
            Completed      = Color(0xFFA09694)
            TopBarBackground    = Color(0xFFEEE2DF)
            BottomBarBackground = Color(0xFFEEE2DF)
            isDark = false
        }
        else -> {
            if (isDark) {
                Blue500        = Color(0xFFE0E0E0)
                Blue100        = Color(0xFF505052)
                Blue50         = Color(0xFF2C2C2E)
                White          = Color(0xFF38383A)
                Grey50         = Color(0xFF48484A)
                Grey100        = Color(0xFF48484A)
                TextPrimary    = Color(0xFFFFFFFF)
                TextSecondary  = Color(0xFFAAAAAA)
                TextDisabled   = Color(0xFF757575)
                Divider        = Color(0xFF48484A)
                Completed      = Color(0xFF757575)
                AvatarGrey     = Color(0xFF48484A)
                Red500         = Color(0xFFE63946)
                TopBarBackground    = White
                BottomBarBackground = White
            } else {
                Blue500        = Color(0xFF212121)
                White          = Color(0xFFEEE2DF)
                Grey100        = Color(0xFFF8EFEF)
                Grey50         = Color(0xFFFAF3F3)
                TextPrimary    = Color(0xFF212121)
                TextSecondary  = Color(0xFF6E6462)
                TextDisabled   = Color(0xFFA09694)
                Divider        = Color(0xFF212121).copy(alpha = 0.2f)
                Completed      = Color(0xFFA09694)
                TopBarBackground    = Color(0xFFEEE2DF)
                BottomBarBackground = Color(0xFFEEE2DF)
            }
        }
    }

    val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    val customPrimary = prefs.getString("custom_primary", null)
    val customSecondary = prefs.getString("custom_secondary", null)

    if (!customPrimary.isNullOrBlank()) {
        parseHexColor(customPrimary)?.let { color ->
            White = color
            TopBarBackground = color
            BottomBarBackground = color

            val bgLuminance = color.calculateLuminance()
            val isCustomDark = bgLuminance <= 0.5f
            isDark = isCustomDark

            if (isCustomDark) {
                // Dark background custom theme:
                TextPrimary = Color(0xFFFFFFFF)
                TextSecondary = Color(0xFFAAAAAA)
                TextDisabled = Color(0xFF757575)
                Completed = Color(0xFF757575)
                Divider = Color(0xFFFFFFFF).copy(alpha = 0.15f)
                Grey100 = Color(0xFFFFFFFF).copy(alpha = 0.12f)
                Grey50 = Color(0xFFFFFFFF).copy(alpha = 0.07f)
                if (customSecondary.isNullOrBlank()) {
                    Blue500 = Color(0xFFE0E0E0)
                }
            } else {
                // Light background custom theme:
                TextPrimary = Color(0xFF1E1E1E)
                TextSecondary = Color(0xFF555555)
                TextDisabled = Color(0xFF888888)
                Completed = Color(0xFF888888)
                Divider = Color(0xFF000000).copy(alpha = 0.15f)
                Grey100 = Color(0xFF000000).copy(alpha = 0.08f)
                Grey50 = Color(0xFF000000).copy(alpha = 0.04f)
                if (customSecondary.isNullOrBlank()) {
                    Blue500 = Color(0xFF1E1E1E)
                }
            }
        }
    }

    if (!customSecondary.isNullOrBlank()) {
        parseHexColor(customSecondary)?.let { color ->
            Blue500 = color
        }
    }

    ThemeState.isThemeDark = isDark
}