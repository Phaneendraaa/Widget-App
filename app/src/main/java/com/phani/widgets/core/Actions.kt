package com.phani.widgets.core

import kotlin.math.roundToInt

/** Result of a widget tap: a message to show, and whether Undo applies. */
class ActResult(val msg: String?, val undo: Boolean = true, val openSpend: Boolean = false)

/** Runs the same actions as the prototype. Used by the in-app widgets and by real home-screen widgets. */
object Actions {
    fun run(act: String, id: String?, value: String?, cid: String? = null): ActResult {
        val d = AppStore.data
        AppStore.pendingCid = cid
        val k = today().toString()
        when (act) {
            "spend-q" -> {
                val v = value?.toDoubleOrNull() ?: return ActResult(null)
                AppStore.change(true) { log.add(Spend(uid(), v, k, "Quick add")) }
                return ActResult("Logged ${Fmt.money(v)} · ${Fmt.money(spendState().left)} left today")
            }
            "spend-open" -> return ActResult(null, false, openSpend = true)
            "undo" -> return ActResult(if (AppStore.undo()) "Undone" else null, false)
            "cnt-" -> {
                val c = d.counters.firstOrNull { it.id == id } ?: return ActResult(null)
                if ((c.log[k] ?: 0) <= 0) return ActResult(null, false)
                val n = value?.toIntOrNull() ?: 1
                AppStore.change(true) { c.log[k] = maxOf(0, (c.log[k] ?: 0) - n) }
                return ActResult("${c.name}: ${c.log[k]} of ${c.goal}${if (c.unit == "g") "g" else ""}")
            }
            "fast" -> {
                return if (d.fast.start != null) {
                    val h = (System.currentTimeMillis() - d.fast.start!!) / 3_600_000.0
                    AppStore.change(true) { fast.history.add(FastRec(fast.start!!, System.currentTimeMillis())); fast.start = null }
                    ActResult("Fast ended at " + Fmt.hhmm(h))
                } else {
                    AppStore.change(true) { fast.start = System.currentTimeMillis() }
                    ActResult("Fast started · goal ${d.fast.target}h")
                }
            }
            "cnt" -> {
                val c = d.counters.firstOrNull { it.id == id } ?: return ActResult(null)
                val n = value?.toIntOrNull() ?: 1
                AppStore.change(true) { c.log[k] = (c.log[k] ?: 0) + n }
                return ActResult("${c.name}: ${c.log[k]} of ${c.goal}${if (c.unit == "g") "g" else ""}")
            }
            "hab" -> {
                val h = d.habits.firstOrNull { it.id == id } ?: return ActResult(null)
                val before = streak(h).n
                AppStore.change(true) {
                    if (h.done.contains(k)) h.done.remove(k) else {
                        h.done.add(k)
                        val n = streak(h).n
                        if (n > h.record) h.record = n
                        if (n % 7 == 0 && n != before && h.freezes < 3) h.freezes++
                    }
                }
                return ActResult("${h.name}: ${streak(h).n}-day streak")
            }
            "goal" -> {
                val g = d.goals.firstOrNull { it.id == id } ?: return ActResult(null)
                AppStore.change(true) { g.done = minOf(g.target, ((g.done + g.step) * 100).roundToInt() / 100.0) }
                val a = pace(g).ahead.roundToInt()
                return ActResult("${g.name}: ${Fmt.trimNum(g.done)}/${Fmt.trimNum(g.target)} · " + if (a <= -1) "${-a}d behind" else if (a >= 1) "${a}d ahead" else "on track")
            }
            "rit" -> {
                val key = id ?: return ActResult(null)
                val name = if (key.startsWith("salah:")) key.removePrefix("salah:") else d.rituals.firstOrNull { it.id == key }?.name ?: return ActResult(null)
                AppStore.change(true) { Rituals.toggle(this, key) }
                val med = d.rituals.firstOrNull { it.id == key }?.kind == "med"
                return ActResult("$name " + if (med) "taken" else "done")
            }
            "att-p", "att-a" -> {
                val s = subjectBy(id) ?: return ActResult(null)
                AppStore.change(true) { s.t++; if (act == "att-p") s.a++ }
                val r = att(s)
                return ActResult("${s.short} ${if (act == "att-p") "present" else "absent"} · ${(r.p * 100).roundToInt()}%, " +
                    if (r.ok) (if (r.skip > 0) "can miss ${r.skip}" else "none left to miss") else "attend next ${r.need}")
            }
        }
        return ActResult(null)
    }
}
