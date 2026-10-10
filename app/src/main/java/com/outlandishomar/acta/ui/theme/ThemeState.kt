package com.outlandishomar.acta.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var currentThemeMode by mutableStateOf("SYSTEM") // LIGHT, DARK, SYSTEM
    var isThemeDark by mutableStateOf(false)
}

fun applyThemeMode(context: Context, isSystemDark: Boolean) {
    val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    var savedMode = prefs.getString("mode", "SYSTEM") ?: "SYSTEM"
    if (savedMode == "GREY") {
        savedMode = "DARK"
        prefs.edit().putString("mode", "DARK").apply()
    }
    ThemeState.currentThemeMode = savedMode

    updateThemeColors(context, savedMode, isSystemDark)
}

fun saveThemeMode(context: Context, mode: String, isSystemDark: Boolean) {
    context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE).edit().putString("mode", mode).apply()
    ThemeState.currentThemeMode = mode
    
    updateThemeColors(context, mode, isSystemDark)
}
