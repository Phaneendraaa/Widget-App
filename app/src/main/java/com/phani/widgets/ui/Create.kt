package com.phani.widgets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.CITIES
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.Counter
import com.phani.widgets.core.DateItem
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.Habit
import com.phani.widgets.core.Goal
import com.phani.widgets.core.Note
import com.phani.widgets.core.SchedItem
import com.phani.widgets.core.Subject
import com.phani.widgets.core.Sub
import com.phani.widgets.core.WClock
import com.phani.widgets.core.WConf
import com.phani.widgets.core.today
import com.phani.widgets.core.uid
import com.phani.widgets.widget.Widgets
import com.phani.widgets.widget.preferredHeight
import kotlinx.coroutines.delay

/**
 * The widget editor: opened by tapping a design in the gallery, or a saved widget in My widgets.
 * One screen: preview, your details, size, style, and Add to home screen.
 */
object Create {
    var open by mutableStateOf(false)
    var conf by mutableStateOf<WConf?>(null)
    var editing by mutableStateOf(false)
    /** Set when a widget was dropped on the home screen first and needs one of ours. */
    var bindId by mutableIntStateOf(-1)
    /** Shows the "add to home screen" sheet. */
    var adding by mutableStateOf(false)
    private var madeRef: String? = null

    fun startFrom(type: String, design: String, bindId: Int = -1, theme: String? = null) {
        val t = Catalog.type(type)
        val d = t.designs.firstOrNull { it.id == design } ?: t.designs[0]
        val ref = when (type) {
            "countdown" -> if (d.id == "stack") null else uid().also { id -> AppStore.change { dates.add(DateItem(id, "", today().plusDays(30).toString(), false)) } }
            "streak" -> uid().also { id -> AppStore.change { habits.add(Habit(id, "", 0, 1, mutableSetOf(), mutableSetOf())) } }
            "counter" -> uid().also { id -> AppStore.change { counters.add(Counter(id, "", "glasses", 8, mutableMapOf())) } }
            "pace" -> uid().also { id -> AppStore.change { goals.add(Goal(id, "", "pages", 100.0, 0.0, today().toString(), today().plusDays(30).toString(), 5.0)) } }
            "note" -> uid().also { id -> AppStore.change { notes.add(Note(id, "")) } }
            else -> null
        }
        madeRef = ref
        conf = WConf(uid(), type, d.id, d.size, ref = ref, theme = theme ?: "paper")
        this.bindId = bindId; editing = false; adding = false; open = true
    }

    fun startEdit(c: WConf, add: Boolean = false) {
        bindId = -1; conf = c; editing = true; madeRef = null; open = true; adding = add
    }

    /** Save the widget to My widgets (new or changed). */
    fun commit() {
        val c = conf ?: return
        AppStore.change { val i = placed.indexOfFirst { it.id == c.id }; if (i >= 0) placed[i] = c else placed.add(0, c) }
        madeRef = null; editing = true
    }

    fun close() {
        // a new widget left without saving leaves nothing behind
        val r = madeRef
        if (r != null && AppStore.data.placed.none { it.ref == r }) AppStore.change { dates.removeAll { it.id == r }; habits.removeAll { it.id == r }; counters.removeAll { it.id == r }; goals.removeAll { it.id == r }; notes.removeAll { it.id == r } }
        madeRef = null; open = false; conf = null; bindId = -1; adding = false
    }

    fun delete(c: WConf) {
        AppStore.change(true) {
            placed.removeAll { it.id == c.id }
            val r = c.ref
            if (r != null && placed.none { it.ref == r }) { dates.removeAll { it.id == r }; habits.removeAll { it.id == r }; counters.removeAll { it.id == r }; goals.removeAll { it.id == r }; notes.removeAll { it.id == r } }
        }
        open = false; conf = null; adding = false
        Toaster.show("Widget deleted", true)
    }
}

