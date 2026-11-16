package com.outlandishomar.acta.widget

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.isAnyHideTaskModeActive
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.registerHideModeTaskId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class QuickCaptureActivity : Activity() {
    private lateinit var titleInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acta_quick_capture)

        window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val headerTitle = findViewById<TextView>(R.id.acta_quick_capture_title)
        val headerDate = findViewById<TextView>(R.id.acta_quick_capture_date)
        titleInput = findViewById(R.id.acta_quick_capture_input)
        notesInput = findViewById(R.id.acta_quick_capture_description)
        cancelButton = findViewById(R.id.acta_quick_capture_cancel)
        saveButton = findViewById(R.id.acta_quick_capture_save)

        val today = LocalDate.now()
        headerDate.text = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

        val palette = WidgetPaletteResolver.resolve(this)
        applyPalette(palette, headerTitle, headerDate)

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

        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            saveTask()
        }

        updateSaveState()
        titleInput.requestFocus()
        titleInput.post {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateSaveState() {
        val canSave = !isSaving && titleInput.text?.toString()?.trim()?.isNotEmpty() == true
        saveButton.isEnabled = canSave
        saveButton.alpha = if (canSave) 1f else 0.5f
    }

    private fun applyPalette(palette: WidgetPalette, title: TextView, dateView: TextView) {
        tintDrawable(findViewById(R.id.acta_quick_capture_root), palette.background)
        title.setTextColor(palette.text)
        dateView.setTextColor(palette.mutedText)

        titleInput.setTextColor(palette.text)
        titleInput.setHintTextColor(palette.mutedText)
        tintDrawable(titleInput, palette.card)

        notesInput.setTextColor(palette.text)
        notesInput.setHintTextColor(palette.mutedText)
        tintDrawable(notesInput, palette.card)

        cancelButton.setTextColor(palette.accent)
        tintDrawable(saveButton, palette.accent)
        saveButton.setTextColor(palette.onAccent)
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
}
