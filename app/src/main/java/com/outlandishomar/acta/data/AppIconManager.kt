package com.outlandishomar.acta.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.outlandishomar.acta.R

enum class AppIcon(
    val id: String,
    val displayName: String,
    val aliasClassName: String,
    @param:DrawableRes val previewDrawableRes: Int,
    val isDarkBackground: Boolean
) {
    DEFAULT(
        id = "default",
        displayName = "Default",
        aliasClassName = "com.outlandishomar.acta.MainActivityDefault",
        previewDrawableRes = R.drawable.theappicon_foreground,
        isDarkBackground = false
    ),
    THREE_DOTS(
        id = "3dots",
        displayName = "3 Dots",
        aliasClassName = "com.outlandishomar.acta.MainActivity3Dots",
        previewDrawableRes = R.drawable.icon_3dots_foreground,
        isDarkBackground = false
    ),
    CURRENT_2(
        id = "current2",
        displayName = "Current 2",
        aliasClassName = "com.outlandishomar.acta.MainActivityCurrent2",
        previewDrawableRes = R.drawable.icon_current2_foreground,
        isDarkBackground = false
    ),
    ACTA_WORD(
        id = "actaword",
        displayName = "Acta Word",
        aliasClassName = "com.outlandishomar.acta.MainActivityActaWord",
        previewDrawableRes = R.drawable.icon_actaword_foreground,
        isDarkBackground = false
    );

    companion object {
        fun fromId(id: String): AppIcon = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

object AppIconManager {
    var currentIcon by mutableStateOf(AppIcon.DEFAULT)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getString("selected_app_icon", AppIcon.DEFAULT.id) ?: AppIcon.DEFAULT.id
        currentIcon = AppIcon.fromId(savedId)
    }

    fun setAppIcon(context: Context, newIcon: AppIcon) {
        if (currentIcon == newIcon) return

        val packageManager = context.packageManager
        val packageName = context.packageName

        // 1. Enable the newly selected icon alias first
        val targetComponent = ComponentName(packageName, newIcon.aliasClassName)
        packageManager.setComponentEnabledSetting(
            targetComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 2. Disable all other icon aliases
        for (icon in AppIcon.entries) {
            if (icon != newIcon) {
                val component = ComponentName(packageName, icon.aliasClassName)
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

        // 3. Save selection and update state
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_app_icon", newIcon.id).apply()
        currentIcon = newIcon
    }
}
