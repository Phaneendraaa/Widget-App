package com.phani.widgets.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.phani.widgets.MainActivity
import com.phani.widgets.R
import com.phani.widgets.WidgetConfigActivity
import com.phani.widgets.core.Actions
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.WConf
import com.phani.widgets.core.uid
import com.phani.widgets.core.toJson
import com.phani.widgets.CountdownWidget
import kotlin.math.max

class WideWidget : BaseWidget() { override val size = "w" }
class LargeWidget : BaseWidget() { override val size = "m" }

const val ACT_TAP = "com.phani.widgets.TAP"
const val ACT_PINNED = "com.phani.widgets.PINNED"
const val ACT_TICK = "com.phani.widgets.TICK"

/** Grid of invisible tap cells laid over the widget image. */
private val CELLS = intArrayOf(
    R.id.c00, R.id.c01, R.id.c02, R.id.c03, R.id.c04, R.id.c05,
    R.id.c10, R.id.c11, R.id.c12, R.id.c13, R.id.c14, R.id.c15,
    R.id.c20, R.id.c21, R.id.c22, R.id.c23, R.id.c24, R.id.c25,
    R.id.c30, R.id.c31, R.id.c32, R.id.c33, R.id.c34, R.id.c35,
)
private const val COLS = 6
private const val ROWS = 4

abstract class BaseWidget : AppWidgetProvider() {
    abstract val size: String

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Widgets.prepare(context)
        ids.forEach { Widgets.render(context, manager, it, size) }
        Widgets.scheduleTick(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        Widgets.prepare(context)
        Widgets.render(context, manager, id, size)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        Widgets.prepare(context)
        AppStore.change { ids.forEach { bound.remove(it) } }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACT_TAP -> {
                Widgets.prepare(context)
                val act = intent.getStringExtra("act") ?: return
                if (act == "open" || act == "spend-open") {
                    context.startActivity(Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra("open", act).putExtra("cid", intent.getStringExtra("cid")))
                    return
                }
                Actions.run(act, intent.getStringExtra("id"), intent.getStringExtra("value"))
                Widgets.updateAll(context)
            }
            ACT_PINNED -> {
                Widgets.prepare(context)
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                val pending = Widgets.takePending(context)
                if (id != -1 && pending != null) AppStore.change { bound[id] = pending }
                Widgets.updateAll(context)
            }
            ACT_TICK, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                Widgets.prepare(context)
                Widgets.updateAll(context)
            }
        }
    }
}

object Widgets {
    private val providers = listOf(CountdownWidget::class.java to "s", WideWidget::class.java to "w", LargeWidget::class.java to "m")

    fun prepare(ctx: Context) {
        AppStore.load(ctx)
        WFonts.init(ctx.assets)
        if (AppStore.onChanged == null) {
            val app = ctx.applicationContext
            AppStore.onChanged = { updateAll(app) }
        }
    }

    fun providerFor(size: String): Class<*> = providers.first { it.second == size }.first

    fun updateAll(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        providers.forEach { (cls, size) ->
            m.getAppWidgetIds(ComponentName(ctx, cls)).forEach { render(ctx, m, it, size) }
        }
    }

    /** Widget ids that really exist on the home screen right now. */
    fun liveIds(ctx: Context): Set<Int> {
        val m = AppWidgetManager.getInstance(ctx)
        return providers.flatMap { m.getAppWidgetIds(ComponentName(ctx, it.first)).toList() }.toSet()
    }

    /** Forget home-screen widgets the user removed while the app wasn't listening. */
    fun prune(ctx: Context) {
        val live = liveIds(ctx)
        if (AppStore.data.bound.keys.any { it !in live }) AppStore.change { bound.keys.retainAll(live) }
    }

    /** True when this saved widget is on the home screen. */
    fun isOnPhone(id: String): Boolean = AppStore.data.bound.values.any { it.id == id }

    /**
     * The look a home-screen widget shows: the latest version of the widget saved in the app,
     * so edits in the app show up on the phone straight away.
     */
    fun confFor(id: Int, size: String): WConf? {
        val b = AppStore.data.bound[id] ?: return null
        val latest = AppStore.data.placed.firstOrNull { it.id == b.id } ?: return b
        return if (latest.size == size) latest else latest.copy(design = b.design, size = size)
    }

    fun placedCount(ctx: Context): Int {
        val m = AppWidgetManager.getInstance(ctx)
        return providers.sumOf { m.getAppWidgetIds(ComponentName(ctx, it.first)).size }
    }

    /** Size in dp the launcher gives this widget (portrait: min width × max height). */
    private fun sizeDp(m: AppWidgetManager, id: Int, size: String): Pair<Float, Float> {
        val o = m.getAppWidgetOptions(id)
        var w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
        var h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
        if (w <= 0f) w = if (size == "s") 160f else 330f
        if (h <= 0f) h = when (size) { "s" -> w; "w" -> 84f; else -> 184f }
        return w to h
    }

