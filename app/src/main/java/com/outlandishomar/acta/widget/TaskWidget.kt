package com.outlandishomar.acta.widget

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.outlandishomar.acta.data.model.TaskCompletion
import com.outlandishomar.acta.MainActivity
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.CustomAlert
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import java.time.LocalDate
import java.time.ZoneId
import androidx.glance.color.ColorProvider
import com.outlandishomar.acta.R
import com.outlandishomar.acta.ui.components.RichTextHelper

class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getInstance(context)
        val taskDao = database.taskDao()

        val dateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Use suspend (non-Flow) queries for instant fresh reads
        val allTasks = taskDao.getTasksByDateNow(dateMillis)
        // Repeating tasks store their completion in task_completions, not the
        // isCompleted column. Merge them the same way the app does so the widget
        // reflects a completion immediately.
        val completions = taskDao.getCompletionsForDateNow(dateMillis)
        val completionIds = completions.map { it.taskId }.toSet()
        val resolvedTasks = allTasks.map { task ->
            if (task.repeatMode == "ONCE") task
            else task.copy(isCompleted = completionIds.contains(task.id))
        }
        val firstTask = resolvedTasks.filter { !it.isHighlighted && !it.isCompleted }.firstOrNull()

        val subTasks = if (firstTask != null) {
            taskDao.getSubTasksForTasksNow(listOf(firstTask.id))
        } else {
            emptyList()
        }

        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        var savedMode = prefs.getString("mode", "SYSTEM") ?: "SYSTEM"
        if (savedMode == "GREY") {
            savedMode = "DARK"
        }

        provideContent {
            GlanceTheme {
                WidgetContent(context, firstTask, subTasks, savedMode)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, task: Task?, subTasks: List<SubTask>, savedMode: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (task != null) {
                putExtra("open_task_id", task.id)
            }
        }

        // Premium Dark Aesthetic
        val bgColor = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFF18181A), night = Color(0xFF18181A))
            "LIGHT" -> ColorProvider(day = Color.White, night = Color.White)
            else -> ColorProvider(day = Color.White, night = Color(0xFF18181A))
        }
        val textColorSecondary = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA))
            "LIGHT" -> ColorProvider(day = Color(0xFF757575), night = Color(0xFF757575))
            else -> ColorProvider(day = Color(0xFF757575), night = Color(0xFFAAAAAA))
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(16.dp)
                .clickable(actionStartActivity(intent))
        ) {
            if (task == null) {
                EmptyStateContent(savedMode)
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Task content
                    TaskContent(task = task, subTasks = subTasks, savedMode = savedMode)

                    // Push sync icon to bottom-right
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.updatebuttonwhite),
                            contentDescription = "Refresh",
                            modifier = GlanceModifier
                                .size(22.dp)
                                .clickable(
                                    actionSendBroadcast(
                                        Intent(WidgetActionReceiver.ACTION_REFRESH).apply {
                                            component = ComponentName(context, WidgetActionReceiver::class.java)
                                        }
                                    )
                                ),
                            colorFilter = ColorFilter.tint(textColorSecondary)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyStateContent(savedMode: String) {
        val textColorSecondary = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA))
            "LIGHT" -> ColorProvider(day = Color(0xFF757575), night = Color(0xFF757575))
            else -> ColorProvider(day = Color(0xFF757575), night = Color(0xFFAAAAAA))
        }
        val checkColor = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFF8E8E93), night = Color(0xFF8E8E93))
            "LIGHT" -> ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFFBDBDBD))
            else -> ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFF8E8E93))
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        color = checkColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "No tasks for today",
                    style = TextStyle(
                        color = textColorSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    @Composable
    private fun TaskContent(task: Task, subTasks: List<SubTask>, savedMode: String) {
        val context = LocalContext.current
        val textColorPrimary = when(savedMode) {
            "DARK" -> ColorProvider(day = Color.White, night = Color.White)
            "LIGHT" -> ColorProvider(day = Color(0xFF212121), night = Color(0xFF212121))
            else -> ColorProvider(day = Color(0xFF212121), night = Color.White)
        }
        val textColorSecondary = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA))
            "LIGHT" -> ColorProvider(day = Color(0xFF757575), night = Color(0xFF757575))
            else -> ColorProvider(day = Color(0xFF757575), night = Color(0xFFAAAAAA))
        }
        val connectorColor = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFF4A4A4C), night = Color(0xFF4A4A4C))
            "LIGHT" -> ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFFBDBDBD))
            else -> ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFF4A4A4C))
        }
        val checkCircleColor = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFF8E8E93), night = Color(0xFF8E8E93))
            "LIGHT" -> ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFFBDBDBD))
            else -> ColorProvider(day = Color(0xFFBDBDBD), night = Color(0xFF8E8E93))
        }
        
        // Standard disabled colors for checked text
        val checkedTextColor = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFF6E6E73), night = Color(0xFF6E6E73))
            "LIGHT" -> ColorProvider(day = Color(0xFF9E9E9E), night = Color(0xFF9E9E9E))
            else -> ColorProvider(day = Color(0xFF9E9E9E), night = Color(0xFF6E6E73))
        }
        
        val metaIconColor = when(savedMode) {
            "DARK" -> ColorProvider(day = Color(0xFF8E8E93), night = Color(0xFF8E8E93))
            "LIGHT" -> ColorProvider(day = Color(0xFF9E9E9E), night = Color(0xFF9E9E9E))
            else -> ColorProvider(day = Color(0xFF9E9E9E), night = Color(0xFF8E8E93))
        }

        val widgetNotes = RichTextHelper.parseToRichSpans(task.description).first
        val hasContent = widgetNotes.isNotBlank() || subTasks.isNotEmpty()

        val taskIdKey = ActionParameters.Key<Int>("task_id")
        val repeatModeKey = ActionParameters.Key<String>("repeat_mode")
        val taskDateKey = ActionParameters.Key<Long>("task_date")
        
        val toggleTaskAction = actionRunCallback<ToggleTaskAction>(
            actionParametersOf(
                taskIdKey to task.id,
                repeatModeKey to task.repeatMode,
                taskDateKey to task.date
            )
        )
        
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            // === TITLE ROW ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                // Check circle
                Image(
                    provider = ImageProvider(
                        if (task.isCompleted) R.drawable.widget_circle_checked
                        else R.drawable.widget_circle_unchecked
                    ),
                    contentDescription = "Mark complete",
                    modifier = GlanceModifier.size(22.dp).clickable(
                        toggleTaskAction
                    ),
                    colorFilter = ColorFilter.tint(checkCircleColor)
                )

                Spacer(modifier = GlanceModifier.width(10.dp))

                // Task title
                Text(
                    text = task.title,
                    style = TextStyle(
                        color = if (task.isCompleted) checkedTextColor else textColorPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough
                                         else TextDecoration.None
                    ),
                    maxLines = 1
                )

                // Time
                if (task.taskTimeHour != null && task.taskTimeMinute != null) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    val h = if (task.taskTimeHour % 12 == 0) 12 else task.taskTimeHour % 12
                    val ap = if (task.taskTimeHour < 12) "AM" else "PM"
                    Text(
                        text = String.format("%d:%02d %s", h, task.taskTimeMinute, ap),
                        style = TextStyle(color = metaIconColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )

                    // Notification bell
                    val hasAlert = task.alertEnabled ||
                        CustomAlert.parseList(task.additionalAlerts).any { it.isEnabled }
                    if (hasAlert) {
                        Spacer(modifier = GlanceModifier.width(3.dp))
                        Image(
                            provider = ImageProvider(R.drawable.widget_icon_notification),
                            contentDescription = "Alert",
                            modifier = GlanceModifier.size(13.dp),
                            colorFilter = ColorFilter.tint(metaIconColor)
                        )
                    }
                }

                // Push repeat icon right
                Spacer(modifier = GlanceModifier.defaultWeight())

                if (task.repeatMode != "ONCE") {
                    Image(
                        provider = ImageProvider(R.drawable.widget_icon_repeat),
                        contentDescription = "Repeat",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(metaIconColor)
                    )
                }
            }

            // === STUB: bridge from circle bottom to the connector rows below ===
            if (hasContent) {
                Row(modifier = GlanceModifier.fillMaxWidth().height(6.dp)) {
                    Box(modifier = GlanceModifier.width(22.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(connectorColor)) {}
                    }
                }
            }

            // === NOTES ROW ===
            if (widgetNotes.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().height(28.dp)
                ) {
                    // Vertical line (show only if subtasks follow)
                    if (subTasks.isNotEmpty()) {
                        Box(modifier = GlanceModifier.width(22.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(connectorColor)) {}
                        }
                    } else {
                        Spacer(modifier = GlanceModifier.width(22.dp))
                    }

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    Text(
                        text = widgetNotes,
                        style = TextStyle(color = textColorSecondary, fontSize = 14.sp),
                        maxLines = 1
                    )
                }
            }

            // === SUBTASK ROWS ===
            subTasks.forEachIndexed { index, subTask ->
                val isLast = index == subTasks.lastIndex
                
                val subTaskIdKey = ActionParameters.Key<Int>("subtask_id")
                val toggleSubTaskAction = actionRunCallback<ToggleSubTaskAction>(
                    actionParametersOf(subTaskIdKey to subTask.id)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth().height(28.dp)
                ) {
                    // Spine Box: Draws vertical line and horizontal branch intrinsically
                    Box(modifier = GlanceModifier.width(22.dp).fillMaxHeight()) {
                        // Vertical Line Layer
                        Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = GlanceModifier.width(1.dp).defaultWeight().background(connectorColor)) {}
                            if (!isLast) {
                                Box(modifier = GlanceModifier.width(1.dp).defaultWeight().background(connectorColor)) {}
                            } else {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                        }
                        
                        // Horizontal Branch Layer
                        Column(modifier = GlanceModifier.fillMaxSize()) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Box(modifier = GlanceModifier.defaultWeight().height(1.dp).background(connectorColor)) {}
                            }
                            Spacer(modifier = GlanceModifier.defaultWeight())
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(2.dp))

                    // Subtask circle
                    Image(
                        provider = ImageProvider(
                            if (subTask.isCompleted) R.drawable.widget_circle_checked
                            else R.drawable.widget_circle_unchecked
                        ),
                        contentDescription = "Toggle subtask",
                        modifier = GlanceModifier.size(16.dp).clickable(
                            toggleSubTaskAction
                        ),
                        colorFilter = ColorFilter.tint(checkCircleColor)
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Subtask title
                    Text(
                        text = subTask.title,
                        style = TextStyle(
                            color = if (subTask.isCompleted) checkedTextColor else textColorSecondary,
                            fontSize = 14.sp,
                            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough
                                             else TextDecoration.None
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[ActionParameters.Key<Int>("task_id")] ?: return
        val repeatMode = parameters[ActionParameters.Key<String>("repeat_mode")] ?: "ONCE"
        val taskDate = parameters[ActionParameters.Key<Long>("task_date")] ?: -1L
        
        val database = AppDatabase.getInstance(context)
        val taskDao = database.taskDao()
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
        TaskWidget().update(context, glanceId)
    }
}

class ToggleSubTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val subTaskId = parameters[ActionParameters.Key<Int>("subtask_id")] ?: return
        val database = AppDatabase.getInstance(context)
        val taskDao = database.taskDao()
        val subTask = taskDao.getSubTaskById(subTaskId)
        if (subTask != null) {
            taskDao.updateSubTask(subTask.copy(isCompleted = !subTask.isCompleted))
        }
        TaskWidget().update(context, glanceId)
    }
}
