package com.phani.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Catalog
import com.phani.widgets.core.WConf
import com.phani.widgets.ui.AppTheme
import com.phani.widgets.ui.Fonts
import com.phani.widgets.ui.Pad
import com.phani.widgets.ui.T
import com.phani.widgets.ui.TopBar
import com.phani.widgets.ui.WidgetView
import com.phani.widgets.ui.pal
import com.phani.widgets.ui.wall
import com.phani.widgets.ui.Preview
import com.phani.widgets.ui.widgetName
import com.phani.widgets.ui.BigButton
import com.phani.widgets.widget.LargeWidget
import com.phani.widgets.widget.WideWidget
import com.phani.widgets.widget.Widgets

/** Opens when a widget is added from the launcher: pick which widget and design it shows. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        Widgets.prepare(this)
        Fonts.init(assets)

        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        val size = when (info?.provider) {
            ComponentName(this, WideWidget::class.java) -> "w"
            ComponentName(this, LargeWidget::class.java) -> "m"
            ComponentName(this, com.phani.widgets.widget.TallWidget::class.java) -> "t"
            else -> "s"
        }

        // Placed from the app with a chosen design: bind it straight away.
        val pending = Widgets.takePending(this)
        if (pending != null && pending.size == size) { done(id, pending); return }
        // already linked (the add popup's callback got here first)
        AppStore.data.bound[id]?.let { done(id, it); return }

        setContent {
            val mode = AppStore.data.theme
            val dark = when (mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            AppTheme(dark) {
                AppStore.rev
                val p = pal()
                val mine = AppStore.data.placed
                val fits = mine.filter { it.size == size }
                // made for another size: offer the same widget in a design that fits here
                val others = mine.filter { it.size != size }.mapNotNull { c ->
                    Catalog.type(c.type).designs.firstOrNull { it.size == size }?.let { d -> c.copy(design = d.id, size = size) }
                }
                Column(Modifier.fillMaxSize().background(p.paper).windowInsetsPadding(WindowInsets.systemBars).verticalScroll(rememberScrollState()).padding(horizontal = Pad).padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TopBar("Which widget is this?", if (mine.isEmpty()) "You haven't made one yet. Make one now and it appears here." else "Pick one you made. It stays in sync with the app.")
                    (fits + others).forEach { c ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.card).clickable { done(id, c) }.padding(10.dp)) {
                            Preview(c)
                            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(widgetName(c), style = T.bodyB, color = p.ink, modifier = Modifier.weight(1f))
                                Text("Use this", style = T.smallB, color = p.ink)
                            }
                        }
                    }
                    BigButton("Make a new one", kind = if (mine.isEmpty()) "solid" else "ghost") {
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        startActivity(Intent(this@WidgetConfigActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .putExtra("bindId", id).putExtra("size", size))
                        finish()
                    }
                }
            }
        }
    }

    private fun pick(id: Int, conf: WConf) {
        done(id, conf)
    }

    private fun done(id: Int, conf: WConf) {
        AppStore.change { bound[id] = conf }
        Widgets.updateAll(this)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}
