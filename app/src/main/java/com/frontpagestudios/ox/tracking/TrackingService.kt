package com.frontpagestudios.ox.tracking

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.frontpagestudios.ox.data.LiveTrip
import com.frontpagestudios.ox.data.Prefs
import com.frontpagestudios.ox.data.Trip
import com.frontpagestudios.ox.data.TripMode
import com.frontpagestudios.ox.data.TripPoint
import com.frontpagestudios.ox.data.TripRepo
import com.frontpagestudios.ox.util.Geo
import com.frontpagestudios.ox.util.Perms
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Foreground service that records GPS points while a trip is live.
 * Started manually from the app, or automatically by [TransitionReceiver]
 * when Android detects the phone is in a vehicle.
 */
class TrackingService : Service() {

    companion object {
        const val ACTION_START = "com.frontpagestudios.ox.START"
        const val ACTION_AUTO_START = "com.frontpagestudios.ox.AUTO_START"
        const val ACTION_STOP = "com.frontpagestudios.ox.STOP"
        const val EXTRA_MODE = "mode"

        fun start(ctx: Context, mode: TripMode, auto: Boolean) {
            TrackingState.lastSaved.value = null
            val i = Intent(ctx, TrackingService::class.java)
                .setAction(if (auto) ACTION_AUTO_START else ACTION_START)
                .putExtra(EXTRA_MODE, mode.name)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            runCatching {
                ctx.startService(Intent(ctx, TrackingService::class.java).setAction(ACTION_STOP))
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var fused: FusedLocationProviderClient
    private var ticker: Job? = null
    private var finishing = false
    private var lastNotif = 0L
    private var requesting = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { onLocation(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                if (TrackingState.live.value == null && !finishing) stopSelf() else finish(manual = true)
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_AUTO_START -> {
                if (!goForeground()) return START_NOT_STICKY
                val auto = intent.action == ACTION_AUTO_START
                val mode = runCatching { TripMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: "") }
                    .getOrDefault(Prefs.vehicle.value)
                val cur = TrackingState.live.value
                when {
                    cur == null -> begin(LiveTrip(start = System.currentTimeMillis(), mode = mode, auto = auto))
                    !auto -> TrackingState.live.value = cur.copy(auto = false, mode = mode)
                }
            }
            else -> {
                // Restarted by Android after being killed: continue the saved trip.
                val snap = TripRepo.loadLive()
                if (snap == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                TrackingState.live.value = snap
                if (!goForeground()) return START_NOT_STICKY
                begin(snap)
            }
        }
        return START_STICKY
    }

    private fun goForeground(): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, Notifs.ID_TRACK, Notifs.tracking(this, TrackingState.live.value), type)
        true
    } catch (e: Exception) {
        stopSelf()
        false
    }

    @SuppressLint("MissingPermission")
    private fun begin(trip: LiveTrip) {
        finishing = false
        TrackingState.autoEndHint = false
        TrackingState.live.value = trip
        if (!Perms.location(this)) {
            finish(manual = true)
            return
        }
        if (!requesting) {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setWaitForAccurateLocation(false)
                .build()
            fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
            requesting = true
        }
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(10_000)
                checkAutoStop()
                refreshNotif(force = true)
            }
        }
    }

    private fun onLocation(loc: Location) {
        val cur = TrackingState.live.value ?: return
        if (finishing) return
        val limit = if (cur.points.isEmpty()) 60f else 35f
        if (loc.hasAccuracy() && loc.accuracy > limit) return

        val now = System.currentTimeMillis()
        val last = cur.points.lastOrNull()
        val step = if (last != null) Geo.dist(last.lat, last.lon, loc.latitude, loc.longitude) else 0.0
        val dt = if (last != null) ((now - last.t).coerceAtLeast(1)) / 1000.0 else 0.0

        // Ignore GPS jumps faster than ~250 km/h.
        if (last != null && step / dt > 70) return

        val raw = if (loc.hasSpeed()) loc.speed else if (dt > 0) (step / dt).toFloat() else 0f
        val speed = if (raw < 0.4f) 0f else raw
        val threshold = if (cur.mode == TripMode.WALK) 0.5f else 1.6f
        val moving = speed > threshold

        val here = TripPoint(loc.latitude, loc.longitude, now, speed)
        val addPoint = last == null || step >= 4.0
        val points = if (addPoint) cur.points + here else cur.points
        val next = cur.copy(
            points = points,
            distance = cur.distance + if (addPoint && last != null) step else 0.0,
            speed = speed,
            maxSpeed = max(cur.maxSpeed, speed),
            lastMoving = if (moving) now else cur.lastMoving,
            current = here,
        )
        TrackingState.live.value = next
        if (addPoint && points.size % 15 == 0) TripRepo.saveLiveAsync(next)
        refreshNotif(force = false)
    }

    private fun checkAutoStop() {
        val cur = TrackingState.live.value ?: return
        if (!cur.auto || finishing) return
        val idle = System.currentTimeMillis() - cur.lastMoving
        if ((TrackingState.autoEndHint && idle > 90_000) || idle > 5 * 60_000) finish(manual = false)
    }

    private fun refreshNotif(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotif < 5000) return
        lastNotif = now
        if (finishing) return
        runCatching {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.notify(Notifs.ID_TRACK, Notifs.tracking(this, TrackingState.live.value))
        }
    }

    private fun finish(manual: Boolean) {
        if (finishing) return
        finishing = true
        val cur = TrackingState.live.value
        fused.removeLocationUpdates(callback)
        requesting = false
        ticker?.cancel()
        TrackingState.saving.value = true

        scope.launch {
            var savedId = ""
            if (cur != null) {
                var pts = cur.points
                var end = System.currentTimeMillis()
                if (!manual) {
                    // Auto trips end when you stopped moving, not when we noticed.
                    end = max(cur.lastMoving, cur.start)
                    pts = pts.filter { it.t <= end + 15_000 }
                }
                val dist = Geo.length(pts)
                val dur = end - cur.start
                val keep = pts.size >= 2 && if (manual) dist > 30 else (dist >= 400 && dur >= 120_000)
                if (keep) {
                    val first = pts.first()
                    val lastP = pts.last()
                    val names = withContext(Dispatchers.IO) {
                        Geo.place(this@TrackingService, first) to Geo.place(this@TrackingService, lastP)
                    }
                    val trip = Trip(
                        id = "t${cur.start}",
                        start = cur.start,
                        end = end,
                        mode = cur.mode,
                        auto = cur.auto,
                        points = pts,
                        distance = dist,
                        maxSpeed = pts.maxOfOrNull { it.speed } ?: cur.maxSpeed,
                        origin = names.first,
                        destination = names.second,
                    )
                    TripRepo.save(trip)
                    savedId = trip.id
                    if (cur.auto) Notifs.tripSaved(this@TrackingService, trip)
                }
            }
            TripRepo.clearLive()
            TrackingState.live.value = null
            TrackingState.lastSaved.value = savedId
            TrackingState.saving.value = false
            TrackingState.autoEndHint = false
            ServiceCompat.stopForeground(this@TrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { fused.removeLocationUpdates(callback) }
        scope.cancel()
        super.onDestroy()
    }
}
