package com.outlandishomar.acta.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.outlandishomar.acta.R
import com.outlandishomar.acta.ui.components.TaskIcons

class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = TasksWidgetFactory(
        applicationContext,
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    )
}

class TasksWidgetFactory(
    private val context: Context,
    private val appWidgetId: Int,
    initialSnapshot: WidgetSnapshot? = null
) : RemoteViewsService.RemoteViewsFactory {

    sealed class WidgetRow {
        data class MainTask(val item: WidgetTaskItem) : WidgetRow()
        data class SubTask(
            val subTask: WidgetSubTaskItem,
            val parentTask: WidgetTaskItem,
            val isLastSubTask: Boolean
        ) : WidgetRow()
    }

    private var snapshot: WidgetSnapshot = initialSnapshot ?: WidgetTaskLoader.loadSnapshot(context)
    private var rows: List<WidgetRow> = buildRows(snapshot)

    private fun buildRows(snap: WidgetSnapshot): List<WidgetRow> {
        val list = ArrayList<WidgetRow>()
        for (task in snap.tasks) {
            list.add(WidgetRow.MainTask(task))
            val subs = task.subTasks
            subs.forEachIndexed { idx, sub ->
                list.add(
                    WidgetRow.SubTask(
                        subTask = sub,
                        parentTask = task,
                        isLastSubTask = idx == subs.lastIndex
                    )
                )
            }
        }
        return list
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        snapshot = WidgetTaskLoader.loadSnapshot(context)
        rows = buildRows(snapshot)
    }

    override fun onDestroy() {}

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews = getViewAt(position, rows.size)

    internal fun getViewAt(position: Int, renderedCount: Int): RemoteViews {
        if (position !in rows.indices) {
            return RemoteViews(context.packageName, R.layout.acta_widget_item)
        }
        val row = rows[position]
        val isLastRenderedRow = position == renderedCount - 1
        return when (row) {
            is WidgetRow.MainTask -> bindMainTaskRow(row.item, isLastRenderedRow)
            is WidgetRow.SubTask -> bindSubTaskRow(row, isLastRenderedRow)
        }
    }

    private fun bindMainTaskRow(item: WidgetTaskItem, isLastRenderedRow: Boolean): RemoteViews {
        val palette = snapshot.palette
        val hasSubTasks = item.subTasks.isNotEmpty()
        val hasNotes = !item.notesPreview.isNullOrBlank() || item.showNotesIconOnly

        return RemoteViews(context.packageName, R.layout.acta_widget_item).apply {
            setTextViewText(R.id.acta_widget_item_title, item.title)
            setTextColor(R.id.acta_widget_item_title, palette.text)

            // Optional task icon between checkbox and title
            val iconOption = TaskIcons.getOptionByName(item.iconName)
            when {
                iconOption?.drawableRes != null -> {
                    setViewVisibility(R.id.acta_widget_item_task_icon, View.VISIBLE)
                    setImageViewResource(R.id.acta_widget_item_task_icon, iconOption.drawableRes)
                    setInt(R.id.acta_widget_item_task_icon, "setColorFilter", palette.text)
                }
                iconOption?.icon != null -> {
                    val density = context.resources.displayMetrics.density
                    val sizePx = (16f * density).toInt().coerceAtLeast(24)
                    val bmp = TaskIcons.renderImageVectorToBitmap(iconOption.icon, sizePx, palette.text)
                    setViewVisibility(R.id.acta_widget_item_task_icon, View.VISIBLE)
                    setImageViewBitmap(R.id.acta_widget_item_task_icon, bmp)
                    setInt(R.id.acta_widget_item_task_icon, "setColorFilter", 0)
                }
                else -> {
                    setViewVisibility(R.id.acta_widget_item_task_icon, View.GONE)
                }
            }

            // Square checkbox
            setImageViewResource(R.id.acta_widget_item_checkbox_box, R.drawable.acta_widget_checkbox_unchecked)
            setInt(R.id.acta_widget_item_checkbox_box, "setColorFilter", palette.checkboxBorder)
            setViewVisibility(R.id.acta_widget_item_checkbox_tick, View.GONE)

            setOnClickFillInIntent(
                R.id.acta_widget_item_ring_target,
                Intent().setData(Uri.parse(WidgetTapActivity.checkoffUri(item.id, item.repeatMode, item.dateMillis)))
            )

            // Notes row (either 1-line text OR notes icon if > 1 line)
            if (hasNotes) {
                setViewVisibility(R.id.acta_widget_item_context_row, View.VISIBLE)
                if (item.showNotesIconOnly) {
                    setViewVisibility(R.id.acta_widget_item_notes_icon, View.VISIBLE)
                    setInt(R.id.acta_widget_item_notes_icon, "setColorFilter", palette.faintNotesText)
                    setViewVisibility(R.id.acta_widget_item_context, View.GONE)
                } else {
                    setViewVisibility(R.id.acta_widget_item_notes_icon, View.GONE)
                    setViewVisibility(R.id.acta_widget_item_context, View.VISIBLE)
                    setTextViewText(R.id.acta_widget_item_context, item.notesPreview)
                    setTextColor(R.id.acta_widget_item_context, palette.faintNotesText)
                }
                if (hasSubTasks) {
                    setViewVisibility(R.id.acta_widget_item_notes_stem, View.VISIBLE)
                    setInt(R.id.acta_widget_item_notes_stem, "setBackgroundColor", palette.checkboxBorder)
                } else {
                    setViewVisibility(R.id.acta_widget_item_notes_stem, View.GONE)
                }
            } else {
                setViewVisibility(R.id.acta_widget_item_context_row, View.GONE)
            }

            // Bottom stem connecting to subtasks
            if (hasSubTasks) {
                setViewVisibility(R.id.acta_widget_item_bottom_stem, View.VISIBLE)
                setInt(R.id.acta_widget_item_bottom_stem, "setBackgroundColor", palette.checkboxBorder)
                setViewVisibility(R.id.acta_widget_item_divider, View.GONE)
            } else {
                setViewVisibility(R.id.acta_widget_item_bottom_stem, View.GONE)
                setInt(R.id.acta_widget_item_divider, "setBackgroundColor", palette.border)
                setViewVisibility(R.id.acta_widget_item_divider, if (isLastRenderedRow) View.GONE else View.VISIBLE)
            }

            // Scheduled time + notification bell next to title, and repeat icon at the end
            val timeLabel = item.timeLabel
            if (!timeLabel.isNullOrBlank()) {
                setViewVisibility(R.id.acta_widget_item_due, View.VISIBLE)
                setTextViewText(R.id.acta_widget_item_due, timeLabel)
                setTextColor(R.id.acta_widget_item_due, palette.faintNotesText)
                if (item.hasAlert) {
                    setViewVisibility(R.id.acta_widget_item_bell, View.VISIBLE)
                    setInt(R.id.acta_widget_item_bell, "setColorFilter", palette.faintNotesText)
                } else {
                    setViewVisibility(R.id.acta_widget_item_bell, View.GONE)
                }
            } else {
                setViewVisibility(R.id.acta_widget_item_due, View.GONE)
                setViewVisibility(R.id.acta_widget_item_bell, View.GONE)
            }

            if (item.repeatMode != "ONCE") {
                setViewVisibility(R.id.acta_widget_item_repeat, View.VISIBLE)
                setInt(R.id.acta_widget_item_repeat, "setColorFilter", palette.faintNotesText)
            } else {
                setViewVisibility(R.id.acta_widget_item_repeat, View.GONE)
            }

            // Row click opens TaskPeekActivity popup
            setOnClickFillInIntent(
                R.id.acta_widget_item,
                Intent().setData(Uri.parse(WidgetTapActivity.openTaskUri(item.id, item.dateMillis)))
            )
        }
    }

    private fun bindSubTaskRow(row: WidgetRow.SubTask, isLastRenderedRow: Boolean): RemoteViews {
        val palette = snapshot.palette
        val sub = row.subTask
        val parent = row.parentTask

        return RemoteViews(context.packageName, R.layout.acta_widget_subtask_item).apply {
            setImageViewResource(
                R.id.acta_widget_subtask_connector,
                if (row.isLastSubTask) R.drawable.acta_widget_tree_last else R.drawable.acta_widget_tree_middle
            )
            setInt(R.id.acta_widget_subtask_connector, "setColorFilter", palette.checkboxBorder)

            if (sub.isCompleted) {
                setImageViewResource(R.id.acta_widget_subtask_checkbox_box, R.drawable.acta_widget_subcheckbox_filled)
                setInt(R.id.acta_widget_subtask_checkbox_box, "setColorFilter", palette.accent)
                setViewVisibility(R.id.acta_widget_subtask_checkbox_tick, View.VISIBLE)
                setInt(R.id.acta_widget_subtask_checkbox_tick, "setColorFilter", palette.onAccent)

                val struck = SpannableString(sub.title).apply {
                    if (isNotEmpty()) {
                        setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                setTextViewText(R.id.acta_widget_subtask_title, struck)
                setTextColor(R.id.acta_widget_subtask_title, palette.disabledText)
            } else {
                setImageViewResource(R.id.acta_widget_subtask_checkbox_box, R.drawable.acta_widget_subcheckbox_unchecked)
                setInt(R.id.acta_widget_subtask_checkbox_box, "setColorFilter", palette.checkboxBorder)
                setViewVisibility(R.id.acta_widget_subtask_checkbox_tick, View.GONE)

                setTextViewText(R.id.acta_widget_subtask_title, sub.title)
                setTextColor(R.id.acta_widget_subtask_title, palette.text)
            }

            // Toggle subtask checkbox
            setOnClickFillInIntent(
                R.id.acta_widget_subtask_checkbox_target,
                Intent().setData(
                    Uri.parse(
                        WidgetTapActivity.subtaskCheckoffUri(
                            subTaskId = sub.id,
                            repeatMode = parent.repeatMode,
                            dateMillis = parent.dateMillis,
                            newCompleted = !sub.isCompleted
                        )
                    )
                )
            )

            // Divider after the last subtask of the group
            val showDivider = row.isLastSubTask && !isLastRenderedRow
            setInt(R.id.acta_widget_subtask_divider, "setBackgroundColor", palette.border)
            setViewVisibility(R.id.acta_widget_subtask_divider, if (showDivider) View.VISIBLE else View.GONE)

            // Row click opens TaskPeekActivity popup
            setOnClickFillInIntent(
                R.id.acta_widget_subtask_item,
                Intent().setData(Uri.parse(WidgetTapActivity.openTaskUri(parent.id, parent.dateMillis)))
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = when (val row = rows.getOrNull(position)) {
        is WidgetRow.MainTask -> row.item.id.toLong()
        is WidgetRow.SubTask -> (1L shl 32) or (row.subTask.id.toLong() and 0xFFFFFFFFL)
        null -> position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
