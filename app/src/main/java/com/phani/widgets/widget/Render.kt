package com.phani.widgets.widget

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.PathParser
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.WConf
import kotlin.math.max
import kotlin.math.min

/** A tappable area on a rendered widget, in pixels. */
class Hit(val rect: RectF, val act: String, val id: String? = null, val value: String? = null)

class Rendered(val bitmap: Bitmap, val hits: List<Hit>)

/** Fonts as Android typefaces, shared by every widget render. */
object WFonts {
    private val cache = HashMap<String, Typeface>()
    private var assets: AssetManager? = null
    fun init(a: AssetManager) { assets = a }
    fun get(file: String, w: Int): Typeface = cache.getOrPut("$file/$w") {
        val a = assets ?: return@getOrPut Typeface.DEFAULT
        try { Typeface.Builder(a, "fonts/$file").setFontVariationSettings("'wght' $w").build() } catch (e: Exception) { Typeface.DEFAULT }
    }
    fun serif(): Typeface = cache.getOrPut("serif") {
        val a = assets ?: return@getOrPut Typeface.SERIF
        try { Typeface.createFromAsset(a, "fonts/InstrumentSerif.ttf") } catch (e: Exception) { Typeface.SERIF }
    }
}

/** Colours for one widget, resolved from its look and accent. */
class WPal(val bg: Int, val fg: Int, val mut: Int, val soft: Int, val acc: Int, val accOnBtn: Int, val btnText: Int)

const val C_SIGNAL = 0xFFFF5A1F.toInt()
val ACCENTS = linkedMapOf(
    "signal" to 0xFFFF5A1F.toInt(), "cobalt" to 0xFF3B5BFF.toInt(), "mint" to 0xFF12A26B.toInt(),
    "lilac" to 0xFF8E6CFF.toInt(), "sun" to 0xFFF0B400.toInt(), "rose" to 0xFFF2557A.toInt(),
)

