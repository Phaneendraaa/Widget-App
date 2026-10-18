package com.phani.widgets.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.Hit
import com.phani.widgets.widget.preferredHeight
import com.phani.widgets.widget.renderWidget
import kotlinx.coroutines.delay

/** Ticks every 30 s so clocks, timers and countdowns stay current on screen. */
object Clock {
    var tick by androidx.compose.runtime.mutableLongStateOf(0L)
}

@Composable
fun ClockTicker() {
    LaunchedEffect(Unit) { while (true) { delay(30_000); Clock.tick++ } }
}

/**
 * Draws a widget exactly as it appears on the home screen (same renderer).
 * When [live], taps on its buttons run [onHit]; otherwise [onTap] gets the whole tap.
 */
@Composable
fun WidgetView(conf: WConf, modifier: Modifier = Modifier, live: Boolean = true, onTap: (() -> Unit)? = null, onHit: (Hit) -> Unit = {}) {
    val rev = AppStore.rev
    val tick = Clock.tick
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wDp = maxWidth.value
        val hDp = preferredHeight(conf, wDp)
        val density = LocalDensity.current.density
        val r = remember(conf, rev, tick, wDp, hDp, AppStore.data.wall) {
            renderWidget(conf, (wDp * density).toInt(), (hDp * density).toInt(), density)
        }
        Image(
            bitmap = r.bitmap.asImageBitmap(),
            contentDescription = conf.type,
            modifier = Modifier.fillMaxWidth().height(hDp.dp).pointerInput(r, live, onTap) {
                detectTapGestures { o ->
                    if (!live) { onTap?.invoke(); return@detectTapGestures }
                    val h = r.hits.lastOrNull { it.rect.contains(o.x, o.y) }
                    if (h != null) onHit(h) else onTap?.invoke()
                }
            },
        )
    }
}
