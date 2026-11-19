package com.example.greeno2

import com.example.greeno2.data.TransportMode
import com.example.greeno2.domain.TripValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripValidatorTest {
    private val tenMinutes = 10 * 60_000L

    @Test
    fun normalWalkIsEligible() {
        // 0.8 km in 10 minutes = 4.8 km/h
        val v = TripValidator.validate(TransportMode.WALK, 800.0, tenMinutes, 6.0)
        assertTrue(v.eligible)
        assertFalse(v.suspicious)
    }

    @Test
    fun walkAtCarSpeedIsFlagged() {
        // 8 km in 10 minutes = 48 km/h
        val v = TripValidator.validate(TransportMode.WALK, 8_000.0, tenMinutes, 50.0)
        assertFalse(v.eligible)
        assertTrue(v.suspicious)
    }

    @Test
    fun shortSpikeOnBikeIsFlagged() {
        // Plausible average, but one sustained segment at 120 km/h
        val v = TripValidator.validate(TransportMode.BIKE, 3_000.0, tenMinutes, 120.0)
        assertTrue(v.suspicious)
    }

    @Test
    fun tinyTripEarnsNothingButIsNotSuspicious() {
        val v = TripValidator.validate(TransportMode.WALK, 50.0, tenMinutes, 3.0)
        assertFalse(v.eligible)
        assertFalse(v.suspicious)
    }

    @Test
    fun trainCanGoFast() {
        // 30 km in 15 minutes = 120 km/h
        val v = TripValidator.validate(TransportMode.TRAIN, 30_000.0, 15 * 60_000L, 140.0)
        assertTrue(v.eligible)
    }
}
