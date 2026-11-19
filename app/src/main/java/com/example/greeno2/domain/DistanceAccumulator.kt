package com.example.greeno2.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val timeMs: Long,
)

/**
 * Turns a stream of raw GPS fixes into a trip distance, filtering the usual GPS problems:
 * - fixes with poor accuracy are dropped;
 * - tiny movements (jitter while standing still) are not counted, but are not lost either:
 *   the reference point is kept, so slow real movement still adds up once it exceeds the threshold;
 * - out-of-order fixes are ignored.
 *
 * Pure Kotlin so it can be unit-tested without a device.
 */
class DistanceAccumulator(
    private val maxAccuracyM: Float = 35f,
    private val minStepM: Double = 4.0,
) {
    var distanceMeters: Double = 0.0
        private set
    var maxSegmentSpeedKmh: Double = 0.0
        private set
    var lastSpeedKmh: Double = 0.0
        private set
    var acceptedPoints: Int = 0
        private set

    private var last: GeoPoint? = null

    /** Returns true if the point was accepted as part of the route. */
    fun add(p: GeoPoint): Boolean {
        if (p.accuracyM > maxAccuracyM) return false
        val prev = last
        if (prev == null) {
            last = p
            acceptedPoints++
            return true
        }
        val dtS = (p.timeMs - prev.timeMs) / 1000.0
        if (dtS <= 0) return false

        val d = haversineMeters(prev.lat, prev.lon, p.lat, p.lon)
        // Movement smaller than the GPS uncertainty is treated as noise.
        if (d < max(minStepM, p.accuracyM * 0.5)) return false

        val speedKmh = d / dtS * 3.6
        lastSpeedKmh = speedKmh
        // Only trust segment speeds over a few seconds; 1-second segments are spiky.
        if (dtS >= 3.0) maxSegmentSpeedKmh = max(maxSegmentSpeedKmh, speedKmh)

        distanceMeters += d
        last = p
        acceptedPoints++
        return true
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0

        fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
        }
    }
}
