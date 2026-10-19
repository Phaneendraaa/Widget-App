package com.phani.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Counter
import com.phani.widgets.core.Goal
import com.phani.widgets.core.Habit
import com.phani.widgets.core.today
import com.phani.widgets.ui.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Captures each quick panel (opened from a widget) into build/quick.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class QuickTest {
    @Test
    fun panels() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        AppStore.load(ctx); Clock.enabled = false
        AppStore.change {
            onboarded = true; budget = 2400.0
            counters.add(Counter("c1", "Water", "glasses", 8, mutableMapOf(today().toString() to 3)))
            habits.add(Habit("h1", "Run", 5, 1, mutableSetOf(today().minusDays(1).toString()), mutableSetOf()))
            dates.add(com.phani.widgets.core.DateItem("d1", "Goa trip", today().plusDays(40).toString(), false))
            placed.add(com.phani.widgets.core.WConf("x", "counter", "glass", "s", ref = "c1"))
            placed.add(com.phani.widgets.core.WConf("y", "countdown", "big", "s", ref = "d1"))
            counters.firstOrNull()?.log?.apply { (1L..6L).forEach { put(today().minusDays(it).toString(), (2 + it % 6).toInt()) } }
            goals.add(Goal("g1", "Books this year", "books", 24.0, 9.0, today().minusDays(100).toString(), today().plusDays(60).toString(), 1.0))
        }
        val shots = mutableListOf<Pair<String, Bitmap>>()
        fun cap(name: String, kind: String, ref: String?, cid: String = "x") {
            val c = Robolectric.buildActivity(QuickActivity::class.java, QuickActivity.intent(ctx, kind, ref, cid)).setup()
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            val v = c.get().window.decorView
            val b = Bitmap.createBitmap(maxOf(1, v.width), maxOf(1, v.height), Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b)); shots += name to b
            c.pause().stop().destroy()
        }
        cap("Spend", "spend", null)
        cap("Fast: start", "fast", null)
        AppStore.change { fast.start = System.currentTimeMillis() - 5 * 3_600_000 }
        cap("Fast: running", "fast", null)
        cap("Counter", "counter", "c1")
        cap("Streak", "habit", "h1")
        cap("Pace", "goal", "g1")
        cap("Stats: countdown", "stats", null, "y")
        val k = 0.36f
        val cw = (shots[0].second.width * k).toInt(); val ch = (shots[0].second.height * k).toInt()
        val cols = 3; val gap = 30; val lab = 50
        val rows = (shots.size + cols - 1) / cols
        val out = Bitmap.createBitmap(cols * (cw + gap) + gap, rows * (ch + lab + gap) + gap, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out); cv.drawColor(Color.rgb(0x88, 0x88, 0x88))
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 34f; color = Color.BLACK }
        shots.forEachIndexed { i, (name, b) ->
            val x = gap + (i % cols) * (cw + gap); val y = gap + (i / cols) * (ch + lab + gap)
            cv.drawText(name, x.toFloat(), (y + 36).toFloat(), tp)
            cv.drawBitmap(b, Rect(0, 0, b.width, b.height), Rect(x, y + lab, x + cw, y + lab + ch), null)
        }
        val f = File("build/quick.png"); f.parentFile?.mkdirs()
        f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
