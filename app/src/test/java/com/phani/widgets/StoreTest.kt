package com.phani.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.WConf
import com.phani.widgets.core.sampleData
import com.phani.widgets.ui.AppShell
import com.phani.widgets.ui.AppTheme
import com.phani.widgets.ui.Clock
import com.phani.widgets.ui.Create
import com.phani.widgets.ui.Fonts
import com.phani.widgets.ui.Nav
import com.phani.widgets.widget.WFonts
import com.phani.widgets.widget.renderWidget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Full-resolution screens and widgets for the Play Store listing (build/store/). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class StoreTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val dir = File("build/store").apply { mkdirs() }
    private fun save(b: Bitmap, name: String) = File(dir, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    @Test
    fun store() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        AppStore.load(ctx); Fonts.init(ctx.assets); WFonts.init(ctx.assets)
        Clock.enabled = false
        // a lived-in app: the example data, with a set of widgets saved
        val d = sampleData("USD").also {
            it.onboarded = true; it.zoom = 0.9f; it.look = "paper"
            it.placed.clear()
            it.placed.addAll(listOf(
                WConf("p1", "countdown", "pass", "m", theme = "app", ref = "d1"),
                WConf("p2", "streak", "flame", "s", theme = "app", ref = "h1"),
                WConf("p3", "counter", "liquid", "s", theme = "app", ref = "c1"),
                WConf("p4", "spend", "metal", "m", theme = "app"),
                WConf("p5", "weather", "skyhours", "m", theme = "app"),
            ))
        }
        AppStore.replace(d)

        // single widgets, large, for the listing artwork
        val widgets = mapOf(
            "w_pass" to WConf("x", "countdown", "pass", "m", theme = "paper", ref = "d1"),
            "w_metal" to WConf("x", "spend", "metal", "m", theme = "ink"),
            "w_metal_paper" to WConf("x", "spend", "metal", "m", theme = "paper"),
            "w_flame" to WConf("x", "streak", "flame", "s", theme = "paper", ref = "h1"),
            "w_liquid" to WConf("x", "counter", "liquid", "s", theme = "paper", ref = "c1"),
            "w_led" to WConf("x", "countdown", "led", "w", theme = "paper", ref = "d1"),
            "w_word" to WConf("x", "clock", "word", "s", theme = "paper"),
            "w_tear" to WConf("x", "today", "tear", "s", theme = "paper"),
            "w_night" to WConf("x", "moon", "night", "s", theme = "paper"),
            "w_sky" to WConf("x", "weather", "sky", "s", theme = "paper"),
            "w_skyhours" to WConf("x", "weather", "skyhours", "m", theme = "paper"),
            "w_skytall" to WConf("x", "weather", "skytall", "t", theme = "paper"),
            "w_aqi" to WConf("x", "aqi", "gauge", "s", theme = "paper"),
            "w_rates" to WConf("x", "rates", "list", "m", theme = "midnight"),
            "w_ticker" to WConf("x", "rates", "ticker", "w", theme = "midnight"),
            "w_tiles" to WConf("x", "subs", "tiles", "m", theme = "paper"),
            "w_protein" to WConf("x", "protein", "ring", "s", theme = "paper", ref = "cp"),
            "w_bottle" to WConf("x", "counter", "bottle", "t", theme = "paper", ref = "c1"),
            "w_tube" to WConf("x", "fast", "tube", "t", theme = "paper"),
            "w_meds" to WConf("x", "meds", "list", "m", theme = "paper"),
            "w_calm" to WConf("x", "calm", "tall", "t", theme = "midnight", ref = "cm"),
            "w_calm_s" to WConf("x", "calm", "breathe", "s", theme = "midnight", ref = "cm"),
            "w_prayer" to WConf("x", "prayer", "list", "m", theme = "midnight"),
            "w_prayer_s" to WConf("x", "prayer", "next", "s", theme = "midnight"),
            "w_heat" to WConf("x", "streak", "heat", "m", theme = "paper", ref = "h1"),
            "w_climb" to WConf("x", "pace", "climb", "m", theme = "paper", ref = "g1"),
            "w_polaroid" to WConf("x", "note", "polaroid", "s", theme = "paper"),
            "w_diwali" to WConf("x", "countdown", "big", "s", theme = "neon", ref = "d1"),
        )
        val dens = 4f
        widgets.forEach { (name, c) ->
            val wDp = if (c.size == "s" || c.size == "t") 160f else 330f
            val hDp = com.phani.widgets.widget.preferredHeight(c, wDp)
            save(renderWidget(c, (wDp * dens).toInt(), (hDp * dens).toInt(), dens).bitmap, name)
        }

        rule.setContent { AppTheme(AppStore.data.theme == "dark") { AppShell() } }
        fun shot(name: String) {
            rule.waitForIdle()
            val v = rule.activity.window.decorView
            val b = Bitmap.createBitmap(maxOf(1, v.width), maxOf(1, v.height), Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b))
            save(b, name)
        }
        fun step(block: () -> Unit) { rule.runOnIdle(block) }

        shot("s_gallery")
        step { Nav.screen = "mine" }; shot("s_mine")
        step { Nav.screen = "home"; com.phani.widgets.ui.StyleSheet.open = true }; shot("s_styles")
        step { com.phani.widgets.ui.StyleSheet.open = false; Create.startEdit(AppStore.data.placed.first { it.id == "p1" }) }; shot("s_editor")
        step { Create.close(); com.phani.widgets.ui.StyleSheet.pick("midnight") }; shot("s_gallery_midnight")
        step { com.phani.widgets.ui.StyleSheet.pick("paper"); Nav.screen = "settings" }; shot("s_settings")
    }
}
