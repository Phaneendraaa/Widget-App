package com.phani.widgets.core

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

fun uid(): String = UUID.randomUUID().toString().replace("-", "").take(8)

class Spend(var id: String, var amt: Double, var date: String, var note: String)
class Sub(var id: String, var name: String, var amt: Double, var cycle: String, var next: String)
class Habit(var id: String, var name: String, var record: Int, var freezes: Int, val done: MutableSet<String>, val frozen: MutableSet<String>)
class Goal(var id: String, var name: String, var unit: String, var target: Double, var done: Double, var start: String, var end: String, var step: Double)
class DateItem(var id: String, var name: String, var date: String, var yearly: Boolean)
class FastRec(val start: Long, val end: Long)
class Fast(var start: Long?, var target: Int, val history: MutableList<FastRec>)
class Counter(var id: String, var name: String, var unit: String, var goal: Int, val log: MutableMap<String, Int>)
class WClock(var id: String, var city: String, var tz: String)
class Subject(var id: String, var name: String, var short: String, var a: Int, var t: Int)
class Note(var id: String, var text: String)
class SchedItem(var s: String, var e: String, var title: String, var place: String, var sub: String?)

/** One widget's look and content. Used by the in-app Home preview and by real home-screen widgets. */
data class WConf(
    val id: String,
    val type: String,
    val design: String,
    val size: String,
    val style: String = "paper",
    val accent: String = "signal",
    val font: String = "grotesk",
    val corner: String = "round",
    val label: Boolean = true,
    val ref: String? = null,
    /** Full customisation. Null / -1 means "follow the look". Colours are #RRGGBB. */
    val bg: String? = null,
    val fg: String? = null,
    val alpha: Int = 100,
    val radius: Int = -1,
    val outline: Boolean = false,
    val title: String? = null,
)

class AppData(
    var pro: Boolean,
    var onboarded: Boolean,
    var sampleData: Boolean,
    var wall: String,
    var cur: String,
    var h24: Boolean,
    var dmy: Boolean,
    var req: Int,
    var theme: String,
    var budget: Double,
    var payday: Int,
    val log: MutableList<Spend>,
    val subs: MutableList<Sub>,
    val habits: MutableList<Habit>,
    val goals: MutableList<Goal>,
    val dates: MutableList<DateItem>,
    var fast: Fast,
    val counters: MutableList<Counter>,
    val clocks: MutableList<WClock>,
    val subjects: MutableList<Subject>,
    val sched: MutableMap<Int, MutableList<SchedItem>>,
    val placed: MutableList<WConf>,
    val bound: MutableMap<Int, WConf>,
    var howSeen: Boolean = false,
    val notes: MutableList<Note> = mutableListOf(),
)

/* ---------------- JSON ---------------- */

private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strs(): List<String> = (0 until length()).map { getString(it) }
private fun JSONObject.optStr(k: String): String? = if (has(k) && !isNull(k)) getString(k) else null

fun WConf.toJson(): JSONObject = JSONObject()
    .put("id", id).put("type", type).put("design", design).put("size", size).put("style", style)
    .put("accent", accent).put("font", font).put("corner", corner).put("label", label).put("ref", ref ?: JSONObject.NULL)
    .put("bg", bg ?: JSONObject.NULL).put("fg", fg ?: JSONObject.NULL).put("alpha", alpha).put("radius", radius)
    .put("outline", outline).put("title", title ?: JSONObject.NULL)

fun wconf(o: JSONObject) = WConf(
    o.getString("id"), o.getString("type"), o.getString("design"), o.getString("size"),
    o.optString("style", "paper"), o.optString("accent", "signal"), o.optString("font", "grotesk"),
    o.optString("corner", "round"), o.optBoolean("label", true), o.optStr("ref"),
    o.optStr("bg"), o.optStr("fg"), o.optInt("alpha", 100), o.optInt("radius", -1), o.optBoolean("outline", false), o.optStr("title"),
)

