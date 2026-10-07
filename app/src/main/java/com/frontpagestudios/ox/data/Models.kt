package com.frontpagestudios.ox.data

enum class TripMode(val label: String) {
    CAR("Car"), BIKE("Bike"), WALK("Walk");

    val isVehicle: Boolean get() = this != WALK
}

data class TripPoint(
    val lat: Double,
    val lon: Double,
    val t: Long,
    val speed: Float, // m/s
)

data class Trip(
    val id: String,
    val start: Long,
    val end: Long,
    val mode: TripMode,
    val auto: Boolean,
    val points: List<TripPoint>,
    val distance: Double, // meters
    val maxSpeed: Float, // m/s
    val origin: String?,
    val destination: String?,
    /** true when the user typed the names themselves; they win over saved places */
    val renamed: Boolean = false,
) {
    val durationMs: Long get() = (end - start).coerceAtLeast(0)
    val avgSpeed: Double
        get() = if (durationMs > 0) distance / (durationMs / 1000.0) else 0.0
    val fromName: String
        get() = if (renamed && origin != null) origin else Places.match(points.firstOrNull())?.name ?: origin ?: "Start"
    val toName: String
        get() = if (renamed && destination != null) destination else Places.match(points.lastOrNull())?.name ?: destination ?: "Finish"
    val title: String
        get() = "$fromName → $toName"
    val steps: Int get() = (distance / 0.76).toInt()
}

/** In-progress trip, owned by TrackingService and observed by the UI. */
data class LiveTrip(
    val start: Long,
    val mode: TripMode,
    val auto: Boolean,
    val points: List<TripPoint> = emptyList(),
    val distance: Double = 0.0,
    val speed: Float = 0f,
    val maxSpeed: Float = 0f,
    val lastMoving: Long = start,
    val current: TripPoint? = null,
) {
    val avgSpeed: Double
        get() {
            val sec = (System.currentTimeMillis() - start) / 1000.0
            return if (sec > 0) distance / sec else 0.0
        }
}
