package com.phani.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                HomeScreen()
            }
        }
    }
}

private val ScreenBg = Color(0xFFECECF1)

/** Returns false when the launcher can't or won't take the request. */
fun requestPin(ctx: Context, c: Countdown): Boolean {
    Store.setPending(ctx, c.id)
    return try {
        val mgr = AppWidgetManager.getInstance(ctx)
        mgr.isRequestPinAppWidgetSupported &&
            mgr.requestPinAppWidget(ComponentName(ctx, CountdownWidget::class.java), null, null)
    } catch (e: Exception) {
        false
    }
}

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(Store.all(ctx)) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Countdown?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ScreenBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New countdown") },
                containerColor = Color(0xFF0F0F0F),
                contentColor = Color(0xFFFFC107),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Countdowns",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
            Text(
                "No popup after tapping Add? Long-press your home screen → Widgets → Countdown Widgets, drag it out, then pick your countdown.",
                color = Color(0xFF6B6B76),
            )
            Spacer(Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No countdowns yet.\nTap “New countdown” to make your first.",
                        color = Color(0xFF6B6B76),
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
                ) {
                    items(items, key = { it.id }) { c ->
                        val st = Styles.get(c.style)
                        CountdownCard(c) {
                            Button(
                                onClick = { if (!requestPin(ctx, c)) showHelp = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(st.accent),
                                    contentColor = Color(st.bg),
                                ),
                            ) { Text("Add to home screen", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { editing = c; showEditor = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(st.sub))
                            }
                            IconButton(onClick = {
                                Store.delete(ctx, c.id)
                                items = Store.all(ctx)
                                CountdownWidget.refreshAll(ctx)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(st.sub))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } },
            title = { Text("Add it manually") },
            text = {
                Text(
                    "Your launcher didn't accept the request.\n\n" +
                        "1. Long-press an empty spot on your home screen\n" +
                        "2. Tap Widgets\n" +
                        "3. Find Countdown Widgets and drag it out\n" +
                        "4. Pick your countdown\n\n" +
                        "Tip: in Settings → Apps → Countdown Widgets → Permissions, allow “Home screen shortcuts” if you see it, then try the button again."
                )
            },
        )
    }

    if (showEditor) {
        Editor(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = {
                Store.upsert(ctx, it)
                items = Store.all(ctx)
                CountdownWidget.refreshAll(ctx)
                showEditor = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Editor(initial: Countdown?, onDismiss: () -> Unit, onSave: (Countdown) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now().plusDays(30)) }
    var yearly by remember { mutableStateOf(initial?.yearly ?: true) }
    var style by remember { mutableStateOf(initial?.style ?: 0) }
    var showPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initial == null) "New countdown" else "Edit countdown",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name (e.g. Birthday)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Repeat every year", Modifier.weight(1f))
                Switch(checked = yearly, onCheckedChange = { yearly = it })
            }
            Text("Style", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Styles.all.forEachIndexed { i, s ->
                    val selected = style == i
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(s.bg))
                            .border(
                                BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Color(0xFF6750A4) else Color(0xFFBDBDC6)),
                                RoundedCornerShape(18.dp),
                            )
                            .clickable { style = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("12", color = Color(s.accent), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
            Button(
                onClick = {
                    onSave(
                        Countdown(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title.trim().ifEmpty { "Countdown" },
                            date = date,
                            yearly = yearly,
                            style = style,
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
