package com.outlandishomar.acta.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.applyAppSettings
import com.outlandishomar.acta.data.isAnyHideTaskModeActive
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.CustomAlert
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.registerHideModeTaskId
import com.outlandishomar.acta.receiver.AlarmScheduler
import com.outlandishomar.acta.ui.components.AdvancedNotesDialog
import com.outlandishomar.acta.ui.components.CustomTimePickerDialog
import com.outlandishomar.acta.ui.components.HowOftenSection
import com.outlandishomar.acta.ui.components.RichTextHelper
import com.outlandishomar.acta.ui.components.StartEndDialog
import com.outlandishomar.acta.ui.components.TaskIconPickerDialog
import com.outlandishomar.acta.ui.components.TaskIcons
import com.outlandishomar.acta.ui.components.WhenSection
import com.outlandishomar.acta.ui.theme.ActaTheme
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.Divider
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary
import com.outlandishomar.acta.ui.theme.White
import com.outlandishomar.acta.ui.theme.applyThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt

class QuickCaptureActivity : ComponentActivity() {
    private data class SubtaskRowHolder(
        val row: LinearLayout,
        val input: EditText
    )

    private lateinit var scrollView: ScrollView
    private lateinit var iconButton: FrameLayout
    private lateinit var iconImageView: ImageView
    private lateinit var titleInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var advancedNotesButton: LinearLayout
    private lateinit var advancedNotesText: TextView
    private lateinit var advancedNotesArrow: ImageView
    private lateinit var subtasksSection: View
    private lateinit var subtasksContainer: LinearLayout
    private lateinit var subtasksTopDivider: View
    private lateinit var subtasksBottomDivider: View
    private lateinit var advancedComposeView: ComposeView
    private lateinit var addSubtaskButton: ImageButton
    private lateinit var advancedToggleButton: LinearLayout
    private lateinit var advancedToggleText: TextView
    private lateinit var advancedToggleArrow: ImageView
    private lateinit var saveButton: ImageButton
    private lateinit var palette: WidgetPalette

    private val subtaskRows = mutableListOf<SubtaskRowHolder>()
    private var isSaving = false
    private var isUpdatingNotesProgrammatically = false

    // Shared reactive state between native views and Compose overlays/sections
    private val selectedIconNameState = mutableStateOf<String?>(null)
    private val showIconPickerDialogState = mutableStateOf(false)
    private val richNotesState = mutableStateOf("")
    private val showAdvancedNotesDialogState = mutableStateOf(false)
    private val isExpandedState = mutableStateOf(false)
    private val hasSubtasksState = mutableStateOf(false)

    // When? & Alerts state
    private val whenEnabledState = mutableStateOf(false)
    private val selectedHourState = mutableStateOf(LocalTime.now().hour)
    private val selectedMinuteState = mutableStateOf(((LocalTime.now().minute + 4) / 5 * 5) % 60)
    private val alertEnabledState = mutableStateOf(false)
    private val additionalAlertsList = mutableStateListOf<CustomAlert>()

    // How often? & Start/End Date state
    private val selectedTimeDateState = mutableStateOf(LocalDate.now())
    private val endDateState = mutableStateOf<LocalDate?>(null)
    private val hasEndDateState = mutableStateOf(false)
    private val repeatModeState = mutableStateOf("ONCE")
    private val repeatIntervalState = mutableStateOf(1)
    private val repeatDaysSetState = mutableStateOf<Set<Int>>(emptySet())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyAppSettings(this)
        val isSysDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        applyThemeMode(this, isSysDark)

        val now = LocalTime.now()
        val roundedMin = ((now.minute + 4) / 5) * 5
        selectedHourState.value = if (roundedMin >= 60) (now.hour + 1) % 24 else now.hour
        selectedMinuteState.value = roundedMin % 60
        selectedTimeDateState.value = LocalDate.now()

        setContentView(R.layout.acta_quick_capture)