/** The name people know a widget by: their title, or what it tracks. */
fun widgetName(c: WConf): String {
    c.title?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val d = AppStore.data
    return when (c.type) {
        "countdown" -> if (c.design == "stack") "Coming up" else d.dates.firstOrNull { it.id == c.ref }?.name?.ifBlank { null } ?: "Countdown"
        "streak" -> d.habits.firstOrNull { it.id == c.ref }?.name?.ifBlank { null } ?: "Streak"
        "counter" -> d.counters.firstOrNull { it.id == c.ref }?.name?.ifBlank { null } ?: "Counter"
        "clock" -> d.clocks.take(if (c.design == "pair") 2 else 3).joinToString(" · ") { it.city }.ifBlank { "World clock" }
        "pace" -> d.goals.firstOrNull { it.id == c.ref }?.name?.ifBlank { null } ?: "Pace"
        "note" -> d.notes.firstOrNull { it.id == c.ref }?.text?.lineSequence()?.firstOrNull()?.take(30)?.ifBlank { null } ?: "Note"
        else -> Catalog.type(c.type).name
    }
}

@Composable
fun CreateScreen() {
    AppStore.rev
    val p = pal()
    BackHandler { Create.close() }
    val c = Create.conf ?: return
    val ok = infoOk(c)
    val onPhone = Widgets.isOnPhone(c.id)
    val saved = AppStore.data.placed.any { it.id == c.id }
    Column(Modifier.fillMaxSize().background(p.paper).windowInsetsPadding(WindowInsets.systemBars).imePadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(Ic.back, "Back") { Create.close() }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(Catalog.type(c.type).name, style = T.bodyB.copy(fontSize = 17.sp), color = p.ink)
                Text("${Catalog.design(c.type, c.design).name} · ${Catalog.sizeGrid[c.size]}", style = T.small, color = p.muted)
            }
            if (saved) RoundBtn(Ic.trash, "Delete") { Create.delete(c) }
        }
        Box(Modifier.padding(horizontal = Pad)) { Preview(c) }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Pad).padding(top = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHead("1", infoQuestion(c), infoSub(c))
            InfoFields(c)
            Spacer(Modifier.height(4.dp))
            SectionHead("2", "Size on your home screen", null)
            SizePicker(c)
            Spacer(Modifier.height(4.dp))
            SectionHead("3", "Theme", "Pick a look. Each one is made to fit every widget.")
            StyleSection(c)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Pad).padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                !ok -> BigButton(hint(c), kind = "ghost") {}
                onPhone -> BigButton("Save") { Create.commit(); Create.close(); Toaster.show("Saved. Your home screen is updated.") }
                else -> BigButton("Add to home screen") { Create.commit(); Create.adding = true }
            }
            if (ok && !onPhone) Text(if (saved) "Save changes only" else "Save to My widgets for later", style = T.smallB, color = p.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { Create.commit(); Create.close(); Nav.screen = "mine"; Toaster.show("Saved to My widgets") }.padding(10.dp))
        }
    }
    if (Create.adding) AddSheet(c)
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    val p = pal()
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = p.ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SectionHead(n: String, title: String, sub: String?) {
    val p = pal()
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(p.ink), contentAlignment = Alignment.Center) {
            Text(n, style = T.smallB, color = p.paper)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = T.h3.copy(fontSize = 19.sp), color = p.ink)
            if (sub != null) Text(sub, style = T.small, color = p.muted, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Small / Wide / Large: switches to this widget's design in that size. */
@Composable
private fun SizePicker(c: WConf) {
    val p = pal()
    val t = Catalog.type(c.type)
    val locked = Widgets.isOnPhone(c.id)
    val sizes = listOf("s", "w", "m").filter { s -> t.designs.any { it.size == s } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        sizes.forEach { s ->
            val on = c.size == s
            val enabled = !locked || on
            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) p.ink else p.card)
                .clickable(enabled = enabled) {
                    val d = t.designs.first { it.size == s }
                    Create.conf = c.copy(design = d.id, size = s)
                }.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SizeIcon(s, if (on) p.paper else if (enabled) p.ink else p.muted.copy(alpha = 0.4f))
                Spacer(Modifier.height(6.dp))
                Text(Catalog.sizeName[s] ?: "", style = T.smallB, color = if (on) p.paper else if (enabled) p.ink else p.muted.copy(alpha = 0.4f))
                Text(Catalog.sizeGrid[s] ?: "", style = T.small.copy(fontSize = 12.sp), color = if (on) p.paper.copy(alpha = 0.7f) else p.muted)
            }
        }
    }
    if (locked) Text("It's on your home screen at this size. To change size, add it again and remove the old one.", style = T.small, color = p.muted)
}

@Composable
private fun SizeIcon(s: String, color: Color) {
    val (w, h) = when (s) { "s" -> 18.dp to 18.dp; "w" -> 30.dp to 9.dp; else -> 30.dp to 18.dp }
    Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(w, h).clip(RoundedCornerShape(4.dp)).border(2.dp, color, RoundedCornerShape(4.dp)))
    }
}

