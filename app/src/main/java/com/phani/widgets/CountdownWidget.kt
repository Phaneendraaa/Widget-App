package com.phani.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Birthday countdown. Target date is hardcoded for the MVP: 25 December. */
class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> refreshAll(context)
        }
    }

    companion object {
        private const val MONTH = 12
        private const val DAY = 25

        fun daysLeft(today: LocalDate = LocalDate.now()): Long {
            var next = LocalDate.of(today.year, MONTH, DAY)
            if (next.isBefore(today)) next = next.plusYears(1)
            return ChronoUnit.DAYS.between(today, next)
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val days = daysLeft()
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            if (days == 0L) {
                views.setTextViewText(R.id.widget_days, "🎂")
                views.setTextViewText(R.id.widget_label, "Happy birthday!")
            } else {
                views.setTextViewText(R.id.widget_days, days.toString())
                views.setTextViewText(
                    R.id.widget_label,
                    if (days == 1L) "day to birthday" else "days to birthday"
                )
            }
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(id, views)
        }
    }
}
