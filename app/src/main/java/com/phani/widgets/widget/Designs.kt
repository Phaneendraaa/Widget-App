package com.phani.widgets.widget

import android.graphics.Paint
import android.graphics.Path
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.CUR
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.att
import com.phani.widgets.core.cnt
import com.phani.widgets.core.dateNext
import com.phani.widgets.core.dayIndex
import com.phani.widgets.core.daySched
import com.phani.widgets.core.fastState
import com.phani.widgets.core.mins
import com.phani.widgets.core.nextUp
import com.phani.widgets.core.pace
import com.phani.widgets.core.plural
import com.phani.widgets.core.rightNow
import com.phani.widgets.core.spendState
import com.phani.widgets.core.streak
import com.phani.widgets.core.subjectBy
import com.phani.widgets.core.subsMonthly
import com.phani.widgets.core.subsSorted
import com.phani.widgets.core.today
import com.phani.widgets.core.tzNow
import com.phani.widgets.core.tzOffset
import com.phani.widgets.core.tzText
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val D get() = AppStore.data
private val L = Paint.Align.LEFT
private val R = Paint.Align.RIGHT
private val C = Paint.Align.CENTER

/** Every widget design, drawn on a [Ctx]. Layout values follow the prototype's CSS. */
object Designs {

    fun draw(x: Ctx) {
        when (x.conf.type) {
            "now" -> now(x)
            "spend" -> spend(x)
            "subs" -> subs(x)
            "fast" -> fast(x)
            "counter" -> counter(x)
            "streak" -> streakW(x)
            "pace" -> paceW(x)
            "countdown" -> countdown(x)
            "year" -> year(x)
            "clock" -> clock(x)
            "next" -> next(x)
            "attendance" -> attendance(x)
        }
    }

    /* ---------- shared layout bits ---------- */

    private fun Ctx.top(text: String, right: String? = null, opt: Boolean = true) {
        if (!opt || showLabel) label(text, pad, pad + 11.5f, if (right != null) W - 2 * pad - measure(right, 12f, uiFace(400)) - 8f else W - 2 * pad)
        if (right != null) small(right, W - pad, pad + 11.5f, align = R)
    }

    private fun Ctx.empty(lbl: String, msg: String) {
        top(lbl, opt = false)
        small(msg, pad, H - pad - 2f)
    }

    /**
     * Big numeral bottom-aligned with an optional unit beside it, shrunk so it never rises above
     * [topLimit] or runs past the right edge. Returns numeral width.
     */
    private fun Ctx.bigWithUnit(n: String, unit: String?, base: Float, size: Float, color: Int = p.fg, topLimit: Float = 0f): Float {
        var sz = size
        if (topLimit > 0f) sz = min(sz, (base - topLimit) / 0.74f)
        val unitW = if (unit != null) measure(unit, 14f, uiFace(500)) + 6f else 0f
        val avail = W - 2 * pad - unitW
        val nw = numW(n, sz)
        if (nw > avail && nw > 0f) sz *= avail / nw
        sz = max(sz, 14f)
        val w = num(n, pad, base, sz, color)
        if (unit != null) text(unit, pad + w + 6f, base - 2f, 14f, uiFace(500), p.fg)
        return w
    }

    /** Largest size (up to [size]) at which [s] fits in [maxW]. */
    private fun Ctx.fitNum(s: String, size: Float, maxW: Float): Float {
        val w = numW(s, size)
        return if (w > maxW && w > 0f) max(10f, size * maxW / w) else size
    }

    /** Where content may start below the label row. */
    private val Ctx.below: Float get() = pad + 20f

    private fun clampSz(lo: Float, pctOfW: Float, hi: Float, W: Float) = (W * pctOfW).coerceIn(lo, hi)

    /** Left text column for 4×1 widgets; returns the width available. */
    private fun Ctx.wideLeft(rightReserve: Float): Float = W - 18f - 18f - rightReserve

    /* ---------- Right now ---------- */

    private fun now(x: Ctx) = with(x) {
        val r = rightNow()
        val hot = r.hot
        when (conf.design) {
            "ticker" -> {
                val bw = if (r.act != null) measure(r.act.label, 14f, uiFace(600)) + 28f else 0f
                val lw = wideLeft(if (r.act != null) bw + 14f else 0f)
                val mid = H / 2
                label(r.k, 18f, mid - 14f, lw)
                t2(r.h, 18f, mid + 4f, 16f, if (hot) p.acc else p.fg, lw)
                small(r.sub, 18f, mid + 21f, maxW = lw)
                if (r.act != null) button(W - 18f - bw, mid - 20f, bw, 40f, if (hot) "acc" else "solid", r.act.kind, r.act.id, text = r.act.label)
            }
            "bold" -> {
                top("Right now")
                val sz = clampSz(20f, 0.135f, 28f, W)
                para(r.h, pad, pad + 26f, W - 2 * pad, sz, numFace(), if (hot) p.acc else p.fg, numSpacing() + 0.02f, 1.02f, 4)
                para(r.sub, pad, H - pad - 32f, W - 2 * pad, 12f, uiFace(400), p.mut, 0f, 1.15f, 2)
            }
            else -> {
                if (showLabel) label("Right now · ${r.k}", pad, pad + 11.5f, W - 2 * pad - 16f)
                if (hot) circle(W - pad - 4f, pad + 7.5f, 4f, fill(p.acc))
                val hh = para(r.h, pad, pad + 26f, W - 2 * pad, 26f, numFace(), p.fg, numSpacing() + 0.015f, 1.05f, 2)
                para(r.sub, pad, pad + 32f + hh, W - 2 * pad, 13f, uiFace(400), p.mut, 0f, 1.2f, 2)
                if (r.act != null) button(pad, H - pad - 40f, W - 2 * pad, 40f, if (hot) "acc" else "solid", r.act.kind, r.act.id, text = r.act.label)
            }
        }
    }

