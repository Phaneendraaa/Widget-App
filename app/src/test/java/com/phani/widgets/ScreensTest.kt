package com.phani.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Sub
import com.phani.widgets.core.WClock
import com.phani.widgets.core.WConf
import com.phani.widgets.core.today
import com.phani.widgets.core.uid
import com.phani.widgets.ui.AppShell
import com.phani.widgets.ui.AppTheme
import com.phani.widgets.ui.Clock
import com.phani.widgets.ui.Create
import com.phani.widgets.ui.Fonts
import com.phani.widgets.ui.Nav
import com.phani.widgets.widget.WFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Captures every main screen of the app into build/screens.png so the UI can be checked before a release. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun screens() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        AppStore.load(ctx); Fonts.init(ctx.assets); WFonts.init(ctx.assets)
        Clock.enabled = false
        rule.setContent { AppTheme(false) { AppShell() } }
        val shots = mutableListOf<Pair<String, Bitmap>>()
        fun shot(name: String) {
            rule.waitForIdle()
            val v = rule.activity.window.decorView
            val b = Bitmap.createBitmap(maxOf(1, v.width), maxOf(1, v.height), Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b))
            shots += name to b
        }
        fun step(block: () -> Unit) { rule.runOnIdle(block) }

        shot("Welcome")
        step { AppStore.change { onboarded = true } }; shot("Widgets (home)")
        step { com.phani.widgets.ui.Gallery.setZoom(1f) }; shot("Zoom 100%")
        step { com.phani.widgets.ui.Gallery.setZoom(1.4f) }; shot("Zoom 140%")
        step { com.phani.widgets.ui.Gallery.setZoom(0.6f) }
        step { com.phani.widgets.ui.StyleSheet.pick("midnight") }; shot("Style: Midnight")
        step { com.phani.widgets.ui.StyleSheet.pick("glass") }; shot("Style: Glass")
        step { com.phani.widgets.ui.StyleSheet.pick("paper"); com.phani.widgets.ui.StyleSheet.open = true }; shot("Choose your style")
        step { com.phani.widgets.ui.StyleSheet.open = false }
        step { com.phani.widgets.ui.Gallery.size = "s" }; shot("Widgets, small only")
        step { com.phani.widgets.ui.Gallery.size = null; Create.startFrom("countdown", "big") }; shot("Editor, new")
        step { val c = Create.conf!!; AppStore.change { dates.first { it.id == c.ref }.name = "Lisbon trip" } }; shot("Editor, filled")
        step { Create.conf = Create.conf!!.copy(design = "flip", size = "w") }; shot("Editor, Wide size")
        step { Create.commit(); Create.adding = true }; shot("Add to home screen")
        step {
            Create.close()
            AppStore.change {
                budget = 2400.0
                subs.add(Sub(uid(), "Netflix", 15.49, "month", today().plusDays(3).toString()))
                clocks.add(WClock(uid(), "London", "Europe/London")); clocks.add(WClock(uid(), "Tokyo", "Asia/Tokyo"))
                placed.add(WConf(uid(), "spend", "strip", "w", "ink", "mint"))
                placed.add(WConf(uid(), "subs", "next", "s", accent = "rose"))
            }
            Nav.screen = "mine"
        }
        shot("My widgets")
        step {
            val cid = uid()
            AppStore.change { counters.add(com.phani.widgets.core.Counter("cw", "Water", "glasses", 8, mutableMapOf())); placed.add(0, WConf(cid, "counter", "glass", "s", ref = "cw")) }
            com.phani.widgets.core.Actions.run("cnt", "cw", null, cid)
        }
        shot("After +1: Undo on widget")
        step { Nav.screen = "home"; Create.startFrom("subs", "next") }; shot("Editor, renewals")
        step { Create.close(); Create.startFrom("clock", "pair") }; shot("Editor, clock")
        step { Create.close(); Create.startFrom("streak", "big"); val c = Create.conf!!; AppStore.change { habits.first { it.id == c.ref }.apply { name = "Run"; listOf(1L, 2L, 3L, 5L, 6L).forEach { done.add(today().minusDays(it).toString()) } } } }
        rule.runOnIdle { }; shot("Editor, streak calendar")
        step { Create.close(); AppStore.change { counters.first { it.id == "cw" }.log.apply { (1L..6L).forEach { put(today().minusDays(it).toString(), (3 + it % 5).toInt()) } } }; Create.startEdit(AppStore.data.placed.first { it.type == "counter" }) }; shot("Editor, counter stats")
        step { Create.close(); Create.startFrom("spend", "number"); AppStore.change { bPeriod = "custom"; bStart = today().toString(); bEnd = today().plusDays(9).toString() }; Create.conf = Create.conf!!.copy(opt = "period") }
        shot("Editor, budget options")
        step { Create.close(); Create.startFrom("note", "sticky") }; shot("Editor, note")
        step { Create.close(); Create.startFrom("attendance", "bars") }; shot("Editor, attendance")
        step { Create.close(); Create.startFrom("next", "card") }; shot("Editor, schedule")
        step { Create.close(); Create.startFrom("moon", "moon") }; shot("Editor, moon")
        step { Create.close(); Nav.screen = "home"; com.phani.widgets.ui.Gallery.query = "glass" }; shot("Search: glass")
        step { com.phani.widgets.ui.Gallery.query = "ring" }; shot("Search: ring")
        step { com.phani.widgets.ui.Gallery.query = ""; Nav.screen = "settings" }; shot("Settings")

        val k = 0.36f
        val cw = (shots[0].second.width * k).toInt(); val ch = (shots[0].second.height * k).toInt()
        val cols = 4; val gap = 30; val lab = 50
        val rows = (shots.size + cols - 1) / cols
        val out = Bitmap.createBitmap(cols * (cw + gap) + gap, rows * (ch + lab + gap) + gap, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out); cv.drawColor(Color.rgb(0x88, 0x88, 0x88))
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 34f; color = Color.BLACK }
        shots.forEachIndexed { i, (name, b) ->
            val x = gap + (i % cols) * (cw + gap); val y = gap + (i / cols) * (ch + lab + gap)
            cv.drawText(name, x.toFloat(), (y + 36).toFloat(), tp)
            cv.drawBitmap(b, Rect(0, 0, b.width, b.height), Rect(x, y + lab, x + cw, y + lab + ch), null)
        }
        val f = File("build/screens.png"); f.parentFile?.mkdirs()
        f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