private fun argb(a: Float, rgb: Int): Int = ((a * 255).toInt().coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

fun hexColor(s: String?): Int? = s?.let { runCatching { android.graphics.Color.parseColor(it) }.getOrNull() }
fun accentColor(a: String): Int = ACCENTS[a] ?: hexColor(a) ?: C_SIGNAL
fun luma(c: Int): Float = (0.299f * ((c shr 16) and 255) + 0.587f * ((c shr 8) and 255) + 0.114f * (c and 255)) / 255f

fun widgetPal(conf: WConf, wallFg: Int, wallBg: Int): WPal {
    val base = basePal(conf, wallFg, wallBg)
    var bg = base.bg; var fg = base.fg; var mut = base.mut; var soft = base.soft; var btnText = base.btnText
    val cbg = hexColor(conf.bg)
    if (cbg != null) {
        bg = cbg or 0xFF000000.toInt()
        fg = if (luma(bg) > 0.6f) 0xFF121211.toInt() else 0xFFF6F4EE.toInt()
        mut = argb(0.68f, fg); soft = argb(0.14f, fg); btnText = bg
    }
    val cfg = hexColor(conf.fg)
    if (cfg != null) { fg = cfg or 0xFF000000.toInt(); mut = argb(0.68f, fg); soft = argb(0.15f, fg) }
    if (conf.alpha < 100) {
        val a = ((bg ushr 24) and 255) / 255f * conf.alpha.coerceIn(0, 100) / 100f
        bg = argb(a, bg)
    }
    // text on an accent-filled button stays readable for any custom accent
    val accOnBtn = if (conf.style == "tint" && cbg == null) base.accOnBtn else if (luma(base.acc) > 0.72f) 0xFF141413.toInt() else 0xFFFFFFFF.toInt()
    return WPal(bg, fg, mut, soft, base.acc, accOnBtn, btnText)
}

private fun basePal(conf: WConf, wallFg: Int, wallBg: Int): WPal {
    val acc = accentColor(conf.accent)
    val lightAcc = luma(acc) > 0.62f
    return when (conf.style) {
        "ink" -> WPal(0xFF151514.toInt(), 0xFFF3F1EB.toInt(), 0xFFA9A59D.toInt(), 0xFF2C2B29.toInt(), acc, 0xFFFFFFFF.toInt(), 0xFF151514.toInt())
        "frost" -> WPal(argb(0.62f, 0xFFFFFF), 0xFF121211.toInt(), 0xFF4E4A44.toInt(), argb(0.10f, 0x121211), acc, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        "smoke" -> WPal(argb(0.58f, 0x121211), 0xFFF6F4EE.toInt(), argb(0.74f, 0xF6F4EE), argb(0.16f, 0xF6F4EE), acc, 0xFFFFFFFF.toInt(), 0xFF141413.toInt())
        "clear" -> WPal(0x00000000, wallFg, argb(0.74f, wallFg), argb(0.18f, wallFg), acc, 0xFFFFFFFF.toInt(), wallBg or 0xFF000000.toInt())
        "tint" -> if (lightAcc) WPal(acc, 0xFF141413.toInt(), argb(0.72f, 0x141413), argb(0.16f, 0x141413), 0xFF141413.toInt(), acc, acc)
                  else WPal(acc, 0xFFFFFFFF.toInt(), argb(0.8f, 0xFFFFFF), argb(0.24f, 0xFFFFFF), 0xFFFFFFFF.toInt(), acc, acc)
        else -> WPal(0xFFFFFFFF.toInt(), 0xFF121211.toInt(), 0xFF5D5952.toInt(), 0xFFECEAE4.toInt(), acc, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
    }
}

/**
 * Drawing context: sizes are in dp, converted with [k] (pixels per dp).
 * Mirrors the CSS of the web prototype so a design looks the same here and there.
 */
class Ctx(val c: Canvas, val wPx: Float, val hPx: Float, val k: Float, val p: WPal, val conf: WConf) {
    val hits = ArrayList<Hit>()
    val W = wPx / k
    val H = hPx / k
    val pad = if (min(W, H) < 150f) 13f else 16f
    val showLabel = conf.label
    /** The user's own title when they set one, otherwise the design's default heading. */
    fun ttl(def: String): String = conf.title?.trim()?.takeIf { it.isNotEmpty() } ?: def
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)

    fun px(v: Float) = v * k

    /* ---- type ---- */
    fun uiFace(w: Int) = WFonts.get("Geist.ttf", w)
    fun monoFace(w: Int) = WFonts.get("GeistMono.ttf", w)
    fun numFace(): Typeface = when (conf.font) { "serif" -> WFonts.serif(); "mono" -> monoFace(500); else -> uiFace(600) }
    fun numSpacing(): Float = when (conf.font) { "serif" -> -0.02f; "mono" -> -0.06f; else -> -0.05f }
    fun labelFace(): Typeface = monoFace(500)
    fun t2Face(): Typeface = if (conf.font == "mono") monoFace(600) else uiFace(600)

    private fun prep(size: Float, face: Typeface, color: Int, spacing: Float, align: Paint.Align): TextPaint {
        tp.reset(); tp.isAntiAlias = true
        tp.textSize = px(size); tp.typeface = face; tp.color = color; tp.letterSpacing = spacing; tp.textAlign = align
        return tp
    }

    fun measure(s: String, size: Float, face: Typeface, spacing: Float = 0f): Float = prep(size, face, 0, spacing, Paint.Align.LEFT).measureText(s) / k

    /** Draws one line with its baseline at [base] (dp). Ellipsizes to [maxW] if given. Returns width in dp. */
    fun text(s: String, x: Float, base: Float, size: Float, face: Typeface, color: Int, spacing: Float = 0f,
             align: Paint.Align = Paint.Align.LEFT, maxW: Float = 0f): Float {
        val t = prep(size, face, color, spacing, align)
        val str = if (maxW > 0) TextUtils.ellipsize(s, t, px(maxW), TextUtils.TruncateAt.END).toString() else s
        c.drawText(str, px(x), px(base), t)
        return t.measureText(str) / k
    }

    /** Wrapped text in a box; returns the height used in dp. */
    fun para(s: String, x: Float, top: Float, width: Float, size: Float, face: Typeface, color: Int, spacing: Float = 0f,
             lineMult: Float = 1.1f, maxLines: Int = 3): Float {
        val t = prep(size, face, color, spacing, Paint.Align.LEFT)
        val lay = StaticLayout.Builder.obtain(s, 0, s.length, t, max(1, px(width).toInt()))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, lineMult).setIncludePad(false)
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build()
        c.save(); c.translate(px(x), px(top)); lay.draw(c); c.restore()
        return lay.height / k
    }

    /** Mono uppercase label with tracking, like `.wl` in the prototype. Baseline-anchored. */
    fun label(s: String, x: Float, base: Float, maxW: Float = W - 2 * pad, color: Int = p.mut, align: Paint.Align = Paint.Align.LEFT) {
        text(s.uppercase(), x, base, 10.5f, labelFace(), color, 0.12f, align, maxW)
    }

    fun num(s: String, x: Float, base: Float, size: Float, color: Int = p.fg, align: Paint.Align = Paint.Align.LEFT): Float =
        text(s, x, base, size, numFace(), color, numSpacing(), align)

    fun numW(s: String, size: Float) = measure(s, size, numFace(), numSpacing())

    /** Cap height factor so numerals sit like CSS line-height .85. */
    fun cap(size: Float) = size * 0.72f

    fun small(s: String, x: Float, base: Float, color: Int = p.mut, size: Float = 12f, align: Paint.Align = Paint.Align.LEFT, maxW: Float = 0f, weight: Int = 400) =
        text(s, x, base, size, uiFace(weight), color, 0f, align, maxW)

    fun t2(s: String, x: Float, base: Float, size: Float, color: Int = p.fg, maxW: Float = 0f, align: Paint.Align = Paint.Align.LEFT) =
        text(s, x, base, size, t2Face(), color, -0.01f, align, maxW)

    /* ---- shapes ---- */
    fun fill(color: Int) = paint.apply { reset(); isAntiAlias = true; style = Paint.Style.FILL; this.color = color }
    fun stroke(color: Int, w: Float) = paint.apply { reset(); isAntiAlias = true; style = Paint.Style.STROKE; this.color = color; strokeWidth = px(w); strokeCap = Paint.Cap.ROUND }

    fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float, pnt: Paint) = c.drawRoundRect(RectF(px(x), px(y), px(x + w), px(y + h)), px(r), px(r), pnt)
    fun circle(cx: Float, cy: Float, r: Float, pnt: Paint) = c.drawCircle(px(cx), px(cy), px(r), pnt)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, pnt: Paint) = c.drawLine(px(x1), px(y1), px(x2), px(y2), pnt)

    fun dashed(x1: Float, y: Float, x2: Float, color: Int) {
        val pt = stroke(color, 1f); pt.strokeCap = Paint.Cap.BUTT; pt.pathEffect = DashPathEffect(floatArrayOf(px(4f), px(3f)), 0f)
        c.drawLine(px(x1), px(y), px(x2), px(y), pt); pt.pathEffect = null
    }

    /** Progress ring centred on (cx, cy). */
    fun ring(cx: Float, cy: Float, size: Float, sw: Float, pct: Float, color: Int, track: Int = p.soft) {
        val r = (size - sw) / 2
        val rect = RectF(px(cx - r), px(cy - r), px(cx + r), px(cy + r))
        c.drawArc(rect, 0f, 360f, false, stroke(track, sw))
        val sweep = 360f * pct.coerceIn(0f, 1f)
        if (sweep > 0.5f) c.drawArc(rect, -90f, sweep, false, stroke(color, sw))
    }

    /** Track bar with optional fill and a tick marker (0..1). */
    fun bar(x: Float, y: Float, w: Float, h: Float, pct: Float, color: Int = p.fg, tick: Float? = null) {
        rrect(x, y, w, h, h / 2, fill(p.soft))
        val fw = w * pct.coerceIn(0f, 1f)
        if (fw > 0.5f) rrect(x, y, max(fw, h), h, h / 2, fill(color))
        if (tick != null) rrect(x + w * tick - 0.75f, y - 3f, 1.5f, h + 6f, 0.75f, fill(p.mut))
    }

    /* ---- icons (24-unit SVG paths) ---- */
    fun icon(paths: List<String>, cx: Float, cy: Float, size: Float, color: Int, sw: Float = 2f, filled: Boolean = false) {
        val s = px(size) / 24f
        val m = Matrix(); m.setScale(s, s); m.postTranslate(px(cx) - px(size) / 2, px(cy) - px(size) / 2)
        val pnt = if (filled) fill(color) else stroke(color, sw * size / 24f).apply { strokeJoin = Paint.Join.ROUND }
        paths.forEach { d -> val path: Path = PathParser.createPathFromPathData(d); path.transform(m); c.drawPath(path, pnt) }
    }

    /** Button like `.wbtn`. kind: solid, ghost, acc. Registers a hit area. */
    fun button(x: Float, y: Float, w: Float, h: Float, kind: String, act: String, id: String? = null, value: String? = null,
               text: String? = null, ic: List<String>? = null, icFilled: Boolean = false, icSize: Float = 18f) {
        val r = min(h / 2, 999f)
        val fgc: Int
        when (kind) {
            "acc" -> { rrect(x, y, w, h, r, fill(p.acc)); fgc = p.accOnBtn }
            "ghost" -> { rrect(x + 0.5f, y + 0.5f, w - 1f, h - 1f, r, stroke(argb(0.22f, p.fg) or 0, 1f).apply { color = (0x38 shl 24) or (p.fg and 0xFFFFFF) }); fgc = p.fg }
            else -> { rrect(x, y, w, h, r, fill(p.fg)); fgc = p.btnText }
        }
        var tw = 0f
        val face = uiFace(600)
        if (text != null) tw = measure(text, 14f, face)
        val iw = if (ic != null) icSize else 0f
        val gap = if (text != null && ic != null) 6f else 0f
        var cx = x + w / 2 - (tw + iw + gap) / 2
        if (ic != null) { icon(ic, cx + iw / 2, y + h / 2, iw, fgc, 2.2f, icFilled); cx += iw + gap }
        if (text != null) text(text, cx, y + h / 2 + 5f, 14f, face, fgc)
        hits.add(Hit(RectF(px(x), px(y), px(x + w), px(y + h)), act, id, value))
    }

    /** A small Undo pill in the top-right corner, shown for a few seconds after a tap changed something. */
    fun undoButton() {
        val w = 70f; val h = 30f
        val x = W - pad - w + 4f; val y = max(4f, pad - 8f)
        rrect(x - 3f, y - 3f, w + 6f, h + 6f, (h + 6f) / 2, fill(p.bg or 0xFF000000.toInt()))
        button(x, y, w, h, "solid", "undo", text = "Undo")
    }

    fun hitArea(x: Float, y: Float, w: Float, h: Float, act: String, id: String? = null) {
        hits.add(Hit(RectF(px(x), px(y), px(x + w), px(y + h)), act, id))
    }
}

