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
import com.phani.widgets.core.Sub
import com.phani.widgets.core.WClock
import com.phani.widgets.core.WConf
import com.phani.widgets.core.today
import com.phani.widgets.core.uid
import com.phani.widgets.widget.Widgets
import com.phani.widgets.widget.preferredHeight
import kotlinx.coroutines.delay

/**
 * Making or editing one widget, as four full-screen steps:
 * 0 Pick → 1 Your info → 2 Style → 3 Add to phone.
 */
object Create {
    var open by mutableStateOf(false)
    var step by mutableIntStateOf(0)
    var conf by mutableStateOf<WConf?>(null)
    var editing by mutableStateOf(false)
    /** Set when a widget was dropped on the home screen first and needs one of ours. */
    var bindId by mutableIntStateOf(-1)
    var size by mutableStateOf<String?>(null)
    private var madeRef: String? = null

    fun startNew(bindId: Int = -1, size: String? = null) {
        this.bindId = bindId; this.size = size
        conf = null; editing = false; madeRef = null; step = 0; open = true
    }

    fun startEdit(c: WConf) {
        bindId = -1; size = null; conf = c; editing = true; madeRef = null; step = 1; open = true
    }

    fun pick(type: String) {
        val t = Catalog.type(type)
        val d = t.designs.firstOrNull { size == null || it.size == size } ?: t.designs[0]
        val ref = when (type) {
            "countdown" -> uid().also { id -> AppStore.change { dates.add(DateItem(id, "", today().plusDays(30).toString(), false)) } }
            "streak" -> uid().also { id -> AppStore.change { habits.add(Habit(id, "", 0, 1, mutableSetOf(), mutableSetOf())) } }
            "counter" -> uid().also { id -> AppStore.change { counters.add(Counter(id, "", "glasses", 8, mutableMapOf())) } }
            else -> null
        }
        madeRef = ref
        conf = WConf(uid(), type, d.id, d.size, ref = ref)
        step = 1
    }

    /** Save the widget to the app's list (new or changed). */
    fun commit() {
        val c = conf ?: return
        AppStore.change { val i = placed.indexOfFirst { it.id == c.id }; if (i >= 0) placed[i] = c else placed.add(0, c) }
        madeRef = null
    }

    fun close() {
        // a new widget abandoned half-way leaves nothing behind
        val r = madeRef
        if (r != null && AppStore.data.placed.none { it.ref == r }) AppStore.change { dates.removeAll { it.id == r }; habits.removeAll { it.id == r }; counters.removeAll { it.id == r } }
        madeRef = null; open = false; conf = null; bindId = -1; size = null
    }

    fun back() { if (step == 0 || (editing && step == 1)) close() else step-- }

    fun delete(c: WConf) {
        AppStore.change(true) {
            placed.removeAll { it.id == c.id }
            val r = c.ref
            if (r != null && placed.none { it.ref == r }) { dates.removeAll { it.id == r }; habits.removeAll { it.id == r }; counters.removeAll { it.id == r } }
        }
        open = false; conf = null
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
        else -> Catalog.type(c.type).name
    }
}

@Composable
fun CreateScreen() {
    AppStore.rev
    val p = pal()
    AppStore.rev
    BackHandler { Create.back() }
    val c = Create.conf
    Column(Modifier.fillMaxSize().background(p.paper).windowInsetsPadding(WindowInsets.systemBars).imePadding()) {
        // header: back · step · close
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(if (Create.step == 0 || (Create.editing && Create.step == 1)) Ic.x else Ic.back, "Back") { Create.back() }
            Spacer(Modifier.weight(1f))
            StepDots(Create.step)
            Spacer(Modifier.weight(1f))
            if (Create.editing && c != null && Create.step == 1) RoundBtn(Ic.trash, "Delete") { Create.delete(c) }
            else Spacer(Modifier.size(44.dp))
        }
        when {
            Create.step == 0 || c == null -> PickStep()
            Create.step == 1 -> InfoStep(c)
            Create.step == 2 -> StyleStep(c)
            else -> AddStep(c)
        }
    }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    AppStore.rev
    val p = pal()
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = p.ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StepDots(step: Int) {
    AppStore.rev
    val p = pal()
    val names = listOf("Pick", "Your info", "Style", "Add")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Step ${step + 1} of 4 · ${names[step]}", style = T.smallB, color = p.muted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (0..3).forEach { i -> Box(Modifier.height(4.dp).width(if (i == step) 22.dp else 10.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) p.ink else p.soft)) }
        }
    }
}

