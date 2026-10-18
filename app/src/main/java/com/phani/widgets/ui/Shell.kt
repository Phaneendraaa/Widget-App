package com.phani.widgets.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phani.widgets.core.AppStore

/** Two places: Home (your widgets) and Settings. Making a widget is a full-screen flow on top. */
object Nav {
    var screen by mutableStateOf("home")
}

@Composable
fun AppShell() {
    val p = pal()
    AppStore.rev
    ClockTicker()
    Box(Modifier.fillMaxSize().background(p.paper)) {
        if (Nav.screen == "settings") BackHandler { Nav.screen = "home" }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Pad)
                .padding(top = 8.dp, bottom = 120.dp),
        ) {
            if (Nav.screen == "settings") SettingsScreen() else HomeScreen()
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = Pad).padding(bottom = 14.dp)) {
            ToastHost(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (AppStore.undo()) Toaster.show("Undone")
            }
            if (Nav.screen == "home" && AppStore.data.placed.isNotEmpty()) BigButton("New widget", icon = Ic.plus) { Create.startNew() }
        }
        if (Create.open) CreateScreen()
        SheetHost()
        if (!AppStore.data.onboarded) Onboarding()
    }
}
