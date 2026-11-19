package com.example.greeno2

import com.example.greeno2.data.TransportMode
import com.example.greeno2.domain.CarbonCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarbonCalculatorTest {
    @Test
    fun walkingSavesTheFullCarBaseline() {
        val r = CarbonCalculator.calculate(TransportMode.WALK, 5_000.0)
        assertEquals(5.0, r.distanceKm, 1e-9)
        assertEquals(0.0, r.emittedKg, 1e-9)
        assertEquals(0.85, r.savedKg, 1e-9)
        assertEquals(8, r.credits)
    }

    @Test
    fun carTripsSaveNothingAndEarnNothing() {
        val r = CarbonCalculator.calculate(TransportMode.CAR, 10_000.0)
        assertEquals(1.7, r.emittedKg, 1e-9)
        assertEquals(0.0, r.savedKg, 1e-9)
        assertEquals(0, r.credits)
    }

    @Test
    fun busSavesLessThanBike() {
        val bus = CarbonCalculator.calculate(TransportMode.BUS, 10_000.0)
        val bike = CarbonCalculator.calculate(TransportMode.BIKE, 10_000.0)
        assertTrue(bus.savedKg < bike.savedKg)
        assertEquals(0.7, bus.savedKg, 1e-9)
    }

    @Test
    fun negativeDistanceIsTreatedAsZero() {
        val r = CarbonCalculator.calculate(TransportMode.BIKE, -50.0)
        assertEquals(0.0, r.savedKg, 1e-9)
        assertEquals(0, r.credits)
    }
}
