package com.outlandishomar.acta.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.outlandishomar.acta.R
import kotlin.math.ceil
import kotlin.math.roundToInt

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

    private var snapshot: WidgetSnapshot =
        initialSnapshot ?: WidgetTaskLoader.loadSnapshot(context, allowCachedForAnimation = false)

    override fun onCreate() {
        // Initial snapshot already loaded
    }

    override fun onDataSetChanged() {
        snapshot = WidgetTaskLoader.loadSnapshot(
            context,
            allowCachedForAnimation = WidgetCheckoffAnimator.hasActiveAnimations()
        )
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
        val phase = item.animPhase

        val density = context.resources.displayMetrics.density
        fun dpToPx(dp: Float): Int = (dp * density).roundToInt()

        // Visual progression matching Acta's in-app animation + shrink-and-fade disappearance
        val strikeFraction: Float
        val rowAlpha: Float
        val titleSp: Float
        val notesSp: Float
        val dueSp: Float
        val padH: Int
        val padV: Int
        val boxPad: Int
        val tickPad: Int
        val isCheckedVisual: Boolean

        when (phase) {
            null -> {
                isCheckedVisual = false
                strikeFraction = 0f
                rowAlpha = 1f
                titleSp = 14.5f
                notesSp = 12f
                dueSp = 12f
                padH = 0
                padV = dpToPx(6f)
                boxPad = 0
                tickPad = 0
            }
            0 -> {
                // Phase 0: Checkbox fills & checkmark snaps in, pencil strike begins
                isCheckedVisual = true
                strikeFraction = 0.40f
                rowAlpha = 0.88f
                titleSp = 14.3f
                notesSp = 11.8f
                dueSp = 11.8f
                padH = dpToPx(1f)
                padV = dpToPx(6f)
                boxPad = 0
                tickPad = 0
            }
            1 -> {
                // Phase 1: Pencil strike sweeps across text
                isCheckedVisual = true
                strikeFraction = 0.75f
                rowAlpha = 0.76f
                titleSp = 14.1f
                notesSp = 11.6f
                dueSp = 11.6f
                padH = dpToPx(2f)
                padV = dpToPx(5.5f)
                boxPad = 0
                tickPad = 0
            }
            2 -> {
                // Phase 2: Full strikethrough & settled dimmed state (scale ~0.97)
                isCheckedVisual = true
                strikeFraction = 1.0f
                rowAlpha = 0.65f
                titleSp = 13.9f
                notesSp = 11.4f
                dueSp = 11.4f
                padH = dpToPx(3f)
                padV = dpToPx(5f)
                boxPad = dpToPx(0.5f)
                tickPad = 0
            }
            3 -> {
                // Phase 3: Shrink inward + fade
                isCheckedVisual = true
                strikeFraction = 1.0f
                rowAlpha = 0.45f
                titleSp = 13.0f
                notesSp = 10.6f
                dueSp = 10.6f
                padH = dpToPx(12f)
                padV = dpToPx(4f)
                boxPad = dpToPx(2f)
                tickPad = dpToPx(1f)
            }
            4 -> {
                // Phase 4: Shrink inward + fade deeper
                isCheckedVisual = true
                strikeFraction = 1.0f
                rowAlpha = 0.22f
                titleSp = 11.8f
                notesSp = 9.6f
                dueSp = 9.6f
                padH = dpToPx(24f)
                padV = dpToPx(2f)
                boxPad = dpToPx(4f)
                tickPad = dpToPx(2.5f)
            }
            else -> {
                // Phase 5: Final fade just before vanishing
                isCheckedVisual = true
                strikeFraction = 1.0f
                rowAlpha = 0.06f
                titleSp = 10.5f
                notesSp = 8.5f
                dueSp = 8.5f
                padH = dpToPx(36f)
                padV = dpToPx(1f)
                boxPad = dpToPx(6f)
                tickPad = dpToPx(4f)
            }
        }

        val titleColor = if (isCheckedVisual) {
            withAlphaFactor(palette.disabledText, rowAlpha)
        } else {
            palette.text
        }
        val mutedColor = withAlphaFactor(palette.mutedText, rowAlpha)
        val dividerColor = withAlphaFactor(palette.border, rowAlpha)
        val checkboxBoxColor = withAlphaFactor(
            if (isCheckedVisual) palette.accent else palette.checkboxBorder,
            rowAlpha
        )
        val tickColor = withAlphaFactor(palette.onAccent, rowAlpha)

        return RemoteViews(context.packageName, R.layout.acta_widget_item).apply {
            setViewPadding(R.id.acta_widget_item_inner, padH, padV, padH, padV)

            // Title with progressive pencil strikethrough
            setTextViewText(R.id.acta_widget_item_title, struckText(item.title, strikeFraction))
            setTextColor(R.id.acta_widget_item_title, titleColor)
            setTextViewTextSize(R.id.acta_widget_item_title, TypedValue.COMPLEX_UNIT_SP, titleSp)

            // Rounded-square checkbox (filled + checkmark when checked, outlined square when unchecked)
            setImageViewResource(
                R.id.acta_widget_item_checkbox_box,
                if (isCheckedVisual) R.drawable.acta_widget_checkbox_filled
                else R.drawable.acta_widget_checkbox_unchecked
            )
            setInt(R.id.acta_widget_item_checkbox_box, "setColorFilter", checkboxBoxColor)
            setViewPadding(R.id.acta_widget_item_checkbox_box, boxPad, boxPad, boxPad, boxPad)

            if (isCheckedVisual) {
                setViewVisibility(R.id.acta_widget_item_checkbox_tick, View.VISIBLE)
                setInt(R.id.acta_widget_item_checkbox_tick, "setColorFilter", tickColor)
                setViewPadding(R.id.acta_widget_item_checkbox_tick, tickPad, tickPad, tickPad, tickPad)
            } else {
                setViewVisibility(R.id.acta_widget_item_checkbox_tick, View.GONE)
            }

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
                setTextColor(R.id.acta_widget_item_context, mutedColor)
                setTextViewTextSize(R.id.acta_widget_item_context, TypedValue.COMPLEX_UNIT_SP, notesSp)
            } else {
                setViewVisibility(R.id.acta_widget_item_context_row, View.GONE)
            }

            // Optional scheduled time
            val timeLabel = item.timeLabel
            if (!timeLabel.isNullOrBlank()) {
                setViewVisibility(R.id.acta_widget_item_due, View.VISIBLE)
                setTextViewText(R.id.acta_widget_item_due, timeLabel)
                setTextColor(R.id.acta_widget_item_due, mutedColor)
                setTextViewTextSize(R.id.acta_widget_item_due, TypedValue.COMPLEX_UNIT_SP, dueSp)
            } else {
                setViewVisibility(R.id.acta_widget_item_due, View.GONE)
            }

            // Divider
            setInt(R.id.acta_widget_item_divider, "setBackgroundColor", dividerColor)
            setViewVisibility(R.id.acta_widget_item_divider, if (isLast) View.GONE else View.VISIBLE)

            // Row click opens task in app
            setOnClickFillInIntent(
                R.id.acta_widget_item,
                Intent().setData(Uri.parse(WidgetTapActivity.openTaskUri(item.id)))
            )
        }
    }

    private fun struckText(title: String, fraction: Float): CharSequence {
        if (fraction <= 0f || title.isEmpty()) return title
        val endIndex = ceil(title.length * fraction.coerceIn(0f, 1f)).toInt().coerceIn(0, title.length)
        if (endIndex <= 0) return title
        return SpannableString(title).apply {
            setSpan(StrikethroughSpan(), 0, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun withAlphaFactor(color: Int, factor: Float): Int {
        val baseAlpha = Color.alpha(color)
        val newAlpha = (baseAlpha * factor).roundToInt().coerceIn(0, 255)
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        snapshot.tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
