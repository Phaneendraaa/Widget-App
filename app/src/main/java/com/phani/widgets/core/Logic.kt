package com.phani.widgets.core

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CurInfo(val locale: Locale, val name: String, val pro: String, val quick: List<Int>)

val CUR: Map<String, CurInfo> = linkedMapOf(
    "USD" to CurInfo(Locale.forLanguageTag("en-US"), "US dollar", "$9.99", listOf(5, 10, 20)),
    "EUR" to CurInfo(Locale.forLanguageTag("de-DE"), "Euro", "9,99 €", listOf(5, 10, 20)),
    "GBP" to CurInfo(Locale.forLanguageTag("en-GB"), "British pound", "£8.99", listOf(5, 10, 20)),
    "CAD" to CurInfo(Locale.forLanguageTag("en-CA"), "Canadian dollar", "CA$12.99", listOf(5, 10, 20)),
    "AUD" to CurInfo(Locale.forLanguageTag("en-AU"), "Australian dollar", "A$14.99", listOf(5, 10, 20)),
    "CHF" to CurInfo(Locale.forLanguageTag("de-CH"), "Swiss franc", "CHF 9.90", listOf(5, 10, 20)),
    "JPY" to CurInfo(Locale.forLanguageTag("ja-JP"), "Japanese yen", "¥1,500", listOf(500, 1000, 2000)),
    "INR" to CurInfo(Locale.forLanguageTag("en-IN"), "Indian rupee", "₹299", listOf(50, 100, 200)),
)

val CITIES = listOf(
    "New York" to "America/New_York", "Los Angeles" to "America/Los_Angeles", "Chicago" to "America/Chicago",
    "Toronto" to "America/Toronto", "Vancouver" to "America/Vancouver", "Mexico City" to "America/Mexico_City",
    "São Paulo" to "America/Sao_Paulo", "London" to "Europe/London", "Dublin" to "Europe/Dublin", "Paris" to "Europe/Paris",
    "Berlin" to "Europe/Berlin", "Amsterdam" to "Europe/Amsterdam", "Madrid" to "Europe/Madrid", "Zurich" to "Europe/Zurich",
    "Stockholm" to "Europe/Stockholm", "Dubai" to "Asia/Dubai", "Mumbai" to "Asia/Kolkata", "Singapore" to "Asia/Singapore",
    "Hong Kong" to "Asia/Hong_Kong", "Seoul" to "Asia/Seoul", "Tokyo" to "Asia/Tokyo", "Sydney" to "Australia/Sydney",
    "Melbourne" to "Australia/Melbourne", "Auckland" to "Pacific/Auckland",
)

private val D get() = AppStore.data

