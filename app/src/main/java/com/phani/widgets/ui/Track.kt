package com.phani.widgets.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.CITIES
import com.phani.widgets.core.CUR
import com.phani.widgets.core.Counter
import com.phani.widgets.core.DateItem
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.Goal
import com.phani.widgets.core.Habit
import com.phani.widgets.core.SchedItem
import com.phani.widgets.core.Spend
import com.phani.widgets.core.Sub
import com.phani.widgets.core.Subject
import com.phani.widgets.core.WClock
import com.phani.widgets.core.att
import com.phani.widgets.core.cnt
import com.phani.widgets.core.dateNext
import com.phani.widgets.core.dayIndex
import com.phani.widgets.core.daySched
import com.phani.widgets.core.fastState
import com.phani.widgets.core.mins
import com.phani.widgets.core.pace
import com.phani.widgets.core.pd
import com.phani.widgets.core.plural
import com.phani.widgets.core.spendState
import com.phani.widgets.core.streak
import com.phani.widgets.core.subjectBy
import com.phani.widgets.core.subsMonthly
import com.phani.widgets.core.subsSorted
import com.phani.widgets.core.today
import com.phani.widgets.core.tzNow
import com.phani.widgets.core.tzOffset
import com.phani.widgets.core.tzText
import com.phani.widgets.core.uid
import java.time.LocalDate
import kotlin.math.roundToInt

object TrackState {
    var tab by mutableStateOf("money")
    var sheet by mutableStateOf<String?>(null)
    var id by mutableStateOf<String?>(null)
    fun open(kind: String, id: String? = null) { this.id = id; sheet = kind }
}

private val TABS = listOf("money" to "Money", "subs" to "Renewals", "habits" to "Habits", "goals" to "Goals", "dates" to "Dates",
    "fast" to "Fasting", "counters" to "Counters", "clocks" to "Clocks", "sched" to "Schedule", "classes" to "Classes")

@Composable
private fun Chev() = Icon(Ic.chev, null, tint = pal().muted, modifier = Modifier.size(18.dp))

@Composable
private fun PlusIcon() = Icon(Ic.plus, null, tint = pal().muted, modifier = Modifier.size(18.dp))

@Composable
private fun Note(t: String) = Text(t, style = T.small, color = pal().muted, modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp))

@Composable
private fun Val(t: String, size: Int = 15, color: Color? = null) = Text(t, style = T.bodyB.copy(fontSize = size.sp), color = color ?: pal().ink, maxLines = 1)

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, solid: Boolean, desc: String, onClick: () -> Unit) {
    val p = pal()
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(if (solid) p.ink else p.soft).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = if (solid) p.paper else p.ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun TrackScreen() {
    val p = pal()
    val d = AppStore.data
    TopBar("Your info", "Widgets read from here. Change something and every widget updates.")
    if (d.sampleData) SampleBanner("This is example data.", "Use my own") { TrackState.open("clear") }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TABS.forEach { (k, l) ->
            val on = TrackState.tab == k
            Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (on) p.ink else Color.Transparent)
                .then(if (on) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(19.dp))).clickable { TrackState.tab = k }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) { Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted) }
        }
    }
    Spacer(Modifier.height(16.dp))
    when (TrackState.tab) {
        "money" -> MoneyTab()
        "subs" -> SubsTab()
        "habits" -> HabitsTab()
        "goals" -> GoalsTab()
        "dates" -> DatesTab()
        "fast" -> FastTab()
        "counters" -> CountersTab()
        "clocks" -> ClocksTab()
        "sched" -> SchedTab()
        else -> ClassesTab()
    }
}

/* ---------------- tabs ---------------- */

