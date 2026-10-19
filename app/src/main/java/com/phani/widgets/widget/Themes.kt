package com.phani.widgets.widget

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.phani.widgets.core.WConf

/**
 * Ready-made looks. A theme sets colours, type, corners and a finishing touch (glass sheen, retro double rule,
 * neon glow…) so people only pick a theme and a size, and every combination is designed to look right.
 */
class WTheme(
    val id: String, val name: String, val blurb: String,
    val style: String, val bg: String? = null, val fg: String? = null, val accent: String,
    val font: String = "grotesk", val radius: Int = 26, val alpha: Int = 100, val deco: String? = null,
    /** Second colour for gradient backgrounds. */
    val bg2: String? = null,
)

object Themes {
    val all = listOf(
        WTheme("paper", "Paper", "Clean and minimal", "paper", accent = "signal"),
        WTheme("midnight", "Midnight", "Deep navy, calm", "paper", bg = "#0F1626", fg = "#E8ECF5", accent = "#7AA2FF", radius = 28),
        WTheme("glass", "Glass", "See-through frosted glass", "paper", bg = "#FFFFFF", fg = "#141414", accent = "#FF5A1F", alpha = 42, radius = 30, deco = "glass"),
        WTheme("glassdark", "Smoked glass", "Dark see-through glass", "paper", bg = "#0E0E10", fg = "#F5F5F7", accent = "#FFB020", alpha = 46, radius = 30, deco = "glass"),
        WTheme("modern", "Modern", "Bold gradient", "paper", bg = "#5B3DF5", bg2 = "#E0397A", fg = "#FFFFFF", accent = "#FFE14D", radius = 32, deco = "gradient"),
        WTheme("ocean", "Ocean", "Cool blue gradient", "paper", bg = "#0B6E99", bg2 = "#1FB5A8", fg = "#FFFFFF", accent = "#FFE9A8", radius = 32, deco = "gradient"),
        WTheme("sunset", "Sunset", "Warm peach glow", "paper", bg = "#FFB37A", bg2 = "#FF6B6B", fg = "#2A1408", accent = "#2A1408", radius = 32, deco = "gradient"),
        WTheme("retro", "Old school", "Typewriter on cream paper", "paper", bg = "#F2E8D5", fg = "#3B2A20", accent = "#B4442E", font = "mono", radius = 6, deco = "retro"),
        WTheme("newspaper", "Newspaper", "Serif type, printed feel", "paper", bg = "#F4F1EA", fg = "#111111", accent = "#111111", font = "serif", radius = 4, deco = "rule"),
        WTheme("neon", "Neon", "Glowing lines at night", "paper", bg = "#0A0A12", fg = "#F0EEFF", accent = "#39FF9C", radius = 22, deco = "neon"),
        WTheme("terminal", "Terminal", "Green on black, for coders", "paper", bg = "#0C100C", fg = "#B9F6B0", accent = "#4CFF6A", font = "mono", radius = 10, deco = "scan"),
        WTheme("pastel", "Pastel", "Soft and friendly", "paper", bg = "#FDE7EF", bg2 = "#E6E8FF", fg = "#2B2440", accent = "#E2557F", radius = 32, deco = "gradient"),
        WTheme("ink", "Ink", "Black and bold", "ink", accent = "signal"),
        WTheme("wallpaper", "Wallpaper", "Matches your wallpaper and dark mode", "you", accent = "signal", radius = 28),
        WTheme("clear", "No background", "Just the text, on your wallpaper", "clear", accent = "signal"),
    )

    fun get(id: String?): WTheme? = all.firstOrNull { it.id == id }

    /** The look a widget actually draws with: its theme's settings, or its own when it has no theme. */
    fun resolve(c: WConf): WConf {
        val t = get(c.theme) ?: return c
        return c.copy(style = t.style, bg = t.bg, fg = t.fg, accent = t.accent, font = t.font, radius = t.radius, alpha = t.alpha, outline = false)
    }

    private fun col(h: String?) = hexColor(h) ?: 0

    /** Behind the content: gradient fills and glass sheen. */
    fun under(c: Canvas, t: WTheme, w: Float, h: Float, k: Float) {
        when (t.deco) {
            "gradient" -> c.drawRect(0f, 0f, w, h, Paint().apply {
                shader = LinearGradient(0f, 0f, w, h, col(t.bg) or 0xFF000000.toInt(), col(t.bg2) or 0xFF000000.toInt(), Shader.TileMode.CLAMP)
            })
            "glass" -> {
                // a soft highlight in the top-left, like light catching frosted glass
                c.drawRect(0f, 0f, w, h, Paint().apply {
                    shader = LinearGradient(0f, 0f, w * 0.7f, h, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
                })
                c.drawCircle(w * 0.12f, -h * 0.1f, h * 0.9f, Paint().apply {
                    shader = RadialGradient(w * 0.12f, -h * 0.1f, h * 0.9f, 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
                })
            }
            "scan" -> {
                val p = Paint().apply { color = 0x0E000000 }
                var y = 0f
                while (y < h) { c.drawRect(0f, y, w, y + 1.2f * k, p); y += 3f * k }
            }
        }
    }

    /** On top of the content: borders and glow. */
    fun over(c: Canvas, t: WTheme, w: Float, h: Float, k: Float, r: Float, fg: Int, acc: Int) {
        val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        when (t.deco) {
            "glass" -> {
                st.strokeWidth = 1.2f * k; st.color = if (t.id == "glass") 0x99FFFFFF.toInt() else 0x40FFFFFF
                c.drawRoundRect(RectF(st.strokeWidth / 2, st.strokeWidth / 2, w - st.strokeWidth / 2, h - st.strokeWidth / 2), r, r, st)
            }
            "retro" -> {
                st.strokeWidth = 1.4f * k; st.color = (fg and 0xFFFFFF) or 0xB0000000.toInt()
                val i1 = 4f * k; val i2 = 7.5f * k
                c.drawRoundRect(RectF(i1, i1, w - i1, h - i1), 3f * k, 3f * k, st)
                st.strokeWidth = 0.8f * k
                c.drawRoundRect(RectF(i2, i2, w - i2, h - i2), 2f * k, 2f * k, st)
            }
            "rule" -> {
                st.strokeWidth = 1f * k; st.color = (fg and 0xFFFFFF) or 0x60000000
                c.drawRect(RectF(st.strokeWidth, st.strokeWidth, w - st.strokeWidth, h - st.strokeWidth), st)
            }
            "neon" -> {
                val inset = 2.5f * k
                val rr = RectF(inset, inset, w - inset, h - inset)
                st.strokeWidth = 4f * k; st.color = (acc and 0xFFFFFF) or 0x70000000
                st.maskFilter = BlurMaskFilter(6f * k, BlurMaskFilter.Blur.NORMAL)
                c.drawRoundRect(rr, r - inset, r - inset, st)
                st.maskFilter = null; st.strokeWidth = 1.4f * k; st.color = acc
                c.drawRoundRect(rr, r - inset, r - inset, st)
            }
        }
    }
}
