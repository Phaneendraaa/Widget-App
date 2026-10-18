package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.SchedItem
import com.phani.widgets.core.Subject
import com.phani.widgets.core.dayIndex
import com.phani.widgets.core.daySched
import com.phani.widgets.core.mins
import com.phani.widgets.core.uid

private fun num2(s: String): Double? = s.replace(",", ".").trim().toDoubleOrNull()

@Composable
internal fun FastInfo() {
    AppStore.rev
    val f = AppStore.data.fast
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Goal")
        Seg(listOf(12, 14, 16, 18, 20).map { it to "${it}h" }, f.target) { v -> AppStore.change { fast.target = v } }
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    BigButton(if (f.start == null) "Start a fast" else "Change or end this fast", kind = "ghost") {
        ctx.startActivity(com.phani.widgets.QuickActivity.intent(ctx, "fast", null, null))
    }
    FastHistory()
}

@Composable
internal fun PaceInfo(c: com.phani.widgets.core.WConf) {
    AppStore.rev
    val g = AppStore.data.goals.firstOrNull { it.id == c.ref } ?: return
    var name by remember(g.id) { mutableStateOf(g.name) }
    var target by remember(g.id) { mutableStateOf(Fmt.trimNum(g.target)) }
    var unit by remember(g.id) { mutableStateOf(g.unit) }
    var done by remember(g.id) { mutableStateOf(Fmt.trimNum(g.done)) }
    var step by remember(g.id) { mutableStateOf(Fmt.trimNum(g.step)) }
    Field("Goal", name, { name = it; AppStore.change { g.name = it.take(30) } }, "Read 24 books")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Target", target, { target = it; num2(it)?.let { v -> AppStore.change { g.target = maxOf(1.0, v) } } }, "24", KeyboardType.Decimal, Modifier.weight(1f))
        Field("Counted in", unit, { unit = it; AppStore.change { g.unit = it.ifBlank { "units" }.take(14) } }, "books", modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Done so far", done, { done = it; num2(it)?.let { v -> AppStore.change { g.done = maxOf(0.0, v) } } }, "0", KeyboardType.Decimal, Modifier.weight(1f))
        Field("The + button adds", step, { step = it; num2(it)?.let { v -> AppStore.change { g.step = maxOf(0.01, v) } } }, "1", KeyboardType.Decimal, Modifier.weight(1f))
    }
    DateField("Deadline", g.end) { v -> if (v > g.start) AppStore.change { g.end = v } else Toaster.show("Pick a day after today") }
}

@Composable
internal fun NoteInfo(c: com.phani.widgets.core.WConf) {
    AppStore.rev
    val p = pal()
    val n = AppStore.data.notes.firstOrNull { it.id == c.ref } ?: return
    var text by remember(n.id) { mutableStateOf(n.text) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Note")
        BasicTextField(
            value = text, onValueChange = { v -> text = v.take(160); AppStore.change { n.text = text } },
            textStyle = T.body.copy(fontSize = 17.sp, color = p.ink), cursorBrush = SolidColor(p.ink),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().heightIn(min = 110.dp).clip(RoundedCornerShape(16.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    if (text.isEmpty()) Text("Drink water. Call mum. Ship it on Friday.", style = T.body.copy(fontSize = 17.sp), color = p.muted.copy(alpha = 0.6f))
                    inner()
                }
            },
        )
        Text("${text.length}/160", style = T.small, color = p.muted)
    }
}