@Composable
private fun MoneyTab() {
    val p = pal()
    val s = spendState()
    ListCard {
        ListRow("Safe to spend today", "${plural(s.days, "day")} until it resets · ${Fmt.short(s.resets)}", trailing = { Val(Fmt.money(s.left), 20, if (s.left < 0) p.warn else null) })
        ListRow("Daily allowance", "Recalculated every time you spend", trailing = { Val(Fmt.money(s.per)) })
        ListRow("Spent this month", "of ${Fmt.money(AppStore.data.budget)}", divider = false, trailing = { Val(Fmt.money(s.spent)) })
    }
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("Log spend", Modifier.weight(1f), icon = Ic.plus) { TrackState.open("spend") }
        BigButton("Budget", Modifier.weight(1f), kind = "ghost") { TrackState.open("budget") }
    }
    SectionTitle("Recent")
    val recent = AppStore.data.log.sortedByDescending { it.date }.take(8)
    ListCard {
        if (recent.isEmpty()) ListRow("Nothing logged yet", divider = false)
        recent.forEachIndexed { i, x ->
            ListRow(x.note.ifBlank { "Spend" }, Fmt.full(pd(x.date)), divider = i < recent.size - 1, trailing = {
                Val(Fmt.money(x.amt))
                Box(Modifier.size(36.dp).clickable {
                    AppStore.change(true) { log.removeAll { it.id == x.id } }; Toaster.show("Spend deleted", true)
                }, contentAlignment = Alignment.Center) { Icon(Ic.x, "Delete", tint = p.muted, modifier = Modifier.size(18.dp)) }
            })
        }
    }
}

@Composable
private fun SubsTab() {
    val l = subsSorted()
    ListCard {
        l.forEach { r ->
            ListRow(r.s.name, "${Fmt.money(r.s.amt)} / ${r.s.cycle} · next ${Fmt.short(r.d)}", trailing = { Pill(if (r.days == 0) "Today" else "${r.days}d", if (r.days <= 3) "warn" else "") }) { TrackState.open("sub", r.s.id) }
        }
        ListRow("Add subscription", divider = false, trailing = { PlusIcon() }) { TrackState.open("sub") }
    }
    Note("All together: ${Fmt.money(subsMonthly())} a month, ${Fmt.money(subsMonthly() * 12, 0)} a year.")
}

@Composable
private fun HabitsTab() {
    ListCard {
        AppStore.data.habits.forEach { h ->
            val st = streak(h)
            ListRow(h.name, "${st.n}-day streak · best ${h.record} · ${plural(h.freezes, "freeze")}", trailing = {
                if (st.canFreeze) ChipButton("Freeze", icon = Ic.snow) {
                    AppStore.change(true) { h.frozen.add(today().minusDays(1).toString()); h.freezes-- }
                    Toaster.show("Yesterday frozen · ${h.name} streak kept", true)
                }
                RoundIcon(Ic.check, st.today, if (st.today) "Undo" else "Check in") {
                    val r = com.phani.widgets.core.Actions.run("hab", h.id, null); r.msg?.let { Toaster.show(it, true) }
                }
            })
        }
        ListRow("Add habit", divider = false, trailing = { PlusIcon() }) { TrackState.open("habit") }
    }
    Note("Missed yesterday? Use a freeze and the streak carries on. Every 7 days in a row earns one, up to 3.")
}

@Composable
private fun GoalsTab() {
    ListCard {
        AppStore.data.goals.forEach { g ->
            val r = pace(g); val a = r.ahead.roundToInt()
            ListRow(g.name, "${Fmt.trimNum(g.done)}/${Fmt.trimNum(g.target)}${Fmt.unit(g.unit)} · ${Fmt.num(r.per)}/day · ${r.left}d left",
                trailing = { Pill(if (a <= -1) "${-a}d behind" else if (a >= 1) "${a}d ahead" else "On track", if (a <= -1) "warn" else "good") }) { TrackState.open("goal", g.id) }
        }
        ListRow("Add goal", divider = false, trailing = { PlusIcon() }) { TrackState.open("goal") }
    }
    Note("Pace compares where you are with where you'd be if you spread the work evenly to the deadline.")
}

