package com.phani.widgets.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Single source of truth. Screens read [rev] so they recompose after every change. */
object AppStore {
    lateinit var data: AppData
        private set
    var rev by mutableIntStateOf(0)
        private set
    private var file: File? = null
    private var undoSnap: String? = null
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        val f = File(ctx.applicationContext.filesDir, "widgetapp.json")
        file = f
        data = try {
            if (f.exists()) {
                val o = JSONObject(f.readText())
                val d = appDataFrom(o)
                // v6 starts everyone clean: earlier builds filled the app with example data.
                if (o.optInt("v", 5) < 6 && d.sampleData) emptyData(d.cur).also { it.theme = d.theme; it.h24 = d.h24; it.dmy = d.dmy } else d
            } else emptyData(guessCurrency())
        } catch (e: Exception) {
            emptyData(guessCurrency())
        }
        loaded = true
        save()
    }

    /** Example data, used only to draw previews (gallery, design picker, onboarding). Never saved. */
    private var sampleCache: AppData? = null
    val sample: AppData get() = sampleCache?.takeIf { it.cur == data.cur } ?: sampleData(data.cur).also { sampleCache = it }

    /** What widget renderers read: the example data while drawing a preview, otherwise the user's own. */
    var previewing = false
    val view: AppData get() = if (previewing) sample else data

    fun save() {
        try { file?.writeText(data.toJson().toString()) } catch (_: Exception) {}
    }

    /** Apply a change, save it, and refresh every screen. [undoable] keeps a snapshot for Undo. */
    fun change(undoable: Boolean = false, block: AppData.() -> Unit) {
        if (undoable) undoSnap = data.toJson().toString()
        data.block()
        save()
        rev++
        onChanged?.invoke()
    }

    fun canUndo() = undoSnap != null

    fun undo(): Boolean {
        val s = undoSnap ?: return false
        data = appDataFrom(JSONObject(s))
        undoSnap = null
        save()
        rev++
        onChanged?.invoke()
        return true
    }

    fun replace(d: AppData) {
        data = d
        save()
        rev++
        onChanged?.invoke()
    }

    /** Set by the widget layer so home-screen widgets redraw after app changes. */
    var onChanged: (() -> Unit)? = null

    fun guessCurrency(): String {
        val l = Locale.getDefault()
        return when (l.country.uppercase()) {
            "IN" -> "INR"; "GB" -> "GBP"; "CA" -> "CAD"; "AU", "NZ" -> "AUD"; "CH" -> "CHF"; "JP" -> "JPY"
            "DE", "FR", "ES", "IT", "NL", "IE", "AT", "BE", "PT", "FI", "GR" -> "EUR"
            else -> "USD"
        }
    }
}