    /* ---------- Safe to spend ---------- */

    private fun spend(x: Ctx) = with(x) {
        val s = spendState()
        val over = s.left < 0
        val q = (CUR[D.cur] ?: CUR.getValue("USD")).quick
        val big = if (over) "−" + Fmt.money(-s.left) else Fmt.money(s.left)
        val used = (s.today / max(1.0, s.per)).toFloat().coerceIn(0f, 1f)
        when (conf.design) {
            "ring" -> {
                top("Safe to spend")
                val bh = min(36f, H * 0.22f)
                val btnY = H - pad - bh
                val rs = min(84f, btnY - 6f - below)
                val sw = max(5f, rs * 0.085f)
                val cy = below + rs / 2
                ring(W / 2, cy, rs, sw, 1f - used, if (over) p.acc else p.fg)
                val bigR = if (over) "−" + Fmt.money(-s.left, 0) else Fmt.money(s.left, 0)
                val fs = fitNum(bigR, min(19f, rs * 0.25f), rs - 2 * sw - 10f)
                val showSub = rs >= 66f
                num(bigR, W / 2, cy + (if (showSub) 1f else cap(fs) / 2), fs, if (over) p.acc else p.fg, C)
                if (showSub) small(if (over) "over today" else "left today", W / 2, cy + 14f, size = 9.5f, align = C)
                val bw = (W - 2 * pad - 6f) / 2
                button(pad, btnY, bw, bh, "solid", "spend-q", value = q[0].toString(), text = "+${q[0]}")
                button(pad + bw + 6f, btnY, bw, bh, "ghost", "spend-open", ic = Paths.plus)
            }
            "strip" -> {
                val lw = wideLeft(66f)
                label("Safe today · ${plural(s.days, "day")} left", 18f, 24f, lw)
                num(big, 18f, 50f, 26f, if (over) p.acc else p.fg)
                small("of " + Fmt.money(s.per), 18f + lw, 50f, align = R)
                bar(18f, 60f, lw, 4f, used, if (over) p.acc else p.fg)
                button(W - 18f - 52f, H / 2 - 26f, 52f, 52f, "solid", "spend-open", ic = Paths.plus, icSize = 20f)
            }
            "receipt" -> {
                top("Safe to spend · resets " + Fmt.short(s.resets))
                val nb = pad + 26f + cap(44f)
                num(big, pad, nb, 44f, if (over) p.acc else p.fg)
                small("left of " + Fmt.money(s.per), W - pad, nb - 15f, align = R)
                small("today", W - pad, nb - 1f, align = R)
                dashed(pad, nb + 12f, W - pad, p.soft)
                var y = nb + 30f
                val rows = s.todays.takeLast(3).reversed()
                if (rows.isEmpty()) { small("Nothing spent yet today", pad, y); }
                rows.forEach { r ->
                    text(r.note.ifBlank { "Spend" }, pad, y, 13f, uiFace(400), p.fg, maxW = W - 2 * pad - 70f)
                    text(Fmt.money(r.amt), W - pad, y, 13f, uiFace(500), p.fg, align = R); y += 19f
                }
                val bw = (W - 2 * pad - 12f) / 3
                q.forEachIndexed { i, v -> button(pad + i * (bw + 6f), H - pad - 40f, bw, 40f, "ghost", "spend-q", value = v.toString(), text = "+$v") }
            }
            else -> {
                top("Safe to spend")
                val bh = min(40f, H * 0.24f)
                val btnTop = H - pad - bh
                val subBase = btnTop - 10f
                val sz = fitNum(big, min(clampSz(30f, 0.24f, 46f, W), (subBase - 18f - below) / 0.74f), W - 2 * pad)
                num(big, pad, subBase - 18f, sz, if (over) p.acc else p.fg)
                small((if (over) "over " else "left of ") + Fmt.money(s.per) + " today", pad, subBase, maxW = W - 2 * pad)
                val bw = (W - 2 * pad - 6f) / 2
                button(pad, btnTop, bw, bh, "solid", "spend-q", value = q[0].toString(), text = "+${q[0]}")
                button(pad + bw + 6f, btnTop, bw, bh, "ghost", "spend-q", value = q[1].toString(), text = "+${q[1]}")
            }
        }
    }

    /* ---------- Renewals ---------- */