@Composable
private fun DatesTab() {
    ListCard {
        AppStore.data.dates.forEach { x ->
            val r = dateNext(x)
            ListRow(x.name, Fmt.full(r.d) + if (x.yearly) " · every year" else "", trailing = {
                Val(if (r.days < 0) "${-r.days}d ago" else if (r.days == 0) "Today" else "${r.days} days")
            }) { TrackState.open("date", x.id) }
        }
        ListRow("Add date", divider = false, trailing = { PlusIcon() }) { TrackState.open("date") }
    }
}

@Composable
private fun FastTab() {
    val f = fastState()
    val d = AppStore.data
    ListCard {
        ListRow(if (f.on) "Fasting now" else "Not fasting",
            if (f.on) "Started ${Fmt.at(d.fast.start!!, true)} · goal at ${Fmt.at(f.endMs, true)}" else "Start when you finish eating",
            divider = false, trailing = { Val(if (f.on) Fmt.hhmm(f.h) else "—", 20) })
    }
    BigButton(if (f.on) "End fast" else "Start fast", Modifier.padding(top = 12.dp), icon = if (f.on) Ic.stop else Ic.play) {
        val r = com.phani.widgets.core.Actions.run("fast", null, null); r.msg?.let { Toaster.show(it, true) }
    }
    SectionTitle("Goal")
    Seg(listOf(12, 14, 16, 18, 20).map { it to "${it}h" }, d.fast.target) { v -> AppStore.change { fast.target = v }; Toaster.show("Goal set to ${v}h") }
    if (d.fast.history.isNotEmpty()) {
        SectionTitle("Recent fasts")
        val h = d.fast.history.takeLast(5).reversed()
        ListCard {
            h.forEachIndexed { i, x ->
                val day = java.time.Instant.ofEpochMilli(x.start).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                ListRow(Fmt.full(day), "${Fmt.at(x.start, true)} to ${Fmt.at(x.end, true)}", divider = i < h.size - 1, trailing = { Val(Fmt.hhmm((x.end - x.start) / 3_600_000.0)) })
            }
        }
    }
}

@Composable
private fun CountersTab() {
    ListCard {
        AppStore.data.counters.forEach { c ->
            ListRow(c.name, "${cnt(c)} of ${c.goal} ${c.unit} today", trailing = {
                RoundIcon(Ic.x, false, "Remove one") { AppStore.change { val k = today().toString(); c.log[k] = maxOf(0, (c.log[k] ?: 0) - 1) } }
                RoundIcon(Ic.plus, true, "Add one") { val r = com.phani.widgets.core.Actions.run("cnt", c.id, null); r.msg?.let { Toaster.show(it, true) } }
            })
        }
        ListRow("Add counter", divider = false, trailing = { PlusIcon() }) { TrackState.open("counter") }
    }
}

@Composable
private fun ClocksTab() {
    val p = pal()
    ListCard {
        AppStore.data.clocks.forEachIndexed { i, k ->
            ListRow(k.city, "${tzOffset(k.tz)} from you", divider = i < AppStore.data.clocks.size - 1, trailing = {
                Val(tzText(tzNow(k.tz)))
                Box(Modifier.size(36.dp).clickable { AppStore.change { clocks.removeAll { it.id == k.id } } }, contentAlignment = Alignment.Center) {
                    Icon(Ic.x, "Remove ${k.city}", tint = p.muted, modifier = Modifier.size(18.dp))
                }
            })
        }
        if (AppStore.data.clocks.isEmpty()) ListRow("No cities yet", divider = false)
    }
    Spacer(Modifier.height(14.dp))
    Picker("Add a city", "Choose…", CITIES.map { it.second to it.first }) { tz ->
        val city = CITIES.first { it.second == tz }.first
        AppStore.change { clocks.add(WClock(uid(), city, tz)) }; Toaster.show("$city added")
    }
}

