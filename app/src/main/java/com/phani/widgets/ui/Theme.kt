@file:OptIn(ExperimentalTextApi::class)

package com.phani.widgets.ui

import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Quiet Ink: paper and ink, one signal colour. Same tokens as the web prototype. */
@Immutable
data class Pal(
    val paper: Color, val card: Color, val ink: Color, val muted: Color, val line: Color, val soft: Color,
    val warn: Color, val good: Color, val scrim: Color, val nav: Color, val dark: Boolean,
)

val LightPal = Pal(
    paper = Color(0xFFEEECE6), card = Color(0xFFFFFFFF), ink = Color(0xFF121211), muted = Color(0xFF5D5952),
    line = Color(0x17121211), soft = Color(0xFFE3E0D9), warn = Color(0xFFB93D0A), good = Color(0xFF1D7448),
    scrim = Color(0x6B121211), nav = Color(0xF2FFFFFF), dark = false,
)
val DarkPal = Pal(
    paper = Color(0xFF0D0D0C), card = Color(0xFF1A1A18), ink = Color(0xFFF2F0EA), muted = Color(0xFFA8A49C),
    line = Color(0x17FFFFFF), soft = Color(0xFF2A2926), warn = Color(0xFFFF8B52), good = Color(0xFF53C08A),
    scrim = Color(0x99000000), nav = Color(0xF21E1E1C), dark = true,
)

val Signal = Color(0xFFFF5A1F)

object Fonts {
    lateinit var ui: FontFamily
    lateinit var mono: FontFamily
    lateinit var serif: FontFamily
    private var ready = false

    fun init(a: AssetManager) {
        if (ready) return
        fun v(path: String, w: Int) = Font(path, a, FontWeight(w), FontStyle.Normal, FontVariation.Settings(FontVariation.weight(w)))
        ui = FontFamily(listOf(400, 500, 600, 700).map { v("fonts/Geist.ttf", it) })
        mono = FontFamily(listOf(400, 500, 600).map { v("fonts/GeistMono.ttf", it) })
        serif = FontFamily(
            Font("fonts/InstrumentSerif.ttf", a, FontWeight.Normal, FontStyle.Normal),
            Font("fonts/InstrumentSerif-Italic.ttf", a, FontWeight.Normal, FontStyle.Italic),
        )
        ready = true
    }
}

val LocalPal = staticCompositionLocalOf { LightPal }

object T {
    val h1 get() = TextStyle(fontFamily = Fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-1.1).sp, lineHeight = 34.sp)
    val h3 get() = TextStyle(fontFamily = Fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.4).sp)
    val body get() = TextStyle(fontFamily = Fonts.ui, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val bodyB get() = body.copy(fontWeight = FontWeight.SemiBold)
    val small get() = TextStyle(fontFamily = Fonts.ui, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val smallB get() = small.copy(fontWeight = FontWeight.SemiBold)
    val label get() = TextStyle(fontFamily = Fonts.mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.4.sp)
    val nav get() = TextStyle(fontFamily = Fonts.ui, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
}

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPal provides if (dark) DarkPal else LightPal, content = content)
}
