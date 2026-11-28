package com.outlandishomar.acta.widget

import android.annotation.TargetApi
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.view.View
import android.widget.RemoteViews
import com.outlandishomar.acta.MainActivity
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.util.RepeatTaskCalculator
import com.outlandishomar.acta.ui.components.RichTextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class WidgetPalette(
    val background: Int,
    val card: Int,
    val text: Int,
    val mutedText: Int,
    val faintNotesText: Int,
    val hintText: Int,
    val disabledText: Int,
    val checkboxBorder: Int,
    val accent: Int,
    val onAccent: Int,
    val border: Int
)

data class WidgetSubTaskItem(
    val id: Int,
    val taskId: Int,
    val title: String,
    val isCompleted: Boolean
)

data class WidgetTaskItem(
    val id: Int,
    val title: String,
    val iconName: String?,
    val notesPreview: String?,
    val showNotesIconOnly: Boolean,
    val timeLabel: String?,
    val hasAlert: Boolean,
    val repeatMode: String,
    val dateMillis: Long,
    val subTasks: List<WidgetSubTaskItem>
)

data class WidgetSnapshot(
    val dateLabel: String,
    val tasks: List<WidgetTaskItem>,
    val palette: WidgetPalette
)

object OptimisticCheckoffStore {
    private val checkedTimestamps = ConcurrentHashMap<Int, Long>()
    private val subTaskOverrides = ConcurrentHashMap<Int, Pair<Boolean, Long>>()
    private const val TTL_MS = 10_000L

    fun markChecked(taskId: Int) {
        checkedTimestamps[taskId] = System.currentTimeMillis()
    }

    fun clear(taskId: Int) {
        checkedTimestamps.remove(taskId)
    }

    fun isChecked(taskId: Int): Boolean {
        val ts = checkedTimestamps[taskId] ?: return false
        if (System.currentTimeMillis() - ts > TTL_MS) {
            checkedTimestamps.remove(taskId)
            return false
        }
        return true
    }

    fun setSubTaskOverride(subTaskId: Int, isCompleted: Boolean) {
        subTaskOverrides[subTaskId] = isCompleted to System.currentTimeMillis()
    }

    fun clearSubTaskOverride(subTaskId: Int) {
        subTaskOverrides.remove(subTaskId)
    }

    fun getSubTaskOverride(subTaskId: Int): Boolean? {
        val pair = subTaskOverrides[subTaskId] ?: return null
        if (System.currentTimeMillis() - pair.second > TTL_MS) {
            subTaskOverrides.remove(subTaskId)
            return null
        }
        return pair.first
    }
}

