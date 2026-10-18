package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp

internal fun hex(c: Int) = String.format("#%06X", c and 0xFFFFFF)

internal val ACCENT_PALETTE = listOf(
    "#FF5A1F", "#E5484D", "#F2557A", "#D6409F", "#8E6CFF", "#6E56CF", "#3B5BFF", "#0091FF",
    "#0EA5A4", "#12A26B", "#30A46C", "#9BBF2A", "#F0B400", "#FF8A00", "#8D6E63", "#868E96", "#121211", "#FFFFFF",
)
internal val BG_PALETTE = listOf(
    "#FFFFFF", "#F6F4EE", "#EEECE6", "#E8E1D3", "#D9D4C7", "#FFE8D6", "#FDE2E4", "#EAE4F7", "#DDE7F7", "#E2ECE9", "#FFF3BF",
    "#121211", "#1F2328", "#2B2D42", "#1E3A5F", "#0B3D2E", "#3A1F4F", "#5C1A1A", "#FF5A1F", "#3B5BFF", "#12A26B",
)
internal val TEXT_PALETTE = listOf("#121211", "#3D3A35", "#5D5952", "#FFFFFF", "#F3F1EB", "#FF5A1F", "#3B5BFF", "#12A26B", "#F0B400", "#8E6CFF")

/** A row of colour swatches, an optional Auto chip, and a Custom mixer (hue, saturation, brightness). */
@Composable
internal fun ColorField(label: String, value: String?, palette: List<String>, auto: String?, onChange: (String?) -> Unit) {
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
                Chip(auto, on) { onChange(null); custom = false }
            }
            palette.forEach { h ->
                val c = Color(android.graphics.Color.parseColor(h))
                Box(Modifier.border(1.dp, p.line, RoundedCornerShape(20.dp))) { Swatch(c, cur == h, 40.dp) { onChange(h); custom = false } }
            }
            val mine = cur != null && cur !in palette
            Chip("Custom", custom || mine) { custom = !custom }
        }
        if (custom) {
            val base = com.phani.widgets.widget.hexColor(cur) ?: 0xFFFF5A1F.toInt()
            val hsv = remember(label) { FloatArray(3).also { android.graphics.Color.colorToHSV(base, it) } }
            var h by remember(label) { mutableStateOf(hsv[0]) }
            var sat by remember(label) { mutableStateOf(hsv[1]) }
            var v by remember(label) { mutableStateOf(hsv[2]) }
            val mixed = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v))
            val emit = { onChange(hex(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)))) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Color(mixed)).border(1.dp, p.line, RoundedCornerShape(18.dp)))
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
internal fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val p = pal()
    Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (on) p.ink else p.card)
        .then(if (on) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(20.dp)))
        .clickable(onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.smallB, color = if (on) p.paper else p.ink)
    }
}

@Composable
private fun sliderColors() = pal().let { SliderDefaults.colors(thumbColor = it.ink, activeTrackColor = it.ink, inactiveTrackColor = it.soft) }

@Composable
internal fun MiniSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val p = pal()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.small, color = p.muted, modifier = Modifier.width(84.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f), colors = sliderColors())
    }
}

@Composable
internal fun SliderField(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit) {
    val p = pal()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label)
            Spacer(Modifier.weight(1f))
            Text(shown, style = T.smallB, color = p.ink)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, colors = sliderColors())
    }
}

@Composable
internal fun ToggleRow(label: String, on: Boolean, sub: String? = null, onClick: () -> Unit) {
    val p = pal()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).clickable(onClick = onClick).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = T.bodyB, color = p.ink)
            if (sub != null) Text(sub, style = T.small, color = p.muted)
        }
        Toggle(on)
    }
}
