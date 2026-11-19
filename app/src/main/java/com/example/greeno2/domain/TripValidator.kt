package com.example.greeno2.domain

import com.example.greeno2.data.TransportMode

/**
 * Anti-cheat rules applied when a trip ends.
 *
 * - Very short trips are recorded but earn nothing (stops GPS noise and tap-farming).
 * - Trips whose average speed is impossible for the chosen mode are flagged: the classic cheat
 *   is choosing "Walk" or "Bicycle" while riding in a car.
 * - A sustained segment far faster than the mode allows is flagged for the same reason.
 */
object TripValidator {
    const val MIN_DISTANCE_M = 100.0
    const val MIN_DURATION_MS = 60_000L

    /** Multiplier over the mode's max average speed allowed for a single GPS segment. */
    const val SEGMENT_SPEED_TOLERANCE = 2.0

    data class Verdict(
        /** Whether the trip may earn credits / count toward savings. */
        val eligible: Boolean,
        /** Whether the trip looks faked or mis-labelled (shown to the user, excluded from ranks). */
        val suspicious: Boolean,
        val reason: String?,
    )

    fun averageSpeedKmh(distanceMeters: Double, durationMs: Long): Double {
        if (durationMs <= 0) return 0.0
        return (distanceMeters / 1000.0) / (durationMs / 3_600_000.0)
    }

    fun validate(
        mode: TransportMode,
        distanceMeters: Double,
        durationMs: Long,
        maxSegmentSpeedKmh: Double,
    ): Verdict {
        if (distanceMeters < MIN_DISTANCE_M) {
            return Verdict(false, false, "Trip too short to earn credits (under ${MIN_DISTANCE_M.toInt()} m)")
        }
        if (durationMs < MIN_DURATION_MS) {
            return Verdict(false, false, "Trip too short to earn credits (under 1 minute)")
        }
        val avg = averageSpeedKmh(distanceMeters, durationMs)
        if (avg > mode.maxAvgSpeedKmh) {
            return Verdict(
                false, true,
                "Average speed %.0f km/h is too fast for %s".format(avg, mode.label.lowercase()),
            )
        }
        if (maxSegmentSpeedKmh > mode.maxAvgSpeedKmh * SEGMENT_SPEED_TOLERANCE) {
            return Verdict(
                false, true,
                "Moved at %.0f km/h, which is unrealistic for %s".format(maxSegmentSpeedKmh, mode.label.lowercase()),
            )
        }
        return Verdict(true, false, null)
    }
}
