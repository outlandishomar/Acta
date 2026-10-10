package com.outlandishomar.acta.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.outlandishomar.acta.data.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

import com.outlandishomar.acta.data.model.CustomAlert

object AlarmScheduler {

    fun scheduleTaskAlarm(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        
        // Cancel existing alarms to avoid duplicates if they were removed
        cancelTaskAlarm(context, task.id)

        val date = Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate()

        // 1. Schedule Primary Alert
        if (task.alertEnabled && task.taskTimeHour != null && task.taskTimeMinute != null) {
            val primaryTime = LocalTime.of(task.taskTimeHour, task.taskTimeMinute)
            scheduleAlarm(context, alarmManager, task, date, primaryTime, task.id * 100, canUseExact)
        }

        // 2. Schedule Additional Alerts
        val customAlerts = CustomAlert.parseList(task.additionalAlerts)
        customAlerts.forEachIndexed { index, alert ->
            if (alert.isEnabled) {
                val time = LocalTime.of(alert.hour, alert.minute)
                scheduleAlarm(context, alarmManager, task, date, time, (task.id * 100) + index + 1, canUseExact)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        task: Task,
        date: LocalDate,
        time: LocalTime,
        requestCode: Int,
        useExact: Boolean
    ) {
        var alarmTime = LocalDateTime.of(date, time)

        if (alarmTime.isBefore(LocalDateTime.now())) {
            if (task.repeatMode == "ONCE") return
            
            if (task.repeatMode == "DAILY") {
                val todayTime = LocalDateTime.of(LocalDate.now(), time)
                alarmTime = if (todayTime.isBefore(LocalDateTime.now())) {
                    todayTime.plusDays(1)
                } else {
                    todayTime
                }
            }
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmTimeMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (useExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent)
                Log.d("AlarmScheduler", "Scheduled EXACT alarm for task ${task.id} at $alarmTime with code $requestCode")
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent)
                Log.d("AlarmScheduler", "Scheduled INEXACT alarm for task ${task.id} at $alarmTime with code $requestCode")
            }
        } catch (e: SecurityException) {
            // Fallback to inexact if exact throws
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent)
                Log.d("AlarmScheduler", "Fallback: scheduled INEXACT alarm for task ${task.id} at $alarmTime")
            } catch (e2: Exception) {
                Log.e("AlarmScheduler", "Failed to schedule any alarm: ${e2.message}")
            }
        }
    }

    fun cancelTaskAlarm(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        
        // Cancel primary and up to 50 additional alerts
        for (i in 0..50) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (taskId * 100) + i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        Log.d("AlarmScheduler", "Canceled alarms for task $taskId")
    }
}
