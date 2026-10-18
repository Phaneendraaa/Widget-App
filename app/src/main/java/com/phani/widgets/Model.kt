package com.phani.widgets

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Countdown(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val yearly: Boolean,
    val style: Int,
)

class CdStyle(
    val name: String,
    val bgRes: Int,
    val bg: Int,
    val text: Int,
    val accent: Int,
    val sub: Int,
)

object Styles {
    val all = listOf(
        CdStyle("Black & Yellow", R.drawable.widget_bg_0, 0xFF0F0F0F.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFC107.toInt(), 0xFF9A9A9A.toInt()),
        CdStyle("Minimal", R.drawable.widget_bg_1, 0xFFF2F2F2.toInt(), 0xFF111111.toInt(), 0xFF111111.toInt(), 0xFF7A7A7A.toInt()),
        CdStyle("Blue", R.drawable.widget_bg_2, 0xFF2F6BFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFD6E2FF.toInt()),
    )

    fun get(i: Int): CdStyle = all.getOrElse(i) { all[0] }
}

class Shown(val big: String, val caption: String)

private fun occurrence(base: LocalDate, year: Int): LocalDate {
    val ym = YearMonth.of(year, base.monthValue)
    return ym.atDay(minOf(base.dayOfMonth, ym.lengthOfMonth()))
}

fun Countdown.target(today: LocalDate = LocalDate.now()): LocalDate {
    if (!yearly) return date
    val thisYear = occurrence(date, today.year)
    return if (thisYear.isBefore(today)) occurrence(date, today.year + 1) else thisYear
}

fun Countdown.daysLeft(today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, target(today))

fun Countdown.shown(today: LocalDate = LocalDate.now()): Shown {
    val d = daysLeft(today)
    return when {
        d == 0L -> Shown("🎉", "Today")
        d > 0L -> Shown(d.toString(), if (d == 1L) "day left" else "days left")
        else -> Shown((-d).toString(), if (d == -1L) "day ago" else "days ago")
    }
}

fun Countdown.dateLabel(): String = target().format(DateTimeFormatter.ofPattern("d MMM yyyy"))

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("countdowns", Context.MODE_PRIVATE)

    fun all(c: Context): List<Countdown> {
        val arr = JSONArray(sp(c).getString("list", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Countdown(
                id = o.getString("id"),
                title = o.getString("title"),
                date = LocalDate.parse(o.getString("date")),
                yearly = o.getBoolean("yearly"),
                style = o.getInt("style"),
            )
        }
    }

    private fun write(c: Context, list: List<Countdown>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("date", it.date.toString())
                    .put("yearly", it.yearly)
                    .put("style", it.style)
            )
        }
        sp(c).edit().putString("list", arr.toString()).apply()
    }

    fun upsert(c: Context, item: Countdown) {
        val list = all(c).toMutableList()
        val i = list.indexOfFirst { it.id == item.id }
        if (i >= 0) list[i] = item else list.add(0, item)
        write(c, list)
    }

    fun delete(c: Context, id: String) = write(c, all(c).filter { it.id != id })

    fun get(c: Context, id: String): Countdown? = all(c).firstOrNull { it.id == id }

    fun bind(c: Context, widgetId: Int, countdownId: String) =
        sp(c).edit().putString("w$widgetId", countdownId).apply()

    fun bound(c: Context, widgetId: Int): String? = sp(c).getString("w$widgetId", null)

    fun unbind(c: Context, widgetId: Int) = sp(c).edit().remove("w$widgetId").apply()

    fun setPending(c: Context, id: String) =
        sp(c).edit().putString("pending", id).putLong("pendingAt", System.currentTimeMillis()).apply()

    /** Countdown the user just asked to pin (valid for 2 minutes), consumed on read. */
    fun takePending(c: Context): String? {
        val id = sp(c).getString("pending", null)
        val at = sp(c).getLong("pendingAt", 0L)
        sp(c).edit().remove("pending").apply()
        return if (id != null && System.currentTimeMillis() - at < 120_000L) id else null
    }
}
