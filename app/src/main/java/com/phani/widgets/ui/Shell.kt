package com.phani.widgets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.phani.widgets.core.AppStore

/** Navigation state shared by screens. */
object Nav {
    var tab by mutableStateOf("home")
}

@Composable
fun AppShell() {
    val p = pal()
    AppStore.rev
    Box(Modifier.fillMaxSize().background(p.paper)) {
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Pad)
                .padding(top = 12.dp, bottom = 120.dp),
        ) {
            when (Nav.tab) {
                "home" -> HomeScreen()
                "gallery" -> GalleryScreen()
                "track" -> TrackScreen()
                else -> SettingsScreen()
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = Pad).padding(bottom = 12.dp)) {
            ToastHost(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (AppStore.undo()) Toaster.show("Undone")
            }
            PillNav()
        }
        SheetHost()
        if (!AppStore.data.onboarded) Onboarding()
    }
}

@Composable
private fun PillNav() {
    val p = pal()
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(p.nav)
            .border(1.dp, p.line, RoundedCornerShape(32.dp)).padding(6.dp),
    ) {
        listOf(Triple("home", "My widgets", Ic.home), Triple("gallery", "New", Ic.plus), Triple("track", "My info", Ic.track), Triple("settings", "Settings", Ic.gear))
            .forEach { (k, l, ic) -> NavItem(k, l, ic, Modifier.weight(1f)) }
    }
}

@Composable
private fun NavItem(key: String, label: String, icon: ImageVector, modifier: Modifier) {
    val p = pal()
    val on = Nav.tab == key
    Column(
        modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).background(if (on) p.ink else Color.Transparent).clickable { Nav.tab = key },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, label, tint = if (on) p.paper else p.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = T.nav, color = if (on) p.paper else p.muted)
    }
}