fun AppData.toJson(): JSONObject {
    val o = JSONObject()
    o.put("v", 6).put("pro", pro).put("onboarded", onboarded).put("sampleData", sampleData).put("wall", wall)
        .put("cur", cur).put("h24", h24).put("dmy", dmy).put("req", req).put("theme", theme)
        .put("budget", budget).put("payday", payday)
    o.put("log", JSONArray(log.map { JSONObject().put("id", it.id).put("amt", it.amt).put("date", it.date).put("note", it.note) }))
    o.put("subs", JSONArray(subs.map { JSONObject().put("id", it.id).put("name", it.name).put("amt", it.amt).put("cycle", it.cycle).put("next", it.next) }))
    o.put("habits", JSONArray(habits.map {
        JSONObject().put("id", it.id).put("name", it.name).put("record", it.record).put("freezes", it.freezes)
            .put("done", JSONArray(it.done.toList())).put("frozen", JSONArray(it.frozen.toList()))
    }))
    o.put("goals", JSONArray(goals.map {
        JSONObject().put("id", it.id).put("name", it.name).put("unit", it.unit).put("target", it.target).put("done", it.done)
            .put("start", it.start).put("end", it.end).put("step", it.step)
    }))
    o.put("dates", JSONArray(dates.map { JSONObject().put("id", it.id).put("name", it.name).put("date", it.date).put("yearly", it.yearly) }))
    o.put("fast", JSONObject().put("start", fast.start ?: JSONObject.NULL).put("target", fast.target)
        .put("history", JSONArray(fast.history.map { JSONObject().put("s", it.start).put("e", it.end) })))
    o.put("counters", JSONArray(counters.map {
        val lg = JSONObject(); it.log.forEach { (k, v) -> lg.put(k, v) }
        JSONObject().put("id", it.id).put("name", it.name).put("unit", it.unit).put("goal", it.goal).put("log", lg)
    }))
    o.put("clocks", JSONArray(clocks.map { JSONObject().put("id", it.id).put("city", it.city).put("tz", it.tz) }))
    o.put("subjects", JSONArray(subjects.map { JSONObject().put("id", it.id).put("name", it.name).put("short", it.short).put("a", it.a).put("t", it.t) }))
    val sc = JSONObject()
    sched.forEach { (d, l) ->
        sc.put(d.toString(), JSONArray(l.map { JSONObject().put("s", it.s).put("e", it.e).put("title", it.title).put("place", it.place).put("sub", it.sub ?: JSONObject.NULL) }))
    }
    o.put("sched", sc)
    o.put("placed", JSONArray(placed.map { it.toJson() }))
    val b = JSONObject(); bound.forEach { (k, v) -> b.put(k.toString(), v.toJson()) }
    o.put("bound", b)
    o.put("howSeen", howSeen)
    o.put("notes", JSONArray(notes.map { JSONObject().put("id", it.id).put("text", it.text) }))
    return o
}

