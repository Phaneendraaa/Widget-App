package com.phani.widgets.core

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** Something done at a time each day: a prayer, a medicine dose. [done] holds the dates it was done. */
class Ritual(val id: String, var name: String, var time: String, var kind: String, var note: String, val done: MutableSet<String>)

/** One of today's items, made from a ritual or a calculated prayer time. [key] is what "done" is stored under. */
class DayItem(val key: String, val name: String, val time: LocalTime, val done: Boolean, val note: String = "")

/** Islamic prayer times, worked out on the phone from the city's position (the standard astronomical method). */
object Salah {
    class Method(val id: String, val name: String, val fajr: Double, val isha: Double, val ishaMin: Int = 0)

    val METHODS = listOf(
        Method("karachi", "Karachi", 18.0, 18.0),
        Method("mwl", "Muslim World League", 18.0, 17.0),
        Method("isna", "North America (ISNA)", 15.0, 15.0),
        Method("egypt", "Egypt", 19.5, 17.5),
        Method("makkah", "Umm al-Qura (Makkah)", 18.5, 0.0, ishaMin = 90),
    )

    fun defaultMethod(cur: String) = when (cur) { "INR", "PKR", "BDT" -> "karachi"; "USD", "CAD" -> "isna"; "SAR", "AED", "QAR", "KWD" -> "makkah"; else -> "mwl" }
    fun defaultHanafi(cur: String) = cur in listOf("INR", "PKR", "BDT")

    val NAMES = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
    private fun fix(a: Double, b: Double) = (a - b * floor(a / b)).let { if (it < 0) it + b else it }

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0); val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /** Sun declination and equation of time for a Julian date. */
    private fun sun(jd: Double): Pair<Double, Double> {
        val dd = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * dd, 360.0)
        val q = fix(280.459 + 0.98564736 * dd, 360.0)
        val l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * dd
        val ra = fix(darctan2(dcos(e) * dsin(l), dcos(l)) / 15, 24.0)
        return darcsin(dsin(e) * dsin(l)) to (q / 15 - fix(ra, 24.0))
    }

    /** Today's six times (Fajr…Isha) in the phone's time zone, or null if they can't be worked out here (polar days). */
    fun times(date: LocalDate, lat: Double, lon: Double, method: String, hanafi: Boolean): List<Pair<String, LocalTime>>? {
        val m = METHODS.firstOrNull { it.id == method } ?: METHODS[1]
        val tz = ZoneId.systemDefault().rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jd = julian(date.year, date.monthValue, date.dayOfMonth) - lon / (15 * 24.0)
        fun midDay(t: Double): Double { val (_, eqt) = sun(jd + t); return fix(12 - eqt, 24.0) }
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double? {
            val (decl, _) = sun(jd + t)
            val noon = midDay(t)
            val c = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if (c < -1 || c > 1) return null
            val tt = darccos(c) / 15
            return noon + if (ccw) -tt else tt
        }
        fun asr(factor: Double, t: Double): Double? {
            val (decl, _) = sun(jd + t)
            val angle = -darccot(factor + dtan(abs(lat - decl)))
            return angleTime(angle, t, false)
        }
        val fajr = angleTime(m.fajr, 5 / 24.0, true) ?: return null
        val rise = angleTime(0.833, 6 / 24.0, true) ?: return null
        val dhuhr = midDay(12 / 24.0)
        val asrT = asr(if (hanafi) 2.0 else 1.0, 13 / 24.0) ?: return null
        val sset = angleTime(0.833, 18 / 24.0, false) ?: return null
        val isha = if (m.ishaMin > 0) sset + m.ishaMin / 60.0 else angleTime(m.isha, 18 / 24.0, false) ?: return null
        fun lt(h0: Double, plusMin: Int = 0): LocalTime {
            val h = fix(h0 + tz - lon / 15.0 + plusMin / 60.0, 24.0)
            val mins = Math.round(h * 60).toInt() % (24 * 60)
            return LocalTime.of(mins / 60, mins % 60)
        }
        return listOf("Fajr" to lt(fajr), "Sunrise" to lt(rise), "Dhuhr" to lt(dhuhr, 1), "Asr" to lt(asrT), "Maghrib" to lt(sset, 1), "Isha" to lt(isha))
    }
}

object Rituals {
    private fun parse(hm: String): LocalTime = runCatching { LocalTime.parse(hm) }.getOrDefault(LocalTime.of(8, 0))

