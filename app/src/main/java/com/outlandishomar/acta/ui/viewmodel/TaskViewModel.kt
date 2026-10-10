package com.outlandishomar.acta.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.model.TaskCompletion
import com.outlandishomar.acta.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import androidx.glance.appwidget.updateAll
import com.outlandishomar.acta.receiver.AlarmScheduler
import com.outlandishomar.acta.ui.components.EditType

sealed interface UndoAction {
    val message: String
    data class TaskDeleted(
        val task: Task,
        val subTasks: List<SubTask>,
        val completions: List<TaskCompletion> = emptyList()
    ) : UndoAction {
        override val message: String = "Task deleted"
    }

    data class InstanceDeleted(
        val taskId: Int,
        val dateMillis: Long
    ) : UndoAction {
        override val message: String = "Instance deleted"
    }

    data class FutureDeleted(
        val task: Task,
        val previousEndDate: Long?
    ) : UndoAction {
        override val message: String = "Future tasks deleted"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getInstance(application).taskDao())
    }

    // Currently selected calendar day
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _undoAction = MutableStateFlow<UndoAction?>(null)
    val undoAction: StateFlow<UndoAction?> = _undoAction.asStateFlow()
    private var undoJob: Job? = null

    private fun showUndo(action: UndoAction) {
        undoJob?.cancel()
        _undoAction.value = action
        undoJob = viewModelScope.launch {
            delay(4000)
            _undoAction.value = null
        }
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                com.outlandishomar.acta.widget.TaskWidget().updateAll(getApplication())
            } catch (e: Exception) {}
        }
    }


