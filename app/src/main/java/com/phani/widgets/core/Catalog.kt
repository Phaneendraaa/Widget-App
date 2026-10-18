package com.phani.widgets.core

class DesignDef(val id: String, val name: String, val size: String, val pro: Boolean = false)
class TypeDef(val type: String, val name: String, val cat: String, val desc: String, val designs: List<DesignDef>, val isNew: Boolean = false, val pro: Boolean = false)

/** Every widget and its designs, same as the web prototype. Sizes: s = 2×2, w = 4×1, m = 4×2. */
object Catalog {
    val types = listOf(
        TypeDef("now", "Right now", "smart", "Looks across everything you track and shows the one thing that needs you, with one tap to act.",
            listOf(DesignDef("card", "Card", "m"), DesignDef("ticker", "Ticker", "w"), DesignDef("bold", "Headline", "s", true)), isNew = true, pro = true),
        TypeDef("spend", "Safe to spend", "money", "What you can spend today so your monthly budget lasts. Log with one tap and the number updates.",
            listOf(DesignDef("ring", "Ring", "s"), DesignDef("number", "Number", "s"), DesignDef("strip", "Strip", "w"), DesignDef("receipt", "Receipt", "m", true)), isNew = true),
        TypeDef("subs", "Renewals", "money", "Your next subscription charge before it hits, and what they all cost a month.",
            listOf(DesignDef("next", "Next charge", "s"), DesignDef("list", "Upcoming", "m"), DesignDef("total", "Monthly total", "s", true)), isNew = true),
        TypeDef("fast", "Fasting", "health", "A fasting timer that runs on your home screen. Start and end with one tap.",
            listOf(DesignDef("ring", "Ring", "s"), DesignDef("timer", "Timer", "w"), DesignDef("big", "Big hours", "s", true))),
        TypeDef("counter", "Counter", "health", "Water, coffee, reps, anything. Tap +1 right on the widget.",
            listOf(DesignDef("glass", "Glass", "s"), DesignDef("dots", "Dots", "w"), DesignDef("big", "Big number", "s"))),
        TypeDef("streak", "Streak", "habits", "Check in with a tap. Miss a day and a freeze keeps the streak alive.",
            listOf(DesignDef("week", "Week", "w"), DesignDef("heat", "Heatmap", "m"), DesignDef("big", "Big number", "s"), DesignDef("ring", "Week ring", "s", true))),
        TypeDef("pace", "Pace", "habits", "A goal with a deadline. Shows what you need per day and whether you're ahead or behind.",
            listOf(DesignDef("ring", "Ring", "s"), DesignDef("bar", "Track", "w"), DesignDef("daily", "Daily target", "s")), isNew = true, pro = true),
        TypeDef("countdown", "Countdown", "time", "Days until the date that matters. Repeats yearly for birthdays and holidays.",
            listOf(DesignDef("big", "Big number", "s"), DesignDef("flip", "Flip", "w"), DesignDef("ring", "Ring", "s", true), DesignDef("stack", "Coming up", "m"))),
        TypeDef("year", "Progress", "time", "How much of the year, month, week and day is gone.",
            listOf(DesignDef("dots", "Year in dots", "m"), DesignDef("bars", "Four bars", "m"), DesignDef("ring", "Year ring", "s")), pro = true),
        TypeDef("clock", "World clock", "time", "The time where your people are, with the offset from you.",
            listOf(DesignDef("pair", "Two cities", "w"), DesignDef("analog", "Analog", "m", true), DesignDef("list", "Three cities", "m"))),
        TypeDef("next", "Next up", "work", "What's next on your schedule, where, and how long until it starts.",
            listOf(DesignDef("card", "Card", "w"), DesignDef("today", "Today", "m"))),
        TypeDef("attendance", "Attendance", "student", "Each class against your required minimum. Tells you how many you can miss, or must attend.",
            listOf(DesignDef("bars", "All classes", "m"), DesignDef("single", "One class", "s"), DesignDef("mark", "Mark class", "s"))),
    )
    val cats = listOf("all" to "All", "smart" to "Smart", "money" to "Money", "health" to "Health", "habits" to "Habits & goals", "time" to "Time", "work" to "Schedule", "student" to "Student")
    val styles = listOf(Triple("paper", "Paper", false), Triple("ink", "Ink", false), Triple("tint", "Tint", true), Triple("frost", "Frost", true), Triple("smoke", "Smoke", true), Triple("clear", "Clear", true))
    val fonts = listOf(Triple("grotesk", "Grotesk", false), Triple("serif", "Serif", true), Triple("mono", "Mono", true))
    val corners = listOf("round" to "Round", "soft" to "Soft", "sharp" to "Sharp")

    fun type(t: String) = types.first { it.type == t }
    fun design(t: String, d: String) = type(t).designs.firstOrNull { it.id == d } ?: type(t).designs[0]
    val totalDesigns get() = types.sumOf { it.designs.size }

    fun defaultRef(t: String): String? {
        val d = AppStore.data
        return when (t) {
            "countdown" -> d.dates.firstOrNull()?.id
            "pace" -> d.goals.firstOrNull()?.id
            "streak" -> d.habits.firstOrNull()?.id
            "counter" -> d.counters.firstOrNull()?.id
            else -> null
        }
    }

    /** What the "Shows" picker offers for this design, or null when it doesn't apply. */
    fun refOptions(c: WConf): List<Pair<String?, String>>? {
        val d = AppStore.data
        return when {
            c.type == "countdown" && c.design != "stack" -> d.dates.map { it.id to it.name }
            c.type == "pace" -> d.goals.map { it.id to it.name }
            c.type == "streak" -> d.habits.map { it.id to it.name }
            c.type == "counter" -> d.counters.map { it.id to it.name }
            c.type == "attendance" && c.design == "single" -> listOf<Pair<String?, String>>(null to "Lowest class (auto)") + d.subjects.map { it.id to it.name }
            else -> null
        }
    }

    fun newConf(t: String, design: DesignDef = type(t).designs[0]) = WConf(uid(), t, design.id, design.size, ref = defaultRef(t))
}