/** Title + scrolling body + a fixed bottom button: the shape of every step. */
@Composable
private fun ColumnScope.StepBody(title: String, sub: String?, top: (@Composable () -> Unit)? = null, bottom: @Composable () -> Unit, body: @Composable ColumnScope.() -> Unit) {
    AppStore.rev
    val p = pal()
    if (top != null) Box(Modifier.padding(horizontal = Pad)) { top() }
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Pad).padding(top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text(title, style = T.h1.copy(fontSize = 28.sp, lineHeight = 31.sp), color = p.ink)
            if (sub != null) Text(sub, style = T.body, color = p.muted, modifier = Modifier.padding(top = 6.dp))
        }
        body()
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = Pad).padding(top = 8.dp, bottom = 12.dp)) { bottom() }
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

/* ---------------- 1. Pick ---------------- */

@Composable
private fun ColumnScope.PickStep() {
    AppStore.rev
    val p = pal()
    val size = Create.size
    val types = Catalog.visible.filter { t -> size == null || t.designs.any { it.size == size } }
    StepBody(
        "What should it show?",
        if (size != null) "For your ${Catalog.sizeName[size]} widget. Tap one." else "Tap one. You'll add your own info next.",
        bottom = {},
    ) {
        types.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t ->
                    val d = t.designs.firstOrNull { size == null || it.size == size } ?: t.designs[0]
                    val conf = WConf("pk" + t.type, t.type, d.id, d.size)
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(p.card).clickable { Create.pick(t.type) }.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (wbg, _) = wall()
                        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(wbg), contentAlignment = Alignment.Center) {
                            Thumb(conf, 156f, 146f, sample = true) { Create.pick(t.type) }
                        }
                        Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text(t.name, style = T.bodyB, color = p.ink)
                            Text(short(t.type), style = T.small, color = p.muted, maxLines = 2)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun short(type: String) = when (type) {
    "countdown" -> "Days until a date that matters"
    "streak" -> "Tick off a habit every day"
    "spend" -> "What you can spend today"
    "subs" -> "Your next subscription charge"
    "counter" -> "Tap +1 for water, coffee, reps"
    "clock" -> "The time where your people are"
    else -> ""
}

/* ---------------- 2. Your info ---------------- */

@Composable
private fun ColumnScope.InfoStep(c: WConf) {
    AppStore.rev
    val d = AppStore.data
    val ok = when (c.type) {
        "countdown" -> c.design == "stack" || d.dates.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "streak" -> d.habits.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "counter" -> d.counters.firstOrNull { it.id == c.ref }?.name?.isNotBlank() == true
        "spend" -> d.budget > 0
        "subs" -> d.subs.isNotEmpty()
        "clock" -> d.clocks.isNotEmpty()
        else -> true
    }
    val (title, sub) = when (c.type) {
        "countdown" -> "What are you counting down to?" to null
        "streak" -> "What habit are you building?" to "Tap the ✓ on the widget each day you do it."
        "counter" -> "What are you counting?" to "Tap the widget to add one. It starts again at zero every day."
        "spend" -> "What's your monthly budget?" to "The widget shows what you can spend today so the month lasts. Log spending with its buttons."
        "subs" -> "Which subscriptions do you pay?" to "The widget shows the next charge before it hits."
        "clock" -> "Which cities?" to "Pick up to 3. Two show on the Wide size."
        else -> "Your info" to null
    }
    StepBody(title, sub, top = { Preview(c) }, bottom = {
        BigButton(if (ok) "Next: style it" else hint(c.type), kind = if (ok) "solid" else "ghost") { if (ok) Create.step = 2 }
    }) {
        when (c.type) {
            "countdown" -> CountdownInfo(c)
            "streak" -> StreakInfo(c)
            "counter" -> CounterInfo(c)
            "spend" -> SpendInfo()
            "subs" -> SubsInfo()
            "clock" -> ClockInfo()
        }
    }
}

private fun hint(type: String) = when (type) {
    "spend" -> "Enter a budget to continue"
    "subs" -> "Add one to continue"
    "clock" -> "Pick a city to continue"
    else -> "Give it a name to continue"
}

private fun num(s: String): Double? = s.replace(",", ".").trim().toDoubleOrNull()

@Composable
private fun CountdownInfo(c: WConf) {
    AppStore.rev
    if (c.design == "stack") {
        Text("This design lists all your countdowns. Make a countdown first with another design.", style = T.body, color = pal().muted); return
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
    val t = today().toString()
    ToggleRow("I did it today", t in h.done) { AppStore.change { if (t in h.done) h.done.remove(t) else h.done.add(t) } }
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
}

@Composable
private fun SpendInfo() {
    AppStore.rev
    val d = AppStore.data
    var budget by remember { mutableStateOf(if (d.budget > 0) Fmt.trimNum(d.budget) else "") }
    Field("Monthly budget (${d.cur})", budget, { budget = it; AppStore.change { this.budget = maxOf(0.0, num(it) ?: 0.0) } }, "2000", KeyboardType.Decimal)
    Picker("Starts again on", "Day ${d.payday} of each month", (1..28).map { it to "Day $it" }) { v -> AppStore.change { payday = v } }
    Text("Not counting rent or bills? Leave them out of the budget.", style = T.small, color = pal().muted)
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

/* ---------------- 3. Style ---------------- */

@Composable
private fun ColumnScope.StyleStep(start: WConf) {
    AppStore.rev
    var tab by remember { mutableStateOf("design") }
    val c = start
    val set: (WConf) -> Unit = { Create.conf = it }
    val p = pal()
    StepBody("Make it yours", null, top = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Preview(c)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("design" to "Design", "colour" to "Colour", "text" to "Text", "shape" to "Shape").forEach { (k, l) ->
                    val on = tab == k
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) p.ink else Color.Transparent).clickable { tab = k },
                        contentAlignment = Alignment.Center) { Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted) }
                }
            }
        }
    }, bottom = {
        BigButton(if (Create.editing) "Save" else "Next: add to phone") {
            Create.commit()
            if (Create.editing && Widgets.isOnPhone(c.id)) { Create.close(); Toaster.show("Saved. Your home screen is updated.") } else Create.step = 3
        }
    }) {
        when (tab) {
            "design" -> DesignTab(c, set)
            "colour" -> ColourTab(c, set)
            "text" -> TextTab(c, set)
            else -> ShapeTab(c, set)
        }
    }
}

