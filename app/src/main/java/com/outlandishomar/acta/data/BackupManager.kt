package com.outlandishomar.acta.data

import android.content.Context
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.model.TaskCompletion
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

class BackupManager(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).taskDao()

    // ── Export ────────────────────────────────────────────────────────

    suspend fun exportToStream(outputStream: OutputStream) {
        val tasks = dao.getAllTasks()
        val subTasks = dao.getAllSubTasks()
        val completions = dao.getAllCompletions()
        val orders = dao.getAllTaskOrders()

        val themePrefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val root = JSONObject().apply {
            put("version", 1)
            put("exportDate", Instant.now().toString())

            put("settings", JSONObject().apply {
                put("themeMode", themePrefs.getString("mode", "SYSTEM"))
                put("firstDayOfWeek", appPrefs.getInt("first_day_of_week", 1))
            })

            put("tasks", JSONArray().apply {
                tasks.forEach { t -> put(taskToJson(t)) }
            })

            put("subTasks", JSONArray().apply {
                subTasks.forEach { s -> put(subTaskToJson(s)) }
            })

            put("completions", JSONArray().apply {
                completions.forEach { c -> put(completionToJson(c)) }
            })

            put("orders", JSONArray().apply {
                orders.forEach { o -> put(orderToJson(o)) }
            })
        }

        outputStream.bufferedWriter().use { it.write(root.toString(2)) }
    }

    // ── Import ────────────────────────────────────────────────────────

    suspend fun importFromStream(inputStream: InputStream) {
        val json = inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(json)

        // Parse settings
        val settings = root.optJSONObject("settings")
        if (settings != null) {
            val themeMode = settings.optString("themeMode", "SYSTEM")
            context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .edit().putString("mode", themeMode).apply()

            val firstDay = settings.optInt("firstDayOfWeek", 1)
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit().putInt("first_day_of_week", firstDay).apply()
        }

        // Parse data
        val tasks = jsonArrayToList(root.optJSONArray("tasks")) { jsonToTask(it) }
        val subTasks = jsonArrayToList(root.optJSONArray("subTasks")) { jsonToSubTask(it) }
        val completions = jsonArrayToList(root.optJSONArray("completions")) { jsonToCompletion(it) }
        val orders = jsonArrayToList(root.optJSONArray("orders")) { jsonToOrder(it) }

        // Clear and insert (deleteAllTasks cascades to sub_tasks, task_completions, and task_orders)
        dao.deleteAllTasks()
        dao.insertTasks(tasks)
        dao.insertSubTasks(subTasks)
        dao.insertCompletions(completions)
        dao.insertTaskOrdersBackup(orders)
    }

    // ── JSON ↔ Model helpers ──────────────────────────────────────────

    private fun taskToJson(t: Task) = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("description", t.description)
        put("date", t.date)
        put("isCompleted", t.isCompleted)
        put("isHighlighted", t.isHighlighted)
        put("iconName", t.iconName ?: JSONObject.NULL)
        put("orderIndex", t.orderIndex)
        put("createdAt", t.createdAt)
        put("taskTimeHour", t.taskTimeHour ?: JSONObject.NULL)
        put("taskTimeMinute", t.taskTimeMinute ?: JSONObject.NULL)
        put("repeatMode", t.repeatMode)
        put("repeatInterval", t.repeatInterval)
        put("repeatDays", t.repeatDays)
        put("endDate", t.endDate ?: JSONObject.NULL)
        put("alertEnabled", t.alertEnabled)
        put("additionalAlerts", t.additionalAlerts)
    }

    private fun jsonToTask(j: JSONObject) = Task(
        id = j.getInt("id"),
        title = j.getString("title"),
        description = j.optString("description", ""),
        date = j.getLong("date"),
        isCompleted = j.optBoolean("isCompleted", false),
        isHighlighted = j.optBoolean("isHighlighted", false),
        iconName = j.opt("iconName")?.takeIf { it != JSONObject.NULL }?.toString(),
        orderIndex = j.optInt("orderIndex", 0),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        taskTimeHour = j.opt("taskTimeHour")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull(),
        taskTimeMinute = j.opt("taskTimeMinute")?.takeIf { it != JSONObject.NULL }?.toString()?.toIntOrNull(),
        repeatMode = j.optString("repeatMode", "ONCE"),
        repeatInterval = j.optInt("repeatInterval", 1),
        repeatDays = j.optString("repeatDays", ""),
        endDate = j.opt("endDate")?.takeIf { it != JSONObject.NULL }?.toString()?.toLongOrNull(),
        alertEnabled = j.optBoolean("alertEnabled", false),
        additionalAlerts = j.optString("additionalAlerts", "")
    )

    private fun subTaskToJson(s: SubTask) = JSONObject().apply {
        put("id", s.id)
        put("taskId", s.taskId)
        put("title", s.title)
        put("isCompleted", s.isCompleted)
        put("createdAt", s.createdAt)
    }

    private fun jsonToSubTask(j: JSONObject) = SubTask(
        id = j.getInt("id"),
        taskId = j.getInt("taskId"),
        title = j.getString("title"),
        isCompleted = j.optBoolean("isCompleted", false),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    private fun completionToJson(c: TaskCompletion) = JSONObject().apply {
        put("taskId", c.taskId)
        put("dateMillis", c.dateMillis)
    }

    private fun jsonToCompletion(j: JSONObject) = TaskCompletion(
        taskId = j.getInt("taskId"),
        dateMillis = j.getLong("dateMillis")
    )

    private fun orderToJson(o: com.outlandishomar.acta.data.model.TaskOrder) = JSONObject().apply {
        put("taskId", o.taskId)
        put("dateMillis", o.dateMillis)
        put("orderIndex", o.orderIndex)
    }

    private fun jsonToOrder(j: JSONObject) = com.outlandishomar.acta.data.model.TaskOrder(
        taskId = j.getInt("taskId"),
        dateMillis = j.getLong("dateMillis"),
        orderIndex = j.getInt("orderIndex")
    )

    private fun <T> jsonArrayToList(arr: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { mapper(arr.getJSONObject(it)) }
    }
}