fun appDataFrom(o: JSONObject): AppData {
    val f = o.getJSONObject("fast")
    val sc = o.getJSONObject("sched")
    val sched = mutableMapOf<Int, MutableList<SchedItem>>()
    sc.keys().forEach { k ->
        sched[k.toInt()] = sc.getJSONArray(k).objs().map { SchedItem(it.getString("s"), it.getString("e"), it.getString("title"), it.getString("place"), it.optStr("sub")) }.toMutableList()
    }
    val bo = o.optJSONObject("bound") ?: JSONObject()
    val bound = mutableMapOf<Int, WConf>()
    bo.keys().forEach { k -> bound[k.toInt()] = wconf(bo.getJSONObject(k)) }
    return AppData(
        pro = o.getBoolean("pro"), onboarded = o.getBoolean("onboarded"), sampleData = o.getBoolean("sampleData"),
        wall = o.getString("wall"), cur = o.getString("cur"), h24 = o.getBoolean("h24"), dmy = o.getBoolean("dmy"),
        req = o.getInt("req"), theme = o.optString("theme", "auto"),
        budget = o.getDouble("budget"), payday = o.getInt("payday"),
        log = o.getJSONArray("log").objs().map { Spend(it.getString("id"), it.getDouble("amt"), it.getString("date"), it.getString("note")) }.toMutableList(),
        subs = o.getJSONArray("subs").objs().map { Sub(it.getString("id"), it.getString("name"), it.getDouble("amt"), it.getString("cycle"), it.getString("next")) }.toMutableList(),
        habits = o.getJSONArray("habits").objs().map {
            Habit(it.getString("id"), it.getString("name"), it.getInt("record"), it.getInt("freezes"),
                it.getJSONArray("done").strs().toMutableSet(), it.getJSONArray("frozen").strs().toMutableSet())
        }.toMutableList(),
        goals = o.getJSONArray("goals").objs().map {
            Goal(it.getString("id"), it.getString("name"), it.getString("unit"), it.getDouble("target"), it.getDouble("done"),
                it.getString("start"), it.getString("end"), it.getDouble("step"))
        }.toMutableList(),
        dates = o.getJSONArray("dates").objs().map { DateItem(it.getString("id"), it.getString("name"), it.getString("date"), it.getBoolean("yearly")) }.toMutableList(),
        fast = Fast(if (f.isNull("start")) null else f.getLong("start"), f.getInt("target"),
            f.getJSONArray("history").objs().map { FastRec(it.getLong("s"), it.getLong("e")) }.toMutableList()),
        counters = o.getJSONArray("counters").objs().map {
            val lg = it.getJSONObject("log"); val m = mutableMapOf<String, Int>(); lg.keys().forEach { k -> m[k] = lg.getInt(k) }
            Counter(it.getString("id"), it.getString("name"), it.getString("unit"), it.getInt("goal"), m)
        }.toMutableList(),
        clocks = o.getJSONArray("clocks").objs().map { WClock(it.getString("id"), it.getString("city"), it.getString("tz")) }.toMutableList(),
        subjects = o.getJSONArray("subjects").objs().map { Subject(it.getString("id"), it.getString("name"), it.getString("short"), it.getInt("a"), it.getInt("t")) }.toMutableList(),
        sched = sched,
        placed = o.getJSONArray("placed").objs().map { wconf(it) }.toMutableList(),
        bound = bound,
        howSeen = o.optBoolean("howSeen", false),
        notes = (o.optJSONArray("notes") ?: JSONArray()).objs().map { Note(it.getString("id"), it.getString("text")) }.toMutableList(),
    )
}

/** A fresh start: nothing tracked, no widgets. */
fun emptyData(cur: String): AppData = AppData(
    pro = true, onboarded = false, sampleData = false, wall = "stone", cur = cur, h24 = false,
    dmy = cur !in listOf("USD", "CAD"), req = 75, theme = "auto", budget = 0.0, payday = 1,
    log = mutableListOf(), subs = mutableListOf(), habits = mutableListOf(), goals = mutableListOf(), dates = mutableListOf(),
    fast = Fast(null, 16, mutableListOf()), counters = mutableListOf(), clocks = mutableListOf(), subjects = mutableListOf(),
    sched = mutableMapOf(), placed = mutableListOf(), bound = mutableMapOf(),
)

/* ---------------- sample data ---------------- */