@Composable
internal fun SchedInfo() {
    AppStore.rev
    val p = pal()
    var day by remember { mutableStateOf(dayIndex()) }
    var title by remember { mutableStateOf("") }
    var s by remember { mutableStateOf("09:00") }
    var e by remember { mutableStateOf("10:00") }
    var place by remember { mutableStateOf("") }
    var every by remember { mutableStateOf("one") }
    Seg(listOf(1, 2, 3, 4, 5, 6, 0).map { it to Fmt.dowName(it).take(2) }, day) { day = it }
    val l = daySched(day)
    ListCard {
        if (l.isEmpty()) ListRow("Nothing on ${Fmt.dowName(day)} yet", divider = false)
        l.forEachIndexed { i, c ->
            ListRow(c.title, "${Fmt.hm(c.s)} – ${Fmt.hm(c.e)}" + if (c.place.isNotBlank() && c.place != "—") " · ${c.place}" else "", divider = i < l.size - 1,
                trailing = { Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(18.dp)) }) { AppStore.change { sched[day]?.remove(c) } }
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Add to your schedule", style = T.bodyB, color = p.ink)
        Field("What", title, { title = it }, "Team standup")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField("Starts", s, Modifier.weight(1f)) { s = it }
            TimeField("Ends", e, Modifier.weight(1f)) { e = it }
        }
        Field("Where (optional)", place, { place = it }, "Room 204")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Repeats")
            Seg(listOf("one" to "Only ${Fmt.dowName(day).take(3)}", "week" to "Mon–Fri", "all" to "Every day"), every) { every = it }
        }
        BigButton("Add", kind = if (title.isNotBlank()) "solid" else "ghost") {
            if (title.isBlank()) { Toaster.show("Say what it is first"); return@BigButton }
            if (mins(e) <= mins(s)) { Toaster.show("The end time must be after the start"); return@BigButton }
            val days = when (every) { "week" -> listOf(1, 2, 3, 4, 5); "all" -> (0..6).toList(); else -> listOf(day) }
            AppStore.change { days.forEach { d -> sched.getOrPut(d) { mutableListOf() }.add(SchedItem(s, e, title.trim(), place.trim(), null)) } }
            title = ""; place = ""
        }
    }
}

@Composable
internal fun ClassesInfo() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Minimum attendance")
        Seg(listOf(60, 65, 70, 75, 80, 85).map { it to "$it%" }, d.req) { v -> AppStore.change { req = v } }
    }
    if (d.subjects.isNotEmpty()) ListCard {
        d.subjects.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = T.bodyB, color = p.ink, maxLines = 1)
                    Text("Attended ${s.a} of ${s.t}", style = T.small, color = p.muted)
                }
                MiniBtn("✓") { AppStore.change { s.a++; s.t++ } }
                MiniBtn("✗") { AppStore.change { s.t++ } }
                Box(Modifier.size(36.dp).clickable { AppStore.change { subjects.remove(s) } }, contentAlignment = Alignment.Center) {
                    Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    var name by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var t by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (d.subjects.isEmpty()) "Add your first class" else "Add another class", style = T.bodyB, color = p.ink)
        Field("Class", name, { name = it }, "Computer Networks")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Attended so far", a, { a = it }, "0", KeyboardType.Number, Modifier.weight(1f))
            Field("Held so far", t, { t = it }, "0", KeyboardType.Number, Modifier.weight(1f))
        }
        BigButton("Add class", kind = if (name.isNotBlank()) "solid" else "ghost") {
            if (name.isBlank()) { Toaster.show("Name the class first"); return@BigButton }
            val tt = maxOf(0, t.toIntOrNull() ?: 0); val aa = (a.toIntOrNull() ?: 0).coerceIn(0, tt)
            val short = name.trim().split(" ").filter { it.isNotBlank() }.let { w -> if (w.size > 1) w.take(3).joinToString("") { it.take(1).uppercase() } else name.trim().take(3).uppercase() }
            AppStore.change { subjects.add(Subject(uid(), name.trim(), short, aa, tt)) }
            name = ""; a = ""; t = ""
        }
    }
}

@Composable
private fun MiniBtn(t: String, onClick: () -> Unit) {
    val p = pal()
    Box(Modifier.padding(start = 6.dp).size(36.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, p.line, RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) { Text(t, style = T.bodyB, color = p.ink) }
}


/* ---------------- history you can fix ---------------- */

@Composable
private fun HistoryTitle(t: String, sub: String? = null) {
    val p = pal()
    Column(Modifier.padding(top = 6.dp)) {
        Text(t, style = T.bodyB, color = p.ink)
        if (sub != null) Text(sub, style = T.small, color = p.muted)
    }
}

private fun dayLabel(d: java.time.LocalDate): String {
    val t = com.phani.widgets.core.today()
    return when (d) { t -> "Today"; t.minusDays(1) -> "Yesterday"; else -> "${Fmt.dowName(d.dayOfWeek.value % 7)}, ${Fmt.short(d)}" }
}

/** This month's spending, newest first, each removable. */
@Composable
internal fun SpendHistory() {
    AppStore.rev
    val p = pal()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val d = AppStore.data
    val l = d.log.sortedByDescending { it.date }.take(12)
    HistoryTitle("Recent spending", if (l.isEmpty()) "Nothing logged yet." else "Logged by mistake? Tap × to remove it.")
    if (l.isNotEmpty()) ListCard {
        l.forEachIndexed { i, sp ->
            ListRow(Fmt.money(sp.amt), "${sp.note.ifBlank { "Spending" }} · ${dayLabel(java.time.LocalDate.parse(sp.date))}", divider = i < l.size - 1,
                trailing = { Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(18.dp)) }) {
                AppStore.change(true) { log.remove(sp) }; Toaster.show("Removed ${Fmt.money(sp.amt)}", true)
            }
        }
    }
    BigButton("Add spending", kind = "ghost", icon = Ic.plus) { ctx.startActivity(com.phani.widgets.QuickActivity.intent(ctx, "spend", null, null)) }
}

