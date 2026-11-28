package com.outlandishomar.acta.widget

import android.app.Activity
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.SubTaskCompletion
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.model.TaskCompletion
import com.outlandishomar.acta.ui.components.RichTextHelper
import com.outlandishomar.acta.ui.components.TaskIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

class TaskPeekActivity : Activity() {
    private var taskId: Int = -1
    private var dateMillis: Long = -1L
    private var currentTask: Task? = null
    private var subTasks: MutableList<SubTask> = mutableListOf()
    private lateinit var palette: WidgetPalette
    private lateinit var subTasksContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = intent?.getIntExtra(EXTRA_TASK_ID, -1) ?: -1
        dateMillis = intent?.getLongExtra(EXTRA_DATE_MILLIS, -1L)?.takeIf { it > 0L }
            ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (taskId == -1) {
            finish()
            return
        }

        val loaded = runBlocking(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(applicationContext).taskDao()
            val t = dao.getTaskById(taskId) ?: return@runBlocking null
            val rawSubs = dao.getSubTasksForTasksNow(listOf(taskId))
            val resolvedSubs = if (t.repeatMode != "ONCE") {
                val subComps = dao.getSubTaskCompletionsForDateNow(dateMillis).map { it.subTaskId }.toSet()
                rawSubs.map { it.copy(isCompleted = subComps.contains(it.id)) }
            } else {
                rawSubs
            }
            t to resolvedSubs
        }

        if (loaded == null) {
            finish()
            return
        }

        currentTask = loaded.first
        subTasks = loaded.second.toMutableList()
        palette = WidgetPaletteResolver.resolve(this)

        setContentView(R.layout.acta_task_peek)

        val root = findViewById<View>(R.id.acta_task_peek_root)
        (root.background?.mutate() as? GradientDrawable)?.setColor(palette.background)

        val checkboxTarget = findViewById<View>(R.id.acta_task_peek_checkbox_target)
        val checkboxBox = findViewById<ImageView>(R.id.acta_task_peek_checkbox_box)
        val taskIconView = findViewById<ImageView>(R.id.acta_task_peek_task_icon)
        val titleView = findViewById<TextView>(R.id.acta_task_peek_title)
        val timeView = findViewById<TextView>(R.id.acta_task_peek_time)
        val bellView = findViewById<ImageView>(R.id.acta_task_peek_bell)
        val repeatView = findViewById<ImageView>(R.id.acta_task_peek_repeat)
        val notesView = findViewById<TextView>(R.id.acta_task_peek_notes)
        subTasksContainer = findViewById(R.id.acta_task_peek_subtasks)
        val openAppBtn = findViewById<Button>(R.id.acta_task_peek_open_app)
        val closeBtn = findViewById<ImageButton>(R.id.acta_task_peek_close)

        val task = currentTask!!
        checkboxBox.setColorFilter(palette.checkboxBorder)

        val iconOption = TaskIcons.getOptionByName(task.iconName)
        when {
            iconOption?.drawableRes != null -> {
                taskIconView.setImageResource(iconOption.drawableRes)
                taskIconView.setColorFilter(palette.text)
                taskIconView.visibility = View.VISIBLE
            }
            iconOption?.icon != null -> {
                val sizePx = (18f * resources.displayMetrics.density).roundToInt().coerceAtLeast(24)
                taskIconView.setImageBitmap(TaskIcons.renderImageVectorToBitmap(iconOption.icon, sizePx, palette.text))
                taskIconView.clearColorFilter()
                taskIconView.visibility = View.VISIBLE
            }
            else -> {
                taskIconView.visibility = View.GONE
            }
        }

        titleView.text = task.title
        titleView.setTextColor(palette.text)