@Composable
private fun DesignTab(c: WConf, set: (WConf) -> Unit) {
    AppStore.rev
    val p = pal()
    val t = Catalog.type(c.type)
    val onPhone = Widgets.isOnPhone(c.id)
    val designs = t.designs.filter { Create.size == null || it.size == Create.size }.filter { !(onPhone && it.size != c.size) }
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
    if (onPhone) Text("It's already on your home screen, so only designs of the same size are shown.", style = T.small, color = p.muted)
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

/* ---------------- 4. Add to phone ---------------- */

@Composable
private fun ColumnScope.AddStep(c: WConf) {
    AppStore.rev
    val p = pal()
    val ctx = LocalContext.current
    // dropped on the home screen first: link it now
    LaunchedEffect(c.id) {
        if (Create.bindId != -1) {
            val id = Create.bindId
            AppStore.change { bound[id] = c }
            Widgets.updateAll(ctx)
            Create.bindId = -1
        }
    }
    val on = Widgets.isOnPhone(c.id)
    var asked by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    LaunchedEffect(asked) { if (asked) { delay(3500); if (!Widgets.isOnPhone(c.id)) guide = true } }
    val canPin = remember { android.appwidget.AppWidgetManager.getInstance(ctx).isRequestPinAppWidgetSupported }
    val sizeName = Catalog.sizeName[c.size] ?: "Small"

    if (on) {
        StepBody("It's on your home screen", "Change it here any time. The home screen updates by itself.", top = { Preview(c) }, bottom = {
            BigButton("Done") { Create.close() }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(p.good), contentAlignment = Alignment.Center) {
                    Icon(Ic.check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("${widgetName(c)} is live", style = T.bodyB, color = p.ink)
            }
        }
        return
    }

    StepBody("Put it on your home screen", "It's saved. One more step to see it on your phone.", top = { Preview(c) }, bottom = {
        if (canPin && !guide) {
            BigButton(if (asked) "Waiting for your phone…" else "Add to home screen") {
                asked = true
                if (!Widgets.requestPin(ctx, c)) guide = true
            }
            Spacer(Modifier.height(8.dp))
        }
        BigButton(if (guide || !canPin) "Done" else "Later", kind = "ghost") { Create.close() }
    }) {
        if (asked && !guide) Text("Your phone should ask you to confirm. Tap Add on that popup.", style = T.body, color = p.muted)
        if (guide || !canPin) {
            Text(if (canPin) "No popup? Some phones block it. Adding it by hand takes 10 seconds:" else "Add it like this:", style = T.body, color = p.ink)
            Guide(sizeName, Catalog.sizeGrid[c.size] ?: "", widgetName(c))
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
