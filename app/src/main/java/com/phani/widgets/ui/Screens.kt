package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

object HomeState {
    var edit by mutableStateOf(false)
}

fun runHit(hit: com.phani.widgets.widget.Hit) {
    val r = com.phani.widgets.core.Actions.run(hit.act, hit.id, hit.value)
    if (r.openSpend) { Toaster.show("The spend keypad arrives with the Track build"); return }
    if (r.msg != null) Toaster.show(r.msg, r.undo)
}

@Composable
fun HomeScreen() {
    val p = pal()
    val d = AppStore.data
    val edit = HomeState.edit
    ClockTicker()
    TopBar("Home", if (edit) "Tap a widget to restyle it." else "Live widgets. Tap the buttons on them.") {
        ChipButton(if (edit) "Done" else "Edit", solid = edit, icon = if (edit) null else Ic.brush) { HomeState.edit = !edit }
    }
    val (wbg, wfg) = wall()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(wbg).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Fmt.now(), style = T.smallB, color = wfg)
            Text(Fmt.full(today()), style = T.smallB, color = wfg)
        }
        // pack small widgets two per row, wide and large ones full width
        val rows = mutableListOf<List<Int>>()
        var pending: Int? = null
        d.placed.forEachIndexed { i, w ->
            if (w.size == "s") {
                if (pending == null) pending = i else { rows.add(listOf(pending!!, i)); pending = null }
            } else {
                if (pending != null) { rows.add(listOf(pending!!)); pending = null }
                rows.add(listOf(i))
            }
        }
        if (pending != null) rows.add(listOf(pending!!))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    r.forEach { i ->
                        val conf = d.placed[i]
                        val full = conf.size != "s"
                        Box(if (full) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                            WidgetView(conf, live = !edit, onTap = { if (edit) Toaster.show("Restyling arrives with the customise build") }) { runHit(it) }
                            if (edit) Row(Modifier.align(Alignment.TopEnd).padding(top = 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (i > 0) EditDot(Ic.up) { AppStore.change { val x = placed.removeAt(i); placed.add(i - 1, x) } }
                                EditDot(Ic.x) { AppStore.change(true) { placed.removeAt(i) }; Toaster.show("Widget removed", true) }
                            }
                        }
                        if (!full && r.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(24.dp)).background(wfg.copy(alpha = 0.06f))
                    .border(1.5.dp, wfg.copy(alpha = 0.45f), RoundedCornerShape(24.dp)).clickable { Nav.tab = "gallery" },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.plus, null, tint = wfg, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add widget", style = T.bodyB, color = wfg)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 22.dp).height(68.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.26f)),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.5f))) }
        }
    }
    Row(Modifier.padding(top = 16.dp, start = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WALLS.forEach { (k, v) -> Swatch(v.first, d.wall == k) { AppStore.change { wall = k } } }
    }
    if (d.sampleData) {
        Spacer(Modifier.height(10.dp))
        SampleBanner("Showing example data.", "Make it yours") { Nav.tab = "track" }
    }
}

@Composable
private fun EditDot(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val p = pal()
    Box(Modifier.offset(x = 6.dp, y = (-8).dp).size(32.dp).clip(RoundedCornerShape(16.dp)).background(p.ink).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = p.paper, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun GalleryScreen() {
    TopBar("Widgets", "12 widgets · 39 designs · all offline")
    EmptyCard("The gallery is next", "Widget designs, looks and the customise screen land in the next builds.")
}

@Composable
fun TrackScreen() {
    TopBar("Track", "Everything your widgets read from.")
    EmptyCard("Track is on its way", "Money, renewals, habits, goals and the rest arrive in build 5.")
}

@Composable
fun SettingsScreen() {
    val p = pal()
    val d = AppStore.data
    TopBar("Settings", "Works offline. Nothing leaves your phone.")
    if (d.pro) Hero("PRO · LIFETIME", "Everything unlocked. Thank you.")
    else Hero("WIDGET APP PRO", "Every design, every style, unlimited widgets.", "${CUR[d.cur]?.pro ?: "$9.99"} once. No subscription.") {
        Toaster.show("Pro unlock arrives in a later build")
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

    SectionTitle("Data")
    var confirm by remember { mutableStateOf(false) }
    ListCard {
        ListRow("Load example data", trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) {
            val pro = d.pro
            val s = sampleData(d.cur); s.pro = pro; s.onboarded = true; s.theme = d.theme
            AppStore.replace(s)
            Toaster.show("Example data loaded")
        }
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
    Text("Widget App · 0.4", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 22.dp))
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
