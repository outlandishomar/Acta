package com.outlandishomar.acta.widget

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.TaskCompletion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

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
                    // Instant optimistic checkoff so the widget removes the row immediately
                    OptimisticCheckoffStore.markChecked(taskId)
                    val appContext = applicationContext
                    WidgetRenderer.WORKER.execute {
                        runCatching {
                            // Refresh immediately using optimistic state
                            WidgetRenderer.refreshAll(appContext)
                            // Persist completion to Room database
                            runBlocking(Dispatchers.IO) {
                                val taskDao = AppDatabase.getInstance(appContext).taskDao()
                                if (repeatMode != "ONCE" && dateMillis != -1L) {
                                    taskDao.insertCompletion(TaskCompletion(taskId, dateMillis))
                                } else {
                                    val task = taskDao.getTaskById(taskId)
                                    if (task != null) {
                                        taskDao.update(task.copy(isCompleted = true))
                                    }
                                }
                            }
                            OptimisticCheckoffStore.clear(taskId)
                            WidgetRenderer.refreshAll(appContext)
                        }
                    }
                }
            }
            data.scheme == SCHEME && data.host == TASK_HOST -> {
                val taskId = data.lastPathSegment?.toIntOrNull() ?: -1
                startActivity(WidgetRenderer.appIntent(this, openTaskId = taskId))
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
