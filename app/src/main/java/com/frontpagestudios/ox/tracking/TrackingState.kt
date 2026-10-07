package com.frontpagestudios.ox.tracking

import com.frontpagestudios.ox.data.LiveTrip
import com.frontpagestudios.ox.data.TripMode
import kotlinx.coroutines.flow.MutableStateFlow

object TrackingState {
    /** Current trip being recorded, or null when idle. */
    val live = MutableStateFlow<LiveTrip?>(null)

    /** True while the finished trip is being named and written to disk. */
    val saving = MutableStateFlow(false)

    /** Id of the trip that was just saved; "" means it was too short and got discarded. */
    val lastSaved = MutableStateFlow<String?>(null)

    /** Set by activity recognition when the phone leaves a vehicle. */
    @Volatile
    var autoEndHint: Boolean = false

    fun setMode(mode: TripMode) {
        live.value = live.value?.copy(mode = mode)
    }
}
