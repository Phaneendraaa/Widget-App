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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.Themes
import com.phani.widgets.widget.WTheme
import com.phani.widgets.widget.accentColor
import com.phani.widgets.widget.hexColor

/** The app-wide style: chosen once, used by every widget that follows it. */
object StyleSheet {
    var open by mutableStateOf(false)
    /** True on first launch: the picker is the first thing people choose. */
    var first by mutableStateOf(false)

    fun current(): WTheme = Themes.get(AppStore.look) ?: Themes.all[0]

    /** Widgets that have a look of their own instead of following the app style. */
    fun ownLook(): Int = AppStore.data.placed.count { it.theme != Themes.APP }

    fun pick(id: String) = AppStore.change { look = id }

    /** Make every saved widget (and the ones on the home screen) follow the app style. */
    fun allFollow() = AppStore.change {
        placed.replaceAll { it.copy(theme = Themes.APP) }
        bound.keys.toList().forEach { k -> bound[k]?.let { bound[k] = it.copy(theme = Themes.APP) } }
    }
}

/** A small colour chip for a theme: its background, with its accent as a dot. */
@Composable
internal fun Swatch(t: WTheme, size: Dp) {
    val p = pal()
    val shape = RoundedCornerShape(size * 0.3f)
    val bg = when (t.style) {
        "ink" -> Color(0xFF151514)
        "you" -> p.soft
        "clear" -> Color.Transparent
        else -> t.bg?.let { hexColor(it) }?.let { Color(it) } ?: Color.White
    }.let { if (t.alpha < 100) it.copy(alpha = 0.55f + 0.45f * t.alpha / 100f) else it }
    val bg2 = t.bg2?.let { hexColor(it) }?.let { Color(it) } ?: bg
    val acc = if (t.accent == "signal") Color(accentColor("signal")) else Color(accentColor(t.accent))
    Box(Modifier.size(size).clip(shape).background(themeBackdrop(t.id)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(bg, bg2))).border(1.dp, p.soft, shape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * 0.3f).clip(CircleShape).background(acc))
        }
    }
}

/** The "Your style" row at the top of the Widgets page. */
@Composable
fun StyleCard() {
    AppStore.rev
    val p = pal()
    val t = StyleSheet.current()
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(18.dp)).background(p.card).clickable { StyleSheet.open = true }
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Swatch(t, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Your style", style = T.small.copy(fontSize = 12.sp), color = p.muted)
            Text(t.name, style = T.smallB.copy(fontSize = 15.sp), color = p.ink, maxLines = 1)
        }
        Text("Change", style = T.smallB, color = p.ink)
        Icon(Ic.chev, null, tint = p.ink, modifier = Modifier.size(18.dp))
    }
}

/* ---------------- Choose your style ---------------- */

/** A home screen made of our widgets, to show what a style looks like on a real phone. */
private val HOME = listOf(
    listOf(WConf("pv1", "spend", "metal", "m")),
    listOf(WConf("pv2", "streak", "flame", "s"), WConf("pv3", "counter", "liquid", "s")),
    listOf(WConf("pv4", "countdown", "led", "w")),
    listOf(WConf("pv5", "clock", "word", "s"), WConf("pv6", "today", "tear", "s")),
)

/** A wallpaper that suits each style, so it's shown the way people would really use it. Second value: light text on it. */
private fun phoneWall(id: String): Pair<List<Color>, Boolean> = when (id) {
    "midnight" -> listOf(Color(0xFF1B2440), Color(0xFF3B4C7A)) to true
    "glass" -> listOf(Color(0xFF2B5876), Color(0xFFD38A5C), Color(0xFFF2C38B)) to true
    "glassdark" -> listOf(Color(0xFF4B3B6B), Color(0xFFC06C84), Color(0xFFF8B195)) to true
    "modern" -> listOf(Color(0xFFEDE8F7), Color(0xFFD6CCF2)) to false
    "ocean" -> listOf(Color(0xFFD5ECF2), Color(0xFF93C9D8)) to false
    "sunset" -> listOf(Color(0xFFFFE6CF), Color(0xFFF3B39E)) to false
    "retro" -> listOf(Color(0xFFCDBB9F), Color(0xFFA08766)) to false
    "newspaper" -> listOf(Color(0xFFE6E1D6), Color(0xFFCBC4B6)) to false
    "neon" -> listOf(Color(0xFF07070F), Color(0xFF1C1236)) to true
    "terminal" -> listOf(Color(0xFF040704), Color(0xFF0E1A10)) to true
    "pastel" -> listOf(Color(0xFFFBE2EC), Color(0xFFDFE3FB)) to false
    "ink" -> listOf(Color(0xFFE9E5DD), Color(0xFFCFC9BE)) to false
    "paper" -> listOf(Color(0xFF8EA3B5), Color(0xFF5E7487)) to true
    else -> if (id.startsWith("drop_")) listOf(Color(0xFF1C1B22), Color(0xFF3B3946)) to true else { val (bg, fg) = wall(); listOf(bg, bg) to (fg.red + fg.green + fg.blue > 1.5f) }
}

