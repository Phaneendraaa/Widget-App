package com.phani.widgets.widget

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Counter
import com.phani.widgets.core.calmStreak
import com.phani.widgets.core.cnt
import com.phani.widgets.core.today
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Protein and meditation. */
object MoreDesigns {
    private val D get() = AppStore.view
    private val C = Paint.Align.CENTER
    private val R = Paint.Align.RIGHT
    private fun a(alpha: Float, c: Int) = ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)

    fun draw(x: Ctx): Boolean {
        when (x.conf.type) {
            "protein" -> protein(x)
            "calm" -> calm(x)
            "prayer", "meds" -> rituals(x)
            else -> return false
        }
        return true
    }

    private fun ref(unit: String, id: String?): Counter? =
        D.counters.firstOrNull { it.id == id } ?: D.counters.firstOrNull { it.unit == unit }

    private fun Ctx.none(lbl: String, msg: String) { label(ttl(lbl), pad, pad + 11.5f); small(msg, pad, H - pad - 2f, maxW = W - 2 * pad) }

    /* ---------- Protein ---------- */

    private fun protein(x: Ctx) = with(x) {
        val ct = ref("g", conf.ref) ?: run { none("Protein", "Set your daily goal"); return@with }
        val v = cnt(ct); val goal = max(1, ct.goal)
        val pct = (v.toFloat() / goal).coerceIn(0f, 1f)
        val done = v >= goal
        val col = if (done) p.acc else p.fg
        when (conf.design) {
            "bar" -> {
                val lw = W - 2 * pad - 106f
                label(ttl(ct.name) + " · goal ${goal}g", pad + 2f, pad + 10f, lw)
                val n = "${v}g"
                num(n, pad + 2f, H / 2 + 12f, 28f, col)
                small(if (done) "Goal reached" else "${goal - v}g to go", pad + 2f + numW(n, 28f) + 8f, H / 2 + 12f, size = 11.5f, maxW = lw - numW(n, 28f) - 8f)
                bar(pad + 2f, H - pad - 8f, lw, 5f, pct, if (done) p.acc else p.fg)
                button(W - pad - 98f, H / 2 - 21f, 54f, 42f, "solid", "cnt", ct.id, "20", text = "+20")
                button(W - pad - 40f, H / 2 - 21f, 40f, 42f, "ghost", "quick", ic = Paths.plus, icSize = 16f)
            }
            "column" -> {
                label(ttl(ct.name), pad, pad + 11.5f, W - 2 * pad)
                val bh = 32f; val btnY = H - pad - bh
                val tTop = pad + 24f; val tBot = btnY - 12f; val tw = 26f; val tx = pad + 2f
                rrect(tx, tTop, tw, tBot - tTop, tw / 2, fill(p.soft))
                if (v > 0) {
                    val fh = max(tw, (tBot - tTop) * pct)
                    c.drawRoundRect(RectF(px(tx), px(tBot - fh), px(tx + tw), px(tBot)), px(tw / 2), px(tw / 2), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(0f, px(tBot - fh), 0f, px(tBot), a(0.75f, p.acc), p.acc, Shader.TileMode.CLAMP)
                    })
                }
                val rx = tx + tw + 14f
                num("$v", rx, tTop + cap(34f) + 2f, min(34f, (W - pad - rx) / max(1f, numW("$v", 34f) / 34f)), col)
                small("of ${goal}g", rx, tTop + cap(34f) + 18f, size = 11.5f, maxW = W - pad - rx)
                small(if (done) "Goal reached" else "${goal - v}g to go", rx, tTop + cap(34f) + 34f, if (done) p.acc else p.fg, 11.5f, maxW = W - pad - rx, weight = 600)
                val bw = (W - 2 * pad - 12f) / 3
                button(pad, btnY, bw, bh, "ghost", "cnt", ct.id, "10", text = "+10")
                button(pad + bw + 6f, btnY, bw, bh, "solid", "cnt", ct.id, "25", text = "+25")
                button(pad + 2 * (bw + 6f), btnY, bw, bh, "ghost", "quick", ic = Paths.plus, icSize = 14f)
            }
            else -> { // ring
                label(ttl(ct.name), pad, pad + 11.5f, W - 2 * pad)
                val bh = min(34f, H * 0.21f); val btnY = H - pad - bh
                val rs = min(86f, btnY - 8f - (pad + 20f))
                val cy = pad + 20f + rs / 2
                ring(W / 2, cy, rs, max(6f, rs * 0.09f), pct, col)
                val n = "$v"
                val sz = min(24f, (rs - 26f) / max(1f, numW(n, 24f) / 24f))
                num(n, W / 2, cy + 3f, sz, col, C)
                small("of ${goal}g", W / 2, cy + 17f, size = 10f, align = C)
                val bw = (W - 2 * pad - 6f) / 2
                button(pad, btnY, bw, bh, "solid", "cnt", ct.id, "20", text = "+20")
                button(pad + bw + 6f, btnY, bw, bh, "ghost", "quick", ic = Paths.plus)
            }
        }
    }

    /* ---------- Meditation ---------- */

    /** Soft rings, like a breath held in the middle of the widget. */
    private fun Ctx.breath(cx: Float, cy: Float, r: Float, filled: Float) {
        c.drawCircle(px(cx), px(cy), px(r * 1.25f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(px(cx), px(cy), px(r * 1.25f), a(0.22f, p.acc), a(0f, p.acc), Shader.TileMode.CLAMP)
        })
        listOf(1f, 0.78f, 0.56f).forEachIndexed { i, k -> circle(cx, cy, r * k, fill(a(0.10f + i * 0.08f, p.acc))) }
        // how much of today's goal is done, as a ring
        val st = stroke(a(0.25f, p.fg), 2f); circle(cx, cy, r, st)
        if (filled > 0f) c.drawArc(RectF(px(cx - r), px(cy - r), px(cx + r), px(cy + r)), -90f, 360f * filled.coerceIn(0f, 1f), false, stroke(p.acc, 3f))
    }

    private fun calm(x: Ctx) = with(x) {
        val ct = ref("min", conf.ref) ?: run { none("Meditation", "Set your daily minutes"); return@with }
        val v = cnt(ct); val goal = max(1, ct.goal)
        val pct = v.toFloat() / goal
        val stk = calmStreak(ct)
        val sub = if (stk > 0) "$stk-day streak" else "Start a streak"
        when (conf.design) {
            "strip" -> {
                val lw = W - 2 * pad - 90f
                label(ttl(ct.name) + " · " + sub, pad + 2f, pad + 10f, lw)
                val n = "$v"
                num(n, pad + 2f, H / 2 + 10f, 26f, p.fg)
                small("of $goal min today", pad + 2f + numW(n, 26f) + 6f, H / 2 + 10f, size = 11.5f, maxW = lw - 40f)
                // the last 7 days
                val t0 = today()
                for (i in 0 until 7) {
                    val d = t0.minusDays((6 - i).toLong())
                    val m = ct.log[d.toString()] ?: 0
                    val cx = pad + 8f + i * 16f
                    if (m >= goal) circle(cx, H - pad - 6f, 5f, fill(p.acc)) else if (m > 0) circle(cx, H - pad - 6f, 5f, fill(a(0.45f, p.acc)))
                    else circle(cx, H - pad - 6f, 4.4f, stroke(a(0.3f, p.fg), 1.2f))
                }
                button(W - pad - 80f, H / 2 - 20f, 80f, 40f, "solid", "quick", text = "Begin")
            }
            "tall" -> {
                label(ttl(ct.name), pad, pad + 11.5f, W - 2 * pad)
                val bh = 34f; val btnY = H - pad - bh
                val r = min(W / 2 - pad - 4f, (btnY - 60f - (pad + 22f)) / 2)
                val cy = pad + 22f + r + 6f
                breath(W / 2, cy, r, pct)
                text("Breathe", W / 2, cy + 5f, 14f, uiFace(600), p.fg, align = C)
                val n = "$v / $goal min"
                text(n, W / 2, cy + r + 24f, 15f, uiFace(600), if (v >= goal) p.acc else p.fg, align = C)
                small(sub, W / 2, cy + r + 40f, size = 11.5f, align = C)
                button(pad, btnY, W - 2 * pad, bh, "solid", "quick", text = "Begin")
            }
            else -> { // breathe
                label(ttl(ct.name), pad, pad + 11.5f, W - 2 * pad - 30f)
                val bh = min(32f, H * 0.2f); val btnY = H - pad - bh
                val r = min(W / 2 - pad - 18f, (btnY - 8f - (pad + 20f)) / 2)
                val cy = pad + 20f + r + 2f
                breath(W / 2, cy, r, pct)
                text("$v", W / 2, cy + 2f, min(24f, r * 0.7f), numFace(), if (v >= goal) p.acc else p.fg, align = C)
                small("of $goal min", W / 2, cy + 15f, size = 10f, align = C)
                small(if (stk > 0) "$stk" else "", W - pad, pad + 11.5f, p.acc, 11.5f, R, weight = 600)
                button(pad, btnY, W - 2 * pad, bh, "solid", "quick", text = "Begin")
            }
        }
    }

    /* ---------- Prayer and medicine ---------- */

    private fun hm(t: java.time.LocalTime): String =
        t.format(java.time.format.DateTimeFormatter.ofPattern(if (D.h24) "HH:mm" else "h:mm a", Locale.US)).lowercase()

    private fun until(t: java.time.LocalTime): String {
        val m = java.time.Duration.between(java.time.LocalTime.now(), t).toMinutes().toInt()
        return when { m <= 0 -> "now"; m < 60 -> "in $m min"; else -> "in ${m / 60}h ${m % 60}m" }
    }

    private fun rituals(x: Ctx) = with(x) {
        val med = conf.type == "meds"
        val items = com.phani.widgets.core.Rituals.items(conf.type, D)
        val title = ttl(if (med) "Medicine" else "Prayer")
        if (items.isEmpty()) {
            none(title, if (med) "Add a medicine" else if (D.prayMode == "salah" && D.place == null) "Pick your city" else "Add a prayer time"); return@with
        }
        val f = com.phani.widgets.core.Rituals.focus(items)
        val doneN = items.count { it.done }
        val doneWord = if (med) "Taken" else "Done"
        when (conf.design) {
            "list", "tall" -> {
                label(title, pad, pad + 11.5f, W - 2 * pad - 50f)
                small("$doneN/${items.size}", W - pad, pad + 11.5f, size = 11.5f, align = R)
                val top = pad + 22f
                val rh = min(if (W < 220f) 38f else 28f, (H - pad - top) / max(1, items.size))
                items.forEachIndexed { i, it ->
                    val y = top + rh * i
                    val focus = f?.first?.key == it.key
                    if (focus) rrect(pad - 6f, y + 1f, W - 2 * pad + 12f, rh - 2f, 10f, fill(a(if (f!!.second) 0.16f else 0.08f, p.acc)))
                    val base = y + rh / 2 + 4.5f
                    if (W < 220f) {
                        // narrow: the name, with its time underneath
                        text(it.name, pad, base - 5f, 13f, uiFace(if (focus) 600 else 500), if (it.done) p.mut else p.fg, maxW = W - 2 * pad - 26f)
                        text(hm(it.time), pad, base + 8f, 10.5f, monoFace(500), p.mut)
                    } else {
                        text(hm(it.time), pad, base, 11.5f, monoFace(500), if (it.done) p.mut else p.fg)
                        val nx = pad + if (D.h24) 46f else 62f
                        text(it.name, nx, base, 13.5f, uiFace(if (focus) 600 else 500), if (it.done) p.mut else p.fg, maxW = W - pad - nx - 30f)
                    }
                    val cx = W - pad - 10f; val cy = y + rh / 2
                    if (it.done) { circle(cx, cy, 9f, fill(p.fg)); icon(Paths.check, cx, cy, 12f, p.bg or 0xFF000000.toInt(), 2.6f) }
                    else circle(cx, cy, 8.4f, stroke(a(if (focus && f!!.second) 1f else 0.4f, if (focus && f!!.second) p.acc else p.fg), 1.6f))
                    hitArea(pad - 6f, y, W - 2 * pad + 12f, rh, "rit", it.key)
                }
            }
            else -> { // next
                label(title, pad, pad + 11.5f, W - 2 * pad - 30f)
                small("$doneN/${items.size}", W - pad, pad + 11.5f, size = 11.5f, align = R)
                val bh = min(32f, H * 0.2f); val btnY = H - pad - bh
                if (f == null) {
                    text("All done today", pad, pad + 44f, 16f, uiFace(600), p.fg, maxW = W - 2 * pad)
                    small(if (med) "Every dose taken" else "Every prayer marked", pad, pad + 61f, size = 11.5f, maxW = W - 2 * pad)
                } else {
                    val (it, due) = f
                    text(it.name, pad, pad + 44f, 18f, uiFace(600), p.fg, maxW = W - 2 * pad)
                    val tm = hm(it.time)
                    val sz = min(26f, (W - 2 * pad) / max(1f, numW(tm, 26f) / 26f))
                    num(tm, pad, pad + 48f + cap(sz) + 6f, sz, if (due) p.acc else p.fg)
                    small(if (due) "Due now" else until(it.time), pad, pad + 48f + cap(sz) + 22f, if (due) p.acc else p.mut, 11.5f, maxW = W - 2 * pad, weight = if (due) 600 else 400)
                    if (due) button(pad, btnY, W - 2 * pad, bh, "solid", "rit", it.key, text = doneWord)
                }
                if (f == null || !f.second) {
                    // today's items as dots
                    val n = items.size; val gap = 6f
                    val dw = min(12f, (W - 2 * pad - gap * (n - 1)) / n)
                    items.forEachIndexed { i, itx ->
                        val cx = pad + dw / 2 + i * (dw + gap); val cy = btnY + bh / 2
                        if (itx.done) circle(cx, cy, dw / 2, fill(p.fg)) else circle(cx, cy, dw / 2 - 0.8f, stroke(a(0.4f, p.fg), 1.4f))
                    }
                }
            }
        }
    }
}
