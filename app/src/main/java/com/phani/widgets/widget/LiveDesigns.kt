package com.phani.widgets.widget

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Live
import com.phani.widgets.core.Wx
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Weather, air quality and rates: live widgets drawn from the data [Live] keeps fresh. */
object LiveDesigns {
    private val D get() = AppStore.view
    private val L = Paint.Align.LEFT
    private val R = Paint.Align.RIGHT
    private val C = Paint.Align.CENTER
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val SUN = 0xFFFFB020.toInt()
    private const val RAIN = 0xFF4DA3FF.toInt()
    private const val BOLT = 0xFFFFC83D.toInt()
    private const val UP = 0xFF22A06B.toInt()
    private const val DOWN = 0xFFE5484D.toInt()

    private fun a(alpha: Float, c: Int) = ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)

    fun draw(x: Ctx): Boolean {
        when (x.conf.type) {
            "weather" -> weather(x)
            "aqi" -> air(x)
            "rates" -> rates(x)
            else -> return false
        }
        return true
    }

    private fun Ctx.fit(s: String, size: Float, maxW: Float): Float {
        val w = numW(s, size)
        return if (w > maxW && w > 0f) max(10f, size * maxW / w) else size
    }

    private fun Ctx.waiting(lbl: String, msg: String) {
        label(ttl(lbl), pad, pad + 11.5f)
        small(msg, pad, H - pad - 2f, maxW = W - 2 * pad)
    }

    private val f get() = D.tempF
    private fun deg(c: Double) = Live.deg(c, f)

    private fun hourLabel(t: String): String = runCatching {
        val h = LocalDateTime.parse(t).hour
        if (D.h24) String.format(Locale.US, "%02d", h) else when { h == 0 -> "12a"; h < 12 -> "${h}a"; h == 12 -> "12p"; else -> "${h - 12}p" }
    }.getOrDefault("")

    private fun dayLabel(d: String, i: Int): String = if (i == 0) "Today" else runCatching {
        LocalDate.parse(d).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }.getOrDefault("")

    /* ---------- weather icons ---------- */

    private fun Ctx.cloudPath(cx: Float, cy: Float, s: Float): Path {
        val p = Path()
        p.addCircle(px(cx - s * 0.17f), px(cy + s * 0.04f), px(s * 0.17f), Path.Direction.CW)
        val q = Path(); q.addCircle(px(cx + s * 0.03f), px(cy - s * 0.07f), px(s * 0.23f), Path.Direction.CW); p.op(q, Path.Op.UNION)
        val r = Path(); r.addCircle(px(cx + s * 0.23f), px(cy + s * 0.06f), px(s * 0.15f), Path.Direction.CW); p.op(r, Path.Op.UNION)
        val b = Path(); b.addRoundRect(RectF(px(cx - s * 0.34f), px(cy + s * 0.02f), px(cx + s * 0.38f), px(cy + s * 0.21f)), px(s * 0.095f), px(s * 0.095f), Path.Direction.CW)
        p.op(b, Path.Op.UNION)
        return p
    }

    private fun Ctx.sun(cx: Float, cy: Float, r: Float, col: Int) {
        circle(cx, cy, r, fill(col))
        val st = stroke(col, max(1.2f, r * 0.28f))
        for (i in 0 until 8) {
            val t = i * PI / 4
            line(cx + r * 1.45f * cos(t).toFloat(), cy + r * 1.45f * sin(t).toFloat(), cx + r * 1.95f * cos(t).toFloat(), cy + r * 1.95f * sin(t).toFloat(), st)
        }
    }

    private fun Ctx.moonShape(cx: Float, cy: Float, r: Float, col: Int) {
        val m = Path(); m.addCircle(px(cx), px(cy), px(r), Path.Direction.CW)
        val cut = Path(); cut.addCircle(px(cx + r * 0.55f), px(cy - r * 0.35f), px(r * 0.85f), Path.Direction.CW)
        m.op(cut, Path.Op.DIFFERENCE)
        c.drawPath(m, fill(col))
    }

    /** A weather icon for [code], in a box [s] wide centred on (cx, cy). [light] = drawn on a dark or coloured surface. */
    private fun Ctx.icon(code: Int, day: Boolean, cx: Float, cy: Float, s: Float, light: Boolean) {
        val cloud = if (light) 0xF2FFFFFF.toInt() else if (luma(p.bg or 0xFF000000.toInt()) > 0.5f) 0xFFBFC6D1.toInt() else 0xFFE3E7EE.toInt()
        val moonC = if (light) 0xFFF4F1E6.toInt() else if (luma(p.bg or 0xFF000000.toInt()) > 0.5f) 0xFF8E96A6.toInt() else 0xFFE9E4D2.toInt()
        fun sky(ox: Float, oy: Float, r: Float) = if (day) sun(cx + ox, cy + oy, r, SUN) else moonShape(cx + ox, cy + oy, r * 1.35f, moonC)
        when (code) {
            0 -> sky(0f, 0f, s * 0.2f)
            1, 2 -> { sky(-s * 0.13f, -s * 0.13f, s * 0.15f); c.drawPath(cloudPath(cx + s * 0.06f, cy + s * 0.08f, s * 0.82f), fill(cloud)) }
            3 -> c.drawPath(cloudPath(cx, cy, s), fill(cloud))
            45, 48 -> { val st = stroke(cloud, s * 0.075f); for (i in 0 until 3) line(cx - s * 0.36f + i * s * 0.06f, cy - s * 0.16f + i * s * 0.16f, cx + s * 0.36f - (2 - i) * s * 0.06f, cy - s * 0.16f + i * s * 0.16f, st) }
            in 71..77, 85, 86 -> {
                c.drawPath(cloudPath(cx, cy - s * 0.12f, s * 0.92f), fill(cloud))
                listOf(-0.2f, 0f, 0.2f).forEachIndexed { i, ox -> circle(cx + s * ox, cy + s * (0.27f + (i % 2) * 0.09f), s * 0.045f, fill(if (light) WHITE else RAIN)) }
            }
            in 95..99 -> {
                c.drawPath(cloudPath(cx, cy - s * 0.12f, s * 0.92f), fill(cloud))
                val b = Path().apply {
                    moveTo(px(cx + s * 0.02f), px(cy + s * 0.08f)); lineTo(px(cx - s * 0.1f), px(cy + s * 0.3f)); lineTo(px(cx + s * 0.0f), px(cy + s * 0.3f))
                    lineTo(px(cx - s * 0.06f), px(cy + s * 0.46f)); lineTo(px(cx + s * 0.12f), px(cy + s * 0.22f)); lineTo(px(cx + s * 0.02f), px(cy + s * 0.22f)); close()
                }
                c.drawPath(b, fill(BOLT))
            }
            in 51..67, in 80..82 -> {
                c.drawPath(cloudPath(cx, cy - s * 0.12f, s * 0.92f), fill(cloud))
                val st = stroke(if (light) a(0.9f, WHITE) else RAIN, s * 0.06f)
                listOf(-0.18f, 0f, 0.18f).forEach { ox -> line(cx + s * ox, cy + s * 0.22f, cx + s * (ox - 0.06f), cy + s * 0.40f, st) }
            }
            else -> c.drawPath(cloudPath(cx, cy, s), fill(cloud))
        }
    }

    /* ---------- the sky (Signature) ---------- */

    private fun skyCols(code: Int, day: Boolean): IntArray = when {
        code in 95..99 -> intArrayOf(0xFF2A3245.toInt(), 0xFF56617B.toInt())
        code in 51..67 || code in 80..82 -> if (day) intArrayOf(0xFF3E5368.toInt(), 0xFF7F92A3.toInt()) else intArrayOf(0xFF151C2B.toInt(), 0xFF323E55.toInt())
        code in 71..77 || code == 85 || code == 86 -> if (day) intArrayOf(0xFF7F97AE.toInt(), 0xFFD5E0EA.toInt()) else intArrayOf(0xFF26324A.toInt(), 0xFF56657E.toInt())
        code == 3 || code == 45 || code == 48 -> if (day) intArrayOf(0xFF5F7C94.toInt(), 0xFFAEBFCC.toInt()) else intArrayOf(0xFF181F30.toInt(), 0xFF39455E.toInt())
        else -> if (day) intArrayOf(0xFF2D7DE0.toInt(), 0xFF8EC9F9.toInt()) else intArrayOf(0xFF0A1330.toInt(), 0xFF25365F.toInt())
    }

    /** A full sky for the current weather: sun glow, moon and stars, clouds, rain or snow. */
    private fun Ctx.sky(w: Wx) {
        val day = w.day; val code = w.code
        val cols = skyCols(code, day)
        c.drawRect(0f, 0f, px(W), px(H), Paint().apply { shader = LinearGradient(0f, 0f, 0f, px(H), cols[0], cols[1], Shader.TileMode.CLAMP) })
        val clear = code <= 2
        if (day && clear) {
            val sx = W * 0.82f; val sy = H * 0.2f
            c.drawCircle(px(sx), px(sy), px(H * 0.6f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(px(sx), px(sy), px(H * 0.6f), 0x88FFE9A8.toInt(), 0x00FFE9A8, Shader.TileMode.CLAMP)
            })
            circle(sx, sy, min(W, H) * 0.12f, fill(0xFFFFE07A.toInt()))
        }
        if (!day) {
            val rnd = java.util.Random(11)
            val n = if (clear) 34 else 12
            for (i in 0 until n) circle(rnd.nextFloat() * W, rnd.nextFloat() * H * 0.75f, 0.5f + rnd.nextFloat() * 0.9f, fill(a(0.35f + rnd.nextFloat() * 0.5f, WHITE)))
            if (clear) moonShape(W * 0.8f, H * 0.2f, min(W, H) * 0.1f, 0xFFF4F0E2.toInt())
        }
        if (code in 1..3 || code >= 45) {
            val ca = if (code == 1 || code == 2) 0.55f else 0.38f
            c.drawPath(cloudPath(W * 0.72f, H * 0.32f, min(W, H) * 0.62f), fill(a(ca, WHITE)))
            c.drawPath(cloudPath(W * 0.25f, H * 0.18f, min(W, H) * 0.42f), fill(a(ca * 0.7f, WHITE)))
        }
        if (code in 51..67 || code in 80..82 || code in 95..99) {
            val st = stroke(a(0.28f, WHITE), 1.2f)
            val rnd = java.util.Random(5)
            for (i in 0 until 40) { val x0 = rnd.nextFloat() * (W + 20f); val y0 = rnd.nextFloat() * H; line(x0, y0, x0 - 4f, y0 + 10f, st) }
        }
        if (code in 71..77 || code == 85 || code == 86) {
            val rnd = java.util.Random(7)
            for (i in 0 until 36) circle(rnd.nextFloat() * W, rnd.nextFloat() * H, 1f + rnd.nextFloat() * 1.4f, fill(a(0.75f, WHITE)))
        }
        if (code in 95..99) {
            val b = Path().apply {
                moveTo(px(W * 0.7f), px(H * 0.3f)); lineTo(px(W * 0.62f), px(H * 0.52f)); lineTo(px(W * 0.68f), px(H * 0.52f))
                lineTo(px(W * 0.63f), px(H * 0.72f)); lineTo(px(W * 0.77f), px(H * 0.46f)); lineTo(px(W * 0.7f), px(H * 0.46f)); close()
            }
            c.drawPath(b, fill(a(0.85f, BOLT)))
        }
        // a soft shade at the bottom so the text always reads
        c.drawRect(0f, px(H * 0.45f), px(W), px(H), Paint().apply { shader = LinearGradient(0f, px(H * 0.45f), 0f, px(H), 0x00000000, 0x55000000, Shader.TileMode.CLAMP) })
    }

    /* ---------- Weather ---------- */

    private fun weather(x: Ctx) = with(x) {
        val pl = D.place ?: run { waiting("Weather", "Pick a city"); return@with }
        val w = D.wx ?: run { waiting(pl.name, "Getting the weather…"); return@with }
        val t = Live.today(w)
        val hl = if (t != null) "H ${deg(t.hi)}  L ${deg(t.lo)}" else ""
        val stale = System.currentTimeMillis() - w.at > 3 * 3_600_000L
        val city = ttl(pl.name) + if (stale) " · " + Live.ago(w.at) else ""
        when (conf.design) {
            "sky" -> {
                sky(w)
                text(city.uppercase(), pad, pad + 11.5f, 10.5f, labelFace(), a(0.85f, WHITE), 0.12f, maxW = W - 2 * pad)
                val tx = deg(w.temp)
                val sz = fit(tx, min(58f, H * 0.4f), W - 2 * pad)
                num(tx, pad - 2f, H - pad - 34f, sz, WHITE)
                text(Live.cond(w.code), pad, H - pad - 16f, 13f, uiFace(600), WHITE, maxW = W - 2 * pad)
                text(hl, pad, H - pad - 1f, 11.5f, uiFace(500), a(0.82f, WHITE), maxW = W - 2 * pad)
            }
            "skyhours" -> {
                sky(w)
                text(city.uppercase(), pad, pad + 11.5f, 10.5f, labelFace(), a(0.85f, WHITE), 0.12f, maxW = W * 0.6f)
                val tx = deg(w.temp)
                num(tx, pad - 2f, pad + 22f + cap(46f), 46f, WHITE)
                text(Live.cond(w.code) + (if (hl.isNotEmpty()) "  ·  $hl" else ""), pad, pad + 22f + cap(46f) + 18f, 12.5f, uiFace(600), WHITE, maxW = W - 2 * pad)
                // the next hours as a line across the sky
                val hrs = Live.nextHours(w, 8)
                if (hrs.size >= 2) {
                    val x0 = pad + 8f; val x1 = W - pad - 8f
                    val yTop = H - pad - 52f; val yBot = H - pad - 22f
                    val lo = hrs.minOf { it.temp }; val hi = hrs.maxOf { it.temp }
                    val span = max(1.0, hi - lo)
                    val pts = hrs.mapIndexed { i, h -> (x0 + (x1 - x0) * i / (hrs.size - 1)) to (yBot - ((h.temp - lo) / span).toFloat() * (yBot - yTop)) }
                    val path = Path()
                    pts.forEachIndexed { i, (xx, yy) ->
                        if (i == 0) path.moveTo(px(xx), px(yy)) else {
                            val (px0, py0) = pts[i - 1]; val mx = (px0 + xx) / 2
                            path.cubicTo(px(mx), px(py0), px(mx), px(yy), px(xx), px(yy))
                        }
                    }
                    c.drawPath(path, stroke(a(0.9f, WHITE), 2f))
                    pts.forEachIndexed { i, (xx, yy) ->
                        circle(xx, yy, 2.6f, fill(WHITE))
                        text(deg(hrs[i].temp), xx, yy - 6f, 10.5f, uiFace(600), WHITE, align = C)
                        text(if (i == 0) "Now" else hourLabel(hrs[i].t), xx, H - pad - 4f, 10f, uiFace(500), a(0.8f, WHITE), align = C)
                    }
                }
            }
            "skytall" -> {
                sky(w)
                text(city.uppercase(), pad, pad + 11.5f, 10.5f, labelFace(), a(0.85f, WHITE), 0.12f, maxW = W - 2 * pad)
                val tx = deg(w.temp)
                val sz = fit(tx, 50f, W - 2 * pad)
                num(tx, pad - 2f, pad + 22f + cap(sz), sz, WHITE)
                text(Live.cond(w.code), pad, pad + 22f + cap(sz) + 18f, 13f, uiFace(600), WHITE, maxW = W - 2 * pad)
                text(hl, pad, pad + 22f + cap(sz) + 33f, 11.5f, uiFace(500), a(0.82f, WHITE), maxW = W - 2 * pad)
                // the next hours, one per row
                val top = pad + 22f + cap(sz) + 46f
                val rows = ((H - pad - top) / 24f).toInt().coerceIn(1, 6)
                val rh = (H - pad - top) / rows
                Live.nextHours(w, rows + 1).drop(1).take(rows).forEachIndexed { i, h ->
                    val y = top + rh * i
                    line(pad, y, W - pad, y, stroke(a(0.18f, WHITE), 1f))
                    text(hourLabel(h.t), pad, y + rh / 2 + 4.5f, 12f, uiFace(500), a(0.85f, WHITE))
                    icon(h.code, isDayHour(h.t, w), W / 2 + 4f, y + rh / 2, 18f, true)
                    text(deg(h.temp), W - pad, y + rh / 2 + 4.5f, 13f, uiFace(600), WHITE, align = R)
                }
            }
            "hours" -> {
                val lw = 100f
                label(city, pad + 2f, pad + 10f, lw)
                val tx = deg(w.temp)
                val sz = fit(tx, 30f, lw - 34f)
                num(tx, pad + 2f, H - pad - 6f, sz, p.fg)
                icon(w.code, w.day, pad + 2f + numW(tx, sz) + 18f, H - pad - 6f - cap(sz) / 2, 26f, false)
                val hrs = Live.nextHours(w, 6).drop(1).take(5)
                val x0 = pad + lw + 12f; val cw = (W - pad - x0) / max(1, hrs.size)
                hrs.forEachIndexed { i, h ->
                    val cx = x0 + cw * i + cw / 2
                    text(hourLabel(h.t), cx, pad + 10f, 10.5f, uiFace(500), p.mut, align = C)
                    icon(h.code, isDayHour(h.t, w), cx, H / 2 + 1f, 22f, false)
                    text(deg(h.temp), cx, H - pad - 4f, 12.5f, uiFace(600), p.fg, align = C)
                }
            }
            "week" -> {
                label(city, pad, pad + 11.5f, W - 2 * pad)
                val tx = deg(w.temp)
                icon(w.code, w.day, pad + 22f, pad + 46f, 44f, false)
                num(tx, pad + 54f, pad + 46f + cap(38f) / 2, 38f, p.fg)
                val rx = pad + 54f + numW(tx, 38f) + 12f
                text(Live.cond(w.code), rx, pad + 40f, 14f, uiFace(600), p.fg, maxW = W - pad - rx)
                small("Feels ${deg(w.feels)} · $hl", rx, pad + 57f, size = 11.5f, maxW = W - pad - rx)
                val days = Live.nextDays(w, 5)
                val cw = (W - 2 * pad) / max(1, days.size)
                val y0 = H - pad - 62f
                line(pad, y0 - 8f, W - pad, y0 - 8f, stroke(p.soft, 1f))
                days.forEachIndexed { i, d ->
                    val cx = pad + cw * i + cw / 2
                    text(dayLabel(d.d, i), cx, y0 + 6f, 11f, uiFace(600), if (i == 0) p.fg else p.mut, align = C)
                    icon(d.code, true, cx, y0 + 26f, 22f, false)
                    text(deg(d.hi), cx, y0 + 48f, 12.5f, uiFace(600), p.fg, align = C)
                    text(deg(d.lo), cx, y0 + 61f, 11f, uiFace(500), p.mut, align = C)
                }
            }
            else -> { // "now"
                label(city, pad, pad + 11.5f, W - 2 * pad - 40f)
                icon(w.code, w.day, W - pad - 18f, pad + 18f, 38f, false)
                val tx = deg(w.temp)
                val sz = fit(tx, min(54f, H * 0.36f), W - 2 * pad)
                num(tx, pad - 1f, H - pad - 34f, sz, p.fg)
                text(Live.cond(w.code), pad, H - pad - 16f, 13f, uiFace(600), p.fg, maxW = W - 2 * pad)
                small(hl, pad, H - pad - 1f, size = 11.5f, maxW = W - 2 * pad)
            }
        }
    }

    private fun isDayHour(t: String, w: Wx): Boolean {
        val h = runCatching { LocalDateTime.parse(t).hour }.getOrDefault(12)
        val rise = w.rise?.let { runCatching { LocalDateTime.parse(it).hour }.getOrNull() } ?: 6
        val set = w.set?.let { runCatching { LocalDateTime.parse(it).hour }.getOrNull() } ?: 18
        return h in rise..set
    }

    /* ---------- Air quality ---------- */

    private val BANDS = listOf(50, 100, 150, 200, 300, 400)

    private fun air(x: Ctx) = with(x) {
        val pl = D.place ?: run { waiting("Air quality", "Pick a city"); return@with }
        val q = D.aq ?: run { waiting("Air · ${pl.name}", "Checking the air…"); return@with }
        val (name, col) = Live.aqiBand(q.aqi)
        val city = ttl("Air · ${pl.name}")
        when (conf.design) {
            "strip" -> {
                label(city, pad + 2f, pad + 10f, 150f)
                val n = q.aqi.toString()
                num(n, pad + 2f, H - pad - 5f, 30f, col)
                val nw = numW(n, 30f)
                text(name, pad + 2f + nw + 8f, H - pad - 16f, 13f, uiFace(600), p.fg, maxW = 150f - nw)
                small("US AQI", pad + 2f + nw + 8f, H - pad - 3f, size = 10.5f)
                // the scale, with you on it
                val bx = W * 0.52f; val bw = W - pad - bx; val by = H / 2 - 3f
                bands(bx, by, bw, 6f, q.aqi)
                small("PM2.5 ${q.pm25.roundToInt()} · PM10 ${q.pm10.roundToInt()} µg/m³", bx, by + 24f, size = 10.5f, maxW = bw)
                small(Live.aqiAdvice(q.aqi), bx, by - 10f, size = 10.5f, maxW = bw)
            }
            else -> { // "gauge"
                label(city, pad, pad + 11.5f, W - 2 * pad)
                val r = min((W - 2 * pad) / 2 - 6f, (H - pad - 52f - (pad + 20f)) * 0.95f)
                val cx = W / 2; val cy = pad + 24f + r + 4f
                val oval = RectF(px(cx - r), px(cy - r), px(cx + r), px(cy + r))
                val sw = max(6f, r * 0.16f)
                val segs = 6; val gap = 3f
                for (i in 0 until segs) {
                    val (_, sc) = Live.aqiBand(if (i == 0) 25 else BANDS[i - 1] + 1)
                    val st = stroke(if (q.aqi > (if (i == 0) 0 else BANDS[i - 1])) sc else a(0.25f, sc), sw); st.strokeCap = Paint.Cap.BUTT
                    c.drawArc(oval, 180f + i * 30f + gap / 2, 30f - gap, false, st)
                }
                // where today sits
                val pos = when { q.aqi <= 300 -> q.aqi / 300f * 5f; else -> 5f + min(1f, (q.aqi - 300) / 200f) } / 6f
                val ang = PI + PI * pos
                val mx = cx + r * cos(ang).toFloat(); val my = cy + r * sin(ang).toFloat()
                circle(mx, my, sw * 0.75f, fill(p.bg or 0xFF000000.toInt()))
                circle(mx, my, sw * 0.5f, fill(p.fg))
                val n = q.aqi.toString()
                val sz = fit(n, min(34f, r * 0.7f), r * 1.3f)
                num(n, cx, cy - 2f, sz, p.fg, C)
                text(name, cx, H - pad - 16f, 13f, uiFace(600), col, align = C, maxW = W - 2 * pad)
                small("PM2.5 ${q.pm25.roundToInt()} µg/m³", cx, H - pad - 1f, size = 11f, align = C, maxW = W - 2 * pad)
            }
        }
    }

    /** The AQI scale as coloured steps, with a marker at [aqi]. */
    private fun Ctx.bands(x0: Float, y: Float, w: Float, h: Float, aqi: Int) {
        val segs = 6; val gap = 2f
        val sw = (w - gap * (segs - 1)) / segs
        for (i in 0 until segs) {
            val (_, sc) = Live.aqiBand(if (i == 0) 25 else BANDS[i - 1] + 1)
            rrect(x0 + i * (sw + gap), y, sw, h, h / 2, fill(sc))
        }
        val pos = (if (aqi <= 300) aqi / 300f * 5f else 5f + min(1f, (aqi - 300) / 200f)) / 6f
        val mx = x0 + w * pos.coerceIn(0f, 1f)
        circle(mx, y + h / 2, h * 1.1f, fill(p.bg or 0xFF000000.toInt()))
        circle(mx, y + h / 2, h * 0.75f, fill(p.fg))
    }

    /* ---------- Rates ---------- */

    private fun rates(x: Ctx) = with(x) {
        val keys = D.rates
        if (keys.isEmpty()) { waiting("Rates", "Pick what to follow"); return@with }
        val cur = D.cur
        val newest = keys.mapNotNull { D.quotes[it]?.at }.maxOrNull()
        when (conf.design) {
            "ticker" -> {
                val items = keys.take(3)
                val cw = (W - 2 * pad) / items.size
                items.forEachIndexed { i, k ->
                    val q = D.quotes[k]
                    val x0 = pad + cw * i + (if (i > 0) 8f else 2f)
                    if (i > 0) line(pad + cw * i, pad + 4f, pad + cw * i, H - pad - 4f, stroke(p.soft, 1f))
                    text(Live.quoteName(k).uppercase(), x0, pad + 10f, 10f, labelFace(), p.mut, 0.1f, maxW = cw - 12f)
                    if (q == null) { small("…", x0, H / 2 + 8f); return@forEachIndexed }
                    val v = Live.fmt(k, q.v, cur)
                    text(v, x0, H / 2 + 8f, fitText(v, 17f, cw - 12f), uiFace(600), p.fg, -0.01f)
                    q.ch?.let { ch -> text(Live.pct(ch), x0, H - pad - 3f, 10.5f, uiFace(600), if (ch >= 0) UP else DOWN, maxW = cw - 12f) }
                }
            }
            "column" -> {
                label(ttl("Rates"), pad, pad + 11.5f, W - 2 * pad)
                val rows = keys.take(4)
                val top = pad + 22f
                val rh = (H - pad - top) / max(1, rows.size)
                rows.forEachIndexed { i, k ->
                    val q = D.quotes[k]
                    val y = top + rh * i
                    if (i > 0) line(pad, y, W - pad, y, stroke(p.soft, 1f))
                    text(Live.quoteName(k).uppercase(), pad, y + 15f, 10f, labelFace(), p.mut, 0.1f, maxW = W - 2 * pad)
                    if (q == null) { small("…", pad, y + 36f); return@forEachIndexed }
                    val v = Live.fmt(k, q.v, cur)
                    text(v, pad, y + 35f, fitText(v, 18f, W - 2 * pad), uiFace(600), p.fg, -0.01f)
                    q.ch?.let { ch -> if (rh >= 54f) text(Live.pct(ch), pad, y + 50f, 10.5f, uiFace(600), if (ch >= 0) UP else DOWN) }
                }
            }
            "one" -> {
                val k = D.rates.first(); val q = D.quotes[k]
                label(ttl(Live.quoteName(k)), pad, pad + 11.5f, W - 2 * pad)
                small(Live.quoteSub(k, cur), pad, pad + 28f, size = 11f, maxW = W - 2 * pad)
                if (q == null) { small("Getting the latest…", pad, H - pad - 2f); return@with }
                val v = Live.fmt(k, q.v, cur)
                val sz = fitText(v, 30f, W - 2 * pad)
                text(v, pad, H - pad - 24f, sz, uiFace(600), p.fg, -0.02f)
                q.ch?.let { ch ->
                    val s = Live.pct(ch); val col = if (ch >= 0) UP else DOWN
                    val tw = measure(s, 11.5f, uiFace(600))
                    rrect(pad, H - pad - 17f, tw + 14f, 19f, 9.5f, fill(a(0.14f, col)))
                    text(s, pad + 7f, H - pad - 3.5f, 11.5f, uiFace(600), col)
                }
            }
            else -> { // "list"
                label(ttl("Rates"), pad, pad + 11.5f, W - 2 * pad - 90f)
                if (newest != null) small(Live.ago(newest), W - pad, pad + 11.5f, size = 11f, align = R)
                val rows = keys.take(4)
                val top = pad + 24f
                val rh = min(36f, (H - pad - top) / max(1, rows.size))
                rows.forEachIndexed { i, k ->
                    val q = D.quotes[k]
                    val y = top + rh * i
                    if (i > 0) line(pad, y, W - pad, y, stroke(p.soft, 1f))
                    // a second line only when the rows have room for it
                    val roomy = rh >= 34f
                    val base = if (roomy) y + rh / 2 + 1f else y + rh / 2 + 5f
                    t2(Live.quoteName(k), pad, base, 14f, maxW = W * 0.4f)
                    if (roomy) small(Live.quoteSub(k, cur), pad, base + 13f, size = 10.5f, maxW = W * 0.42f)
                    val vb = if (roomy) base + 5f else base
                    if (q == null) { small("…", W - pad, vb, align = R); return@forEachIndexed }
                    val v = Live.fmt(k, q.v, cur)
                    text(v, W - pad - 74f, vb, fitText(v, 16f, W * 0.36f), uiFace(600), p.fg, -0.01f, R)
                    q.ch?.let { ch -> text(Live.pct(ch), W - pad, vb, 11f, uiFace(600), if (ch >= 0) UP else DOWN, align = R) }
                }
            }
        }
    }

    private fun Ctx.fitText(s: String, size: Float, maxW: Float): Float {
        val w = measure(s, size, uiFace(600), -0.01f)
        return if (w > maxW && w > 0f) max(9f, size * maxW / w) else size
    }
}
