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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.phani.widgets.core.AppStore

/** Widgets (the gallery, home), My widgets, and Settings. The editor opens full-screen on top. */
object Nav {
    var screen by mutableStateOf("home")
}

@Composable
fun AppShell() {
    val p = pal()
    AppStore.rev
    ClockTicker()
    // each tab keeps its own scroll position
    val scrolls = androidx.compose.runtime.remember { mutableMapOf<String, androidx.compose.foundation.ScrollState>() }
    val scroll = scrolls.getOrPut(Nav.screen) { androidx.compose.foundation.ScrollState(0) }
    Box(Modifier.fillMaxSize().background(p.paper)) {
        if (Nav.screen != "home") BackHandler { Nav.screen = "home" }
        val z = Gallery.zoom()
        val outer = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(outer.density * z, outer.fontScale),
        ) {
            Column(
                Modifier.fillMaxSize().pinchZoom()
                    .graphicsLayer {
                        val live = Gallery.pinch
                        if (live > 0f) { scaleX = live / z; scaleY = live / z; transformOrigin = TransformOrigin(0.5f, 0f) }
                    }
                    .verticalScroll(scroll)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = Pad)
                    // room for the bottom bar, which keeps its normal size
                    .padding(top = 8.dp, bottom = (120f / z).dp),
            ) {
                when (Nav.screen) {
                    "settings" -> SettingsScreen()
                    "mine" -> MineScreen()
                    else -> GalleryScreen()
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = Pad).padding(bottom = 14.dp)) {
            ToastHost(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                if (AppStore.undo()) Toaster.show("Undone")
            }
            BottomTabs()
        }
        if (Create.open) CreateScreen()
        SheetHost()
        if (StyleSheet.open) StylePicker()
        if (!AppStore.data.onboarded) Onboarding()
    }
}