    /** Today's prayers, from calculated times or your own list. */
    fun prayers(d: AppData, date: LocalDate = today()): List<DayItem> {
        val k = date.toString()
        if (d.prayMode == "salah") {
            val pl = d.place ?: return emptyList()
            val t = Salah.times(date, pl.lat, pl.lon, d.prayMethod.ifBlank { Salah.defaultMethod(d.cur) }, d.asrHanafi) ?: return emptyList()
            val done = d.salahDone[k] ?: emptySet<String>()
            return t.filter { it.first != "Sunrise" }.map { (n, tm) -> DayItem("salah:$n", n, tm, n in done) }
        }
        return d.rituals.filter { it.kind == "prayer" && it.name.isNotBlank() }.map { DayItem(it.id, it.name, parse(it.time), k in it.done, it.note) }.sortedBy { it.time }
    }

    fun meds(d: AppData, date: LocalDate = today()): List<DayItem> {
        val k = date.toString()
        return d.rituals.filter { it.kind == "med" && it.name.isNotBlank() }.map { DayItem(it.id, it.name, parse(it.time), k in it.done, it.note) }.sortedBy { it.time }
    }

    fun items(type: String, d: AppData) = if (type == "meds") meds(d) else prayers(d)

    /** What to show first: the latest one that's due and not done, else the next one coming up. */
    fun focus(items: List<DayItem>): Pair<DayItem, Boolean>? {
        val now = LocalTime.now()
        items.lastOrNull { !it.done && !it.time.isAfter(now) }?.let { return it to true }
        items.firstOrNull { it.time.isAfter(now) }?.let { return it to false }
        return null
    }

    fun toggle(d: AppData, key: String) {
        val k = today().toString()
        if (key.startsWith("salah:")) {
            val n = key.removePrefix("salah:")
            val s = d.salahDone.getOrPut(k) { mutableSetOf() }
            if (!s.remove(n)) s.add(n)
            // keep only the last two weeks
            d.salahDone.keys.filter { it < today().minusDays(14).toString() }.forEach { d.salahDone.remove(it) }
        } else d.rituals.firstOrNull { it.id == key }?.let { if (!it.done.remove(k)) it.done.add(k) }
    }

    /** Next time any reminder is due (prayers if on, medicines if on), as epoch ms. */
    fun nextReminder(d: AppData): Long? {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val out = mutableListOf<LocalDateTime>()
        for (day in 0..1) {
            val date = today().plusDays(day.toLong())
            if (d.remPray) prayers(d, date).forEach { out.add(date.atTime(it.time)) }
            if (d.remMeds) meds(d, date).forEach { out.add(date.atTime(it.time)) }
        }
        return out.filter { it.isAfter(now.plusSeconds(30)) }.minOrNull()?.atZone(zone)?.toInstant()?.toEpochMilli()
    }

    /** Items due right now (within the last few minutes), for the reminder that just fired. */
    fun dueNow(d: AppData): List<Pair<String, DayItem>> {
        val now = LocalTime.now()
        fun near(t: LocalTime) = !t.isAfter(now.plusMinutes(1)) && t.isAfter(now.minusMinutes(5))
        val out = mutableListOf<Pair<String, DayItem>>()
        if (d.remPray) prayers(d).filter { near(it.time) && !it.done }.forEach { out.add("prayer" to it) }
        if (d.remMeds) meds(d).filter { near(it.time) && !it.done }.forEach { out.add("meds" to it) }
        return out
    }

    fun write(d: AppData): JSONObject {
        val o = JSONObject()
        o.put("rituals", JSONArray(d.rituals.map {
            JSONObject().put("id", it.id).put("name", it.name).put("time", it.time).put("kind", it.kind).put("note", it.note).put("done", JSONArray(it.done.toList()))
        }))
        val sd = JSONObject(); d.salahDone.forEach { (k, v) -> sd.put(k, JSONArray(v.toList())) }
        o.put("salahDone", sd).put("prayMode", d.prayMode).put("prayMethod", d.prayMethod).put("asrHanafi", d.asrHanafi)
            .put("remPray", d.remPray).put("remMeds", d.remMeds)
        return o
    }

    fun read(o: JSONObject?, d: AppData) {
        if (o == null) return
        o.optJSONArray("rituals")?.let { a ->
            d.rituals.clear()
            for (i in 0 until a.length()) {
                val r = a.getJSONObject(i)
                val dn = r.optJSONArray("done")
                d.rituals.add(Ritual(r.getString("id"), r.getString("name"), r.getString("time"), r.getString("kind"), r.optString("note"),
                    (0 until (dn?.length() ?: 0)).map { dn!!.getString(it) }.toMutableSet()))
            }
        }
        o.optJSONObject("salahDone")?.let { sd -> sd.keys().forEach { k -> val a = sd.getJSONArray(k); d.salahDone[k] = (0 until a.length()).map { a.getString(it) }.toMutableSet() } }
        d.prayMode = o.optString("prayMode", d.prayMode); d.prayMethod = o.optString("prayMethod", d.prayMethod)
        d.asrHanafi = o.optBoolean("asrHanafi", d.asrHanafi)
        d.remPray = o.optBoolean("remPray", false); d.remMeds = o.optBoolean("remMeds", false)
    }
}