fun sampleData(cur: String): AppData {
    val t = LocalDate.now()
    val y = t.year
    fun d(n: Long) = t.plusDays(n).toString()
    val k = when (cur) { "JPY" -> 100.0; "INR" -> 60.0; else -> 1.0 }
    fun r2(v: Double) = Math.round(v * k * 100) / 100.0
    val run = mutableSetOf<String>()
    for (i in 1..34) if (i != 6 && i != 19 && i != 27 && !(i > 22 && i % 5 == 0)) run.add(d(-i.toLong()))
    val read = mutableSetOf<String>()
    for (i in 0 until 40) if (i % 3 != 2) read.add(d(-i.toLong()))
    val wk = { listOf(
        SchedItem("09:30", "09:45", "Team standup", "Video call", null),
        SchedItem("11:00", "11:50", "Machine Learning", "Room 204", "ml"),
        SchedItem("13:30", "14:30", "Design review", "Studio", null),
        SchedItem("15:00", "15:50", "Computer Networks", "Room 110", "cn"),
        SchedItem("18:00", "19:00", "Run club", "Park gate", null),
    ).toMutableList() }
    val sched = mutableMapOf<Int, MutableList<SchedItem>>()
    for (day in 1..5) sched[day] = wk()
    sched[6] = mutableListOf(SchedItem("10:00", "11:30", "Long run", "River path", null))
    return AppData(
        pro = false, onboarded = false, sampleData = true, wall = "stone", cur = cur, h24 = false,
        dmy = cur !in listOf("USD", "CAD"), req = 75, theme = "auto",
        budget = r2(2400.0), payday = 1,
        log = mutableListOf(
            Spend(uid(), r2(6.5), d(0), "Coffee"), Spend(uid(), r2(14.0), d(0), "Lunch"),
            Spend(uid(), r2(84.0), d(-1), "Groceries"), Spend(uid(), r2(18.0), d(-2), "Ride"),
            Spend(uid(), r2(640.0), d(-9), "Rent share"),
        ),
        subs = mutableListOf(
            Sub("s1", "Netflix", r2(15.49), "month", d(2)), Sub("s2", "Spotify", r2(11.99), "month", d(9)),
            Sub("s3", "iCloud+", r2(2.99), "month", d(14)), Sub("s4", "Gym", r2(39.0), "month", d(21)),
            Sub("s5", "Adobe", r2(263.88), "year", d(47)),
        ),
        habits = mutableListOf(
            Habit("h1", "Run", 16, 1, run, mutableSetOf(d(-6))),
            Habit("h2", "Read", 9, 2, read, mutableSetOf()),
            Habit("h3", "Meditate", 4, 1, mutableSetOf(d(-1), d(-2)), mutableSetOf()),
        ),
        goals = mutableListOf(
            Goal("g1", "Books this year", "books", 24.0, 17.0, LocalDate.of(y, 1, 1).toString(), LocalDate.of(y, 12, 31).toString(), 1.0),
            Goal("g2", "Marathon training", "km", 400.0, 182.0, d(-40), d(30), 5.0),
        ),
        dates = mutableListOf(
            DateItem("d1", "Lisbon trip", d(26), false), DateItem("d2", "Mom's birthday", d(11), true),
            DateItem("d3", "Christmas", LocalDate.of(y, 12, 25).toString(), true), DateItem("d4", "Half marathon", d(30), false),
        ),
        fast = Fast(System.currentTimeMillis() - (13.4 * 3_600_000).toLong(), 16, mutableListOf()),
        counters = mutableListOf(
            Counter("c1", "Water", "glasses", 8, mutableMapOf(d(0) to 5)),
            Counter("c2", "Coffee", "cups", 3, mutableMapOf(d(0) to 2)),
        ),
        clocks = mutableListOf(WClock("k1", "New York", "America/New_York"), WClock("k2", "London", "Europe/London"), WClock("k3", "Tokyo", "Asia/Tokyo")),
        subjects = mutableListOf(
            Subject("ml", "Machine Learning", "ML", 28, 36), Subject("cn", "Computer Networks", "CN", 32, 45),
            Subject("db", "Databases", "DB", 39, 50), Subject("os", "Operating Systems", "OS", 44, 49),
        ),
        sched = sched,
        placed = mutableListOf(
            WConf(uid(), "now", "card", "m", "ink", "signal"),
            WConf(uid(), "spend", "ring", "s", "paper", "mint"),
            WConf(uid(), "fast", "ring", "s", "paper", "signal"),
            WConf(uid(), "countdown", "flip", "w", "ink", "sun", "mono", ref = "d1"),
            WConf(uid(), "streak", "heat", "m", "paper", "signal", ref = "h1"),
            WConf(uid(), "clock", "pair", "w", "frost", "cobalt", "serif"),
        ),
        bound = mutableMapOf(),
        notes = mutableListOf(Note("n1", "Drink water. Call mum. Ship it on Friday.")),
    )
}
