package com.outlandishomar.acta.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.SubTaskCompletion
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
                    OptimisticCheckoffStore.markChecked(taskId)
                    val appContext = applicationContext
                    WidgetRenderer.WORKER.execute {
                        runCatching {
                            WidgetRenderer.refreshAll(appContext)
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
            data.scheme == SCHEME && data.host == SUBTASK_CHECKOFF_HOST -> {
                val subTaskId = data.lastPathSegment?.toIntOrNull() ?: -1
                val repeatMode = data.getQueryParameter("repeatMode") ?: "ONCE"
                val dateMillis = data.getQueryParameter("date")?.toLongOrNull() ?: -1L
                val newCompleted = data.getQueryParameter("completed") == "1"
                if (subTaskId != -1) {
                    OptimisticCheckoffStore.setSubTaskOverride(subTaskId, newCompleted)
                    val appContext = applicationContext
                    WidgetRenderer.WORKER.execute {
                        runCatching {
                            WidgetRenderer.refreshAll(appContext)
                            runBlocking(Dispatchers.IO) {
                                val taskDao = AppDatabase.getInstance(appContext).taskDao()
                                if (repeatMode != "ONCE" && dateMillis != -1L) {
                                    if (newCompleted) {
                                        taskDao.insertSubTaskCompletion(SubTaskCompletion(subTaskId, dateMillis))
                                    } else {
                                        taskDao.deleteSubTaskCompletion(subTaskId, dateMillis)
                                    }
                                } else {
                                    val subTask = taskDao.getSubTaskById(subTaskId)
                                    if (subTask != null) {
                                        taskDao.updateSubTask(subTask.copy(isCompleted = newCompleted))
                                    }
                                }
                            }
                            OptimisticCheckoffStore.clearSubTaskOverride(subTaskId)
                            WidgetRenderer.refreshAll(appContext)
                        }
                    }
                }
            }
            data.scheme == SCHEME && data.host == TASK_HOST -> {
                val taskId = data.lastPathSegment?.toIntOrNull() ?: -1
                val dateMillis = data.getQueryParameter("date")?.toLongOrNull() ?: -1L
                if (taskId != -1) {
                    startActivity(
                        Intent(this, TaskPeekActivity::class.java).apply {
                            putExtra(TaskPeekActivity.EXTRA_TASK_ID, taskId)
                            putExtra(TaskPeekActivity.EXTRA_DATE_MILLIS, dateMillis)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
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
        const val SUBTASK_CHECKOFF_HOST = "subcheckoff"
        const val TASK_HOST = "task"

        fun checkoffUri(taskId: Int, repeatMode: String, dateMillis: Long): String =
            "$SCHEME://$CHECKOFF_HOST/$taskId?repeatMode=${Uri.encode(repeatMode)}&date=$dateMillis"

        fun subtaskCheckoffUri(
            subTaskId: Int,
            repeatMode: String,
            dateMillis: Long,
            newCompleted: Boolean
        ): String =
            "$SCHEME://$SUBTASK_CHECKOFF_HOST/$subTaskId?repeatMode=${Uri.encode(repeatMode)}&date=$dateMillis&completed=${if (newCompleted) "1" else "0"}"

        fun openTaskUri(taskId: Int, dateMillis: Long): String =
            "$SCHEME://$TASK_HOST/$taskId?date=$dateMillis"
    }
}