    fun render(ctx: Context, m: AppWidgetManager, id: Int, size: String) {
        val conf = confFor(id, size)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_host)
        val density = ctx.resources.displayMetrics.density
        val (wDp, hDp) = sizeDp(m, id, size)
        if (conf == null) {
            // Not set up yet: show a quiet placeholder that opens the picker.
            val ph = WConf("setup", "now", "card", size, "paper")
            val r = renderPlaceholder(ph, (wDp * density).toInt(), (hDp * density).toInt(), density)
            rv.setImageViewBitmap(R.id.img, r)
            val cfg = Intent(ctx, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(ctx, id * 100, cfg, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            CELLS.forEach { rv.setOnClickPendingIntent(it, pi) }
            m.updateAppWidget(id, rv)
            return
        }
        val r = renderWidget(conf.copy(size = size), (wDp * density).toInt(), (hDp * density).toInt(), density)
        rv.setImageViewBitmap(R.id.img, r.bitmap)
        rv.setContentDescription(R.id.img, conf.type)
        val cw = r.bitmap.width / COLS.toFloat()
        val ch = r.bitmap.height / ROWS.toFloat()
        val assigned = arrayOfNulls<Hit>(CELLS.size)
        // each cell takes the hit under its centre
        for (i in CELLS.indices) {
            val cx = (i % COLS + 0.5f) * cw; val cy = (i / COLS + 0.5f) * ch
            assigned[i] = r.hits.lastOrNull { it.rect.contains(cx, cy) }
        }
        // every hit gets at least the cell under its own centre
        r.hits.forEach { h ->
            val col = (h.rect.centerX() / cw).toInt().coerceIn(0, COLS - 1)
            val row = (h.rect.centerY() / ch).toInt().coerceIn(0, ROWS - 1)
            val i = row * COLS + col
            if (assigned[i] == null) assigned[i] = h
        }
        for (i in CELLS.indices) {
            val h = assigned[i]
            val intent = Intent(ctx, providerFor(size)).setAction(ACT_TAP)
                .putExtra("act", h?.act ?: "open").putExtra("id", h?.id).putExtra("value", h?.value).putExtra("cid", conf.id)
            val pi = PendingIntent.getBroadcast(ctx, id * 100 + i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(CELLS[i], pi)
        }
        m.updateAppWidget(id, rv)
    }

    private fun renderPlaceholder(conf: WConf, w: Int, h: Int, density: Float): android.graphics.Bitmap {
        val bmp = android.graphics.Bitmap.createBitmap(max(1, w), max(1, h), android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val pal = widgetPal(conf, 0xFF1C1C1A.toInt(), 0xFFD7D3CA.toInt())
        val x = Ctx(c, w.toFloat(), h.toFloat(), density, pal, conf)
        x.rrect(0f, 0f, x.W, x.H, 26f, x.fill(pal.bg))
        x.label("Widget App", x.pad, x.pad + 11.5f)
        x.text("Tap to set up", x.pad, x.H - x.pad - 20f, 17f, x.uiFace(600), pal.fg)
        x.small("Pick one of your widgets", x.pad, x.H - x.pad - 2f)
        return bmp
    }

    /* ---- pinning from the app ---- */

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    /** Ask the launcher to place [conf]. Returns false if the launcher can't. */
    fun requestPin(ctx: Context, conf: WConf): Boolean {
        val m = AppWidgetManager.getInstance(ctx)
        if (!m.isRequestPinAppWidgetSupported) return false
        prefs(ctx).edit().putString("pending", conf.toJson().toString())
            .putLong("pendingAt", System.currentTimeMillis()).apply()
        val cb = Intent(ctx, providerFor(conf.size)).setAction(ACT_PINNED)
        val pi = PendingIntent.getBroadcast(ctx, 7, cb, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        return try { m.requestPinAppWidget(ComponentName(ctx, providerFor(conf.size)), null, pi) } catch (e: Exception) { false }
    }

    fun takePending(ctx: Context): WConf? {
        val p = prefs(ctx)
        val s = p.getString("pending", null) ?: return null
        val at = p.getLong("pendingAt", 0)
        p.edit().remove("pending").apply()
        if (System.currentTimeMillis() - at > 5 * 60_000) return null
        return try { com.phani.widgets.core.wconf(org.json.JSONObject(s)) } catch (e: Exception) { null }
    }

    /** Light refresh every 15 minutes for timers, clocks and schedules. Inexact, so it costs almost no battery. */
    fun scheduleTick(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, 9, Intent(ctx, CountdownWidget::class.java).setAction(ACT_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi)
    }
}
