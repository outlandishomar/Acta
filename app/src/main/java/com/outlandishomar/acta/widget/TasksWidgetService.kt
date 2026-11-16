package com.outlandishomar.acta.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.outlandishomar.acta.R

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

    private var snapshot: WidgetSnapshot = initialSnapshot ?: WidgetTaskLoader.loadSnapshot(context)

    override fun onCreate() {
        // Initial snapshot already loaded
    }

    override fun onDataSetChanged() {
        snapshot = WidgetTaskLoader.loadSnapshot(context)
    }

    override fun onDestroy() {}

    override fun getCount(): Int = snapshot.tasks.size

    override fun getViewAt(position: Int): RemoteViews = getViewAt(position, snapshot.tasks.size)

    internal fun getViewAt(position: Int, renderedCount: Int): RemoteViews {
        val tasks = snapshot.tasks
        if (position !in tasks.indices) {
            return RemoteViews(context.packageName, R.layout.acta_widget_item)
        }
        val item = tasks[position]
        val palette = snapshot.palette
        val isLast = position == renderedCount - 1

        return RemoteViews(context.packageName, R.layout.acta_widget_item).apply {
            setTextViewText(R.id.acta_widget_item_title, item.title)
            setTextColor(R.id.acta_widget_item_title, palette.text)

            setImageViewResource(R.id.acta_widget_item_priority, R.drawable.acta_widget_ring)
            setInt(R.id.acta_widget_item_priority, "setColorFilter", palette.mutedText)

            // Checkoff click target
            setOnClickFillInIntent(
                R.id.acta_widget_item_ring_target,
                Intent().setData(Uri.parse(WidgetTapActivity.checkoffUri(item.id, item.repeatMode, item.dateMillis)))
            )

            // Optional 1-line notes preview
            val notes = item.notesPreview
            if (!notes.isNullOrBlank()) {
                setViewVisibility(R.id.acta_widget_item_context_row, View.VISIBLE)
                setTextViewText(R.id.acta_widget_item_context, notes)
                setTextColor(R.id.acta_widget_item_context, palette.mutedText)
            } else {
                setViewVisibility(R.id.acta_widget_item_context_row, View.GONE)
            }

            // Optional scheduled time
            val timeLabel = item.timeLabel
            if (!timeLabel.isNullOrBlank()) {
                setViewVisibility(R.id.acta_widget_item_due, View.VISIBLE)
                setTextViewText(R.id.acta_widget_item_due, timeLabel)
                setTextColor(R.id.acta_widget_item_due, palette.mutedText)
            } else {
                setViewVisibility(R.id.acta_widget_item_due, View.GONE)
            }

            // Divider
            setInt(R.id.acta_widget_item_divider, "setBackgroundColor", palette.border)
            setViewVisibility(R.id.acta_widget_item_divider, if (isLast) View.GONE else View.VISIBLE)

            // Row click opens task in app
            setOnClickFillInIntent(
                R.id.acta_widget_item,
                Intent().setData(Uri.parse(WidgetTapActivity.openTaskUri(item.id)))
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        snapshot.tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