    private fun subs(x: Ctx) = with(x) {
        val l = subsSorted()
        if (l.isEmpty()) { empty("Renewals", "Add a subscription in Track"); return@with }
        when (conf.design) {
            "list" -> {
                top("Upcoming charges", Fmt.money(subsMonthly(), 0) + "/mo")
                var y = pad + 40f
                l.take(4).forEachIndexed { i, r ->
                    val sz = if (i == 0) 20f else 15f
                    val dtxt = if (r.days == 0) "Now" else "${r.days}d"
                    num(dtxt, pad, y, sz, if (i == 0 && r.days <= 3) p.acc else p.fg)
                    t2(r.s.name, pad + 46f, y, if (i == 0) 15f else 14f, maxW = W - 2 * pad - 46f - 70f)
                    text(Fmt.money(r.s.amt), W - pad, y, 13f, uiFace(500), p.fg, align = R)
                    y += if (i == 0) 30f else 26f
                }
            }
            "total" -> {
                top("Subscriptions")
                val tot = Fmt.money(subsMonthly(), 0)
                val base = H - pad - 40f
                val sz = fitNum(tot, min(clampSz(28f, 0.22f, 42f, W), (base - below) / 0.74f), W - 2 * pad)
                num(tot, pad, base, sz)
                small("a month · " + plural(D.subs.size, "plan"), pad, base + 18f)
                small("Next: ${l[0].s.name} in ${l[0].days}d", pad, H - pad - 2f, maxW = W - 2 * pad)
            }
            else -> {
                val r = l[0]
                top("Next charge")
                val nameBase = if (showLabel) pad + 36f else pad + 16f
                t2(r.s.name, pad, nameBase, 17f, maxW = W - 2 * pad)
                val sz = clampSz(40f, 0.32f, 64f, W)
                val base = H - pad - 24f
                bigWithUnit(if (r.days == 0) "Today" else r.days.toString(), if (r.days == 0) null else if (r.days == 1) "day" else "days", base, sz, if (r.days <= 3) p.acc else p.fg, nameBase + 8f)
                small(Fmt.money(r.s.amt) + " · " + Fmt.short(r.d), pad, H - pad - 2f)
            }
        }
    }

    /* ---------- Fasting ---------- */

    private fun fast(x: Ctx) = with(x) {
        val f = fastState()
        when (conf.design) {
            "timer" -> {
                val lw = wideLeft(66f)
                label(if (f.on) "Fasting · goal ${f.target}h" else "Not fasting", 18f, 24f, lw - 40f)
                if (f.on) small("${(f.pct * 100).roundToInt()}%", 18f + lw, 24f, align = R)
                num(if (f.on) Fmt.hhmm(f.h) else "0:00", 18f, 50f, 26f)
                bar(18f, 60f, lw, 4f, f.pct.toFloat(), p.acc)
                button(W - 18f - 52f, H / 2 - 26f, 52f, 52f, if (f.on) "ghost" else "solid", "fast", ic = if (f.on) Paths.stop else Paths.play, icFilled = true, icSize = 20f)
            }
            "big" -> {
                top(if (f.on) "Fasting" else "Fast")
                val base = H - pad - 36f
                val sz = min(clampSz(44f, 0.36f, 70f, W), (base - below) / 0.74f)
                val w = num(if (f.on) floor(f.h).toInt().toString() else "0", pad, base, sz)
                text("h " + (if (f.on) ((f.h % 1) * 60).toInt().toString().padStart(2, '0') else "00") + "m", pad + w + 4f, base - 2f, 15f, uiFace(500), p.fg)
                bar(pad, base + 10f, W - 2 * pad, 3f, f.pct.toFloat(), p.acc)
                small(if (!f.on) "Tap to start a ${f.target}h fast" else if (f.h >= f.target) "Goal reached" else "Goal ${f.target}h · " + Fmt.at(f.endMs), pad, H - pad - 2f, maxW = W - 2 * pad)
                hitArea(0f, 0f, W, H, "fast")
            }
            else -> {
                top(if (f.on) "Fasting" else "Fast", "${f.target}h")
                val bh = min(36f, H * 0.22f)
                val btnY = H - pad - bh
                val rs = min(84f, btnY - 6f - below)
                val sw = max(5f, rs * 0.085f)
                val cy = below + rs / 2
                ring(W / 2, cy, rs, sw, f.pct.toFloat(), p.acc)
                val tx = if (f.on) Fmt.hhmm(f.h) else "—"
                val fs = fitNum(tx, min(20f, rs * 0.26f), rs - 2 * sw - 10f)
                num(tx, W / 2, cy + cap(fs) / 2, fs, align = C)
                button(pad, btnY, W - 2 * pad, bh, if (f.on) "ghost" else "solid", "fast", text = if (f.on) "End" else "Start", ic = if (f.on) Paths.stop else Paths.play, icFilled = true, icSize = 14f)
            }
        }
    }

    /* ---------- Counter ---------- */

