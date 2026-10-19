package com.phani.widgets.widget

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.cnt
import com.phani.widgets.core.fastState
import com.phani.widgets.core.streak
import com.phani.widgets.core.today
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Tall (2×3) designs: things that read naturally top to bottom — a glass filling up, a fast rising, a month of check-ins. */
object TallDesigns {
    private val D get() = AppStore.view
    private val C = Paint.Align.CENTER
    private val R = Paint.Align.RIGHT
    private fun a(alpha: Float, c: Int) = ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)

    fun draw(x: Ctx): Boolean {
        when ("${x.conf.type}/${x.conf.design}") {
            "counter/bottle" -> bottle(x)
            "fast/tube" -> tube(x)
            "streak/month" -> month(x)
            else -> return false
        }
        return true
    }

    private fun Ctx.none(lbl: String, msg: String) { label(ttl(lbl), pad, pad + 11.5f); small(msg, pad, H - pad - 2f, maxW = W - 2 * pad) }

    /* ---------- a tall glass of water ---------- */

    private fun bottle(x: Ctx) = with(x) {
        val ct = D.counters.firstOrNull { it.id == conf.ref } ?: D.counters.firstOrNull() ?: run { none("Counter", "Add a counter"); return@with }
        val v = cnt(ct)
        val pct = (v.toFloat() / max(1, ct.goal)).coerceIn(0f, 1f)
        label(ttl(ct.name), pad, pad + 11.5f, W - 2 * pad - 40f)
        text("$v/${ct.goal}", W - pad, pad + 12f, 13f, uiFace(600), if (v >= ct.goal) p.acc else p.fg, align = R)
        val bh = 34f; val btnY = H - pad - bh
        val gTop = pad + 26f; val gBot = btnY - 12f; val gh = gBot - gTop
        val gw = min(W - 2 * pad - 24f, gh * 0.52f)
        val gx = (W - gw) / 2
        val glass = Path().apply {
            moveTo(px(gx), px(gTop)); lineTo(px(gx + gw), px(gTop))
            lineTo(px(gx + gw * 0.9f), px(gBot - 6f)); quadTo(px(gx + gw * 0.88f), px(gBot), px(gx + gw * 0.8f), px(gBot))
            lineTo(px(gx + gw * 0.2f), px(gBot)); quadTo(px(gx + gw * 0.12f), px(gBot), px(gx + gw * 0.1f), px(gBot - 6f)); close()
        }
        c.save(); c.clipPath(glass)
        c.drawPath(glass, fill(a(0.06f, p.fg)))
        // marks for each glass of the goal
        val step = (gh - 4f) / max(1, ct.goal)
        for (i in 1 until ct.goal) line(gx + gw * 0.12f, gBot - step * i, gx + gw * 0.24f, gBot - step * i, stroke(a(0.25f, p.fg), 1f))
        if (pct > 0f) {
            val level = gBot - (gh - 3f) * pct
            fun wave(phase: Float, amp: Float, col: Int, shader: Shader?) {
                val path = Path(); path.moveTo(px(gx - 2f), px(gBot + 2f))
                var xx = gx - 2f
                while (xx <= gx + gw + 2f) { path.lineTo(px(xx), px(level + amp * sin(((xx - gx) / gw) * 2.4f * PI.toFloat() + phase))); xx += 1.5f }
                path.lineTo(px(gx + gw + 2f), px(gBot + 2f)); path.close()
                c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; this.shader = shader })
            }
            wave(1.6f, 2.4f, a(0.35f, p.acc), null)
            wave(0f, 2.6f, p.acc, LinearGradient(0f, px(level), 0f, px(gBot), a(0.8f, p.acc), p.acc, Shader.TileMode.CLAMP))
            listOf(0.3f to 0.8f, 0.62f to 0.6f, 0.45f to 0.9f, 0.7f to 0.85f, 0.38f to 0.7f).forEach { (bx, by) ->
                val yy = gBot - (gBot - level) * (1f - by)
                if (yy > level + 4f) circle(gx + gw * bx, yy, 1.7f, stroke(a(0.7f, 0xFFFFFFFF.toInt()), 0.8f))
            }
        }
        c.restore()
        c.drawPath(glass, stroke(a(0.55f, p.fg), 1.6f).apply { strokeJoin = Paint.Join.ROUND })
        line(gx + gw * 0.2f, gTop + 8f, gx + gw * 0.28f, gBot - 14f, stroke(a(0.35f, 0xFFFFFFFF.toInt()), 1.8f))
        val mw = 40f
        button(pad, btnY, mw, bh, "ghost", "cnt-", ct.id, ic = Paths.minus, icSize = 13f)
        button(pad + mw + 6f, btnY, W - 2 * pad - mw - 6f, bh, "solid", "cnt", ct.id, text = "+1")
    }

    /* ---------- a fast, rising up a tube ---------- */

    private fun tube(x: Ctx) = with(x) {
        val f = fastState()
        label(ttl("Fasting"), pad, pad + 11.5f, W - 2 * pad)
        val bh = 34f; val btnY = H - pad - bh
        val tTop = pad + 26f; val tBot = btnY - 12f
        val tw = 28f; val tx = pad + 2f
        rrect(tx, tTop, tw, tBot - tTop, tw / 2, fill(p.soft))
        if (f.on && f.pct > 0.0) {
            val fh = max(tw, (tBot - tTop) * f.pct.toFloat())
            val pnt = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, px(tBot - fh), 0f, px(tBot), a(0.8f, p.acc), p.acc, Shader.TileMode.CLAMP) }
            c.drawRoundRect(android.graphics.RectF(px(tx), px(tBot - fh), px(tx + tw), px(tBot)), px(tw / 2), px(tw / 2), pnt)
        }
        // hour marks at 25 / 50 / 75 %
        for (q in 1..3) { val yy = tBot - (tBot - tTop) * q / 4f; line(tx + tw + 4f, yy, tx + tw + 9f, yy, stroke(p.mut, 1.2f)) }
        val rx = tx + tw + 16f
        val tm = if (f.on) Fmt.hhmm(f.h) else "0:00"
        val sz = min(30f, (W - pad - rx) / max(1f, numW(tm, 30f) / 30f))
        num(tm, rx, tTop + cap(sz) + 2f, sz, p.fg)
        small("of ${f.target}h goal", rx, tTop + cap(sz) + 18f, size = 11f, maxW = W - pad - rx)
        if (f.on) {
            val start = f.endMs - f.target * 3_600_000L
            small(if (f.h >= f.target) "Goal reached" else "${Fmt.hhmm(((1 - f.pct) * f.target))} to go", rx, tTop + cap(sz) + 36f,
                if (f.h >= f.target) p.acc else p.fg, 11.5f, maxW = W - pad - rx, weight = 600)
            small("Ends " + Fmt.at(f.endMs), rx, tBot - 16f, size = 10.5f, maxW = W - pad - rx)
            small("Began " + Fmt.at(start), rx, tBot - 2f, size = 10.5f, maxW = W - pad - rx)
        } else small("Ready when you are", rx, tTop + cap(sz) + 36f, size = 11f, maxW = W - pad - rx)
        button(pad, btnY, W - 2 * pad, bh, if (f.on) "ghost" else "solid", "fast", text = if (f.on) "End" else "Start")
    }

    /* ---------- a month of check-ins ---------- */

    private fun month(x: Ctx) = with(x) {
        val h = D.habits.firstOrNull { it.id == conf.ref } ?: D.habits.firstOrNull() ?: run { none("Streak", "Add a habit"); return@with }
        val st = streak(h)
        label(ttl(h.name), pad, pad + 11.5f, W - 2 * pad - 40f)
        button(W - pad - 34f, pad - 6f, 34f, 34f, if (st.today) "ghost" else "acc", "hab", h.id, ic = Paths.check, icSize = 16f)
        val n = st.n.toString()
        num(n, pad, pad + 30f + cap(40f), 40f, p.fg)
        small(if (st.n == 1) "day streak" else "day streak", pad + numW(n, 40f) + 6f, pad + 30f + cap(40f), size = 12f, maxW = W - 2 * pad)
        // five weeks, Monday first, ending with this week
        val t0 = today()
        val weeks = 5
        val start = t0.minusDays((t0.dayOfWeek.value - 1).toLong()).minusWeeks((weeks - 1).toLong())
        val gTop = pad + 30f + cap(40f) + 22f
        val gap = 4f
        val cell = min((W - 2 * pad - 6 * gap) / 7, (H - pad - gTop - 14f - (weeks - 1) * gap) / weeks)
        val gx = pad + (W - 2 * pad - (7 * cell + 6 * gap)) / 2
        for (d in 0 until 7) {
            val nm = java.time.DayOfWeek.of(d + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault())
            text(nm, gx + d * (cell + gap) + cell / 2, gTop + 4f, 9.5f, uiFace(500), p.mut, align = C)
        }
        for (w in 0 until weeks) for (d in 0 until 7) {
            val day = start.plusDays((w * 7 + d).toLong())
            val cx = gx + d * (cell + gap); val cy = gTop + 12f + w * (cell + gap)
            val k = day.toString()
            when {
                day.isAfter(t0) -> rrect(cx + 0.75f, cy + 0.75f, cell - 1.5f, cell - 1.5f, cell * 0.3f, stroke(a(0.25f, p.fg), 1f))
                h.done.contains(k) -> rrect(cx, cy, cell, cell, cell * 0.3f, fill(if (day == t0) p.acc else p.fg))
                h.frozen.contains(k) -> rrect(cx + 0.75f, cy + 0.75f, cell - 1.5f, cell - 1.5f, cell * 0.3f, stroke(p.mut, 1.5f))
                else -> rrect(cx, cy, cell, cell, cell * 0.3f, fill(p.soft))
            }
        }
    }
}
