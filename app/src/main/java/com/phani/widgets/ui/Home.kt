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

fun runHit(hit: Hit) {
    val r = Actions.run(hit.act, hit.id, hit.value)
    if (r.openSpend) { TrackState.open("spend"); return }
    if (r.msg != null) Toaster.show(r.msg, r.undo)
}

/** Gallery filters. [size] and [bindId] are set when an empty widget on the home screen asked for one. */
object Gallery {
    var cat by mutableStateOf("all")
    var size by mutableStateOf<String?>(null)
    var bindId by mutableIntStateOf(-1)
    var query by mutableStateOf("")
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
private fun TopTitle(title: String) {
    val p = pal()
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.h1, color = p.ink, modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(p.card).clickable { Nav.screen = "settings" }, contentAlignment = Alignment.Center) {
            Icon(Ic.gear, "Settings", tint = p.ink, modifier = Modifier.size(22.dp))
        }
    }
}

/* ---------------- Gallery: every design, straight away ---------------- */

@Composable
fun GalleryScreen() {
    AppStore.rev
    val p = pal()
    TopTitle("Widgets")
    val size = Gallery.size
    if (size != null) {
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(16.dp)).background(p.ink).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Pick one for your ${Catalog.sizeName[size]} (${Catalog.sizeGrid[size]}) widget", style = T.smallB, color = p.paper, modifier = Modifier.weight(1f))
            Text("Show all", style = T.smallB, color = p.paper.copy(alpha = 0.7f), modifier = Modifier.clickable { Gallery.size = null; Gallery.bindId = -1 }.padding(6.dp))
        }
    }
    SearchBar()
    // category tabs: the row slides along by itself so the next tabs come into view
    val cats = listOf("all" to "All") + Catalog.visible.map { it.type to it.name }
    val tabState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(Gallery.cat) {
        val i = cats.indexOfFirst { it.first == Gallery.cat }.coerceAtLeast(0)
        tabState.animateScrollToItem(maxOf(0, i - 1))
    }
    androidx.compose.foundation.lazy.LazyRow(state = tabState, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cats.size) { idx ->
            val (k, l) = cats[idx]
            val on = Gallery.cat == k
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) p.ink else p.card).clickable { Gallery.cat = k }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center) { Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.ink) }
        }
    }
    val q = Gallery.query.trim()
    val items = Catalog.visible.filter { q.isNotEmpty() || Gallery.cat == "all" || it.type == Gallery.cat }
        .flatMap { t -> t.designs.filter { size == null || it.size == size }.map { t to it } }
        .filter { (t, d) -> q.isEmpty() || listOf(t.name, d.name, t.desc, typeBlurb(t.type), Catalog.sizeName[d.size] ?: "").any { it.contains(q, ignoreCase = true) } }
    if (q.isNotEmpty()) Text(if (items.isEmpty()) "No widgets match \u201c$q\u201d" else "${items.size} result" + (if (items.size == 1) "" else "s"),
        style = T.small, color = p.muted, modifier = Modifier.padding(bottom = 10.dp))
    if (items.isEmpty()) { if (q.isEmpty()) Text("No designs in this size here yet.", style = T.body, color = p.muted); return }
    // small designs pair up, wide and large take the whole row
    val rows = mutableListOf<List<Pair<TypeDef, DesignDef>>>()
    var half: Pair<TypeDef, DesignDef>? = null
    items.forEach { it2 ->
        if (it2.second.size == "s") { if (half == null) half = it2 else { rows.add(listOf(half!!, it2)); half = null } }
        else rows.add(listOf(it2))
    }
    half?.let { rows.add(listOf(it)) }
    Column(Modifier.pointerInput(Unit) {
        var dx = 0f
        detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = {
            if (Gallery.query.isBlank() && kotlin.math.abs(dx) > 120f) {
                val i = cats.indexOfFirst { it.first == Gallery.cat }.coerceAtLeast(0)
                val j = (if (dx < 0) i + 1 else i - 1).coerceIn(0, cats.size - 1)
                Gallery.cat = cats[j].first
            }
        }) { _, d -> dx += d }
    }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, d) -> DesignCard(t, d, Modifier.weight(1f)) }
                if (row.size == 1 && row[0].second.size == "s") Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DesignCard(t: TypeDef, d: DesignDef, modifier: Modifier) {
    val p = pal()
    val (wbg, _) = wall()
    val conf = WConf("g${t.type}${d.id}", t.type, d.id, d.size, if (Math.floorMod((t.type + d.id).hashCode(), 3) == 0) "ink" else "paper")
    val open = { Create.startFrom(t.type, d.id, Gallery.bindId); Gallery.bindId = -1; Gallery.size = null }
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(p.card).clickable { open() }) {
        Box(Modifier.fillMaxWidth().background(wbg).padding(10.dp), contentAlignment = Alignment.Center) {
            Zoomed(conf, 0.78f) { open() }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(if (d.size == "s") t.name else "${t.name} · ${d.name}", style = T.smallB.copy(fontSize = 14.sp), color = p.ink, maxLines = 1)
            Text(if (d.size == "s") "${d.name} · ${Catalog.sizeGrid[d.size]}" else "${Catalog.sizeGrid[d.size]} · ${typeBlurb(t.type)}", style = T.small.copy(fontSize = 12.sp), color = p.muted, maxLines = 1)
        }
    }
}

/** Draws a widget at its normal size, then shrinks it like zooming out, so nothing inside re-flows or breaks. */
@Composable
private fun Zoomed(c: WConf, zoom: Float, onTap: () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val nw = if (c.size == "s") 160f else 330f
        val k = minOf(zoom, maxWidth.value / nw)
        val nh = com.phani.widgets.widget.preferredHeight(c, nw)
        Box(Modifier.size((nw * k).dp, (nh * k).dp), contentAlignment = Alignment.Center) {
            WidgetView(c, Modifier.requiredWidth(nw.dp).graphicsLayer(scaleX = k, scaleY = k), live = false, sample = true, onTap = onTap)
        }
    }
}

/* ---------------- My widgets ---------------- */

@Composable
fun MineScreen() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    TopTitle("My widgets")
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
    val (wbg, _) = wall()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.card).clickable { Create.startEdit(c) }) {
        Box(Modifier.fillMaxWidth().background(wbg).padding(14.dp), contentAlignment = Alignment.Center) {
            WidgetView(c, Modifier.widthIn(max = if (c.size == "s") 160.dp else 330.dp), live = true, onTap = { Create.startEdit(c) }) { runHit(it) }
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
    Row(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(p.nav).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Triple("home", "Widgets", Ic.grid), Triple("mine", "My widgets", Ic.home)).forEach { (k, l, ic) ->
            val on = Nav.screen == k
            Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (on) p.ink else Color.Transparent).clickable { Nav.screen = k },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(ic, null, tint = if (on) p.paper else p.muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted)
            }
        }
    }
}
