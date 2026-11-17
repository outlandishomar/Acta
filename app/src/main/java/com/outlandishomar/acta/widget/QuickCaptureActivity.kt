package com.outlandishomar.acta.widget

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.isAnyHideTaskModeActive
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.registerHideModeTaskId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class QuickCaptureActivity : Activity() {
    private lateinit var scrollView: ScrollView
    private lateinit var titleInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var subtasksSection: View
    private lateinit var subtasksContainer: LinearLayout
    private lateinit var subtasksTopDivider: View
    private lateinit var subtasksBottomDivider: View
    private lateinit var addSubtaskButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var palette: WidgetPalette
    private val subtaskInputs = mutableListOf<EditText>()
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acta_quick_capture)

        window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        scrollView = findViewById(R.id.acta_quick_capture_scroll)
        titleInput = findViewById(R.id.acta_quick_capture_input)
        notesInput = findViewById(R.id.acta_quick_capture_description)
        subtasksSection = findViewById(R.id.acta_quick_capture_subtasks_section)
        subtasksContainer = findViewById(R.id.acta_quick_capture_subtasks_container)
        subtasksTopDivider = findViewById(R.id.acta_quick_capture_subtasks_top_divider)
        subtasksBottomDivider = findViewById(R.id.acta_quick_capture_subtasks_bottom_divider)
        addSubtaskButton = findViewById(R.id.acta_quick_capture_add_subtask)
        saveButton = findViewById(R.id.acta_quick_capture_save)

        palette = WidgetPaletteResolver.resolve(this)
        applyPalette(palette)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSaveState()
            }
        }
        titleInput.addTextChangedListener(watcher)

        titleInput.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_NEXT || enterPressed) {
                notesInput.requestFocus()
                true
            } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveTask()
                true
            } else {
                false
            }
        }

        addSubtaskButton.setOnClickListener {
            addSubTaskRow()
        }

        saveButton.setOnClickListener {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                finish()
            } else {
                saveTask()
            }
        }

        updateSaveState()
        titleInput.requestFocus()
        titleInput.post {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun addSubTaskRow() {
        subtasksSection.visibility = View.VISIBLE

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4f)
                bottomMargin = dp(4f)
            }
        }

        val dragIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(20f), dp(20f)).apply {
                marginEnd = dp(10f)
            }
            setImageResource(R.drawable.acta_drag_handle_icon)
            setColorFilter(
                Color.argb(
                    150,
                    Color.red(palette.mutedText),
                    Color.green(palette.mutedText),
                    Color.blue(palette.mutedText)
                )
            )
        }

        val unfocusedBorderColor = palette.border
        val focusedBorderColor = palette.accent

        fun buildFieldDrawable(focused: Boolean): GradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8f).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(if (focused) dp(1.5f) else dp(1f), if (focused) focusedBorderColor else unfocusedBorderColor)
        }

        val subInput = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            minHeight = dp(46f)
            background = buildFieldDrawable(false)
            hint = "type..."
            textSize = 14f
            setTextColor(palette.text)
            setHintTextColor(palette.hintText)
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
            maxLines = 3
            setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
            setOnFocusChangeListener { v, hasFocus ->
                v.background = buildFieldDrawable(hasFocus)
            }
        }

        val deleteBtn = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(34f), dp(34f)).apply {
                marginStart = dp(4f)
            }
            val attrs = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless))
            val bgRes = attrs.getResourceId(0, 0)
            attrs.recycle()
            if (bgRes != 0) setBackgroundResource(bgRes) else setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.acta_delete_icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Remove subtask"
            setOnClickListener {
                subtaskInputs.remove(subInput)
                subtasksContainer.removeView(row)
                if (subtaskInputs.isEmpty()) {
                    subtasksSection.visibility = View.GONE
                }
            }
        }

        row.addView(dragIcon)
        row.addView(subInput)
        row.addView(deleteBtn)

        subtaskInputs.add(subInput)
        subtasksContainer.addView(row)

        subInput.requestFocus()
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
            subInput.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(subInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateSaveState() {
        val hasTitle = titleInput.text?.toString()?.trim()?.isNotEmpty() == true
        saveButton.isEnabled = !isSaving
        saveButton.alpha = if (!isSaving && hasTitle) 1f else 0.45f
    }

    private fun applyPalette(palette: WidgetPalette) {
        // Use the exact same background color as the widget
        tintDrawable(findViewById(R.id.acta_quick_capture_root), palette.background)

        titleInput.setTextColor(palette.text)
        titleInput.setHintTextColor(palette.hintText)

        notesInput.setTextColor(palette.mutedText)
        notesInput.setHintTextColor(palette.hintText)

        subtasksTopDivider.setBackgroundColor(palette.border)
        subtasksBottomDivider.setBackgroundColor(palette.border)

        addSubtaskButton.setColorFilter(palette.text)
        saveButton.setColorFilter(palette.accent)
    }

    private fun tintDrawable(view: View, color: Int) {
        (view.background?.mutate() as? GradientDrawable)?.setColor(color)
    }

    private fun saveTask() {
        if (isSaving) return
        val title = titleInput.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            titleInput.requestFocus()
            return
        }
        val notes = notesInput.text?.toString()?.trim().orEmpty()
        val subTaskTitles = subtaskInputs
            .map { it.text?.toString()?.trim().orEmpty() }
            .filter { it.isNotEmpty() }

        isSaving = true
        updateSaveState()

        val appContext = applicationContext
        WidgetRenderer.WORKER.execute {
            val saved = runCatching {
                runBlocking(Dispatchers.IO) {
                    val todayMillis = LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    val taskDao = AppDatabase.getInstance(appContext).taskDao()
                    val maxOrder = taskDao.getMaxOrderIndex() ?: 0
                    val newTask = Task(
                        title = title,
                        description = notes,
                        date = todayMillis,
                        isCompleted = false,
                        isHighlighted = false,
                        orderIndex = maxOrder + 1
                    )
                    val taskId = taskDao.insert(newTask).toInt()
                    for (subTitle in subTaskTitles) {
                        taskDao.insertSubTask(SubTask(taskId = taskId, title = subTitle))
                    }
                    if (isAnyHideTaskModeActive()) {
                        registerHideModeTaskId(appContext, taskId)
                    }
                }
                WidgetRenderer.refreshAll(appContext)
                true
            }.getOrDefault(false)

            runOnUiThread {
                if (saved) {
                    Toast.makeText(appContext, "Task added for today", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    isSaving = false
                    updateSaveState()
                    Toast.makeText(appContext, "Could not save task", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()
}