/** The widget at a fixed size on a wallpaper-coloured backdrop. */
@Composable
fun Preview(c: WConf, sample: Boolean = false, modifier: Modifier = Modifier) {
    AppStore.rev
    val (wbg, _) = wall()
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(wbg).padding(vertical = 18.dp, horizontal = 18.dp), contentAlignment = Alignment.Center) {
        WidgetView(c, Modifier.widthIn(max = if (c.size == "s") 160.dp else 330.dp), live = false, sample = sample)
    }
}

/** A widget shrunk to fit a box, for thumbnails. */
@Composable
fun Thumb(c: WConf, boxW: Float, boxH: Float, sample: Boolean = false, onTap: (() -> Unit)? = null) {
    AppStore.rev
    val bw = if (c.size == "s") 160f else 330f
    val bh = preferredHeight(c, bw)
    val k = minOf((boxW - 8f) / bw, (boxH - 8f) / bh)
    Box(Modifier.size(boxW.dp, boxH.dp), contentAlignment = Alignment.Center) {
        WidgetView(c, Modifier.requiredWidth(bw.dp).graphicsLayer(scaleX = k, scaleY = k), live = false, sample = sample, onTap = onTap)
    }
}

/** One line on what each widget does, for the gallery. */
fun typeBlurb(type: String) = when (type) {
    "countdown" -> "Days until a date that matters"
    "streak" -> "Tick off a habit every day"
    "spend" -> "What you can spend today"
    "subs" -> "Your next subscription charge"
    "counter" -> "Tap +1 for water, coffee, reps"
    "clock" -> "The time where your people are"
    "fast" -> "A fasting timer, one tap to start"
    "pace" -> "Are you on track for your goal?"
    "year" -> "How much of the year is gone"
    "today" -> "Today's date and the month"
    "moon" -> "Tonight's moon phase"
    "note" -> "A line you see every unlock"
    "next" -> "What's next on your schedule"
    "attendance" -> "Classes you can miss, or must attend"
    "now" -> "The one thing that needs you"
    else -> ""
}

/* ---------------- 2. Your info ---------------- */

private fun infoOk(c: WConf): Boolean {
    val d = AppStore.data
    return when (c.type) {
        "countdown" -> if (c.design == "stack") d.dates.any { it.name.isNotBlank() } else d.dates.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "streak" -> d.habits.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "counter" -> d.counters.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "spend" -> d.budget > 0
        "subs" -> d.subs.isNotEmpty()
        "clock" -> d.clocks.isNotEmpty()
        "pace" -> d.goals.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "note" -> d.notes.firstOrNull { it.id == c.ref }?.text?.isNotBlank() == true
        "next" -> d.sched.values.any { it.isNotEmpty() }
        "attendance" -> d.subjects.isNotEmpty()
        else -> true
    }
}

private fun infoQuestion(c: WConf) = when (c.type) {
    "countdown" -> if (c.design == "stack") "Your countdowns" else "What are you counting down to?"
    "streak" -> "What habit are you building?"
    "counter" -> "What are you counting?"
    "spend" -> "What's your budget?"
    "subs" -> "Which subscriptions do you pay?"
    "clock" -> "Which cities?"
    "fast" -> "Your fasting goal"
    "pace" -> "What's your goal?"
    "year" -> "Nothing to fill in"
    "today" -> "Nothing to fill in"
    "moon" -> "Nothing to fill in"
    "note" -> "What should it say?"
    "next" -> "Your weekly schedule"
    "attendance" -> "Your classes"
    "now" -> "Nothing to fill in"
    else -> "Your details"
}

private fun infoSub(c: WConf): String? = when (c.type) {
    "streak" -> "Tap the ✓ on the widget each day you do it."
    "counter" -> "Tap the widget to add one. It starts at zero every day."
    "spend" -> "Shows what you can spend so your budget lasts."
    "subs" -> "Shows the next charge before it hits."
    "clock" -> "Pick up to 3. The Wide size shows two."
    "fast" -> "Start and end your fast with one tap on the widget."
    "pace" -> "Shows what you need each day to hit it, and whether you're ahead."
    "year" -> "It works out how much of the year, month, week and day is gone."
    "today" -> "It shows today's date and updates at midnight."
    "moon" -> "It works out tonight's moon phase on your phone."
    "next" -> "Shows what's next and how long until it starts."
    "attendance" -> "Shows how many classes you can miss, or must attend."
    "now" -> "It looks across your other widgets and shows the one thing that needs you."
    else -> null
}