    private fun counter(x: Ctx) = with(x) {
        val ct = D.counters.firstOrNull { it.id == conf.ref } ?: D.counters.firstOrNull()
        if (ct == null) { empty("Counter", "Add a counter in Track"); return@with }
        val v = cnt(ct)
        val pct = (v.toFloat() / ct.goal).coerceIn(0f, 1f)
        when (conf.design) {
            "dots" -> {
                val lw = wideLeft(66f)
                label(ct.name, 18f, 26f, lw - 40f)
                small("$v/${ct.goal}", 18f + lw, 26f, align = R)
                val per = max(1, ((lw + 5f) / 19f).toInt())
                for (i in 0 until ct.goal) {
                    val row = i / per; val col = i % per
                    circle(18f + 7f + col * 19f, 46f + row * 18f, 7f, fill(if (i < v) p.acc else p.soft))
                }
                button(W - 18f - 52f, H / 2 - 26f, 52f, 52f, "solid", "cnt", ct.id, ic = Paths.plus, icSize = 20f)
            }
            "big" -> {
                top(ct.name, "of ${ct.goal}")
                val sz = clampSz(46f, 0.38f, 74f, W)
                val base = H - pad - 26f
                bigWithUnit(v.toString(), ct.unit, base, sz, if (v >= ct.goal) p.acc else p.fg, below + 4f)
                small(if (v >= ct.goal) "Goal reached" else "Tap the number to add 1", pad, H - pad - 2f, maxW = W - 2 * pad)
                hitArea(0f, pad + 18f, W, H - pad - 40f, "cnt", ct.id)
            }
            else -> {
                top(ct.name, "$v/${ct.goal}")
                val gh = min(58f, H - pad - 22f - below - 8f).coerceAtLeast(30f); val gw = gh * 54f / 58f
                val gfs = min(34f, gh * 0.6f)
                val nw = numW(v.toString(), gfs)
                val total = gw + 12f + nw
                val gx = W / 2 - total / 2
                val gy = (below + H - pad - 18f) / 2 - gh / 2
                val glass = Path().apply {
                    moveTo(px(gx + gw * 0.11f), px(gy + 4f)); lineTo(px(gx + gw * 0.89f), px(gy + 4f)); lineTo(px(gx + gw * 0.8f), px(gy + gh - 2f))
                    lineTo(px(gx + gw * 0.2f), px(gy + gh - 2f)); close()
                }
                c.save(); c.clipPath(glass)
                val fillH = (gh - 6f) * pct
                rrect(gx, gy + gh - 2f - fillH, gw, fillH + 2f, 0f, fill(p.acc))
                c.restore()
                c.drawPath(glass, stroke(p.fg, 2f).apply { strokeJoin = Paint.Join.ROUND })
                num(v.toString(), gx + gw + 12f, gy + gh / 2 + cap(gfs) / 2, gfs)
                small("Tap to add 1", W / 2, H - pad - 2f, align = C)
                hitArea(0f, pad + 18f, W, H - pad - 18f, "cnt", ct.id)
            }
        }
    }

    /* ---------- Streak ---------- */

    private fun streakW(x: Ctx) = with(x) {
        val h = D.habits.firstOrNull { it.id == conf.ref } ?: D.habits.firstOrNull()
        if (h == null) { empty("Streak", "Add a habit in Track"); return@with }
        val st = streak(h)
        val t0 = today()
        val rec = if (st.n >= h.record && st.n > 1) "New best" else "Best ${h.record}"
        fun dots(x0: Float, cy: Float, d: Float, gap: Float) {
            for (i in 6 downTo 0) {
                val k = t0.minusDays(i.toLong()).toString()
                val cx = x0 + (6 - i) * (d + gap) + d / 2
                val done = h.done.contains(k); val frz = h.frozen.contains(k)
                when {
                    i == 0 && done -> circle(cx, cy, d / 2, fill(p.acc))
                    i == 0 -> circle(cx, cy, d / 2 - 1f, stroke(p.acc, 2f))
                    done -> circle(cx, cy, d / 2, fill(p.fg))
                    frz -> circle(cx, cy, d / 2 - 1f, stroke(p.mut, 2f))
                    else -> circle(cx, cy, d / 2, fill(p.soft))
                }
            }
        }
        when (conf.design) {
            "heat" -> {
                if (showLabel) label("${h.name} · 20 weeks", pad, pad + 11.5f, W - 2 * pad - 52f)
                val base = pad + 22f + cap(26f) + 6f
                val w = num(st.n.toString(), pad, base, 26f)
                text("day streak · $rec", pad + w + 6f, base, 14f, uiFace(500), p.fg, maxW = W - 2 * pad - w - 60f)
                button(W - pad - 44f, pad, 44f, 44f, if (st.today) "ghost" else "acc", "hab", h.id, ic = Paths.check)
                val cols = 20; val rows = 7; val gap = 3f
                val gtop = base + 14f
                val cell = min((W - 2 * pad - gap * (cols - 1)) / cols, (H - pad - gtop - gap * (rows - 1)) / rows)
                val start = t0.minusDays((cols * rows - 1).toLong())
                for (i in 0 until cols * rows) {
                    val col = i / rows; val row = i % rows
                    val k = start.plusDays(i.toLong()).toString()
                    val cx = pad + col * (cell + gap); val cy = gtop + row * (cell + gap)
                    val last = i == cols * rows - 1
                    when {
                        h.done.contains(k) -> rrect(cx, cy, cell, cell, 2.5f, fill(if (last) p.acc else p.fg))
                        h.frozen.contains(k) -> rrect(cx + 0.75f, cy + 0.75f, cell - 1.5f, cell - 1.5f, 2.5f, stroke(p.mut, 1.5f))
                        else -> rrect(cx, cy, cell, cell, 2.5f, fill(p.soft))
                    }
                }
            }
            "big" -> {
                top("${h.name} · $rec")
                val sz = clampSz(44f, 0.36f, 70f, W)
                val base = H - pad - 26f
                bigWithUnit(st.n.toString(), "days", base, sz, topLimit = below + 4f)
                dots(pad, H - pad - 6f, 11f, 4f)
                hitArea(0f, pad + 18f, W, H - pad - 40f, "hab", h.id)
            }
            "ring" -> {
                top(h.name)
                val rs = min(88f, H - pad - 16f - below - 6f)
                val cy = below + rs / 2
                val r = rs / 2 - 4.5f
                val segDeg = 360f / 7
                for (i in 0 until 7) {
                    val k = t0.minusDays((6 - i).toLong()).toString()
                    val on = h.done.contains(k)
                    val col = if (on) (if (i == 6) p.acc else p.fg) else p.soft
                    val rect = android.graphics.RectF(px(W / 2 - r), px(cy - r), px(W / 2 + r), px(cy + r))
                    c.drawArc(rect, -90f + i * segDeg + 4f, segDeg - 8f, false, stroke(col, 9f).apply { strokeCap = Paint.Cap.BUTT })
                }
                num(st.n.toString(), W / 2, cy + 6f, 26f, align = C)
                small("days", W / 2, cy + 19f, size = 10f, align = C)
                small(if (st.today) "Done today" else "Tap to check in", W / 2, H - pad - 2f, align = C)
                hitArea(0f, pad + 18f, W, H - pad - 18f, "hab", h.id)
            }
            else -> {
                val lw = wideLeft(44f + 14f + 7 * 15f + 14f)
                label("${h.name} · $rec", 18f, H / 2 - 6f, lw)
                num("${st.n} days", 18f, H / 2 + 17f, 22f)
                val dx = W - 18f - 44f - 14f - (7 * 11f + 6 * 4f)
                dots(dx, H / 2, 11f, 4f)
                button(W - 18f - 44f, H / 2 - 22f, 44f, 44f, if (st.today) "ghost" else "acc", "hab", h.id, ic = Paths.check)
            }
        }
    }

