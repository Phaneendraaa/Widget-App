package com.phani.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Store.unbind(context, it) }
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
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))
            ids.forEach { render(context, manager, it) }
        }

        fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val cd = Store.bound(context, id)?.let { Store.get(context, it) }
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            if (cd == null) {
                val st = Styles.get(0)
                views.setInt(R.id.widget_root, "setBackgroundResource", st.bgRes)
                views.setTextViewText(R.id.widget_title, "COUNTDOWN")
                views.setTextViewText(R.id.widget_big, "?")
                views.setTextViewText(R.id.widget_caption, "Tap to set up")
                views.setTextViewText(R.id.widget_date, "")
                views.setTextColor(R.id.widget_title, st.sub)
                views.setTextColor(R.id.widget_big, st.accent)
                views.setTextColor(R.id.widget_caption, st.text)
                views.setTextColor(R.id.widget_date, st.sub)
            } else {
                val st = Styles.get(cd.style)
                val shown = cd.shown()
                views.setInt(R.id.widget_root, "setBackgroundResource", st.bgRes)
                views.setTextViewText(R.id.widget_title, cd.title.uppercase())
                views.setTextViewText(R.id.widget_big, shown.big)
                views.setTextViewText(R.id.widget_caption, shown.caption)
                views.setTextViewText(R.id.widget_date, cd.dateLabel())
                views.setTextColor(R.id.widget_title, st.sub)
                views.setTextColor(R.id.widget_big, st.accent)
                views.setTextColor(R.id.widget_caption, st.text)
                views.setTextColor(R.id.widget_date, st.sub)
            }

            val open = PendingIntent.getActivity(
                context, id, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(id, views)
        }
    }
}
