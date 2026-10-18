package com.phani.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Opens when a Countdown widget is added: choose which saved countdown it shows. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        fun choose(countdownId: String) {
            Store.bind(this, widgetId, countdownId)
            CountdownWidget.render(this, AppWidgetManager.getInstance(this), widgetId)
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            )
            finish()
        }

        // Added via "Add to home screen" in the app: bind straight away, no extra step.
        val pending = Store.takePending(this)
        if (pending != null && Store.get(this, pending) != null) {
            choose(pending)
            return
        }

        val items = Store.all(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(containerColor = Color(0xFFECECF1)) { inner ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(inner)
                            .padding(horizontal = 20.dp)
                    ) {
                        Text(
                            "Pick a countdown",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
                        )
                        if (items.isEmpty()) {
                            Text("You haven't made any countdowns yet.", color = Color(0xFF6B6B76))
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                startActivity(Intent(this@WidgetConfigActivity, MainActivity::class.java))
                                finish()
                            }) { Text("Create one") }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                            ) {
                                items(items, key = { it.id }) { c ->
                                    CountdownCard(c, Modifier.clickable { choose(c.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
