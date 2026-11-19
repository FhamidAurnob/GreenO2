package com.example.greeno2.data

/**
 * Transport modes the user can pick before a trip.
 *
 * [kgCo2PerKm] are per-passenger-km estimates, rounded from the UK government (DESNZ/DEFRA)
 * greenhouse-gas conversion factors. They are estimates, not measurements, and are kept in one
 * place so the environmental-organisation stakeholders can review and tune them.
 *
 * [maxAvgSpeedKmh] is the fastest average speed we accept as plausible for the mode; anything
 * faster is flagged as a likely mis-labelled (or faked) trip and earns no credits.
 */
enum class TransportMode(
    val label: String,
    val emoji: String,
    val kgCo2PerKm: Double,
    val maxAvgSpeedKmh: Double,
) {
    WALK("Walk", "🚶", 0.0, 12.0),
    BIKE("Bicycle", "🚲", 0.0, 35.0),
    E_BIKE("E-bike", "🚴", 0.005, 40.0),
    E_SCOOTER("E-scooter", "🛴", 0.035, 30.0),
    BUS("Bus", "🚌", 0.10, 90.0),
    TRAIN("Train", "🚆", 0.035, 200.0),
    CAR("Car", "🚗", 0.17, 160.0);

    companion object {
        fun fromName(name: String?): TransportMode =
            entries.firstOrNull { it.name == name } ?: WALK
    }
}

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: Long,
)

data class Trip(
    val id: Long,
    val userId: Long,
    val mode: TransportMode,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val emittedKg: Double,
    val savedKg: Double,
    val credits: Int,
    val flagged: Boolean,
    val note: String?,
) {
    val durationMs: Long get() = (endTime - startTime).coerceAtLeast(0)
    val distanceKm: Double get() = distanceMeters / 1000.0
}

data class UserStats(
    val tripCount: Int = 0,
    val distanceKm: Double = 0.0,
    val emittedKg: Double = 0.0,
    val savedKg: Double = 0.0,
    val creditsEarned: Int = 0,
    val creditsSpent: Int = 0,
) {
    val creditBalance: Int get() = creditsEarned - creditsSpent
}

data class LeaderboardEntry(
    val userId: Long,
    val name: String,
    val isDemo: Boolean,
    val savedKg: Double,
    val distanceKm: Double,
    val tripCount: Int,
)

data class MartItem(
    val id: String,
    val title: String,
    val emoji: String,
    val cost: Int,
    val partner: String,
)

data class Redemption(
    val id: Long,
    val itemId: String,
    val title: String,
    val cost: Int,
    val code: String,
    val time: Long,
)

/** Rewards offered in the in-app mart. Partners are placeholders until real vendors sign on. */
object MartCatalog {
    val items = listOf(
        MartItem("coffee", "Coffee voucher", "☕", 150, "Partner café"),
        MartItem("bottle", "Reusable water bottle", "🧴", 400, "Eco store"),
        MartItem("transit", "10% off a transit pass", "🎫", 600, "City transit"),
        MartItem("tree", "Plant a tree in your name", "🌳", 800, "Reforestation NGO"),
        MartItem("bike_service", "Free bike tune-up", "🔧", 1000, "Local bike shop"),
    )

    fun byId(id: String): MartItem? = items.firstOrNull { it.id == id }
}
