package com.outlandishomar.acta.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
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
    private data class SubtaskRowHolder(
        val row: LinearLayout,
        val input: EditText
    )

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
    private val subtaskRows = mutableListOf<SubtaskRowHolder>()
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

    @SuppressLint("ClickableViewAccessibility")
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
            layoutParams = LinearLayout.LayoutParams(dp(32f), dp(46f)).apply {
                marginStart = dp(-4f)
                marginEnd = dp(4f)
            }
            setPadding(dp(6f), dp(13f), dp(6f), dp(13f))
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(R.drawable.acta_drag_handle_icon)
            setColorFilter(
                Color.argb(
                    150,
                    Color.red(palette.mutedText),
                    Color.green(palette.mutedText),
                    Color.blue(palette.mutedText)
                )
            )
            contentDescription = "Reorder subtask"
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

        val holder = SubtaskRowHolder(row = row, input = subInput)

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
                subtaskRows.remove(holder)
                subtasksContainer.removeView(row)
                if (subtaskRows.isEmpty()) {
                    subtasksSection.visibility = View.GONE
                }
            }
        }

        setupDragHandle(dragIcon, holder)

        row.addView(dragIcon)
        row.addView(subInput)
        row.addView(deleteBtn)

        subtaskRows.add(holder)
        subtasksContainer.addView(row)

        subInput.requestFocus()
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
            subInput.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(subInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragHandle(dragHandle: View, holder: SubtaskRowHolder) {
        var startRawY = 0f
        var draggedStartIndex = -1
        var currentTargetIndex = -1
        var stepHeight = 0f
        val targetShifts = HashMap<View, Float>()

        dragHandle.setOnTouchListener { _, event ->
            if (subtaskRows.size <= 1) return@setOnTouchListener false
            val draggedRow = holder.row

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    draggedStartIndex = subtaskRows.indexOf(holder)
                    if (draggedStartIndex == -1) return@setOnTouchListener false
                    currentTargetIndex = draggedStartIndex
                    startRawY = event.rawY
                    val lp = draggedRow.layoutParams as? ViewGroup.MarginLayoutParams
                    stepHeight = (draggedRow.height + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)).toFloat()
                    targetShifts.clear()

                    scrollView.requestDisallowInterceptTouchEvent(true)
                    draggedRow.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    draggedRow.translationZ = dp(6f).toFloat()
                    draggedRow.scaleX = 1.02f
                    draggedRow.scaleY = 1.02f
                    draggedRow.alpha = 0.94f
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (draggedStartIndex == -1) return@setOnTouchListener false
                    val rawDy = event.rawY - startRawY
                    val minDy = -draggedRow.top.toFloat()
                    val maxDy = (subtasksContainer.height - draggedRow.bottom).toFloat()
                    val clampedDy = rawDy.coerceIn(minMost(minDy, 0f), maxMost(maxDy, 0f))
                    draggedRow.translationY = clampedDy

                    val draggedCenterY = draggedRow.top + draggedRow.height / 2f + clampedDy
                    var newTarget = draggedStartIndex

                    if (clampedDy < 0f) {
                        for (i in 0 until draggedStartIndex) {
                            val other = subtaskRows[i].row
                            val otherCenterY = other.top + other.height / 2f
                            if (draggedCenterY < otherCenterY) {
                                newTarget = minOf(newTarget, i)
                            }
                        }
                    } else if (clampedDy > 0f) {
                        for (i in (draggedStartIndex + 1) until subtaskRows.size) {
                            val other = subtaskRows[i].row
                            val otherCenterY = other.top + other.height / 2f
                            if (draggedCenterY > otherCenterY) {
                                newTarget = maxOf(newTarget, i)
                            }
                        }
                    }

                    if (newTarget != currentTargetIndex) {
                        currentTargetIndex = newTarget
                        draggedRow.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    }

                    for (i in subtaskRows.indices) {
                        if (i == draggedStartIndex) continue
                        val otherRow = subtaskRows[i].row
                        val desiredShift = when {
                            draggedStartIndex < currentTargetIndex && i in (draggedStartIndex + 1)..currentTargetIndex -> -stepHeight
                            currentTargetIndex < draggedStartIndex && i in currentTargetIndex until draggedStartIndex -> stepHeight
                            else -> 0f
                        }
                        if (targetShifts[otherRow] != desiredShift) {
                            targetShifts[otherRow] = desiredShift
                            otherRow.animate()
                                .translationY(desiredShift)
                                .setDuration(140)
                                .start()
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (draggedStartIndex == -1) return@setOnTouchListener false
                    scrollView.requestDisallowInterceptTouchEvent(false)

                    val focusedOldIndex = subtaskRows.indexOfFirst { it.input.hasFocus() }
                    val selStart = if (focusedOldIndex != -1) {
                        subtaskRows[focusedOldIndex].input.selectionStart.coerceAtLeast(0)
                    } else {
                        0
                    }

                    for (item in subtaskRows) {
                        item.row.animate().cancel()
                        item.row.translationY = 0f
                        item.row.translationZ = 0f
                        item.row.scaleX = 1f
                        item.row.scaleY = 1f
                        item.row.alpha = 1f
                    }

                    if (currentTargetIndex in subtaskRows.indices && currentTargetIndex != draggedStartIndex) {
                        val texts = subtaskRows.map { it.input.text?.toString().orEmpty() }.toMutableList()
                        val movedText = texts.removeAt(draggedStartIndex)
                        texts.add(currentTargetIndex, movedText)

                        for (i in subtaskRows.indices) {
                            val edit = subtaskRows[i].input
                            if (edit.text?.toString() != texts[i]) {
                                edit.setText(texts[i])
                            }
                        }

                        val newFocusedIndex = when {
                            focusedOldIndex == draggedStartIndex -> currentTargetIndex
                            draggedStartIndex < currentTargetIndex && focusedOldIndex in (draggedStartIndex + 1)..currentTargetIndex -> focusedOldIndex - 1
                            currentTargetIndex < draggedStartIndex && focusedOldIndex in currentTargetIndex until draggedStartIndex -> focusedOldIndex + 1
                            else -> focusedOldIndex
                        }
                        if (newFocusedIndex in subtaskRows.indices) {
                            val targetInput = subtaskRows[newFocusedIndex].input
                            targetInput.requestFocus()
                            val len = targetInput.text?.length ?: 0
                            targetInput.setSelection(selStart.coerceIn(0, len))
                        }
                    }

                    draggedStartIndex = -1
                    currentTargetIndex = -1
                    targetShifts.clear()
                    true
                }

                else -> false
            }
        }
    }

    private fun minMost(a: Float, b: Float): Float = if (a < b) a else b
    private fun maxMost(a: Float, b: Float): Float = if (a > b) a else b

    private fun updateSaveState() {
        val hasTitle = titleInput.text?.toString()?.trim()?.isNotEmpty() == true
        saveButton.isEnabled = !isSaving
        saveButton.alpha = if (!isSaving && hasTitle) 1f else 0.45f
    }

    private fun applyPalette(palette: WidgetPalette) {
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
        val subTaskTitles = subtaskRows
            .map { it.input.text?.toString()?.trim().orEmpty() }
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
