package com.outlandishomar.acta.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.outlandishomar.acta.MainActivity
import com.outlandishomar.acta.R

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("EXTRA_TASK_ID", -1)
        val taskTitle = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Task"

        Log.d("TaskAlarmReceiver", "Received alarm for task $taskId: $taskTitle")

        if (taskId != -1) {
            showNotification(context, taskId, taskTitle)
        }
    }

    private fun showNotification(context: Context, taskId: Int, taskTitle: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_task_id", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "task_alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(taskTitle)
            .setContentText("Starting Now")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(taskId, builder.build())
            } else {
                Log.w("TaskAlarmReceiver", "Missing POST_NOTIFICATIONS permission.")
                // Optionally handle missing permission (e.g., in Android 13+)
            }
        }
    }
}
