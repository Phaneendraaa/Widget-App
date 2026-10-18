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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.WConf
import com.phani.widgets.core.plural
import com.phani.widgets.core.subjectBy
import com.phani.widgets.core.subsMonthly
import com.phani.widgets.core.today
import com.phani.widgets.widget.Widgets

/* ---------- what each widget reads, in plain words, and where to edit it ---------- */

/** One line describing the data a widget shows, e.g. "Budget $2,400 a month · resets on the 1st". */
fun dataSummary(c: WConf): String {
    val d = AppStore.data
    return when (c.type) {
        "now" -> "Everything you track. It picks the most urgent thing."
        "spend" -> "Budget ${Fmt.money(d.budget)} a month · resets on day ${d.payday}"
        "subs" -> if (d.subs.isEmpty()) "No subscriptions yet" else "${plural(d.subs.size, "subscription")} · ${Fmt.money(subsMonthly())} a month"
        "fast" -> "Fasting goal ${d.fast.target} hours"
        "counter" -> d.counters.firstOrNull { it.id == c.ref }?.let { "${it.name} · goal ${it.goal} ${it.unit} a day" } ?: "No counter yet"
        "streak" -> d.habits.firstOrNull { it.id == c.ref }?.let { "Habit: ${it.name}" } ?: "No habit yet"
        "pace" -> d.goals.firstOrNull { it.id == c.ref }?.let { "${it.name} · ${Fmt.trimNum(it.target)}${Fmt.unit(it.unit)} by ${Fmt.short(com.phani.widgets.core.pd(it.end))}" } ?: "No goal yet"
        "countdown" -> if (c.design == "stack") "Your next 3 dates" else d.dates.firstOrNull { it.id == c.ref }?.let { "${it.name} · ${Fmt.full(com.phani.widgets.core.pd(it.date))}" } ?: "No date yet"
        "year" -> "Today's date. Nothing to set up."
        "clock" -> if (d.clocks.isEmpty()) "No cities yet" else d.clocks.take(3).joinToString(", ") { it.city }
        "next" -> "Your weekly schedule"
        "attendance" -> if (c.design == "single" && c.ref != null) "Class: ${subjectBy(c.ref)?.name ?: "—"} · minimum ${d.req}%" else "${plural(d.subjects.size, "class")} · minimum ${d.req}%"
        else -> ""
    }
}

/** Label for the edit link, or null when there is nothing to edit. */
fun dataEditLabel(c: WConf): String? = when (c.type) {
    "now", "year" -> null
    "spend" -> "Edit budget"
    "subs" -> "Edit subscriptions"
    "fast" -> "Edit fasting"
    "counter" -> "Edit counters"
    "streak" -> "Edit habits"
    "pace" -> "Edit goal"
    "countdown" -> if (c.design == "stack") "Edit dates" else "Edit date"
    "clock" -> "Edit cities"
    "next" -> "Edit schedule"
    "attendance" -> "Edit classes"
    else -> null
}

/** Opens the right place in Track for this widget's data. */
fun editData(c: WConf) {
    Sheets.custom = null
    Flow.actions = null
    val d = AppStore.data
    when (c.type) {
        "spend" -> { Nav.tab = "track"; TrackState.tab = "money"; TrackState.open("budget") }
        "subs" -> { Nav.tab = "track"; TrackState.tab = "subs"; if (d.subs.isEmpty()) TrackState.open("sub") }
        "fast" -> { Nav.tab = "track"; TrackState.tab = "fast" }
        "counter" -> { Nav.tab = "track"; TrackState.tab = "counters"; if (d.counters.isEmpty()) TrackState.open("counter") }
        "streak" -> { Nav.tab = "track"; TrackState.tab = "habits"; if (d.habits.isEmpty()) TrackState.open("habit") }
        "pace" -> { Nav.tab = "track"; TrackState.tab = "goals"; TrackState.open("goal", c.ref ?: d.goals.firstOrNull()?.id) }
        "countdown" -> { Nav.tab = "track"; TrackState.tab = "dates"; if (c.design != "stack") TrackState.open("date", c.ref ?: d.dates.firstOrNull()?.id) }
        "clock" -> { Nav.tab = "track"; TrackState.tab = "clocks" }
        "next" -> { Nav.tab = "track"; TrackState.tab = "sched"; TrackState.open("sched") }
        "attendance" -> { Nav.tab = "track"; TrackState.tab = "classes"; if (d.subjects.isEmpty()) TrackState.open("subject") }
    }
}

/** A card that says what a widget shows, with a link to change it. */
@Composable
fun DataCard(c: WConf) {
    val p = pal()
    val label = dataEditLabel(c)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SHOWS", style = T.label.copy(fontSize = 10.5.sp), color = p.muted)
            Spacer(Modifier.height(4.dp))
            Text(dataSummary(c), style = T.body, color = p.ink)
        }
        if (label != null) Text(label, style = T.smallB.copy(fontSize = 14.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = p.ink,
            modifier = Modifier.clickable { editData(c) }.padding(10.dp))
    }
}

/* ---------- My widgets (Home) ---------- */

object Flow {
    var actions by mutableStateOf<Int?>(null)
}