@Composable
private fun InfoFields(c: WConf) {
    AppStore.rev
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (Create.editing) StatsCard(c)
        when (c.type) {
            "countdown" -> CountdownInfo(c)
            "streak" -> StreakInfo(c)
            "counter" -> { CounterInfo(c); RemindToggle("water", "Nudge me if I'm behind", "Every 2 hours, 10 am to 8 pm") }
            "spend" -> SpendInfo(c)
            "subs" -> { SubsInfo(); RemindToggle("subs", "Remind me the day before", "So you can cancel in time") }
            "clock" -> ClockInfo()
            "fast" -> FastInfo()
            "pace" -> PaceInfo(c)
            "note" -> NoteInfo(c)
            "next" -> SchedInfo()
            "attendance" -> ClassesInfo()
        }
    }
}

private fun hint(c: WConf) = when (c.type) {
    "spend" -> "Enter your budget above"
    "subs" -> "Add a subscription above"
    "clock" -> "Pick a city above"
    "countdown" -> if (c.design == "stack") "Make a countdown first" else "Give it a name above"
    "note" -> "Write your note above"
    "next" -> "Add something to your schedule"
    "attendance" -> "Add a class above"
    else -> "Give it a name above"
}

private fun num(s: String): Double? = s.replace(",", ".").trim().toDoubleOrNull()

@Composable
private fun CountdownInfo(c: WConf) {
    AppStore.rev
    if (c.design == "stack") {
        Text("This design lists your next three countdowns. Make them with the Small or Wide size first.", style = T.body, color = pal().muted); return
    }
    val x = AppStore.data.dates.firstOrNull { it.id == c.ref } ?: return
    var name by remember(x.id) { mutableStateOf(x.name) }
    Field("Name", name, { name = it; AppStore.change { x.name = it.take(40) } }, "My birthday")
    DateField("Date", x.date) { v -> AppStore.change { x.date = v } }
    ToggleRow("Repeats every year", x.yearly, "For birthdays and holidays") { AppStore.change { x.yearly = !x.yearly } }
}

@Composable
private fun StreakInfo(c: WConf) {
    AppStore.rev
    val h = AppStore.data.habits.firstOrNull { it.id == c.ref } ?: return
    var name by remember(h.id) { mutableStateOf(h.name) }
    Field("Habit", name, { name = it; AppStore.change { h.name = it.take(30) } }, "Run")
    HabitCalendar(h)
    RemindToggle("habit", "Remind me if I forget", "A nudge if you haven't checked in")
}

@Composable
private fun CounterInfo(c: WConf) {
    AppStore.rev
    val ct = AppStore.data.counters.firstOrNull { it.id == c.ref } ?: return
    var name by remember(ct.id) { mutableStateOf(ct.name) }
    var goal by remember(ct.id) { mutableStateOf(ct.goal.toString()) }
    var unit by remember(ct.id) { mutableStateOf(ct.unit) }
    Field("Name", name, { name = it; AppStore.change { ct.name = it.take(30) } }, "Water")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Field("Daily goal", goal, { goal = it; it.toIntOrNull()?.let { g -> AppStore.change { ct.goal = g.coerceIn(1, 999) } } }, "8", KeyboardType.Number, Modifier.weight(1f))
        Field("Counted in", unit, { unit = it; AppStore.change { ct.unit = it.ifBlank { "times" }.take(14) } }, "glasses", modifier = Modifier.weight(1f))
    }
    CounterHistory(ct)
}

