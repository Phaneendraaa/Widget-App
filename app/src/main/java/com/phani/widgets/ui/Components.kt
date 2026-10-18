package com.phani.widgets.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Pad = 16.dp
val CardR = RoundedCornerShape(24.dp)

@Composable
fun pal() = LocalPal.current

fun Modifier.tap(onClick: () -> Unit) = this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

@Composable
fun TopBar(title: String, sub: String, trailing: (@Composable () -> Unit)? = null) {
    val p = pal()
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.h1, color = p.ink)
            Spacer(Modifier.height(6.dp))
            Text(sub, style = T.small, color = p.muted)
        }
        trailing?.invoke()
    }
}

@Composable
fun ChipButton(text: String, solid: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit) {
    val p = pal()
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (solid) p.ink else p.card)
            .then(if (solid) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(20.dp)))
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (solid) p.paper else p.ink, modifier = Modifier.size(18.dp))
        Text(text, style = T.smallB.copy(fontSize = 14.sp), color = if (solid) p.paper else p.ink)
    }
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, kind: String = "solid", icon: ImageVector? = null, onClick: () -> Unit) {
    val p = pal()
    val bg = if (kind == "solid") p.ink else Color.Transparent
    val fg = when (kind) { "solid" -> p.paper; "danger" -> p.warn; else -> p.ink }
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(bg)
            .then(if (kind == "solid") Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(28.dp)))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = T.bodyB.copy(fontSize = 16.sp), color = fg)
    }
}

/** Segmented control: one pressed option. */
@Composable
fun <K> Seg(options: List<Pair<K, String>>, selected: K, locked: (K) -> Boolean = { false }, onPick: (K) -> Unit) {
    val p = pal()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (k, l) ->
            val on = k == selected
            Row(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (on) p.ink else Color.Transparent).clickable { onPick(k) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (locked(k)) { Icon(Ic.lock, null, tint = if (on) p.paper else p.muted, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
                Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted, maxLines = 1)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text.uppercase(), style = T.label, color = pal().muted, modifier = Modifier.padding(top = 26.dp, bottom = 12.dp))
}

@Composable
fun ListCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(CardR).background(pal().card), content = content)
}

@Composable
fun ListRow(
    title: String,
    detail: String? = null,
    divider: Boolean = true,
    titleColor: Color? = null,
    lead: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val p = pal()
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            lead?.invoke(this)
            Column(Modifier.weight(1f)) {
                Text(title, style = T.bodyB, color = titleColor ?: p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (detail != null) Text(detail, style = T.small, color = p.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            trailing?.invoke(this)
        }
        if (divider) HorizontalDivider(color = p.line, thickness = 1.dp)
    }
}

@Composable
fun Pill(text: String, kind: String = "") {
    val p = pal()
    val (bg, fg) = when (kind) {
        "warn" -> Signal.copy(alpha = 0.16f) to p.warn
        "good" -> p.good.copy(alpha = 0.14f) to p.good
        else -> p.soft to p.ink
    }
    Box(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.smallB.copy(fontSize = 12.sp), color = fg, maxLines = 1)
    }
}

@Composable
fun Feat(icon: ImageVector, title: String, sub: String) {
    val p = pal()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = p.ink, modifier = Modifier.size(22.dp))
        Column { Text(title, style = T.bodyB, color = p.ink); Text(sub, style = T.small, color = p.muted) }
    }
}

@Composable
fun Hero(kicker: String, headline: String, sub: String? = null, onClick: (() -> Unit)? = null) {
    val p = pal()
    Column(
        Modifier.fillMaxWidth().clip(CardR).background(p.ink).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(kicker, style = T.label.copy(fontSize = 11.sp), color = p.paper.copy(alpha = 0.7f))
        Text(headline, style = T.h3.copy(fontSize = 22.sp, lineHeight = 26.sp), color = p.paper)
        if (sub != null) Text(sub, style = T.small.copy(fontSize = 14.sp), color = p.paper.copy(alpha = 0.75f))
    }
}

@Composable
fun SampleBanner(text: String, action: String, onClick: () -> Unit) {
    val p = pal()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.soft).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = T.small, color = p.ink, modifier = Modifier.weight(1f))
        Text(action, style = T.smallB.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = p.ink,
            modifier = Modifier.clickable(onClick = onClick).padding(8.dp))
    }
}

/* ---------- toast with undo ---------- */

object Toaster {
    var msg by mutableStateOf<String?>(null)
    var undo by mutableStateOf(false)
    var stamp by mutableStateOf(0L)
    fun show(m: String, canUndo: Boolean = false) { msg = m; undo = canUndo; stamp = System.currentTimeMillis() }
}

@Composable
fun ToastHost(modifier: Modifier, onUndo: () -> Unit) {
    val p = pal()
    val m = Toaster.msg
    androidx.compose.runtime.LaunchedEffect(Toaster.stamp) {
        if (m != null) { kotlinx.coroutines.delay(3200); Toaster.msg = null }
    }
    AnimatedVisibility(visible = m != null, modifier = modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.ink).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp).heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(m ?: "", style = T.small.copy(fontSize = 14.sp), color = p.paper, modifier = Modifier.weight(1f))
            if (Toaster.undo) Text("Undo", style = T.smallB.copy(fontSize = 14.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), color = p.paper,
                modifier = Modifier.clickable { Toaster.msg = null; onUndo() }.padding(10.dp))
        }
    }
}

@Composable
fun EmptyCard(title: String, body: String) {
    val p = pal()
    Column(Modifier.fillMaxWidth().clip(CardR).background(p.card).padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = T.bodyB.copy(fontSize = 16.sp), color = p.ink)
        Spacer(Modifier.height(4.dp))
        Text(body, style = T.small, color = p.muted)
    }
}

@Composable
fun Swatch(color: Color, selected: Boolean, size: Dp = 36.dp, onClick: () -> Unit) {
    val p = pal()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 2))
            .then(if (selected) Modifier.border(BorderStroke(2.dp, p.ink), RoundedCornerShape(size / 2)) else Modifier)
            .padding(if (selected) 4.dp else 0.dp).clip(RoundedCornerShape(size / 2)).background(color).clickable(onClick = onClick),
    )
}

val NoPad = PaddingValues(0.dp)