@Composable
private fun SchedTab() {
    BigButton("Edit weekly schedule", kind = "ghost") { TrackState.open("sched") }
    SectionTitle("Today")
    val l = daySched(dayIndex())
    ListCard {
        if (l.isEmpty()) ListRow("Nothing today", divider = false)
        l.forEachIndexed { i, c ->
            ListRow(c.title, "${c.place} · until ${Fmt.hm(c.e)}", divider = i < l.size - 1, lead = { Text(Fmt.hm(c.s), style = T.smallB.copy(fontSize = 14.sp), color = pal().ink, modifier = Modifier.width(76.dp)) })
        }
    }
}

@Composable
private fun ClassesTab() {
    val d = AppStore.data
    ListCard {
        d.subjects.forEach { s ->
            val r = att(s)
            ListRow(s.name, "${s.a} of ${s.t} attended", trailing = {
                Pill(if (r.ok) (if (r.skip > 0) "Miss ${r.skip}" else "None left") else "Attend ${r.need}", if (r.ok) "" else "warn")
                Val("${(r.p * 100).roundToInt()}%")
            }) { TrackState.open("subject", s.id) }
        }
        ListRow("Add class", divider = false, trailing = { PlusIcon() }) { TrackState.open("subject") }
    }
    BigButton("What if I miss some?", Modifier.padding(top = 12.dp), kind = "ghost") {
        if (!d.pro) Sheets.showPaywall("What-if is part of Pro") else TrackState.open("whatif")
    }
    SectionTitle("Required attendance")
    Seg(listOf(60, 70, 75, 80, 90).map { it to "$it%" }, d.req) { v -> AppStore.change { req = v }; Toaster.show("Required attendance set to $v%") }
    Note("Set this to your school's rule. Many Indian colleges use 75%.")
}

/* ---------------- form pieces ---------------- */

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "", keyboard: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier) {
    val p = pal()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = T.smallB, color = p.muted)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = T.body.copy(fontSize = 16.sp, color = p.ink), cursorBrush = SolidColor(p.ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = T.body.copy(fontSize = 16.sp), color = p.muted.copy(alpha = 0.6f))
                    inner()
                }
            },
        )
    }
}

@Composable
private fun TapField(label: String, shown: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = pal()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = T.smallB, color = p.muted)
        Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(shown, style = T.body.copy(fontSize = 16.sp), color = p.ink, modifier = Modifier.weight(1f), maxLines = 1)
            Chev()
        }
    }
}

