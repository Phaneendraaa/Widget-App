package com.phani.widgets.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Same stroke icons as the prototype, from SVG path data on a 24-unit grid. */
object Ic {
    private fun make(name: String, width: Float = 1.8f, fill: Boolean = false, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                fill = if (fill) SolidColor(Color.Black) else null,
                stroke = if (fill) null else SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val home = make("home", 1.8f, false, "M4 11l8-6 8 6v8a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z")
    val grid = make("grid", 1.8f, false, "M6 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M15 4h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M6 13h12a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z")
    val track = make("track", 1.8f, false, "M5 19V9", "M12 19V5", "M19 19v-7")
    val gear = make("gear", 1.8f, false, "M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z", "M12 3v2.5", "M12 18.5V21", "M3 12h2.5", "M18.5 12H21", "M5.6 5.6l1.8 1.8", "M16.6 16.6l1.8 1.8", "M5.6 18.4l1.8-1.8", "M16.6 7.4l1.8-1.8")
    val plus = make("plus", 2f, false, "M12 5v14", "M5 12h14")
    val check = make("check", 2.4f, false, "M5 12.5l4.5 4.5L19 7.5")
    val x = make("x", 2.2f, false, "M6 6l12 12", "M18 6L6 18")
    val up = make("up", 2.2f, false, "M6 14l6-6 6 6")
    val chev = make("chev", 2f, false, "M9 6l6 6-6 6")
    val brush = make("brush", 1.9f, false, "M14 4l6 6-8.5 8.5a3 3 0 0 1-2.1.9H5v-4.4a3 3 0 0 1 .9-2.1z", "M12 6l6 6")
    val snow = make("snow", 1.8f, false, "M12 3v18", "M4.2 7.5l15.6 9", "M4.2 16.5l15.6-9")
    val lock = make("lock", 2f, false, "M7.5 11h9a2.5 2.5 0 0 1 2.5 2.5v4a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 17.5v-4A2.5 2.5 0 0 1 7.5 11z", "M8 11V8a4 4 0 0 1 8 0v3")
    val bolt = make("bolt", 1.8f, false, "M13 3L5 13.5h6L10 21l8-10.5h-6z")
    val shield = make("shield", 1.8f, false, "M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6z")
    val wifi = make("wifi", 1.8f, false, "M3 3l18 18", "M8.5 16.5a5 5 0 0 1 7 0", "M5 13a10 10 0 0 1 5-2.6", "M19 13a10 10 0 0 0-2.3-1.6", "M2 9.5a15 15 0 0 1 4-2.4", "M22 9.5A15 15 0 0 0 11 5.6")
    val palette = make("palette", 1.8f, false, "M12 3a9 9 0 1 0 0 18c1.2 0 2-.8 2-1.8 0-1.2-1-1.6-1-2.7 0-1 .8-1.5 1.8-1.5H17a4 4 0 0 0 4-4C21 6.5 17 3 12 3z")
    val play = make("play", 1f, true, "M8 5.5v13l10-6.5z")
    val stop = make("stop", 1f, true, "M9 7h6a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z")
}
