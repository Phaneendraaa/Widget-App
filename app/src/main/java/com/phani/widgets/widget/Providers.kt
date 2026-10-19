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
const val ACT_MINUTE = "com.phani.widgets.MINUTE"

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
                val r = Actions.run(act, intent.getStringExtra("id"), intent.getStringExtra("value"), intent.getStringExtra("cid"))
                val n = intent.getIntExtra("notif", -1)
                if (n != -1) context.getSystemService(android.app.NotificationManager::class.java).cancel(n)
                Widgets.updateAll(context)
                // take the Undo button away again once its time is up
                if (r.undo && r.msg != null) Widgets.refreshSoon(context, AppStore.UNDO_MS + 500)
            }
            ACT_PINNED -> {
                Widgets.prepare(context)
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                val pending = Widgets.takePending(context)
                if (id != -1 && pending != null) AppStore.change { bound[id] = pending }
                Widgets.updateAll(context)
            }
            ACT_TICK, ACT_MINUTE, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Widgets.prepare(context)
                Widgets.updateAll(context)
                Widgets.scheduleTick(context)
                // weather, air and rates: fetch what's out of date, then redraw
                if (com.phani.widgets.core.Live.wanted(AppStore.data)) {
                    val pr = goAsync()
                    com.phani.widgets.core.Live.refresh(context) { pr.finish() }
                }
            }
        }
    }
}

object Widgets {
    private val providers = listOf(CountdownWidget::class.java to "s", WideWidget::class.java to "w", LargeWidget::class.java to "m")

    fun prepare(ctx: Context) {
        AppStore.load(ctx)
        Photos.init(ctx)
        Dyn.load(ctx)
        WFonts.init(ctx.assets)
        if (AppStore.onChanged == null) {
            val app = ctx.applicationContext
            AppStore.onChanged = { updateAll(app); scheduleTick(app) }
        }
    }

    fun providerFor(size: String): Class<*> = providers.first { it.second == size }.first

    fun updateAll(ctx: Context) {
        Dyn.load(ctx)
        val m = AppWidgetManager.getInstance(ctx)
        providers.forEach { (cls, size) ->
            m.getAppWidgetIds(ComponentName(ctx, cls)).forEach { render(ctx, m, it, size) }
        }
    }