@Composable
private fun SpendInfo(c: WConf) {
    AppStore.rev
    val d = AppStore.data
    val p = pal()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Budget for")
        Seg(listOf("month" to "A month", "week" to "A week", "custom" to "Set dates"), d.bPeriod) { v ->
            AppStore.change { bPeriod = v; if (v == "custom" && bStart == null) { bStart = today().toString(); bEnd = today().plusDays(13).toString() } }
        }
    }
    when (d.bPeriod) {
        "week" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Week starts on")
            Seg(listOf(1 to "Monday", 7 to "Sunday", 6 to "Saturday"), d.weekStart) { v -> AppStore.change { weekStart = v } }
        }
        "custom" -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField("From", d.bStart ?: today().toString(), Modifier.weight(1f)) { v -> AppStore.change { bStart = v; if ((bEnd ?: v) < v) bEnd = v } }
            DateField("To", d.bEnd ?: today().plusDays(13).toString(), Modifier.weight(1f)) { v -> if (v >= (d.bStart ?: v)) AppStore.change { bEnd = v } else Toaster.show("Pick a day after the start") }
        }
        else -> Picker("Starts again on", "Day ${d.payday} of each month (payday)", (1..28).map { it to "Day $it" }) { v -> AppStore.change { payday = v } }
    }
    var budget by remember { mutableStateOf(if (d.budget > 0) Fmt.trimNum(d.budget) else "") }
    val forWhat = when (d.bPeriod) { "week" -> "for the week"; "custom" -> "for these dates"; else -> "for the month" }
    Field("Budget $forWhat (${d.cur})", budget, { budget = it; AppStore.change { this.budget = maxOf(0.0, num(it) ?: 0.0) } }, "2000", KeyboardType.Decimal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Each day's amount")
        Seg(listOf(true to "Adjusts as I spend", false to "Same every day"), d.bFlex) { v -> AppStore.change { bFlex = v } }
        Text(if (d.bFlex) "Spend less today and the coming days get a little more. Spend more and they get a little less, so the total still lasts."
            else "The budget is split evenly. Every day gets the same amount, whatever you spent yesterday.", style = T.small, color = p.muted)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("This widget shows")
        val opts = listOf("day" to "Today", "week" to "This week", "period" to when (d.bPeriod) { "week" -> "Whole week"; "custom" -> "Whole trip"; else -> "Whole month" })
            .filter { !(it.first == "week" && d.bPeriod == "week") }
        Seg(opts, c.opt ?: "day") { v -> Create.conf = c.copy(opt = v) }
    }
    SpendHistory()
}

@Composable
private fun SubsInfo() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    if (d.subs.isNotEmpty()) ListCard {
        d.subs.forEachIndexed { i, s ->
            ListRow(s.name, "${Fmt.money(s.amt)} a ${s.cycle} · next ${Fmt.short(java.time.LocalDate.parse(s.next))}", divider = i < d.subs.size - 1,
                trailing = { Icon(Ic.x, "Remove", tint = p.muted, modifier = Modifier.size(18.dp)) }) { AppStore.change { subs.remove(s) } }
        }
    }
    var name by remember { mutableStateOf("") }
    var amt by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("month") }
    var next by remember { mutableStateOf(today().plusDays(7).toString()) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (d.subs.isEmpty()) "Add your first one" else "Add another", style = T.bodyB, color = p.ink)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Netflix", "Spotify", "YouTube Premium", "iCloud+", "Prime", "Disney+", "Gym", "ChatGPT").forEach { n -> Chip(n, name == n) { name = n } }
        }
        Field("Name", name, { name = it }, "Netflix")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Price", amt, { amt = it }, "9.99", KeyboardType.Decimal, Modifier.weight(1f))
            Picker("Billed", if (cycle == "year") "Yearly" else "Monthly", listOf("month" to "Monthly", "year" to "Yearly"), Modifier.weight(1f)) { cycle = it }
        }
        DateField("Next charge", next) { next = it }
        val ready = name.isNotBlank() && num(amt) != null
        BigButton("Add ${name.ifBlank { "subscription" }}", kind = if (ready) "solid" else "ghost") {
            val a = num(amt) ?: run { Toaster.show("Add a price"); return@BigButton }
            if (name.isBlank()) { Toaster.show("Add a name"); return@BigButton }
            AppStore.change { subs.add(Sub(uid(), name.trim(), a, cycle, next)) }
            name = ""; amt = ""
        }
    }
}

@Composable
private fun ClockInfo() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    var q by remember { mutableStateOf("") }
    if (d.clocks.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        d.clocks.forEach { k ->
            Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(p.ink).clickable { AppStore.change { clocks.remove(k) } }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(k.city, style = T.smallB, color = p.paper)
                Icon(Ic.x, "Remove", tint = p.paper, modifier = Modifier.size(14.dp))
            }
        }
    }
    Field("Find a city", q, { q = it }, "London")
    val list = CITIES.filter { (c, _) -> q.isBlank() || c.contains(q.trim(), ignoreCase = true) }.filter { (c, _) -> d.clocks.none { it.city == c } }
    ListCard {
        list.take(if (q.isBlank()) 12 else 30).forEachIndexed { i, (city, tz) ->
            ListRow(city, com.phani.widgets.core.tzOffset(tz), divider = i < minOf(list.size, if (q.isBlank()) 12 else 30) - 1,
                trailing = { Icon(Ic.plus, "Add", tint = p.muted, modifier = Modifier.size(18.dp)) }) {
                if (d.clocks.size >= 3) Toaster.show("Up to 3 cities. Remove one first.")
                else { AppStore.change { clocks.add(WClock(uid(), city, tz)) }; q = "" }
            }
        }
    }
}

