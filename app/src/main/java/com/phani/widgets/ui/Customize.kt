@file:OptIn(ExperimentalMaterial3Api::class)

package com.phani.widgets.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.CUR
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.WConf
import com.phani.widgets.widget.ACCENTS
import com.phani.widgets.widget.Widgets
import com.phani.widgets.widget.preferredHeight

/** Which sheet is open. Only one at a time, like the prototype. */
object Sheets {
    var custom by mutableStateOf<WConf?>(null)
    var customIndex by mutableStateOf(-1)
    var paywall by mutableStateOf<String?>(null)
    var open by mutableStateOf(false)

    fun customNew(type: String) { custom = Catalog.newConf(type); customIndex = -1 }
    fun customEdit(i: Int) { custom = AppStore.data.placed[i]; customIndex = i }
    fun showPaywall(reason: String = "") { custom = null; paywall = reason }
}

@Composable
fun Sheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = pal()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = p.paper, scrimColor = p.scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 2.dp).size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(p.soft)) },
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = T.h3, color = p.ink, modifier = Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(p.card).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                Icon(Ic.x, "Close", tint = p.ink, modifier = Modifier.size(20.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(18.dp), content = content,
        )
    }
}

@Composable
fun SheetHost() {
    val c = Sheets.custom
    if (c != null) CustomizeSheet(c)
    val pw = Sheets.paywall
    if (pw != null) PaywallSheet(pw)
}

@Composable
private fun FieldLabel(t: String) = Text(t, style = T.smallB, color = pal().muted)

