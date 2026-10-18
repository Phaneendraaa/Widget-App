package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.CUR
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.sampleData
import com.phani.widgets.core.today

val WALLS = linkedMapOf(
    "stone" to (Color(0xFFD7D3CA) to Color(0xFF1C1C1A)),
    "night" to (Color(0xFF16171C) to Color(0xFFF3F1EB)),
    "sage" to (Color(0xFFB5C0AE) to Color(0xFF1C1C1A)),
    "dusk" to (Color(0xFF3A3F55) to Color(0xFFF3F1EB)),
    "peach" to (Color(0xFFEBC8B4) to Color(0xFF1C1C1A)),
    "sky" to (Color(0xFFAFC6DA) to Color(0xFF1C1C1A)),
)

fun wall() = WALLS[AppStore.data.wall] ?: WALLS.getValue("stone")

object GalleryState { var cat by mutableStateOf("all") }

@Composable
fun GalleryScreen() {
    val p = pal()
    val d = AppStore.data
    TopBar("New widget", "Tap one to pick its design, look and what it shows.") {
        if (d.pro) Pill("Pro", "good") else ChipButton("Get Pro", solid = true) { Sheets.showPaywall() }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.phani.widgets.core.Catalog.cats.forEach { (k, l) ->
            val on = GalleryState.cat == k
            Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (on) p.ink else Color.Transparent)
                .then(if (on) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(19.dp))).clickable { GalleryState.cat = k }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) { Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted) }
        }
    }
    val items = com.phani.widgets.core.Catalog.types.filter { GalleryState.cat == "all" || it.cat == GalleryState.cat }
    if (GalleryState.cat == "all") {
        Spacer(Modifier.height(18.dp))
        val r = com.phani.widgets.core.rightNow()
        Hero("ONLY IN WIDGET APP", "One widget that knows what needs you.", "Right now it would say: \u201c${r.h}\u201d.") { Sheets.customNew("now") }
    }
    Spacer(Modifier.height(18.dp))
    val (wbg, _) = wall()
    // two-column layout: small designs pair up, wide ones take the row
    val rows = mutableListOf<List<com.phani.widgets.core.TypeDef>>()
    var half: com.phani.widgets.core.TypeDef? = null
    items.forEach { t ->
        if (t.designs[0].size == "s") { if (half == null) half = t else { rows.add(listOf(half!!, t)); half = null } }
        else rows.add(listOf(t))
    }
    if (half != null) rows.add(listOf(half!!))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { t ->
                    val dd = t.designs[0]
                    val conf = com.phani.widgets.core.WConf("g" + t.type, t.type, dd.id, dd.size, ref = com.phani.widgets.core.Catalog.defaultRef(t.type))
                    Column(Modifier.weight(1f).clip(CardR).background(p.card).clickable { Sheets.customNew(t.type) }.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(wbg).padding(12.dp)) {
                            WidgetView(conf, live = false, onTap = { Sheets.customNew(t.type) })
                        }
                        Text(t.name, style = T.bodyB, color = p.ink, modifier = Modifier.padding(horizontal = 6.dp))
                        Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${t.designs.size} designs", style = T.small.copy(fontSize = 12.sp), color = p.muted, modifier = Modifier.weight(1f))
                            if (t.isNew) Badge("NEW", true)
                            if (t.isNew && t.pro) Spacer(Modifier.size(6.dp))
                            if (t.pro) Badge("PRO", false)
                        }
                    }
                }
                if (row.size == 1 && row[0].designs[0].size == "s") Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Badge(t: String, new: Boolean) {
    val p = pal()
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (new) Signal else p.ink).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(t, style = T.label.copy(fontSize = 10.sp), color = if (new) Color(0xFF1A0A02) else p.paper)
    }
}

@Composable
fun SettingsScreen() {
    val p = pal()
    val d = AppStore.data
    TopBar("Settings", "Works offline. Nothing leaves your phone.")
    if (d.pro) Hero("PRO · LIFETIME", "Everything unlocked. Thank you.")
    else Hero("WIDGET APP PRO", "Every design, every style, unlimited widgets.", "${CUR[d.cur]?.pro ?: "$9.99"} once. No subscription.") {
        Sheets.showPaywall()
    }

    SectionTitle("App appearance")
    Seg(listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"), d.theme) { AppStore.change { theme = it } }

    SectionTitle("Region")
    CurrencyPicker()
    Spacer(Modifier.height(12.dp))
    Text("Time", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
    Seg(listOf(false to "12-hour", true to "24-hour"), d.h24) { AppStore.change { h24 = it } }
    Spacer(Modifier.height(12.dp))
    Text("Dates", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
    Seg(listOf(false to "Oct 25", true to "25 Oct"), d.dmy) { AppStore.change { dmy = it } }

    SectionTitle("Privacy")
    ListCard {
        Feat(Ic.wifi, "No internet needed", "Every widget is calculated on your phone.")
        Feat(Ic.shield, "No account, no ads, no tracking", "Your data stays on this device and in your own phone backup.")
        Feat(Ic.bolt, "Easy on battery", "Widgets change when you tap them, or when the day or your schedule changes.")
    }

    SectionTitle("Home screen widgets")
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val onPhone = remember(AppStore.rev) { com.phani.widgets.widget.Widgets.placedCount(ctx) }
    ListCard {
        ListRow("On your home screen", if (onPhone == 0) "None yet" else "${onPhone} widget" + (if (onPhone == 1) "" else "s") + ", updated as you tap",
            trailing = { Text("$onPhone", style = T.bodyB, color = p.ink) })
        ListRow("How to add and keep them updated", divider = false, trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) { TrackState.open("help") }
    }

    SectionTitle("Data")
    var confirm by remember { mutableStateOf(false) }
    ListCard {
        ListRow("Load example data", trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) {
            val pro = d.pro
            val s = sampleData(d.cur); s.pro = pro; s.onboarded = true; s.theme = d.theme
            AppStore.replace(s)
            Toaster.show("Example data loaded")
        }
        ListRow("Show welcome again", trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) { AppStore.change { onboarded = false } }
        ListRow(if (confirm) "Tap again to delete everything" else "Delete all my data", titleColor = p.warn, divider = false,
            trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) {
            if (!confirm) { confirm = true; return@ListRow }
            confirm = false
            AppStore.change(undoable = true) {
                subs.clear(); habits.clear(); goals.clear(); dates.clear(); subjects.clear(); clocks.clear(); counters.clear()
                placed.clear(); sched.clear(); log.clear(); sampleData = false
                fast.start = null; fast.history.clear()
            }
            Toaster.show("All data deleted", canUndo = true)
        }
    }
    Text("Widget App · 0.9", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 22.dp))
}

@Composable
private fun CurrencyPicker() {
    val p = pal()
    val d = AppStore.data
    var open by remember { mutableStateOf(false) }
    Text("Currency", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
    Box {
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.card)
                .border(1.dp, p.line, RoundedCornerShape(16.dp)).clickable { open = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${CUR[d.cur]?.name ?: d.cur} (${d.cur})", style = T.body.copy(fontSize = 16.sp), color = p.ink, modifier = Modifier.weight(1f))
            Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(p.card)) {
            CUR.forEach { (k, v) ->
                DropdownMenuItem(text = { Text("${v.name} ($k)", style = T.body, color = p.ink) }, onClick = {
                    open = false
                    AppStore.change { cur = k }
                    Toaster.show("Currency set to $k")
                })
            }
        }
    }
}
