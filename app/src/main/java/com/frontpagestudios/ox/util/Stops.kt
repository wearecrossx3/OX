package com.frontpagestudios.ox.util

import com.frontpagestudios.ox.data.TripPoint

/** A moment the vehicle stood still — usually a traffic signal or a jam. */
data class StopEvent(
    val lat: Double,
    val lon: Double,
    val start: Long,
    val durationMs: Long,
)

object Stops {
    private const val SLOW = 1.4f          // m/s ≈ 5 km/h
    private const val MIN_STOP = 10_000L   // shorter halts are just slowing down
    private const val MERGE_GAP = 6_000L   // two halts this close are one stop

    /**
     * Points are only recorded after ~4 m of movement, so standing still shows up
     * as a long gap between two nearby points, or as points with ~0 speed.
     */
    fun find(points: List<TripPoint>): List<StopEvent> {
        if (points.size < 2) return emptyList()
        val raw = mutableListOf<StopEvent>()
        var sLat = 0.0; var sLon = 0.0; var sStart = -1L; var sEnd = -1L
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            val dt = b.t - a.t
            val near = Geo.dist(a.lat, a.lon, b.lat, b.lon) < 30
            val slow = (b.speed < SLOW && a.speed < SLOW) || (dt > 8_000 && near)
            if (slow) {
                if (sStart < 0) { sStart = a.t; sLat = a.lat; sLon = a.lon }
                sEnd = b.t
            } else if (sStart >= 0) {
                raw += StopEvent(sLat, sLon, sStart, sEnd - sStart)
                sStart = -1
            }
        }
        if (sStart >= 0) raw += StopEvent(sLat, sLon, sStart, sEnd - sStart)

        // merge halts that are seconds apart (creeping in a queue)
        val merged = mutableListOf<StopEvent>()
        raw.forEach { e ->
            val last = merged.lastOrNull()
            if (last != null && e.start - (last.start + last.durationMs) < MERGE_GAP &&
                Geo.dist(last.lat, last.lon, e.lat, e.lon) < 60
            ) {
                merged[merged.lastIndex] = last.copy(durationMs = e.start + e.durationMs - last.start)
            } else merged += e
        }
        return merged.filter { it.durationMs >= MIN_STOP }
    }

    fun totalMs(stops: List<StopEvent>) = stops.sumOf { it.durationMs }
}