        if (task.taskTimeHour != null && task.taskTimeMinute != null) {
            val h = if (task.taskTimeHour % 12 == 0) 12 else task.taskTimeHour % 12
            val ap = if (task.taskTimeHour < 12) "AM" else "PM"
            timeView.text = String.format(Locale.US, "%d:%02d %s", h, task.taskTimeMinute, ap)
            timeView.setTextColor(palette.faintNotesText)
            timeView.visibility = View.VISIBLE

            val hasAlert = task.alertEnabled ||
                com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts).any { it.isEnabled }
            if (hasAlert) {
                bellView.setColorFilter(palette.faintNotesText)
                bellView.visibility = View.VISIBLE
            } else {
                bellView.visibility = View.GONE
            }
        } else {
            timeView.visibility = View.GONE
            bellView.visibility = View.GONE
        }

        if (task.repeatMode != "ONCE") {
            repeatView.setColorFilter(palette.faintNotesText)
            repeatView.visibility = View.VISIBLE
        } else {
            repeatView.visibility = View.GONE
        }

        val plainNotes = RichTextHelper.parseToRichSpans(task.description).first.trim()
        if (plainNotes.isNotEmpty()) {
            notesView.text = plainNotes
            notesView.setTextColor(palette.faintNotesText)
            notesView.visibility = View.VISIBLE
        } else {
            notesView.visibility = View.GONE
        }

        renderSubTasks()

        checkboxTarget.setOnClickListener {
            completeMainTask()
        }

        openAppBtn.setTextColor(palette.mutedText)
        openAppBtn.setOnClickListener {
            startActivity(WidgetRenderer.appIntent(this, openTaskId = task.id))
            finish()
        }

        closeBtn.setColorFilter(palette.accent)
        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun renderSubTasks() {
        subTasksContainer.removeAllViews()
        if (subTasks.isEmpty()) {
            subTasksContainer.visibility = View.GONE
            return
        }
        subTasksContainer.visibility = View.VISIBLE

        subTasks.forEachIndexed { index, sub ->
            val isLast = index == subTasks.lastIndex
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(34f)
                )
                isClickable = true
                isFocusable = true
            }

            val connector = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(24f), dp(34f))
                scaleType = ImageView.ScaleType.FIT_XY
                setImageResource(if (isLast) R.drawable.acta_widget_tree_last else R.drawable.acta_widget_tree_middle)
                setColorFilter(palette.checkboxBorder)
            }

            val boxFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(26f), dp(34f)).apply {
                    marginStart = dp(-2f)
                    marginEnd = dp(6f)
                }
            }

            val boxImg = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(dp(17f), dp(17f), Gravity.CENTER)
                scaleType = ImageView.ScaleType.FIT_CENTER
                if (sub.isCompleted) {
                    setImageResource(R.drawable.acta_widget_subcheckbox_filled)
                    setColorFilter(palette.accent)
                } else {
                    setImageResource(R.drawable.acta_widget_subcheckbox_unchecked)
                    setColorFilter(palette.checkboxBorder)
                }
            }
            boxFrame.addView(boxImg)

            if (sub.isCompleted) {
                val tickImg = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(dp(11f), dp(11f), Gravity.CENTER)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(R.drawable.acta_widget_checkmark)
                    setColorFilter(palette.onAccent)
                }
                boxFrame.addView(tickImg)
            }

            val titleWrap = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }

            val subTitle = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = sub.title
                textSize = 14f
                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                textAlignment = View.TEXT_ALIGNMENT_GRAVITY
                if (sub.isCompleted) {
                    setTextColor(palette.disabledText)
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    setTextColor(palette.text)
                    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
            titleWrap.addView(subTitle)

            row.addView(connector)
            row.addView(boxFrame)
            row.addView(titleWrap)

            row.setOnClickListener {
                toggleSubTask(index)
            }

            subTasksContainer.addView(row)
        }
    }

    private fun toggleSubTask(index: Int) {
        val sub = subTasks.getOrNull(index) ?: return
        val task = currentTask ?: return
        val newCompleted = !sub.isCompleted
        subTasks[index] = sub.copy(isCompleted = newCompleted)
        renderSubTasks()

        OptimisticCheckoffStore.setSubTaskOverride(sub.id, newCompleted)
        val appContext = applicationContext
        WidgetRenderer.WORKER.execute {
            runCatching {
                WidgetRenderer.refreshAll(appContext)
                runBlocking(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance(appContext).taskDao()
                    if (task.repeatMode != "ONCE") {
                        if (newCompleted) {
                            dao.insertSubTaskCompletion(SubTaskCompletion(sub.id, dateMillis))
                        } else {
                            dao.deleteSubTaskCompletion(sub.id, dateMillis)
                        }
                    } else {
                        dao.updateSubTask(sub.copy(isCompleted = newCompleted))
                    }
                }
                OptimisticCheckoffStore.clearSubTaskOverride(sub.id)
                WidgetRenderer.refreshAll(appContext)
            }
        }
    }

    private fun completeMainTask() {
        val task = currentTask ?: return
        OptimisticCheckoffStore.markChecked(task.id)
        val appContext = applicationContext
        WidgetRenderer.WORKER.execute {
            runCatching {
                WidgetRenderer.refreshAll(appContext)
                runBlocking(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance(appContext).taskDao()
                    if (task.repeatMode != "ONCE") {
                        dao.insertCompletion(TaskCompletion(task.id, dateMillis))
                    } else {
                        dao.update(task.copy(isCompleted = true))
                    }
                }
                OptimisticCheckoffStore.clear(task.id)
                WidgetRenderer.refreshAll(appContext)
            }
        }
        finish()
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        const val EXTRA_TASK_ID = "com.outlandishomar.acta.widget.EXTRA_TASK_ID"
        const val EXTRA_DATE_MILLIS = "com.outlandishomar.acta.widget.EXTRA_DATE_MILLIS"
    }
}
