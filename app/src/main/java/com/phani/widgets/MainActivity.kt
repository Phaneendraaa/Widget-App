package com.phani.widgets

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.phani.widgets.core.AppStore
import com.phani.widgets.ui.AppShell
import com.phani.widgets.ui.AppTheme
import com.phani.widgets.ui.Create
import com.phani.widgets.ui.Fonts
import com.phani.widgets.ui.TrackState
import com.phani.widgets.widget.Widgets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppStore.load(this)
        Fonts.init(assets)
        com.phani.widgets.widget.WFonts.init(assets)
        Widgets.prepare(this)
        Widgets.scheduleTick(this)
        handle(intent)
        setContent {
            AppStore.rev
            val mode = AppStore.data.theme
            val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            AppTheme(dark) { AppShell() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        Widgets.prune(this)
    }

    /** Opened from a home-screen widget or from the widget setup screen. */
    private fun handle(i: Intent?) {
        i ?: return
        val bind = i.getIntExtra("bindId", -1)
        if (bind != -1) {
            Create.startNew(bindId = bind, size = i.getStringExtra("size"))
            i.removeExtra("bindId")
            return
        }
        when (i.getStringExtra("open")) {
            "spend-open" -> TrackState.open("spend")
            "open" -> {
                val c = AppStore.data.placed.firstOrNull { it.id == i.getStringExtra("cid") }
                if (c != null && !Create.open) Create.startEdit(c)
            }
        }
        i.removeExtra("open")
    }
}