object Paths {
    val plus = listOf("M12 5v14", "M5 12h14")
    val minus = listOf("M5 12h14")
    val check = listOf("M5 12.5l4.5 4.5L19 7.5")
    val x = listOf("M6 6l12 12", "M18 6L6 18")
    val play = listOf("M8 5.5v13l10-6.5z")
    val stop = listOf("M9 7h6a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z")
}

val CORNERS = mapOf("round" to 26f, "soft" to 16f, "sharp" to 6f)

/** Preferred height in dp for a design when the host gives no size (in-app preview). */
fun preferredHeight(conf: WConf, widthDp: Float): Float = when (conf.size) {
    "s" -> widthDp
    "w" -> 84f
    else -> when ("${conf.type}/${conf.design}") {
        "streak/heat" -> 224f
        "spend/receipt" -> 214f
        "countdown/stack" -> 196f
        "next/today" -> 200f
        "year/dots" -> 196f
        "clock/analog" -> 176f
        else -> 184f
    }
}

/** Render a widget to a bitmap with its tap areas. Width/height in pixels. */
fun renderWidget(conf: WConf, wPx: Int, hPx: Int, density: Float, sample: Boolean = false): Rendered {
    if (!sample) return draw(conf, wPx, hPx, density)
    AppStore.previewing = true
    try { return draw(conf.copy(ref = Catalog.defaultRefIn(conf.type, AppStore.sample)), wPx, hPx, density) } finally { AppStore.previewing = false }
}

