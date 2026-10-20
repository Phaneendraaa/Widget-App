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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Live
import com.phani.widgets.core.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Pick the city for weather and air quality: type a few letters, tap the right one. */
@Composable
fun PlaceField() {
    AppStore.rev
    val p = pal()
    val ctx = LocalContext.current
    val cur = AppStore.data.place
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { Live.refresh(ctx) }
    LaunchedEffect(q) {
        val t = q.trim()
        if (t.length < 2) { results = emptyList(); failed = false; return@LaunchedEffect }
        delay(350)
        searching = true
        val r = withContext(Dispatchers.IO) { Live.searchPlaces(t) }
        results = r; failed = r.isEmpty(); searching = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("City")
        if (cur != null) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cur.name, style = T.bodyB, color = p.ink)
                    if (cur.area.isNotBlank()) Text(cur.area, style = T.small, color = p.muted)
                }
                Text(AppStore.data.wx?.let { "Updated " + Live.ago(it.at) } ?: "Updating…", style = T.small, color = p.muted)
            }
        }
        Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(p.card).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.search, null, tint = p.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) Text(if (cur == null) "Search for your city" else "Change city", style = T.body, color = p.muted)
                BasicTextField(q, { q = it.take(40) }, singleLine = true, textStyle = T.body.copy(color = p.ink), cursorBrush = SolidColor(p.ink),
                    modifier = Modifier.fillMaxWidth())
            }
        }
        if (searching) Text("Searching…", style = T.small, color = p.muted)
        else if (failed && q.trim().length >= 2) Text("No city found. Check the spelling, or your internet.", style = T.small, color = p.muted)
        results.forEach { pl ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card).clickable {
                AppStore.change { place = pl; wx = null; aq = null }
                q = ""; results = emptyList()
                Live.refresh(ctx, force = true)
            }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pl.name, style = T.smallB, color = p.ink)
                    if (pl.area.isNotBlank()) Text(pl.area, style = T.small.copy(fontSize = 12.sp), color = p.muted)
                }
                Text("Use", style = T.smallB, color = p.ink)
            }
        }
        Text("Only the city is sent, to the free Open-Meteo service. No location access needed.", style = T.small.copy(fontSize = 12.sp), color = p.muted)
    }
}

@Composable
fun TempUnitField() {
    AppStore.rev
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Temperature")
        Seg(listOf(false to "°C", true to "°F"), AppStore.data.tempF) { v -> AppStore.change { tempF = v } }
    }
}

/** Choose the rates to follow: gold, crypto, and your currency against others. */
@Composable
fun RatesField() {
    AppStore.rev
    val p = pal()
    val ctx = LocalContext.current
    val d = AppStore.data
    val cur = d.cur
    LaunchedEffect(d.rates.size) { Live.refresh(ctx) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("You follow")
        if (d.rates.isEmpty()) Text("Nothing yet. Add one below.", style = T.small, color = p.muted)
        d.rates.forEachIndexed { i, k ->
            val q = d.quotes[k]
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Live.quoteName(k) + if (i == 0) "  ·  shown first" else "", style = T.smallB, color = p.ink)
                    Text(Live.quoteSub(k, cur), style = T.small.copy(fontSize = 12.sp), color = p.muted)
                }
                Text(q?.let { Live.fmt(k, it.v, cur) } ?: "…", style = T.smallB, color = p.ink)
                if (i > 0) Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable {
                    AppStore.change { rates.removeAt(i); rates.add(0, k) }
                }, contentAlignment = Alignment.Center) { Icon(Ic.up, "Show first", tint = p.muted, modifier = Modifier.size(16.dp)) }
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { AppStore.change { rates.remove(k) } }, contentAlignment = Alignment.Center) {
                    Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(16.dp))
                }
            }
        }
        val options = buildList {
            add("gold")
            Live.COINS.keys.forEach { add("c:$it") }
            if (cur in Live.FX) listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "SGD", "CNY").filter { it != cur }.forEach { add("fx:$it:$cur") }
            if (cur != "USD" && cur in Live.FX) add("fx:$cur:USD")
        }.filter { it !in d.rates }
        if (d.rates.size < 6 && options.isNotEmpty()) {
            FieldLabel("Add")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { k ->
                    Box(Modifier.clip(RoundedCornerShape(18.dp)).background(p.card).clickable {
                        AppStore.change { rates.add(k) }
                        Live.refresh(ctx, force = true)
                    }.padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text("+ " + Live.quoteName(k), style = T.smallB, color = p.ink)
                    }
                }
            }
        }
        val newest = d.rates.mapNotNull { d.quotes[it]?.at }.maxOrNull()
        Text((if (newest != null) "Updated " + Live.ago(newest) + " · " else "") + "Refresh now", style = T.smallB, color = p.muted,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { Live.refresh(ctx, force = true) }.padding(vertical = 6.dp))
        Text("Rates come from free public services (ECB reference rates, CoinGecko). Gold follows the international price.",
            style = T.small.copy(fontSize = 12.sp), color = p.muted)
    }
}

