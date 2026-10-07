package com.frontpagestudios.ox.data

/** All trips between the same two named ends, e.g. Home → Office. */
data class RouteGroup(
    val key: String,
    val from: String,
    val to: String,
    val trips: List<Trip>, // newest first
) {
    val count get() = trips.size
    val avgMs: Long get() = if (trips.isEmpty()) 0 else trips.sumOf { it.durationMs } / trips.size
    val best: Trip? get() = trips.minByOrNull { it.durationMs }
    val worst: Trip? get() = trips.maxByOrNull { it.durationMs }
    val avgDistance: Double get() = if (trips.isEmpty()) 0.0 else trips.sumOf { it.distance } / trips.size
    val fromPlace: Boolean get() = trips.firstOrNull()?.let { Places.match(it.points.firstOrNull()) != null } == true
}

object RouteBook {
    fun keyOf(t: Trip) = "${t.fromName}|${t.toName}"

    fun groups(trips: List<Trip>): List<RouteGroup> = trips
        .filter { it.mode.isVehicle || it.fromName != it.toName }
        .groupBy { keyOf(it) }
        .map { (k, list) -> RouteGroup(k, list.first().fromName, list.first().toName, list.sortedByDescending { it.start }) }
        .sortedWith(compareByDescending<RouteGroup> { it.count }.thenByDescending { it.trips.first().start })

    fun find(trips: List<Trip>, key: String): RouteGroup? = groups(trips).firstOrNull { it.key == key }

    /** Average duration by departure time, in 30-minute slots. Returns slot start minute-of-day → avg ms. */
    fun departureSlots(group: RouteGroup): List<Pair<Int, Long>> {
        val cal = java.util.Calendar.getInstance()
        return group.trips.groupBy { t ->
            cal.timeInMillis = t.start
            val m = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            (m / 30) * 30
        }.map { (slot, list) -> slot to list.sumOf { it.durationMs } / list.size }
            .sortedBy { it.first }
    }
}
