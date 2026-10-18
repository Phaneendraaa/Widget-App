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
    var help by mutableStateOf(false)
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
    TrackSheetHost()
    if (Sheets.help) HelpSheet { Sheets.help = false }
}

@Composable
internal fun FieldLabel(t: String) = Text(t, style = T.smallB, color = pal().muted)

@Composable
fun Toggle(on: Boolean) {
    val p = pal()
    Box(Modifier.size(52.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) p.ink else p.soft).padding(3.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(Color.White))
    }
}
