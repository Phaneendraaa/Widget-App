package com.phani.widgets.widget

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.phani.widgets.MainActivity
import com.phani.widgets.QuickActivity
import com.phani.widgets.R
import com.phani.widgets.core.AppStore
import com.phani.widgets.core.Fmt
import com.phani.widgets.core.cnt
import com.phani.widgets.core.streak
import com.phani.widgets.core.subNext
import com.phani.widgets.core.today
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local reminders: worked out on the phone, never sent from a server.
 * Each one is a single alarm for its next time; when it fires we check whether it's still needed,
 * post it if so, and set the next one.
 */
object Reminders {
    private const val CH = "reminders"
    private val zone get() = ZoneId.systemDefault()

    fun schedule(ctx: Context) {
        val app = ctx.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val d = AppStore.data
        fun set(code: Int, kind: String, at: Long?) {
            val pi = PendingIntent.getBroadcast(app, 200 + code, Intent(app, ReminderReceiver::class.java).setAction("rem.$kind"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (at == null) am.cancel(pi) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
        set(1, "habit", if (d.remHabit) nextAt(d.remHabitAt) else null)
        set(2, "subs", if (d.remSubs) nextAt("10:00") else null)
        set(3, "fast", if (d.remFast && d.fast.start != null) (d.fast.start!! + d.fast.target * 3_600_000L).takeIf { it > System.currentTimeMillis() } else null)
        set(4, "water", if (d.remWater) nextWater() else null)
        set(5, "ritual", if (d.remPray || d.remMeds) com.phani.widgets.core.Rituals.nextReminder(d) else null)
    }

    /** The next time today or tomorrow at hh:mm. */
    private fun nextAt(hm: String): Long {
        val (h, m) = hm.split(":").map { it.toIntOrNull() ?: 0 }
        var t = LocalDateTime.now().withHour(h).withMinute(m).withSecond(0).withNano(0)
        if (!t.isAfter(LocalDateTime.now())) t = t.plusDays(1)
        return t.atZone(zone).toInstant().toEpochMilli()
    }

    /** Every 2 hours from 10:00 to 20:00. */
    private fun nextWater(): Long {
        val now = LocalDateTime.now()
        val slots = listOf(10, 12, 14, 16, 18, 20).map { now.withHour(it).withMinute(0).withSecond(0).withNano(0) }
        val t = slots.firstOrNull { it.isAfter(now) } ?: slots[0].plusDays(1)
        return t.atZone(zone).toInstant().toEpochMilli()
    }

    fun channel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null)
            nm.createNotificationChannel(NotificationChannel(CH, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Streaks at risk, renewals coming up, fasts finished"
            })
    }

    /** Runs when a reminder's time comes. */
    fun fire(ctx: Context, kind: String) {
        val d = AppStore.data
        val k = today().toString()
        when (kind) {
            "habit" -> {
                // only habits that have a widget, and only if not yet done today
                val ids = d.placed.filter { it.type == "streak" }.mapNotNull { it.ref }.toSet()
                val due = d.habits.filter { (ids.isEmpty() || it.id in ids) && it.name.isNotBlank() && k !in it.done }
                due.take(3).forEachIndexed { i, h ->
                    val n = streak(h).n
                    val done = PendingIntent.getBroadcast(ctx, 300 + i, Intent(ctx, CountdownWidgetReceiver).setAction(ACT_TAP)
                        .putExtra("act", "hab").putExtra("id", h.id).putExtra("notif", 10 + i), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    post(ctx, 10 + i, if (n > 0) "${h.name}: keep your $n-day streak" else "${h.name}: check in today",
                        if (n > 0) "Not checked in yet today." else "Start a streak today.", "Mark done" to done)
                }
            }
            "subs" -> {
                val soon = d.subs.map { subNext(it) }.filter { it.days == 1 }
                soon.take(3).forEachIndexed { i, sn ->
                    post(ctx, 20 + i, "${sn.s.name} charges tomorrow", "${Fmt.money(sn.s.amt)} on ${Fmt.full(sn.d)}. Cancel today if you don't need it.")
                }
            }
            "fast" -> if (d.fast.start != null) {
                val end = PendingIntent.getActivity(ctx, 400, QuickActivity.intent(ctx, "fast", null, null), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                post(ctx, 30, "${d.fast.target}h fast done", "Nice work. End it now or keep going.", "End fast" to end)
            }
            "ritual" -> com.phani.widgets.core.Rituals.dueNow(d).take(3).forEachIndexed { i, (kind, it) ->
                val med = kind == "meds"
                val done = PendingIntent.getBroadcast(ctx, 600 + i, Intent(ctx, CountdownWidgetReceiver).setAction(ACT_TAP)
                    .putExtra("act", "rit").putExtra("id", it.key).putExtra("notif", 60 + i), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                post(ctx, 60 + i, "Time for ${it.name}", if (med) it.note.ifBlank { "Tap Taken once you've had it." } else "It's ${it.name} time.",
                    (if (med) "Taken" else "Done") to done)
            }
            "water" -> d.counters.firstOrNull { c -> d.placed.any { it.type == "counter" && it.ref == c.id } }?.let { c ->
                val v = cnt(c)
                val hour = LocalDateTime.now().hour
                val expected = (c.goal * ((hour - 8).coerceIn(0, 12) / 12.0)).toInt()
                if (v < expected) post(ctx, 40, "${c.name}: $v of ${c.goal}", "A little behind for this time of day.")
            }
        }
        schedule(ctx)
    }

    private val CountdownWidgetReceiver = com.phani.widgets.CountdownWidget::class.java

    private fun post(ctx: Context, id: Int, title: String, text: String, action: Pair<String, PendingIntent>? = null) {
        channel(ctx)
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(ctx, 500 + id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = (if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(ctx, CH) else @Suppress("DEPRECATION") android.app.Notification.Builder(ctx))
            .setSmallIcon(R.drawable.ic_notif).setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true)
        if (action != null) b.addAction(android.app.Notification.Action.Builder(null, action.first, action.second).build())
        ctx.getSystemService(NotificationManager::class.java).notify(id, b.build())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Widgets.prepare(context)
        Reminders.fire(context, intent.action?.removePrefix("rem.") ?: return)
    }
}