    /* ---------- Pace ---------- */

    private fun paceW(x: Ctx) = with(x) {
        val g = D.goals.firstOrNull { it.id == conf.ref } ?: D.goals.firstOrNull()
        if (g == null) { empty("Pace", "Add a goal in Track"); return@with }
        val r = pace(g)
        val a = r.ahead.roundToInt()
        val stTxt = if (a <= -1) plural(-a, "day") + " behind" else if (a >= 1) plural(a, "day") + " ahead" else "On track"
        val stCol = if (a <= -1) p.acc else p.fg
        val per = Fmt.num(r.per) + Fmt.unit(g.unit)
        when (conf.design) {
            "bar" -> {
                val lw = wideLeft(66f)
                label(g.name, 18f, 24f, lw - 60f)
                small("${Fmt.trimNum(g.done)}/${Fmt.trimNum(g.target)}", 18f + lw, 24f, align = R)
                bar(18f, 36f, lw, 6f, r.pct.toFloat(), if (a <= -1) p.acc else p.fg, r.exp.toFloat())
                val w = small("$per/day · ", 18f, 64f, p.fg, 13f)
                small(stTxt, 18f + w, 64f, stCol, 13f, weight = 600)
                button(W - 18f - 52f, H / 2 - 26f, 52f, 52f, "solid", "goal", g.id, ic = Paths.plus, icSize = 20f)
            }
            "daily" -> {
                top(g.name)
                small("Needed per day", pad, pad + 36f)
                val sz = clampSz(38f, 0.30f, 60f, W)
                val base = H - pad - 24f
                bigWithUnit(Fmt.num(r.per), g.unit, base, sz, topLimit = pad + 44f)
                val w = small(stTxt, pad, H - pad - 2f, stCol, weight = 600)
                small(" · ${r.left}d left", pad + w, H - pad - 2f, p.fg)
            }
            else -> {
                top(g.name)
                val rs = min(48f, H - pad - 38f - below - 4f)
                val cy = below + 2f + rs / 2
                ring(pad + rs / 2, cy, rs, 6f, r.pct.toFloat(), if (a <= -1) p.acc else p.fg)
                val pt = "${(r.pct * 100).roundToInt()}%"
                val pfs = fitNum(pt, 22f, W - 2 * pad - rs - 12f)
                num(pt, pad + rs + 12f, cy + cap(pfs) / 2, pfs)
                text("$per a day", pad, H - pad - 18f, 12.5f, uiFace(600), p.fg, maxW = W - 2 * pad)
                small(stTxt, pad, H - pad - 2f, stCol, weight = 600, maxW = W - 2 * pad)
                hitArea(0f, pad + 18f, W, H - pad - 50f, "goal", g.id)
            }
        }
    }

    /* ---------- Countdown ---------- */