@Composable
fun DateField(label: String, iso: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val d = runCatching { LocalDate.parse(iso) }.getOrDefault(today())
    TapField(label, Fmt.full(d), modifier) {
        DatePickerDialog(ctx, { _, y, m, day -> onPick(LocalDate.of(y, m + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }
}

@Composable
fun TimeField(label: String, hm: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val h = hm.split(":")[0].toIntOrNull() ?: 9
    val m = hm.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    TapField(label, Fmt.hm(hm), modifier) {
        TimePickerDialog(ctx, { _, hh, mm -> onPick("${hh.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}") }, h, m, AppStore.data.h24).show()
    }
}

@Composable
fun <K> Picker(label: String, shown: String, options: List<Pair<K, String>>, modifier: Modifier = Modifier, onPick: (K) -> Unit) {
    val p = pal()
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TapField(label, shown) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(p.card)) {
            options.forEach { (k, l) -> DropdownMenuItem(text = { Text(l, style = T.body, color = p.ink) }, onClick = { open = false; onPick(k) }) }
        }
    }
}

private fun num(s: String): Double? = s.replace(",", ".").trim().toDoubleOrNull()

/* ---------------- sheets ---------------- */

@Composable
fun TrackSheetHost() {
    val k = TrackState.sheet ?: return
    val id = TrackState.id
    val close = { TrackState.sheet = null }
    when (k) {
        "spend" -> SpendSheet(close)
        "budget" -> BudgetSheet(close)
        "sub" -> SubSheet(id, close)
        "goal" -> GoalSheet(id, close)
        "date" -> DateSheet(id, close)
        "habit" -> HabitSheet(close)
        "counter" -> CounterSheet(close)
        "subject" -> SubjectSheet(id, close)
        "whatif" -> WhatIfSheet(close)
        "sched" -> SchedSheet(close)
        "clear" -> ClearSheet(close)
        "help" -> HelpSheet(close)
    }
}

@Composable
private fun SpendSheet(close: () -> Unit) {
    val p = pal()
    var amt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val q = (CUR[AppStore.data.cur] ?: CUR.getValue("USD")).quick
    Sheet("Log spend", close) {
        Text(Fmt.money(num(amt) ?: 0.0, if (amt.contains(".")) 2 else 0), style = T.h1.copy(fontSize = 52.sp, lineHeight = 56.sp), color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            (q + listOf((q[2] * 2.5).roundToInt())).forEach { v ->
                ChipButton("+$v") { amt = Fmt.trimNum(((num(amt) ?: 0.0) + v) * 100 / 100.0) }
            }
        }
        Field("For (optional)", note, { note = it }, "Coffee, groceries, ride…")
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(p.card).clickable {
                            amt = when {
                                key == "⌫" -> amt.dropLast(1)
                                key == "." -> if (amt.contains(".")) amt else (amt.ifEmpty { "0" }) + "."
                                amt.contains(".") && amt.substringAfter(".").length >= 2 -> amt
                                amt.replace(".", "").length >= 7 -> amt
                                else -> (amt + key).trimStart('0').ifEmpty { "0" }.let { if (it.startsWith(".")) "0$it" else it }
                            }
                        }, contentAlignment = Alignment.Center) { Text(key, style = T.h3.copy(fontSize = 22.sp), color = p.ink) }
                    }
                }
            }
        }
        BigButton("Save") {
            val n = num(amt)
            if (n == null || n <= 0) { Toaster.show("Enter an amount first"); return@BigButton }
            AppStore.change(true) { log.add(Spend(uid(), n, today().toString(), note.trim())) }
            close(); Toaster.show("Logged ${Fmt.money(n)} · ${Fmt.money(spendState().left)} left today", true)
        }
    }
}

@Composable
private fun BudgetSheet(close: () -> Unit) {
    val d = AppStore.data
    var b by remember { mutableStateOf(Fmt.trimNum(d.budget)) }
    var day by remember { mutableStateOf(d.payday) }
    Sheet("Monthly budget", close) {
        Field("Budget for the month", b, { b = it }, keyboard = KeyboardType.Decimal)
        Picker("Resets on day", "$day of every month", (1..28).map { it to "$it of every month" }) { day = it }
        Text("Usually your payday. What's left is spread evenly over the days until then.", style = T.small, color = pal().muted)
        BigButton("Save") { AppStore.change { budget = maxOf(0.0, num(b) ?: 0.0); payday = day }; close(); Toaster.show("Budget saved") }
    }
}

@Composable
private fun SubSheet(id: String?, close: () -> Unit) {
    val s = AppStore.data.subs.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(s?.name ?: "") }
    var amt by remember { mutableStateOf(s?.amt?.let { Fmt.trimNum(it) } ?: "") }
    var cycle by remember { mutableStateOf(s?.cycle ?: "month") }
    var next by remember { mutableStateOf(s?.next ?: today().plusDays(7).toString()) }
    Sheet(if (s != null) "Edit subscription" else "New subscription", close) {
        Field("Name", name, { name = it }, "Netflix")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Price", amt, { amt = it }, keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Picker("Billed", if (cycle == "year") "Yearly" else "Monthly", listOf("month" to "Monthly", "year" to "Yearly"), Modifier.weight(1f)) { cycle = it }
        }
        DateField("Next charge", next) { next = it }
        BigButton("Save") {
            val a = num(amt)
            if (name.isBlank() || a == null) { Toaster.show("Add a name and a price"); return@BigButton }
            AppStore.change { if (s != null) { s.name = name.trim(); s.amt = a; s.cycle = cycle; s.next = next } else subs.add(Sub(uid(), name.trim(), a, cycle, next)) }
            close(); Toaster.show(if (s != null) "Subscription saved" else "Subscription added")
        }
        if (s != null) BigButton("Delete subscription", kind = "danger") { AppStore.change(true) { subs.remove(s) }; close(); Toaster.show("Subscription deleted", true) }
    }
}

