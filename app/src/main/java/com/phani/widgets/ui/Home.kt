package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.Actions
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.DesignDef
import com.phani.widgets.core.TypeDef
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.Hit
import com.phani.widgets.widget.Widgets

fun runHit(hit: Hit, cid: String? = null) {
    if (hit.act == "open") return
    val r = Actions.run(hit.act, hit.id, hit.value, cid)
    if (r.openSpend) { TrackState.open("spend"); return }
    if (r.msg != null) Toaster.show(r.msg, r.undo)
}

/** Gallery filters. [size] and [bindId] are set when an empty widget on the home screen asked for one. */
object Gallery {
    var cat by mutableStateOf("all")
    var size by mutableStateOf<String?>(null)
    var bindId by mutableIntStateOf(-1)
    var query by mutableStateOf("")
    /** Zoom while a pinch is under way; -1 when not pinching. */
    var pinch by mutableStateOf(-1f)
    val LEVELS = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f, 1.2f, 1.4f)
    const val MIN = 0.5f
    const val MAX = 1.4f
    /** The zoom the tabs are laid out at. */
    fun zoom(): Float = AppStore.data.zoom
    /** What the zoom control shows: the live pinch, or the saved zoom. */
    fun shown(): Float = if (pinch > 0f) pinch else AppStore.data.zoom
    fun setZoom(z: Float) = AppStore.change { zoom = z.coerceIn(MIN, MAX) }
    fun zoomIn() { val z = zoom(); setZoom(LEVELS.firstOrNull { it > z + 0.01f } ?: MAX) }
    fun zoomOut() { val z = zoom(); setZoom(LEVELS.lastOrNull { it < z - 0.01f } ?: MIN) }
}

/** Pinch with two fingers to zoom the previews; one finger still scrolls and swipes as usual. */
internal fun Modifier.pinchZoom(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
        var z = Gallery.zoom()
        var pinching = false
        do {
            val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
            if (e.changes.count { it.pressed } >= 2) {
                pinching = true
                z = (z * e.calculateZoom()).coerceIn(Gallery.MIN, Gallery.MAX)
                Gallery.pinch = z
                e.changes.forEach { it.consume() }
            }
        } while (e.changes.any { it.pressed })
        if (pinching) { Gallery.pinch = -1f; Gallery.setZoom(z) }
    }
}

