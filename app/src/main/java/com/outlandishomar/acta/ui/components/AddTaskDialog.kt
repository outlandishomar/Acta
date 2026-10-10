package com.outlandishomar.acta.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import com.outlandishomar.acta.ui.theme.*
import com.outlandishomar.acta.data.AppSettings
import kotlinx.coroutines.launch

private class SubTaskData(val id: String = UUID.randomUUID().toString(), initialTitle: String) {
    var title by mutableStateOf(initialTitle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    initialTitle: String = "",
    initialNotes: String = "",
    initialSubTasks: List<String> = emptyList(),
    initialIconName: String? = null,
    isHighlighted: Boolean = false,
    isEditMode: Boolean = false,
    initialTimeHour: Int? = null,
    initialTimeMinute: Int? = null,
    initialRepeatMode: String = "ONCE",
    initialRepeatInterval: Int = 1,
    initialRepeatDays: String = "",
    initialStartDate: LocalDate = LocalDate.now(),
    initialEndDate: LocalDate? = null,
    initialAlertEnabled: Boolean = false,
    initialAdditionalAlerts: List<com.outlandishomar.acta.data.model.CustomAlert> = emptyList(),
    enableAdvanced: Boolean = true,
    onNavigateToPro: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onAdd: (title: String, notes: String, subTasks: List<String>, iconName: String?,
            timeHour: Int?, timeMinute: Int?,
            repeatMode: String, repeatInterval: Int, repeatDays: String,
            startDate: LocalDate, endDate: LocalDate?, alertEnabled: Boolean,
            additionalAlerts: List<com.outlandishomar.acta.data.model.CustomAlert>,
            editType: EditType) -> Unit
) {
    var showEditRepeatingDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Hide keyboard immediately when the sheet starts to hide (swipe down or tap outside)
    LaunchedEffect(sheetState.targetValue) {
        if (sheetState.targetValue == SheetValue.Hidden) {
            keyboardController?.hide()
        }
    }

    // Alert state
    var alertEnabled by remember { mutableStateOf(initialAlertEnabled) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                alertEnabled = true
            } else {
                alertEnabled = false
            }
        }
    )
    
    val additionalAlerts = remember {
        mutableStateListOf(*initialAdditionalAlerts.toTypedArray())
    }

    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf(initialNotes) }
    
    var selectedIconName by remember { 
        mutableStateOf<String?>(
            if (isEditMode) initialIconName 
            else null
        ) 
    }
    var showAdditionalAlertTimePicker by remember { mutableStateOf(false) }
    var showAdvancedNotesDialog by remember { mutableStateOf(false) }
    
    val subTasks = remember { 
        mutableStateListOf<SubTaskData>().apply { 
            initialSubTasks.forEach { 
                add(SubTaskData(initialTitle = it)) 
            }
        } 
    }

    // --- When? state ---
    var whenEnabled by remember { mutableStateOf(initialTimeHour != null) }
    val now = remember { LocalTime.now() }
    val roundedMinute = remember { ((now.minute + 4) / 5) * 5 }
    val initialHour = remember { if (roundedMinute >= 60) (now.hour + 1) % 24 else now.hour }
    val initialMin = remember { roundedMinute % 60 }
    var selectedHour by remember { mutableStateOf(initialTimeHour ?: initialHour) }
    var selectedMinute by remember { mutableStateOf(initialTimeMinute ?: initialMin) }
    var showCustomTimePicker by remember { mutableStateOf(false) }
    var selectedTimeDate by remember { mutableStateOf(initialStartDate) }
    
    // --- Start/End Date state ---
    var endDate by remember { mutableStateOf(initialEndDate) }
    var hasEndDate by remember { mutableStateOf(initialEndDate != null) }
    var showStartEndDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var editingDateType by remember { mutableStateOf("START") } // "START" or "END"

    // --- How often? state ---
    var repeatMode by remember { mutableStateOf(initialRepeatMode) }
    var repeatInterval by remember { mutableStateOf(initialRepeatInterval) }
    var repeatDaysSet by remember(initialRepeatMode, initialRepeatDays, initialStartDate) {
        mutableStateOf(
            if (initialRepeatDays.isNotBlank()) {
                initialRepeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            } else if (initialRepeatMode == "WEEKLY") {
                setOf(initialStartDate.dayOfWeek.value)
            } else {
                emptySet()
            }
        )
    }
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            // Delay to allow the ModalBottomSheet animation to finish smoothly
            // before bringing up the keyboard, preventing jumpy UI.
            kotlinx.coroutines.delay(400)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val lazyListState = rememberLazyListState()
    var previousSubTaskCount by remember { mutableIntStateOf(subTasks.size) }
    LaunchedEffect(subTasks.size) {
        if (subTasks.size > previousSubTaskCount) {
            lazyListState.animateScrollToItem(subTasks.size - 1)
        }
        previousSubTaskCount = subTasks.size
    }
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = subTasks.indexOfFirst { it.id == from.key }
        val toIndex = subTasks.indexOfFirst { it.id == to.key }
        if (fromIndex != -1 && toIndex != -1) {
            val moved = subTasks.removeAt(fromIndex)
            subTasks.add(toIndex, moved)
        }
    }

    // Arrow rotation: 0° = pointing up (collapsed), 180° = pointing down (expanded)
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrowRotation"
    )
    val performSubmit = { editType: EditType ->
        val validSubTasks = subTasks.map { it.title }.filter { it.isNotBlank() }
        val finalTimeHour = if (whenEnabled) selectedHour else null
        val finalTimeMinute = if (whenEnabled) selectedMinute else null
        val finalRepeatDays = if (repeatMode == "WEEKLY" && repeatDaysSet.isEmpty()) {
            selectedTimeDate.dayOfWeek.value.toString()
        } else {
            repeatDaysSet.sorted().joinToString(",")
        }
        onAdd(
            title.trim(), notes, validSubTasks, selectedIconName,
            finalTimeHour, finalTimeMinute,
            repeatMode, repeatInterval, finalRepeatDays,
            selectedTimeDate, if (hasEndDate) endDate else null,
            alertEnabled,
            additionalAlerts,
            editType
        )
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { 
            keyboardController?.hide()
            onDismiss() 
        },
            sheetState = sheetState,
            containerColor = White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextPrimary) }
        ) {
        BackHandler {
            if (isExpanded) {
                isExpanded = false
            } else {
                keyboardController?.hide()
                onDismiss()
            }
        }
        
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val maxSheetHeight = this.maxHeight
            val targetMinHeight by animateDpAsState(
                targetValue = if (enableAdvanced && isExpanded) (maxSheetHeight - 24.dp) else 0.dp,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 300f
                ),
                label = "sheetHeight"
            )
            
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = targetMinHeight)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                ) {
                    // Expand/Collapse toggle row at the top right
                    if (enableAdvanced) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isExpanded = !isExpanded }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Advanced",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    fontFamily = SfProText,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = TextPrimary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(arrowRotation)
                                )
                            }
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                    // Main Task text field
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProText,
                                textDirection = androidx.compose.ui.text.style.TextDirection.Content,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Left
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue500),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (title.isEmpty()) {
                                        Text(
                                            "What would you like to do?",
                                            color = TextSecondary.copy(alpha = 0.5f),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    val (displayNotesText, displayNotesSpans) = remember(notes) {
                        RichTextHelper.parseToRichSpans(notes)
                    }

                    androidx.compose.foundation.text.BasicTextField(
                        value = displayNotesText,
                        onValueChange = { newText ->
                            if (displayNotesSpans.isEmpty()) {
                                notes = newText
                            } else {
                                val diff = newText.length - displayNotesText.length
                                val newSpans = if (diff > 0) {
                                    RichTextHelper.adjustSpansForInsertion(displayNotesSpans, 0, diff, false, false, false, false, null, 0)
                                } else if (diff < 0) {
                                    RichTextHelper.adjustSpansForDeletion(displayNotesSpans, 0, -diff)
                                } else displayNotesSpans
                                notes = RichTextHelper.serializeToHtml(newText, newSpans)
                            }
                        },
                        visualTransformation = remember(displayNotesSpans) { RichVisualTransformation { displayNotesSpans } },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal,
                            fontFamily = SfProText,
                            textDirection = androidx.compose.ui.text.style.TextDirection.Content,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Left
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue500),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (displayNotesText.isEmpty()) {
                                    Text(
                                        "notes",
                                        color = TextSecondary.copy(alpha = 0.5f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                innerTextField()
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        )
                    )

                    // Advanced notes button placed below notes on the left
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAdvancedNotesDialog = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                fontFamily = SfProText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Advanced Notes",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Sub-tasks section
                    if (subTasks.isNotEmpty()) {
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .heightIn(max = 300.dp)
                        ) {
                            items(subTasks, key = { it.id }) { subTaskItem ->
                                val itemFocusRequester = remember { FocusRequester() }

                                LaunchedEffect(Unit) {
                                    if (subTaskItem.title.isEmpty()) {
                                        itemFocusRequester.requestFocus()
                                    }
                                }

                                ReorderableItem(reorderState, key = subTaskItem.id) { isDragging ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        // Drag handle
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "Reorder",
                                            tint = TextSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .draggableHandle()
                                        )

                                        // Connector line visual
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(40.dp)
                                                .padding(end = 0.dp)
                                        )

                                        OutlinedTextField(
                                            value = subTaskItem.title,
                                            onValueChange = { subTaskItem.title = it },
                                            placeholder = {
                                                Text(
                                                    "type...",
                                                    color = TextSecondary.copy(alpha = 0.5f),
                                                    fontSize = 14.sp
                                                )
                                            },
                                            singleLine = false,
                                            maxLines = 5,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 52.dp)
                                                .focusRequester(itemFocusRequester),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Blue500,
                                                unfocusedBorderColor = Divider,
                                                cursorColor = Blue500,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = TextPrimary, 
                                                fontSize = 14.sp, 
                                                fontFamily = SfProText,
                                                textDirection = androidx.compose.ui.text.style.TextDirection.Content,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Left
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Default
                                            )
                                        )

                                        IconButton(
                                            onClick = { subTasks.remove(subTaskItem) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Remove subtask",
                                                tint = Red500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                    }

                    // --- Expanded content: When? and How often? ---
                    if (enableAdvanced) {
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 300f
                                )
                            ) + fadeIn(animationSpec = tween(200)),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 300f
                                )
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(Modifier.height(16.dp))
                                WhenSection(
                                    enabled = whenEnabled,
                                    onToggle = { whenEnabled = it },
                                    selectedHour = selectedHour,
                                    selectedMinute = selectedMinute,
                                    onTimeChange = { h, m -> selectedHour = h; selectedMinute = m },
                                    onAdjustTimeClick = { showCustomTimePicker = true },
                                    alertEnabled = alertEnabled,
                                    onAlertEnabledChange = { enabled ->
                                        if (enabled) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                                    alertEnabled = true
                                                } else {
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            } else {
                                                alertEnabled = true
                                            }
                                        } else {
                                            alertEnabled = false
                                        }
                                    },
                                    additionalAlerts = additionalAlerts,
                                    onToggleAdditionalAlert = { index, enabled ->
                                        if (index in additionalAlerts.indices) {
                                            if (enabled) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                                        additionalAlerts[index] = additionalAlerts[index].copy(isEnabled = true)
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                } else {
                                                    additionalAlerts[index] = additionalAlerts[index].copy(isEnabled = true)
                                                }
                                            } else {
                                                additionalAlerts.removeAt(index)
                                            }
                                        }
                                    },
                                    onAddAlertClick = {
                                        showAdditionalAlertTimePicker = true
                                    }
                                )

                                Spacer(Modifier.height(24.dp))

                                HowOftenSection(
                                    repeatMode = repeatMode,
                                    onRepeatModeChange = { mode ->
                                        repeatMode = mode
                                        if (mode == "WEEKLY" && repeatDaysSet.isEmpty()) {
                                            repeatDaysSet = setOf(selectedTimeDate.dayOfWeek.value)
                                        }
                                        if (mode != "ONCE") {
                                            coroutineScope.launch {
                                                delay(150)
                                                scrollState.animateScrollTo(scrollState.maxValue)
                                            }
                                        }
                                    },
                                    repeatInterval = repeatInterval,
                                    onRepeatIntervalChange = { repeatInterval = it },
                                    repeatDaysSet = repeatDaysSet,
                                    onRepeatDaysChange = { repeatDaysSet = it },
                                    startDate = selectedTimeDate,
                                    endDate = endDate,
                                    hasEndDate = hasEndDate,
                                    onDateClick = { showStartEndDialog = true }
                                )
                            }
                        }
                    }
                } // Close inner Column

                // Toolbar (+ and ✓ buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Add sub-task button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                subTasks.add(SubTaskData(initialTitle = ""))
                                coroutineScope.launch {
                                    lazyListState.animateScrollToItem(subTasks.size - 1)
                                }
                            },
                            enabled = true,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add subtask",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Submit/Done Button
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            if (title.isNotBlank()) {
                                if (isEditMode && initialRepeatMode != "ONCE") {
                                    showEditRepeatingDialog = true
                                } else {
                                    performSubmit(EditType.ALL)
                                }
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Done",
                            tint = Blue500,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEditRepeatingDialog) {
        BasicAlertDialog(
            onDismissRequest = { showEditRepeatingDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Surface(
                color = White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text(
                        text = "Edit Task",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "This is a repeating task.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(24.dp))

                    val crimsonRed = Color(0xFFE63946)

                    TextButton(
                        onClick = {
                            showEditRepeatingDialog = false
                            performSubmit(EditType.INSTANCE)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Edit this task only",
                            color = crimsonRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            showEditRepeatingDialog = false
                            performSubmit(EditType.FUTURE)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Edit all future tasks",
                            color = crimsonRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            showEditRepeatingDialog = false
                            performSubmit(EditType.ALL)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Edit all tasks",
                            color = crimsonRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { showEditRepeatingDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }



    if (showAdvancedNotesDialog) {
        AdvancedNotesDialog(
            initialNotes = notes,
            onDismiss = { showAdvancedNotesDialog = false },
            onDone = { updatedNotes ->
                notes = updatedNotes
            }
        )
    }

    if (showCustomTimePicker) {
        CustomTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showCustomTimePicker = false },
            onTimeSelected = { h, m ->
                selectedHour = h
                selectedMinute = m
                showCustomTimePicker = false
            }
        )
    }

    if (showAdditionalAlertTimePicker) {
        CustomTimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismiss = { showAdditionalAlertTimePicker = false },
            onTimeSelected = { h, m ->
                additionalAlerts.add(com.outlandishomar.acta.data.model.CustomAlert(h, m, true))
                showAdditionalAlertTimePicker = false
                
                // Also request notification permission if not already granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        )
    }

    if (showStartEndDialog) {
        StartEndDialog(
            startDate = selectedTimeDate,
            endDate = endDate ?: selectedTimeDate.plusDays(7),
            hasEndDate = hasEndDate,
            onHasEndDateChange = { hasEndDate = it },
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
            selectedTimeDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            (endDate ?: selectedTimeDate.plusDays(7)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        if (editingDateType == "START") {
                            selectedTimeDate = pickedDate
                        } else {
                            endDate = pickedDate
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
            colors = DatePickerDefaults.colors(
                containerColor = White
            )
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

// ─── "When?" Section ───────────────────────────────────────────────────────────

@Composable
private fun WhenSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    selectedHour: Int,
    selectedMinute: Int,
    onTimeChange: (Int, Int) -> Unit,
    onAdjustTimeClick: () -> Unit,
    alertEnabled: Boolean,
    onAlertEnabledChange: (Boolean) -> Unit,
    additionalAlerts: List<com.outlandishomar.acta.data.model.CustomAlert>,
    onToggleAdditionalAlert: (Int, Boolean) -> Unit,
    onAddAlertClick: () -> Unit,
    onProClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row: "When?" on left, toggle circle on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "When?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Toggle circle
            ToggleCircle(
                checked = enabled,
                onCheckedChange = { onToggle(it) }
            )
        }

        // Expandable time picker content
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimeScrollPicker(
                    selectedHour = selectedHour,
                    selectedMinute = selectedMinute,
                    onTimeChange = onTimeChange
                )

                Spacer(Modifier.height(12.dp))

                // Adjust Time Button
                OutlinedButton(
                    onClick = onAdjustTimeClick,
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Blue500
                    ),
                    border = BorderStroke(1.dp, Blue500)
                ) {
                    Text("Adjust Time", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(32.dp))

                // Alerts Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Needs alerts?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(16.dp))

                    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
                    val timeString = LocalTime.of(selectedHour, selectedMinute).format(timeFormatter)

                    AlertItem(
                        label = timeString,
                        enabled = alertEnabled,
                        onToggle = onAlertEnabledChange
                    )
                    Spacer(Modifier.height(12.dp))

                    additionalAlerts.forEachIndexed { index, customAlert ->
                        val alertTimeString = LocalTime.of(customAlert.hour, customAlert.minute).format(timeFormatter)
                        AlertItem(
                            label = alertTimeString,
                            enabled = customAlert.isEnabled,
                            onToggle = { enabled -> onToggleAdditionalAlert(index, enabled) },
                            alpha = 1f
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // Add Alert Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Grey100)
                            .clickable { onAddAlertClick() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Alert",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Add Alert",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertItem(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    alpha: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Grey100)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (enabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                color = if (enabled) TextPrimary else TextSecondary,
                textDecoration = if (enabled) androidx.compose.ui.text.style.TextDecoration.None else androidx.compose.ui.text.style.TextDecoration.LineThrough,
                fontWeight = FontWeight.Medium
            )
        }

        Icon(
            imageVector = if (enabled) Icons.Filled.Close else Icons.Filled.Add,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToggleCircle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) Blue500 else TextPrimary,
        animationSpec = tween(200),
        label = "toggleBorder"
    )
    val fillColor by animateColorAsState(
        targetValue = if (checked) Blue500 else Color.Transparent,
        animationSpec = tween(200),
        label = "toggleFill"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = fillColor,
        border = BorderStroke(1.5.dp, borderColor),
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.size(22.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = scaleOut(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Enabled",
                    tint = White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeScrollPicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    // Generate standard 15-min time slots, plus inject the custom time if needed
    val timeSlots = remember(selectedHour, selectedMinute) {
        val slots = (0..23).flatMap { h ->
            (0..45 step 15).map { m -> h to m }
        }.toMutableList()
        
        if (selectedMinute % 15 != 0) {
            val roundedMin = (selectedMinute / 15) * 15
            val indexToReplace = slots.indexOfFirst { it.first == selectedHour && it.second == roundedMin }
            if (indexToReplace != -1) {
                slots[indexToReplace] = selectedHour to selectedMinute
            }
        }
        slots.toList()
    }

    val selectedIndex = remember(selectedHour, selectedMinute) {
        // Search for the exact time pair (works for both standard 15-min and custom adjusted times)
        timeSlots.indexOfFirst { it.first == selectedHour && it.second == selectedMinute }.coerceAtLeast(0)
    }

    // We want the initial selected item to be centered. With 88dp padding, the center item is directly at the start of the content area.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )
    val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)

    // Sync state when adjusted externally (e.g., from CustomTimePickerDialog)
    LaunchedEffect(selectedHour, selectedMinute) {
        if (!listState.isScrollInProgress) {
            val targetIndex = timeSlots.indexOfFirst { it.first == selectedHour && it.second == selectedMinute }.coerceAtLeast(0)
            
            // Check current centered item
            val center = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
            val currentCenterIndex = listState.layoutInfo.visibleItemsInfo.minByOrNull { 
                kotlin.math.abs((it.offset + it.size / 2f) - center) 
            }?.index ?: 0
            
            if (currentCenterIndex != targetIndex) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Auto-select center item when scroll settles
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val center = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
            val centerIndex = listState.layoutInfo.visibleItemsInfo.minByOrNull { 
                kotlin.math.abs((it.offset + it.size / 2f) - center) 
            }?.index
            
            if (centerIndex != null && centerIndex in timeSlots.indices) {
                val (h, m) = timeSlots[centerIndex]
                onTimeChange(h, m)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        // Selection indicator lines
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.5f)
                .height(44.dp)
                .background(Grey100.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(timeSlots.size) { index ->
                val (h, m) = timeSlots[index]
                val displayHour = if (h % 12 == 0) 12 else h % 12
                val amPm = if (h < 12) "AM" else "PM"
                val timeText = String.format("%02d:%02d %s", displayHour, m, amPm)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(44.dp)
                        .graphicsLayer {
                            val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (itemInfo != null) {
                                val center = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
                                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                val distance = kotlin.math.abs(center - itemCenter)
                                val maxDistance = listState.layoutInfo.viewportSize.height / 2f
                                val ratio = (1f - (distance / maxDistance)).coerceIn(0f, 1f)
                                val scale = 0.8f + (0.2f * ratio)
                                scaleX = scale
                                scaleY = scale
                                alpha = 0.3f + (0.7f * ratio)
                            } else {
                                scaleX = 0.8f
                                scaleY = 0.8f
                                alpha = 0.3f
                            }
                        }
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            val currentCenter = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
                            val clickedItemCenter = listState.layoutInfo.visibleItemsInfo.find { it.index == index }?.let { it.offset + it.size / 2f }
                            if (clickedItemCenter != null && kotlin.math.abs(clickedItemCenter - currentCenter) < 10f) {
                                // Already centered, do nothing special, onTimeChange handled by scroll settle
                            } else {
                                // Animate to this item
                                onTimeChange(h, m)
                            }
                        }
                ) {
                    Text(
                        text = timeText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─── "How often?" Section ──────────────────────────────────────────────────────

@Composable
private fun HowOftenSection(
    repeatMode: String,
    onRepeatModeChange: (String) -> Unit,
    repeatInterval: Int,
    onRepeatIntervalChange: (Int) -> Unit,
    repeatDaysSet: Set<Int>,
    onRepeatDaysChange: (Set<Int>) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate?,
    hasEndDate: Boolean,
    onDateClick: () -> Unit,
    onProClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "How often?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (repeatMode != "ONCE") {
                val formatter = DateTimeFormatter.ofPattern("M/d/yy")
                val dateText = if (hasEndDate && endDate != null) {
                    "${startDate.format(formatter)} - ${endDate.format(formatter)}"
                } else {
                    "Starting on ${startDate.format(formatter)}"
                }
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Grey100)
                            .clickable { onDateClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dateText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Segmented buttons
        val modes = listOf("ONCE" to "Once", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Grey100)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            modes.forEach { (mode, label) ->
                val isSelected = repeatMode == mode
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) Modifier.background(Blue500)
                            else Modifier
                        )
                        .clickable { onRepeatModeChange(mode) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) White else Blue500
                    )
                }
            }
        }

        // Sub-options based on mode
        AnimatedVisibility(
            visible = repeatMode != "ONCE",
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                when (repeatMode) {
                    "DAILY" -> DailyOptions(repeatInterval, onRepeatIntervalChange)
                    "WEEKLY" -> WeeklyOptions(repeatInterval, onRepeatIntervalChange, repeatDaysSet, onRepeatDaysChange)
                    "MONTHLY" -> MonthlyOptions(repeatInterval, onRepeatIntervalChange)
                }
            }
        }
    }
}

@Composable
private fun DailyOptions(interval: Int, onIntervalChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Grey100)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (interval == 1) "Every day" else "Every $interval days",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            IntervalStepper(
                value = interval,
                onChange = onIntervalChange,
                enabled = true,
                maxValue = 100
            )
        }
    }
}

@Composable
private fun WeeklyOptions(
    interval: Int,
    onIntervalChange: (Int) -> Unit,
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Grey100)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (interval == 1) "Every week" else "Every $interval weeks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            IntervalStepper(interval, onIntervalChange, enabled = true)
        }

        Spacer(Modifier.height(8.dp))

        // Day-of-week chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val startDay = AppSettings.firstDayOfWeek
            val daysOrder = (0..6).map { startDay.plus(it.toLong()) }
            daysOrder.forEach { day ->
                val isSelected = selectedDays.contains(day.value)
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) Blue500 else Grey100,
                    label = "dayChipBg"
                )
                val chipText by animateColorAsState(
                    targetValue = if (isSelected) White else TextSecondary,
                    label = "dayChipText"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipBg)
                        .clickable {
                            val newSet = selectedDays.toMutableSet()
                            if (isSelected) newSet.remove(day.value) else newSet.add(day.value)
                            onDaysChange(newSet)
                        }
                ) {
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipText
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyOptions(interval: Int, onIntervalChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Grey100)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (interval == 1) "Every month" else "Every $interval months",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        IntervalStepper(interval, onIntervalChange, enabled = true)
    }
}

@Composable
private fun IntervalStepper(
    value: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
    maxValue: Int = 100
) {
    val canDecrease = enabled && value > 1
    val canIncrease = enabled && value < maxValue

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Divider.copy(alpha = 0.5f))
    ) {
        IconButton(
            onClick = { if (canDecrease) onChange(value - 1) },
            modifier = Modifier.size(40.dp),
            enabled = canDecrease
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Decrease",
                tint = if (canDecrease) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp)
                .background(TextSecondary.copy(alpha = 0.3f))
        )

        IconButton(
            onClick = { if (canIncrease) onChange(value + 1) },
            modifier = Modifier.size(40.dp),
            enabled = canIncrease
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Increase",
                tint = if (canIncrease) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    var selectedH by remember { mutableStateOf(if (initialHour % 12 == 0) 12 else initialHour % 12) }
    var selectedM by remember { mutableStateOf(initialMinute) }
    var selectedAmPm by remember { mutableStateOf(if (initialHour < 12) 0 else 1) } // 0 = AM, 1 = PM

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TopBarBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextPrimary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Wheel
                WheelPicker(
                    items = (1..12).map { String.format("%02d", it) },
                    initialIndex = selectedH - 1,
                    onItemSelected = { selectedH = it + 1 },
                    modifier = Modifier.weight(1f)
                )

                // Minute Wheel
                WheelPicker(
                    items = (0..59).map { String.format("%02d", it) },
                    initialIndex = selectedM,
                    onItemSelected = { selectedM = it },
                    modifier = Modifier.weight(1f)
                )

                // AM/PM Wheel
                WheelPicker(
                    items = listOf("AM", "PM"),
                    initialIndex = selectedAmPm,
                    onItemSelected = { selectedAmPm = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done Button
            Row(modifier = Modifier.fillMaxWidth().padding(end = 24.dp), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = {
                        val h24 = when {
                            selectedAmPm == 0 && selectedH == 12 -> 0
                            selectedAmPm == 1 && selectedH < 12 -> selectedH + 12
                            else -> selectedH
                        }
                        onTimeSelected(h24, selectedM)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TopBarBackground)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val center = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
            val centerIndex = listState.layoutInfo.visibleItemsInfo.minByOrNull { 
                kotlin.math.abs((it.offset + it.size / 2f) - center) 
            }?.index
            
            if (centerIndex != null && centerIndex in items.indices) {
                onItemSelected(centerIndex)
            }
        }
    }

    Box(
        modifier = modifier.height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator lines
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .height(44.dp)
                .background(Grey100.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        )
        
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .graphicsLayer {
                            val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (itemInfo != null) {
                                val center = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
                                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                val distance = kotlin.math.abs(center - itemCenter)
                                val maxDistance = listState.layoutInfo.viewportSize.height / 2f
                                val ratio = (1f - (distance / maxDistance)).coerceIn(0f, 1f)
                                val scale = 0.8f + (0.2f * ratio)
                                scaleX = scale
                                scaleY = scale
                                alpha = 0.3f + (0.7f * ratio)
                            } else {
                                scaleX = 0.8f
                                scaleY = 0.8f
                                alpha = 0.3f
                            }
                        }
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            val currentCenter = listState.layoutInfo.viewportStartOffset + (listState.layoutInfo.viewportSize.height / 2f)
                            val clickedItemCenter = listState.layoutInfo.visibleItemsInfo.find { it.index == index }?.let { it.offset + it.size / 2f }
                            if (clickedItemCenter != null && kotlin.math.abs(clickedItemCenter - currentCenter) < 10f) {
                                // Already centered, do nothing
                            } else {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                ) {
                    Text(
                        text = items[index],
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartEndDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    hasEndDate: Boolean,
    onHasEndDateChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextPrimary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            // Start Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Start",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 56.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Grey100)
                        .clickable { onStartDateClick() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = startDate.format(formatter),
                        fontSize = 18.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // End Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasEndDate,
                        onCheckedChange = onHasEndDateChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFE63946),
                            uncheckedColor = TextSecondary.copy(alpha = 0.5f),
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "End",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasEndDate) TextPrimary else TextSecondary.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasEndDate) Grey100 else Color.Transparent)
                        .clickable(enabled = hasEndDate) { onEndDateClick() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = endDate.format(formatter),
                        fontSize = 18.sp,
                        color = if (hasEndDate) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedNotesDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit
) {
    val (parsedText, parsedSpans) = remember(initialNotes) {
        RichTextHelper.parseToRichSpans(initialNotes)
    }
    var spans by remember { mutableStateOf(parsedSpans) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = parsedText,
                selection = TextRange(parsedText.length)
            )
        )
    }

    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }
    var isStrikethroughActive by remember { mutableStateOf(false) }
    var activeHighlightColor by remember { mutableStateOf<Color?>(null) }
    var activeHeadingLevel by remember { mutableStateOf(0) }
    var showHeadingMenu by remember { mutableStateOf(false) }
    var showHighlightPalette by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val toolbarScrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Auto-scroll when typing near or at the bottom to maintain comfortable typing room above the toolbar
    LaunchedEffect(textFieldValue.selection, textFieldValue.text) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        val cursorOffset = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        if (cursorOffset <= layout.layoutInput.text.length) {
            try {
                val cursorRect = layout.getCursorRect(cursorOffset)
                val cursorTop = cursorRect.top
                val cursorBottom = cursorRect.bottom
                val viewportHeight = scrollState.viewportSize
                if (viewportHeight > 0) {
                    val currentScroll = scrollState.value
                    val cursorScreenY = cursorBottom - currentScroll
                    val bottomComfortMargin = with(density) { 140.dp.toPx() }
                    val topComfortMargin = with(density) { 40.dp.toPx() }

                    if (cursorScreenY > viewportHeight - bottomComfortMargin) {
                        val targetScroll = (cursorBottom - (viewportHeight - bottomComfortMargin)).toInt().coerceIn(0, scrollState.maxValue)
                        scrollState.animateScrollTo(targetScroll)
                    } else if (cursorTop - currentScroll < topComfortMargin) {
                        val targetScroll = (cursorTop - topComfortMargin).toInt().coerceIn(0, scrollState.maxValue)
                        scrollState.animateScrollTo(targetScroll)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Update active indicators based on cursor position
    LaunchedEffect(textFieldValue.selection) {
        val sel = textFieldValue.selection
        val pos = sel.min
        val text = textFieldValue.text
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', pos).let { if (it == -1) text.length else it }

        val activeSpansAtCursor = spans.filter { it.start <= pos && it.end >= pos }
        val lineSpans = spans.filter { it.start < lineEnd && it.end > lineStart }

        isBoldActive = activeSpansAtCursor.any { it.isBold }
        isItalicActive = activeSpansAtCursor.any { it.isItalic }
        isUnderlineActive = activeSpansAtCursor.any { it.isUnderline }
        isStrikethroughActive = activeSpansAtCursor.any { it.isStrikethrough }
        activeHeadingLevel = lineSpans.map { it.headingLevel }.filter { it > 0 }.maxOrNull() ?: 0
        activeHighlightColor = activeSpansAtCursor.mapNotNull { it.highlightColor }.firstOrNull()?.let { Color(it.toULong()) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        var imeBottomPx by remember { mutableIntStateOf(0) }

        DisposableEffect(view) {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            val dialogWindow = (parent as? DialogWindowProvider)?.window
            dialogWindow?.let { w ->
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                WindowCompat.setDecorFitsSystemWindows(w, false)
            }

            val listener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                imeBottomPx = imeHeight
                insets
            }
            ViewCompat.setOnApplyWindowInsetsListener(view, listener)
            ViewCompat.requestApplyInsets(view)

            onDispose {
                ViewCompat.setOnApplyWindowInsetsListener(view, null)
            }
        }

        val composeImeBottom = WindowInsets.ime.getBottom(density)
        val effectiveImeBottomPx = maxOf(composeImeBottom, imeBottomPx)
        val isKeyboardOpen = effectiveImeBottomPx > 0
        val imeBottomPaddingDp = with(density) { effectiveImeBottomPx.toDp() }

        BackHandler {
            if (isKeyboardOpen) {
                keyboardController?.hide()
            } else {
                onDismiss()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = imeBottomPaddingDp)
            ) {
                // Top Bar: Back button on the very top left, Done button on the top right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val serialized = RichTextHelper.serializeToHtml(textFieldValue.text, spans)
                            onDone(serialized)
                            onDismiss()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Done",
                            tint = Blue500,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Blank page free-writing notes area with typewriter-style overscroll space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newVal ->
                                val oldVal = textFieldValue
                                val oldText = oldVal.text
                                val newText = newVal.text

                                if (newText.length > oldText.length) {
                                    val insertIndex = oldVal.selection.min
                                    val insertedCount = newText.length - oldText.length
                                    val insertedText = if (insertIndex + insertedCount <= newText.length) {
                                        newText.substring(insertIndex, insertIndex + insertedCount)
                                    } else ""

                                    if (insertedText == "\n") {
                                        val lineStart = oldText.lastIndexOf('\n', (insertIndex - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                        val prevLine = oldText.substring(lineStart, insertIndex)

                                        when {
                                            prevLine == "• " || prevLine == "•" -> {
                                                val cleanText = oldText.substring(0, lineStart) + "\n" + oldText.substring(insertIndex)
                                                val newCursor = lineStart + 1
                                                spans = RichTextHelper.adjustSpansForDeletion(spans, lineStart, insertIndex - lineStart)
                                                textFieldValue = TextFieldValue(cleanText, TextRange(newCursor))
                                                return@BasicTextField
                                            }
                                            prevLine.startsWith("• ") -> {
                                                val bulletPrefix = "• "
                                                val adjustedText = newText.substring(0, insertIndex + 1) + bulletPrefix + newText.substring(insertIndex + 1)
                                                val newCursor = insertIndex + 1 + bulletPrefix.length
                                                spans = RichTextHelper.adjustSpansForInsertion(spans, insertIndex, 1 + bulletPrefix.length, isBoldActive, isItalicActive, isUnderlineActive, isStrikethroughActive, activeHighlightColor, 0)
                                                textFieldValue = TextFieldValue(adjustedText, TextRange(newCursor))
                                                return@BasicTextField
                                            }
                                            prevLine.matches(Regex("^\\d+\\.\\s*$")) -> {
                                                val cleanText = oldText.substring(0, lineStart) + "\n" + oldText.substring(insertIndex)
                                                val newCursor = lineStart + 1
                                                spans = RichTextHelper.adjustSpansForDeletion(spans, lineStart, insertIndex - lineStart)
                                                textFieldValue = TextFieldValue(cleanText, TextRange(newCursor))
                                                return@BasicTextField
                                            }
                                            prevLine.matches(Regex("^(\\d+)\\.\\s+.+")) -> {
                                                val match = Regex("^(\\d+)\\.\\s+").find(prevLine)!!
                                                val num = match.groupValues[1].toIntOrNull() ?: 1
                                                val numberPrefix = "${num + 1}. "
                                                val adjustedText = newText.substring(0, insertIndex + 1) + numberPrefix + newText.substring(insertIndex + 1)
                                                val newCursor = insertIndex + 1 + numberPrefix.length
                                                spans = RichTextHelper.adjustSpansForInsertion(spans, insertIndex, 1 + numberPrefix.length, isBoldActive, isItalicActive, isUnderlineActive, isStrikethroughActive, activeHighlightColor, 0)
                                                textFieldValue = TextFieldValue(adjustedText, TextRange(newCursor))
                                                return@BasicTextField
                                            }
                                            else -> {
                                                spans = RichTextHelper.adjustSpansForInsertion(spans, insertIndex, 1, isBoldActive, isItalicActive, isUnderlineActive, isStrikethroughActive, activeHighlightColor, 0)
                                            }
                                        }
                                    } else {
                                        spans = RichTextHelper.adjustSpansForInsertion(
                                            spans,
                                            insertIndex,
                                            insertedCount,
                                            isBoldActive,
                                            isItalicActive,
                                            isUnderlineActive,
                                            isStrikethroughActive,
                                            activeHighlightColor,
                                            activeHeadingLevel
                                        )
                                    }
                                } else if (newText.length < oldText.length) {
                                    val deleteStart = newVal.selection.min
                                    val deletedCount = oldText.length - newText.length

                                    val lineStart = oldText.lastIndexOf('\n', (deleteStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                    val linePrefix = oldText.substring(lineStart, (deleteStart + deletedCount).coerceAtMost(oldText.length))
                                    if (linePrefix == "• " && deleteStart <= lineStart + 2) {
                                        val cleanText = oldText.substring(0, lineStart) + oldText.substring((lineStart + 2).coerceAtMost(oldText.length))
                                        spans = RichTextHelper.adjustSpansForDeletion(spans, lineStart, 2)
                                        textFieldValue = TextFieldValue(cleanText, TextRange(lineStart))
                                        return@BasicTextField
                                    }

                                    spans = RichTextHelper.adjustSpansForDeletion(spans, deleteStart, deletedCount)
                                }

                                textFieldValue = newVal
                            },
                            onTextLayout = { layout ->
                                textLayoutResult = layout
                            },
                            visualTransformation = remember(spans) { RichVisualTransformation { spans } },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 17.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Normal,
                                fontFamily = SfProText,
                                lineHeight = 24.sp,
                                textDirection = androidx.compose.ui.text.style.TextDirection.Content,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Left
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue500),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 120.dp)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            "Write notes...",
                                            color = TextSecondary.copy(alpha = 0.5f),
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Normal,
                                            fontFamily = SfProText
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default
                            )
                        )

                        // Generous bottom typing space (Typewriter overscroll)
                        // Allows the user to freely scroll their notes up so the current typing line
                        // is comfortably situated in the middle/upper portion of the screen
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusRequester.requestFocus()
                                    textFieldValue = textFieldValue.copy(
                                        selection = TextRange(textFieldValue.text.length)
                                    )
                                    keyboardController?.show()
                                }
                        )
                    }
                }

                // Popup Menus (Heading Menu / Highlight Palette)
                if (showHeadingMenu) {
                    HeadingMenuPopup(
                        currentHeading = activeHeadingLevel,
                        onSelect = { level ->
                            spans = RichTextHelper.applyHeadingToLine(textFieldValue, spans, level)
                            activeHeadingLevel = level
                            showHeadingMenu = false
                        },
                        onDismiss = { showHeadingMenu = false }
                    )
                }

                if (showHighlightPalette) {
                    HighlightPalettePopup(
                        activeColor = activeHighlightColor,
                        onColorSelected = { color ->
                            activeHighlightColor = color
                            spans = RichTextHelper.applyHighlightColor(textFieldValue, spans, color)
                            showHighlightPalette = false
                        },
                        onClear = {
                            activeHighlightColor = null
                            spans = RichTextHelper.applyHighlightColor(textFieldValue, spans, null)
                            showHighlightPalette = false
                        }
                    )
                }

                // Bottom Formatting Toolbar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isKeyboardOpen) {
                                Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            } else {
                                Modifier
                            }
                        ),
                    color = White
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = Divider.copy(alpha = 0.25f), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Formatting tools
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(toolbarScrollState),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Heading (H)
                                ToolbarButton(
                                    isActive = activeHeadingLevel > 0,
                                    onClick = {
                                        showHighlightPalette = false
                                        showHeadingMenu = !showHeadingMenu
                                    }
                                ) {
                                    Text(
                                        text = if (activeHeadingLevel > 0) "H$activeHeadingLevel" else "H",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeHeadingLevel > 0) Blue500 else TextPrimary,
                                        fontFamily = SfProText
                                    )
                                }

                                // Bold (B)
                                ToolbarButton(
                                    isActive = isBoldActive,
                                    onClick = {
                                        val (newSpans, active) = RichTextHelper.toggleBold(textFieldValue, spans, isBoldActive)
                                        spans = newSpans
                                        isBoldActive = active
                                    }
                                ) {
                                    Text(
                                        text = "B",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBoldActive) Blue500 else TextPrimary,
                                        fontFamily = SfProText
                                    )
                                }

                                // Highlighter (Pen with active color underline)
                                ToolbarButton(
                                    isActive = activeHighlightColor != null,
                                    onClick = {
                                        showHeadingMenu = false
                                        showHighlightPalette = !showHighlightPalette
                                    }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Highlight",
                                            tint = if (activeHighlightColor != null) Blue500 else TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.height(1.5.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(2.5.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(activeHighlightColor ?: Color(0xFFFFD600))
                                        )
                                    }
                                }

                                val currentText = textFieldValue.text
                                val selPos = textFieldValue.selection.min
                                val curLineStart = currentText.lastIndexOf('\n', (selPos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                val curLineEnd = currentText.indexOf('\n', selPos).let { if (it == -1) currentText.length else it }
                                val curLine = if (curLineStart <= curLineEnd && curLineEnd <= currentText.length) currentText.substring(curLineStart, curLineEnd) else ""
                                val isBulletOnLine = curLine.startsWith("• ")
                                val isNumberedOnLine = curLine.matches(Regex("^\\d+\\.\\s*.*"))

                                // Bullet list
                                ToolbarButton(
                                    isActive = isBulletOnLine,
                                    onClick = {
                                        val (newVal, newSpans) = RichTextHelper.toggleBulletList(textFieldValue, spans)
                                        textFieldValue = newVal
                                        spans = newSpans
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = "Bullet List",
                                        tint = if (isBulletOnLine) Blue500 else TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Numbered list
                                ToolbarButton(
                                    isActive = isNumberedOnLine,
                                    onClick = {
                                        val (newVal, newSpans) = RichTextHelper.toggleNumberedList(textFieldValue, spans)
                                        textFieldValue = newVal
                                        spans = newSpans
                                    }
                                ) {
                                    NumberedListToolbarIcon(tint = if (isNumberedOnLine) Blue500 else TextPrimary)
                                }

                                // Outdent
                                ToolbarButton(
                                    onClick = {
                                        val (newVal, newSpans) = RichTextHelper.outdentLine(textFieldValue, spans)
                                        textFieldValue = newVal
                                        spans = newSpans
                                    }
                                ) {
                                    IndentToolbarIcon(isIndent = false, tint = TextPrimary)
                                }

                                // Indent
                                ToolbarButton(
                                    onClick = {
                                        val (newVal, newSpans) = RichTextHelper.indentLine(textFieldValue, spans)
                                        textFieldValue = newVal
                                        spans = newSpans
                                    }
                                ) {
                                    IndentToolbarIcon(isIndent = true, tint = TextPrimary)
                                }

                                // Italic (I)
                                ToolbarButton(
                                    isActive = isItalicActive,
                                    onClick = {
                                        val (newSpans, active) = RichTextHelper.toggleItalic(textFieldValue, spans, isItalicActive)
                                        spans = newSpans
                                        isItalicActive = active
                                    }
                                ) {
                                    Text(
                                        text = "I",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Normal,
                                        fontStyle = FontStyle.Italic,
                                        fontFamily = FontFamily.Serif,
                                        color = if (isItalicActive) Blue500 else TextPrimary
                                    )
                                }

                                // Underline (U)
                                ToolbarButton(
                                    isActive = isUnderlineActive,
                                    onClick = {
                                        val (newSpans, active) = RichTextHelper.toggleUnderline(textFieldValue, spans, isUnderlineActive)
                                        spans = newSpans
                                        isUnderlineActive = active
                                    }
                                ) {
                                    Text(
                                        text = "U",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = TextDecoration.Underline,
                                        color = if (isUnderlineActive) Blue500 else TextPrimary,
                                        fontFamily = SfProText
                                    )
                                }

                                // Strikethrough (S)
                                ToolbarButton(
                                    isActive = isStrikethroughActive,
                                    onClick = {
                                        val (newSpans, active) = RichTextHelper.toggleStrikethrough(textFieldValue, spans, isStrikethroughActive)
                                        spans = newSpans
                                        isStrikethroughActive = active
                                    }
                                ) {
                                    Text(
                                        text = "S",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = if (isStrikethroughActive) Blue500 else TextPrimary,
                                        fontFamily = SfProText
                                    )
                                }

                                // Keyboard Hide
                                ToolbarButton(
                                    onClick = { keyboardController?.hide() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardHide,
                                        contentDescription = "Hide Keyboard",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            // Bottom Done button
                            IconButton(
                                onClick = {
                                    val serialized = RichTextHelper.serializeToHtml(textFieldValue.text, spans)
                                    onDone(serialized)
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Done",
                                    tint = Blue500,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }
}

@Composable
private fun HeadingMenuPopup(
    currentHeading: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Grey100,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple("H₁", "Heading 1", 1),
                Triple("H₂", "Heading 2", 2),
                Triple("H₃", "Heading 3", 3),
                Triple("Body", "Normal", 0)
            ).forEach { (label, desc, level) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (currentHeading == level) Blue500.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSelect(level) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (level > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentHeading == level) Blue500 else TextPrimary
                        )
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = if (currentHeading == level) Blue500 else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightPalettePopup(
    activeColor: Color?,
    onColorSelected: (Color) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Grey100,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear circle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, TextSecondary.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.5.dp)
                        .rotate(45f)
                        .background(Red500)
                )
            }

            // Colors
            RichTextHelper.HighlightColors.forEach { color ->
                val isSelected = activeColor == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(2.dp, Blue500, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Blue500.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun NumberedListToolbarIcon(tint: Color) {
    Column(
        modifier = Modifier.size(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("1", "2", "3").forEach { num ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = num,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                    lineHeight = 7.5.sp,
                    modifier = Modifier.width(7.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(tint)
                )
            }
        }
    }
}

@Composable
private fun IndentToolbarIcon(isIndent: Boolean, tint: Color) {
    Row(
        modifier = Modifier.size(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!isIndent) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.5.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).clip(RoundedCornerShape(1.dp)).background(tint))
            Box(modifier = Modifier.fillMaxWidth(0.65f).height(1.5.dp).clip(RoundedCornerShape(1.dp)).background(tint))
            Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).clip(RoundedCornerShape(1.dp)).background(tint))
        }
        if (isIndent) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
