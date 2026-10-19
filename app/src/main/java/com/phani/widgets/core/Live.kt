package com.phani.widgets.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

/* ---------------- live data: weather, air, rates ---------------- */

/** A city picked for weather and air quality. */
class Place(val name: String, val area: String, val lat: Double, val lon: Double)

class WxHour(val t: String, val temp: Double, val code: Int, val pop: Int)
class WxDay(val d: String, val hi: Double, val lo: Double, val code: Int, val pop: Int)

/** Weather for the picked city. Temperatures in °C; [offset] is the city's UTC offset in seconds. */
class Wx(
    val at: Long, val temp: Double, val feels: Double, val code: Int, val day: Boolean, val hum: Int, val wind: Double,
    val offset: Int, val hours: List<WxHour>, val days: List<WxDay>, val rise: String?, val set: String?,
)

/** Air quality (US AQI scale) for the picked city. */
class Aq(val at: Long, val aqi: Int, val pm25: Double, val pm10: Double)

/** One rate: its value, change in % (vs a day ago), and when it was fetched. */
class Quote(val v: Double, val ch: Double?, val at: Long)

/**
 * Public data fetched straight from free, keyless services (Open-Meteo, Frankfurter, CoinGecko).
 * Only the city or currencies you pick are sent; nothing about you. Nothing is fetched until you add a live widget.
 */
object Live {
    const val WX_MS = 30 * 60_000L
    const val AQ_MS = 60 * 60_000L
    const val Q_MS = 30 * 60_000L

