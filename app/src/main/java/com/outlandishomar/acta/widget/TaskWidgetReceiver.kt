package com.outlandishomar.acta.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

class TaskWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetRenderer.render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(
                ACTION_MIDNIGHT,
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetReceiver::class.java))
            WidgetRenderer.render(context, manager, ids)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_MIDNIGHT = "com.outlandishomar.acta.widget.MIDNIGHT"
        private const val REQUEST_MIDNIGHT = 4615
        private const val MIDNIGHT_WINDOW_MS = 10L * 60 * 1000

        fun scheduleMidnightRefresh(context: Context, now: Long = System.currentTimeMillis()) {
            val app = context.applicationContext
            val alarm = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(app, TaskWidgetReceiver::class.java).setAction(ACTION_MIDNIGHT)
            val pendingIntent = PendingIntent.getBroadcast(
                app,
                REQUEST_MIDNIGHT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarm.setWindow(AlarmManager.RTC, nextLocalMidnight(now), MIDNIGHT_WINDOW_MS, pendingIntent)
        }

        private fun nextLocalMidnight(now: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
            Calendar.getInstance(timeZone).apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
    }
}
