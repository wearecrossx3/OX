package com.frontpagestudios.ox.util

import com.frontpagestudios.ox.data.Trip

data class Hotspot(
    val lat: Double,
    val lon: Double,
    val hits: Int,
    val totalMs: Long,
    val tripCount: Int,
) {
    val avgMs: Long get() = if (hits > 0) totalMs / hits else 0
}

/** Groups every stop from every vehicle trip into places you keep getting stuck. */
object Hotspots {
    fun build(trips: List<Trip>, radius: Double = 55.0): List<Hotspot> {
        class C(var lat: Double, var lon: Double, var hits: Int, var total: Long, val trips: MutableSet<String>)
        val cs = mutableListOf<C>()
        trips.filter { it.mode.isVehicle }.forEach { t ->
            Stops.find(t.points).forEach { s ->
                val c = cs.minByOrNull { Geo.dist(it.lat, it.lon, s.lat, s.lon) }
                    ?.takeIf { Geo.dist(it.lat, it.lon, s.lat, s.lon) <= radius }
                if (c == null) cs += C(s.lat, s.lon, 1, s.durationMs, mutableSetOf(t.id))
                else {
                    c.lat = (c.lat * c.hits + s.lat) / (c.hits + 1)
                    c.lon = (c.lon * c.hits + s.lon) / (c.hits + 1)
                    c.hits++; c.total += s.durationMs; c.trips += t.id
                }
            }
        }
        return cs.map { Hotspot(it.lat, it.lon, it.hits, it.total, it.trips.size) }
            .sortedWith(compareByDescending<Hotspot> { it.totalMs }.thenByDescending { it.hits })
    }
}