    /** Redraw every widget after [ms], to clear an Undo button. */
    fun refreshSoon(ctx: Context, ms: Long) {
        val app = ctx.applicationContext
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ updateAll(app) }, ms)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(app, 11, Intent(app, CountdownWidget::class.java).setAction(ACT_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + ms, pi)
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

    /**
     * The exact sizes the home screen shows this widget at (Android 12+ reports them, one per orientation).
     * Drawing at each exact size keeps circles round and squares square; the min/max guess can be the wrong shape.
     */
    private fun exactSizes(m: AppWidgetManager, id: Int): List<Pair<Float, Float>> {
        if (android.os.Build.VERSION.SDK_INT < 31) return emptyList()
        val o = m.getAppWidgetOptions(id)
        @Suppress("DEPRECATION")
        val l: List<android.util.SizeF> = (if (android.os.Build.VERSION.SDK_INT >= 33) o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, android.util.SizeF::class.java)
            else o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)) ?: return emptyList()
        return l.filter { it.width > 20f && it.height > 20f }.map { it.width to it.height }
            .distinctBy { (it.first.toInt() / 4) to (it.second.toInt() / 4) }.take(3)
    }

    fun render(ctx: Context, m: AppWidgetManager, id: Int, size: String) {
        val conf = confFor(id, size)
        val density = ctx.resources.displayMetrics.density
        val exact = exactSizes(m, id)
        if (exact.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= 31) {
            // one drawing per real size; the launcher shows the one that matches, so nothing is stretched
            val map = exact.withIndex().associate { (n, wh) -> android.util.SizeF(wh.first, wh.second) to views(ctx, conf, id, size, wh.first, wh.second, density, n) }
            m.updateAppWidget(id, RemoteViews(map))
        } else {
            val (wDp, hDp) = sizeDp(m, id, size)
            m.updateAppWidget(id, views(ctx, conf, id, size, wDp, hDp, density, 0))
        }
    }

    /** [slot] keeps each size's tap targets separate, so a tap acts on what's drawn at that size. */
    private fun views(ctx: Context, conf: WConf?, id: Int, size: String, wDp: Float, hDp: Float, density: Float, slot: Int): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_host)
        if (conf == null) {
            // Not set up yet: show a quiet placeholder that opens the picker.
            val ph = WConf("setup", "now", "card", size, "paper")
            val r = renderPlaceholder(ph, (wDp * density).toInt(), (hDp * density).toInt(), density)
            rv.setImageViewBitmap(R.id.img, r)
            val cfg = Intent(ctx, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(ctx, id * 1000 + slot * 100 + 99, cfg, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            CELLS.forEach { rv.setOnClickPendingIntent(it, pi) }
            return rv
        }
        val k = density * widgetScale(size, wDp)
        val r = renderWidget(conf.copy(size = size), (wDp * density).toInt(), (hDp * density).toInt(), k)
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
            val quick = com.phani.widgets.QuickActivity.kindFor(conf.type)
            val act = h?.act ?: if (quick != null) "quick" else "open"
            val qk = when (act) { "quick" -> quick; "spend-open" -> "spend"; "fast" -> "fast"; else -> null }
            val pi = if (qk != null) {
                // a deliberate change (any amount, start earlier, confirm end) happens in the quick panel
                PendingIntent.getActivity(ctx, id * 1000 + slot * 100 + i, com.phani.widgets.QuickActivity.intent(ctx, qk, conf.ref, conf.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else if (act == "open") {
                val open = Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .setData(android.net.Uri.parse("widget://$id/$slot/$i")) // keeps each cell's intent distinct
                    .putExtra("open", act).putExtra("cid", conf.id)
                PendingIntent.getActivity(ctx, id * 1000 + slot * 100 + i, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else {
                val intent = Intent(ctx, providerFor(size)).setAction(ACT_TAP)
                    .putExtra("act", act).putExtra("id", h?.id).putExtra("value", h?.value).putExtra("cid", conf.id)
                PendingIntent.getBroadcast(ctx, id * 1000 + slot * 100 + i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            rv.setOnClickPendingIntent(CELLS[i], pi)
        }
        return rv
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

    /**
     * Keeps widgets current without draining the battery. All alarms are non-waking: if the phone is asleep
     * they simply run the next time it's used, which is when the widgets are seen.
     *  - every 15 minutes, for anything that changes slowly
     *  - just after midnight, so counters reset and countdowns move on (Android no longer sends apps "date changed")
     *  - every minute while clocks, timers or schedules are on the home screen
     * Restarted after the phone reboots or the app updates.
     */
    fun scheduleTick(ctx: Context) {
        val app = ctx.applicationContext
        Reminders.schedule(app)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        fun pi(code: Int, action: String) = PendingIntent.getBroadcast(app, code, Intent(app, CountdownWidget::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi(9, ACT_TICK))
        val zone = java.time.ZoneId.systemDefault()
        val midnight = java.time.LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusSeconds(15).toInstant().toEpochMilli()
        am.set(AlarmManager.RTC, midnight, pi(12, ACT_TICK))
        val minute = pi(13, ACT_MINUTE)
        if (needsMinute()) {
            val now = System.currentTimeMillis()
            am.set(AlarmManager.RTC, now - now % 60_000 + 60_000 + 1_000, minute)
        } else am.cancel(minute)
    }

    /** Widgets on the home screen that show the time or a running timer. */
    private fun needsMinute(): Boolean {
        val d = AppStore.data
        return d.bound.values.any { it.type in setOf("clock", "next", "now") || (it.type == "fast" && d.fast.start != null) }
    }
}