    private fun countdown(x: Ctx) = with(x) {
        if (conf.design == "stack") {
            val rows = D.dates.map { it to dateNext(it) }.filter { it.second.days >= 0 }.sortedBy { it.second.days }.take(3)
            if (rows.isEmpty()) { empty("Coming up", "Add a date in Track"); return@with }
            top("Coming up")
            var y = pad + 26f
            rows.forEachIndexed { i, (d, n) ->
                val sz = if (i == 0) 42f else 26f
                val base = y + cap(sz)
                num(n.days.toString(), pad, base, sz, if (i == 0) p.acc else p.fg)
                val nx = pad + if (i == 0) 74f else 52f
                t2(d.name, nx, base - (if (i == 0) 16f else 10f), if (i == 0) 16f else 14f, maxW = W - nx - pad)
                small((if (n.days == 1) "day" else "days") + " · " + Fmt.full(n.d), nx, base + 2f, maxW = W - nx - pad)
                y = base + 16f
            }
            return@with
        }
        val d = D.dates.firstOrNull { it.id == conf.ref } ?: D.dates.firstOrNull()
        if (d == null) { empty("Countdown", "Add a date in Track"); return@with }
        val n = dateNext(d)
        val span = if (d.yearly) 365 else max(n.days, 1) + 30
        val prog = (1f - n.days.toFloat() / span).coerceIn(0f, 1f)
        when (conf.design) {
            "flip" -> {
                val digits = n.days.toString().padStart(3, '0')
                val tw = 36f; val gap = 5f
                val tilesW = digits.length * tw + (digits.length - 1) * gap
                val vx = W - 18f - 10f
                val tx = vx - 10f - tilesW
                label(d.name, 18f, H / 2 - 6f, tx - 18f - 10f)
                text(Fmt.full(n.d), 18f, H / 2 + 14f, 15f, t2Face(), p.fg, maxW = tx - 18f - 10f)
                digits.forEachIndexed { i, ch ->
                    val x0 = tx + i * (tw + gap)
                    rrect(x0, H / 2 - 28f, tw, 56f, 10f, fill(p.soft))
                    num(ch.toString(), x0 + tw / 2, H / 2 + cap(30f) / 2, 30f, align = C)
                    line(x0, H / 2, x0 + tw, H / 2, stroke(p.bg or 0xFF000000.toInt(), 1f).apply { strokeCap = Paint.Cap.BUTT; alpha = 230 })
                }
                c.save(); c.rotate(-90f, px(vx + 4f), px(H / 2))
                text("DAYS", vx + 4f, H / 2 + 4f, 10f, labelFace(), p.mut, 0.12f, C)
                c.restore()
            }
            "ring" -> {
                top(d.name)
                val rs = min(84f, H - pad - 16f - below - 6f)
                val cy = below + rs / 2
                ring(W / 2, cy, rs, max(5f, rs * 0.085f), prog, p.acc)
                val fs = fitNum(n.days.toString(), min(28f, rs * 0.34f), rs - 22f)
                num(n.days.toString(), W / 2, cy + (if (rs >= 64f) 4f else cap(fs) / 2), fs, align = C)
                if (rs >= 64f) small(if (n.days == 1) "day" else "days", W / 2, cy + 17f, size = 10f, align = C)
                small(Fmt.full(n.d), W / 2, H - pad - 2f, align = C)
            }
            else -> {
                top(d.name)
                val sz = clampSz(44f, 0.36f, 70f, W)
                val base = H - pad - 34f
                bigWithUnit(if (n.days == 0) "Today" else n.days.toString(), if (n.days == 0) null else if (n.days == 1) "day" else "days", base, sz, topLimit = below + 4f)
                bar(pad, base + 12f, W - 2 * pad, 3f, prog, p.acc)
                small(Fmt.full(n.d), pad, H - pad - 2f)
            }
        }
    }

    /* ---------- Progress ---------- */

    private fun year(x: Ctx) = with(x) {
        val t0 = today()
        val now = LocalTime.now()
        val yStart = LocalDate.of(t0.year, 1, 1)
        val n = yStart.lengthOfYear()
        val dn = t0.dayOfYear - 1
        val dayFrac = (now.hour * 60 + now.minute) / 1440f
        val yp = (dn + dayFrac) / n
        when (conf.design) {
            "bars" -> {
                top("${t0.year} · how much is gone")
                val md = t0.lengthOfMonth()
                val mp = (t0.dayOfMonth - 1 + now.hour / 24f) / md
                val wd = (t0.dayOfWeek.value - 1)
                val wp = (wd + now.hour / 24f) / 7f
                val rows = listOf("Year" to yp, "Month" to mp, "Week" to wp, "Day" to dayFrac)
                var y = pad + 40f
                rows.forEach { (l, v) ->
                    t2(l, pad, y + 4f, 13f)
                    bar(pad + 62f, y - 3f, W - 2 * pad - 62f - 54f, 6f, v, if (l == "Day") p.acc else p.fg)
                    text("${(v * 100).roundToInt()}%", W - pad, y + 4f, 13f, uiFace(600), p.fg, align = R)
                    y += 30f
                }
            }
            "ring" -> {
                top(t0.year.toString())
                val rs = min(84f, H - pad - 16f - below - 6f)
                val cy = below + rs / 2
                ring(W / 2, cy, rs, max(5f, rs * 0.085f), yp, p.acc)
                val yt = "${floor(yp * 100).toInt()}%"
                val fs = fitNum(yt, min(26f, rs * 0.32f), rs - 22f)
                num(yt, W / 2, cy + cap(fs) / 2, fs, align = C)
                small("${n - dn - 1} days left", W / 2, H - pad - 2f, align = C)
            }
            else -> {
                top("${t0.year} · day ${dn + 1}", String.format("%.1f%%", yp * 100))
                val cols = 31; val gap = 3f
                val d = (W - 2 * pad - gap * (cols - 1)) / cols
                val gtop = pad + 26f
                for (i in 0 until n) {
                    val col = i % cols; val row = i / cols
                    val cx = pad + col * (d + gap) + d / 2; val cy = gtop + row * (d + gap) + d / 2
                    circle(cx, cy, d / 2, fill(if (i < dn) p.fg else if (i == dn) p.acc else p.soft))
                }
                small("${n - dn - 1} days left this year", pad, H - pad - 2f)
            }
        }
    }

