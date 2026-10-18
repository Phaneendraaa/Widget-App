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
    var help by mutableStateOf(false)
    var open by mutableStateOf(false)

    fun customNew(type: String) { custom = Catalog.newConf(type); customIndex = -1 }
    fun customEdit(i: Int) { custom = AppStore.data.placed[i]; customIndex = i }
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
    TrackSheetHost()
    val a = Flow.actions
    if (a != null) WidgetActionsSheet(a)
    if (Sheets.help) HelpSheet { Sheets.help = false }
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
    val close = { Sheets.custom = null }
    Sheet(if (editing) "Customise ${t.name}" else t.name, close) {
        val (wbg, _) = wall()
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(wbg).padding(20.dp), contentAlignment = Alignment.Center) {
            WidgetView(cu.copy(id = "pv"), Modifier.widthIn(max = if (cu.size == "s") 168.dp else 340.dp), live = false)
        }
        if (!editing) Text(t.desc, style = T.body, color = p.muted)
        DataCard(cu)

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
                        row.forEach { (k, l, _) ->
                            val on = cu.style == k && cu.bg == null
                            Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) p.ink else Color.Transparent)
                                .clickable { cu = cu.copy(style = k, bg = null, fg = null, alpha = 100) },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text(l, style = T.smallB.copy(fontSize = 14.sp), color = if (on) p.paper else p.muted)
                            }
                        }
                    }
                }
            }
            Text("A starting point. Fine-tune every colour below.", style = T.small, color = p.muted)
        }

        ColorField("Accent colour", cu.accent.let { ACCENTS[it]?.let { v -> hex(v) } ?: it }, ACCENT_PALETTE, auto = null) { cu = cu.copy(accent = it ?: "signal") }
        ColorField("Background", cu.bg, BG_PALETTE, auto = "From look") { cu = cu.copy(bg = it) }
        ColorField("Text colour", cu.fg, TEXT_PALETTE, auto = "Auto") { cu = cu.copy(fg = it) }

        SliderField("Background opacity", cu.alpha.toFloat(), 0f..100f, "${cu.alpha}%") { cu = cu.copy(alpha = it.toInt()) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Typeface")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Catalog.fonts.forEach { (k, l, _) ->
                    val on = cu.font == k
                    val fam = when (k) { "serif" -> Fonts.serif; "mono" -> Fonts.mono; else -> Fonts.ui }
                    Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) p.ink else Color.Transparent).clickable { cu = cu.copy(font = k) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(l, style = T.smallB.copy(fontFamily = fam, fontSize = if (k == "serif") 17.sp else 14.sp, fontWeight = if (k == "serif") FontWeight.Normal else FontWeight.SemiBold), color = if (on) p.paper else p.muted)
                    }
                }
            }
        }

        val rad = if (cu.radius >= 0) cu.radius else (com.phani.widgets.widget.CORNERS[cu.corner] ?: 26f).toInt()
        SliderField("Corner roundness", rad.toFloat(), 0f..40f, if (rad == 0) "Square" else "$rad") { cu = cu.copy(radius = it.toInt()) }

        Field("Title", cu.title ?: "", { cu = cu.copy(title = it.take(32).ifEmpty { null }) }, placeholder = "Default title")

        ToggleRow("Show title", cu.label) { cu = cu.copy(label = !cu.label) }
        ToggleRow("Outline", cu.outline) { cu = cu.copy(outline = !cu.outline) }

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

        Text("Reset look to default", style = T.bodyB, color = p.ink, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                cu = cu.copy(style = "paper", accent = "signal", font = "grotesk", corner = "round", label = true, bg = null, fg = null, alpha = 100, radius = -1, outline = false, title = null)
            }.padding(12.dp))

        BigButton(if (editing) "Save changes" else "Add widget") {
            if (editing) {
                val i = Sheets.customIndex
                AppStore.change { if (i in placed.indices) placed[i] = cu }
                close(); Toaster.show("${t.name} updated")
            } else {
                val conf = cu.copy(id = com.phani.widgets.core.uid())
                AppStore.change { placed.add(conf) }
                close(); Nav.tab = "home"
                val pinned = Widgets.requestPin(ctx, conf)
                if (pinned) Toaster.show("${t.name} added. Confirm on the next screen to put it on your phone.") else { Toaster.show("${t.name} added to My widgets"); Sheets.help = true }
            }
        }
        if (editing) BigButton("Remove", kind = "danger") {
            val i = Sheets.customIndex
            AppStore.change(true) { if (i in placed.indices) placed.removeAt(i) }
            close(); Toaster.show("Widget removed", true)
        }
        else Text("Next, your phone asks where to place it on your home screen.", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun hex(c: Int) = String.format("#%06X", c and 0xFFFFFF)

private val ACCENT_PALETTE = listOf(
    "#FF5A1F", "#E5484D", "#F2557A", "#D6409F", "#8E6CFF", "#6E56CF", "#3B5BFF", "#0091FF",
    "#0EA5A4", "#12A26B", "#30A46C", "#9BBF2A", "#F0B400", "#FF8A00", "#8D6E63", "#868E96", "#121211", "#FFFFFF",
)
private val BG_PALETTE = listOf(
    "#FFFFFF", "#F6F4EE", "#EEECE6", "#E8E1D3", "#D9D4C7", "#FFE8D6", "#FDE2E4", "#EAE4F7", "#DDE7F7", "#E2ECE9", "#FFF3BF",
    "#121211", "#1F2328", "#2B2D42", "#1E3A5F", "#0B3D2E", "#3A1F4F", "#5C1A1A", "#FF5A1F", "#3B5BFF", "#12A26B",
)
private val TEXT_PALETTE = listOf("#121211", "#3D3A35", "#5D5952", "#FFFFFF", "#F3F1EB", "#FF5A1F", "#3B5BFF", "#12A26B", "#F0B400", "#8E6CFF")

/** A row of colour swatches, an optional Auto chip, and a Custom picker with hue, saturation and brightness. */
@Composable
private fun ColorField(label: String, value: String?, palette: List<String>, auto: String?, onChange: (String?) -> Unit) {
    val p = pal()
    var custom by remember { mutableStateOf(false) }
    val cur = value?.uppercase()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label)
            Spacer(Modifier.weight(1f))
            Text(cur ?: (auto ?: ""), style = T.small, color = p.muted)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (auto != null) {
                val on = cur == null
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (on) p.ink else p.card)
                    .then(if (on) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(20.dp)))
                    .clickable { onChange(null); custom = false }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(auto, style = T.smallB, color = if (on) p.paper else p.ink)
                }
            }
            palette.forEach { h ->
                val c = Color(android.graphics.Color.parseColor(h))
                Box(Modifier.border(1.dp, p.line, RoundedCornerShape(20.dp))) { Swatch(c, cur == h, 40.dp) { onChange(h); custom = false } }
            }
            val inPalette = cur != null && cur in palette
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (custom || (cur != null && !inPalette)) p.ink else p.card)
                .then(if (custom) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(20.dp)))
                .clickable { custom = !custom }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("Custom", style = T.smallB, color = if (custom || (cur != null && !inPalette)) p.paper else p.ink)
            }
        }
        if (custom) {
            val base = com.phani.widgets.widget.hexColor(cur) ?: 0xFFFF5A1F.toInt()
            val hsv = remember(label) { FloatArray(3).also { android.graphics.Color.colorToHSV(base, it) } }
            var h by remember(label) { mutableStateOf(hsv[0]) }
            var sat by remember(label) { mutableStateOf(hsv[1]) }
            var v by remember(label) { mutableStateOf(hsv[2]) }
            val emit = { onChange(hex(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)))) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)))).border(1.dp, p.line, RoundedCornerShape(18.dp)))
                    Spacer(Modifier.width(12.dp))
                    Text("Drag to mix your own colour", style = T.small, color = p.muted)
                }
                MiniSlider("Hue", h, 0f..360f) { h = it; emit() }
                MiniSlider("Saturation", sat, 0f..1f) { sat = it; emit() }
                MiniSlider("Brightness", v, 0f..1f) { v = it; emit() }
            }
        }
    }
}

@Composable
private fun MiniSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val p = pal()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.small, color = p.muted, modifier = Modifier.width(84.dp))
        androidx.compose.material3.Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = p.ink, activeTrackColor = p.ink, inactiveTrackColor = p.soft))
    }
}

@Composable
private fun SliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit) {
    val p = pal()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label)
            Spacer(Modifier.weight(1f))
            Text(shown, style = T.smallB, color = p.ink)
        }
        androidx.compose.material3.Slider(value = value, onValueChange = onChange, valueRange = range,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = p.ink, activeTrackColor = p.ink, inactiveTrackColor = p.soft))
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onClick: () -> Unit) {
    val p = pal()
    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(p.card).clickable(onClick = onClick).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.bodyB, color = p.ink, modifier = Modifier.weight(1f))
        Toggle(on)
    }
}

@Composable
fun Toggle(on: Boolean) {
    val p = pal()
    Box(Modifier.size(52.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) p.ink else p.soft).padding(3.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(Color.White))
    }
}
