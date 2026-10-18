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
    WConf("ob$type$design", type, design, Catalog.design(type, design).size, style, accent, font)

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
        Box(Modifier.weight(1f)) { WidgetView(a, live = false, sample = true) }
        Box(Modifier.weight(1f)) { WidgetView(b, live = false, sample = true) }
    }
}

@Composable
fun Onboarding() {
    val p = pal()
    Column(
        Modifier.fillMaxSize().background(p.paper).windowInsetsPadding(WindowInsets.systemBars).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Pair2(mk("countdown", "big", "paper", "cobalt", "serif"), mk("streak", "big", "ink", "signal"))
            WidgetView(mk("spend", "strip", "paper", "mint"), live = false, sample = true)
        }
        Spacer(Modifier.height(28.dp))
        Headline("Widgets that ", "answer.")
        Sub("Days until your trip. Whether your streak is safe. What you can spend today. Right on your home screen.")
        Spacer(Modifier.height(32.dp))
        BigButton("Get started") { AppStore.change { onboarded = true } }
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
            ListRow("3. Pick Small, Wide or Large and drag it out")
            ListRow("4. Pick one of your widgets from the list", divider = false)
        }
        Text("Or make a widget in the app and tap Add to home screen.", style = T.small, color = p.muted)
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