        @Suppress("DEPRECATION")
        window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        scrollView = findViewById(R.id.acta_quick_capture_scroll)
        iconButton = findViewById(R.id.acta_quick_capture_icon_btn)
        iconImageView = findViewById(R.id.acta_quick_capture_icon_img)
        titleInput = findViewById(R.id.acta_quick_capture_input)
        notesInput = findViewById(R.id.acta_quick_capture_description)
        advancedNotesButton = findViewById(R.id.acta_quick_capture_advanced_notes_btn)
        advancedNotesText = findViewById(R.id.acta_quick_capture_advanced_notes_text)
        advancedNotesArrow = findViewById(R.id.acta_quick_capture_advanced_notes_arrow)
        subtasksSection = findViewById(R.id.acta_quick_capture_subtasks_section)
        subtasksContainer = findViewById(R.id.acta_quick_capture_subtasks_container)
        subtasksTopDivider = findViewById(R.id.acta_quick_capture_subtasks_top_divider)
        subtasksBottomDivider = findViewById(R.id.acta_quick_capture_subtasks_bottom_divider)
        advancedComposeView = findViewById(R.id.acta_quick_capture_advanced_compose)
        addSubtaskButton = findViewById(R.id.acta_quick_capture_add_subtask)
        advancedToggleButton = findViewById(R.id.acta_quick_capture_advanced_toggle)
        advancedToggleText = findViewById(R.id.acta_quick_capture_advanced_toggle_text)
        advancedToggleArrow = findViewById(R.id.acta_quick_capture_advanced_toggle_arrow)
        saveButton = findViewById(R.id.acta_quick_capture_save)