/** The last 7 days, each with − and +, so a wrong count can be fixed. */
@Composable
internal fun CounterHistory(ct: com.phani.widgets.core.Counter) {
    AppStore.rev
    val p = pal()
    HistoryTitle("Last 7 days", "Tapped by mistake? Fix any day here.")
    ListCard {
        (0L..6L).forEach { back ->
            val day = com.phani.widgets.core.today().minusDays(back)
            val k = day.toString()
            val v = ct.log[k] ?: 0
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dayLabel(day), style = T.body, color = p.ink, modifier = Modifier.weight(1f))
                MiniBtn("−") { if (v > 0) AppStore.change { ct.log[k] = v - 1 } }
                Text("$v", style = T.bodyB, color = if (v >= ct.goal) Signal else p.ink, modifier = Modifier.width(44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                MiniBtn("+") { AppStore.change { ct.log[k] = v + 1 } }
            }
        }
    }
}

/** Four weeks of days: tap any day to mark it done or not. */
@Composable
internal fun HabitCalendar(h: com.phani.widgets.core.Habit) {
    AppStore.rev
    val p = pal()
    val t = com.phani.widgets.core.today()
    val start = t.minusDays(27L + (t.dayOfWeek.value - 1)) // Monday, 4 weeks back
    HistoryTitle("Your days", "Tap a day to tick or untick it. Forgot yesterday? Tick it here.")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.card).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, style = T.small, color = p.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f)) } }
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(t.plusDays((7 - t.dayOfWeek.value).toLong())) }.toList()
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    val k = day.toString()
                    val future = day.isAfter(t)
                    val on = k in h.done
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (on) (if (day == t) Signal else p.ink) else if (future) Color.Transparent else p.soft)
                        .then(if (day == t && !on) Modifier.border(2.dp, Signal, RoundedCornerShape(10.dp)) else Modifier)
                        .clickable(enabled = !future) {
                            AppStore.change { if (on) h.done.remove(k) else { h.done.add(k); h.frozen.remove(k); val n = com.phani.widgets.core.streak(h).n; if (n > h.record) h.record = n } }
                        }, contentAlignment = Alignment.Center) {
                        Text("${day.dayOfMonth}", style = T.smallB, color = if (on) p.paper else if (future) p.muted.copy(alpha = 0.4f) else p.ink)
                    }
                }
            }
        }
    }
    Text("${com.phani.widgets.core.streak(h).n}-day streak · best ${maxOf(h.record, com.phani.widgets.core.streak(h).n)}", style = T.small, color = p.muted)
}

/** Past fasts with how long they lasted, each removable. */
@Composable
internal fun FastHistory() {
    AppStore.rev
    val p = pal()
    val l = AppStore.data.fast.history.sortedByDescending { it.start }.take(8)
    if (l.isEmpty()) return
    HistoryTitle("Past fasts", "A fast you didn't mean to log? Tap × to remove it.")
    ListCard {
        l.forEachIndexed { i, r ->
            val h = (r.end - r.start) / 3_600_000.0
            val d = java.time.Instant.ofEpochMilli(r.start).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            ListRow(Fmt.hhmm(h) + " h", dayLabel(d), divider = i < l.size - 1,
                trailing = { Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(18.dp)) }) {
                AppStore.change(true) { fast.history.remove(r) }; Toaster.show("Fast removed", true)
            }
        }
    }
}
