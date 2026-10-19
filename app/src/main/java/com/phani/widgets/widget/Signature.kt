package com.phani.widgets.widget

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.PathParser
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.DateItem
import com.phani.widgets.core.DateNext
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.dateNext
import com.phani.widgets.core.cnt
import com.phani.widgets.core.fastState
import com.phani.widgets.core.pace
import com.phani.widgets.core.streak
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Signature designs: widgets drawn as real objects (a boarding pass, a tear-off calendar page,
 * a dot-matrix display, a word clock, a night sky). Each reads at a glance and looks like nothing else.
 */
object Signature {
    private val D get() = AppStore.view
    private val L = Paint.Align.LEFT
    private val R = Paint.Align.RIGHT
    private val C = Paint.Align.CENTER

    private fun a(alpha: Float, c: Int) = ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24) or (c and 0xFFFFFF)
    private val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    /** Designs that draw their own shape instead of the usual card (a polaroid floating on the wallpaper). */
    fun freeform(c: com.phani.widgets.core.WConf) = c.type == "note" && c.design == "polaroid"

    /** Draws the design if it's a signature one; returns false otherwise. */
    fun draw(x: Ctx): Boolean {
        when ("${x.conf.type}/${x.conf.design}") {
            "countdown/pass" -> pass(x)
            "countdown/tear" -> tear(x, countdown = true)
            "countdown/led" -> led(x)
            "today/tear" -> tear(x, countdown = false)
            "clock/word" -> wordClock(x)
            "moon/night" -> nightMoon(x)
            "countdown/photo", "countdown/photowide" -> photoCountdown(x)
            "today/photo" -> photoDate(x)
            "note/polaroid" -> polaroid(x)
            "counter/liquid" -> liquid(x)
            "streak/flame" -> flame(x)
            "fast/arc" -> sunArc(x)
            "pace/climb" -> summit(x)
            else -> return false
        }
        return true
    }

    /* ---------- shared bits ---------- */

    private fun Ctx.date(): Pair<DateItem, DateNext>? {
        val d = D.dates.firstOrNull { it.id == conf.ref } ?: D.dates.firstOrNull() ?: return null
        return d to dateNext(d)
    }

    private fun Ctx.nothing(msg: String) {
        label(ttl("Countdown"), pad, pad + 11.5f)
        small(msg, pad, H - pad - 2f)
    }

    private fun Ctx.fit(s: String, size: Float, maxW: Float): Float {
        val w = numW(s, size)
        return if (w > maxW && w > 0f) max(10f, size * maxW / w) else size
    }

    /** A Material "flight" glyph (Apache 2.0), rotated to fly right. */
    private fun Ctx.plane(cx: Float, cy: Float, size: Float, color: Int) {
        val path = PathParser.createPathFromPathData("M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z")
        val s = px(size) / 24f
        val m = Matrix()
        m.setRotate(90f, 12f, 12f); m.postScale(s, s); m.postTranslate(px(cx) - px(size) / 2, px(cy) - px(size) / 2)
        path.transform(m)
        c.drawPath(path, fill(color))
    }

    private fun Ctx.vdash(xx: Float, y1: Float, y2: Float, color: Int) {
        val pt = stroke(color, 1.2f); pt.strokeCap = Paint.Cap.BUTT; pt.pathEffect = DashPathEffect(floatArrayOf(px(3f), px(3.5f)), 0f)
        c.drawLine(px(xx), px(y1), px(xx), px(y2), pt); pt.pathEffect = null
    }

    /* ---------- Boarding pass ---------- */

    private fun pass(x: Ctx) = with(x) {
        val dn = date() ?: run { nothing("Add a trip or date"); return@with }
        val (d, n) = dn
        val name = d.name.ifBlank { "Your trip" }
        val code = name.filter { it.isLetter() }.uppercase().take(3).padEnd(3, 'X')
        val stubX = W * 0.68f
        val m = 18f

        // perforation and the two punched notches (really see-through)
        vdash(stubX, 14f, H - 14f, a(0.35f, p.fg))
        c.drawCircle(px(stubX), 0f, px(10f), clear)
        c.drawCircle(px(stubX), px(H), px(10f), clear)

        // left: route
        label(ttl("Boarding pass"), m, m + 9f, stubX - 2 * m - 20f)
        plane(stubX - m - 8f, m + 5f, 15f, p.acc)
        val codeSz = min(34f, (H - 70f) * 0.55f)
        val cy = m + 22f + cap(codeSz) + 4f
        num("NOW", m, cy, codeSz, p.fg)
        num(code, stubX - m, cy, codeSz, p.fg, R)
        val l1 = m + numW("NOW", codeSz) + 10f
        val l2 = stubX - m - numW(code, codeSz) - 10f
        if (l2 - l1 > 30f) {
            val mid = (l1 + l2) / 2
            val pt = stroke(a(0.4f, p.fg), 1.2f); pt.pathEffect = DashPathEffect(floatArrayOf(px(2f), px(3f)), 0f)
            c.drawLine(px(l1), px(cy - cap(codeSz) / 2), px(mid - 10f), px(cy - cap(codeSz) / 2), pt)
            c.drawLine(px(mid + 10f), px(cy - cap(codeSz) / 2), px(l2), px(cy - cap(codeSz) / 2), pt)
            pt.pathEffect = null
            plane(mid, cy - cap(codeSz) / 2, 14f, p.fg)
        }
        small("Today", m, cy + 16f, size = 11f)
        small(name, stubX - m, cy + 16f, size = 11f, align = R, maxW = stubX - 2 * m - 50f)

        // fields
        val fy = H - m
        val colW = (stubX - 2 * m) / 3
        listOf("Date" to Fmt.short(n.d), "Day" to n.d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), "Repeats" to if (d.yearly) "Yearly" else "Once")
            .forEachIndexed { i, (k, v) ->
                label(k, m + colW * i, fy - 17f, colW - 4f)
                t2(v, m + colW * i, fy, 14f, maxW = colW - 4f)
            }

        // stub: the big number
        val sx = stubX + 14f; val sw = W - stubX - 28f
        label("Days", sx, m + 9f, sw)
        val big = if (n.days == 0) "0" else n.days.toString()
        val bs = fit(big, min(58f, H * 0.36f), sw)
        num(big, sx + sw / 2, H * 0.58f, bs, if (n.days <= 7) p.acc else p.fg, C)
        small(if (n.days == 0) "today" else "to go", sx + sw / 2, H * 0.58f + 15f, size = 11f, align = C)
        // barcode
        var bx = sx; var seed = name.hashCode()
        val by = H - m - 14f
        while (bx < sx + sw - 1f) {
            seed = seed * 1103515245 + 12345
            val bw = 0.8f + ((seed ushr 16) and 3) * 0.7f
            rrect(bx, by, min(bw, sx + sw - bx), 14f, 0f, fill(a(0.85f, p.fg)))
            bx += bw + 1.1f + ((seed ushr 20) and 1)
        }
    }

    /* ---------- Tear-off calendar page ---------- */

    private fun tear(x: Ctx, countdown: Boolean) = with(x) {
        val today = LocalDate.now()
        val dn = if (countdown) date() else null
        if (countdown && dn == null) { nothing("Add a date"); return@with }
        val bandH = H * 0.30f
        // the coloured header band, with binding holes and rings
        rrect(0f, 0f, W, bandH, 0f, fill(p.acc))
        rrect(0f, bandH - 1.2f, W, 1.2f, 0f, fill(a(0.18f, 0)))
        listOf(W * 0.3f, W * 0.7f).forEach { rx ->
            circle(rx, bandH * 0.38f, 3.6f, fill(a(0.35f, 0)))
            rrect(rx - 1.8f, -3f, 3.6f, bandH * 0.38f + 3f, 1.8f, fill(a(0.92f, 0xFFF4F2EC.toInt())))
        }
        val head = if (countdown) ttl(dn!!.first.name.ifBlank { "Days until" }) else today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        text(head.uppercase(), W / 2, bandH * 0.84f, 10.5f, labelFace(), p.accOnBtn, 0.14f, C, W - 2 * pad)

        // the page
        val bigS = if (countdown) dn!!.second.days.toString() else today.dayOfMonth.toString()
        val avail = H - bandH - 30f
        val sz = fit(bigS, min(avail / 0.78f, 78f), W - 2 * pad)
        val base = bandH + 8f + (avail + cap(sz)) / 2
        num(bigS, W / 2, base, sz, p.fg, C)
        val sub = if (countdown) (if (dn!!.second.days == 1) "day · " else "days · ") + Fmt.short(dn.second.d)
        else today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        small(sub, W / 2, H - pad + 1f, p.mut, 11.5f, C, W - 2 * pad - 20f, 500)

        // a curled bottom-right corner
        val cs = 20f
        val cut = Path().apply { moveTo(px(W - cs), px(H)); lineTo(px(W), px(H - cs)); lineTo(px(W), px(H)); close() }
        c.drawPath(cut, clear)
        val flap = Path().apply { moveTo(px(W - cs), px(H)); lineTo(px(W), px(H - cs)); lineTo(px(W - cs), px(H - cs)); close() }
        val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(px(W - cs), px(H - cs), px(W - cs / 2), px(H - cs / 2), p.soft, a(1f, p.bg or 0xFF000000.toInt()), Shader.TileMode.CLAMP)
            setShadowLayer(px(3f), -px(1f), -px(1f), a(0.25f, 0))
        }
        c.drawPath(flap, fp)
    }

    /* ---------- Dot-matrix countdown ---------- */

    private val GLYPHS = mapOf(
        '0' to "01110100011001110101110011000101110", '1' to "00100011000010000100001000010001110",
        '2' to "01110100010000100010001000100011111", '3' to "11111000100010000010000011000101110",
        '4' to "00010001100101010010111110001000010", '5' to "11111100001111000001000011000101110",
        '6' to "00110010001000011110100011000101110", '7' to "11111000010001000100010000100001000",
        '8' to "01110100011000101110100011000101110", '9' to "01110100011000101111000010001001100",
    )

    /** Draws digits as a 5×7 LED matrix; returns the width used. */
    private fun Ctx.ledDigits(s: String, right: Float, top: Float, pitch: Float, on: Int, off: Int) : Float {
        val gap = pitch * 1.2f
        val w = s.length * 5 * pitch + (s.length - 1) * gap
        var x0 = right - w
        val r = pitch * 0.4f
        val glow = luma(p.bg or 0xFF000000.toInt()) < 0.45f
        s.forEach { ch ->
            val g = GLYPHS[ch] ?: GLYPHS.getValue('0')
            for (i in 0 until 35) {
                val cx = x0 + (i % 5) * pitch + pitch / 2
                val cy = top + (i / 5) * pitch + pitch / 2
                if (g[i] == '1') {
                    if (glow) circle(cx, cy, r * 1.55f, fill(a(0.22f, on)))
                    circle(cx, cy, r, fill(on))
                } else circle(cx, cy, r * 0.75f, fill(off))
            }
            x0 += 5 * pitch + gap
        }
        return w
    }

    private fun led(x: Ctx) = with(x) {
        val dn = date() ?: run { nothing("Add a date"); return@with }
        val (d, n) = dn
        val s = n.days.toString()
        var pitch = (H - 22f) / 7f
        val maxW = W * 0.58f
        val need = s.length * 5 * pitch + (s.length - 1) * pitch * 1.2f
        if (need > maxW) pitch *= maxW / need
        val top = (H - 7 * pitch) / 2
        val w = ledDigits(s, W - 18f, top, pitch, p.acc, a(0.10f, p.fg))
        val lw = W - 36f - w - 14f
        label(ttl(if (n.days == 1) "Day to" else "Days to"), 18f, H / 2 - 11f, lw)
        t2(d.name.ifBlank { "Your day" }, 18f, H / 2 + 7f, 17f, maxW = lw)
        small(Fmt.full(n.d), 18f, H / 2 + 23f, maxW = lw)
    }

    /* ---------- Word clock ---------- */

    private val GRID = listOf(
        "ITLISASAMPM", "ACQUARTERDC", "TWENTYFIVEX", "HALFSTENFTO", "PASTERUNINE",
        "ONESIXTHREE", "FOURFIVETWO", "EIGHTELEVEN", "SEVENTWELVE", "TENSEOCLOCK",
    )
    private val WORDS = mapOf(
        "IT" to Triple(0, 0, 1), "IS" to Triple(0, 3, 4), "A" to Triple(1, 0, 0), "QUARTER" to Triple(1, 2, 8),
        "TWENTY" to Triple(2, 0, 5), "FIVE_M" to Triple(2, 6, 9), "HALF" to Triple(3, 0, 3), "TEN_M" to Triple(3, 5, 7),
        "TO" to Triple(3, 9, 10), "PAST" to Triple(4, 0, 3), "OCLOCK" to Triple(9, 5, 10),
    )
    private val HOURS = listOf(
        Triple(8, 5, 10), Triple(5, 0, 2), Triple(6, 8, 10), Triple(5, 6, 10), Triple(6, 0, 3), Triple(6, 4, 7),
        Triple(5, 3, 5), Triple(8, 0, 4), Triple(7, 0, 4), Triple(4, 7, 10), Triple(9, 0, 2), Triple(7, 5, 10),
    ) // 12, 1, 2 … 11

    private fun litCells(t: LocalTime): Set<Pair<Int, Int>> {
        val m5 = t.minute / 5 * 5
        var h = t.hour % 12
        val words = mutableListOf("IT", "IS")
        when (m5) {
            0 -> words += "OCLOCK"
            5 -> words += listOf("FIVE_M", "PAST"); 10 -> words += listOf("TEN_M", "PAST")
            15 -> words += listOf("A", "QUARTER", "PAST"); 20 -> words += listOf("TWENTY", "PAST")
            25 -> words += listOf("TWENTY", "FIVE_M", "PAST"); 30 -> words += listOf("HALF", "PAST")
            35 -> words += listOf("TWENTY", "FIVE_M", "TO"); 40 -> words += listOf("TWENTY", "TO")
            45 -> words += listOf("A", "QUARTER", "TO"); 50 -> words += listOf("TEN_M", "TO")
            55 -> words += listOf("FIVE_M", "TO")
        }
        if (m5 > 30) h = (h + 1) % 12
        val cells = mutableSetOf<Pair<Int, Int>>()
        (words.mapNotNull { WORDS[it] } + HOURS[h]).forEach { (r, a, b) -> for (c in a..b) cells += r to c }
        return cells
    }

    private fun wordClock(x: Ctx) = with(x) {
        val now = LocalTime.now()
        val lit = litCells(now)
        val inset = pad - 2f
        val cw = (W - 2 * inset) / 11f
        val ch = (H - 2 * inset - 4f) / 10f
        val sz = min(cw * 0.86f, ch * 0.8f)
        val face = if (conf.font == "grotesk") uiFace(600) else numFace()
        GRID.forEachIndexed { r, row ->
            row.forEachIndexed { col, letter ->
                val on = (r to col) in lit
                text(letter.toString(), inset + cw * col + cw / 2, inset + 2f + ch * r + ch / 2 + sz * 0.36f, sz, face,
                    if (on) p.fg else a(0.13f, p.fg), 0f, C)
            }
        }
        // the extra minutes, as dots in the corners
        val extra = now.minute % 5
        listOf(4f to 4f, W - 4f to 4f, W - 4f to H - 4f, 4f to H - 4f).forEachIndexed { i, (dx, dy) ->
            circle(dx + (if (dx < W / 2) 3f else -3f), dy + (if (dy < H / 2) 3f else -3f), 1.8f, fill(if (i < extra) p.acc else a(0.12f, p.fg)))
        }
    }

    /* ---------- Night-sky moon ---------- */

    private fun moonAge(): Double {
        val days = (System.currentTimeMillis() / 86_400_000.0) - 10_962.6
        val f = (days / 29.530588853) % 1.0
        return if (f < 0) f + 1 else f
    }

    private fun phaseName(a: Double) = when {
        a < 0.03 || a > 0.97 -> "New moon"; a < 0.22 -> "Waxing crescent"; a < 0.28 -> "First quarter"
        a < 0.47 -> "Waxing gibbous"; a < 0.53 -> "Full moon"; a < 0.72 -> "Waning gibbous"; a < 0.78 -> "Last quarter"
        else -> "Waning crescent"
    }

    private fun nightMoon(x: Ctx) = with(x) {
        val skyTop = 0xFF0A1030.toInt(); val skyBot = 0xFF27305E.toInt()
        c.drawRect(0f, 0f, px(W), px(H), Paint().apply { shader = LinearGradient(0f, 0f, 0f, px(H), skyTop, skyBot, Shader.TileMode.CLAMP) })
        // stars
        var seed = 7
        repeat(34) {
            seed = seed * 1103515245 + 12345; val sx = ((seed ushr 8) and 0xFFFF) / 65535f * W
            seed = seed * 1103515245 + 12345; val sy = ((seed ushr 8) and 0xFFFF) / 65535f * H * 0.8f
            seed = seed * 1103515245 + 12345; val sr = 0.4f + ((seed ushr 12) and 7) / 7f * 0.9f
            if (sy > pad + 16f || (sx > pad + 50f && sx < W - pad - 40f)) circle(sx, sy, sr, fill(a(0.35f + sr * 0.4f, 0xFFFFFF)))
        }
        val age = moonAge()
        val r = min(W, H) * 0.24f
        val cx = W / 2; val cy = H * 0.45f
        // glow
        c.drawCircle(px(cx), px(cy), px(r * 1.7f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(px(cx), px(cy), px(r * 1.7f), a(0.22f, 0xFFF3EBD0.toInt()), 0, Shader.TileMode.CLAMP)
        })
        // the moon itself, shaded like a sphere, with seas and craters
        c.drawCircle(px(cx), px(cy), px(r), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(px(cx - r * 0.35f), px(cy - r * 0.35f), px(r * 1.5f), 0xFFFBF8EE.toInt(), 0xFFB9B3A1.toInt(), Shader.TileMode.CLAMP)
        })
        val sea = a(0.16f, 0xFF4D4838.toInt())
        listOf(Triple(-0.30f, -0.22f, 0.30f), Triple(0.18f, -0.30f, 0.22f), Triple(0.05f, 0.18f, 0.26f), Triple(-0.38f, 0.28f, 0.16f), Triple(0.40f, 0.10f, 0.14f))
            .forEach { (ox, oy, rr) -> circle(cx + ox * r, cy + oy * r, rr * r, fill(sea)) }
        listOf(Triple(0.30f, 0.45f, 0.07f), Triple(-0.10f, -0.55f, 0.06f), Triple(-0.55f, -0.05f, 0.05f), Triple(0.55f, -0.35f, 0.05f))
            .forEach { (ox, oy, rr) -> circle(cx + ox * r, cy + oy * r, rr * r, stroke(a(0.22f, 0xFF4D4838.toInt()), 0.8f)) }
        // the shadow, with a soft terminator
        val k = cos(2 * PI * age).toFloat()
        val path = Path()
        val steps = 48
        for (i in 0..steps) {
            val yy = -r + 2 * r * i / steps
            val w = sqrt(max(0f, r * r - yy * yy))
            val xx = if (age < 0.5) w * k else -w * k
            if (i == 0) path.moveTo(px(cx + xx), px(cy + yy)) else path.lineTo(px(cx + xx), px(cy + yy))
        }
        for (i in steps downTo 0) {
            val yy = -r + 2 * r * i / steps
            val w = sqrt(max(0f, r * r - yy * yy))
            path.lineTo(px(cx + if (age < 0.5) -w else w), px(cy + yy))
        }
        path.close()
        if (abs(age - 0.5) > 0.015) c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = a(0.86f, 0xFF0B1230.toInt()); maskFilter = BlurMaskFilter(px(r * 0.05f), BlurMaskFilter.Blur.NORMAL)
        })
        val illum = ((1 - cos(2 * PI * age)) / 2 * 100).roundToInt()
        val white = 0xFFF4F1E8.toInt()
        if (showLabel) text(ttl("Moon").uppercase(), pad, pad + 9f, 10.5f, labelFace(), a(0.7f, white), 0.12f, L)
        text("$illum%", W - pad, pad + 9f, 12f, uiFace(500), a(0.7f, white), 0f, R)
        val toFull = (((0.5 - age) % 1.0 + 1.0) % 1.0 * 29.53).roundToInt()
        text(phaseName(age), W / 2, H - pad - 16f, 14f, uiFace(600), white, -0.01f, C, W - 2 * pad)
        text(if (toFull == 0) "Full moon tonight" else "Full moon in $toFull day" + (if (toFull == 1) "" else "s"), W / 2, H - pad - 1f, 11f, uiFace(400), a(0.65f, white), 0f, C, W - 2 * pad)
    }

    /* ---------- Photo widgets ---------- */

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** The user's photo, cropped to fill; or a calm dusk landscape until they pick one. */
    private fun Ctx.photo(x0: Float, y0: Float, w: Float, h: Float) {
        val dst = android.graphics.RectF(px(x0), px(y0), px(x0 + w), px(y0 + h))
        val b = Photos.get(conf.photo)
        if (b != null) {
            val sr = b.width / b.height.toFloat(); val dr = dst.width() / dst.height()
            val src = if (sr > dr) { val cw = (b.height * dr).toInt(); android.graphics.Rect((b.width - cw) / 2, 0, (b.width + cw) / 2, b.height) }
                      else { val ch = (b.width / dr).toInt(); android.graphics.Rect(0, (b.height - ch) / 2, b.width, (b.height + ch) / 2) }
            c.drawBitmap(b, src, dst, bmpPaint)
            return
        }
        c.save(); c.clipRect(dst)
        c.drawRect(dst, Paint().apply { shader = LinearGradient(0f, dst.top, 0f, dst.bottom,
            intArrayOf(0xFF24305E.toInt(), 0xFF8D5B8C.toInt(), 0xFFF08A5D.toInt(), 0xFFF9C784.toInt()), floatArrayOf(0f, 0.45f, 0.75f, 1f), Shader.TileMode.CLAMP) })
        c.drawCircle(dst.centerX() + dst.width() * 0.18f, dst.top + dst.height() * 0.62f, dst.height() * 0.13f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD9FFE6B0.toInt() })
        fun ridge(base: Float, amp: Float, freq: Float, phase: Float, col: Int) {
            val path = Path(); path.moveTo(dst.left, dst.bottom)
            var xx = dst.left
            while (xx <= dst.right + 4f) {
                val t = (xx - dst.left) / dst.width()
                val yy = dst.top + dst.height() * (base - amp * (sin(t * freq + phase) * 0.6f + sin(t * freq * 2.3f + phase * 1.7f) * 0.4f))
                path.lineTo(xx, yy); xx += 6f
            }
            path.lineTo(dst.right, dst.bottom); path.close()
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col })
        }
        ridge(0.70f, 0.10f, 5f, 0.4f, 0xCC6B4A7A.toInt())
        ridge(0.80f, 0.08f, 7f, 2.1f, 0xE5402F55.toInt())
        ridge(0.90f, 0.05f, 9f, 4.0f, 0xFF231A33.toInt())
        c.restore()
    }

    /** Darkens the top and bottom so white text reads on any photo. */
    private fun Ctx.shade(top: Float, bottomFrom: Float) {
        if (top > 0f) c.drawRect(0f, 0f, px(W), px(top), Paint().apply { shader = LinearGradient(0f, 0f, 0f, px(top), 0x66000000, 0x00000000, Shader.TileMode.CLAMP) })
        c.drawRect(0f, px(bottomFrom), px(W), px(H), Paint().apply { shader = LinearGradient(0f, px(bottomFrom), 0f, px(H), 0x00000000, 0xA6000000.toInt(), Shader.TileMode.CLAMP) })
    }

    private val WHITE = 0xFFFFFFFF.toInt()

    private fun photoCountdown(x: Ctx) = with(x) {
        photo(0f, 0f, W, H)
        val dn = date()
        shade(46f, H * 0.42f)
        if (dn == null) { text("Add a date", pad, H - pad - 2f, 13f, uiFace(600), WHITE); return@with }
        val (d, n) = dn
        val name = d.name.ifBlank { "Your day" }
        if (showLabel) text(ttl(name).uppercase(), pad, pad + 9f, 10.5f, labelFace(), a(0.85f, WHITE), 0.12f, L, W - 2 * pad - (if (conf.design == "photowide") 90f else 0f))
        val wide = conf.design == "photowide"
        val big = n.days.toString()
        val sz = fit(big, if (wide) min(64f, H * 0.42f) else min(56f, H * 0.38f), W * 0.6f)
        val base = H - pad - (if (wide) 2f else 18f)
        val w = num(big, pad, base, sz, WHITE)
        text(if (n.days == 1) "day" else "days", pad + w + 6f, base - 3f, 15f, uiFace(500), a(0.9f, WHITE))
        if (wide) {
            // a frosted date pill, top right
            val ds = Fmt.short(n.d)
            val tw = measure(ds, 12f, uiFace(600)) + 20f
            rrect(W - pad - tw, pad - 3f, tw, 24f, 12f, fill(0x40FFFFFF))
            text(ds, W - pad - tw / 2, pad + 13f, 12f, uiFace(600), WHITE, 0f, C)
        } else text(Fmt.short(n.d), pad, H - pad - 1f, 11.5f, uiFace(500), a(0.8f, WHITE))
    }

    private fun photoDate(x: Ctx) = with(x) {
        photo(0f, 0f, W, H)
        shade(46f, H * 0.45f)
        val t = LocalDate.now()
        if (showLabel) text(ttl(t.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())).uppercase(), pad, pad + 9f, 10.5f, labelFace(), a(0.85f, WHITE), 0.12f, L, W - 2 * pad)
        val sz = min(60f, H * 0.4f)
        num(t.dayOfMonth.toString(), pad, H - pad - 18f, sz, WHITE)
        text(t.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + t.year, pad, H - pad - 1f, 12f, uiFace(500), a(0.85f, WHITE), maxW = W - 2 * pad)
    }

    private fun polaroid(x: Ctx) = with(x) {
        c.drawColor(0, PorterDuff.Mode.CLEAR)
        val n = D.notes.firstOrNull { it.id == conf.ref } ?: D.notes.firstOrNull()
        val text = n?.text?.trim().orEmpty().ifEmpty { "A note to self" }
        val cw = W * 0.80f; val ch = H * 0.90f
        c.save()
        c.rotate(-3.5f, px(W / 2), px(H / 2))
        val left = (W - cw) / 2; val top = (H - ch) / 2
        val card = android.graphics.RectF(px(left), px(top), px(left + cw), px(top + ch))
        c.drawRoundRect(card, px(2.5f), px(2.5f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBFAF5.toInt(); setShadowLayer(px(5f), 0f, px(2.5f), 0x55000000)
        })
        val inset = cw * 0.065f
        val ph = ch - inset - 40f
        photo(left + inset, top + inset, cw - 2 * inset, ph)
        para(text, left + inset, top + inset + ph + 5f, cw - 2 * inset, 12.5f, WFonts.serifItalic(), 0xFF2B2A26.toInt(), 0f, 0.95f, 2)
        // a strip of tape
        c.rotate(6f, px(W / 2), px(top))
        rrect(W / 2 - 18f, top - 7f, 36f, 13f, 1.5f, fill(0x99F3E7C8.toInt()))
        c.restore()
    }

    /* ---------- Water glass ---------- */

    private fun liquid(x: Ctx) = with(x) {
        val ct = D.counters.firstOrNull { it.id == conf.ref } ?: D.counters.firstOrNull() ?: run { nothing("Add a counter"); return@with }
        val v = cnt(ct)
        val pct = (v.toFloat() / ct.goal).coerceIn(0f, 1f)
        if (showLabel) label(ttl(ct.name), pad, pad + 11.5f, W - 2 * pad)
        val bh = min(32f, H * 0.2f); val btnY = H - pad - bh
        val gTop = pad + 22f; val gBot = btnY - 9f; val gh = gBot - gTop; val gw = gh * 0.64f
        val gx = pad + 2f
        val glass = Path().apply {
            moveTo(px(gx), px(gTop)); lineTo(px(gx + gw), px(gTop))
            lineTo(px(gx + gw * 0.88f), px(gBot - 5f)); quadTo(px(gx + gw * 0.86f), px(gBot), px(gx + gw * 0.78f), px(gBot))
            lineTo(px(gx + gw * 0.22f), px(gBot)); quadTo(px(gx + gw * 0.14f), px(gBot), px(gx + gw * 0.12f), px(gBot - 5f)); close()
        }
        c.save(); c.clipPath(glass)
        c.drawPath(glass, fill(a(0.06f, p.fg)))
        if (pct > 0f) {
            val level = gBot - (gh - 3f) * pct
            fun wave(phase: Float, amp: Float, col: Int, shader: Shader?) {
                val path = Path(); path.moveTo(px(gx - 2f), px(gBot + 2f))
                var xx = gx - 2f
                while (xx <= gx + gw + 2f) {
                    path.lineTo(px(xx), px(level + amp * sin(((xx - gx) / gw) * 2.6f * PI.toFloat() + phase)))
                    xx += 1.5f
                }
                path.lineTo(px(gx + gw + 2f), px(gBot + 2f)); path.close()
                c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; this.shader = shader })
            }
            wave(1.6f, 2.2f, a(0.35f, p.acc), null)
            wave(0f, 2.4f, p.acc, LinearGradient(0f, px(level), 0f, px(gBot), a(0.82f, p.acc), p.acc, Shader.TileMode.CLAMP))
            listOf(0.3f to 0.75f, 0.62f to 0.55f, 0.45f to 0.88f, 0.72f to 0.82f).forEach { (bx, by) ->
                val yy = gBot - (gBot - level) * (1f - by)
                if (yy > level + 4f) circle(gx + gw * bx, yy, 1.6f, stroke(a(0.7f, WHITE), 0.8f))
            }
        }
        c.restore()
        c.drawPath(glass, stroke(a(0.55f, p.fg), 1.6f).apply { strokeJoin = Paint.Join.ROUND })
        line(gx + gw * 0.2f, gTop + 6f, gx + gw * 0.28f, gBot - 10f, stroke(a(0.35f, WHITE), 1.6f))
        // the count
        val rx = gx + gw + 14f
        val sz = fit(v.toString(), min(46f, gh * 0.62f), W - rx - pad)
        num(v.toString(), rx, gTop + 4f + cap(sz), sz, if (v >= ct.goal) p.acc else p.fg)
        small("of ${ct.goal} ${ct.unit}", rx, gTop + 4f + cap(sz) + 16f, maxW = W - rx - pad)
        if (v >= ct.goal) small("Goal reached", rx, gTop + 4f + cap(sz) + 31f, p.acc, 11.5f, maxW = W - rx - pad, weight = 600)
        val mw = 44f
        button(pad, btnY, mw, bh, "ghost", "cnt-", ct.id, ic = Paths.minus, icSize = 13f)
        button(pad + mw + 6f, btnY, W - 2 * pad - mw - 6f, bh, "solid", "cnt", ct.id, text = "+1")
    }

    /* ---------- Streak flame ---------- */

    private fun flame(x: Ctx) = with(x) {
        val h = D.habits.firstOrNull { it.id == conf.ref } ?: D.habits.firstOrNull() ?: run { nothing("Add a habit"); return@with }
        val st = streak(h)
        if (showLabel) label(ttl(h.name), pad, pad + 11.5f, W - 2 * pad - 44f)
        button(W - pad - 36f, pad - 6f, 36f, 36f, if (st.today) "ghost" else "acc", "hab", h.id, ic = Paths.check)
        val grow = 0.55f + 0.45f * min(1f, st.n / 30f)
        val by = H - pad - 2f
        val fh = (H - pad - 30f - pad) * grow
        val fw = fh * 0.66f
        val cx = pad + (W * 0.46f - pad) / 2 + 4f
        val lit = st.today
        if (lit) c.drawCircle(px(cx), px(by - fh * 0.42f), px(fh * 0.75f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(px(cx), px(by - fh * 0.42f), px(fh * 0.75f), 0x55FF7A1F, 0x00FF7A1F, Shader.TileMode.CLAMP)
        })
        /** A flame: round base, narrow body, tip leaning right. */
        fun shape(k: Float, oy: Float, dx: Float = 0f): Path {
            val h2 = fh * k; val w2 = fw * k; val b = by - oy; val x0 = cx + dx
            return Path().apply {
                moveTo(px(x0), px(b))
                cubicTo(px(x0 - w2 * 0.52f), px(b - h2 * 0.04f), px(x0 - w2 * 0.58f), px(b - h2 * 0.40f), px(x0 - w2 * 0.24f), px(b - h2 * 0.62f))
                cubicTo(px(x0 - w2 * 0.04f), px(b - h2 * 0.78f), px(x0 - w2 * 0.14f), px(b - h2 * 0.92f), px(x0 + w2 * 0.06f), px(b - h2))
                cubicTo(px(x0 + w2 * 0.12f), px(b - h2 * 0.82f), px(x0 + w2 * 0.40f), px(b - h2 * 0.72f), px(x0 + w2 * 0.44f), px(b - h2 * 0.48f))
                cubicTo(px(x0 + w2 * 0.56f), px(b - h2 * 0.22f), px(x0 + w2 * 0.40f), px(b), px(x0), px(b))
                close()
            }
        }
        val hot = intArrayOf(0xFFFF3D1F.toInt(), 0xFFFF8A1F.toInt(), 0xFFFFD45A.toInt())
        val ember = intArrayOf(a(0.55f, 0xFFB4441E.toInt()), a(0.40f, 0xFFD9762E.toInt()), a(0.25f, 0xFFE9A04A.toInt()))
        val cols = if (lit) hot else ember
        fun grad(top: Float) = LinearGradient(0f, px(by), 0f, px(top), cols, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        // a smaller tongue behind, then the main flame, then the bright core
        c.drawPath(shape(0.62f, 0f, -fw * 0.30f), Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = grad(by - fh * 0.62f); alpha = 200 })
        c.drawPath(shape(1f, 0f), Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = grad(by - fh) })
        c.drawPath(shape(0.5f, fh * 0.03f, fw * 0.02f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, px(by), 0f, px(by - fh * 0.5f), if (lit) 0xFFFFF6D0.toInt() else a(0.35f, 0xFFFFE2B0.toInt()), if (lit) 0xFFFFC23D.toInt() else a(0.2f, 0xFFF0B060.toInt()), Shader.TileMode.CLAMP)
        })
        // the number
        val rx = W * 0.5f
        val sz = fit(st.n.toString(), min(50f, H * 0.34f), W - rx - pad)
        val nb = H * 0.5f + cap(sz) / 2
        num(st.n.toString(), rx, nb, sz, p.fg)
        small(if (st.n == 1) "day streak" else "day streak", rx, nb + 15f, maxW = W - rx - pad)
        small(if (lit) "Lit today" else "Not lit yet", rx, nb + 30f, if (lit) p.acc else p.mut, 11.5f, maxW = W - rx - pad + 6f, weight = 600)
    }

    /* ---------- Fasting sun arc ---------- */

    private fun sunArc(x: Ctx) = with(x) {
        val f = fastState()
        if (showLabel) label(ttl(if (f.on) "Fasting · goal ${f.target}h" else "Fast · ${f.target}h goal"), pad, pad + 11.5f, W - 2 * pad - 80f)
        button(W - pad - 72f, pad - 7f, 72f, 30f, if (f.on) "ghost" else "solid", "fast", text = if (f.on) "End" else "Start")
        val cy = H - pad - 20f
        val r = min(W / 2 - 46f, cy - (pad + 34f))
        val cx = W / 2
        val oval = android.graphics.RectF(px(cx - r), px(cy - r), px(cx + r), px(cy + r))
        val track = stroke(a(0.22f, p.fg), 2f); track.strokeCap = Paint.Cap.ROUND; track.pathEffect = DashPathEffect(floatArrayOf(px(2f), px(5f)), 0f)
        c.drawArc(oval, 180f, 180f, false, track); track.pathEffect = null
        line(cx - r - 22f, cy, cx + r + 22f, cy, stroke(a(0.18f, p.fg), 1f))
        val pct = f.pct.toFloat()
        if (f.on && pct > 0.005f) {
            val pr = stroke(p.acc, 4.5f)
            c.drawArc(oval, 180f, 180f * pct, false, pr)
        }
        // the sun, travelling the arc from start to goal
        val ang = PI * (1 - pct)
        val sx = cx + r * cos(ang).toFloat(); val sy = cy - r * sin(ang).toFloat()
        if (f.on) {
            c.drawCircle(px(sx), px(sy), px(16f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(px(sx), px(sy), px(16f), a(0.45f, p.acc), a(0f, p.acc), Shader.TileMode.CLAMP)
            })
            for (i in 0 until 8) {
                val t = i * PI / 4
                line(sx + 9.5f * cos(t).toFloat(), sy + 9.5f * sin(t).toFloat(), sx + 13f * cos(t).toFloat(), sy + 13f * sin(t).toFloat(), stroke(p.acc, 1.4f))
            }
            circle(sx, sy, 6.5f, fill(p.acc))
        }
        // the time
        val tx = if (f.on) Fmt.hhmm(f.h) else "0:00"
        val ts = fit(tx, min(32f, r * 0.42f), r * 1.3f)
        num(tx, cx, cy - 14f, ts, p.fg, C)
        small(if (!f.on) "Ready when you are" else if (f.h >= f.target) "Goal reached" else "${((1 - f.pct) * f.target).let { Fmt.hhmm(it) }} to go", cx, cy - 1f, if (f.on && f.h >= f.target) p.acc else p.mut, 11f, C)
        if (f.on) {
            val start = f.endMs - f.target * 3_600_000L
            small(Fmt.at(start), cx - r, cy + 15f, size = 11f, align = C)
            small(Fmt.at(f.endMs), cx + r, cy + 15f, size = 11f, align = C)
        }
    }

    /* ---------- Goal summit ---------- */

    private fun summit(x: Ctx) = with(x) {
        val g = D.goals.firstOrNull { it.id == conf.ref } ?: D.goals.firstOrNull() ?: run { nothing("Add a goal"); return@with }
        val r = pace(g)
        val pct = r.pct.toFloat().coerceIn(0f, 1f)
        val colW = W * 0.42f
        if (showLabel) label(ttl(g.name), pad, pad + 11.5f, colW - 4f)
        val ps = "${(pct * 100).roundToInt()}%"
        val sz = fit(ps, min(40f, H * 0.26f), colW - pad)
        num(ps, pad, pad + 24f + cap(sz), sz, p.fg)
        small("${Fmt.trimNum(g.done)} of ${Fmt.trimNum(g.target)} ${g.unit}", pad, pad + 24f + cap(sz) + 16f, maxW = colW - 4f)
        val ahead = r.ahead.roundToInt()
        small(if (ahead <= -1) "${-ahead} days behind" else if (ahead >= 1) "$ahead days ahead" else "On track", pad, pad + 24f + cap(sz) + 31f,
            if (ahead <= -1) p.acc else p.fg, 11.5f, maxW = colW - 4f, weight = 600)
        button(pad, H - pad - 30f, 30f, 30f, "solid", "goal", g.id, ic = Paths.plus, icSize = 14f)
        // the mountain
        val mx0 = W * 0.38f; val mx1 = W + 4f; val my0 = pad + 4f; val my1 = H + 2f
        fun P(u: Float, v: Float) = (mx0 + (mx1 - mx0) * u) to (my0 + (my1 - my0) * v)
        fun poly(vararg pts: Pair<Float, Float>): Path = Path().apply {
            pts.forEachIndexed { i, (u, v) -> val (xx, yy) = P(u, v); if (i == 0) moveTo(px(xx), px(yy)) else lineTo(px(xx), px(yy)) }; close()
        }
        val light = luma(p.bg or 0xFF000000.toInt()) > 0.5f
        c.drawPath(poly(0f to 1f, 0.16f to 0.64f, 0.27f to 0.72f, 0.52f to 0.08f, 0.70f to 0.42f, 0.80f to 0.34f, 1f to 0.86f, 1f to 1f), fill(a(if (light) 0.17f else 0.11f, p.fg)))
        c.drawPath(poly(0f to 1f, 0.16f to 0.64f, 0.27f to 0.72f, 0.52f to 0.08f, 0.46f to 1f), fill(a(if (light) 0.10f else 0.07f, p.fg)))
        val snow = if (luma(p.bg or 0xFF000000.toInt()) > 0.5f) 0xFFFFFFFF.toInt() else a(0.9f, 0xFFEFF2F7.toInt())
        c.drawPath(poly(0.45f to 0.21f, 0.52f to 0.08f, 0.60f to 0.22f, 0.565f to 0.19f, 0.53f to 0.25f, 0.495f to 0.19f), fill(snow))
        // the trail, walked so far in colour
        val trail = listOf(0.06f to 0.97f, 0.24f to 0.80f, 0.15f to 0.66f, 0.34f to 0.52f, 0.28f to 0.40f, 0.45f to 0.27f, 0.52f to 0.10f).map { P(it.first, it.second) }
        val seg = trail.zipWithNext { p1, p2 -> sqrt((p2.first - p1.first) * (p2.first - p1.first) + (p2.second - p1.second) * (p2.second - p1.second)) }
        val total = seg.sum()
        val dash = stroke(a(0.4f, p.fg), 1.4f); dash.pathEffect = DashPathEffect(floatArrayOf(px(2.5f), px(3f)), 0f)
        val tp = Path().apply { moveTo(px(trail[0].first), px(trail[0].second)); trail.drop(1).forEach { lineTo(px(it.first), px(it.second)) } }
        c.drawPath(tp, dash); dash.pathEffect = null
        var left = total * pct; var hx = trail[0].first; var hy = trail[0].second
        val done = Path().apply { moveTo(px(hx), px(hy)) }
        for (i in seg.indices) {
            if (left <= 0f) break
            val t = min(1f, left / seg[i])
            hx = trail[i].first + (trail[i + 1].first - trail[i].first) * t
            hy = trail[i].second + (trail[i + 1].second - trail[i].second) * t
            done.lineTo(px(hx), px(hy)); left -= seg[i]
        }
        c.drawPath(done, stroke(p.acc, 2.4f).apply { strokeJoin = Paint.Join.ROUND })
        // flag on the summit
        val (fx, fy) = P(0.52f, 0.08f)
        line(fx, fy, fx, fy - 16f, stroke(p.fg, 1.4f))
        c.drawPath(Path().apply { moveTo(px(fx), px(fy - 16f)); lineTo(px(fx + 11f), px(fy - 12.5f)); lineTo(px(fx), px(fy - 9f)); close() }, fill(p.acc))
        // you, on the trail
        circle(hx, hy, 6f, fill(p.bg or 0xFF000000.toInt()))
        circle(hx, hy, 4.5f, fill(p.acc))
    }
}
