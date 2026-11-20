package com.example.greeno2

import com.example.greeno2.data.TransportMode
import com.example.greeno2.data.Trip
import com.example.greeno2.domain.Challenges
import com.example.greeno2.domain.SustainabilityQuiz
import com.example.greeno2.domain.TripPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class QuizAndChallengesTest {
    private val quiz = SustainabilityQuiz

    @Test
    fun bestAnswersScoreHundred() {
        val answers = listOf(setOf(0), setOf(0), setOf(0), setOf(0), setOf(0, 1, 2, 3))
        val r = quiz.score(answers)
        assertEquals(100, r.score)
        assertEquals(TransportMode.WALK, r.preferredMode)
    }

    @Test
    fun carHeavyAnswersScoreLowAndPickCar() {
        val answers = listOf(setOf(4), setOf(4), setOf(3), setOf(3), setOf(4))
        val r = quiz.score(answers)
        assertEquals(5, r.score)
        assertEquals(TransportMode.CAR, r.preferredMode)
    }

    @Test
    fun exclusiveOptionClearsOthersAndViceVersa() {
        val q5 = quiz.questions[4]
        val withTwo = quiz.toggle(q5, quiz.toggle(q5, emptySet(), 0), 1)
        assertEquals(setOf(0, 1), withTwo)
        val exclusive = quiz.toggle(q5, withTwo, 4)
        assertEquals(setOf(4), exclusive)
        assertEquals(setOf(2), quiz.toggle(q5, exclusive, 2))
    }

    @Test
    fun singleChoiceReplacesSelection() {
        val q1 = quiz.questions[0]
        assertEquals(setOf(3), quiz.toggle(q1, setOf(1), 3))
    }

    // Wednesday 18 Nov 2026; the week starts Monday 16 Nov.
    private val today = LocalDate.of(2026, 11, 18)
    private fun at(day: Int, hour: Int = 9) =
        LocalDate.of(2026, 11, day).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun trip(mode: TransportMode, km: Double, day: Int, flagged: Boolean = false) =
        Trip(0, 1, mode, at(day), at(day) + 1_800_000, km * 1000, 0.0, 0.0, 0, flagged, null)

    @Test
    fun challengeProgressCountsOnlyThisWeek() {
        val trips = listOf(
            trip(TransportMode.WALK, 4.0, 16),
            trip(TransportMode.BIKE, 10.0, 17),
            trip(TransportMode.BUS, 8.0, 18),
            trip(TransportMode.WALK, 50.0, 10), // last week, ignored
            trip(TransportMode.WALK, 30.0, 18, flagged = true), // flagged, ignored
        )
        val p = Challenges.progress(trips, today, ZoneOffset.UTC).associateBy { it.challenge.id }
        assertEquals(22.0, p.getValue("low_carbon_20").current, 1e-9)
        assertTrue(p.getValue("low_carbon_20").complete)
        assertEquals(4.0, p.getValue("walk_10").current, 1e-9)
        assertEquals(3.0, p.getValue("streak_5").current, 1e-9)
        assertTrue(p.getValue("mix_3").complete)
        assertEquals(3.0, p.getValue("car_free").current, 1e-9)
    }

    @Test
    fun carTripBreaksCarFreeWeek() {
        val trips = listOf(trip(TransportMode.WALK, 2.0, 16), trip(TransportMode.CAR, 5.0, 17))
        val p = Challenges.progress(trips, today, ZoneOffset.UTC).first { it.challenge.id == "car_free" }
        assertEquals(0.0, p.current, 1e-9)
        assertFalse(p.complete)
    }

    @Test
    fun periodKeyIsIsoWeek() {
        assertEquals("2026-W47", Challenges.periodKey(today))
    }

    @Test
    fun plannerSuggestsRealisticGreenModes() {
        assertEquals(TransportMode.WALK, TripPlanner.suggest(1.5))
        assertEquals(TransportMode.BIKE, TripPlanner.suggest(6.0))
        assertEquals(TransportMode.BUS, TripPlanner.suggest(30.0))
        assertEquals(TransportMode.TRAIN, TripPlanner.suggest(120.0))
        // Comparison is sorted from lowest to highest emissions, with the car last.
        assertEquals(TransportMode.CAR, TripPlanner.compare(10.0).last().mode)
    }
}
