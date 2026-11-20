package com.example.greeno2.domain

import com.example.greeno2.data.TransportMode
import com.example.greeno2.data.Trip
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * Weekly community challenges ("Join Challenges" in the design). Progress is measured from the
 * user's verified (non-flagged) trips in the current Monday–Sunday week. Joined challenges that
 * are complete can be claimed once per week for bonus Green Points.
 */
data class Challenge(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val target: Double,
    val unit: String,
    val points: Int,
)

data class ChallengeProgress(
    val challenge: Challenge,
    val current: Double,
) {
    val fraction: Float get() = (current / challenge.target).toFloat().coerceIn(0f, 1f)
    val complete: Boolean get() = current >= challenge.target
}

object Challenges {
    val all = listOf(
        Challenge("low_carbon_20", "🌿", "Low-Carbon Week", "Travel 20 km by low-carbon transport this week", 20.0, "km", 60),
        Challenge("walk_10", "🚶", "Walk It Off", "Walk 10 km this week", 10.0, "km", 40),
        Challenge("streak_5", "🔥", "Green Commuter", "Make a green trip on 5 different days", 5.0, "days", 70),
        Challenge("mix_3", "🔀", "Mix It Up", "Use 3 different green transport modes", 3.0, "modes", 50),
        Challenge("car_free", "🚫", "Car-Free Week", "Make 5 green trips and no car trips this week", 5.0, "trips", 80),
    )

    fun byId(id: String): Challenge? = all.firstOrNull { it.id == id }

    /** Period key like "2026-W47", used to store joins and claims per week. */
    fun periodKey(date: LocalDate = LocalDate.now()): String =
        "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

    fun weekStart(date: LocalDate = LocalDate.now()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun progress(
        trips: List<Trip>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ChallengeProgress> {
        val start = weekStart(today)
        val week = trips.filter {
            !it.flagged && !Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate().isBefore(start)
        }
        val green = week.filter { it.mode != TransportMode.CAR }
        val greenKm = green.sumOf { it.distanceKm }
        val walkKm = green.filter { it.mode == TransportMode.WALK }.sumOf { it.distanceKm }
        val days = green.map { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }.toSet().size
        val modes = green.map { it.mode }.toSet().size
        val carTrips = week.count { it.mode == TransportMode.CAR }

        return all.map { c ->
            val value = when (c.id) {
                "low_carbon_20" -> greenKm
                "walk_10" -> walkKm
                "streak_5" -> days.toDouble()
                "mix_3" -> modes.toDouble()
                "car_free" -> if (carTrips > 0) 0.0 else green.size.toDouble()
                else -> 0.0
            }
            ChallengeProgress(c, value)
        }
    }
}

/** "Alternative green travel" suggestions for a planned trip, from the design's trip planner. */
object TripPlanner {
    data class Option(val mode: TransportMode, val emittedKg: Double, val savedKg: Double, val points: Int)

    fun compare(distanceKm: Double): List<Option> =
        TransportMode.entries.map { mode ->
            val r = CarbonCalculator.calculate(mode, distanceKm * 1000)
            Option(mode, r.emittedKg, r.savedKg, r.credits)
        }.sortedBy { it.emittedKg }

    /** The greenest option that is realistic for the distance. */
    fun suggest(distanceKm: Double): TransportMode = when {
        distanceKm <= 2.5 -> TransportMode.WALK
        distanceKm <= 10 -> TransportMode.BIKE
        distanceKm <= 20 -> TransportMode.E_BIKE
        distanceKm <= 40 -> TransportMode.BUS
        else -> TransportMode.TRAIN
    }
}
