package com.frontpagestudios.ox.tracking

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.util.Perms
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/** Uses Android's activity recognition to start/stop trips on its own. */
object AutoDetect {
    private const val ACTION = "com.frontpagestudios.ox.TRANSITION"

    private fun pendingIntent(ctx: Context): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(
            ctx, 7, Intent(ctx, TransitionReceiver::class.java).setAction(ACTION), flags
        )
    }

    @SuppressLint("MissingPermission")
    fun enable(ctx: Context) {
        if (!Perms.canAuto(ctx)) return
        val transitions = listOf(DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE).flatMap { type ->
            listOf(
                ActivityTransition.Builder().setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                ActivityTransition.Builder().setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
            )
        }
        runCatching {
            ActivityRecognition.getClient(ctx)
                .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(ctx))
        }
    }

    @SuppressLint("MissingPermission")
    fun disable(ctx: Context) {
        runCatching { ActivityRecognition.getClient(ctx).removeActivityTransitionUpdates(pendingIntent(ctx)) }
    }
}

class TransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        TripRepo.init(context)
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        if (!Prefs.autoDetect.value) return
        val event = result.transitionEvents.lastOrNull() ?: return
        val vehicleType = event.activityType == DetectedActivity.IN_VEHICLE || event.activityType == DetectedActivity.ON_BICYCLE
        if (!vehicleType) return

        if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            if (TrackingState.live.value == null && Perms.location(context)) {
                val mode = if (event.activityType == DetectedActivity.ON_BICYCLE) TripMode.BIKE else Prefs.vehicle.value
                runCatching { TrackingService.start(context, mode, auto = true) }
            }
        } else {
            TrackingState.autoEndHint = true
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        if (Prefs.autoDetect.value) AutoDetect.enable(context)
    }
}