/** Light or dark, one tap. Shows which one is on now (following the phone when set to Auto in Settings). */
@Composable
internal fun LightDark() {
    AppStore.rev
    val p = pal()
    val sys = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (AppStore.data.theme) { "dark" -> true; "light" -> false; else -> sys }
    Row(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(p.card).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(false to Ic.sun, true to Ic.moon).forEach { (d, ic) ->
            val on = d == dark
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) p.ink else Color.Transparent)
                .clickable { AppStore.change { theme = if (d) "dark" else "light" } }, contentAlignment = Alignment.Center) {
                Icon(ic, if (d) "Dark" else "Light", tint = if (on) p.paper else p.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** − 100% + : make everything smaller to see more at once, or bigger to read easily. Tap the number to reset. */
@Composable
internal fun ZoomControl() {
    AppStore.rev
    val p = pal()
    val z = Gallery.shown()
    Row(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(p.paper).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = z > Gallery.MIN + 0.01f) { Gallery.zoomOut() }, contentAlignment = Alignment.Center) {
            Icon(Ic.minus, "Zoom out", tint = if (z > Gallery.MIN + 0.01f) p.ink else p.muted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
        Text("${(z * 100).roundToInt()}%", style = T.smallB, color = p.ink, textAlign = TextAlign.Center,
            modifier = Modifier.width(46.dp).clip(RoundedCornerShape(10.dp)).clickable { Gallery.setZoom(1f) }.padding(vertical = 6.dp))
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = z < Gallery.MAX - 0.01f) { Gallery.zoomIn() }, contentAlignment = Alignment.Center) {
            Icon(Ic.plus, "Zoom in", tint = if (z < Gallery.MAX - 0.01f) p.ink else p.muted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SearchBar() {
    val p = pal()
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).height(48.dp).clip(RoundedCornerShape(24.dp)).background(p.card).padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Ic.search, null, tint = p.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (Gallery.query.isEmpty()) Text("Search widgets", style = T.body.copy(fontSize = 16.sp), color = p.muted)
            androidx.compose.foundation.text.BasicTextField(Gallery.query, { Gallery.query = it.take(30) }, singleLine = true,
                textStyle = T.body.copy(fontSize = 16.sp, color = p.ink), cursorBrush = androidx.compose.ui.graphics.SolidColor(p.ink), modifier = Modifier.fillMaxWidth())
        }
        if (Gallery.query.isNotEmpty()) Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable { Gallery.query = "" }, contentAlignment = Alignment.Center) {
            Icon(Ic.x, "Clear", tint = p.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TopTitle(title: String, end: (@Composable () -> Unit)? = null) {
    val p = pal()
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.h1, color = p.ink, modifier = Modifier.weight(1f))
        end?.invoke()
    }
}

/* ---------------- Gallery: every design, straight away ---------------- */

/** What people want on their screen, in plain words. Each need is one row to swipe through. */
private class Need(val title: String, val sub: String, val types: List<String>)

private val NEEDS = listOf(
    Need("Count down to a day", "Trips, birthdays, exams, launches", listOf("countdown")),
    Need("Weather & air", "Now, the next hours, the week, and the air", listOf("weather", "aqi")),
    Need("Money", "Safe to spend, renewals, rates, gold and crypto", listOf("spend", "subs", "rates")),
    Need("Habits & goals", "Streaks you keep, goals with a deadline", listOf("streak", "pace")),
    Need("Health", "Water, fasting, anything you count", listOf("counter", "fast")),
    Need("Time & date", "Today, world clocks, the year, the moon", listOf("today", "clock", "year", "moon")),
    Need("Your day", "What needs you, what's next, classes, notes", listOf("now", "next", "attendance", "note")),
)

@Composable
fun GalleryScreen() {
    AppStore.rev
    val p = pal()
    TopTitle("Widgets") { LightDark() }
    StyleCard()
    val size = Gallery.size
    if (size != null) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(16.dp)).background(p.ink).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Pick one for your ${Catalog.sizeName[size]} (${Catalog.sizeGrid[size]}) widget", style = T.smallB, color = p.paper, modifier = Modifier.weight(1f))
            Text("Show all", style = T.smallB, color = p.paper.copy(alpha = 0.7f), modifier = Modifier.clickable { Gallery.size = null; Gallery.bindId = -1 }.padding(6.dp))
        }
    }
    SearchBar()
    val q = Gallery.query.trim()
    if (q.isNotEmpty()) { SearchResults(q, size); return }
    Column {
    NEEDS.forEach { n ->
        val items = n.types.flatMap { ty -> val t = Catalog.type(ty); t.designs.filter { size == null || it.size == size }.map { t to it } }
            .sortedByDescending { it.second.sig } // Signature designs lead
        if (items.isEmpty()) return@forEach
        Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)) {
            Text(n.title, style = T.bodyB.copy(fontSize = 19.sp), color = p.ink)
            Text(n.sub, style = T.small, color = p.muted)
        }
        // the row runs to the screen edges, so the next card peeks in and invites a swipe
        androidx.compose.foundation.lazy.LazyRow(
            Modifier.fillMaxWidth().padding(bottom = 20.dp).layout { m, c ->
                val extra = Pad.roundToPx() * 2
                val pl = m.measure(c.copy(minWidth = c.maxWidth + extra, maxWidth = c.maxWidth + extra))
                layout(c.maxWidth, pl.height) { pl.place(-extra / 2, 0) }
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Pad),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items.size) { i ->
                val (t, d) = items[i]
                DesignCard(t, d, Modifier.width(if (d.size == "s") 148.dp else 252.dp), boxH = 150.dp, zoom = 0.7f)
            }
        }
    }
    }
}

