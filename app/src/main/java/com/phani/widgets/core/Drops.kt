package com.phani.widgets.core

import com.phani.widgets.widget.WTheme
import org.json.JSONObject
import java.time.LocalDate

/** A date many people count down to: a festival, a holiday, a big day. [regions] empty = everyone. */
class DropEvent(val name: String, val date: String, val regions: List<String>)

/**
 * New styles and popular countdowns, published as a small public file in the app's GitHub repo, so the app gets
 * fresh looks (Diwali, Christmas, New Year…) without an update. Nothing is sent; the file is only downloaded.
 */
object Drops {
    const val URL = "https://raw.githubusercontent.com/Phaneendraaa/Widget-App/main/drops/drops.json"
    const val MS = 12 * 3_600_000L

    /** Built in, so popular dates work offline and before the first download. */
    private val BUILT_IN = listOf(
        DropEvent("Halloween", "2026-10-31", listOf("US", "GB", "CA", "AU", "EU")),
        DropEvent("Diwali", "2026-11-08", listOf("IN")),
        DropEvent("Thanksgiving", "2026-11-26", listOf("US")),
        DropEvent("Black Friday", "2026-11-27", emptyList()),
        DropEvent("Christmas", "2026-12-25", emptyList()),
        DropEvent("New Year's Eve", "2026-12-31", emptyList()),
        DropEvent("Republic Day", "2027-01-26", listOf("IN")),
        DropEvent("Chinese New Year", "2027-02-06", emptyList()),
        DropEvent("Valentine's Day", "2027-02-14", emptyList()),
        DropEvent("Mother's Day", "2027-03-07", listOf("GB")),
        DropEvent("Eid al-Fitr", "2027-03-10", emptyList()),
        DropEvent("Holi", "2027-03-22", listOf("IN")),
        DropEvent("Easter", "2027-03-28", listOf("US", "GB", "CA", "AU", "EU")),
        DropEvent("Mother's Day", "2027-05-09", listOf("US", "CA", "AU", "IN")),
        DropEvent("Independence Day", "2027-07-04", listOf("US")),
        DropEvent("Independence Day", "2027-08-15", listOf("IN")),
        DropEvent("Diwali", "2027-10-29", listOf("IN")),
    )

    fun region(cur: String): String = when (cur) {
        "INR" -> "IN"; "USD" -> "US"; "GBP" -> "GB"; "CAD" -> "CA"; "AUD" -> "AU"; "EUR" -> "EU"; else -> "XX"
    }

    private var parsedFrom: String? = null
    private var styles: List<WTheme> = emptyList()
    private var styleUntil: Map<String, String> = emptyMap()
    private var events: List<DropEvent> = emptyList()

    private fun parse() {
        val raw = AppStore.dropsRaw
        if (raw === parsedFrom) return
        parsedFrom = raw
        styles = emptyList(); events = emptyList(); styleUntil = emptyMap()
        if (raw.isNullOrBlank()) return
        try {
            val o = JSONObject(raw)
            val until = mutableMapOf<String, String>()
            styles = (o.optJSONArray("styles") ?: org.json.JSONArray()).let { a ->
                (0 until a.length()).mapNotNull { i ->
                    val s = a.getJSONObject(i)
                    val id = "drop_" + s.getString("id")
                    s.optString("until").takeIf { it.isNotBlank() }?.let { until[id] = it }
                    WTheme(id, s.getString("name"), s.optString("blurb"), s.optString("style", "paper"),
                        s.optString("bg").ifBlank { null }, s.optString("fg").ifBlank { null }, s.optString("accent", "signal"),
                        s.optString("font", "grotesk"), s.optInt("radius", 28), s.optInt("alpha", 100),
                        s.optString("deco").ifBlank { null }, s.optString("bg2").ifBlank { null }, drop = true)
                }
            }
            styleUntil = until
            events = (o.optJSONArray("events") ?: org.json.JSONArray()).let { a ->
                (0 until a.length()).map { i ->
                    val e = a.getJSONObject(i)
                    DropEvent(e.getString("name"), e.getString("date"),
                        e.optJSONArray("regions")?.let { r -> (0 until r.length()).map { r.getString(it) } } ?: emptyList())
                }
            }
        } catch (e: Exception) { styles = emptyList(); events = emptyList() }
    }

    /** Every downloaded style, so widgets that use one keep it even after it leaves the picker. */
    fun styles(): List<WTheme> { parse(); return styles }

    /** Styles still on offer in the picker. */
    fun current(): List<WTheme> {
        parse()
        val t = LocalDate.now().toString()
        return styles.filter { s -> styleUntil[s.id]?.let { it >= t } ?: true }
    }

    /** Upcoming popular dates for your part of the world, soonest first. */
    fun upcoming(cur: String, n: Int = 10): List<DropEvent> {
        parse()
        val r = region(cur)
        val t = LocalDate.now().toString()
        return (events + BUILT_IN).filter { it.date >= t && (it.regions.isEmpty() || r in it.regions) }
            .distinctBy { it.name.lowercase() }.sortedBy { it.date }.take(n)
    }
}
