package com.example.greeno2

import com.example.greeno2.domain.DistanceAccumulator
import com.example.greeno2.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceAccumulatorTest {
    // ~0.000009 degrees of latitude ≈ 1 m
    private fun north(meters: Double) = meters / 111_195.0

    @Test
    fun haversineMatchesKnownDistance() {
        // One degree of latitude is ~111.2 km
        val d = DistanceAccumulator.haversineMeters(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_195.0, d, 50.0)
    }

    @Test
    fun accumulatesStraightLineWalk() {
        val acc = DistanceAccumulator()
        for (i in 0..10) {
            acc.add(GeoPoint(north(i * 10.0), 0.0, 5f, i * 5_000L))
        }
        assertEquals(100.0, acc.distanceMeters, 1.0)
        assertEquals(7.2, acc.maxSegmentSpeedKmh, 0.2) // 10 m / 5 s
    }

    @Test
    fun ignoresInaccurateFixes() {
        val acc = DistanceAccumulator()
        acc.add(GeoPoint(0.0, 0.0, 5f, 0))
        assertFalse(acc.add(GeoPoint(north(500.0), 0.0, 200f, 5_000)))
        assertEquals(0.0, acc.distanceMeters, 1e-9)
    }

    @Test
    fun jitterWhileStandingStillIsNotCounted() {
        val acc = DistanceAccumulator()
        acc.add(GeoPoint(0.0, 0.0, 8f, 0))
        for (i in 1..20) {
            val wobble = if (i % 2 == 0) 2.0 else -2.0
            acc.add(GeoPoint(north(wobble), 0.0, 8f, i * 4_000L))
        }
        assertEquals(0.0, acc.distanceMeters, 1e-9)
    }

    @Test
    fun slowMovementStillAddsUp() {
        val acc = DistanceAccumulator(minStepM = 4.0)
        acc.add(GeoPoint(0.0, 0.0, 4f, 0))
        // 1 m steps: each is below the threshold on its own, but they accumulate
        var accepted = 0
        for (i in 1..12) if (acc.add(GeoPoint(north(i.toDouble()), 0.0, 4f, i * 1_000L))) accepted++
        assertTrue(accepted >= 2)
        assertEquals(12.0, acc.distanceMeters, 4.5)
    }
}
