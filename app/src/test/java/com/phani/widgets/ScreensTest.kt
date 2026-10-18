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
        step { com.phani.widgets.ui.Gallery.cat = "countdown" }; shot("Widgets, Countdown tab")
        step { com.phani.widgets.ui.Gallery.cat = "all"; Create.startFrom("countdown", "big") }; shot("Editor, new")
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
        step { Nav.screen = "home"; Create.startFrom("subs", "next") }; shot("Editor, renewals")
        step { Create.close(); Create.startFrom("clock", "pair") }; shot("Editor, clock")
        step { Create.close(); Nav.screen = "settings" }; shot("Settings")

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