fun today(): LocalDate = LocalDate.now()
fun pd(s: String): LocalDate = LocalDate.parse(s)
fun daysBetween(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()
fun mins(t: String): Int { val p = t.split(":"); return p[0].toInt() * 60 + p[1].toInt() }
fun plural(n: Int, w: String) = "$n $w" + if (n == 1) "" else "s"
fun pad2(n: Int) = n.toString().padStart(2, '0')

object Fmt {
    private val MON = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    fun dow(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    fun dowName(i: Int) = DayOfWeek.of(if (i == 0) 7 else i).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    fun short(d: LocalDate) = if (D.dmy) "${d.dayOfMonth} ${MON[d.monthValue - 1]}" else "${MON[d.monthValue - 1]} ${d.dayOfMonth}"
    fun full(d: LocalDate) = "${dow(d)}, ${short(d)}"

    fun money(n: Double, dec: Int? = null): String {
        return try {
            val c = CUR[D.cur] ?: CUR.getValue("USD")
            val nf = NumberFormat.getCurrencyInstance(c.locale)
            nf.currency = Currency.getInstance(D.cur)
            val digits = dec ?: if (abs(n) >= 100 || D.cur == "JPY") 0 else if (n % 1.0 == 0.0) 0 else 2
            nf.maximumFractionDigits = digits
            nf.minimumFractionDigits = digits
            nf.format(n)
        } catch (e: Exception) { "${D.cur} ${n.roundToInt()}" }
    }

    fun hm(t: String): String { val h = t.split(":")[0].toInt(); val m = t.split(":")[1].toInt(); return clock(h, m, true) }
    fun clock(h: Int, m: Int, suffix: Boolean): String =
        if (D.h24) "${pad2(h)}:${pad2(m)}" else "${(h + 11) % 12 + 1}:${pad2(m)}" + if (suffix) (if (h >= 12) " pm" else " am") else ""
    fun now(): String { val t = LocalTime.now(); return clock(t.hour, t.minute, false) }
    fun at(ms: Long, suffix: Boolean = false): String {
        val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()); return clock(t.hour, t.minute, suffix)
    }
    fun num(n: Double): String = if (n >= 10) n.roundToInt().toString() else ((n * 10).roundToInt() / 10.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
    fun unit(u: String) = if (u == "%") "%" else " $u"
    fun hhmm(h: Double): String = "${floor(h).toInt()}:${pad2(((h % 1) * 60).toInt())}"
    fun dur(n: Int): String = if (n < 60) "$n min" else "${n / 60}h" + if (n % 60 != 0) " ${n % 60}m" else ""
    fun trimNum(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()
}

/* ---------------- money ---------------- */

class SpendState(val per: Double, val left: Double, val today: Double, val days: Int, val spent: Double, val resets: LocalDate, val todays: List<Spend>)

fun spendState(): SpendState {
    val t = today()
    val p = D.payday
    var st = t.withDayOfMonth(min(p, t.lengthOfMonth()))
    if (st.isAfter(t)) st = st.minusMonths(1)
    val nx = st.plusMonths(1)
    val left = daysBetween(t, nx)
    var before = 0.0
    var tod = 0.0
    val ti = t.toString()
    D.log.forEach {
        val d = pd(it.date)
        if (d.isBefore(st) || !d.isBefore(nx)) return@forEach
        if (it.date == ti) tod += it.amt else before += it.amt
    }
    val per = (D.budget - before) / max(1, left)
    return SpendState(per, per - tod, tod, left, before + tod, nx, D.log.filter { it.date == ti })
}

class SubNext(val s: Sub, val d: LocalDate, val days: Int)

fun subNext(s: Sub): SubNext {
    val t = today()
    var d = pd(s.next)
    var g = 0
    while (d.isBefore(t) && g++ < 400) d = if (s.cycle == "year") d.plusYears(1) else d.plusMonths(1)
    return SubNext(s, d, daysBetween(t, d))
}

fun subsSorted(): List<SubNext> = D.subs.map { subNext(it) }.sortedBy { it.days }
fun subsMonthly(): Double = D.subs.sumOf { if (it.cycle == "year") it.amt / 12 else it.amt }

/* ---------------- habits, goals, dates ---------------- */

class StreakState(val n: Int, val today: Boolean, val canFreeze: Boolean)

fun streak(h: Habit): StreakState {
    val t = today()
    var n = 0
    var d = if (h.done.contains(t.toString())) t else t.minusDays(1)
    for (i in 0 until 900) {
        val k = d.toString()
        if (h.done.contains(k)) n++ else if (!h.frozen.contains(k)) break
        d = d.minusDays(1)
    }
    val y = t.minusDays(1).toString()
    return StreakState(n, h.done.contains(t.toString()), !h.done.contains(y) && !h.frozen.contains(y) && h.freezes > 0)
}

class PaceState(val pct: Double, val per: Double, val ahead: Double, val left: Int, val exp: Double)

fun pace(g: Goal): PaceState {
    val t = today()
    val total = max(1, daysBetween(pd(g.start), pd(g.end)))
    val el = daysBetween(pd(g.start), t).coerceIn(0, total)
    val left = max(1, daysBetween(t, pd(g.end)))
    val exp = g.target * el / total
    val rate = g.target / total
    return PaceState((g.done / g.target).coerceIn(0.0, 1.0), (g.target - g.done) / left, (g.done - exp) / rate, left, exp / g.target)
}

class DateNext(val d: LocalDate, val days: Int)

fun dateNext(x: DateItem): DateNext {
    val t = today()
    var d = pd(x.date)
    if (x.yearly) {
        d = d.withYear(t.year)
        if (d.isBefore(t)) d = d.withYear(t.year + 1)
    }
    return DateNext(d, daysBetween(t, d))
}

/* ---------------- fasting, counters ---------------- */

class FastState(val on: Boolean, val h: Double, val pct: Double, val target: Int, val endMs: Long)

fun fastState(): FastState {
    val f = D.fast
    val st = f.start ?: return FastState(false, 0.0, 0.0, f.target, 0)
    val h = (System.currentTimeMillis() - st) / 3_600_000.0
    return FastState(true, h, (h / f.target).coerceIn(0.0, 1.0), f.target, st + f.target * 3_600_000L)
}

fun cnt(c: Counter): Int = c.log[today().toString()] ?: 0

/* ---------------- time zones ---------------- */

class TzNow(val h: Int, val m: Int, val wd: String)

fun tzNow(tz: String): TzNow {
    val z = ZonedDateTime.now(ZoneId.of(tz))
    return TzNow(z.hour, z.minute, z.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
}

fun tzOffset(tz: String): String {
    val now = Instant.now()
    val diff = (ZoneId.of(tz).rules.getOffset(now).totalSeconds - ZoneId.systemDefault().rules.getOffset(now).totalSeconds) / 3600.0
    if (diff == 0.0) return "Same time"
    return (if (diff > 0) "+" else "−") + Fmt.num(abs(diff)) + "h"
}

fun tzText(o: TzNow) = Fmt.clock(o.h, o.m, false)

/* ---------------- schedule ---------------- */

/** Day index like JS: 0 = Sunday … 6 = Saturday. */
fun dayIndex(d: LocalDate = today()): Int = d.dayOfWeek.value % 7

class NextUp(val c: SchedItem, val state: String, val left: Int = 0, val inMin: Int = 0, val dayName: String = "")

fun daySched(i: Int): List<SchedItem> = (D.sched[i] ?: mutableListOf()).sortedBy { mins(it.s) }

fun nextUp(): NextUp? {
    val now = LocalTime.now()
    val m = now.hour * 60 + now.minute
    val dow = dayIndex()
    for (c in daySched(dow)) {
        if (mins(c.s) <= m && m < mins(c.e)) return NextUp(c, "now", left = mins(c.e) - m)
        if (mins(c.s) > m) return NextUp(c, "next", inMin = mins(c.s) - m)
    }
    for (k in 1..7) {
        val d = (dow + k) % 7
        val l = daySched(d)
        if (l.isNotEmpty()) return NextUp(l[0], "later", dayName = if (k == 1) "Tomorrow" else Fmt.dowName(d))
    }
    return null
}

/* ---------------- attendance ---------------- */

class Att(val p: Double, val ok: Boolean, val skip: Int, val need: Int)

fun att(s: Subject, xa: Int = 0, xt: Int = 0): Att {
    val a = s.a + xa
    val t = s.t + xt
    val r = D.req / 100.0
    val p = if (t > 0) a.toDouble() / t else 1.0
    return if (p >= r) Att(p, true, floor(a / r - t + 1e-9).toInt(), 0)
    else Att(p, false, 0, ceil((r * t - a) / (1 - r) - 1e-9).toInt())
}

fun subjectBy(id: String?): Subject? = D.subjects.firstOrNull { it.id == id }

/* ---------------- Right now ---------------- */

class NowAct(val label: String, val kind: String, val id: String? = null)
class NowItem(val sc: Double, val k: String, val h: String, val sub: String, val act: NowAct?, val hot: Boolean)

fun rightNow(): NowItem {
    val c = mutableListOf<NowItem>()
    val nh = LocalTime.now().hour
    val sp = spendState()
    if (sp.left < 0) c.add(NowItem(88.0, "Money", "Over today by " + Fmt.money(-sp.left), "Tomorrow's allowance adjusts down", NowAct("Log spend", "spend-open"), true))
    subsSorted().forEach { r -> if (r.days <= 2) c.add(NowItem(80.0 - r.days, "Renewal", r.s.name + if (r.days == 0) " renews today" else " renews in " + plural(r.days, "day"), Fmt.money(r.s.amt) + " · " + Fmt.short(r.d), null, r.days <= 1)) }
    val fs = fastState()
    if (fs.on && fs.h < fs.target && fs.target - fs.h <= 3) c.add(NowItem(78.0, "Fasting", Fmt.hhmm(fs.target - fs.h) + " to your ${fs.target}h goal", "Fasting for " + Fmt.hhmm(fs.h), NowAct("End fast", "fast"), false))
    if (fs.on && fs.h >= fs.target) c.add(NowItem(84.0, "Fasting", "${fs.target}h fast complete", "Fasted " + Fmt.hhmm(fs.h), NowAct("End fast", "fast"), true))
    D.subjects.forEach { s -> val r = att(s); if (!r.ok) c.add(NowItem(86.0 + r.need, "Attendance", s.short + " at ${(r.p * 100).roundToInt()}%", "Attend next ${r.need} to reach ${D.req}%", NowAct("Mark present", "att-p", s.id), true)) }
    D.goals.forEach { g -> val p = pace(g); if (p.ahead <= -1) c.add(NowItem(70 + min(10.0, -p.ahead), "Goal", g.name + ": " + plural((-p.ahead).roundToInt(), "day") + " behind", Fmt.num(p.per) + Fmt.unit(g.unit) + " a day to finish on time", NowAct("+" + Fmt.trimNum(g.step) + Fmt.unit(g.unit), "goal", g.id), true)) }
    D.habits.forEach { h -> val st = streak(h); if (!st.today && st.n >= 3 && nh >= 17) c.add(NowItem(74.0, "Streak", "Keep your ${st.n}-day ${h.name.lowercase()} streak", "Not done today yet", NowAct("Done", "hab", h.id), false)) }
    val nu = nextUp()
    if (nu != null && nu.state == "next" && nu.inMin <= 30) c.add(NowItem(82.0, "Next up", nu.c.title + " in ${nu.inMin} min", nu.c.place, null, false))
    D.counters.forEach { x -> val v = cnt(x); val exp = x.goal * ((nh - 8) / 12.0).coerceIn(0.0, 1.0); if (v < exp - 2) c.add(NowItem(60.0, x.name, "${x.goal - v} ${x.unit} to go", "$v of ${x.goal} today", NowAct("+1", "cnt", x.id), false)) }
    D.dates.forEach { x -> val r = dateNext(x); if (r.days in 0..7) c.add(NowItem(58.0 - r.days, "Coming up", x.name + if (r.days == 0) " is today" else " in " + plural(r.days, "day"), Fmt.full(r.d), null, false)) }
    return c.maxByOrNull { it.sc } ?: NowItem(0.0, "All clear", "Nothing needs you", "Money, goals and habits are on track", null, false)
}