/* ---------------- Style ---------------- */

/** Pick a ready-made theme. Every theme is designed to work with every widget, so there's nothing else to tweak. */
@Composable
private fun StyleSection(c: WConf) {
    AppStore.rev
    val p = pal()
    val set: (WConf) -> Unit = { Create.conf = it }
    if (Catalog.type(c.type).designs.count { it.size == c.size } > 1) {
        FieldLabel("Layout")
        DesignTab(c, set)
        Spacer(Modifier.height(4.dp))
    }
    FieldLabel("Theme")
    val cur = c.theme ?: "paper"
    com.phani.widgets.widget.Themes.all.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { t ->
                val on = cur == t.id
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(p.card)
                    .then(if (on) Modifier.border(2.dp, p.ink, RoundedCornerShape(20.dp)) else Modifier)
                    .clickable { set(c.copy(theme = t.id, title = null, bg = null, fg = null, alpha = 100, radius = -1, outline = false)) }.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(themeBackdrop(t.id)), contentAlignment = Alignment.Center) {
                        Thumb(c.copy(id = "th" + t.id, theme = t.id), 156f, 108f) { set(c.copy(theme = t.id)) }
                    }
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Text(t.name, style = T.smallB, color = p.ink)
                        Text(t.blurb, style = T.small.copy(fontSize = 12.sp), color = p.muted, maxLines = 1)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** See-through themes are shown over a photo-like backdrop so the glass effect is visible. */
@Composable
internal fun themeBackdrop(id: String): androidx.compose.ui.graphics.Brush {
    val (wbg, _) = wall()
    return if (id == "glass" || id == "glassdark" || id == "clear")
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2B5876), Color(0xFFD38A5C), Color(0xFFF2C38B)))
    else androidx.compose.ui.graphics.Brush.linearGradient(listOf(wbg, wbg))
}

@Composable
private fun DesignTab(c: WConf, set: (WConf) -> Unit) {
    AppStore.rev
    val p = pal()
    val t = Catalog.type(c.type)
    val designs = t.designs.filter { it.size == c.size }
    designs.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { d ->
                val on = c.design == d.id
                val th = c.copy(id = "th" + d.id, design = d.id, size = d.size)
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(p.card)
                    .then(if (on) Modifier.border(2.dp, p.ink, RoundedCornerShape(20.dp)) else Modifier)
                    .clickable { set(c.copy(design = d.id, size = d.size)) }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (wbg, _) = wall()
                    Box(Modifier.fillMaxWidth().height(134.dp).clip(RoundedCornerShape(14.dp)).background(wbg), contentAlignment = Alignment.Center) {
                        Thumb(th, 156f, 130f) { set(c.copy(design = d.id, size = d.size)) }
                    }
                    Text(d.name, style = T.smallB, color = p.ink)
                    Text("${Catalog.sizeName[d.size]} · ${Catalog.sizeGrid[d.size]}", style = T.small.copy(fontSize = 12.sp), color = p.muted)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColourTab(c: WConf, set: (WConf) -> Unit) {
    AppStore.rev
    val p = pal()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Starting look")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Catalog.styles.forEach { (k, l, _) -> Chip(l, c.style == k && c.bg == null) { set(c.copy(style = k, bg = null, fg = null, alpha = 100)) } }
        }
    }
    ColorField("Accent", com.phani.widgets.widget.ACCENTS[c.accent]?.let { hex(it) } ?: c.accent, ACCENT_PALETTE, auto = null) { set(c.copy(accent = it ?: "signal")) }
    ColorField("Background", c.bg, BG_PALETTE, auto = "From look") { set(c.copy(bg = it)) }
    SliderField("See-through", (100 - c.alpha).toFloat(), 0f..100f, if (c.alpha == 100) "Solid" else "${100 - c.alpha}%") { set(c.copy(alpha = 100 - it.toInt())) }
}

