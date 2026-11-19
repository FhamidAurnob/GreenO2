package com.example.greeno2

import com.example.greeno2.data.TransportMode
import com.example.greeno2.data.Trip
import com.example.greeno2.domain.Achievements
import com.example.greeno2.domain.EcoLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class GamificationTest {
    @Test
    fun levelsFollowSavedKg() {
        assertEquals(EcoLevel.SEEDLING, EcoLevel.of(0.0))
        assertEquals(EcoLevel.SPROUT, EcoLevel.of(2.0))
        assertEquals(EcoLevel.TREE, EcoLevel.of(30.0))
        assertEquals(EcoLevel.FOREST, EcoLevel.of(1000.0))
    }

    @Test
    fun progressIsBetweenLevels() {
        assertEquals(0.5f, EcoLevel.progress(1.0), 1e-6f) // halfway from 0 to 2
        assertEquals(1f, EcoLevel.progress(500.0), 1e-6f)
    }

    private fun trip(mode: TransportMode, km: Double, saved: Double, flagged: Boolean = false, start: Long = 12 * 3_600_000L) =
        Trip(0, 1, mode, start, start + 600_000, km * 1000, 0.0, saved, 0, flagged, null)

    @Test
    fun achievementsUnlockFromTrips() {
        val trips = listOf(
            trip(TransportMode.WALK, 3.0, 0.5),
            trip(TransportMode.BIKE, 8.0, 1.36, start = 6 * 3_600_000L),
            trip(TransportMode.BUS, 5.0, 0.35),
        )
        val byTitle = Achievements.evaluate(trips, ZoneOffset.UTC).associateBy { it.title }
        assertTrue(byTitle.getValue("First steps").unlocked)
        assertTrue(byTitle.getValue("1 kg saved").unlocked)
        assertTrue(byTitle.getValue("10 km club").unlocked)
        assertTrue(byTitle.getValue("Explorer").unlocked)
        assertTrue(byTitle.getValue("Early bird").unlocked)
        assertFalse(byTitle.getValue("Regular").unlocked)
    }

    @Test
    fun flaggedTripsDoNotCount() {
        val list = Achievements.evaluate(listOf(trip(TransportMode.WALK, 50.0, 8.5, flagged = true)), ZoneOffset.UTC)
        assertTrue(list.none { it.unlocked })
    }
}