fun runHit(hit: com.phani.widgets.widget.Hit) {
    val r = com.phani.widgets.core.Actions.run(hit.act, hit.id, hit.value)
    if (r.openSpend) { TrackState.open("spend"); return }
    if (r.msg != null) Toaster.show(r.msg, r.undo)
}

@Composable
private fun HowItWorks() {
    val p = pal()
    Column(Modifier.fillMaxWidth().clip(CardR).background(p.card).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("How it works", style = T.bodyB.copy(fontSize = 16.sp), color = p.ink, modifier = Modifier.weight(1f))
            Text("Got it", style = T.smallB.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = p.ink,
                modifier = Modifier.clickable { AppStore.change { howSeen = true } }.padding(6.dp))
        }
        Step("1", "Pick a widget", "Choose what it shows and how it looks.") { Nav.tab = "gallery" }
        Step("2", "Add your info", "Your budget, dates, habits and so on, in Track.") { Nav.tab = "track" }
        Step("3", "Put it on your phone", "Tap a widget below, then “Put on my home screen”.") { }
    }
}

@Composable
private fun Step(n: String, title: String, sub: String, onClick: () -> Unit) {
    val p = pal()
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(p.ink), contentAlignment = Alignment.Center) {
            Text(n, style = T.smallB, color = p.paper)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.bodyB, color = p.ink)
            Text(sub, style = T.small, color = p.muted)
        }
    }
}

@Composable
fun HomeScreen() {
    val p = pal()
    val d = AppStore.data
    ClockTicker()
    TopBar("My widgets", if (d.placed.isEmpty()) "Make your first one in a minute." else "Tap a widget to change it or put it on your phone.") {
        ChipButton("New", solid = true, icon = Ic.plus) { Nav.tab = "gallery" }
    }
    if (!d.howSeen) { HowItWorks(); Spacer(Modifier.height(16.dp)) }
    if (d.sampleData) { SampleBanner("These use example data.", "Use my own") { TrackState.open("clear") }; Spacer(Modifier.height(12.dp)) }

    Text("PREVIEW OF YOUR PHONE", style = T.label.copy(fontSize = 11.sp), color = p.muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    val (wbg, wfg) = wall()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(34.dp)).background(wbg).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Fmt.now(), style = T.smallB, color = wfg)
            Text(Fmt.full(today()), style = T.smallB, color = wfg)
        }
        if (d.placed.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No widgets yet", style = T.bodyB.copy(fontSize = 17.sp), color = wfg)
                Text("Pick one and it appears here first.", style = T.small, color = wfg.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            }
        }
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
                        Box(if (conf.size != "s") Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                            WidgetView(conf, live = true, onTap = { Flow.actions = i }) { runHit(it) }
                        }
                        if (conf.size == "s" && r.size == 1) Spacer(Modifier.weight(1f))
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
                    Text(if (d.placed.isEmpty()) "Create a widget" else "Add another widget", style = T.bodyB, color = wfg)
                }
            }
        }
    }
    Row(Modifier.padding(top = 16.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WALLS.forEach { (k, v) -> Swatch(v.first, d.wall == k) { AppStore.change { wall = k } } }
    }
    Text("Pick the colour closest to your wallpaper. The Clear look uses it.", style = T.small, color = p.muted, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

/* ---------- tap a widget: what you can do with it ---------- */

@Composable
fun WidgetActionsSheet(i: Int) {
    val p = pal()
    val ctx = LocalContext.current
    val d = AppStore.data
    val conf = d.placed.getOrNull(i) ?: run { Flow.actions = null; return }
    val t = Catalog.type(conf.type)
    val close = { Flow.actions = null }
    Sheet(t.name, close) {
        val (wbg, _) = wall()
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(wbg).padding(20.dp), contentAlignment = Alignment.Center) {
            WidgetView(conf.copy(id = "act"), Modifier.widthIn(max = if (conf.size == "s") 168.dp else 340.dp), live = false)
        }
        DataCard(conf)
        ListCard {
            ListRow("Put on my home screen", "Adds it to your phone. It stays in sync with this app.", trailing = { Icon(Ic.plus, null, tint = p.ink, modifier = Modifier.size(20.dp)) }) {
                close()
                if (!Widgets.requestPin(ctx, conf)) Sheets.help = true
                else Toaster.show("Confirm on the next screen to place it")
            }
            ListRow("Change look", "Design, colours, typeface, corners", trailing = { Icon(Ic.brush, null, tint = p.ink, modifier = Modifier.size(20.dp)) }) {
                close(); Sheets.customEdit(i)
            }
            val el = dataEditLabel(conf)
            if (el != null) ListRow("Change what it shows", dataSummary(conf), trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) { editData(conf) }
            if (i > 0) ListRow("Move up", trailing = { Icon(Ic.up, null, tint = p.ink, modifier = Modifier.size(20.dp)) }) {
                AppStore.change { val x = placed.removeAt(i); placed.add(i - 1, x) }; close()
            }
            ListRow("Remove from this list", titleColor = p.warn, divider = false) {
                AppStore.change(true) { placed.removeAt(i) }; close(); Toaster.show("Widget removed", true)
            }
        }
        Text("Widgets on your phone's home screen are removed there: long-press it and drag it away.", style = T.small, color = p.muted)
    }
}
