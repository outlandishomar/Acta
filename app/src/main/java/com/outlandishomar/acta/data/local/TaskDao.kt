package com.outlandishomar.acta.data.local

import androidx.room.*
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.SubTaskCompletion
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.model.TaskCompletion
import com.outlandishomar.acta.data.model.TaskException
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE (repeatMode = 'ONCE' AND date = :dateMillis) OR (repeatMode != 'ONCE' AND date <= :dateMillis AND date != 0 AND (endDate IS NULL OR endDate >= :dateMillis)) ORDER BY orderIndex ASC, createdAt ASC")
    fun getTasksByDate(dateMillis: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE (repeatMode = 'ONCE' AND date = :dateMillis) OR (repeatMode != 'ONCE' AND date <= :dateMillis AND date != 0 AND (endDate IS NULL OR endDate >= :dateMillis)) ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getTasksByDateNow(dateMillis: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE isHighlighted = 1 AND isCompleted = 0 AND date < :todayMillis AND date != 0 ORDER BY orderIndex ASC, createdAt ASC")
    fun getPastUncompletedHighlightedTasks(todayMillis: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: TaskCompletion)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND dateMillis = :dateMillis")
    suspend fun deleteCompletion(taskId: Int, dateMillis: Long)

    @Query("SELECT * FROM task_completions WHERE dateMillis = :dateMillis")
    fun getCompletionsForDate(dateMillis: Long): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions WHERE dateMillis = :dateMillis")
    suspend fun getCompletionsForDateNow(dateMillis: Long): List<TaskCompletion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskException(exception: TaskException)

    @Query("SELECT * FROM task_exceptions WHERE dateMillis = :dateMillis")
    fun getExceptionsForDate(dateMillis: Long): Flow<List<TaskException>>

    @Query("DELETE FROM task_exceptions WHERE taskId = :taskId AND dateMillis = :dateMillis")
    suspend fun deleteException(taskId: Int, dateMillis: Long)

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId")
    suspend fun getCompletionsByTaskId(taskId: Int): List<TaskCompletion>

    @Query("DELETE FROM task_completions WHERE taskId = :taskId")
    suspend fun deleteAllCompletionsForTask(taskId: Int)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks WHERE date = 0 ORDER BY orderIndex ASC, createdAt ASC")
    fun getInboxTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Delete
    suspend fun delete(task: Task)

    // Sub-tasks
    @Query("SELECT * FROM sub_tasks WHERE id = :subTaskId")
    suspend fun getSubTaskById(subTaskId: Int): SubTask?

    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getSubTasksByTaskId(taskId: Int): Flow<List<SubTask>>

    @Query("SELECT * FROM sub_tasks WHERE taskId IN (:taskIds) ORDER BY createdAt ASC")
    fun getSubTasksForTasks(taskIds: List<Int>): Flow<List<SubTask>>

    @Query("SELECT * FROM sub_tasks WHERE taskId IN (:taskIds) ORDER BY createdAt ASC")
    suspend fun getSubTasksForTasksNow(taskIds: List<Int>): List<SubTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask)

    @Update
    suspend fun updateSubTask(subTask: SubTask)

    @Delete
    suspend fun deleteSubTask(subTask: SubTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTaskCompletion(completion: SubTaskCompletion)

    @Query("DELETE FROM sub_task_completions WHERE subTaskId = :subTaskId AND dateMillis = :dateMillis")
    suspend fun deleteSubTaskCompletion(subTaskId: Int, dateMillis: Long)

    @Query("SELECT * FROM sub_task_completions WHERE dateMillis = :dateMillis")
    fun getSubTaskCompletionsForDate(dateMillis: Long): Flow<List<SubTaskCompletion>>

    @Query("DELETE FROM sub_task_completions WHERE subTaskId = :subTaskId")
    suspend fun deleteAllCompletionsForSubTask(subTaskId: Int)

    // Backup – bulk queries
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM sub_tasks")
    suspend fun getAllSubTasks(): List<SubTask>

    @Query("SELECT * FROM task_completions")
    suspend fun getAllCompletions(): List<TaskCompletion>

    @Query("SELECT * FROM sub_task_completions")
    suspend fun getAllSubTaskCompletions(): List<SubTaskCompletion>

    @Query("SELECT * FROM task_exceptions")
    suspend fun getAllExceptions(): List<TaskException>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletions(completions: List<TaskCompletion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTaskCompletions(completions: List<SubTaskCompletion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExceptions(exceptions: List<TaskException>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    suspend fun deleteTasksByIds(taskIds: List<Int>)

    @Query("SELECT MAX(orderIndex) FROM tasks")
    suspend fun getMaxOrderIndex(): Int?

    @Query("SELECT * FROM tasks WHERE date != 0")
    fun getAllCalendarTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM task_exceptions")
    fun getAllExceptionsFlow(): Flow<List<TaskException>>

    @Query("SELECT * FROM task_completions")
    fun getAllCompletionsFlow(): Flow<List<TaskCompletion>>

    // Task Orders per date
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskOrders(orders: List<com.outlandishomar.acta.data.model.TaskOrder>)

    @Query("SELECT * FROM task_orders WHERE dateMillis = :dateMillis")
    fun getOrdersForDate(dateMillis: Long): Flow<List<com.outlandishomar.acta.data.model.TaskOrder>>

    @Query("SELECT * FROM task_orders")
    suspend fun getAllTaskOrders(): List<com.outlandishomar.acta.data.model.TaskOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskOrdersBackup(orders: List<com.outlandishomar.acta.data.model.TaskOrder>)
}