@Composable
private fun CustomizeSheet(start: WConf) {
    val p = pal()
    val ctx = LocalContext.current
    val t = Catalog.type(start.type)
    val editing = Sheets.customIndex >= 0
    var cu by remember(start.id, Sheets.customIndex) { mutableStateOf(start) }
    val locked = Catalog.isPro(cu) && !AppStore.data.pro
    val close = { Sheets.custom = null }
    Sheet(if (editing) "Customise ${t.name}" else t.name, close) {
        val (wbg, _) = wall()
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(wbg).padding(20.dp), contentAlignment = Alignment.Center) {
            WidgetView(cu.copy(id = "pv"), Modifier.widthIn(max = if (cu.size == "s") 168.dp else 340.dp), live = false)
        }
        if (!editing) Text(t.desc, style = T.body, color = p.muted)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Design")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                t.designs.forEach { d ->
                    val on = cu.design == d.id
                    val thumbConf = cu.copy(id = "th" + d.id, design = d.id, size = d.size)
                    Column(
                        Modifier.width(128.dp).clip(RoundedCornerShape(18.dp)).background(p.card)
                            .then(if (on) Modifier.border(2.dp, p.ink, RoundedCornerShape(18.dp)) else Modifier)
                            .clickable {
                                val nc = cu.copy(design = d.id, size = d.size)
                                cu = if (Catalog.refOptions(nc) == null) nc.copy(ref = null) else if (nc.ref == null) nc.copy(ref = Catalog.defaultRef(nc.type)) else nc
                            }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val bw = if (d.size == "s") 168f else 340f
                        val bh = preferredHeight(thumbConf, bw)
                        val k = minOf(104f / bw, 76f / bh)
                        Box(Modifier.size(112.dp, 84.dp).clip(RoundedCornerShape(12.dp)).background(wbg), contentAlignment = Alignment.Center) {
                            WidgetView(thumbConf, Modifier.requiredWidth(bw.dp).graphicsLayer(scaleX = k, scaleY = k), live = false, onTap = {
                                val nc = cu.copy(design = d.id, size = d.size)
                                cu = if (Catalog.refOptions(nc) == null) nc.copy(ref = null) else if (nc.ref == null) nc.copy(ref = Catalog.defaultRef(nc.type)) else nc
                            })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if ((d.pro || t.pro) && !AppStore.data.pro) { Icon(Ic.lock, null, tint = p.ink, modifier = Modifier.size(11.dp)); Spacer(Modifier.width(4.dp)) }
                            Text(d.name, style = T.smallB.copy(fontSize = 12.sp), color = p.ink, maxLines = 1)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Look")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Catalog.styles.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { (k, l, pro) ->
                            val on = cu.style == k
                            Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) p.ink else Color.Transparent).clickable { cu = cu.copy(style = k) },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                if (pro && !AppStore.data.pro) { Icon(Ic.lock, null, tint = if (on) p.paper else p.muted, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
                                Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted)
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Accent")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ACCENTS.forEach { (k, v) -> Swatch(Color(v), cu.accent == k, 40.dp) { cu = cu.copy(accent = k) } }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Typeface")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Catalog.fonts.forEach { (k, l, pro) ->
                    val on = cu.font == k
                    val fam = when (k) { "serif" -> Fonts.serif; "mono" -> Fonts.mono; else -> Fonts.ui }
                    Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) p.ink else Color.Transparent).clickable { cu = cu.copy(font = k) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (pro && !AppStore.data.pro) { Icon(Ic.lock, null, tint = if (on) p.paper else p.muted, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
                        Text(l, style = T.smallB.copy(fontFamily = fam, fontSize = if (k == "serif") 17.sp else 14.sp, fontWeight = if (k == "serif") FontWeight.Normal else FontWeight.SemiBold), color = if (on) p.paper else p.muted)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Corners")
            Seg(Catalog.corners, cu.corner) { cu = cu.copy(corner = it) }
        }

        Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(p.card).clickable { cu = cu.copy(label = !cu.label) }.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Show label", style = T.bodyB, color = p.ink, modifier = Modifier.weight(1f))
            Toggle(cu.label)
        }

        val refs = Catalog.refOptions(cu)
        if (refs != null && refs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("Shows")
                var open by remember { mutableStateOf(false) }
                Box {
                    Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.card).border(1.dp, p.line, RoundedCornerShape(16.dp))
                        .clickable { open = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(refs.firstOrNull { it.first == cu.ref }?.second ?: refs[0].second, style = T.body.copy(fontSize = 16.sp), color = p.ink, modifier = Modifier.weight(1f))
                        Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(p.card)) {
                        refs.forEach { (id, name) -> DropdownMenuItem(text = { Text(name, style = T.body, color = p.ink) }, onClick = { open = false; cu = cu.copy(ref = id) }) }
                    }
                }
            }
        }

        val d = AppStore.data
        BigButton(if (locked) "Unlock Pro to use this" else if (editing) "Save changes" else "Add to Home Screen", icon = if (locked) Ic.lock else null) {
            when {
                locked -> Sheets.showPaywall("This look is part of Pro")
                editing -> {
                    val i = Sheets.customIndex
                    AppStore.change { if (i in placed.indices) placed[i] = cu }
                    close(); Toaster.show("${t.name} updated")
                }
                !d.pro && d.placed.size >= Catalog.FREE_LIMIT -> Sheets.showPaywall("Free includes ${Catalog.FREE_LIMIT} widgets")
                else -> {
                    val conf = cu.copy(id = com.phani.widgets.core.uid())
                    AppStore.change { placed.add(conf) }
                    close(); Nav.tab = "home"; HomeState.edit = false
                    val pinned = Widgets.requestPin(ctx, conf)
                    Toaster.show(if (pinned) "${t.name} added. Confirm to place it on your home screen." else "${t.name} added to Home")
                }
            }
        }
        if (locked) Text("Everything with a lock is part of Pro. Try it here as much as you like.", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        else if (editing) BigButton("Remove", kind = "danger") {
            val i = Sheets.customIndex
            AppStore.change(true) { if (i in placed.indices) placed.removeAt(i) }
            close(); Toaster.show("Widget removed", true)
        }
        else Text("This also places the widget on your phone's home screen.", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun Toggle(on: Boolean) {
    val p = pal()
    Box(Modifier.size(52.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) p.ink else p.soft).padding(3.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(Color.White))
    }
}

@Composable
private fun PaywallSheet(reason: String) {
    val p = pal()
    val d = AppStore.data
    Sheet("Widget App Pro", { Sheets.paywall = null }) {
        if (reason.isNotBlank()) Pill(reason)
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                append("Pay once. ")
                pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = Fonts.serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal))
                append("Keep it forever.")
                pop()
            },
            style = T.h1.copy(fontSize = 30.sp, lineHeight = 32.sp), color = p.ink,
        )
        ListCard {
            Feat(Ic.palette, "Every design and style", "Frost, Smoke, Tint and Clear looks. Serif and Mono type.")
            Feat(Ic.grid, "Unlimited widgets", "Free includes ${Catalog.FREE_LIMIT} on your home screen.")
            Feat(Ic.bolt, "Right now, Pace and Progress", "The widgets that think for you.")
            Feat(Ic.shield, "Still private", "No account, no ads, no tracking. Ever.")
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.ink).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LIFETIME", style = T.label.copy(fontSize = 11.sp), color = p.paper.copy(alpha = 0.7f))
                Text(CUR[d.cur]?.pro ?: "$9.99", style = T.h1.copy(fontSize = 36.sp), color = p.paper)
            }
            Text("One payment\nNo subscription", style = T.small, color = p.paper.copy(alpha = 0.75f), textAlign = TextAlign.End)
        }
        BigButton("Unlock Pro") { AppStore.change { pro = true }; Sheets.paywall = null; Toaster.show("Pro unlocked") }
        Text("Test build: this unlocks Pro without payment. Prices are placeholders.", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