@Composable
private fun GoalSheet(id: String?, close: () -> Unit) {
    val g = AppStore.data.goals.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(g?.name ?: "") }
    var target by remember { mutableStateOf(g?.target?.let { Fmt.trimNum(it) } ?: "100") }
    var unit by remember { mutableStateOf(g?.unit ?: "pages") }
    var done by remember { mutableStateOf(g?.done?.let { Fmt.trimNum(it) } ?: "0") }
    var step by remember { mutableStateOf(g?.step?.let { Fmt.trimNum(it) } ?: "5") }
    var start by remember { mutableStateOf(g?.start ?: today().toString()) }
    var end by remember { mutableStateOf(g?.end ?: today().plusDays(30).toString()) }
    Sheet(if (g != null) "Edit goal" else "New goal", close) {
        Field("Goal", name, { name = it }, "Books this year")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Target", target, { target = it }, keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Field("Unit", unit, { unit = it }, "books", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Done so far", done, { done = it }, keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Field("+ button adds", step, { step = it }, keyboard = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField("Start", start, Modifier.weight(1f)) { start = it }
            DateField("Deadline", end, Modifier.weight(1f)) { end = it }
        }
        BigButton("Save") {
            if (name.isBlank()) { Toaster.show("Give the goal a name"); return@BigButton }
            if (end <= start) { Toaster.show("The deadline must be after the start"); return@BigButton }
            val t = maxOf(1.0, num(target) ?: 1.0); val dn = maxOf(0.0, num(done) ?: 0.0); val st = maxOf(0.01, num(step) ?: 1.0)
            AppStore.change {
                if (g != null) { g.name = name.trim(); g.target = t; g.unit = unit.ifBlank { "units" }; g.done = dn; g.step = st; g.start = start; g.end = end }
                else goals.add(Goal(uid(), name.trim(), unit.ifBlank { "units" }, t, dn, start, end, st))
            }
            close(); Toaster.show(if (g != null) "Goal saved" else "Goal added")
        }
        if (g != null) BigButton("Delete goal", kind = "danger") { AppStore.change(true) { goals.remove(g) }; close(); Toaster.show("Goal deleted", true) }
    }
}

@Composable
private fun DateSheet(id: String?, close: () -> Unit) {
    val x = AppStore.data.dates.firstOrNull { it.id == id }
    var name by remember { mutableStateOf(x?.name ?: "") }
    var date by remember { mutableStateOf(x?.date ?: today().plusDays(30).toString()) }
    var yearly by remember { mutableStateOf(x?.yearly ?: false) }
    Sheet(if (x != null) "Edit date" else "New date", close) {
        Field("Name", name, { name = it }, "Trip to Lisbon")
        DateField("Date", date) { date = it }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Repeats", style = T.smallB, color = pal().muted)
            Seg(listOf(false to "Once", true to "Every year"), yearly) { yearly = it }
        }
        BigButton("Save") {
            if (name.isBlank()) { Toaster.show("Give the date a name"); return@BigButton }
            AppStore.change { if (x != null) { x.name = name.trim(); x.date = date; x.yearly = yearly } else dates.add(DateItem(uid(), name.trim(), date, yearly)) }
            close(); Toaster.show(if (x != null) "Date saved" else "Date added")
        }
        if (x != null) BigButton("Delete date", kind = "danger") { AppStore.change(true) { dates.remove(x) }; close(); Toaster.show("Date deleted", true) }
    }
}

