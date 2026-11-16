package com.outlandishomar.acta.widget

import android.app.Activity
import android.net.Uri
import android.os.Bundle

class WidgetTapActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        when {
            data == null -> Unit
            data.scheme == SCHEME && data.host == CHECKOFF_HOST -> {
                val taskId = data.lastPathSegment?.toIntOrNull() ?: -1
                val repeatMode = data.getQueryParameter("repeatMode") ?: "ONCE"
                val dateMillis = data.getQueryParameter("date")?.toLongOrNull() ?: -1L
                if (taskId != -1) {
                    WidgetCheckoffAnimator.startCheckoff(
                        context = applicationContext,
                        taskId = taskId,
                        repeatMode = repeatMode,
                        dateMillis = dateMillis
                    )
                }
            }
            data.scheme == SCHEME && data.host == TASK_HOST -> {
                val taskId = data.lastPathSegment?.toIntOrNull() ?: -1
                if (!WidgetCheckoffAnimator.isAnimating(taskId)) {
                    startActivity(WidgetRenderer.appIntent(this, openTaskId = taskId))
                }
            }
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val SCHEME = "acta-widget"
        const val CHECKOFF_HOST = "checkoff"
        const val TASK_HOST = "task"

        fun checkoffUri(taskId: Int, repeatMode: String, dateMillis: Long): String =
            "$SCHEME://$CHECKOFF_HOST/$taskId?repeatMode=${Uri.encode(repeatMode)}&date=$dateMillis"

        fun openTaskUri(taskId: Int): String =
            "$SCHEME://$TASK_HOST/$taskId"
    }
}
