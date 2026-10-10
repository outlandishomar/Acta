package com.outlandishomar.acta.data.repository

import com.outlandishomar.acta.data.local.TaskDao
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.model.TaskCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksByDate(dateMillis: Long): Flow<List<Task>> {
        val tasksFlow = taskDao.getTasksByDate(dateMillis)
        val completionsFlow = taskDao.getCompletionsForDate(dateMillis)
        val exceptionsFlow = taskDao.getExceptionsForDate(dateMillis)
        val ordersFlow = taskDao.getOrdersForDate(dateMillis)
        
        return combine(tasksFlow, completionsFlow, exceptionsFlow, ordersFlow) { tasks, completions, exceptions, orders ->
            val targetDate = Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            val completionIds = completions.map { it.taskId }.toSet()
            val exceptionIds = exceptions.map { it.taskId }.toSet()
            val orderMap = orders.associate { it.taskId to it.orderIndex }

            val rawList = tasks.mapNotNull { task ->
                if (exceptionIds.contains(task.id)) {
                    null
                } else if (task.repeatMode == "ONCE") {
                    val customOrder = orderMap[task.id]
                    if (customOrder != null) task.copy(orderIndex = customOrder) else task
                } else {
                    val startDate = Instant.ofEpochMilli(task.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        
                    if (doesTaskOccurOnDate(task, startDate, targetDate)) {
                        val isDone = completionIds.contains(task.id)
                        val customOrder = orderMap[task.id]
                        val finalOrder = customOrder ?: task.orderIndex
                        task.copy(date = dateMillis, isCompleted = isDone, orderIndex = finalOrder)
                    } else {
                        null
                    }
                }
            }
            rawList.sortedWith(compareBy<Task> { it.orderIndex }.thenBy { it.createdAt })
        }
    }

    fun getPastUncompletedHighlightedTasks(todayMillis: Long): Flow<List<Task>> {
        return taskDao.getPastUncompletedHighlightedTasks(todayMillis)
    }

    suspend fun saveTaskOrdersForDate(orders: List<com.outlandishomar.acta.data.model.TaskOrder>) {
        if (orders.isNotEmpty()) {
            taskDao.insertTaskOrders(orders)
        }
    }

    suspend fun setTaskCompletionForDate(taskId: Int, dateMillis: Long, isCompleted: Boolean) {
        if (isCompleted) {
            taskDao.insertCompletion(TaskCompletion(taskId, dateMillis))
        } else {
            taskDao.deleteCompletion(taskId, dateMillis)
        }
    }

    suspend fun addException(taskId: Int, dateMillis: Long) {
        taskDao.insertTaskException(com.outlandishomar.acta.data.model.TaskException(taskId, dateMillis))
    }

    fun getInboxTasks(): Flow<List<Task>> =
        taskDao.getInboxTasks()

    suspend fun getTaskById(taskId: Int): Task? = taskDao.getTaskById(taskId)

    suspend fun addTask(task: Task): Long = taskDao.insert(task)

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun updateTasks(tasks: List<Task>) = taskDao.updateTasks(tasks)

    suspend fun deleteTask(task: Task) {
        taskDao.deleteAllCompletionsForTask(task.id)
        taskDao.delete(task)
    }

    suspend fun deleteTasksByIds(taskIds: List<Int>) {
        if (taskIds.isNotEmpty()) {
            taskIds.forEach { id ->
                taskDao.deleteAllCompletionsForTask(id)
            }
            taskDao.deleteTasksByIds(taskIds)
        }
    }

    suspend fun toggleComplete(task: Task) =
        taskDao.update(task.copy(isCompleted = !task.isCompleted))

    suspend fun toggleHighlight(task: Task) =
        taskDao.update(task.copy(isHighlighted = !task.isHighlighted))

    // Sub-tasks
    fun getSubTasksByTaskId(taskId: Int): Flow<List<SubTask>> =
        taskDao.getSubTasksByTaskId(taskId)

    fun getSubTasksForTasks(taskIds: List<Int>): Flow<List<SubTask>> =
        taskDao.getSubTasksForTasks(taskIds)

    suspend fun getSubTasksForTasksNow(taskIds: List<Int>): List<SubTask> =
        taskDao.getSubTasksForTasksNow(taskIds)

    fun getSubTasksForTasks(taskIds: List<Int>, dateMillis: Long, repeatingTaskIds: Set<Int>): Flow<List<SubTask>> {
        val subTasksFlow = taskDao.getSubTasksForTasks(taskIds)
        val completionsFlow = taskDao.getSubTaskCompletionsForDate(dateMillis)

        return subTasksFlow.combine(completionsFlow) { subTasks, completions ->
            val completionIds = completions.map { it.subTaskId }.toSet()
            subTasks.map { subTask ->
                if (repeatingTaskIds.contains(subTask.taskId)) {
                    val isDone = completionIds.contains(subTask.id)
                    subTask.copy(isCompleted = isDone)
                } else {
                    subTask
                }
            }
        }
    }

    suspend fun addSubTask(subTask: SubTask) = taskDao.insertSubTask(subTask)

    suspend fun updateSubTask(subTask: SubTask) = taskDao.updateSubTask(subTask)

    suspend fun deleteSubTask(subTask: SubTask) {
        taskDao.deleteAllCompletionsForSubTask(subTask.id)
        taskDao.deleteSubTask(subTask)
    }

    suspend fun setSubTaskCompletionForDate(subTaskId: Int, dateMillis: Long, isCompleted: Boolean) {
        if (isCompleted) {
            taskDao.insertSubTaskCompletion(com.outlandishomar.acta.data.model.SubTaskCompletion(subTaskId, dateMillis))
        } else {
            taskDao.deleteSubTaskCompletion(subTaskId, dateMillis)
        }
    }

    suspend fun toggleSubTaskComplete(subTask: SubTask) =
        taskDao.updateSubTask(subTask.copy(isCompleted = !subTask.isCompleted))

    suspend fun addTaskWithSubTasks(task: Task, subTaskTitles: List<String>): Long {
        val taskId = taskDao.insert(task).toInt()
        subTaskTitles.forEach { title ->
            taskDao.insertSubTask(
                SubTask(taskId = taskId, title = title.trim())
            )
        }
        return taskId.toLong()
    }

    suspend fun duplicateTaskSameDay(
        taskToDuplicate: Task,
        subTaskTitles: List<String>,
        currentTasks: List<Task>,
        dateMillis: Long
    ): Long {
        val originalIndex = currentTasks.indexOfFirst { it.id == taskToDuplicate.id }
        val insertIndex = if (originalIndex != -1) originalIndex + 1 else currentTasks.size

        val duplicatedTask = taskToDuplicate.copy(
            id = 0,
            date = dateMillis,
            isCompleted = false,
            orderIndex = insertIndex,
            createdAt = System.currentTimeMillis()
        )

        val newTaskId = taskDao.insert(duplicatedTask).toInt()
        subTaskTitles.forEach { title ->
            taskDao.insertSubTask(SubTask(taskId = newTaskId, title = title.trim()))
        }

        val newOrderedTasks = currentTasks.toMutableList()
        val createdTaskWithId = duplicatedTask.copy(id = newTaskId)
        if (originalIndex != -1) {
            newOrderedTasks.add(insertIndex, createdTaskWithId)
        } else {
            newOrderedTasks.add(createdTaskWithId)
        }

        val ordersToSave = mutableListOf<com.outlandishomar.acta.data.model.TaskOrder>()
        val singleTasksToUpdate = mutableListOf<Task>()

        newOrderedTasks.forEachIndexed { index, t ->
            ordersToSave.add(
                com.outlandishomar.acta.data.model.TaskOrder(
                    taskId = t.id,
                    dateMillis = dateMillis,
                    orderIndex = index
                )
            )
            if (t.repeatMode == "ONCE" && t.orderIndex != index) {
                singleTasksToUpdate.add(t.copy(orderIndex = index))
            }
        }

        taskDao.insertTaskOrders(ordersToSave)
        if (singleTasksToUpdate.isNotEmpty()) {
            taskDao.updateTasks(singleTasksToUpdate)
        }

        return newTaskId.toLong()
    }

    private fun doesTaskOccurOnDate(task: Task, startDate: LocalDate, targetDate: LocalDate): Boolean {
        return com.outlandishomar.acta.data.util.RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, targetDate)
    }

    suspend fun getMaxOrderIndex(): Int = taskDao.getMaxOrderIndex() ?: 0

    suspend fun restoreTask(task: Task, subTasks: List<SubTask>, completions: List<TaskCompletion> = emptyList()) {
        taskDao.insert(task)
        if (subTasks.isNotEmpty()) {
            taskDao.insertSubTasks(subTasks)
        }
        if (completions.isNotEmpty()) {
            taskDao.insertCompletions(completions)
        }
    }

    suspend fun removeException(taskId: Int, dateMillis: Long) {
        taskDao.deleteException(taskId, dateMillis)
    }

    suspend fun getAllCompletionsForTask(taskId: Int): List<TaskCompletion> {
        return taskDao.getCompletionsByTaskId(taskId)
    }

    fun getAllCalendarTasksFlow(): Flow<List<Task>> = taskDao.getAllCalendarTasksFlow()
    fun getAllExceptionsFlow(): Flow<List<com.outlandishomar.acta.data.model.TaskException>> = taskDao.getAllExceptionsFlow()
    fun getAllCompletionsFlow(): Flow<List<TaskCompletion>> = taskDao.getAllCompletionsFlow()
}