private fun draw(conf: WConf, wPx: Int, hPx: Int, density: Float): Rendered {
    val (wbg, wfg) = wallColors()
    val bmp = Bitmap.createBitmap(max(1, wPx), max(1, hPx), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val pal = widgetPal(conf, wfg, wbg)
    val ctx = Ctx(c, wPx.toFloat(), hPx.toFloat(), density, pal, conf)
    val r = if (conf.radius >= 0) conf.radius.toFloat() else CORNERS[conf.corner] ?: 26f
    val clip = Path().apply { addRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), r * density, r * density, Path.Direction.CW) }
    c.save()
    c.clipPath(clip)
    if (pal.bg != 0) c.drawColor(pal.bg)
    if (conf.outline) {
        val s = ctx.stroke(argb(0.28f, pal.fg), 1.5f)
        c.drawRoundRect(RectF(0.75f * density, 0.75f * density, wPx - 0.75f * density, hPx - 0.75f * density), r * density, r * density, s)
    } else if (conf.style == "clear") {
        val s = ctx.stroke((0x38 shl 24) or (wfg and 0xFFFFFF), 1f)
        c.drawRoundRect(RectF(0.5f * density, 0.5f * density, wPx - 0.5f * density, hPx - 0.5f * density), r * density, r * density, s)
    }
    try { Designs.draw(ctx) } catch (e: Exception) { ctx.small("Couldn't draw this widget", 16f, 30f) }
    if (!AppStore.previewing && AppStore.undoShownOn(conf.id)) ctx.undoButton()
    c.restore()
    return Rendered(bmp, ctx.hits)
}

/** Wallpaper the user picked in the app, used by the Clear look. */
fun wallColors(): Pair<Int, Int> = when (AppStore.data.wall) {
    "night" -> 0xFF16171C.toInt() to 0xFFF3F1EB.toInt()
    "sage" -> 0xFFB5C0AE.toInt() to 0xFF1C1C1A.toInt()
    "dusk" -> 0xFF3A3F55.toInt() to 0xFFF3F1EB.toInt()
    "peach" -> 0xFFEBC8B4.toInt() to 0xFF1C1C1A.toInt()
    "sky" -> 0xFFAFC6DA.toInt() to 0xFF1C1C1A.toInt()
    else -> 0xFFD7D3CA.toInt() to 0xFF1C1C1A.toInt()
}