@Composable
private fun PhonePreview(t: WTheme, w: Dp) {
    val vw = 360f; val vh = 760f
    val k = w.value / vw
    val (cols, light) = phoneWall(t.id)
    val on = if (light) Color.White else Color(0xFF1C1C1A)
    val outer = LocalDensity.current
    Box(Modifier.size(w, (vh * k).dp).clip(RoundedCornerShape((46 * k).dp)).background(Color(0xFF0B0B0C)).padding((5 * k).dp)) {
        // lay the phone out at real size, drawn at a smaller density so every widget stays sharp
        CompositionLocalProvider(LocalDensity provides Density(outer.density * k, outer.fontScale)) {
            Column(Modifier.size((vw - 10f).dp, (vh - 10f).dp).clip(RoundedCornerShape(41.dp)).background(Brush.linearGradient(cols))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("9:41", style = T.smallB.copy(fontSize = 14.sp), color = on, modifier = Modifier.weight(1f))
                    repeat(3) { Box(Modifier.padding(start = 5.dp).size(if (it == 2) 22.dp else 12.dp, 10.dp).clip(RoundedCornerShape(3.dp)).background(on.copy(alpha = 0.8f))) }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HOME.forEach { row ->
                        if (row.size == 1) WidgetView(row[0].copy(theme = t.id), Modifier.fillMaxWidth(), live = false, sample = true)
                        else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { c -> Box(Modifier.weight(1f)) { WidgetView(c.copy(theme = t.id), Modifier.fillMaxWidth(), live = false, sample = true) } }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().padding(horizontal = 34.dp).padding(bottom = 26.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) { Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(on.copy(alpha = 0.22f))) }
                }
            }
        }
    }
}

/** Swipe through full home screens, one per style, and pick one. Shown on first launch and from "Your style". */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StylePicker() {
    AppStore.rev
    val p = pal()
    val list = Themes.offered
    val close = { StyleSheet.open = false; StyleSheet.first = false }
    BackHandler { close() }
    val start = list.indexOfFirst { it.id == AppStore.look }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { list.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(p.paper).pointerInput(Unit) { detectTapGestures { } }.windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!StyleSheet.first) Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable { close() }, contentAlignment = Alignment.Center) {
                Icon(Ic.back, "Back", tint = p.ink, modifier = Modifier.size(22.dp))
            } else Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text("Choose your style", style = T.h3, color = p.ink)
                Text("Every widget will match. Swipe to see them all.", style = T.small, color = p.muted)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 14.dp)) {
            val pw = minOf(maxWidth * 0.66f, maxHeight * (360f / 760f))
            HorizontalPager(state = pager, contentPadding = PaddingValues(horizontal = (maxWidth - pw) / 2), pageSpacing = 14.dp,
                modifier = Modifier.fillMaxSize()) { i ->
                val off = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                Box(Modifier.fillMaxSize().graphicsLayer(scaleX = 1f - 0.08f * off, scaleY = 1f - 0.08f * off, alpha = 1f - 0.35f * off)
                    .clickable { scope.launch { pager.animateScrollToPage(i) } }, contentAlignment = Alignment.Center) {
                    PhonePreview(list[i], pw)
                }
            }
        }
        val t = list[pager.currentPage]
        val using = t.id == AppStore.look
        Column(Modifier.fillMaxWidth().padding(horizontal = Pad).padding(bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(t, 26.dp)
                Spacer(Modifier.width(10.dp))
                Text(t.name, style = T.bodyB.copy(fontSize = 18.sp), color = p.ink)
                if (t.drop) Box(Modifier.padding(start = 8.dp).clip(RoundedCornerShape(10.dp)).background(p.ink).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("New", style = T.smallB.copy(fontSize = 11.sp), color = p.paper)
                }
                Spacer(Modifier.weight(1f))
                Text("${pager.currentPage + 1} of ${list.size}", style = T.small, color = p.muted)
            }
            Text(t.blurb, style = T.small, color = p.muted, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            BigButton(if (using) "Keep ${t.name}" else "Use ${t.name}") {
                if (!using) { StyleSheet.pick(t.id); Toaster.show("Your widgets now use ${t.name}") }
                close()
            }
            val own = StyleSheet.ownLook()
            if (own > 0) Text(
                (if (own == 1) "1 widget has" else "$own widgets have") + " its own look. Use ${t.name} on all of them",
                style = T.small, color = p.muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { StyleSheet.pick(t.id); StyleSheet.allFollow(); Toaster.show("All your widgets now use ${t.name}"); close() }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
        }
    }
}
