package com.frontpagestudios.ox.tracking

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.frontpagestudios.ox.MainActivity
import com.frontpagestudios.ox.R
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.RouteBook
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Perms
import java.util.Calendar
import java.util.Locale

/**
 * Weekday "time to leave" nudge, built from the user's own history:
 * the usual route, when they normally leave, and the slot that is usually quickest.
 */
object Reminder {
    private const val CH = "ox_reminders"
    private const val RC = 77

    data class Plan(val minute: Int, val title: String, val body: String)

    fun channel(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CH, "Leave-time reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A weekday nudge before your usual trip"
            }
        )
    }

    private fun minuteOfDay(ts: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun label(min: Int): String {
        val h = min / 60; val m = min % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%d:%02d %s", h12, m, if (h < 12) "am" else "pm")
    }

    /** Works out when and what to remind, or null if there isn't enough history yet. */
    fun plan(trips: List<Trip>): Plan? {
        val usual = RouteBook.groups(trips.filter { it.mode.isVehicle }).firstOrNull { it.count >= 2 } ?: return null
        val starts = usual.trips.take(20).map { minuteOfDay(it.start) }.sorted()
        val typical = starts[starts.size / 2]
        val slots = RouteBook.departureSlots(usual)
        val best = slots.minByOrNull { it.second }
        val manual = Prefs.reminderMinute.value
        val at = if (manual >= 0) manual else (typical - 10).coerceAtLeast(0)
        val body = buildString {
            append("${usual.from} → ${usual.to} usually takes ${Format.duration(usual.avgMs)}.")
            if (best != null && slots.size >= 2) append(" Quickest when you leave around ${label(best.first)}.")
        }
        return Plan(at, "Time to head to ${usual.to}", body)
    }

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, RC, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Schedules (or cancels) the next weekday reminder and stores its text. */
    fun schedule(ctx: Context, trips: List<Trip>) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx))
        if (!Prefs.reminder.value) return
        val p = plan(trips) ?: return
        Prefs.raw().edit().putString("rem_title", p.title).putString("rem_body", p.body).putInt("rem_min", p.minute).apply()
        scheduleAt(ctx, p.minute)
    }

    fun scheduleAt(ctx: Context, minute: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minute / 60); set(Calendar.MINUTE, minute % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        while (c.timeInMillis <= now || c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pending(ctx)) }
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context) {
        val sp = Prefs.raw()
        val title = sp.getString("rem_title", null) ?: return
        val body = sp.getString("rem_body", "") ?: ""
        if (!Perms.notifications(ctx)) return
        val open = PendingIntent.getActivity(
            ctx, 9, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat_ox)
            .setColor(Prefs.accent.value.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(RC, n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        if (!Prefs.reminder.value) return
        Reminder.show(context)
        Reminder.scheduleAt(context, Prefs.raw().getInt("rem_min", 8 * 60))
    }
}