@Composable
private fun TextTab(c: WConf, set: (WConf) -> Unit) {
    AppStore.rev
    val p = pal()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Typeface")
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Catalog.fonts.forEach { (k, l, _) ->
                val on = c.font == k
                val fam = when (k) { "serif" -> Fonts.serif; "mono" -> Fonts.mono; else -> Fonts.ui }
                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) p.ink else Color.Transparent).clickable { set(c.copy(font = k)) },
                    contentAlignment = Alignment.Center) {
                    Text(l, style = T.smallB.copy(fontFamily = fam, fontSize = if (k == "serif") 17.sp else 14.sp, fontWeight = if (k == "serif") FontWeight.Normal else FontWeight.SemiBold), color = if (on) p.paper else p.muted)
                }
            }
        }
    }
    ColorField("Text colour", c.fg, TEXT_PALETTE, auto = "Auto") { set(c.copy(fg = it)) }
    Field("Title on the widget", c.title ?: "", { set(c.copy(title = it.take(32).ifEmpty { null })) }, "Leave empty for the default")
    ToggleRow("Show the title", c.label) { set(c.copy(label = !c.label)) }
}

@Composable
private fun ShapeTab(c: WConf, set: (WConf) -> Unit) {
    AppStore.rev
    val rad = if (c.radius >= 0) c.radius else (com.phani.widgets.widget.CORNERS[c.corner] ?: 26f).toInt()
    SliderField("Corners", rad.toFloat(), 0f..40f, if (rad == 0) "Square" else "$rad") { set(c.copy(radius = it.toInt())) }
    ToggleRow("Outline", c.outline, "A thin line around the edge") { set(c.copy(outline = !c.outline)) }
    Text("Reset style", style = T.bodyB, color = pal().ink, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
            set(c.copy(style = "paper", accent = "signal", font = "grotesk", corner = "round", label = true, bg = null, fg = null, alpha = 100, radius = -1, outline = false, title = null))
        }.padding(14.dp))
}

/* ---------------- Add to home screen ---------------- */

@Composable
private fun AddSheet(c: WConf) {
    AppStore.rev
    val p = pal()
    val ctx = LocalContext.current
    val on = Widgets.isOnPhone(c.id)
    var guide by remember { mutableStateOf(false) }
    val canPin = remember { android.appwidget.AppWidgetManager.getInstance(ctx).isRequestPinAppWidgetSupported }
    LaunchedEffect(c.id) {
        val id = Create.bindId
        if (id != -1) {
            // the empty widget is already on the home screen: link it now
            AppStore.change { bound[id] = c }
            Widgets.updateAll(ctx)
            Create.bindId = -1
        } else if (!canPin || !Widgets.requestPin(ctx, c)) guide = true
        else { delay(3500); if (!Widgets.isOnPhone(c.id)) guide = true }
    }
    val done = { Create.close(); Nav.screen = "mine" }
    Sheet(if (on) "Added" else "Add to home screen", done) {
        if (on) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(p.good), contentAlignment = Alignment.Center) {
                    Icon(Ic.check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${widgetName(c)} is on your home screen", style = T.bodyB, color = p.ink)
                    Text("Change it in the app any time. It updates by itself.", style = T.small, color = p.muted)
                }
            }
            BigButton("Done") { done() }
        } else if (!guide) {
            Text("Your phone will ask you to confirm. Tap Add on that popup.", style = T.body, color = p.ink)
            Text("Waiting for your phone…", style = T.small, color = p.muted)
        } else {
            Text(if (canPin) "No popup? Some phones block it. Add it by hand, it takes 10 seconds:" else "Add it like this:", style = T.body, color = p.ink)
            Guide(Catalog.sizeName[c.size] ?: "Small", Catalog.sizeGrid[c.size] ?: "", widgetName(c))
            BigButton("Got it") { done() }
        }
    }
}

@Composable
fun Guide(sizeName: String, grid: String, name: String) {
    AppStore.rev
    val p = pal()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.card).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(
            "Go to your home screen and press and hold an empty spot",
            "Tap Widgets, then find Widget App",
            "Drag “Widget App · $sizeName” ($grid) onto the screen",
            "Pick “$name” from the list that opens",
        ).forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(p.ink), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = T.smallB, color = p.paper)
                }
                Spacer(Modifier.width(12.dp))
                Text(s, style = T.body, color = p.ink, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
