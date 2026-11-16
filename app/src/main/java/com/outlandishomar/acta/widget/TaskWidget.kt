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
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.view.View
import android.widget.RemoteViews
import com.outlandishomar.acta.MainActivity
import com.outlandishomar.acta.R
import com.outlandishomar.acta.data.local.AppDatabase
import com.outlandishomar.acta.data.model.TaskCompletion
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
    val dialogBackground: Int,
    val card: Int,
    val text: Int,
    val mutedText: Int,
    val hintText: Int,
    val disabledText: Int,
    val checkboxBorder: Int,
    val accent: Int,
    val onAccent: Int,
    val border: Int
)

data class WidgetTaskItem(
    val id: Int,
    val title: String,
    val notesPreview: String?,
    val timeLabel: String?,
    val repeatMode: String,
    val dateMillis: Long,
    val animPhase: Int? = null
)

data class WidgetSnapshot(
    val dateLabel: String,
    val tasks: List<WidgetTaskItem>,
    val palette: WidgetPalette
)

/**
 * Coordinates the multi-step widget checkoff animation (square check -> pencil strikethrough ->
 * inward shrink & fade -> removal) while immediately persisting completion to Room DB.
 */
object WidgetCheckoffAnimator {
    private data class AnimEntry(
        val item: WidgetTaskItem,
        @Volatile var phase: Int,
        val originalOrder: Int
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val animatingTasks = ConcurrentHashMap<Int, AnimEntry>()
    private val recentlyCompleted = ConcurrentHashMap<Int, Long>()
    private val cachedOrderCounter = java.util.concurrent.atomic.AtomicInteger(0)

    private const val RECENTLY_COMPLETED_TTL_MS = 10_000L

    private val PHASE_DELAYS_MS = longArrayOf(
        0L,    // Phase 0: Square checkbox checked + 40% pencil strike
        85L,   // Phase 1: 75% pencil strike
        170L,  // Phase 2: 100% pencil strike + dimmed settled look
        340L,  // Phase 3: Begin shrinking inward & fading
        425L,  // Phase 4: Shrink inward & fade further
        510L,  // Phase 5: Almost gone
        595L   // Final: Remove from widget list
    )

    fun isAnimating(taskId: Int): Boolean = animatingTasks.containsKey(taskId)

    fun isRecentlyCompleted(taskId: Int): Boolean {
        val ts = recentlyCompleted[taskId] ?: return false
        if (System.currentTimeMillis() - ts > RECENTLY_COMPLETED_TTL_MS) {
            recentlyCompleted.remove(taskId)
            return false
        }
        return true
    }

    fun activeAnimatingEntries(): List<Pair<WidgetTaskItem, Int>> =
        animatingTasks.values.map { it.item.copy(animPhase = it.phase) to it.originalOrder }

    fun hasActiveAnimations(): Boolean = animatingTasks.isNotEmpty()

    fun startCheckoff(context: Context, taskId: Int, repeatMode: String, dateMillis: Long) {
        if (taskId == -1 || animatingTasks.containsKey(taskId) || isRecentlyCompleted(taskId)) return
        val appContext = context.applicationContext

        // Persist completion immediately in background so the app reflects it even before animation ends
        WidgetRenderer.WORKER.execute {
            runCatching {
                runBlocking(Dispatchers.IO) {
                    val taskDao = AppDatabase.getInstance(appContext).taskDao()
                    if (repeatMode != "ONCE" && dateMillis != -1L) {
                        taskDao.insertCompletion(TaskCompletion(taskId, dateMillis))
                    } else {
                        val task = taskDao.getTaskById(taskId)
                        if (task != null) {
                            taskDao.update(task.copy(isCompleted = true))
                        }
                    }
                }
            }
        }

        // Capture the item from cached snapshot (or load DB snapshot if cache cold)
        WidgetRenderer.WORKER.execute {
            val baseSnapshot = WidgetTaskLoader.lastSnapshot
                ?: WidgetTaskLoader.loadSnapshot(appContext, allowCachedForAnimation = false)
            val index = baseSnapshot.tasks.indexOfFirst { it.id == taskId }
            val item = if (index >= 0) {
                baseSnapshot.tasks[index]
            } else {
                WidgetTaskItem(
                    id = taskId,
                    title = "",
                    notesPreview = null,
                    timeLabel = null,
                    repeatMode = repeatMode,
                    dateMillis = dateMillis
                )
            }
            val orderHint = if (index >= 0) index else cachedOrderCounter.getAndIncrement()
            animatingTasks[taskId] = AnimEntry(item = item, phase = 0, originalOrder = orderHint)

            // Frame 0 immediately
            WidgetRenderer.renderAnimationFrame(appContext)

            // Schedule phases 1..5
            for (phase in 1..5) {
                mainHandler.postDelayed({
                    val entry = animatingTasks[taskId] ?: return@postDelayed
                    entry.phase = phase
                    WidgetRenderer.WORKER.execute {
                        if (animatingTasks.containsKey(taskId)) {
                            WidgetRenderer.renderAnimationFrame(appContext)
                        }
                    }
                }, PHASE_DELAYS_MS[phase])
            }

            // Final removal frame
            mainHandler.postDelayed({
                animatingTasks.remove(taskId)
                recentlyCompleted[taskId] = System.currentTimeMillis()
                WidgetRenderer.WORKER.execute {
                    WidgetRenderer.refreshAll(appContext)
                }
            }, PHASE_DELAYS_MS[6])
        }
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
                    dialogBackground = customPrimary,
                    card = blend(customPrimary, Color.WHITE, 0.12f),
                    text = Color.WHITE,
                    mutedText = muted,
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
                    dialogBackground = customPrimary,
                    card = card,
                    text = text,
                    mutedText = muted,
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
                    dialogBackground = Color.parseColor("#C89F9C"),
                    card = Color.parseColor("#D8B2AF"),
                    text = text,
                    mutedText = muted,
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
                    dialogBackground = Color.parseColor("#D1D1D1"),
                    card = Color.parseColor("#DCDCE0"),
                    text = text,
                    mutedText = muted,
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
                    dialogBackground = Color.parseColor("#B0BBBF"),
                    card = Color.parseColor("#C0CBCE"),
                    text = text,
                    mutedText = muted,
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
                    dialogBackground = Color.parseColor("#A1A499"),
                    card = Color.parseColor("#B1B4A9"),
                    text = text,
                    mutedText = muted,
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
            dialogBackground = Color.parseColor("#343436"),
            card = Color.parseColor("#2C2C2E"),
            text = Color.parseColor("#F2F2F7"),
            mutedText = muted,
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
            dialogBackground = Color.parseColor("#EEE2DF"),
            card = Color.parseColor("#F8EFEF"),
            text = text,
            mutedText = muted,
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

    @Volatile
    var lastSnapshot: WidgetSnapshot? = null
        private set

    fun loadSnapshot(context: Context, allowCachedForAnimation: Boolean = false): WidgetSnapshot {
        val animEntries = WidgetCheckoffAnimator.activeAnimatingEntries()
        if (allowCachedForAnimation) {
            lastSnapshot?.let { cached ->
                val animMap = animEntries.associate { it.first.id to it.first }
                val merged = cached.tasks
                    .filter { task -> animMap.containsKey(task.id) || !WidgetCheckoffAnimator.isRecentlyCompleted(task.id) }
                    .map { task -> animMap[task.id] ?: task }
                return cached.copy(tasks = merged)
            }
        }

        val fresh = runBlocking(Dispatchers.IO) {
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

            val animMap = animEntries.associate { it.first.id to it }

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
                .filter { (!it.isCompleted && !WidgetCheckoffAnimator.isRecentlyCompleted(it.id)) || animMap.containsKey(it.id) }
                .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))

            val items = resolvedTasks.map { task ->
                val activeAnim = animMap[task.id]
                if (activeAnim != null) {
                    activeAnim.first
                } else {
                    val plainNotes = RichTextHelper.parseToRichSpans(task.description).first
                        .lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotEmpty() }
                    val timeLabel = if (task.taskTimeHour != null && task.taskTimeMinute != null) {
                        val h = if (task.taskTimeHour % 12 == 0) 12 else task.taskTimeHour % 12
                        val ap = if (task.taskTimeHour < 12) "AM" else "PM"
                        String.format(Locale.US, "%d:%02d %s", h, task.taskTimeMinute, ap)
                    } else {
                        null
                    }
                    WidgetTaskItem(
                        id = task.id,
                        title = task.title,
                        notesPreview = plainNotes,
                        timeLabel = timeLabel,
                        repeatMode = task.repeatMode,
                        dateMillis = dateMillis
                    )
                }
            }.toMutableList()

            // Ensure any animating task whose DB row was already filtered out stays in place until animation ends
            val existingIds = items.map { it.id }.toHashSet()
            animEntries.sortedBy { it.second }.forEach { (animItem, pos) ->
                if (!existingIds.contains(animItem.id) && animItem.title.isNotEmpty()) {
                    val insertIdx = pos.coerceIn(0, items.size)
                    items.add(insertIdx, animItem)
                }
            }

            WidgetSnapshot(
                dateLabel = today.format(DATE_FORMATTER),
                tasks = items,
                palette = WidgetPaletteResolver.resolve(context)
            )
        }

        lastSnapshot = fresh
        return fresh
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

    fun renderAnimationFrame(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(app, TaskWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val snapshot = WidgetTaskLoader.loadSnapshot(app, allowCachedForAnimation = true)
        if (usesDirectCollections(Build.VERSION.SDK_INT)) {
            for (id in ids) {
                val partial = RemoteViews(app.packageName, R.layout.acta_widget)
                bindDirectCollection(app, partial, id, snapshot)
                manager.partiallyUpdateAppWidget(id, partial)
            }
        } else {
            @Suppress("DEPRECATION")
            manager.notifyAppWidgetViewDataChanged(ids, R.id.acta_widget_list)
        }
    }

    fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val snapshot = WidgetTaskLoader.loadSnapshot(context, allowCachedForAnimation = false)
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

        val visibleCount = snapshot.tasks.count { it.animPhase == null }
        views.setTextViewText(R.id.acta_widget_title, snapshot.dateLabel)
        if (visibleCount > 0) {
            views.setTextViewText(R.id.acta_widget_subtitle, visibleCount.toString())
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
                .setViewTypeCount(1)
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
