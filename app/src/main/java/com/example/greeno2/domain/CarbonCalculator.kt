package com.example.greeno2.domain

import com.example.greeno2.data.TransportMode
import kotlin.math.floor

/**
 * Distance vs. mode = carbon saved + credits earned.
 *
 * "Saved" is measured against the same distance driven alone in an average car, which is the
 * trip the user is most likely replacing. A car trip therefore saves nothing.
 */
object CarbonCalculator {
    /** Average petrol/diesel car, kg CO2e per km (driver only). */
    const val BASELINE_CAR_KG_PER_KM = 0.17

    /** 1 credit per 100 g of CO2e avoided. */
    const val CREDITS_PER_KG_SAVED = 10.0

    /** Roughly what one mature tree absorbs in a year, used only for a friendly comparison. */
    const val TREE_KG_PER_YEAR = 21.0

    data class Result(
        val distanceKm: Double,
        val emittedKg: Double,
        val savedKg: Double,
        val credits: Int,
    )

    fun calculate(mode: TransportMode, distanceMeters: Double): Result {
        val km = (distanceMeters / 1000.0).coerceAtLeast(0.0)
        val emitted = km * mode.kgCo2PerKm
        val saved = (km * (BASELINE_CAR_KG_PER_KM - mode.kgCo2PerKm)).coerceAtLeast(0.0)
        val credits = floor(saved * CREDITS_PER_KG_SAVED).toInt()
        return Result(km, emitted, saved, credits)
    }
}
