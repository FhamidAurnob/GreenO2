package com.example.greeno2.domain

import com.example.greeno2.data.TransportMode
import com.example.greeno2.data.Trip
import java.time.Instant
import java.time.ZoneId

/** Levels unlocked by total CO₂ saved. Gives users a visible goal beyond raw numbers. */
enum class EcoLevel(val title: String, val emoji: String, val minKg: Double) {
    SEEDLING("Seedling", "🌱", 0.0),
    SPROUT("Sprout", "🌿", 2.0),
    SAPLING("Sapling", "🪴", 10.0),
    TREE("Tree", "🌳", 25.0),
    FOREST("Forest guardian", "🌲", 60.0);

    val next: EcoLevel? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(savedKg: Double): EcoLevel = entries.last { savedKg >= it.minKg }

        /** 0..1 progress from the current level toward the next one (1 at the top level). */
        fun progress(savedKg: Double): Float {
            val level = of(savedKg)
            val next = level.next ?: return 1f
            return ((savedKg - level.minKg) / (next.minKg - level.minKg)).toFloat().coerceIn(0f, 1f)
        }
    }
}

data class Achievement(
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

object Achievements {
    fun evaluate(trips: List<Trip>, zone: ZoneId = ZoneId.systemDefault()): List<Achievement> {
        val valid = trips.filter { !it.flagged }
        val green = valid.filter { it.mode != TransportMode.CAR }
        val greenKm = green.sumOf { it.distanceKm }
        val savedKg = valid.sumOf { it.savedKg }
        val pedalKm = green.filter { it.mode == TransportMode.BIKE || it.mode == TransportMode.E_BIKE }
            .sumOf { it.distanceKm }
        val modes = green.map { it.mode }.toSet()
        val earlyBird = green.any { Instant.ofEpochMilli(it.startTime).atZone(zone).hour < 7 }

        return listOf(
            Achievement("🚀", "First steps", "Record your first trip", valid.isNotEmpty()),
            Achievement("☁️", "1 kg saved", "Avoid 1 kg of CO₂", savedKg >= 1.0),
            Achievement("🛣️", "10 km club", "Travel 10 km low-carbon", greenKm >= 10.0),
            Achievement("🔀", "Explorer", "Use 3 green transport modes", modes.size >= 3),
            Achievement("🔟", "Regular", "Record 10 trips", valid.size >= 10),
            Achievement("🚲", "Pedal power", "Cycle 20 km", pedalKm >= 20.0),
            Achievement("🌅", "Early bird", "Go green before 7 am", earlyBird),
            Achievement("🌍", "Climate hero", "Avoid 25 kg of CO₂", savedKg >= 25.0),
        )
    }
}

/** Short eco tips shown on the home screen, one per day. */
object EcoTips {
    private val tips = listOf(
        "Trips under 2 km are often faster on foot or by bike than by car once you count parking.",
        "A bus full of passengers emits far less per person than the same people driving alone.",
        "Combining errands into one walking or cycling loop cuts both emissions and time.",
        "Checking tyre pressure on your bike makes every ride easier and faster.",
        "E-scooters and e-bikes produce most of their emissions in manufacturing, so ride them often.",
        "Try a car-free day once a week. Small habits add up to big savings.",
        "Taking the train instead of driving between cities can cut trip emissions by around 80%.",
    )

    fun forDay(epochDay: Long): String = tips[(epochDay % tips.size).toInt()]
}
