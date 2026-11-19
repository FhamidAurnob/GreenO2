package com.example.greeno2.tracking

import com.example.greeno2.data.TransportMode
import com.example.greeno2.data.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A trip that is being recorded right now. */
data class LiveTrip(
    val userId: Long,
    val mode: TransportMode,
    val startTimeMs: Long,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val gpsFixes: Int = 0,
    /** Accepted route points (lat, lon), used to sketch the route on screen. */
    val path: List<Pair<Double, Double>> = emptyList(),
)

/**
 * Process-wide bridge between [TrackingService] and the UI. The service writes, screens observe.
 */
object TrackingStore {
    private val _live = MutableStateFlow<LiveTrip?>(null)
    val live: StateFlow<LiveTrip?> = _live.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _lastCompleted = MutableStateFlow<Trip?>(null)
    val lastCompleted: StateFlow<Trip?> = _lastCompleted.asStateFlow()

    internal fun update(trip: LiveTrip?) {
        _live.value = trip
    }

    internal fun setSaving(value: Boolean) {
        _saving.value = value
    }

    internal fun complete(trip: Trip) {
        _live.value = null
        _lastCompleted.value = trip
    }

    fun dismissLastCompleted() {
        _lastCompleted.value = null
    }
}
