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
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.Themes
import com.phani.widgets.widget.WTheme
import com.phani.widgets.widget.accentColor
import com.phani.widgets.widget.hexColor

/** The app-wide style: chosen once, used by every widget that follows it. */
object StyleSheet {
    var open by mutableStateOf(false)

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

@Composable
fun StyleSheetHost() {
    if (!StyleSheet.open) return
    AppStore.rev
    val p = pal()
    Sheet("Your style", { StyleSheet.open = false }) {
        Text("Every widget uses this style, in the app and on your home screen. Change it any time.", style = T.small, color = p.muted,
            modifier = Modifier.padding(bottom = 14.dp))
        val cur = AppStore.look
        Themes.all.chunked(2).forEach { row ->
            Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t -> StyleOption(t, cur == t.id, Modifier.weight(1f)) { StyleSheet.pick(t.id) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val own = StyleSheet.ownLook()
        if (own > 0) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(18.dp)).background(p.card).padding(14.dp)) {
                Text(if (own == 1) "1 of your widgets has its own look" else "$own of your widgets have their own look", style = T.smallB, color = p.ink)
                Text("They keep it when you change your style.", style = T.small, color = p.muted, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(p.ink).clickable { StyleSheet.allFollow() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Use my style on all of them", style = T.smallB, color = p.paper)
                }
            }
        }
    }
}

/** One style to pick: a real widget drawn in it, over a backdrop that shows glass and see-through looks. */
@Composable
internal fun StyleOption(t: WTheme, on: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val p = pal()
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(p.card)
        .then(if (on) Modifier.border(2.dp, p.ink, RoundedCornerShape(20.dp)) else Modifier)
        .clickable(onClick = onPick).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(themeBackdrop(t.id)), contentAlignment = Alignment.Center) {
            Thumb(WConf("st" + t.id, "countdown", "big", "s", theme = t.id), 150f, 108f, sample = true, onTap = onPick)
            if (on) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(p.ink), contentAlignment = Alignment.Center) {
                Icon(Ic.check, null, tint = p.paper, modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(t.name, style = T.smallB, color = p.ink, maxLines = 1)
            Text(t.blurb, style = T.small.copy(fontSize = 12.sp), color = p.muted, maxLines = 1)
        }
    }
}
