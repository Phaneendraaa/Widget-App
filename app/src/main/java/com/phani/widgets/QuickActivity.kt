package com.phani.widgets

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.FastRec
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.Spend
import com.phani.widgets.core.cnt
import com.phani.widgets.core.spendState
import com.phani.widgets.core.streak
import com.phani.widgets.core.today
import com.phani.widgets.core.uid
import com.phani.widgets.ui.AppTheme
import com.phani.widgets.ui.BigButton
import com.phani.widgets.ui.Chip
import com.phani.widgets.ui.Fonts
import com.phani.widgets.ui.Ic
import com.phani.widgets.ui.Seg
import com.phani.widgets.ui.T
import com.phani.widgets.ui.Toggle
import com.phani.widgets.ui.pal
import com.phani.widgets.widget.Widgets
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * A small panel that opens straight from a home-screen widget, over whatever is on screen:
 * type an exact amount, start a fast that began earlier, untick yesterday… without opening the app.
 */
class QuickActivity : ComponentActivity() {

    companion object {
        /** Widget types whose body tap opens this panel. */
        fun kindFor(type: String): String? = when (type) {
            "spend" -> "spend"; "fast" -> "fast"; "counter" -> "counter"; "streak" -> "habit"; "pace" -> "goal"
            "protein" -> "protein"; "calm" -> "calm"; "prayer", "meds" -> "rituals"
            "countdown", "subs", "attendance", "next", "year", "today", "moon" -> "stats"
            else -> null
        }

        fun intent(ctx: Context, kind: String, ref: String?, cid: String?): Intent =
            Intent(ctx, QuickActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .setData(android.net.Uri.parse("quick://$kind/${ref ?: ""}/${cid ?: ""}"))
                .putExtra("kind", kind).putExtra("ref", ref).putExtra("cid", cid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Widgets.prepare(this)
        Fonts.init(assets)
        val kind = intent.getStringExtra("kind") ?: run { finish(); return }
        val ref = intent.getStringExtra("ref")
        val cid = intent.getStringExtra("cid")
        setContent {
            AppStore.rev
            val dark = when (AppStore.data.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            AppTheme(dark) {
                val p = pal()
                Box(Modifier.fillMaxSize().background(p.scrim).clickable(remember { MutableInteractionSource() }, null) { finish() }, contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.9f).dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(p.paper)
                            .clickable(remember { MutableInteractionSource() }, null) {}
                            .verticalScroll(rememberScrollState())
                            .windowInsetsPadding(WindowInsets.navigationBars).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (kind) {
                            "spend" -> SpendPanel(cid)
                            "fast" -> FastPanel(cid)
                            "counter" -> CounterPanel(ref, cid)
                            "habit" -> HabitPanel(ref, cid)
                            "goal" -> GoalPanel(ref, cid)
                            "protein" -> ProteinPanel(ref, cid)
                            "calm" -> CalmPanel(ref, cid)
                            "rituals" -> RitualPanel(cid)
                            "stats" -> StatsPanel(cid)
                            else -> finish()
                        }
                        // how it's going, right from the home screen
                        val conf = AppStore.data.placed.firstOrNull { it.id == cid }
                        if (conf != null) {
                            if (kind != "stats") com.phani.widgets.ui.StatsCard(conf)
                            Text("Edit this widget", style = T.smallB, color = p.muted, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                    startActivity(Intent(this@QuickActivity, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        .putExtra("open", "open").putExtra("cid", conf.id))
                                    finish()
                                }.padding(10.dp))
                        }
                    }
                }
            }
        }
    }

    /** Save a change made from this panel, marked as coming from the widget so the widget shows Undo. */
    private fun commit(cid: String?, block: com.phani.widgets.core.AppData.() -> Unit) {
        AppStore.pendingCid = cid
        AppStore.change(true, block)
        Widgets.refreshSoon(this, AppStore.UNDO_MS + 500)
    }

    @Composable
    private fun Header(title: String, sub: String?) {
        val p = pal()
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, style = T.h3, color = p.ink)
                if (sub != null) Text(sub, style = T.small, color = p.muted, modifier = Modifier.padding(top = 2.dp))
            }
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(p.card).clickable { finish() }, contentAlignment = Alignment.Center) {
                Icon(Ic.x, "Close", tint = p.ink, modifier = Modifier.size(18.dp))
            }
        }
    }

    /* ---------- Stats only, for widgets without quick actions ---------- */

    @Composable
    private fun ColumnScope.StatsPanel(cid: String?) {
        AppStore.rev
        val c = AppStore.data.placed.firstOrNull { it.id == cid } ?: run { finish(); return }
        Header(com.phani.widgets.ui.widgetName(c), com.phani.widgets.core.Catalog.type(c.type).name)
        com.phani.widgets.ui.StatsCard(c)
    }

    /* ---------- Spending: any amount, with a category ---------- */

    @Composable
    private fun ColumnScope.SpendPanel(cid: String?) {
        AppStore.rev
        val p = pal()
        val s = spendState()
        var amt by remember { mutableStateOf("") }
        var cat by remember { mutableStateOf<String?>(null) }
        Header("Add spending", "${Fmt.money(s.left)} left today")
        Text(if (amt.isEmpty()) Fmt.money(0.0) else money(amt),
            style = T.h1.copy(fontSize = 44.sp, lineHeight = 46.sp), color = if (amt.isEmpty()) p.muted else p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Food", "Coffee", "Groceries", "Transport", "Shopping", "Bills", "Fun", "Other").forEach { c -> Chip(c, cat == c) { cat = if (cat == c) null else c } }
        }
        Keypad { k ->
            amt = when (k) {
                "⌫" -> amt.dropLast(1)
                "." -> if (amt.contains(".")) amt else (amt.ifEmpty { "0" } + ".")
                else -> if (amt.substringAfter('.', "").length >= 2 && amt.contains(".")) amt else if (amt == "0") k else (amt + k).take(9)
            }
        }
        val v = amt.toDoubleOrNull() ?: 0.0
        BigButton(if (v > 0) "Add ${Fmt.money(v)}" else "Type an amount", kind = if (v > 0) "solid" else "ghost") {
            if (v <= 0) return@BigButton
            commit(cid) { log.add(Spend(uid(), v, today().toString(), cat ?: "")) }
            finish()
        }
    }

    private fun money(raw: String): String {
        val v = raw.toDoubleOrNull() ?: 0.0
        val f = Fmt.money(v)
        // keep what was typed visible, e.g. "12." while typing cents
        return if (raw.endsWith(".")) f.substringBeforeLast('.').substringBeforeLast(',') + "." else f
    }

    @Composable
    private fun Keypad(onKey: (String) -> Unit) {
        val p = pal()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(16.dp)).background(p.card).clickable { onKey(k) }, contentAlignment = Alignment.Center) {
                            Text(k, style = T.h3.copy(fontSize = 22.sp), color = p.ink)
                        }
                    }
                }
            }
        }
    }

    /* ---------- Fasting: start earlier, confirm before ending, fix the start time ---------- */

    @Composable
    private fun ColumnScope.FastPanel(cid: String?) {
        AppStore.rev
        val p = pal()
        val ctx = LocalContext.current
        val f = AppStore.data.fast
        val now = System.currentTimeMillis()
        if (f.start == null) {
            var ago by remember { mutableStateOf(0L) }
            var picked by remember { mutableStateOf<Long?>(null) }
            Header("Start a fast", "Forgot to start on time? Pick when you began.")
            Seg(listOf(12, 14, 16, 18, 20).map { it to "${it}h" }, f.target) { v -> AppStore.change { fast.target = v } }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L to "Now", 30L to "30 min ago", 60L to "1 h ago", 120L to "2 h ago", 180L to "3 h ago").forEach { (m, l) ->
                    Chip(l, picked == null && ago == m) { ago = m; picked = null }
                }
                Chip(picked?.let { "At ${clock(it)}" } ?: "Pick a time", picked != null) {
                    pickTime(ctx, now) { picked = it }
                }
            }
            val start = picked ?: (now - ago * 60_000)
            BigButton("Start fasting" + if (start < now - 60_000) " from ${clock(start)}" else "") {
                commit(cid) { fast.start = start }
                finish()
            }
        } else {
            val h = (now - f.start!!) / 3_600_000.0
            Header("Fasting · ${Fmt.hhmm(h)}", "Started ${clock(f.start!!)} · goal ${f.target}h")
            Seg(listOf(12, 14, 16, 18, 20).map { it to "${it}h" }, f.target) { v -> AppStore.change { fast.target = v } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Change start time", false) { pickTime(ctx, now) { t -> AppStore.change { fast.start = t } } }
            }
            BigButton("End fast now") {
                val st = f.start!!
                commit(cid) { fast.history.add(FastRec(st, System.currentTimeMillis())); fast.start = null }
                finish()
            }
            Text("Started by mistake? Delete it", style = T.smallB, color = p.warn, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { commit(cid) { fast.start = null }; finish() }.padding(10.dp))
        }
    }

    private fun clock(ms: Long): String {
        val t = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ms), ZoneId.systemDefault())
        val hm = "%02d:%02d".format(t.hour, t.minute)
        val day = if (t.toLocalDate() == java.time.LocalDate.now()) "" else if (t.toLocalDate() == java.time.LocalDate.now().minusDays(1)) "yesterday " else "${t.toLocalDate()} "
        return day + Fmt.hm(hm)
    }

    /** A time today, or yesterday when it would be in the future. */
    private fun pickTime(ctx: Context, now: Long, onPick: (Long) -> Unit) {
        val n = LocalDateTime.now()
        TimePickerDialog(ctx, { _, hh, mm ->
            var t = n.withHour(hh).withMinute(mm).withSecond(0)
            if (t.isAfter(n)) t = t.minusDays(1)
            onPick(t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }, n.hour, n.minute, AppStore.data.h24).show()
    }

    /* ---------- Counter ---------- */

    @Composable
    private fun ColumnScope.CounterPanel(ref: String?, cid: String?) {
        AppStore.rev
        val p = pal()
        val c = AppStore.data.counters.firstOrNull { it.id == ref } ?: AppStore.data.counters.firstOrNull() ?: run { finish(); return }
        val v = cnt(c)
        val k = today().toString()
        Header(c.name, "Today · goal ${c.goal} ${c.unit}")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundKey("−", false) { if (v > 0) commit(cid) { c.log[k] = v - 1 } }
            Text("$v", style = T.h1.copy(fontSize = 56.sp, lineHeight = 58.sp), color = if (v >= c.goal) com.phani.widgets.ui.Signal else p.ink,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            RoundKey("+", true) { commit(cid) { c.log[k] = v + 1 } }
        }
        Text(if (v >= c.goal) "Goal reached" else "${c.goal - v} more to reach your goal", style = T.body, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        BigButton("Done", kind = "ghost") { finish() }
    }

    @Composable
    private fun RoundKey(t: String, solid: Boolean, onClick: () -> Unit) {
        val p = pal()
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(if (solid) p.ink else p.card)
            .then(if (solid) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(36.dp))).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Text(t, style = T.h1.copy(fontSize = 34.sp), color = if (solid) p.paper else p.ink)
        }
    }

    /* ---------- Protein: any amount ---------- */

    @Composable
    private fun ColumnScope.ProteinPanel(ref: String?, cid: String?) {
        AppStore.rev
        val p = pal()
        val c = AppStore.data.counters.firstOrNull { it.id == ref } ?: AppStore.data.counters.firstOrNull { it.unit == "g" } ?: run { finish(); return }
        val v = cnt(c)
        val k = today().toString()
        Header(c.name, "Today · goal ${c.goal} g")
        Text("${v} g", style = T.h1.copy(fontSize = 56.sp, lineHeight = 58.sp), color = if (v >= c.goal) com.phani.widgets.ui.Signal else p.ink,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(if (v >= c.goal) "Goal reached" else "${c.goal - v} g to go", style = T.body, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        listOf(listOf(5, 10, 15, 20), listOf(25, 30, 40, 50)).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { g ->
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.card).clickable { commit(cid) { c.log[k] = (c.log[k] ?: 0) + g } },
                        contentAlignment = Alignment.Center) { Text("+$g g", style = T.bodyB, color = p.ink) }
                }
            }
        }
        var raw by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                com.phani.widgets.ui.Field("Any amount (g)", raw, { t -> raw = t.filter { it.isDigit() }.take(3) }, "e.g. 32", androidx.compose.ui.text.input.KeyboardType.Number)
            }
        }
        BigButton(if (raw.toIntOrNull() != null) "Add ${raw} g" else "Done", kind = if (raw.toIntOrNull() != null) "solid" else "ghost") {
            val g = raw.toIntOrNull()
            if (g != null && g > 0) { commit(cid) { c.log[k] = (c.log[k] ?: 0) + g }; raw = "" } else finish()
        }
        if (v > 0) Text("Remove 10 g", style = T.smallB, color = p.muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { commit(cid) { c.log[k] = maxOf(0, v - 10) } }.padding(8.dp))
    }

    /* ---------- Meditation: a guided breathing session ---------- */

    @Composable
    private fun ColumnScope.CalmPanel(ref: String?, cid: String?) {
        AppStore.rev
        val p = pal()
        val c = AppStore.data.counters.firstOrNull { it.id == ref } ?: AppStore.data.counters.firstOrNull { it.unit == "min" } ?: run { finish(); return }
        var mins by remember { mutableStateOf(5) }
        var running by remember { mutableStateOf(false) }
        var startAt by remember { mutableStateOf(0L) }
        var elapsed by remember { mutableStateOf(0L) }
        var finished by remember { mutableStateOf<Int?>(null) }
        val k = today().toString()
        fun log(ms: Long) {
            val m = ((ms + 30_000) / 60_000).toInt()
            if (m > 0) commit(cid) { c.log[k] = (c.log[k] ?: 0) + m }
            finished = m; running = false
        }
        if (running) androidx.compose.runtime.LaunchedEffect(startAt) {
            while (true) {
                elapsed = System.currentTimeMillis() - startAt
                if (elapsed >= mins * 60_000L) { log(mins * 60_000L); break }
                kotlinx.coroutines.delay(200)
            }
        }
        val streak = com.phani.widgets.core.calmStreak(c)
        Header(c.name, "Today ${cnt(c)} of ${c.goal} min" + if (streak > 0) " · $streak-day streak" else "")
        // the breathing circle: in 4 · hold 4 · out 4 · hold 4
        val cyc = (elapsed % 16_000L).toFloat()
        val (phase, scale) = when {
            !running -> "Ready" to 0.7f
            cyc < 4000 -> "Breathe in" to (0.55f + 0.45f * cyc / 4000f)
            cyc < 8000 -> "Hold" to 1f
            cyc < 12000 -> "Breathe out" to (1f - 0.45f * (cyc - 8000f) / 4000f)
            else -> "Hold" to 0.55f
        }
        val sc by androidx.compose.animation.core.animateFloatAsState(scale, androidx.compose.animation.core.tween(220), label = "breath")
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(240.dp).androidx_scale(sc).clip(RoundedCornerShape(120.dp)).background(com.phani.widgets.ui.Signal.copy(alpha = 0.12f)))
            Box(Modifier.size(180.dp).androidx_scale(sc).clip(RoundedCornerShape(90.dp)).background(com.phani.widgets.ui.Signal.copy(alpha = 0.22f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (finished != null) "Well done" else phase, style = T.h3, color = p.ink)
                if (running) {
                    val left = (mins * 60_000L - elapsed).coerceAtLeast(0) / 1000
                    Text(String.format(java.util.Locale.US, "%d:%02d", left / 60, left % 60), style = T.body, color = p.muted)
                } else if (finished != null) Text("${finished} min added", style = T.body, color = p.muted)
            }
        }
        if (!running) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10, 15, 20).forEach { m ->
                    val on = m == mins
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(if (on) p.ink else p.card).clickable { mins = m },
                        contentAlignment = Alignment.Center) { Text("$m", style = T.bodyB, color = if (on) p.paper else p.ink) }
                }
            }
            Text("minutes", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            BigButton(if (finished != null) "Again" else "Begin") { finished = null; startAt = System.currentTimeMillis(); elapsed = 0; running = true }
            if (finished != null) BigButton("Done", kind = "ghost") { finish() }
        } else {
            BigButton("End now", kind = "ghost") { log(elapsed) }
        }
    }

    @Composable
    private fun ColumnScope.RitualPanel(cid: String?) {
        AppStore.rev
        val p = pal()
        val conf = AppStore.data.placed.firstOrNull { it.id == cid }
        val type = conf?.type ?: "prayer"
        val items = com.phani.widgets.core.Rituals.items(type, AppStore.data)
        val med = type == "meds"
        Header(if (med) "Medicine" else "Prayer", "Today · ${items.count { it.done }} of ${items.size} " + if (med) "taken" else "done")
        items.forEach { it ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).clickable { commit(cid) { com.phani.widgets.core.Rituals.toggle(this, it.key) } }
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(it.name, style = T.bodyB, color = p.ink)
                    Text(it.time.format(java.time.format.DateTimeFormatter.ofPattern(if (AppStore.data.h24) "HH:mm" else "h:mm a")) + if (it.note.isNotBlank()) " · " + it.note else "",
                        style = T.small, color = p.muted)
                }
                Toggle(it.done)
            }
        }
        BigButton("Done", kind = "ghost") { finish() }
    }

    private fun Modifier.androidx_scale(s: Float) = this.then(Modifier.graphicsLayer(scaleX = s, scaleY = s))

    /* ---------- Streak: today and yesterday ---------- */

    @Composable
    private fun ColumnScope.HabitPanel(ref: String?, cid: String?) {
        AppStore.rev
        val p = pal()
        val h = AppStore.data.habits.firstOrNull { it.id == ref } ?: AppStore.data.habits.firstOrNull() ?: run { finish(); return }
        val st = streak(h)
        Header(h.name, "${st.n}-day streak · best ${maxOf(h.record, st.n)}")
        listOf(0L to "Today", 1L to "Yesterday").forEach { (back, label) ->
            val key = today().minusDays(back).toString()
            val on = key in h.done
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).clickable {
                commit(cid) {
                    if (on) h.done.remove(key) else { h.done.add(key); h.frozen.remove(key); val n = streak(h).n; if (n > h.record) h.record = n }
                }
            }.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = T.bodyB, color = p.ink)
                    Text(if (on) "Done" else "Not done", style = T.small, color = p.muted)
                }
                Toggle(on)
            }
        }
        Text("Forgot yesterday? Tick it here and your streak is kept.", style = T.small, color = p.muted)
        BigButton("Done", kind = "ghost") { finish() }
    }

    /* ---------- Pace: add any amount ---------- */

    @Composable
    private fun ColumnScope.GoalPanel(ref: String?, cid: String?) {
        AppStore.rev
        val p = pal()
        val g = AppStore.data.goals.firstOrNull { it.id == ref } ?: AppStore.data.goals.firstOrNull() ?: run { finish(); return }
        var amt by remember { mutableStateOf("") }
        Header(g.name, "${Fmt.trimNum(g.done)} of ${Fmt.trimNum(g.target)} ${g.unit}")
        Text(amt.ifEmpty { "0" } + " ${g.unit}", style = T.h1.copy(fontSize = 40.sp), color = if (amt.isEmpty()) p.muted else p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Keypad { k -> amt = when (k) { "⌫" -> amt.dropLast(1); "." -> if (amt.contains(".")) amt else amt.ifEmpty { "0" } + "."; else -> (amt + k).take(7) } }
        val v = amt.toDoubleOrNull() ?: 0.0
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Remove", Modifier.weight(1f), kind = "ghost") {
                if (v <= 0) return@BigButton
                commit(cid) { g.done = maxOf(0.0, ((g.done - v) * 100).roundToInt() / 100.0) }; finish()
            }
            BigButton(if (v > 0) "Add ${Fmt.trimNum(v)}" else "Add", Modifier.weight(1f)) {
                if (v <= 0) return@BigButton
                commit(cid) { g.done = minOf(g.target, ((g.done + v) * 100).roundToInt() / 100.0) }; finish()
            }
        }
    }
}
