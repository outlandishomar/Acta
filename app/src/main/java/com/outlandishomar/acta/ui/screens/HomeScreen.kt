package com.outlandishomar.acta.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
import android.content.ClipDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.outlandishomar.acta.data.AppSettings
import com.outlandishomar.acta.data.shouldHideTask
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.ui.components.*
import com.outlandishomar.acta.ui.theme.*
import com.outlandishomar.acta.ui.viewmodel.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.glance.appwidget.updateAll
import androidx.compose.ui.res.painterResource
import com.outlandishomar.acta.ui.theme.ThemeState
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: TaskViewModel, onSettingsClick: () -> Unit, openTaskId: Int = -1, onTaskOpened: () -> Unit = {}) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayTasksState by viewModel.dayTasksState.collectAsStateWithLifecycle()
    val subTasksMap = dayTasksState.subTasksMap

    val allCalendarTasks by viewModel.allCalendarTasks.collectAsStateWithLifecycle()
    val allExceptions by viewModel.allExceptions.collectAsStateWithLifecycle()
    val allCompletions by viewModel.allCompletions.collectAsStateWithLifecycle()

    val hasTasks: (LocalDate) -> Boolean = remember(
        allCalendarTasks,
        allExceptions,
        allCompletions,
        AppSettings.isHideAllTasksEnabled,
        AppSettings.isHideRegularTasksEnabled,
        AppSettings.isHideTodayTasksEnabled,
        AppSettings.hideModeTaskIds
    ) {
        val exceptionsMap: Map<Int, List<com.outlandishomar.acta.data.model.TaskException>> = allExceptions.groupBy { it.taskId }
        val completionsSet: Set<Pair<Int, Long>> = allCompletions.map { it.taskId to it.dateMillis }.toSet()
        val lambda: (LocalDate) -> Boolean = { date: LocalDate ->
            val isToday = (date == LocalDate.now())
            val visibleTasks = allCalendarTasks.filter { task ->
                !com.outlandishomar.acta.data.shouldHideTask(task = task, isInboxTask = false, isTodayTask = isToday)
            }
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            visibleTasks.any { task ->
                if (task.repeatMode == "ONCE") {
                    task.date == dateMillis && !task.isCompleted
                } else {
                    val startDate = Instant.ofEpochMilli(task.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val occurs = com.outlandishomar.acta.data.util.RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, date)
                    if (occurs) {
                        val taskExceptions = exceptionsMap[task.id] ?: emptyList()
                        val hasException = taskExceptions.any { it.dateMillis == dateMillis }
                        if (!hasException) {
                            !completionsSet.contains(task.id to dateMillis)
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
        }
        lambda
    }

    var showDatePicker by remember { mutableStateOf(false) }

    var taskForActionSheet by remember { mutableStateOf<Task?>(null) }
    var taskForEditDialog by remember { mutableStateOf<Task?>(null) }
    
    val tasks = dayTasksState.tasks

    LaunchedEffect(openTaskId, tasks) {
        if (openTaskId != -1) {
            val target = tasks.find { it.id == openTaskId }
            if (target != null) {
                taskForActionSheet = target
                onTaskOpened()
            }
        }
    }

    val filteredTasks = tasks

    // Local copy for reordering — synchronized with dayTasksState
    var localTasks by remember(dayTasksState.date, dayTasksState.tasks) {
        mutableStateOf(dayTasksState.tasks)
    }
    val currentLocalTasks by rememberUpdatedState(localTasks)

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    var isInitialLoad by remember(dayTasksState.date) { mutableStateOf(true) }
    var previousTaskIds by remember(dayTasksState.date) { mutableStateOf(localTasks.map { it.id }.toSet()) }

    LaunchedEffect(localTasks) {
        if (localTasks.isNotEmpty()) {
            if (isInitialLoad) {
                isInitialLoad = false
                previousTaskIds = localTasks.map { it.id }.toSet()
            } else if (localTasks.size > previousTaskIds.size) {
                val newIndex = localTasks.indexOfFirst { !previousTaskIds.contains(it.id) }.takeIf { it != -1 } ?: (localTasks.size - 1)
                val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val firstVisible = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0

                if (newIndex > lastVisible) {
                    val itemsToScroll = newIndex - lastVisible
                    val avgHeight = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 180f
                    val scrollDistance = itemsToScroll * avgHeight + 250f

                    lazyListState.animateScrollBy(
                        value = scrollDistance,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                        )
                    )
                } else if (newIndex < firstVisible) {
                    lazyListState.animateScrollToItem(newIndex)
                }
                previousTaskIds = localTasks.map { it.id }.toSet()
            } else {
                previousTaskIds = localTasks.map { it.id }.toSet()
            }
        }
    }
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localTasks = localTasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }
    var draggingTaskId by remember { mutableStateOf<Int?>(null) }
    var dropHoverIndex by remember { mutableStateOf<Int?>(null) }
    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var lazyColumnTopPx by remember { mutableFloatStateOf(0f) }
    var lazyColumnBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var unscheduledBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var emptyDropTargetBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var rootPointerPosition by remember { mutableStateOf<Offset?>(null) }
    var activeReorderingTaskId by remember { mutableStateOf<Int?>(null) }
    var lastHoveredTargetTaskId by remember { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val isHoveringUnscheduled = remember(
        activeReorderingTaskId,
        unscheduledBoundsInRoot,
        emptyDropTargetBoundsInRoot,
        lazyColumnBoundsInRoot,
        rootPointerPosition
    ) {
        if (activeReorderingTaskId == null || rootPointerPosition == null) {
            false
        } else {
            val pointer = rootPointerPosition!!
            val bufferPx = with(density) { 16.dp.toPx() }

            // 1. If UnscheduledChipsRow is on screen
            val hoveringRow = unscheduledBoundsInRoot?.let { bounds ->
                val inX = pointer.x in (bounds.left - bufferPx)..(bounds.right + bufferPx)
                val inY = pointer.y in (bounds.top - bufferPx)..(bounds.bottom + bufferPx)
                val listTop = lazyColumnBoundsInRoot?.top ?: bounds.bottom
                val aboveList = pointer.y < listTop && pointer.y >= (bounds.top - bufferPx * 2)
                (inX && inY) || aboveList
            } ?: false

            // 2. If floating drop target is on screen
            val hoveringFloatingTarget = emptyDropTargetBoundsInRoot?.let { bounds ->
                pointer.y in (bounds.top - bufferPx)..(bounds.bottom + bufferPx) &&
                        pointer.x in (bounds.left - bufferPx)..(bounds.right + bufferPx)
            } ?: false

            hoveringRow || hoveringFloatingTarget
        }
    }

    SideEffect {
        if (isHoveringUnscheduled && activeReorderingTaskId != null) {
            lastHoveredTargetTaskId = activeReorderingTaskId
        } else if (!isHoveringUnscheduled) {
            lastHoveredTargetTaskId = null
        }
    }

    val currentIsHoveringUnscheduled by rememberUpdatedState(isHoveringUnscheduled)

    val checkIsPointerOverUnscheduled: () -> Boolean = {
        val pointer = rootPointerPosition
        if (pointer != null) {
            val bufferPx = with(density) { 16.dp.toPx() }

            val hoveringRow = unscheduledBoundsInRoot?.let { bounds ->
                val inX = pointer.x in (bounds.left - bufferPx)..(bounds.right + bufferPx)
                val inY = pointer.y in (bounds.top - bufferPx)..(bounds.bottom + bufferPx)
                val listTop = lazyColumnBoundsInRoot?.top ?: bounds.bottom
                val aboveList = pointer.y < listTop && pointer.y >= (bounds.top - bufferPx * 2)
                (inX && inY) || aboveList
            } ?: false

            val hoveringFloatingTarget = emptyDropTargetBoundsInRoot?.let { bounds ->
                pointer.y in (bounds.top - bufferPx)..(bounds.bottom + bufferPx) &&
                        pointer.x in (bounds.left - bufferPx)..(bounds.right + bufferPx)
            } ?: false

            hoveringRow || hoveringFloatingTarget
        } else false
    }
    val currentCheckIsPointerOverUnscheduled by rememberUpdatedState(checkIsPointerOverUnscheduled)

    var wasHoveringUnscheduled by remember { mutableStateOf(false) }
    LaunchedEffect(isHoveringUnscheduled) {
        if (isHoveringUnscheduled && !wasHoveringUnscheduled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        wasHoveringUnscheduled = isHoveringUnscheduled
    }

    val calculateDropIndex: (Float) -> Int = { rawY ->
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) {
            0
        } else {
            val relativeY = rawY - lazyColumnTopPx
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            if (relativeY <= (firstItem.offset + firstItem.size / 2)) {
                firstItem.index
            } else if (relativeY >= (lastItem.offset + lastItem.size / 2)) {
                lastItem.index + 1
            } else {
                val item = visibleItems.firstOrNull { it.offset <= relativeY && (it.offset + it.size) >= relativeY }
                if (item != null) {
                    val midY = item.offset + item.size / 2
                    if (relativeY <= midY) item.index else item.index + 1
                } else {
                    val closest = visibleItems.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - relativeY) }
                    if (closest != null) {
                        if (relativeY <= closest.offset + closest.size / 2) closest.index else closest.index + 1
                    } else {
                        lastItem.index + 1
                    }
                }
            }
        }
    }

    val dragAndDropTarget = remember(viewModel, localTasks, lazyColumnTopPx) {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null && clipData.itemCount > 0) {
                    draggingTaskId = clipData.getItemAt(0).text.toString().toIntOrNull()
                }
            }

            override fun onMoved(event: DragAndDropEvent) {
                val dropY = event.toAndroidDragEvent().y
                dropHoverIndex = calculateDropIndex(dropY)
            }

            override fun onExited(event: DragAndDropEvent) {
                dropHoverIndex = null
            }

            override fun onEnded(event: DragAndDropEvent) {
                draggingTaskId = null
                dropHoverIndex = null
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null && clipData.itemCount > 0) {
                    val taskIdStr = clipData.getItemAt(0).text.toString()
                    val id = taskIdStr.toIntOrNull()
                    if (id != null) {
                        val dropY = event.toAndroidDragEvent().y
                        val targetIndex = calculateDropIndex(dropY)
                        viewModel.makeTaskRegularById(id, targetIndex)
                        draggingTaskId = null
                        dropHoverIndex = null
                        return true
                    }
                }
                draggingTaskId = null
                dropHoverIndex = null
                return false
            }
        }
    }

    val unscheduledDropTarget = remember(viewModel) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData != null && clipData.itemCount > 0) {
                    val taskIdStr = clipData.getItemAt(0).text.toString()
                    val id = taskIdStr.toIntOrNull()
                    if (id != null) {
                        viewModel.makeTaskUnscheduledById(id)
                        draggingTaskId = null
                        dropHoverIndex = null
                        return true
                    }
                }
                draggingTaskId = null
                dropHoverIndex = null
                return false
            }

            override fun onEnded(event: DragAndDropEvent) {
                draggingTaskId = null
                dropHoverIndex = null
            }
        }
    }


    val isOverlayVisible = taskForActionSheet != null || taskForEditDialog != null
    val blurModifier = if (isOverlayVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(10.dp)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootLayoutCoordinates = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.pressed } ?: event.changes.firstOrNull()
                        if (change != null) {
                            val coords = rootLayoutCoordinates
                            rootPointerPosition = if (coords != null && coords.isAttached) {
                                coords.localToRoot(change.position)
                            } else {
                                change.position
                            }
                        }
                    }
                }
            }
    ) {
        Scaffold(
            containerColor = White,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TopBarBackground)
                ) {
                    TaskTopBar(
                        selectedDate = selectedDate,
                        onCalendarClick = { showDatePicker = true },
                        onSettingsClick = onSettingsClick
                    )
                    CalendarStrip(
                        selectedDate = selectedDate,
                        onDateSelected = viewModel::selectDate,
                        hasTasks = hasTasks
                    )
                }
            },
            modifier = blurModifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedDate) {
                            var totalDrag = 0f
                            var dragHandled = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    totalDrag = 0f
                                    dragHandled = false
                                },
                                onDragEnd = {
                                    if (!dragHandled) {
                                        if (totalDrag > 80f) {
                                            viewModel.selectDate(selectedDate.minusDays(1))
                                        } else if (totalDrag < -80f) {
                                            viewModel.selectDate(selectedDate.plusDays(1))
                                        }
                                    }
                                    totalDrag = 0f
                                    dragHandled = false
                                },
                                onDragCancel = {
                                    totalDrag = 0f
                                    dragHandled = false
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    totalDrag += dragAmount
                                    if (!dragHandled) {
                                        if (totalDrag > 220f) {
                                            viewModel.selectDate(selectedDate.minusDays(1))
                                            dragHandled = true
                                        } else if (totalDrag < -220f) {
                                            viewModel.selectDate(selectedDate.plusDays(1))
                                            dragHandled = true
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val unscheduledTasks = dayTasksState.highlightedTasks
                    var showAddUnscheduledDialog by remember { mutableStateOf(false) }

                    SectionHeader(
                        selectedDate = selectedDate,
                        allDone = (tasks.isNotEmpty() || unscheduledTasks.isNotEmpty()) &&
                                tasks.all { it.isCompleted } &&
                                unscheduledTasks.all { it.isCompleted }
                    )

                    // Unscheduled Tasks: Compact Squircle Chip Strip (always visible so user has persistent drop/add target)
                    UnscheduledChipsRow(
                        tasks = unscheduledTasks,
                        onToggleComplete = viewModel::toggleComplete,
                        onTaskClick = { taskForActionSheet = it },
                        onAddClick = { showAddUnscheduledDialog = true },
                        draggingTaskId = draggingTaskId,
                        onDragStart = { draggingTaskId = it.id },
                        isHovered = isHoveringUnscheduled,
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                unscheduledBoundsInRoot = coordinates.boundsInRoot()
                            }
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { event ->
                                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                },
                                target = unscheduledDropTarget
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = dayTasksState.date,
                            transitionSpec = {
                                val direction = if (targetState.isAfter(initialState)) 1 else -1
                                val springSpec = spring<androidx.compose.ui.unit.IntOffset>(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                                val fadeSpec = tween<Float>(durationMillis = 180)

                                (slideInHorizontally(animationSpec = springSpec) { width -> width * direction } +
                                        fadeIn(animationSpec = fadeSpec))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = springSpec) { width -> -width * direction } +
                                                fadeOut(animationSpec = fadeSpec)
                                    )
                            },
                            label = "DateTransition",
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                        ) { targetDate ->
                            val isToday = (targetDate == LocalDate.now())
                            val rawTasksForDate = if (targetDate == dayTasksState.date) localTasks else emptyList()
                            val tasksForDate = rawTasksForDate.filter { task ->
                                !com.outlandishomar.acta.data.shouldHideTask(task = task, isInboxTask = false, isTodayTask = isToday)
                            }

                            if (tasksForDate.isEmpty()) {
                                val emptyMessage = if (isToday) "No tasks for today" else "No tasks for this day"
                                EmptyState(
                                    message = emptyMessage,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onGloballyPositioned { coordinates ->
                                            lazyColumnTopPx = coordinates.positionInRoot().y
                                            lazyColumnBoundsInRoot = coordinates.boundsInRoot()
                                        }
                                        .dragAndDropTarget(
                                            shouldStartDragAndDrop = { event ->
                                                event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                            },
                                            target = dragAndDropTarget
                                        )
                                )
                            } else {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onGloballyPositioned { coordinates ->
                                            lazyColumnTopPx = coordinates.positionInRoot().y
                                            lazyColumnBoundsInRoot = coordinates.boundsInRoot()
                                        }
                                        .dragAndDropTarget(
                                            shouldStartDragAndDrop = { event ->
                                                event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                            },
                                            target = dragAndDropTarget
                                        ),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    itemsIndexed(tasksForDate, key = { _, task -> task.id }) { index, task ->
                                        ReorderableItem(reorderState, key = task.id) { isDragging ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateItem(
                                                        placementSpec = spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                    .longPressDraggableHandle(
                                                        onDragStarted = {
                                                            activeReorderingTaskId = task.id
                                                        },
                                                        onDragStopped = {
                                                            val taskIdToDrop = task.id
                                                            val wasHovering = currentIsHoveringUnscheduled ||
                                                                    currentCheckIsPointerOverUnscheduled() ||
                                                                    (lastHoveredTargetTaskId == taskIdToDrop)

                                                            activeReorderingTaskId = null
                                                            lastHoveredTargetTaskId = null

                                                            if (wasHovering) {
                                                                val remainingTasks = currentLocalTasks.filter { it.id != taskIdToDrop }
                                                                localTasks = remainingTasks
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.reorderTasks(remainingTasks)
                                                                viewModel.makeTaskUnscheduledById(taskIdToDrop)
                                                            } else {
                                                                viewModel.reorderTasks(currentLocalTasks)
                                                            }
                                                        }
                                                    )
                                            ) {
                                                TaskItem(
                                                    task = task,
                                                    onToggleComplete = viewModel::toggleComplete,
                                                    onDelete = viewModel::deleteTask,
                                                    onMoveToInbox = viewModel::moveTaskToInbox,
                                                    subTasks = subTasksMap[task.id] ?: emptyList(),
                                                    onToggleSubTask = viewModel::toggleSubTaskComplete,
                                                    onTaskClick = { taskForActionSheet = it }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val showEmptyDropTarget = activeReorderingTaskId != null && unscheduledTasks.isEmpty()
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showEmptyDropTarget,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically { -it / 2 },
                            exit = fadeOut(tween(150)) + slideOutVertically { -it / 2 },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            val isHovered = isHoveringUnscheduled
                            val pillBg by animateColorAsState(
                                targetValue = if (isHovered) Blue500 else AvatarGrey.copy(alpha = 0.95f),
                                animationSpec = tween(150),
                                label = "floatingPillBg"
                            )
                            val pillTextColor by animateColorAsState(
                                targetValue = if (isHovered) White else TextPrimary,
                                animationSpec = tween(150),
                                label = "floatingPillTextColor"
                            )
                            val pillScale by animateFloatAsState(
                                targetValue = if (isHovered) 1.05f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                label = "floatingPillScale"
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        emptyDropTargetBoundsInRoot = coordinates.boundsInRoot()
                                    }
                                    .graphicsLayer {
                                        scaleX = pillScale
                                        scaleY = pillScale
                                        shadowElevation = 8.dp.toPx()
                                        shape = RoundedCornerShape(20.dp)
                                        clip = true
                                    }
                                    .background(pillBg, RoundedCornerShape(20.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (isHovered) Blue500 else Divider.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = pillTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Move to Unscheduled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProText,
                                    color = pillTextColor
                                )
                            }
                        }
                    }

                    if (showAddUnscheduledDialog) {
                        AddTaskDialog(
                            isHighlighted = true,
                            initialStartDate = selectedDate,
                            onDismiss = { showAddUnscheduledDialog = false },
                            onAdd = { title, notes, subTasksList, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, _ ->
                                if (subTasksList.isEmpty()) {
                                    viewModel.addTask(title, notes, true, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                                } else {
                                    viewModel.addTaskWithSubTasks(title, notes, true, subTasksList, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts)
                                }
                                showAddUnscheduledDialog = false
                            }
                        )
                    }
                }
            }

        if (isOverlayVisible && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }

    taskForActionSheet?.let { task ->
        TaskActionSheet(
            task = task,
            subTasks = subTasksMap[task.id] ?: emptyList(),
            onDismiss = { taskForActionSheet = null },
            onDelete = { deleteType ->
                when (deleteType) {
                    DeleteType.INSTANCE -> viewModel.deleteTaskInstance(task)
                    DeleteType.FUTURE -> viewModel.deleteFutureTasks(task)
                    DeleteType.ALL -> viewModel.deleteTask(task)
                }
                taskForActionSheet = null
            },
            onToggleComplete = {
                viewModel.toggleComplete(task)
                taskForActionSheet = null
            },
            onToggleSubTask = { subTask ->
                viewModel.toggleSubTaskComplete(subTask)
            },
            onEdit = {
                taskForEditDialog = task
                taskForActionSheet = null
            },
            onDuplicate = { targetDate ->
                viewModel.duplicateTask(task, subTasksMap[task.id] ?: emptyList(), targetDate)
                taskForActionSheet = null
            },
            onToggleHighlight = {
                viewModel.toggleTaskHighlighted(task)
            }
        )
    }

    taskForEditDialog?.let { task ->
        val existingSubTasks = subTasksMap[task.id]?.map { it.title } ?: emptyList()
        AddTaskDialog(
            initialTitle = task.title,
            initialNotes = task.description,
            initialSubTasks = existingSubTasks,
            initialIconName = task.iconName,
            isHighlighted = task.isHighlighted,
            isEditMode = true,
            initialTimeHour = task.taskTimeHour,
            initialTimeMinute = task.taskTimeMinute,
            initialRepeatMode = task.repeatMode,
            initialRepeatInterval = task.repeatInterval,
            initialRepeatDays = task.repeatDays,
            initialStartDate = java.time.Instant.ofEpochMilli(task.date).atZone(ZoneId.systemDefault()).toLocalDate(),
            initialEndDate = task.endDate?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
            initialAlertEnabled = task.alertEnabled,
            initialAdditionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts),
            onDismiss = { taskForEditDialog = null },
            onAdd = { title, notes, subTasksList, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, editType ->
                viewModel.updateTaskAndSubTasks(task, title, notes, subTasksList, iconName, timeHour, timeMinute, repeatMode, repeatInterval, repeatDays, startDate, endDate, alertEnabled, additionalAlerts, editType)
                taskForEditDialog = null
            }
        )
    }

    // --- Material 3 Date Picker Dialog ---
    if (showDatePicker) {
        val currentYear = LocalDate.now().year
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            yearRange = (currentYear - 10)..(currentYear + 10)
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.selectDate(pickedDate)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = Blue500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
}


@Composable
fun TaskTopBar(
    selectedDate: LocalDate,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val title = selectedDate.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (5).dp)
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            fontFamily = SfProDisplay,
            modifier = Modifier.appleClickable(onCalendarClick)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val isDark = ThemeState.isThemeDark
            IconButton(onClick = onCalendarClick) {
                Icon(
                    painter = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.scheduelwhite else com.outlandishomar.acta.R.drawable.schedueldark),
                    contentDescription = "Calendar",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(id = if (isDark) com.outlandishomar.acta.R.drawable.settingswhite else com.outlandishomar.acta.R.drawable.settingsblack),
                    contentDescription = "Settings",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    selectedDate: LocalDate,
    allDone: Boolean
) {
    HorizontalDivider(color = Divider, thickness = 0.5.dp)
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✓",
                fontSize = 32.sp,
                color = Blue500,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap + to add a new task",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
