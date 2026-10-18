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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.CUR
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.sampleData
import com.phani.widgets.core.today
import com.phani.widgets.core.toJson

val WALLS = linkedMapOf(
    "stone" to (Color(0xFFD7D3CA) to Color(0xFF1C1C1A)),
    "night" to (Color(0xFF16171C) to Color(0xFFF3F1EB)),
    "sage" to (Color(0xFFB5C0AE) to Color(0xFF1C1C1A)),
    "dusk" to (Color(0xFF3A3F55) to Color(0xFFF3F1EB)),
    "peach" to (Color(0xFFEBC8B4) to Color(0xFF1C1C1A)),
    "sky" to (Color(0xFFAFC6DA) to Color(0xFF1C1C1A)),
)

fun wall() = WALLS[AppStore.data.wall] ?: WALLS.getValue("stone")

@Composable
fun SettingsScreen() {
    AppStore.rev
    val p = pal()
    val d = AppStore.data
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable { Nav.screen = "home" }, contentAlignment = Alignment.Center) {
            Icon(Ic.back, "Back", tint = p.ink, modifier = Modifier.size(22.dp))
        }
    }
    TopBar("Settings", "Works offline. Nothing leaves your phone.")

    SectionTitle("App appearance")
    Seg(listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"), d.theme) { AppStore.change { theme = it } }

    SectionTitle("Reminders")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { RemindersSection() }

    SectionTitle("Region")
    CurrencyPicker()
    Spacer(Modifier.height(12.dp))
    Text("Time", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
    Seg(listOf(false to "12-hour", true to "24-hour"), d.h24) { AppStore.change { h24 = it } }
    Spacer(Modifier.height(12.dp))
    Text("Dates", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
    Seg(listOf(false to "Oct 25", true to "25 Oct"), d.dmy) { AppStore.change { dmy = it } }

    SectionTitle("Privacy")
    ListCard {
        Feat(Ic.wifi, "No internet needed", "Every widget is calculated on your phone.")
        Feat(Ic.shield, "No account, no ads, no tracking", "Your data stays on this device and in your own phone backup.")
        Feat(Ic.bolt, "Easy on battery", "Widgets change when you tap them, or when the day or your schedule changes.")
    }

    SectionTitle("Home screen widgets")
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val onPhone = remember(AppStore.rev) { com.phani.widgets.widget.Widgets.placedCount(ctx) }
    ListCard {
        ListRow("On your home screen", if (onPhone == 0) "None yet" else "${onPhone} widget" + (if (onPhone == 1) "" else "s") + ", updated as you tap",
            trailing = { Text("$onPhone", style = T.bodyB, color = p.ink) })
        ListRow("How to add and keep them updated", divider = false, trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) { Sheets.help = true }
    }

    SectionTitle("Backup")
    BackupSection()

    SectionTitle("Data")
    var confirm by remember { mutableStateOf(false) }
    ListCard {
        ListRow(if (confirm) "Tap again to delete everything" else "Delete all my data", titleColor = p.warn, divider = false,
            trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) {
            if (!confirm) { confirm = true; return@ListRow }
            confirm = false
            AppStore.change(undoable = true) {
                subs.clear(); habits.clear(); goals.clear(); dates.clear(); subjects.clear(); clocks.clear(); counters.clear()
                placed.clear(); sched.clear(); log.clear(); sampleData = false
                fast.start = null; fast.history.clear()
            }
            Toaster.show("All data deleted", canUndo = true)
        }
    }
    Text("Widget App · 1.1", style = T.small, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 22.dp))
}

@Composable
private fun CurrencyPicker() {
    val p = pal()
    val d = AppStore.data
    var open by remember { mutableStateOf(false) }
    Text("Currency", style = T.smallB, color = p.muted, modifier = Modifier.padding(bottom = 8.dp))
    Box {
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(p.card)
                .border(1.dp, p.line, RoundedCornerShape(16.dp)).clickable { open = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${CUR[d.cur]?.name ?: d.cur} (${d.cur})", style = T.body.copy(fontSize = 16.sp), color = p.ink, modifier = Modifier.weight(1f))
            Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(p.card)) {
            CUR.forEach { (k, v) ->
                DropdownMenuItem(text = { Text("${v.name} ($k)", style = T.body, color = p.ink) }, onClick = {
                    open = false
                    AppStore.change { cur = k }
                    Toaster.show("Currency set to $k")
                })
            }
        }
    }
}


/** Save everything to a file you keep (Drive, Downloads…), and bring it back on a new phone. */
@Composable
private fun BackupSection() {
    val p = pal()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val save = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(AppStore.data.toJson().toString(2).toByteArray()) }
            Toaster.show("Backup saved")
        } catch (e: Exception) { Toaster.show("Couldn't save the backup") }
    }
    val open = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val txt = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@rememberLauncherForActivityResult
            val nd = com.phani.widgets.core.appDataFrom(org.json.JSONObject(txt))
            // widgets already on this phone keep their place; the backup's ids belonged to the old phone
            nd.bound.clear(); nd.bound.putAll(AppStore.data.bound)
            nd.onboarded = true
            AppStore.change(true) { }
            AppStore.replace(nd)
            Toaster.show("Restored ${nd.placed.size} widgets and all your info", true)
        } catch (e: Exception) { Toaster.show("That file isn't a Widget App backup") }
    }
    ListCard {
        ListRow("Save a backup", "A file with all your widgets and info. Keep it in Drive or Downloads.",
            trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) {
            save.launch("widget-app-backup-${com.phani.widgets.core.today()}.json")
        }
        ListRow("Restore from a backup", "On a new phone, pick your backup file.", divider = false,
            trailing = { Icon(Ic.chev, null, tint = p.muted, modifier = Modifier.size(18.dp)) }) {
            open.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
        }
    }
    Text("Your phone's own Google backup also keeps the app's data when it's switched on.", style = T.small, color = p.muted, modifier = Modifier.padding(top = 8.dp))
}