    /** Currencies the exchange-rate service covers (European Central Bank reference rates). */
    val FX = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "SGD", "CNY", "HKD", "NZD", "SEK", "NOK", "DKK",
        "KRW", "MXN", "BRL", "ZAR", "TRY", "PLN", "THB", "MYR", "IDR", "PHP", "ILS", "CZK", "HUF")

    val COINS = linkedMapOf("bitcoin" to ("Bitcoin" to "BTC"), "ethereum" to ("Ethereum" to "ETH"),
        "solana" to ("Solana" to "SOL"), "ripple" to ("XRP" to "XRP"), "dogecoin" to ("Dogecoin" to "DOGE"))

    private const val OZ_G = 31.1034768

    /* ---- labels ---- */

    fun cond(code: Int): String = when (code) {
        0 -> "Clear"; 1 -> "Mostly clear"; 2 -> "Partly cloudy"; 3 -> "Cloudy"
        45, 48 -> "Fog"; in 51..57 -> "Drizzle"; in 61..67 -> "Rain"; in 71..77 -> "Snow"
        in 80..82 -> "Showers"; 85, 86 -> "Snow showers"; in 95..99 -> "Thunderstorm"; else -> "—"
    }

    /** US AQI band: name and colour. */
    fun aqiBand(aqi: Int): Pair<String, Int> = when {
        aqi <= 50 -> "Good" to 0xFF2FB36B.toInt()
        aqi <= 100 -> "Moderate" to 0xFFE2B33A.toInt()
        aqi <= 150 -> "Unhealthy for some" to 0xFFF0883E.toInt()
        aqi <= 200 -> "Unhealthy" to 0xFFE5484D.toInt()
        aqi <= 300 -> "Very unhealthy" to 0xFF9B5DE5.toInt()
        else -> "Hazardous" to 0xFF8B1E3F.toInt()
    }

    fun aqiAdvice(aqi: Int): String = when {
        aqi <= 50 -> "Great day to be outside"
        aqi <= 100 -> "Fine for most people"
        aqi <= 150 -> "Sensitive groups: take it easy"
        aqi <= 200 -> "Limit long time outdoors"
        aqi <= 300 -> "Stay in if you can"
        else -> "Avoid going outside"
    }

    fun quoteName(k: String): String = when {
        k == "gold" -> "Gold"
        k.startsWith("c:") -> COINS[k.drop(2)]?.first ?: k.drop(2).replaceFirstChar { it.uppercase() }
        k.startsWith("fx:") -> k.split(":").let { "${it[1]}/${it[2]}" }
        else -> k
    }

    fun quoteSub(k: String, cur: String): String = when {
        k == "gold" -> if (cur == "INR") "24K · per 10 g" else "per troy ounce"
        k.startsWith("c:") -> (COINS[k.drop(2)]?.second ?: "") + " · 24h"
        k.startsWith("fx:") -> k.split(":").let { "1 ${it[1]} in ${it[2]}" }
        else -> ""
    }

    /** The value as shown: gold per 10 g for rupees, per ounce elsewhere. */
    fun shown(k: String, v: Double, cur: String): Double = if (k == "gold" && cur == "INR") v / OZ_G * 10 else v

    fun fmt(k: String, v: Double, cur: String): String {
        val code = if (k.startsWith("fx:")) k.split(":")[2] else cur
        val x = shown(k, v, cur)
        val dec = when { k.startsWith("fx:") -> if (x < 10) 4 else 2; x >= 1000 -> 0; x >= 1 -> 2; else -> 4 }
        return money(x, code, dec)
    }

    fun money(v: Double, code: String, dec: Int): String = try {
        val loc = CUR[code]?.locale ?: Locale.US
        val f = java.text.NumberFormat.getCurrencyInstance(loc)
        f.currency = Currency.getInstance(code)
        f.minimumFractionDigits = dec; f.maximumFractionDigits = dec
        f.format(v)
    } catch (e: Exception) { String.format(Locale.US, "%,.${dec}f %s", v, code) }

    /** Rates to start with, by home currency. */
    fun defaultRates(cur: String): MutableList<String> = when (cur) {
        "INR" -> mutableListOf("fx:USD:INR", "gold", "c:bitcoin", "c:ethereum")
        "USD" -> mutableListOf("fx:EUR:USD", "gold", "c:bitcoin", "c:ethereum")
        else -> mutableListOf(if (cur in FX) "fx:USD:$cur" else "c:ethereum", "gold", "c:bitcoin", "c:ethereum").distinct().toMutableList()
    }

    /** Hours still to come in the city, from its current hour. */
    fun nextHours(w: Wx, n: Int): List<WxHour> {
        val cityNow = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(w.offset.toLong()).withMinute(0).withSecond(0).withNano(0)
        val i = w.hours.indexOfFirst { runCatching { !LocalDateTime.parse(it.t).isBefore(cityNow) }.getOrDefault(false) }
        return if (i < 0) w.hours.takeLast(n) else w.hours.drop(i).take(n)
    }

    /** Days from today in the city. */
    fun nextDays(w: Wx, n: Int): List<WxDay> {
        val today = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(w.offset.toLong()).toLocalDate()
        return w.days.filter { runCatching { !LocalDate.parse(it.d).isBefore(today) }.getOrDefault(true) }.take(n)
    }

    fun today(w: Wx): WxDay? = nextDays(w, 1).firstOrNull()

    /* ---- JSON (kept inside the app's data file) ---- */

    fun write(d: AppData): JSONObject {
        val o = JSONObject()
        d.place?.let { o.put("place", JSONObject().put("name", it.name).put("area", it.area).put("lat", it.lat).put("lon", it.lon)) }
        d.wx?.let { w ->
            o.put("wx", JSONObject().put("at", w.at).put("temp", w.temp).put("feels", w.feels).put("code", w.code).put("day", w.day)
                .put("hum", w.hum).put("wind", w.wind).put("offset", w.offset).put("rise", w.rise ?: JSONObject.NULL).put("set", w.set ?: JSONObject.NULL)
                .put("hours", JSONArray(w.hours.map { JSONObject().put("t", it.t).put("temp", it.temp).put("code", it.code).put("pop", it.pop) }))
                .put("days", JSONArray(w.days.map { JSONObject().put("d", it.d).put("hi", it.hi).put("lo", it.lo).put("code", it.code).put("pop", it.pop) })))
        }
        d.aq?.let { o.put("aq", JSONObject().put("at", it.at).put("aqi", it.aqi).put("pm25", it.pm25).put("pm10", it.pm10)) }
        o.put("rates", JSONArray(d.rates))
        val q = JSONObject(); d.quotes.forEach { (k, v) -> q.put(k, JSONObject().put("v", v.v).put("ch", v.ch ?: JSONObject.NULL).put("at", v.at)) }
        o.put("quotes", q)
        o.put("tempF", d.tempF)
        return o
    }

    fun read(o: JSONObject?, d: AppData) {
        if (o == null) return
        o.optJSONObject("place")?.let { d.place = Place(it.getString("name"), it.optString("area"), it.getDouble("lat"), it.getDouble("lon")) }
        o.optJSONObject("wx")?.let { w ->
            d.wx = Wx(w.getLong("at"), w.getDouble("temp"), w.getDouble("feels"), w.getInt("code"), w.getBoolean("day"), w.getInt("hum"),
                w.getDouble("wind"), w.getInt("offset"),
                w.getJSONArray("hours").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { WxHour(it.getString("t"), it.getDouble("temp"), it.getInt("code"), it.getInt("pop")) } },
                w.getJSONArray("days").let { a -> (0 until a.length()).map { a.getJSONObject(it) }.map { WxDay(it.getString("d"), it.getDouble("hi"), it.getDouble("lo"), it.getInt("code"), it.getInt("pop")) } },
                if (w.isNull("rise")) null else w.optString("rise"), if (w.isNull("set")) null else w.optString("set"))
        }
        o.optJSONObject("aq")?.let { d.aq = Aq(it.getLong("at"), it.getInt("aqi"), it.getDouble("pm25"), it.getDouble("pm10")) }
        o.optJSONArray("rates")?.let { a -> d.rates.clear(); (0 until a.length()).forEach { d.rates.add(a.getString(it)) } }
        o.optJSONObject("quotes")?.let { q -> q.keys().forEach { k -> val x = q.getJSONObject(k); d.quotes[k] = Quote(x.getDouble("v"), if (x.isNull("ch")) null else x.getDouble("ch"), x.getLong("at")) } }
        d.tempF = o.optBoolean("tempF", d.tempF)
    }

    /* ---- network ---- */

    private fun get(url: String): JSONObject? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 6000
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", "WidgetApp/3.7 (Android)")
        try { if (c.responseCode in 200..299) JSONObject(c.inputStream.bufferedReader().use { it.readText() }) else null } finally { c.disconnect() }
    } catch (e: Exception) { null }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** City search (blocking; call off the main thread). */
    fun searchPlaces(q: String): List<Place> {
        val o = get("https://geocoding-api.open-meteo.com/v1/search?name=${enc(q.trim())}&count=8&language=en&format=json") ?: return emptyList()
        val a = o.optJSONArray("results") ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }.map {
            Place(it.getString("name"), listOfNotNull(it.optString("admin1").ifBlank { null }, it.optString("country").ifBlank { null }).joinToString(", "),
                it.getDouble("latitude"), it.getDouble("longitude"))
        }
    }

    fun fetchWx(p: Place): Wx? {
        val o = get("https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,is_day,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&timezone=auto&forecast_days=7") ?: return null
        return try {
            val c = o.getJSONObject("current"); val h = o.getJSONObject("hourly"); val d = o.getJSONObject("daily")
            val ht = h.getJSONArray("time"); val htp = h.getJSONArray("temperature_2m"); val hc = h.getJSONArray("weather_code"); val hp = h.optJSONArray("precipitation_probability")
            val dt = d.getJSONArray("time")
            Wx(System.currentTimeMillis(), c.getDouble("temperature_2m"), c.optDouble("apparent_temperature", c.getDouble("temperature_2m")),
                c.getInt("weather_code"), c.optInt("is_day", 1) == 1, c.optInt("relative_humidity_2m", 0), c.optDouble("wind_speed_10m", 0.0),
                o.optInt("utc_offset_seconds", 0),
                (0 until ht.length()).map { WxHour(ht.getString(it), htp.optDouble(it, 0.0), hc.optInt(it, 0), hp?.optInt(it, 0) ?: 0) },
                (0 until dt.length()).map {
                    WxDay(dt.getString(it), d.getJSONArray("temperature_2m_max").optDouble(it, 0.0), d.getJSONArray("temperature_2m_min").optDouble(it, 0.0),
                        d.getJSONArray("weather_code").optInt(it, 0), d.optJSONArray("precipitation_probability_max")?.optInt(it, 0) ?: 0)
                },
                d.optJSONArray("sunrise")?.optString(0), d.optJSONArray("sunset")?.optString(0))
        } catch (e: Exception) { null }
    }

    fun fetchAq(p: Place): Aq? {
        val o = get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${p.lat}&longitude=${p.lon}&current=us_aqi,pm2_5,pm10&timezone=auto") ?: return null
        return try {
            val c = o.getJSONObject("current")
            Aq(System.currentTimeMillis(), c.getDouble("us_aqi").roundToInt(), c.optDouble("pm2_5", 0.0), c.optDouble("pm10", 0.0))
        } catch (e: Exception) { null }
    }

    fun fetchQuotes(keys: List<String>, cur: String): Map<String, Quote> {
        val out = mutableMapOf<String, Quote>()
        val now = System.currentTimeMillis()
        // crypto and gold (a gold-backed token tracks one troy ounce), priced in the home currency
        val coins = keys.filter { it.startsWith("c:") }.map { it.drop(2) } + (if ("gold" in keys) listOf("pax-gold") else emptyList())
        if (coins.isNotEmpty()) {
            val vs = cur.lowercase()
            get("https://api.coingecko.com/api/v3/simple/price?ids=${coins.joinToString(",")}&vs_currencies=$vs&include_24hr_change=true")?.let { o ->
                coins.forEach { id ->
                    val x = o.optJSONObject(id) ?: return@forEach
                    if (!x.has(vs)) return@forEach
                    val ch = if (x.has("${vs}_24h_change") && !x.isNull("${vs}_24h_change")) x.getDouble("${vs}_24h_change") else null
                    out[if (id == "pax-gold") "gold" else "c:$id"] = Quote(x.getDouble(vs), ch, now)
                }
            }
        }
        // exchange rates, with the change since the previous published day
        keys.filter { it.startsWith("fx:") }.map { it.split(":") }.filter { it.size == 3 }.groupBy { it[1] }.forEach { (base, ps) ->
            val syms = ps.joinToString(",") { it[2] }
            val latest = get("https://api.frankfurter.dev/v1/latest?base=$base&symbols=$syms") ?: return@forEach
            val day = runCatching { LocalDate.parse(latest.getString("date")) }.getOrNull()
            val prev = day?.let { get("https://api.frankfurter.dev/v1/${it.minusDays(1)}?base=$base&symbols=$syms") }
            val r = latest.optJSONObject("rates") ?: return@forEach
            val pr = prev?.optJSONObject("rates")
            ps.forEach { p ->
                if (!r.has(p[2])) return@forEach
                val v = r.getDouble(p[2])
                val pv = pr?.optDouble(p[2], Double.NaN) ?: Double.NaN
                val ch = if (!pv.isNaN() && pv > 0 && prev?.optString("date") != latest.optString("date")) (v - pv) / pv * 100 else null
                out["fx:$base:${p[2]}"] = Quote(v, ch, now)
            }
        }
        return out
    }

    /* ---- refreshing ---- */

    private val busy = AtomicBoolean(false)
    private val pool = Executors.newCachedThreadPool()
    private val main = Handler(Looper.getMainLooper())

    /** Is anything live set up? (Nothing is fetched otherwise.) */
    fun wanted(d: AppData): Boolean = d.place != null || d.rates.isNotEmpty()

    /**
     * Fetch whatever is out of date (or everything, with [force]) in the background, save it, and redraw the widgets.
     * [done] runs on the main thread when finished, fetched or not.
     */
    fun refresh(ctx: Context, force: Boolean = false, done: (() -> Unit)? = null) {
        val d = AppStore.data
        val now = System.currentTimeMillis()
        val place = d.place
        val needW = place != null && (force || now - (d.wx?.at ?: 0) > WX_MS)
        val needA = place != null && (force || now - (d.aq?.at ?: 0) > AQ_MS)
        val keys = d.rates.toList()
        val needQ = keys.isNotEmpty() && (force || keys.any { now - (d.quotes[it]?.at ?: 0) > Q_MS })
        if (!(needW || needA || needQ) || !busy.compareAndSet(false, true)) { done?.let { main.post(it) }; return }
        val cur = d.cur
        pool.execute {
            try {
                val fw = if (needW) pool.submit<Wx?> { fetchWx(place!!) } else null
                val fa = if (needA) pool.submit<Aq?> { fetchAq(place!!) } else null
                val fq = if (needQ) pool.submit<Map<String, Quote>> { fetchQuotes(keys, cur) } else null
                val w = runCatching { fw?.get(9, TimeUnit.SECONDS) }.getOrNull()
                val a = runCatching { fa?.get(9, TimeUnit.SECONDS) }.getOrNull()
                val q = runCatching { fq?.get(9, TimeUnit.SECONDS) }.getOrNull()
                main.post {
                    if (w != null || a != null || !q.isNullOrEmpty()) AppStore.change {
                        // only keep results for the city that's still picked
                        if (this.place === place) { if (w != null) wx = w; if (a != null) aq = a }
                        q?.forEach { (k, v) -> quotes[k] = v }
                    }
                    done?.invoke()
                }
            } catch (e: Exception) {
                done?.let { main.post(it) }
            } finally { busy.set(false) }
        }
    }

    /** "12 min ago", for showing how fresh the data is. */
    fun ago(at: Long): String {
        val m = ((System.currentTimeMillis() - at) / 60_000).toInt()
        return when { m < 2 -> "just now"; m < 60 -> "$m min ago"; m < 48 * 60 -> "${m / 60} h ago"; else -> "${m / 1440} days ago" }
    }

    fun deg(c: Double, f: Boolean): String = "${(if (f) c * 9 / 5 + 32 else c).roundToInt()}°"

    fun pct(ch: Double): String = (if (ch >= 0) "▲ " else "▼ ") + String.format(Locale.US, "%.2f%%", abs(ch))
}
