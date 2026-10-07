package com.frontpagestudios.ox.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.frontpagestudios.ox.MainActivity
import com.frontpagestudios.ox.R
import com.frontpagestudios.ox.data.LiveTrip
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.util.Format
import com.frontpagestudios.ox.util.Perms

object Notifs {
    const val CH_TRACK = "ox_tracking"
    const val CH_TRIPS = "ox_trips"
    const val ID_TRACK = 41
    private const val LIME = 0xFFD2F53C.toInt()

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_TRACK, "Live tracking", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while OX records a trip"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_TRIPS, "Trip summaries", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A short summary after an auto-detected trip"
            }
        )
    }

    fun tracking(ctx: Context, live: LiveTrip?): Notification {
        val open = PendingIntent.getActivity(
            ctx, 1,
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_LIVE, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            ctx, 2,
            Intent(ctx, TrackingService::class.java).setAction(TrackingService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (live == null || live.points.isEmpty()) "Finding GPS signal…" else
            "${Format.distance(live.distance)}  ·  ${Format.clock(System.currentTimeMillis() - live.start)}  ·  ${Format.speed(live.speed)}"
        val title = when {
            live == null -> "OX is starting"
            live.auto -> "Auto trip · ${live.mode.label}"
            else -> "Tracking · ${live.mode.label}"
        }
        return NotificationCompat.Builder(ctx, CH_TRACK)
            .setSmallIcon(R.drawable.ic_stat_ox)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(LIME)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, "Finish trip", stop)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun tripSaved(ctx: Context, trip: Trip) {
        if (!Perms.notifications(ctx)) return
        val open = PendingIntent.getActivity(
            ctx, 3,
            Intent(ctx, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_TRIP, trip.id)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CH_TRIPS)
            .setSmallIcon(R.drawable.ic_stat_ox)
            .setColor(LIME)
            .setContentTitle(trip.title)
            .setContentText("${Format.duration(trip.durationMs)} · ${Format.distance(trip.distance)} · avg ${Format.kmh(trip.avgSpeed)} km/h")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(trip.id.hashCode(), n) }
    }
}