@Composable
private fun HabitSheet(close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Sheet("New habit", close) {
        Field("Habit", name, { name = it }, "Run")
        Text("You start with one freeze. Every 7 days in a row earns another, up to 3.", style = T.small, color = pal().muted)
        BigButton("Add habit") {
            if (name.isBlank()) return@BigButton
            AppStore.change { habits.add(Habit(uid(), name.trim(), 0, 1, mutableSetOf(), mutableSetOf())) }
            close(); Toaster.show("Habit added")
        }
    }
}

@Composable
private fun CounterSheet(close: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("8") }
    var unit by remember { mutableStateOf("glasses") }
    Sheet("New counter", close) {
        Field("Name", name, { name = it }, "Water")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Daily goal", goal, { goal = it }, keyboard = KeyboardType.Number, modifier = Modifier.weight(1f))
            Field("Unit", unit, { unit = it }, "glasses", modifier = Modifier.weight(1f))
        }
        BigButton("Add counter") {
            if (name.isBlank()) return@BigButton
            AppStore.change { counters.add(Counter(uid(), name.trim(), unit.ifBlank { "times" }, maxOf(1, goal.toIntOrNull() ?: 1), mutableMapOf())) }
            close(); Toaster.show("Counter added")
        }
    }
}

@Composable
private fun SubjectSheet(id: String?, close: () -> Unit) {
    val s = subjectBy(id)
    var name by remember { mutableStateOf(s?.name ?: "") }
    var short by remember { mutableStateOf(s?.short ?: "") }
    var a by remember { mutableStateOf(s?.a?.toString() ?: "0") }
    var t by remember { mutableStateOf(s?.t?.toString() ?: "0") }
    Sheet(if (s != null) "Edit class" else "Add class", close) {
        Field("Class name", name, { name = it }, "Computer Networks")
        Field("Short name on widget", short, { short = it.take(6) }, "CN")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Attended", a, { a = it }, keyboard = KeyboardType.Number, modifier = Modifier.weight(1f))
            Field("Held so far", t, { t = it }, keyboard = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        BigButton("Save") {
            if (name.isBlank()) return@BigButton
            val av = maxOf(0, a.toIntOrNull() ?: 0); val tv = maxOf(av, t.toIntOrNull() ?: 0)
            val sh = short.trim().ifBlank { name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(4) }.uppercase()
            AppStore.change { if (s != null) { s.name = name.trim(); s.short = sh; s.a = av; s.t = tv } else subjects.add(Subject(uid(), name.trim(), sh, av, tv)) }
            close(); Toaster.show(if (s != null) "Class saved" else "Class added")
        }
        if (s != null) BigButton("Delete class", kind = "danger") { AppStore.change(true) { subjects.remove(s) }; close(); Toaster.show("Class deleted", true) }
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    val p = pal()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(-1, 1).forEachIndexed { i, dlt ->
            if (i == 1) Text("$value", style = T.bodyB.copy(fontSize = 18.sp), color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(22.dp)).clickable { onChange(maxOf(0, value + dlt)) },
                contentAlignment = Alignment.Center) { Text(if (dlt < 0) "−" else "+", style = T.bodyB.copy(fontSize = 20.sp), color = p.ink) }
        }
    }
}

@Composable
private fun WhatIfSheet(close: () -> Unit) {
    val p = pal()
    val d = AppStore.data
    var sub by remember { mutableStateOf(d.subjects.firstOrNull()?.id) }
    var skip by remember { mutableStateOf(2) }
    var attend by remember { mutableStateOf(0) }
    Sheet("What if I miss some?", close) {
        val s = subjectBy(sub)
        if (s == null) { EmptyCard("No classes yet", "Add one in Classes."); return@Sheet }
        Picker("Class", s.name, d.subjects.map { it.id to it.name }) { sub = it }
        ListCard {
            ListRow("Miss the next", trailing = { Stepper(skip) { skip = it } })
            ListRow("Then attend", divider = false, trailing = { Stepper(attend) { attend = it } })
        }
        val now = att(s); val r = att(s, attend, skip + attend)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.card).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Now ${(now.p * 100).roundToInt()}%, afterwards", style = T.small, color = p.muted)
                Text(String.format("%.1f%%", r.p * 100), style = T.h1.copy(fontSize = 40.sp, lineHeight = 44.sp), color = if (r.ok) p.ink else p.warn)
            }
            Pill(if (r.ok) "Safe" else "Below ${d.req}%", if (r.ok) "good" else "warn")
        }
        Text(if (r.ok) "You could still miss ${r.skip} more after that." else "You'd need to attend ${r.need} in a row to recover.", style = T.body, color = p.ink)
    }
}

