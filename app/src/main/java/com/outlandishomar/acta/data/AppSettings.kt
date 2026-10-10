package com.outlandishomar.acta.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DayOfWeek

object AppSettings {
    // Default to Monday
    var firstDayOfWeek by mutableStateOf(DayOfWeek.MONDAY)
    
    // Pro Plan State - All features unlocked for everyone
    var isPro by mutableStateOf(true)

    // Lock Screen State
    var isLockScreenEnabled by mutableStateOf(false)
    var isDevicePasscodeEnabled by mutableStateOf(false)
    var lockScreenPin by mutableStateOf("")
    var recoveryCode by mutableStateOf("")
    var securityQuestion by mutableStateOf("")
    var securityAnswer by mutableStateOf("")

    // Hide Tasks State
    var isHideAllTasksEnabled by mutableStateOf(false)
    var isHideRegularTasksEnabled by mutableStateOf(false)
    var isHideTodayTasksEnabled by mutableStateOf(false)
    var isHideInboxTasksEnabled by mutableStateOf(false)

    // Tasks created during hide mode (IDs stored as Set)
    var hideModeTaskIds by mutableStateOf<Set<Int>>(emptySet())
}

fun shouldHideTask(task: com.outlandishomar.acta.data.model.Task, isInboxTask: Boolean, isTodayTask: Boolean = false): Boolean {
    // If the task was created during hide mode, do NOT hide it (it is a demo task)
    if (AppSettings.hideModeTaskIds.contains(task.id)) return false

    if (AppSettings.isHideAllTasksEnabled) return true
    if (isInboxTask) {
        return AppSettings.isHideInboxTasksEnabled
    } else {
        if (AppSettings.isHideRegularTasksEnabled) return true
        if (isTodayTask && AppSettings.isHideTodayTasksEnabled) return true
        return false
    }
}

fun isAnyHideTaskModeActive(): Boolean {
    return AppSettings.isHideAllTasksEnabled ||
        AppSettings.isHideRegularTasksEnabled ||
        AppSettings.isHideTodayTasksEnabled ||
        AppSettings.isHideInboxTasksEnabled
}

fun registerHideModeTaskId(context: Context, taskId: Int) {
    if (!isAnyHideTaskModeActive()) return
    val newSet = AppSettings.hideModeTaskIds + taskId
    AppSettings.hideModeTaskIds = newSet
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("hide_mode_task_ids", newSet.joinToString(","))
        .apply()
}

fun clearHideModeTaskIds(context: Context): Set<Int> {
    val currentIds = AppSettings.hideModeTaskIds
    AppSettings.hideModeTaskIds = emptySet()
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .remove("hide_mode_task_ids")
        .apply()
    return currentIds
}

fun applyAppSettings(context: Context) {
    AppIconManager.init(context)
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    // 1 = Monday, 7 = Sunday
    val savedDay = prefs.getInt("first_day_of_week", DayOfWeek.MONDAY.value)
    AppSettings.firstDayOfWeek = DayOfWeek.of(savedDay)
    
    // Always enable all features for all users
    AppSettings.isPro = true
    
    val lockEnabled = prefs.getBoolean("is_lock_screen_enabled", false)
    val devicePasscodeEnabled = prefs.getBoolean("is_device_passcode_enabled", false)
    AppSettings.isLockScreenEnabled = lockEnabled
    AppSettings.isDevicePasscodeEnabled = devicePasscodeEnabled
    AppSettings.lockScreenPin = prefs.getString("lock_screen_pin", "") ?: ""
    AppSettings.recoveryCode = prefs.getString("recovery_code", "") ?: ""
    AppSettings.securityQuestion = prefs.getString("security_question", "") ?: ""
    AppSettings.securityAnswer = prefs.getString("security_answer", "") ?: ""

    val hideAll = prefs.getBoolean("hide_all_tasks", false)
    val hideRegular = prefs.getBoolean("hide_regular_tasks", false)
    val hideToday = prefs.getBoolean("hide_today_tasks", false)
    val hideInbox = prefs.getBoolean("hide_inbox_tasks", false)

    val savedHideTaskIdsStr = prefs.getString("hide_mode_task_ids", "") ?: ""
    val hideTaskIds = savedHideTaskIdsStr.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
    AppSettings.hideModeTaskIds = hideTaskIds

    AppSettings.isHideAllTasksEnabled = hideAll
    AppSettings.isHideRegularTasksEnabled = hideRegular
    AppSettings.isHideTodayTasksEnabled = hideToday
    AppSettings.isHideInboxTasksEnabled = hideInbox
}

fun saveFirstDayOfWeek(context: Context, day: DayOfWeek) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putInt("first_day_of_week", day.value)
        .apply()
    AppSettings.firstDayOfWeek = day
}

fun saveIsPro(context: Context, isPro: Boolean) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("is_pro", true)
        .apply()
    AppSettings.isPro = true
}

fun saveHideTasksSettings(context: Context, hideAll: Boolean, hideRegular: Boolean, hideToday: Boolean, hideInbox: Boolean) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("hide_all_tasks", hideAll)
        .putBoolean("hide_regular_tasks", hideRegular)
        .putBoolean("hide_today_tasks", hideToday)
        .putBoolean("hide_inbox_tasks", hideInbox)
        .apply()
    AppSettings.isHideAllTasksEnabled = hideAll
    AppSettings.isHideRegularTasksEnabled = hideRegular
    AppSettings.isHideTodayTasksEnabled = hideToday
    AppSettings.isHideInboxTasksEnabled = hideInbox
}

fun saveLockScreenSettings(context: Context, isEnabled: Boolean, pin: String) {
    val editor = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
    editor.putBoolean("is_lock_screen_enabled", isEnabled)

    if (!isEnabled) {
        editor.putString("lock_screen_pin", "")
        if (!AppSettings.isDevicePasscodeEnabled) {
            editor.putString("recovery_code", "")
            editor.putString("security_question", "")
            editor.putString("security_answer", "")
            AppSettings.recoveryCode = ""
            AppSettings.securityQuestion = ""
            AppSettings.securityAnswer = ""
        }
        AppSettings.isLockScreenEnabled = false
        AppSettings.lockScreenPin = ""
    } else {
        editor.putString("lock_screen_pin", pin)
        AppSettings.isLockScreenEnabled = true
        AppSettings.lockScreenPin = pin

        editor.putBoolean("is_device_passcode_enabled", false)
        AppSettings.isDevicePasscodeEnabled = false
    }

    editor.apply()
}

fun saveDevicePasscodeSettings(context: Context, isEnabled: Boolean) {
    val editor = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
    editor.putBoolean("is_device_passcode_enabled", isEnabled)
    AppSettings.isDevicePasscodeEnabled = isEnabled

    if (isEnabled) {
        editor.putBoolean("is_lock_screen_enabled", false)
        editor.putString("lock_screen_pin", "")
        AppSettings.isLockScreenEnabled = false
        AppSettings.lockScreenPin = ""
    } else if (!AppSettings.isLockScreenEnabled) {
        editor.putString("recovery_code", "")
        editor.putString("security_question", "")
        editor.putString("security_answer", "")
        AppSettings.recoveryCode = ""
        AppSettings.securityQuestion = ""
        AppSettings.securityAnswer = ""
    }

    editor.apply()
}

fun saveRecoveryCode(context: Context, code: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("recovery_code", code)
        .apply()
    AppSettings.recoveryCode = code
}

fun saveSecurityQuestion(context: Context, question: String, answer: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("security_question", question)
        .putString("security_answer", answer)
        .apply()
    AppSettings.securityQuestion = question
    AppSettings.securityAnswer = answer
}
