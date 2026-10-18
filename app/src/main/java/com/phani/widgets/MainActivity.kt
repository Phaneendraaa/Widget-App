package com.phani.widgets

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/** Minimal launcher screen so the app is installable and shows the count. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val days = CountdownWidget.daysLeft()
        val view = TextView(this).apply {
            text = "$days days to your birthday\n\nLong-press your home screen → Widgets → Birthday Countdown"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setTypeface(typeface, Typeface.BOLD)
        }
        setContentView(view)
        CountdownWidget.refreshAll(this)
    }
}