        palette = WidgetPaletteResolver.resolve(this)
        applyPalette(palette)
        updateIconButtonVisual()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSaveState()
            }
        }
        titleInput.addTextChangedListener(titleWatcher)

        val notesWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingNotesProgrammatically) return
                val newText = s?.toString().orEmpty()
                val (oldPlain, oldSpans) = RichTextHelper.parseToRichSpans(richNotesState.value)
                richNotesState.value = if (oldSpans.isEmpty()) {
                    newText
                } else {
                    val diff = newText.length - oldPlain.length
                    val adjusted = when {
                        diff > 0 -> RichTextHelper.adjustSpansForInsertion(oldSpans, 0, diff, false, false, false, false, null, 0)
                        diff < 0 -> RichTextHelper.adjustSpansForDeletion(oldSpans, 0, -diff)
                        else -> oldSpans
                    }
                    RichTextHelper.serializeToHtml(newText, adjusted)
                }
            }
        }
        notesInput.addTextChangedListener(notesWatcher)

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

        iconButton.setOnClickListener {
            hideKeyboard()
            showIconPickerDialogState.value = true
        }

        advancedNotesButton.setOnClickListener {
            hideKeyboard()
            showAdvancedNotesDialogState.value = true
        }

        advancedToggleButton.setOnClickListener {
            val nextExpanded = !isExpandedState.value
            isExpandedState.value = nextExpanded
            advancedToggleArrow.animate()
                .rotation(if (nextExpanded) 180f else 0f)
                .setDuration(220)
                .start()
            if (nextExpanded) {
                hideKeyboard()
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

        setupAdvancedComposeContent()

        updateSaveState()
        titleInput.requestFocus()
        titleInput.post {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupAdvancedComposeContent() {
        advancedComposeView.setContent {
            ActaTheme {
                val isExpanded by isExpandedState
                val hasSubtasks by hasSubtasksState
                val selectedIconName by selectedIconNameState
                val showIconPickerDialog by showIconPickerDialogState
                val showAdvancedNotesDialog by showAdvancedNotesDialogState

                var showCustomTimePicker by remember { mutableStateOf(false) }
                var showAdditionalAlertTimePicker by remember { mutableStateOf(false) }
                var showStartEndDialog by remember { mutableStateOf(false) }
                var showDatePickerDialog by remember { mutableStateOf(false) }
                var editingDateType by remember { mutableStateOf("START") }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        alertEnabledState.value = isGranted
                    }
                )

                LaunchedEffect(isExpanded, repeatModeState.value) {
                    if (isExpanded) {
                        delay(160)
                        scrollView.post {
                            scrollView.smoothScrollTo(0, scrollView.bottom)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!hasSubtasks) {
                            HorizontalDivider(
                                color = Divider,
                                thickness = 0.5.dp
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        WhenSection(
                            enabled = whenEnabledState.value,
                            onToggle = { whenEnabledState.value = it },
                            selectedHour = selectedHourState.value,
                            selectedMinute = selectedMinuteState.value,
                            onTimeChange = { h, m ->
                                selectedHourState.value = h
                                selectedMinuteState.value = m
                            },
                            onAdjustTimeClick = { showCustomTimePicker = true },
                            alertEnabled = alertEnabledState.value,
                            onAlertEnabledChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                this@QuickCaptureActivity,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            alertEnabledState.value = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        alertEnabledState.value = true
                                    }
                                } else {
                                    alertEnabledState.value = false
                                }
                            },
                            additionalAlerts = additionalAlertsList,
                            onToggleAdditionalAlert = { index, enabled ->
                                if (index in additionalAlertsList.indices) {
                                    if (enabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            if (ContextCompat.checkSelfPermission(
                                                    this@QuickCaptureActivity,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                additionalAlertsList[index] = additionalAlertsList[index].copy(isEnabled = true)
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        } else {
                                            additionalAlertsList[index] = additionalAlertsList[index].copy(isEnabled = true)
                                        }
                                    } else {
                                        additionalAlertsList.removeAt(index)
                                    }
                                }
                            },
                            onAddAlertClick = { showAdditionalAlertTimePicker = true }
                        )

                        Spacer(Modifier.height(24.dp))

                        HowOftenSection(
                            repeatMode = repeatModeState.value,
                            onRepeatModeChange = { mode ->
                                repeatModeState.value = mode
                                if (mode == "WEEKLY" && repeatDaysSetState.value.isEmpty()) {
                                    repeatDaysSetState.value = setOf(selectedTimeDateState.value.dayOfWeek.value)
                                }
                            },
                            repeatInterval = repeatIntervalState.value,
                            onRepeatIntervalChange = { repeatIntervalState.value = it },
                            repeatDaysSet = repeatDaysSetState.value,
                            onRepeatDaysChange = { repeatDaysSetState.value = it },
                            startDate = selectedTimeDateState.value,
                            endDate = endDateState.value,
                            hasEndDate = hasEndDateState.value,
                            onDateClick = { showStartEndDialog = true }
                        )
                    }
                }

                if (showIconPickerDialog) {
                    TaskIconPickerDialog(
                        selectedIconName = selectedIconName,
                        onIconSelected = { picked ->
                            selectedIconNameState.value = picked
                            updateIconButtonVisual()
                        },
                        onDismiss = { showIconPickerDialogState.value = false }
                    )
                }

                if (showAdvancedNotesDialog) {
                    AdvancedNotesDialog(
                        initialNotes = richNotesState.value,
                        onDismiss = { showAdvancedNotesDialogState.value = false },
                        onDone = { updatedNotes ->
                            richNotesState.value = updatedNotes
                            val plainText = RichTextHelper.parseToRichSpans(updatedNotes).first
                            isUpdatingNotesProgrammatically = true
                            notesInput.setText(plainText)
                            notesInput.setSelection(plainText.length)
                            isUpdatingNotesProgrammatically = false
                        }
                    )
                }

                if (showCustomTimePicker) {
                    CustomTimePickerDialog(
                        initialHour = selectedHourState.value,
                        initialMinute = selectedMinuteState.value,
                        onDismiss = { showCustomTimePicker = false },
                        onTimeSelected = { h, m ->
                            selectedHourState.value = h
                            selectedMinuteState.value = m
                            showCustomTimePicker = false
                        }
                    )
                }

                if (showAdditionalAlertTimePicker) {
                    CustomTimePickerDialog(
                        initialHour = selectedHourState.value,
                        initialMinute = selectedMinuteState.value,
                        onDismiss = { showAdditionalAlertTimePicker = false },
                        onTimeSelected = { h, m ->
                            additionalAlertsList.add(CustomAlert(h, m, true))
                            showAdditionalAlertTimePicker = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(
                                        this@QuickCaptureActivity,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    )
                }

                if (showStartEndDialog) {
                    StartEndDialog(
                        startDate = selectedTimeDateState.value,
                        endDate = endDateState.value ?: selectedTimeDateState.value.plusDays(7),
                        hasEndDate = hasEndDateState.value,
                        onHasEndDateChange = { hasEndDateState.value = it },
                        onDismiss = { showStartEndDialog = false },
                        onStartDateClick = {
                            editingDateType = "START"
                            showDatePickerDialog = true
                        },
                        onEndDateClick = {
                            editingDateType = "END"
                            showDatePickerDialog = true
                        }
                    )
                }

                if (showDatePickerDialog) {
                    val initialSelectedMillis = if (editingDateType == "START") {
                        selectedTimeDateState.value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    } else {
                        (endDateState.value ?: selectedTimeDateState.value.plusDays(7))
                            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    }
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = initialSelectedMillis
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val pickedDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                                    if (editingDateType == "START") {
                                        selectedTimeDateState.value = pickedDate
                                    } else {
                                        endDateState.value = pickedDate
                                    }
                                }
                                showDatePickerDialog = false
                            }) {
                                Text("OK", color = Blue500)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePickerDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        },
                        colors = DatePickerDefaults.colors(containerColor = White)
                    ) {
                        DatePicker(
                            state = datePickerState,
                            colors = DatePickerDefaults.colors(
                                containerColor = White,
                                titleContentColor = Blue500,
                                headlineContentColor = TextPrimary,
                                weekdayContentColor = TextSecondary,
                                subheadContentColor = TextSecondary,
                                navigationContentColor = TextPrimary,
                                yearContentColor = TextPrimary,
                                currentYearContentColor = Blue500,
                                selectedYearContainerColor = Blue500,
                                selectedYearContentColor = White,
                                dayContentColor = TextPrimary,
                                selectedDayContainerColor = Blue500,
                                selectedDayContentColor = White,
                                todayContentColor = Blue500,
                                todayDateBorderColor = Blue500,
                                dividerColor = Divider
                            )
                        )
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val focused = currentFocus ?: titleInput
        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    private fun updateIconButtonVisual() {
        val option = TaskIcons.getOptionByName(selectedIconNameState.value)
        when {
            option?.drawableRes != null -> {
                iconImageView.setImageResource(option.drawableRes)
                iconImageView.setColorFilter(palette.accent)
            }
            option?.icon != null -> {
                val sizePx = dp(19f).coerceAtLeast(24)
                val bmp = TaskIcons.renderImageVectorToBitmap(option.icon, sizePx, palette.accent)
                iconImageView.setImageBitmap(bmp)
                iconImageView.clearColorFilter()
            }
            else -> {
                iconImageView.setImageResource(R.drawable.acta_add_icon)
                iconImageView.setColorFilter(palette.mutedText)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSubTaskRow() {
        subtasksSection.visibility = View.VISIBLE
        hasSubtasksState.value = true

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
                    hasSubtasksState.value = false
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

        val lum = (0.299f * Color.red(palette.background) +
            0.587f * Color.green(palette.background) +
            0.114f * Color.blue(palette.background)) / 255f
        val isDarkBg = lum <= 0.5f
        val iconBtnColor = if (isDarkBg) {
            blendColor(palette.background, Color.WHITE, 0.08f)
        } else {
            blendColor(palette.background, Color.BLACK, 0.06f)
        }
        iconButton.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10f).toFloat()
            setColor(iconBtnColor)
        }

        titleInput.setTextColor(palette.text)
        titleInput.setHintTextColor(palette.hintText)

        notesInput.setTextColor(palette.mutedText)
        notesInput.setHintTextColor(palette.hintText)

        advancedNotesText.setTextColor(palette.mutedText)
        advancedNotesArrow.setColorFilter(palette.mutedText)

        subtasksTopDivider.setBackgroundColor(palette.border)
        subtasksBottomDivider.setBackgroundColor(palette.border)

        addSubtaskButton.setColorFilter(palette.text)
        advancedToggleText.setTextColor(palette.text)
        advancedToggleArrow.setColorFilter(palette.text)
        saveButton.setColorFilter(palette.accent)
    }

    private fun blendColor(c1: Int, c2: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = (Color.red(c1) * inv + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inv + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inv + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
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
        val finalNotes = richNotesState.value.trim().ifEmpty {
            notesInput.text?.toString()?.trim().orEmpty()
        }
        val subTaskTitles = subtaskRows
            .map { it.input.text?.toString()?.trim().orEmpty() }
            .filter { it.isNotEmpty() }

        val iconName = selectedIconNameState.value
        val whenEnabled = whenEnabledState.value
        val finalTimeHour = if (whenEnabled) selectedHourState.value else null
        val finalTimeMinute = if (whenEnabled) selectedMinuteState.value else null
        val repeatMode = repeatModeState.value
        val repeatInterval = repeatIntervalState.value
        val repeatDaysSet = repeatDaysSetState.value
        val startDate = selectedTimeDateState.value
        val finalRepeatDays = if (repeatMode == "WEEKLY" && repeatDaysSet.isEmpty()) {
            startDate.dayOfWeek.value.toString()
        } else {
            repeatDaysSet.sorted().joinToString(",")
        }
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = if (hasEndDateState.value) {
            endDateState.value?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        } else {
            null
        }
        val alertEnabled = alertEnabledState.value
        val additionalAlerts = additionalAlertsList.toList()

        isSaving = true
        updateSaveState()

        val appContext = applicationContext
        WidgetRenderer.WORKER.execute {
            val saved = runCatching {
                runBlocking(Dispatchers.IO) {
                    val taskDao = AppDatabase.getInstance(appContext).taskDao()
                    val maxOrder = taskDao.getMaxOrderIndex() ?: 0
                    val newTask = Task(
                        title = title,
                        description = finalNotes,
                        date = startMillis,
                        isCompleted = false,
                        isHighlighted = false,
                        iconName = iconName,
                        orderIndex = maxOrder + 1,
                        taskTimeHour = finalTimeHour,
                        taskTimeMinute = finalTimeMinute,
                        repeatMode = repeatMode,
                        repeatInterval = repeatInterval,
                        repeatDays = finalRepeatDays,
                        endDate = endMillis,
                        alertEnabled = alertEnabled,
                        additionalAlerts = CustomAlert.serializeList(additionalAlerts)
                    )
                    val taskId = taskDao.insert(newTask).toInt()
                    for (subTitle in subTaskTitles) {
                        taskDao.insertSubTask(SubTask(taskId = taskId, title = subTitle))
                    }
                    if (isAnyHideTaskModeActive()) {
                        registerHideModeTaskId(appContext, taskId)
                    }
                    if (alertEnabled || additionalAlerts.any { it.isEnabled }) {
                        AlarmScheduler.scheduleTaskAlarm(appContext, newTask.copy(id = taskId))
                    }
                }
                WidgetRenderer.refreshAll(appContext)
                true
            }.getOrDefault(false)

            runOnUiThread {
                if (saved) {
                    Toast.makeText(appContext, "Task added", Toast.LENGTH_SHORT).show()
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
