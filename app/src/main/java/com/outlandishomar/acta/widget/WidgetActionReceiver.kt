package com.outlandishomar.acta.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.TaskCompletion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val taskDao = database.taskDao()

                when (action) {
                    ACTION_TOGGLE_TASK -> {
                        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                        val repeatMode = intent.getStringExtra(EXTRA_TASK_REPEAT_MODE) ?: "ONCE"
                        val taskDate = intent.getLongExtra(EXTRA_TASK_DATE, -1L)

                        if (taskId != -1 && taskDate != -1L) {
                            if (repeatMode != "ONCE") {
                                taskDao.insertCompletion(TaskCompletion(taskId, taskDate))
                            } else {
                                val task = taskDao.getTaskById(taskId)
                                if (task != null) {
                                    taskDao.update(task.copy(isCompleted = !task.isCompleted))
                                }
                            }
                        }
                    }
                    ACTION_TOGGLE_SUBTASK -> {
                        val subTaskId = intent.getIntExtra(EXTRA_SUBTASK_ID, -1)
                        if (subTaskId != -1) {
                            val subTask = taskDao.getSubTaskById(subTaskId)
                            if (subTask != null) {
                                taskDao.updateSubTask(subTask.copy(isCompleted = !subTask.isCompleted))
                            }
                        }
                    }
                    ACTION_REFRESH -> {
                        // Just trigger an update
                    }
                }

                // Force an immediate UI refresh for all widget instances
                TaskWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.outlandishomar.acta.widget.TOGGLE_TASK"
        const val ACTION_TOGGLE_SUBTASK = "com.outlandishomar.acta.widget.TOGGLE_SUBTASK"
        const val ACTION_REFRESH = "com.outlandishomar.acta.widget.REFRESH"
        
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_REPEAT_MODE = "extra_task_repeat_mode"
        const val EXTRA_TASK_DATE = "extra_task_date"
        const val EXTRA_SUBTASK_ID = "extra_subtask_id"
    }
}
