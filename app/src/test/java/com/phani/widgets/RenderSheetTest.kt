package com.phani.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.WFonts
import com.phani.widgets.widget.renderWidget
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Renders every widget design at real home-screen sizes into one contact sheet (sheet.png). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderSheetTest {
    @Test
    fun sheet() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppStore.load(ctx)
        WFonts.init(ctx.assets)
        val k = 2.5f
        // smallest common launcher sizes: 2x2 ≈ 150dp, 4x1 ≈ 320x76, 4x2 ≈ 320x160
        // real phone home-screen sizes (a vivo/Pixel-class 4-column grid), scaled the way widgets are on the phone
        val sizes = mapOf("s" to (176f to 176f), "w" to (364f to 92f), "m" to (364f to 192f))
        val items = Catalog.types.flatMap { t -> t.designs.map { d -> WConf("t", t.type, d.id, d.size, if (t.type == "now") "ink" else "paper", ref = Catalog.defaultRef(t.type)) } } + listOf(
            WConf("t", "spend", "ring", "s", accent = "#9BBF2A", bg = "#1E3A5F", radius = 8, outline = true, title = "Coffee money"),
            WConf("t", "spend", "number", "s", opt = "period"),
        ) + com.phani.widgets.widget.Themes.all.flatMap { th -> listOf(
            WConf("t", "countdown", "big", "s", theme = th.id, ref = Catalog.defaultRef("countdown")),
            WConf("t", "streak", "heat", "m", theme = th.id),
            WConf("t", "clock", "pair", "w", theme = th.id),
        ) } + listOf(
            WConf("t", "spend", "strip", "w", opt = "week"),
            WConf("t", "spend", "receipt", "m", opt = "period"),
            WConf("t", "countdown", "flip", "w", accent = "#D6409F", bg = "#FDE2E4", alpha = 70, title = "Goa trip", ref = Catalog.defaultRef("countdown")),
            WConf("t", "streak", "heat", "m", "ink", "#0EA5A4", fg = "#F0B400", radius = 0, ref = Catalog.defaultRef("streak")),
        )
        draw(items, sizes, k, File("build/sheet.png"), photo = false)
        // the same designs when the home screen gives them a taller space (resized, or a launcher with tall cells)
        val tall = mapOf("s" to (176f to 250f), "w" to (364f to 126f), "m" to (300f to 290f))
        val plain = Catalog.types.flatMap { t -> t.designs.map { d -> WConf("t", t.type, d.id, d.size, theme = "paper", ref = Catalog.defaultRef(t.type)) } }
        draw(plain, tall, k, File("build/tall.png"), photo = false)
        // Signature designs in a range of themes, over a photo-like wallpaper so cut-outs and glass show
        val sigs = Catalog.types.flatMap { t -> t.designs.filter { it.sig }.map { t to it } }
        val sigItems = listOf("paper", "midnight", "glass", "modern", "retro", "neon", "terminal", "pastel", "newspaper").flatMap { th ->
            sigs.map { (t, d) -> WConf("t", t.type, d.id, d.size, theme = th, ref = Catalog.defaultRef(t.type)) }
        }
        draw(sigItems, sizes, k, File("build/sig.png"), photo = true)
        // the remaining styles, so every design is checked in every style
        val sig2 = listOf("glassdark", "ocean", "sunset", "ink", "wallpaper", "clear").flatMap { th ->
            sigs.map { (t, d) -> WConf("t", t.type, d.id, d.size, theme = th, ref = Catalog.defaultRef(t.type)) }
        }
        draw(sig2, sizes, k, File("build/sig2.png"), photo = true)
        // downloaded styles (the drops file in the repo), so each new seasonal look is checked before it goes out
        val dropsFile = File("../drops/drops.json")
        if (dropsFile.exists()) {
            AppStore.data.drops = dropsFile.readText()
            val dropItems = com.phani.widgets.core.Drops.styles().flatMap { th -> listOf(
                WConf("t", "countdown", "big", "s", theme = th.id, ref = Catalog.defaultRef("countdown")),
                WConf("t", "streak", "flame", "s", theme = th.id, ref = Catalog.defaultRef("streak")),
                WConf("t", "weather", "now", "s", theme = th.id),
                WConf("t", "spend", "metal", "m", theme = th.id),
                WConf("t", "countdown", "pass", "m", theme = th.id, ref = Catalog.defaultRef("countdown")),
                WConf("t", "rates", "ticker", "w", theme = th.id),
            ) }
            draw(dropItems, sizes, k, File("build/drops.png"), photo = true)
        }
    }

    private fun draw(items: List<WConf>, sizes: Map<String, Pair<Float, Float>>, k: Float, f: File, photo: Boolean) {
        val gap = 16
        val colW = (370 * k).toInt()
        val cells = items.map { c ->
            val (w, h) = sizes.getValue(c.size)
            renderWidget(c, (w * k).toInt(), (h * k).toInt(), k * com.phani.widgets.widget.widgetScale(c.size, w), sample = true).bitmap to "${c.type}/${c.design}" + (c.theme?.let { " · $it" } ?: "")
        }
        val cols = 3
        val rowsH = cells.chunked(cols).map { r -> r.maxOf { it.first.height } + (24 * k).toInt() + gap }
        val out = Bitmap.createBitmap(cols * (colW + gap) + gap, rowsH.sum() + gap, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        if (photo) cv.drawPaint(Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, out.width.toFloat(), out.height.toFloat(),
            intArrayOf(Color.rgb(0x2B, 0x58, 0x76), Color.rgb(0xD3, 0x8A, 0x5C), Color.rgb(0xF2, 0xC3, 0x8B), Color.rgb(0x3A, 0x4E, 0x6E)), null, android.graphics.Shader.TileMode.CLAMP) })
        else cv.drawColor(Color.rgb(0xD7, 0xD3, 0xCA))
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12 * k; color = if (photo) Color.WHITE else Color.BLACK }
        var y = gap
        cells.chunked(cols).forEachIndexed { ri, r ->
            r.forEachIndexed { ci, (bmp, name) ->
                val x = gap + ci * (colW + gap)
                cv.drawText(name, x.toFloat(), y + 14 * k, tp)
                cv.drawBitmap(bmp, x.toFloat(), y + 20 * k, null)
            }
            y += rowsH[ri]
        }
        f.parentFile?.mkdirs()
        FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SHEET " + f.absolutePath)
    }
}