/* ---------------- Prayer and medicine ---------------- */

@Composable
fun PrayerField() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Seg(listOf("salah" to "Salah times", "custom" to "My own times"), d.prayMode) { v -> AppStore.change { prayMode = v } }
        if (d.prayMode == "salah") {
            Text("Worked out on your phone for your city, every day.", style = T.small, color = p.muted)
            PlaceField()
            FieldLabel("Method")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val cur = d.prayMethod.ifBlank { com.phani.widgets.core.Salah.defaultMethod(d.cur) }
                com.phani.widgets.core.Salah.METHODS.forEach { m ->
                    val on = m.id == cur
                    Box(Modifier.clip(RoundedCornerShape(18.dp)).background(if (on) p.ink else p.card).clickable { AppStore.change { prayMethod = m.id } }
                        .padding(horizontal = 14.dp, vertical = 9.dp)) { Text(m.name, style = T.smallB, color = if (on) p.paper else p.ink) }
                }
            }
            FieldLabel("Asr")
            Seg(listOf(false to "Standard", true to "Hanafi"), d.asrHanafi) { v -> AppStore.change { asrHanafi = v } }
            val pl = d.place
            if (pl != null) com.phani.widgets.core.Salah.times(com.phani.widgets.core.today(), pl.lat, pl.lon, d.prayMethod.ifBlank { com.phani.widgets.core.Salah.defaultMethod(d.cur) }, d.asrHanafi)?.let { t ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Today in ${pl.name}", style = T.smallB, color = p.ink)
                    t.forEach { (n, tm) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(n, style = T.small, color = if (n == "Sunrise") p.muted else p.ink, modifier = Modifier.weight(1f))
                            Text(tm.format(java.time.format.DateTimeFormatter.ofPattern(if (d.h24) "HH:mm" else "h:mm a")), style = T.smallB, color = p.ink)
                        }
                    }
                }
            }
            Text("Times can differ by a few minutes from your local mosque.", style = T.small.copy(fontSize = 12.sp), color = p.muted)
        } else {
            Text("Any prayer, puja, aarti or quiet time, at the times you choose.", style = T.small, color = p.muted)
            RitualList("prayer", "Morning puja")
        }
        RemindToggle("pray", "Remind me at each time", "A notification when it's time, with a Done button")
    }
}

@Composable
fun MedsField() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RitualList("med", "Vitamin D")
        RemindToggle("meds", "Remind me when each is due", "A notification at each time, with a Taken button")
        Text("A reminder helps you remember. It isn't medical advice; follow your doctor's instructions.", style = T.small.copy(fontSize = 12.sp), color = pal().muted)
    }
}

/** Your own timed items: a name and a time each, added and removed in place. */
@Composable
fun RitualList(kind: String, hint: String) {
    AppStore.rev
    val p = pal()
    val ctx = LocalContext.current
    val list = AppStore.data.rituals.filter { it.kind == kind }.sortedBy { it.time }
    list.forEach { r ->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, style = T.smallB, color = p.ink)
                if (r.note.isNotBlank()) Text(r.note, style = T.small.copy(fontSize = 12.sp), color = p.muted)
            }
            Box(Modifier.width(118.dp)) { TimeField("", r.time) { v -> AppStore.change { r.time = v }; com.phani.widgets.widget.Reminders.schedule(ctx) } }
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { AppStore.change { rituals.remove(r) } }, contentAlignment = Alignment.Center) {
                Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(16.dp))
            }
        }
    }
    var name by remember(kind) { mutableStateOf("") }
    var time by remember(kind) { mutableStateOf(if (kind == "med") "09:00" else "06:00") }
    var note by remember(kind) { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (list.isEmpty()) "Add your first" else "Add another", style = T.bodyB, color = p.ink)
        Field(if (kind == "med") "Medicine" else "Name", name, { name = it.take(30) }, hint)
        if (kind == "med") Field("Dose (optional)", note, { note = it.take(40) }, "1 tablet with food")
        TimeField("Time", time) { time = it }
        BigButton("Add", kind = if (name.isBlank()) "ghost" else "solid") {
            if (name.isNotBlank()) {
                AppStore.change { rituals.add(com.phani.widgets.core.Ritual(com.phani.widgets.core.uid(), name.trim(), time, kind, note.trim(), mutableSetOf())) }
                com.phani.widgets.widget.Reminders.schedule(ctx)
                name = ""; note = ""
            }
        }
        if (kind == "med") Text("Twice a day? Add it twice, with each time.", style = T.small.copy(fontSize = 12.sp), color = p.muted)
    }
}