data class DayTasksState(
    val date: LocalDate,
    val tasks: List<Task> = emptyList(),
    val highlightedTasks: List<Task> = emptyList(),
    val subTasksMap: Map<Int, List<SubTask>> = emptyMap(),
    val isLoading: Boolean = true
)

    // Atomic combined state for date, tasks, highlighted tasks, and subtasks
    val dayTasksState: StateFlow<DayTasksState> = _selectedDate
        .flatMapLatest { date ->
            val dayMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val isToday = (date == LocalDate.now())
            val pastHighlightedFlow = if (isToday) {
                repository.getPastUncompletedHighlightedTasks(dayMillis)
            } else {
                flowOf(emptyList())
            }

            combine(
                repository.getTasksByDate(dayMillis),
                pastHighlightedFlow
            ) { taskList, pastHighlighted ->
                taskList to pastHighlighted
            }.flatMapLatest { (taskList, pastHighlighted) ->
                val regularTasks = taskList.filter { !it.isHighlighted }
                val dayHighlighted = taskList.filter { it.isHighlighted }
                val allHighlighted = (dayHighlighted + pastHighlighted).distinctBy { it.id }
                val allTasksCombined = regularTasks + allHighlighted
                val ids = allTasksCombined.map { it.id }
                if (ids.isEmpty()) {
                    flowOf(
                        DayTasksState(
                            date = date,
                            tasks = emptyList(),
                            highlightedTasks = emptyList(),
                            subTasksMap = emptyMap(),
                            isLoading = false
                        )
                    )
                } else {
                    val repeatingTaskIds = allTasksCombined.filter { it.repeatMode != "ONCE" }.map { it.id }.toSet()
                    repository.getSubTasksForTasks(ids, dayMillis, repeatingTaskIds)
                        .map { subTasks ->
                            val subMap = subTasks.groupBy { it.taskId }
                            DayTasksState(
                                date = date,
                                tasks = regularTasks,
                                highlightedTasks = allHighlighted,
                                subTasksMap = subMap,
                                isLoading = false
                            )
                        }
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DayTasksState(date = _selectedDate.value, isLoading = true)
        )

    val tasks: StateFlow<List<Task>> = dayTasksState
        .map { it.tasks }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val highlightedTasks: StateFlow<List<Task>> = dayTasksState
        .map { it.highlightedTasks }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subTasksMap: StateFlow<Map<Int, List<SubTask>>> = dayTasksState
        .map { it.subTasksMap }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Inbox tasks (no date assigned)
    val inboxTasks: StateFlow<List<Task>> = repository.getInboxTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Sub-tasks for inbox tasks
    val inboxSubTasksMap: StateFlow<Map<Int, List<SubTask>>> = inboxTasks
        .flatMapLatest { taskList ->
            val ids = taskList.map { it.id }
            if (ids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                repository.getSubTasksForTasks(ids)
                    .map { subTasks -> subTasks.groupBy { it.taskId } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val allCalendarTasks: StateFlow<List<Task>> = repository.getAllCalendarTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allExceptions: StateFlow<List<com.outlandishomar.acta.data.model.TaskException>> = repository.getAllExceptionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCompletions: StateFlow<List<com.outlandishomar.acta.data.model.TaskCompletion>> = repository.getAllCompletionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Actions ---

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addTask(
        title: String,
        description: String,
        isHighlighted: Boolean,
        iconName: String? = null,
        timeHour: Int? = null,
        timeMinute: Int? = null,
        repeatMode: String = "ONCE",
        repeatInterval: Int = 1,
        repeatDays: String = "",
        startDate: LocalDate = _selectedDate.value,
        endDate: LocalDate? = null,
        alertEnabled: Boolean = false,
        additionalAlerts: List<com.outlandishomar.acta.data.model.CustomAlert> = emptyList()
    ) {
        val startMillis = startDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endMillis = endDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            
        viewModelScope.launch {
            val maxOrder = repository.getMaxOrderIndex()
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                date = startMillis,
                isHighlighted = isHighlighted,
                iconName = iconName,
                orderIndex = maxOrder + 1,
                taskTimeHour = timeHour,
                taskTimeMinute = timeMinute,
                repeatMode = repeatMode,
                repeatInterval = repeatInterval,
                repeatDays = repeatDays,
                endDate = endMillis,
                alertEnabled = alertEnabled,
                additionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.serializeList(additionalAlerts)
            )
            val taskId = repository.addTask(task).toInt()
            if (com.outlandishomar.acta.data.isAnyHideTaskModeActive()) {
                com.outlandishomar.acta.data.registerHideModeTaskId(getApplication(), taskId)
            }
            
            if (alertEnabled) {
                AlarmScheduler.scheduleTaskAlarm(getApplication(), task.copy(id = taskId))
            }
        }
    }

    fun addTaskWithSubTasks(
        title: String,
        description: String,
        isHighlighted: Boolean,
        subTaskTitles: List<String>,
        iconName: String? = null,
        timeHour: Int? = null,
        timeMinute: Int? = null,
        repeatMode: String = "ONCE",
        repeatInterval: Int = 1,
        repeatDays: String = "",
        startDate: LocalDate = _selectedDate.value,
        endDate: LocalDate? = null,
        alertEnabled: Boolean = false,
        additionalAlerts: List<com.outlandishomar.acta.data.model.CustomAlert> = emptyList()
    ) {
        val startMillis = startDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val endMillis = endDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
            
        viewModelScope.launch {
            val maxOrder = repository.getMaxOrderIndex()
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                date = startMillis,
                isHighlighted = isHighlighted,
                iconName = iconName,
                orderIndex = maxOrder + 1,
                taskTimeHour = timeHour,
                taskTimeMinute = timeMinute,
                repeatMode = repeatMode,
                repeatInterval = repeatInterval,
                repeatDays = repeatDays,
                endDate = endMillis,
                alertEnabled = alertEnabled,
                additionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.serializeList(additionalAlerts)
            )
            val taskId = repository.addTaskWithSubTasks(
                task,
                subTaskTitles
            ).toInt()
            if (com.outlandishomar.acta.data.isAnyHideTaskModeActive()) {
                com.outlandishomar.acta.data.registerHideModeTaskId(getApplication(), taskId)
            }
            
            if (alertEnabled) {
                AlarmScheduler.scheduleTaskAlarm(getApplication(), task.copy(id = taskId))
            }
        }
    }

    fun addInboxTask(title: String, notes: String, subTaskTitles: List<String> = emptyList()) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxOrderIndex()
            val taskId = if (subTaskTitles.isEmpty()) {
                repository.addTask(
                    Task(title = title.trim(), description = notes.trim(), date = 0L, orderIndex = maxOrder + 1)
                ).toInt()
            } else {
                repository.addTaskWithSubTasks(
                    Task(title = title.trim(), description = notes.trim(), date = 0L, orderIndex = maxOrder + 1),
                    subTaskTitles
                ).toInt()
            }
            if (com.outlandishomar.acta.data.isAnyHideTaskModeActive()) {
                com.outlandishomar.acta.data.registerHideModeTaskId(getApplication(), taskId)
            }
        }
    }

    fun cleanupHideModeTasks() {
        com.outlandishomar.acta.data.clearHideModeTaskIds(getApplication())
    }

    fun toggleComplete(task: Task) = viewModelScope.launch {
        if (task.repeatMode != "ONCE") {
            // For recurring tasks, store completion status for the specific projected date
            repository.setTaskCompletionForDate(task.id, task.date, !task.isCompleted)
        } else {
            // For one-time tasks, simply toggle the DB column
            repository.toggleComplete(task)
        }
        updateWidget()
    }

    fun toggleHighlight(task: Task) = viewModelScope.launch {
        repository.toggleHighlight(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        val originalTask = repository.getTaskById(task.id) ?: task
        val subTasks = repository.getSubTasksForTasksNow(listOf(task.id))
        val completions = repository.getAllCompletionsForTask(task.id)
        repository.deleteTask(task)
        AlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
        updateWidget()
        showUndo(UndoAction.TaskDeleted(originalTask, subTasks, completions))
    }

    fun deleteTaskInstance(task: Task) = viewModelScope.launch {
        repository.addException(task.id, task.date)
        updateWidget()
        showUndo(UndoAction.InstanceDeleted(task.id, task.date))
    }

    fun deleteFutureTasks(task: Task) = viewModelScope.launch {
        // We must fetch the original task because 'task.date' was overwritten by the view date
        val originalTask = repository.getTaskById(task.id)
        if (originalTask != null) {
            val previousEndDate = originalTask.endDate
            val newEndDate = task.date - 86400000L
            repository.updateTask(originalTask.copy(endDate = newEndDate))
            updateWidget()
            showUndo(UndoAction.FutureDeleted(originalTask, previousEndDate))
        }
    }

    fun undoDelete() = viewModelScope.launch {
        undoJob?.cancel()
        val action = _undoAction.value ?: return@launch
        _undoAction.value = null
        when (action) {
            is UndoAction.TaskDeleted -> {
                repository.restoreTask(action.task, action.subTasks, action.completions)
                if (action.task.alertEnabled && action.task.taskTimeHour != null && action.task.taskTimeMinute != null) {
                    AlarmScheduler.scheduleTaskAlarm(getApplication(), action.task)
                }
            }
            is UndoAction.InstanceDeleted -> {
                repository.removeException(action.taskId, action.dateMillis)
            }
            is UndoAction.FutureDeleted -> {
                repository.updateTask(action.task.copy(endDate = action.previousEndDate))
            }
        }
        updateWidget()
    }

    fun dismissUndo() {
        undoJob?.cancel()
        _undoAction.value = null
    }

    fun moveTaskToInbox(task: Task) = viewModelScope.launch {
        repository.updateTask(task.copy(date = 0L))
        updateWidget()
    }

    fun duplicateTask(
        task: Task,
        subTasks: List<SubTask> = emptyList(),
        targetDate: LocalDate? = null
    ) = viewModelScope.launch {
        val dateMillis = if (targetDate != null) {
            targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            task.date
        }

        val subTasksToCopy = if (subTasks.isNotEmpty()) {
            subTasks
        } else {
            repository.getSubTasksForTasksNow(listOf(task.id))
        }
        val subTaskTitles = subTasksToCopy.map { it.title }

        val maxOrder = repository.getMaxOrderIndex()
        val duplicatedTask = task.copy(
            id = 0,
            date = dateMillis,
            isCompleted = false,
            orderIndex = maxOrder + 1,
            createdAt = System.currentTimeMillis()
        )
        val newTaskId = repository.addTaskWithSubTasks(
            duplicatedTask,
            subTaskTitles
        ).toInt()

        if (com.outlandishomar.acta.data.isAnyHideTaskModeActive()) {
            com.outlandishomar.acta.data.registerHideModeTaskId(getApplication(), newTaskId)
        }

        if (duplicatedTask.alertEnabled) {
            AlarmScheduler.scheduleTaskAlarm(getApplication(), duplicatedTask.copy(id = newTaskId))
        }

        updateWidget()
    }

    private suspend fun updateSubTasksForTaskId(taskId: Int, newSubTaskTitles: List<String>) {
        val existingSubTasks = repository.getSubTasksForTasksNow(listOf(taskId))
        val newTitles = newSubTaskTitles.filter { it.isNotBlank() }
        
        val maxSize = maxOf(existingSubTasks.size, newTitles.size)
        for (i in 0 until maxSize) {
            if (i < existingSubTasks.size && i < newTitles.size) {
                repository.updateSubTask(existingSubTasks[i].copy(title = newTitles[i].trim()))
            } else if (i < existingSubTasks.size) {
                repository.deleteSubTask(existingSubTasks[i])
            } else if (i < newTitles.size) {
                repository.addSubTask(SubTask(taskId = taskId, title = newTitles[i].trim()))
            }
        }
    }

    fun updateTaskAndSubTasks(
        task: Task,
        newTitle: String,
        newNotes: String,
        newSubTaskTitles: List<String>,
        newIconName: String? = null,
        timeHour: Int? = task.taskTimeHour,
        timeMinute: Int? = task.taskTimeMinute,
        repeatMode: String = task.repeatMode,
        repeatInterval: Int = task.repeatInterval,
        repeatDays: String = task.repeatDays,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        alertEnabled: Boolean = task.alertEnabled,
        additionalAlerts: List<com.outlandishomar.acta.data.model.CustomAlert> = com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts),
        editType: EditType = EditType.ALL
    ) = viewModelScope.launch {
        val originalTask = repository.getTaskById(task.id) ?: task
        val endMillis = endDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

        when (editType) {
            EditType.INSTANCE -> {
                // 1. Add exception for original repeating task on this date (task.date)
                repository.addException(task.id, task.date)
                
                // 2. Create a new standalone single task for this date only preserving the original task's orderIndex
                val newTask = Task(
                    title = newTitle.trim(),
                    description = newNotes.trim(),
                    date = task.date,
                    isHighlighted = task.isHighlighted,
                    iconName = newIconName,
                    orderIndex = originalTask.orderIndex,
                    taskTimeHour = timeHour,
                    taskTimeMinute = timeMinute,
                    repeatMode = "ONCE",
                    repeatInterval = 1,
                    repeatDays = "",
                    endDate = null,
                    alertEnabled = alertEnabled,
                    additionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.serializeList(additionalAlerts)
                )
                val newTaskId = if (newSubTaskTitles.isEmpty()) {
                    repository.addTask(newTask).toInt()
                } else {
                    repository.addTaskWithSubTasks(newTask, newSubTaskTitles.filter { it.isNotBlank() }).toInt()
                }
                
                if (alertEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(getApplication(), newTask.copy(id = newTaskId))
                }
            }
            
            EditType.FUTURE -> {
                val currentViewingDate = task.date
                if (currentViewingDate <= originalTask.date) {
                    // Editing on or before start date -> update original task directly
                    val startMillis = startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: originalTask.date
                    val updatedTask = originalTask.copy(
                        title = newTitle.trim(),
                        description = newNotes.trim(),
                        date = startMillis,
                        iconName = newIconName,
                        taskTimeHour = timeHour,
                        taskTimeMinute = timeMinute,
                        repeatMode = repeatMode,
                        repeatInterval = repeatInterval,
                        repeatDays = repeatDays,
                        endDate = endMillis,
                        alertEnabled = alertEnabled,
                        additionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.serializeList(additionalAlerts)
                    )
                    repository.updateTask(updatedTask)
                    updateSubTasksForTaskId(task.id, newSubTaskTitles)
                    
                    if (alertEnabled) {
                        AlarmScheduler.scheduleTaskAlarm(getApplication(), updatedTask)
                    } else {
                        AlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
                    }
                } else {
                    // Truncate original task to end yesterday
                    val newEndDate = currentViewingDate - 86400000L
                    repository.updateTask(originalTask.copy(endDate = newEndDate))
                    
                    // Create new repeating task starting today (currentViewingDate) preserving orderIndex
                    val newRepeatingTask = Task(
                        title = newTitle.trim(),
                        description = newNotes.trim(),
                        date = currentViewingDate,
                        isHighlighted = task.isHighlighted,
                        iconName = newIconName,
                        orderIndex = originalTask.orderIndex,
                        taskTimeHour = timeHour,
                        taskTimeMinute = timeMinute,
                        repeatMode = repeatMode,
                        repeatInterval = repeatInterval,
                        repeatDays = repeatDays,
                        endDate = endMillis,
                        alertEnabled = alertEnabled,
                        additionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.serializeList(additionalAlerts)
                    )
                    val newTaskId = if (newSubTaskTitles.isEmpty()) {
                        repository.addTask(newRepeatingTask).toInt()
                    } else {
                        repository.addTaskWithSubTasks(newRepeatingTask, newSubTaskTitles.filter { it.isNotBlank() }).toInt()
                    }
                    
                    if (alertEnabled) {
                        AlarmScheduler.scheduleTaskAlarm(getApplication(), newRepeatingTask.copy(id = newTaskId))
                    }
                }
            }
            
            EditType.ALL -> {
                val startMillis = if (task.date == 0L) 0L else originalTask.date
                val updatedTask = originalTask.copy(
                    title = newTitle.trim(),
                    description = newNotes.trim(),
                    date = startMillis,
                    iconName = newIconName,
                    taskTimeHour = timeHour,
                    taskTimeMinute = timeMinute,
                    repeatMode = repeatMode,
                    repeatInterval = repeatInterval,
                    repeatDays = repeatDays,
                    endDate = endMillis,
                    alertEnabled = alertEnabled,
                    additionalAlerts = com.outlandishomar.acta.data.model.CustomAlert.serializeList(additionalAlerts)
                )
                repository.updateTask(updatedTask)
                updateSubTasksForTaskId(task.id, newSubTaskTitles)
                
                if (alertEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(getApplication(), updatedTask)
                } else {
                    AlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
                }
            }
        }
        updateWidget()
    }

    fun makeTaskRegularById(taskId: Int, targetIndex: Int? = null) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val dateMillis = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val currentRegularTasks = dayTasksState.value.tasks.filter { it.id != taskId }.toMutableList()

            val insertAt = if (targetIndex != null) {
                targetIndex.coerceIn(0, currentRegularTasks.size)
            } else {
                currentRegularTasks.size
            }

            val movedTask = task.copy(isHighlighted = false)
            currentRegularTasks.add(insertAt, movedTask)

            val ordersToSave = mutableListOf<com.outlandishomar.acta.data.model.TaskOrder>()
            val singleTasksToUpdate = mutableListOf<Task>()

            currentRegularTasks.forEachIndexed { index, t ->
                ordersToSave.add(com.outlandishomar.acta.data.model.TaskOrder(taskId = t.id, dateMillis = dateMillis, orderIndex = index))
                if (t.id == task.id) {
                    singleTasksToUpdate.add(t.copy(isHighlighted = false, orderIndex = index))
                } else if (t.repeatMode == "ONCE") {
                    singleTasksToUpdate.add(t.copy(orderIndex = index))
                }
            }

            if (singleTasksToUpdate.isNotEmpty()) {
                repository.updateTasks(singleTasksToUpdate)
            }
            if (ordersToSave.isNotEmpty()) {
                repository.saveTaskOrdersForDate(ordersToSave)
            }
            updateWidget()
        }
    }

    fun makeTaskUnscheduledById(taskId: Int) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                repository.updateTask(task.copy(isHighlighted = true))
                updateWidget()
            }
        }
    }

    fun toggleTaskHighlighted(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isHighlighted = !task.isHighlighted))
            updateWidget()
        }
    }

    fun toggleSubTaskComplete(subTask: SubTask) = viewModelScope.launch {
        val parentTask = repository.getTaskById(subTask.taskId)
        if (parentTask != null && parentTask.repeatMode != "ONCE") {
            val dateMillis = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            repository.setSubTaskCompletionForDate(subTask.id, dateMillis, !subTask.isCompleted)
        } else {
            repository.toggleSubTaskComplete(subTask)
        }
        updateWidget()
    }

    fun deleteSubTask(subTask: SubTask) = viewModelScope.launch {
        repository.deleteSubTask(subTask)
        updateWidget()
    }

    fun reorderTasks(tasksToUpdate: List<Task>) = viewModelScope.launch {
        val dateMillis = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ordersToSave = mutableListOf<com.outlandishomar.acta.data.model.TaskOrder>()
        val singleTasksToUpdate = mutableListOf<Task>()

        tasksToUpdate.forEachIndexed { index, task ->
            ordersToSave.add(com.outlandishomar.acta.data.model.TaskOrder(taskId = task.id, dateMillis = dateMillis, orderIndex = index))
            if (task.repeatMode == "ONCE") {
                repository.getTaskById(task.id)?.let { origTask ->
                    singleTasksToUpdate.add(origTask.copy(orderIndex = index))
                }
            }
        }

        if (singleTasksToUpdate.isNotEmpty()) {
            repository.updateTasks(singleTasksToUpdate)
        }
        if (ordersToSave.isNotEmpty()) {
            repository.saveTaskOrdersForDate(ordersToSave)
        }
        updateWidget()
    }

    init {
        viewModelScope.launch {
            repository.getTasksByDate(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()).collectLatest {
                updateWidget()
            }
        }
    }
}