    /* ---------- World clock ---------- */

    private fun ampm(h: Int) = if (D.h24) "" else if (h >= 12) " pm" else " am"

    private fun clock(x: Ctx) = with(x) {
        val l = D.clocks
        if (l.isEmpty()) { empty("World clock", "Add a city in Track"); return@with }
        when (conf.design) {
            "analog" -> {
                top("World clock")
                val shown = l.take(3)
                val colW = (W - 2 * pad) / shown.size
                shown.forEachIndexed { i, k ->
                    val o = tzNow(k.tz)
                    val cx = pad + colW * i + colW / 2
                    val cy = pad + 30f + 38f
                    val night = o.h < 6 || o.h >= 19
                    circle(cx, cy, 36f, fill(if (night) p.fg else p.soft))
                    for (j in 0 until 12) {
                        val a = j * PI / 6
                        circle(cx + 29f * sin(a).toFloat(), cy - 29f * cos(a).toFloat(), if (j % 3 == 0) 1.8f else 1f, fill(if (night) p.bg or 0xFF000000.toInt() else p.mut))
                    }
                    val ha = ((o.h % 12) + o.m / 60.0) * 30 * PI / 180
                    val ma = o.m * 6 * PI / 180
                    val hc = if (night) (p.bg or 0xFF000000.toInt()) else p.fg
                    line(cx, cy, cx + 17f * sin(ha).toFloat(), cy - 17f * cos(ha).toFloat(), stroke(hc, 3.5f))
                    line(cx, cy, cx + 25f * sin(ma).toFloat(), cy - 25f * cos(ma).toFloat(), stroke(hc, 2f))
                    circle(cx, cy, 3f, fill(p.acc))
                    t2(k.city, cx, cy + 56f, 13f, maxW = colW - 6f, align = C)
                    small(tzOffset(k.tz), cx, cy + 72f, align = C)
                }
            }
            "list" -> {
                top("World clock")
                var y = pad + 30f
                l.take(3).forEach { k ->
                    val o = tzNow(k.tz)
                    val night = o.h < 6 || o.h >= 19
                    if (night) circle(pad + 5f, y + 10f, 5f, fill(p.fg)) else circle(pad + 5f, y + 10f, 4f, stroke(p.acc, 2f))
                    t2(k.city, pad + 20f, y + 8f, 15f, maxW = W / 2)
                    small("${o.wd} · ${tzOffset(k.tz)}", pad + 20f, y + 24f)
                    val ap = ampm(o.h)
                    val apW = if (ap.isNotEmpty()) measure(ap, 12f, uiFace(500)) else 0f
                    num(tzText(o), W - pad - apW, y + 22f, 28f, align = R)
                    if (ap.isNotEmpty()) text(ap, W - pad, y + 22f, 12f, uiFace(500), p.fg, align = R)
                    y += 44f
                }
            }
            else -> {
                val two = l.take(2)
                val colW = (W - 36f - (if (two.size > 1) 28f else 0f)) / two.size
                two.forEachIndexed { i, k ->
                    val o = tzNow(k.tz)
                    val x0 = 18f + i * (colW + 28f)
                    if (i > 0) line(x0 - 14f, 16f, x0 - 14f, H - 16f, stroke(p.soft, 1f))
                    label("${k.city} · ${tzOffset(k.tz)}", x0, H / 2 - 10f, colW)
                    val w = num(tzText(o), x0, H / 2 + 22f, 30f)
                    val ap = ampm(o.h)
                    if (ap.isNotEmpty()) text(ap, x0 + w, H / 2 + 22f, 12f, uiFace(500), p.fg)
                }
            }
        }
    }

    /* ---------- Next up ---------- */