/** Search finds widgets by what they do, their design and size, and styles by name. */
@Composable
private fun SearchResults(q: String, size: String?) {
    val p = pal()
    val styles = com.phani.widgets.widget.Themes.offered.filter { it.name.contains(q, true) || it.blurb.contains(q, true) }
    if (styles.isNotEmpty()) {
        Text("Styles", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            styles.forEach { t ->
                Row(Modifier.clip(RoundedCornerShape(18.dp)).background(p.card).clickable { StyleSheet.pick(t.id); Toaster.show("Your widgets now use ${t.name}") }
                    .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Swatch(t, 24.dp)
                    Text(t.name, style = T.smallB, color = p.ink, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    val items = Catalog.visible.flatMap { t -> t.designs.filter { size == null || it.size == size }.map { t to it } }
        .filter { (t, d) -> listOf(t.name, d.name, t.desc, typeBlurb(t.type), Catalog.sizeName[d.size] ?: "").any { it.contains(q, ignoreCase = true) } }
        .sortedByDescending { it.second.sig }
    Text(if (items.isEmpty()) "No widgets match \u201c$q\u201d" else "${items.size} result" + (if (items.size == 1) "" else "s"),
        style = T.small, color = p.muted, modifier = Modifier.padding(bottom = 10.dp))
    // zoomed out: more per row; zoomed in: fewer, bigger
    val z = Gallery.zoom()
    val smallCols = if (z < 0.8f) 3 else if (z > 1.3f) 1 else 2
    val wideCols = if (z < 0.8f) 2 else 1
    val rows = mutableListOf<MutableList<Pair<TypeDef, DesignDef>>>()
    var curS: MutableList<Pair<TypeDef, DesignDef>>? = null
    var curW: MutableList<Pair<TypeDef, DesignDef>>? = null
    for (it2 in items) {
        if (it2.second.size == "s") {
            if (curS == null || curS.size >= smallCols) { curS = mutableListOf(); rows.add(curS) }
            curS.add(it2)
        } else {
            if (curW == null || curW.size >= wideCols) { curW = mutableListOf(); rows.add(curW) }
            curW.add(it2)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            val cols = if (row[0].second.size == "s") smallCols else wideCols
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, d) -> DesignCard(t, d, Modifier.weight(1f)) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DesignCard(t: TypeDef, d: DesignDef, modifier: Modifier, boxH: androidx.compose.ui.unit.Dp? = null, zoom: Float = 0.78f) {
    AppStore.rev
    val p = pal()
    // every design is shown in the user's own style, so what they see is what they get
    val conf = WConf("g${t.type}${d.id}", t.type, d.id, d.size, theme = com.phani.widgets.widget.Themes.APP)
    val open = { Create.startFrom(t.type, d.id, Gallery.bindId, conf.theme); Gallery.bindId = -1; Gallery.size = null }
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(p.card).clickable { open() }) {
        Box(Modifier.fillMaxWidth().then(if (boxH != null) Modifier.height(boxH) else Modifier).background(themeBackdrop(AppStore.look)).padding(10.dp),
            contentAlignment = Alignment.Center) {
            Zoomed(conf, zoom, maxH = boxH?.let { it.value - 20f }) { open() }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (d.sig) {
                Text("✦ " + d.name, style = T.smallB.copy(fontSize = 14.sp), color = p.ink, maxLines = 1)
                Text("${t.name} · ${Catalog.sizeGrid[d.size]}", style = T.small.copy(fontSize = 12.sp), color = p.muted, maxLines = 1)
            } else {
                Text(if (d.size == "s") t.name else "${t.name} · ${d.name}", style = T.smallB.copy(fontSize = 14.sp), color = p.ink, maxLines = 1)
                Text(if (d.size == "s") "${d.name} · ${Catalog.sizeGrid[d.size]}" else "${Catalog.sizeGrid[d.size]} · ${typeBlurb(t.type)}", style = T.small.copy(fontSize = 12.sp), color = p.muted, maxLines = 1)
            }
        }
    }
}

/** Draws a widget at its normal size, then shrinks it like zooming out, so nothing inside re-flows or breaks. */
@Composable
private fun Zoomed(c: WConf, zoom: Float, maxH: Float? = null, onTap: () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val nw = if (c.size == "s") 160f else 330f
        val nh = com.phani.widgets.widget.preferredHeight(c, nw)
        val k = minOf(zoom, maxWidth.value / nw, maxH?.let { it / nh } ?: zoom)
        Box(Modifier.size((nw * k).dp, (nh * k).dp), contentAlignment = Alignment.Center) {
            WidgetView(c, Modifier.requiredSize(nw.dp, nh.dp).graphicsLayer(scaleX = k, scaleY = k), live = false, sample = true, onTap = onTap)
        }
    }
}

/* ---------------- My widgets ---------------- */

@Composable
fun MineScreen() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    TopTitle("My widgets") { LightDark() }
    if (d.placed.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nothing here yet", style = T.h3, color = p.ink)
            Text("Pick a design in Widgets, fill in your details and add it to your home screen.", style = T.body, color = p.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            BigButton("Browse widgets") { Nav.screen = "home" }
        }
        return
    }
    val live = d.placed.count { Widgets.isOnPhone(it.id) }
    Text("${d.placed.size} saved · $live on your home screen", style = T.small, color = p.muted, modifier = Modifier.padding(bottom = 14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        d.placed.forEach { WidgetCard(it) }
    }
}

@Composable
private fun WidgetCard(c: WConf) {
    AppStore.rev
    val p = pal()
    val on = Widgets.isOnPhone(c.id)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.card).clickable { Create.startEdit(c) }) {
        Box(Modifier.fillMaxWidth().background(themeBackdrop(com.phani.widgets.widget.Themes.idOf(c.theme) ?: "paper")).padding(14.dp), contentAlignment = Alignment.Center) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            WidgetView(c, Modifier.widthIn(max = if (c.size == "s") 160.dp else 330.dp), live = true, onTap = { Create.startEdit(c) }) { h ->
                if (h.act == "spend-open" || h.act == "fast") ctx.startActivity(com.phani.widgets.QuickActivity.intent(ctx, if (h.act == "fast") "fast" else "spend", c.ref, c.id))
                else runHit(h, c.id)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(widgetName(c), style = T.bodyB, color = p.ink, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(if (on) p.good else p.muted.copy(alpha = 0.5f)))
                    Spacer(Modifier.width(6.dp))
                    Text(if (on) "On your home screen" else "Not on your home screen", style = T.small, color = p.muted)
                }
            }
            if (!on) Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(p.ink)
                .clickable { Create.startEdit(c, add = true) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("Add", style = T.smallB, color = p.paper)
            }
            else Text("Edit", style = T.smallB, color = p.ink, modifier = Modifier.clickable { Create.startEdit(c) }.padding(10.dp))
        }
    }
}

/* ---------------- bottom bar ---------------- */

@Composable
fun BottomTabs() {
    val p = pal()
    Row(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(p.nav).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Triple("home", "Widgets", Ic.grid), Triple("mine", "My widgets", Ic.home), Triple("settings", "Settings", Ic.gear)).forEach { (k, l, ic) ->
            val on = Nav.screen == k
            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp)).background(if (on) p.ink else Color.Transparent).clickable { Nav.screen = k },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(ic, l, tint = if (on) p.paper else p.muted, modifier = Modifier.size(20.dp))
                Text(l, style = T.smallB.copy(fontSize = 12.sp), color = if (on) p.paper else p.muted, maxLines = 1)
            }
        }
    }
}


/* ---------------- starter packs ---------------- */