object WidgetPaletteResolver {
    private fun blend(c1: Int, c2: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = (Color.red(c1) * inv + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * inv + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * inv + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun parseHex(hexStr: String?): Int? {
        if (hexStr.isNullOrBlank()) return null
        return try {
            val clean = hexStr.trim().removePrefix("#")
            when (clean.length) {
                6 -> Color.parseColor("#FF$clean")
                8 -> Color.parseColor("#$clean")
                3 -> {
                    val expanded = clean.map { "$it$it" }.joinToString("")
                    Color.parseColor("#FF$expanded")
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun luminance(color: Int): Float {
        return (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
    }

    fun resolve(context: Context): WidgetPalette {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        var mode = prefs.getString("mode", "SYSTEM") ?: "SYSTEM"
        if (mode == "GREY") mode = "DARK"

        val customPrimary = parseHex(prefs.getString("custom_primary", null))
        val customSecondary = parseHex(prefs.getString("custom_secondary", null))

        if (customPrimary != null) {
            val lum = luminance(customPrimary)
            val isDark = lum <= 0.5f
            return if (isDark) {
                val accent = customSecondary ?: Color.parseColor("#E0E0E0")
                val onAccent = if (luminance(accent) > 0.5f) Color.parseColor("#18181A") else Color.WHITE
                val muted = Color.parseColor("#AAAAAA")
                WidgetPalette(
                    background = customPrimary,
                    card = blend(customPrimary, Color.WHITE, 0.12f),
                    text = Color.WHITE,
                    mutedText = muted,
                    faintNotesText = withAlpha(muted, 155),
                    hintText = withAlpha(muted, 128),
                    disabledText = Color.parseColor("#757575"),
                    checkboxBorder = muted,
                    accent = accent,
                    onAccent = onAccent,
                    border = withAlpha(Color.WHITE, 38)
                )
            } else {
                val useDarkTint = lum > 0.85f
                val card = if (useDarkTint) blend(customPrimary, Color.BLACK, 0.08f) else blend(customPrimary, Color.WHITE, 0.22f)
                val accent = customSecondary ?: Color.parseColor("#1E1E1E")
                val onAccent = if (luminance(accent) > 0.5f) Color.parseColor("#1E1E1E") else Color.WHITE
                val text = Color.parseColor("#1E1E1E")
                val muted = Color.parseColor("#555555")
                WidgetPalette(
                    background = customPrimary,
                    card = card,
                    text = text,
                    mutedText = muted,
                    faintNotesText = withAlpha(muted, 155),
                    hintText = withAlpha(muted, 128),
                    disabledText = Color.parseColor("#888888"),
                    checkboxBorder = text,
                    accent = accent,
                    onAccent = onAccent,
                    border = withAlpha(Color.BLACK, 38)
                )
            }
        }

        val isSysDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        return when (mode) {
            "ROSY_TAUPE" -> {
                val text = Color.parseColor("#1E1E1E")
                val muted = Color.parseColor("#5A4341")
                WidgetPalette(
                    background = Color.parseColor("#C89F9C"),
                    card = Color.parseColor("#D8B2AF"),
                    text = text,
                    mutedText = muted,
                    faintNotesText = withAlpha(muted, 155),
                    hintText = withAlpha(muted, 128),
                    disabledText = Color.parseColor("#8A6E6C"),
                    checkboxBorder = text,
                    accent = Color.parseColor("#1E1E1E"),
                    onAccent = Color.parseColor("#DFC0BD"),
                    border = withAlpha(Color.parseColor("#1E1E1E"), 45)
                )
            }
            "DUST_GREY" -> {
                val text = Color.parseColor("#212121")
                val muted = Color.parseColor("#757575")
                WidgetPalette(
                    background = Color.parseColor("#D1D1D1"),
                    card = Color.parseColor("#DCDCE0"),
                    text = text,
                    mutedText = muted,
                    faintNotesText = withAlpha(muted, 155),
                    hintText = withAlpha(muted, 128),
                    disabledText = Color.parseColor("#BDBDBD"),
                    checkboxBorder = text,
                    accent = Color.parseColor("#1E1E1E"),
                    onAccent = Color.parseColor("#FAFAFA"),
                    border = withAlpha(Color.parseColor("#1E1E1E"), 40)
                )
            }
            "SILVER" -> {
                val text = Color.parseColor("#212121")
                val muted = Color.parseColor("#4F575B")
                WidgetPalette(
                    background = Color.parseColor("#B0BBBF"),
                    card = Color.parseColor("#C0CBCE"),
                    text = text,
                    mutedText = muted,
                    faintNotesText = withAlpha(muted, 155),
                    hintText = withAlpha(muted, 128),
                    disabledText = Color.parseColor("#7C8589"),
                    checkboxBorder = text,
                    accent = Color.parseColor("#212121"),
                    onAccent = Color.parseColor("#CCD6D9"),
                    border = withAlpha(Color.parseColor("#212121"), 45)
                )
            }
            "ASH_GREY" -> {
                val text = Color.parseColor("#212121")
                val muted = Color.parseColor("#4A4D43")
                WidgetPalette(
                    background = Color.parseColor("#A1A499"),
                    card = Color.parseColor("#B1B4A9"),
                    text = text,
                    mutedText = muted,
                    faintNotesText = withAlpha(muted, 155),
                    hintText = withAlpha(muted, 128),
                    disabledText = Color.parseColor("#75786E"),
                    checkboxBorder = text,
                    accent = Color.parseColor("#212121"),
                    onAccent = Color.parseColor("#BDBFA6"),
                    border = withAlpha(Color.parseColor("#212121"), 45)
                )
            }
            "SEASHELL", "LIGHT" -> lightDefaultPalette()
            "DARK" -> darkDefaultPalette()
            else -> if (isSysDark) darkDefaultPalette() else lightDefaultPalette()
        }
    }

    private fun darkDefaultPalette(): WidgetPalette {
        val muted = Color.parseColor("#AEAEB2")
        return WidgetPalette(
            background = Color.parseColor("#1C1C1E"),
            card = Color.parseColor("#2C2C2E"),
            text = Color.parseColor("#F2F2F7"),
            mutedText = muted,
            faintNotesText = withAlpha(muted, 155),
            hintText = withAlpha(muted, 128),
            disabledText = Color.parseColor("#757575"),
            checkboxBorder = muted,
            accent = Color.parseColor("#E0E0E0"),
            onAccent = Color.parseColor("#1C1C1E"),
            border = Color.parseColor("#38383A")
        )
    }

    private fun lightDefaultPalette(): WidgetPalette {
        val text = Color.parseColor("#212121")
        val muted = Color.parseColor("#6E6462")
        return WidgetPalette(
            background = Color.parseColor("#EEE2DF"),
            card = Color.parseColor("#F8EFEF"),
            text = text,
            mutedText = muted,
            faintNotesText = withAlpha(muted, 155),
            hintText = withAlpha(muted, 128),
            disabledText = Color.parseColor("#A09694"),
            checkboxBorder = text,
            accent = Color.parseColor("#212121"),
            onAccent = Color.parseColor("#FAF3F3"),
            border = withAlpha(Color.parseColor("#212121"), 45)
        )
    }
}

object WidgetTaskLoader {
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private const val SINGLE_LINE_NOTES_MAX_CHARS = 42

    fun loadSnapshot(context: Context): WidgetSnapshot = runBlocking(Dispatchers.IO) {
        val today = LocalDate.now()
        val dateMillis = today
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val taskDao = AppDatabase.getInstance(context).taskDao()
        val rawTasks = taskDao.getTasksByDateNow(dateMillis)
        val completions = taskDao.getCompletionsForDateNow(dateMillis)
        val exceptions = taskDao.getExceptionsForDateNow(dateMillis)
        val orders = taskDao.getOrdersForDateNow(dateMillis)

        val completionIds = completions.map { it.taskId }.toSet()
        val exceptionIds = exceptions.map { it.taskId }.toSet()
        val orderMap = orders.associate { it.taskId to it.orderIndex }

        val resolvedTasks = rawTasks.mapNotNull { task ->
            if (task.isHighlighted || exceptionIds.contains(task.id)) {
                null
            } else if (task.repeatMode == "ONCE") {
                val customOrder = orderMap[task.id]
                if (customOrder != null) task.copy(orderIndex = customOrder) else task
            } else {
                val startDate = Instant.ofEpochMilli(task.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (RepeatTaskCalculator.doesTaskOccurOnDate(task, startDate, today)) {
                    val isDone = completionIds.contains(task.id)
                    val finalOrder = orderMap[task.id] ?: task.orderIndex
                    task.copy(date = dateMillis, isCompleted = isDone, orderIndex = finalOrder)
                } else {
                    null
                }
            }
        }
            .filter { !it.isCompleted && !OptimisticCheckoffStore.isChecked(it.id) }
            .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))

        val taskIds = resolvedTasks.map { it.id }
        val repeatingTaskIds = resolvedTasks.filter { it.repeatMode != "ONCE" }.map { it.id }.toSet()
        val rawSubTasks = if (taskIds.isNotEmpty()) taskDao.getSubTasksForTasksNow(taskIds) else emptyList()
        val subCompletions = if (taskIds.isNotEmpty()) taskDao.getSubTaskCompletionsForDateNow(dateMillis) else emptyList()
        val subCompletionIds = subCompletions.map { it.subTaskId }.toSet()

        val subTasksByTask = rawSubTasks.map { sub ->
            val dbDone = if (repeatingTaskIds.contains(sub.taskId)) {
                subCompletionIds.contains(sub.id)
            } else {
                sub.isCompleted
            }
            val effectiveDone = OptimisticCheckoffStore.getSubTaskOverride(sub.id) ?: dbDone
            WidgetSubTaskItem(
                id = sub.id,
                taskId = sub.taskId,
                title = sub.title,
                isCompleted = effectiveDone
            )
        }.groupBy { it.taskId }

        val items = resolvedTasks.map { task ->
            val rawPlainNotes = RichTextHelper.parseToRichSpans(task.description).first.trim()
            val nonBlankLines = rawPlainNotes.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val hasNotes = nonBlankLines.isNotEmpty()
            val isMultiLineNotes = nonBlankLines.size > 1 || (nonBlankLines.firstOrNull()?.length ?: 0) > SINGLE_LINE_NOTES_MAX_CHARS

            val notesPreview = if (hasNotes && !isMultiLineNotes) nonBlankLines.first() else null
            val showNotesIconOnly = hasNotes && isMultiLineNotes

            val timeLabel = if (task.taskTimeHour != null && task.taskTimeMinute != null) {
                val h = if (task.taskTimeHour % 12 == 0) 12 else task.taskTimeHour % 12
                val ap = if (task.taskTimeHour < 12) "AM" else "PM"
                String.format(Locale.US, "%d:%02d %s", h, task.taskTimeMinute, ap)
            } else {
                null
            }
            val hasAlert = timeLabel != null && (
                task.alertEnabled ||
                    com.outlandishomar.acta.data.model.CustomAlert.parseList(task.additionalAlerts).any { it.isEnabled }
            )
            WidgetTaskItem(
                id = task.id,
                title = task.title,
                iconName = task.iconName,
                notesPreview = notesPreview,
                showNotesIconOnly = showNotesIconOnly,
                timeLabel = timeLabel,
                hasAlert = hasAlert,
                repeatMode = task.repeatMode,
                dateMillis = dateMillis,
                subTasks = subTasksByTask[task.id] ?: emptyList()
            )
        }

        WidgetSnapshot(
            dateLabel = today.format(DATE_FORMATTER),
            tasks = items,
            palette = WidgetPaletteResolver.resolve(context)
        )
    }
}

object WidgetRenderer {
    private const val REQUEST_CAPTURE = 4612
    private const val REQUEST_ROW = 4613
    private const val REQUEST_HOME_BASE = 3 shl 20
    internal const val DIRECT_COLLECTION_BUDGET_BYTES = 256 * 1024

    val WORKER = Executors.newSingleThreadExecutor { task -> Thread(task, "acta-widget") }

    fun refreshAllAsync(context: Context) {
        val app = context.applicationContext
        WORKER.execute {
            runCatching { refreshAll(app) }
        }
    }

    fun refreshAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(app, TaskWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        render(app, manager, ids)
    }

    fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val snapshot = WidgetTaskLoader.loadSnapshot(context)
        for (id in ids) {
            val views = buildViews(context, id, snapshot)
            manager.updateAppWidget(id, views)
        }
        if (!usesDirectCollections(Build.VERSION.SDK_INT)) {
            @Suppress("DEPRECATION")
            manager.notifyAppWidgetViewDataChanged(ids, R.id.acta_widget_list)
        }
        TaskWidgetReceiver.scheduleMidnightRefresh(context)
    }

    private fun buildViews(context: Context, appWidgetId: Int, snapshot: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.acta_widget)
        val palette = snapshot.palette

        views.setTextViewText(R.id.acta_widget_title, snapshot.dateLabel)
        if (snapshot.tasks.isNotEmpty()) {
            views.setTextViewText(R.id.acta_widget_subtitle, snapshot.tasks.size.toString())
            views.setViewVisibility(R.id.acta_widget_subtitle, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.acta_widget_subtitle, View.GONE)
        }

        views.setTextViewText(R.id.acta_widget_empty, "No tasks for today")
        views.setViewVisibility(R.id.acta_widget_empty, if (snapshot.tasks.isEmpty()) View.VISIBLE else View.GONE)

        views.setInt(R.id.acta_widget_surface, "setColorFilter", palette.background)
        views.setInt(R.id.acta_widget_band, "setColorFilter", palette.background)
        views.setTextColor(R.id.acta_widget_title, palette.text)
        views.setTextColor(R.id.acta_widget_subtitle, palette.mutedText)
        views.setTextColor(R.id.acta_widget_capture, palette.accent)
        views.setInt(R.id.acta_widget_header_divider, "setBackgroundColor", palette.border)
        views.setTextColor(R.id.acta_widget_empty, palette.mutedText)

        views.setOnClickPendingIntent(
            R.id.acta_widget_capture,
            capturePendingIntent(context, appWidgetId)
        )

        val openHomeIntent = PendingIntent.getActivity(
            context,
            REQUEST_HOME_BASE + appWidgetId,
            appIntent(context),
            immutableFlags()
        )
        views.setOnClickPendingIntent(R.id.acta_widget_title_target, openHomeIntent)
        views.setOnClickPendingIntent(R.id.acta_widget_root, openHomeIntent)
        views.setOnClickPendingIntent(R.id.acta_widget_empty, openHomeIntent)
        views.setOnClickPendingIntent(R.id.acta_widget_spacer, openHomeIntent)

        bindCollection(context, views, appWidgetId, snapshot)
        return views
    }

    internal fun usesDirectCollections(sdkInt: Int): Boolean = sdkInt >= 31

    @TargetApi(31)
    private fun bindDirectCollection(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        snapshot: WidgetSnapshot
    ) {
        val factory = TasksWidgetFactory(context, appWidgetId, snapshot)
        val collection = buildDirectCollection(factory)
        views.setRemoteAdapter(R.id.acta_widget_list, collection)
    }

    @TargetApi(31)
    private fun buildDirectCollection(
        factory: TasksWidgetFactory,
        budgetBytes: Int = DIRECT_COLLECTION_BUDGET_BYTES
    ): RemoteViews.RemoteCollectionItems {
        val count = factory.count
        fun buildForLimit(limit: Int): RemoteViews.RemoteCollectionItems {
            val builder = RemoteViews.RemoteCollectionItems.Builder()
                .setViewTypeCount(factory.viewTypeCount)
                .setHasStableIds(true)
            for (i in 0 until limit) {
                builder.addItem(factory.getItemId(i), factory.getViewAt(i, limit))
            }
            return builder.build()
        }

        val full = buildForLimit(count)
        if (count == 0 || parcelSize(full) <= budgetBytes) return full

        var low = 1
        var high = count - 1
        var best = buildForLimit(1)
        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidate = buildForLimit(mid)
            if (parcelSize(candidate) <= budgetBytes) {
                best = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }

    @TargetApi(31)
    private fun parcelSize(collection: RemoteViews.RemoteCollectionItems): Int {
        val parcel = Parcel.obtain()
        return try {
            collection.writeToParcel(parcel, 0)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }

    private fun bindCollection(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        snapshot: WidgetSnapshot
    ) {
        if (usesDirectCollections(Build.VERSION.SDK_INT)) {
            bindDirectCollection(context, views, appWidgetId, snapshot)
        } else {
            @Suppress("DEPRECATION")
            val adapterIntent = Intent(context, TasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.acta_widget_list, adapterIntent)
        }
        views.setEmptyView(R.id.acta_widget_list, R.id.acta_widget_empty)

        val rowTemplate = Intent(context, WidgetTapActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val mutableFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        views.setPendingIntentTemplate(
            R.id.acta_widget_list,
            PendingIntent.getActivity(context, REQUEST_ROW, rowTemplate, mutableFlags)
        )
    }

    internal fun capturePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, QuickCaptureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse("acta-widget://capture/$appWidgetId")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getActivity(context, REQUEST_CAPTURE + appWidgetId, intent, immutableFlags())
    }

    internal fun appIntent(context: Context, openTaskId: Int = -1): Intent =
        Intent(context, MainActivity::class.java).apply {
            if (openTaskId != -1) {
                putExtra("open_task_id", openTaskId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

    private fun immutableFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}

class TaskWidget {
    fun updateAll(context: Context) {
        WidgetRenderer.refreshAllAsync(context)
    }

    companion object {
        fun updateAll(context: Context) {
            WidgetRenderer.refreshAllAsync(context)
        }
    }
}