    private fun next(x: Ctx) = with(x) {
        val nu = nextUp()
        if (nu == null) { empty("Next up", "Add your schedule in Track"); return@with }
        if (conf.design == "today") {
            val now = LocalTime.now(); val m = now.hour * 60 + now.minute
            val rest = daySched(dayIndex()).filter { mins(it.e) > m }.take(4)
            if (rest.isEmpty()) {
                top("Today")
                t2("Day's done", pad, pad + 44f, 18f)
                small("Next: ${nu.c.title} · ${nu.dayName} ${Fmt.hm(nu.c.s)}", pad, pad + 64f, maxW = W - 2 * pad)
                return@with
            }
            top("Today · ${rest.size} left")
            var y = pad + 28f
            rest.forEachIndexed { i, c ->
                val nowc = mins(c.s) <= m
                text(Fmt.hm(c.s), pad, y + 18f, 13f, uiFace(if (i == 0) 600 else 500), if (i == 0) p.fg else p.mut)
                rrect(pad + 72f, y, 3f, 32f, 1.5f, fill(if (i == 0) p.acc else p.soft))
                t2(c.title, pad + 85f, y + 13f, if (i == 0) 15f else 14f, maxW = W - pad - 85f - pad)
                small(if (nowc) "Now · ends " + Fmt.hm(c.e) else c.place, pad + 85f, y + 29f, maxW = W - pad - 85f - pad)
                y += 40f
            }
            return@with
        }
        val lbl = when (nu.state) { "now" -> "Now · ends " + Fmt.hm(nu.c.e); "next" -> "Next · " + Fmt.hm(nu.c.s); else -> nu.dayName + " · " + Fmt.hm(nu.c.s) }
        val right = when (nu.state) { "now" -> Fmt.dur(nu.left) + " left"; "next" -> "in " + Fmt.dur(nu.inMin); else -> Fmt.hm(nu.c.s) }
        val rw = max(numW(right, 22f), measure(nu.c.place, 12f, uiFace(400)))
        val lw = W - 36f - rw - 14f
        label(lbl, 18f, H / 2 - 6f, lw)
        t2(nu.c.title, 18f, H / 2 + 15f, 17f, maxW = lw)
        num(right, W - 18f, H / 2 + 4f, 22f, if (nu.state == "next" && nu.inMin <= 15) p.acc else p.fg, R)
        small(nu.c.place, W - 18f, H / 2 + 21f, align = R)
    }

    /* ---------- Attendance ---------- */

    private fun attendance(x: Ctx) = with(x) {
        if (D.subjects.isEmpty()) { empty("Attendance", "Add classes in Track"); return@with }
        when (conf.design) {
            "single" -> {
                val s = subjectBy(conf.ref) ?: D.subjects.minByOrNull { att(it).p }!!
                val r = att(s)
                top("${s.short} · need ${D.req}%")
                val base = H - pad - 40f
                val sz = min(clampSz(40f, 0.34f, 64f, W), (base - below) / 0.74f)
                val w = num("${(r.p * 100).roundToInt()}", pad, base, sz, if (r.ok) p.fg else p.acc)
                num("%", pad + w, base, sz * 0.45f, if (r.ok) p.fg else p.acc)
                bar(pad, H - pad - 28f, W - 2 * pad, 6f, r.p.toFloat(), if (r.ok) p.fg else p.acc, D.req / 100f)
                val msg = if (r.ok) (if (r.skip > 0) "Can miss ${r.skip}" else "No misses left") else "Attend next ${r.need}"
                text(msg, pad, H - pad - 2f, 13f, uiFace(if (r.ok) 400 else 600), if (r.ok) p.fg else p.acc)
            }
            "mark" -> {
                val now = LocalTime.now(); val m = now.hour * 60 + now.minute
                val withSub = daySched(dayIndex()).filter { it.sub != null }
                var cls = withSub.lastOrNull { mins(it.s) <= m }
                val s = subjectBy(cls?.sub) ?: D.subjects[0]
                if (subjectBy(cls?.sub) == null) cls = null
                val r = att(s)
                top(if (cls != null) Fmt.hm(cls.s) else "Last class")
                val hh = para(s.name, pad, pad + 24f, W - 2 * pad, 17f, t2Face(), p.fg, -0.01f, 1.1f, 2)
                small("${s.a}/${s.t} · ${(r.p * 100).roundToInt()}%", pad, pad + 24f + hh + 16f)
                val bw = (W - 2 * pad - 8f) / 2
                button(pad, H - pad - 40f, bw, 40f, "solid", "att-p", s.id, ic = Paths.check)
                button(pad + bw + 8f, H - pad - 40f, bw, 40f, "ghost", "att-a", s.id, ic = Paths.x)
            }
            else -> {
                top("Attendance · min ${D.req}%")
                var y = pad + 38f
                D.subjects.take(4).forEach { s ->
                    val r = att(s)
                    t2(s.short, pad, y + 5f, 14f)
                    val bx = pad + 44f
                    val bw = W - 2 * pad - 44f - 48f - 78f
                    bar(bx, y - 2f, bw, 6f, r.p.toFloat(), if (r.ok) p.fg else p.acc, D.req / 100f)
                    text("${(r.p * 100).roundToInt()}%", bx + bw + 46f, y + 5f, 13f, uiFace(600), if (r.ok) p.fg else p.acc, align = R)
                    val t = if (r.ok) (if (r.skip > 0) "miss ${r.skip}" else "none left") else "attend ${r.need}"
                    text(t, W - pad, y + 5f, 12f, uiFace(if (r.ok) 400 else 600), if (r.ok) p.mut else p.acc, align = R)
                    y += 30f
                }
            }
        }
    }
}