@Composable
private fun SchedSheet(close: () -> Unit) {
    val p = pal()
    var day by remember { mutableStateOf(dayIndex().let { if (it == 0) 1 else it }) }
    var title by remember { mutableStateOf("") }
    var s by remember { mutableStateOf("09:00") }
    var e by remember { mutableStateOf("10:00") }
    var place by remember { mutableStateOf("") }
    var sub by remember { mutableStateOf<String?>(null) }
    Sheet("Weekly schedule", close) {
        Seg(listOf(1, 2, 3, 4, 5, 6, 0).map { it to Fmt.dowName(it).take(2) }, day) { day = it }
        val l = daySched(day)
        ListCard {
            if (l.isEmpty()) ListRow("Nothing on ${Fmt.dowName(day)}", divider = false)
            l.forEachIndexed { i, c ->
                ListRow(c.title, "${c.place} · until ${Fmt.hm(c.e)}", divider = i < l.size - 1,
                    lead = { Text(Fmt.hm(c.s), style = T.smallB.copy(fontSize = 14.sp), color = p.ink, modifier = Modifier.width(76.dp)) },
                    trailing = {
                        Box(Modifier.size(36.dp).clickable { AppStore.change { sched[day]?.remove(c) } }, contentAlignment = Alignment.Center) {
                            Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(18.dp))
                        }
                    })
            }
        }
        Field("What", title, { title = it }, "Design review")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField("Starts", s, Modifier.weight(1f)) { s = it }
            TimeField("Ends", e, Modifier.weight(1f)) { e = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Where", place, { place = it }, "Room 204", modifier = Modifier.weight(1f))
            Picker("Counts for class", subjectBy(sub)?.short ?: "None", listOf<Pair<String?, String>>(null to "None") + AppStore.data.subjects.map { it.id to (it.short.ifBlank { it.name }) }, Modifier.weight(1f)) { sub = it }
        }
        BigButton("Add to ${Fmt.dowName(day)}", kind = "ghost", icon = Ic.plus) {
            if (title.isBlank()) { Toaster.show("Say what it is first"); return@BigButton }
            if (mins(e) <= mins(s)) { Toaster.show("The end time must be after the start"); return@BigButton }
            AppStore.change { sched.getOrPut(day) { mutableListOf() }.add(SchedItem(s, e, title.trim(), place.trim().ifBlank { "—" }, sub)) }
            title = ""; place = ""; Toaster.show("Added to ${Fmt.dowName(day)}")
        }
    }
}

@Composable
private fun ClearSheet(close: () -> Unit) {
    Sheet("Use your own info?", close) {
        Text("This clears the example data and the example widgets so you can add your own. You can bring the examples back in Settings.", style = T.body, color = pal().ink)
        BigButton("Clear examples", kind = "danger") {
            AppStore.change(true) {
                subs.clear(); habits.clear(); goals.clear(); dates.clear(); subjects.clear(); clocks.clear(); counters.clear()
                placed.clear(); sched.clear(); log.clear(); sampleData = false; fast.start = null; fast.history.clear()
            }
            close(); Nav.tab = "home"; Toaster.show("Cleared. Tap New to make your first widget.", true)
        }
        BigButton("Keep the examples", kind = "ghost", onClick = close)
    }
}
