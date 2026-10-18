package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phani.widgets.core.Actions
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.Hit
import com.phani.widgets.widget.Widgets

fun runHit(hit: Hit) {
    val r = Actions.run(hit.act, hit.id, hit.value)
    if (r.openSpend) { TrackState.open("spend"); return }
    if (r.msg != null) Toaster.show(r.msg, r.undo)
}

@Composable
fun HomeScreen() {
    val p = pal()
    val d = AppStore.data
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Widgets", style = T.h1, color = p.ink, modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(p.card).clickable { Nav.screen = "settings" }, contentAlignment = Alignment.Center) {
            Icon(Ic.gear, "Settings", tint = p.ink, modifier = Modifier.size(22.dp))
        }
    }
    if (d.placed.isEmpty()) { EmptyHome(); return }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        d.placed.forEach { WidgetCard(it) }
    }
}

@Composable
private fun EmptyHome() {
    val p = pal()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val (wbg, _) = wall()
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(wbg).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { WidgetView(WConf("e1", "countdown", "big", "s", accent = "cobalt"), live = false, sample = true) }
            Box(Modifier.weight(1f)) { WidgetView(WConf("e2", "streak", "big", "s", "ink"), live = false, sample = true) }
        }
        Spacer(Modifier.height(6.dp))
        Text("Make your first widget", style = T.h3, color = p.ink)
        Text("Pick one, add your info, style it, and put it on your home screen.", style = T.body, color = p.muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.height(6.dp))
        BigButton("Create a widget", icon = Ic.plus) { Create.startNew() }
    }
}

@Composable
private fun WidgetCard(c: WConf) {
    val p = pal()
    val on = Widgets.isOnPhone(c.id)
    val (wbg, _) = wall()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(p.card).clickable { Create.startEdit(c) }.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(wbg).padding(16.dp), contentAlignment = Alignment.Center) {
            WidgetView(c, Modifier.widthIn(max = if (c.size == "s") 160.dp else 330.dp), live = true, onTap = { Create.startEdit(c) }) { runHit(it) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(widgetName(c), style = T.bodyB, color = p.ink, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(if (on) p.good else p.muted.copy(alpha = 0.5f)))
                    Spacer(Modifier.width(6.dp))
                    Text(if (on) "On your home screen" else "Not on your home screen yet", style = T.small, color = p.muted)
                }
            }
            if (!on) Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(p.ink)
                .clickable { Create.startEdit(c); Create.step = 3 }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("Add", style = T.smallB, color = p.paper)
            }
            else Text("Edit", style = T.smallB, color = p.ink, modifier = Modifier.clickable { Create.startEdit(c) }.padding(10.dp))
        }
    }
}

/** Name of the home-screen size, for help text. */
fun sizeLabel(size: String) = "${Catalog.sizeName[size]} (${Catalog.sizeGrid[size]})"
