package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.WConf

private fun mk(type: String, design: String, style: String, accent: String, font: String = "grotesk") =
    WConf("ob$type$design", type, design, Catalog.design(type, design).size, style, accent, font, ref = Catalog.defaultRef(type))

@Composable
private fun Headline(plain: String, italic: String) {
    val p = pal()
    Text(
        buildAnnotatedString {
            append(plain)
            withStyle(SpanStyle(fontFamily = Fonts.serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp)) { append(italic) }
        },
        style = T.h1.copy(fontSize = 40.sp, lineHeight = 41.sp, letterSpacing = (-1.6).sp), color = p.ink,
    )
}

@Composable
private fun Sub(t: String) = Text(t, style = T.body.copy(fontSize = 16.sp, lineHeight = 23.sp), color = pal().muted, modifier = Modifier.padding(top = 12.dp))

@Composable
private fun Pair2(a: WConf, b: WConf) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)) { WidgetView(a, live = false) }
        Box(Modifier.weight(1f)) { WidgetView(b, live = false) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Onboarding() {
    val p = pal()
    var step by remember { mutableIntStateOf(0) }
    var picks by remember { mutableStateOf(setOf("money", "habits")) }
    val finish = {
        val first = listOf("money" to "money", "subs" to "subs", "habits" to "habits", "health" to "fast", "time" to "dates", "work" to "sched", "student" to "classes")
            .firstOrNull { it.first in picks }?.second
        if (first != null) TrackState.tab = first
        AppStore.change { onboarded = true }
    }
    Column(
        Modifier.fillMaxSize().background(p.paper).windowInsetsPadding(WindowInsets.systemBars).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.End) {
            if (step < 2) Text("Skip", style = T.bodyB, color = p.ink, modifier = Modifier.clickable { AppStore.change { onboarded = true } }.padding(10.dp))
        }
        when (step) {
            0 -> {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pair2(mk("spend", "ring", "ink", "mint"), mk("fast", "ring", "paper", "signal"))
                    Pair2(mk("streak", "big", "paper", "signal", "serif"), mk("subs", "next", "ink", "sun", "mono"))
                }
                Spacer(Modifier.height(16.dp))
                Headline("Widgets that ", "answer.")
                Sub("Not just a clock and a date. What you can spend today, when the next charge hits, whether your streak is safe.")
            }
            1 -> {
                Spacer(Modifier.height(40.dp))
                Headline("What do you want at a ", "glance?")
                Sub("Pick any. You can add the rest later.")
                FlowRow(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("money" to "Spending", "subs" to "Subscriptions", "habits" to "Habits", "health" to "Fasting & water",
                        "time" to "Countdowns", "work" to "My schedule", "student" to "Class attendance").forEach { (k, l) ->
                        val on = k in picks
                        Box(Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(if (on) p.ink else p.card)
                            .then(if (on) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(24.dp)))
                            .clickable { picks = if (on) picks - k else picks + k }.padding(horizontal = 18.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(l, style = T.bodyB, color = if (on) p.paper else p.ink)
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
            else -> {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WidgetView(mk("now", "card", "ink", "signal"), live = false)
                    Pair2(mk("countdown", "big", "paper", "cobalt", "serif"), mk("counter", "glass", "paper", "cobalt"))
                }
                Spacer(Modifier.height(16.dp))
                Headline("Make every one ", "yours.")
                Sub("${Catalog.totalDesigns} designs. Any colour, any corner, your own title. Everything included, no in-app purchases.")
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, androidx.compose.ui.Alignment.CenterHorizontally)) {
            (0..2).forEach { i -> Box(Modifier.height(6.dp).width(if (i == step) 20.dp else 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i == step) p.ink else p.soft)) }
        }
        if (step < 2) BigButton("Continue") { step++ }
        else {
            BigButton("Start with my own info") {
                AppStore.change {
                    subs.clear(); habits.clear(); goals.clear(); dates.clear(); subjects.clear(); counters.clear(); placed.clear(); sched.clear(); log.clear()
                    fast.start = null; fast.history.clear(); sampleData = false
                }
                finish(); Nav.tab = "home"
            }
            Spacer(Modifier.height(10.dp))
            BigButton("Look around with examples first", kind = "ghost") { finish() }
        }
    }
}

@Composable
fun HelpSheet(close: () -> Unit) {
    val p = pal()
    Sheet("Home screen widgets", close) {
        Text("Add one", style = T.bodyB, color = p.ink)
        ListCard {
            ListRow("1. Long-press an empty spot on your home screen")
            ListRow("2. Tap Widgets and find Widget App")
            ListRow("3. Pick Small, Wide or Large and drag it out", divider = false)
        }
        Text("Or tap Edit on Home, then + on any widget.", style = T.small, color = p.muted)
        Text("Keep them up to date", style = T.bodyB, color = p.ink, modifier = Modifier.padding(top = 6.dp))
        Text("Taps update widgets instantly. Clocks and timers refresh about every 15 minutes. Some phones pause apps to save battery; if a widget looks behind, allow Widget App to run in the background:", style = T.small, color = p.muted)
        ListCard {
            ListRow("vivo", "Settings › Battery › Background power use › Widget App › Allow")
            ListRow("OPPO / realme", "App info › Battery › Allow background activity and Auto launch")
            ListRow("Xiaomi / POCO", "App info › Autostart on, Battery saver › No restrictions")
            ListRow("Samsung", "Settings › Battery › Background usage limits › add to Never sleeping apps")
            ListRow("Pixel and others", "App info › Battery › Unrestricted", divider = false)
        }
    }
}
