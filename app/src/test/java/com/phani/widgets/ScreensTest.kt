package com.phani.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
    @get:Rule val rule = createComposeRule()

    @Test
    fun screens() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        AppStore.load(ctx); Fonts.init(ctx.assets); WFonts.init(ctx.assets)
        Clock.enabled = false
        rule.setContent { AppTheme(false) { AppShell() } }
        val shots = mutableListOf<Pair<String, Bitmap>>()
        fun shot(name: String) {
            rule.waitForIdle()
            shots += name to rule.onRoot().captureToImage().asAndroidBitmap()
        }
        fun step(block: () -> Unit) { rule.runOnIdle(block) }

        shot("Welcome")
        step { AppStore.change { onboarded = true } }; shot("Home, empty")
        step { Create.startNew() }; shot("1 Pick")
        step { Create.pick("countdown") }; shot("2 Info, blank")
        step { val c = Create.conf!!; AppStore.change { dates.first { it.id == c.ref }.name = "Lisbon trip" } }; shot("2 Info, filled")
        step { Create.step = 2 }; shot("3 Style")
        step { Create.commit(); Create.step = 3 }; shot("4 Add")
        step {
            Create.close()
            AppStore.change {
                budget = 2400.0
                subs.add(Sub(uid(), "Netflix", 15.49, "month", today().plusDays(3).toString()))
                clocks.add(WClock(uid(), "London", "Europe/London")); clocks.add(WClock(uid(), "Tokyo", "Asia/Tokyo"))
                placed.add(WConf(uid(), "spend", "strip", "w", "ink", "mint"))
                placed.add(WConf(uid(), "subs", "next", "s", accent = "rose"))
                placed.add(WConf(uid(), "clock", "pair", "w", "paper", "cobalt"))
            }
        }
        shot("Home, with widgets")
        step { Create.startNew(); Create.pick("subs") }; shot("2 Info, renewals")
        step { Create.close(); Create.startNew(); Create.pick("clock") }; shot("2 Info, clock")
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
