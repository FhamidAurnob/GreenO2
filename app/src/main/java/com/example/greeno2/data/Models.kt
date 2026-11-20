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
    val quizState: QuizState = QuizState.NOT_SEEN,
    val ecoScore: Int = 0,
    val preferredMode: TransportMode? = null,
)

/** Onboarding quiz progress for a user. Stored as its ordinal. */
enum class QuizState { NOT_SEEN, SKIPPED, COMPLETED;
    companion object {
        fun of(value: Int): QuizState = entries.getOrElse(value) { NOT_SEEN }
    }
}

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
    /** Green Points from trips plus bonuses (quiz, challenges). */
    val creditsEarned: Int = 0,
    val creditsSpent: Int = 0,
    val bonusPoints: Int = 0,
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

enum class RewardCategory(val label: String) {
    TRANSPORT("Transport"),
    ECO_STAYS("Eco-stays"),
    FOOD("Food & drink"),
    ACTIVITIES("Activities"),
}

data class MartItem(
    val id: String,
    val title: String,
    val emoji: String,
    val cost: Int,
    val partner: String,
    val category: RewardCategory,
    val description: String,
)

data class Redemption(
    val id: Long,
    val itemId: String,
    val title: String,
    val cost: Int,
    val code: String,
    val time: Long,
)

/**
 * Rewards that Green Points can be redeemed for, grouped like the design's
 * Eco-stays / Restaurants / Activities tabs. Partners are placeholders until real vendors sign on.
 */
object MartCatalog {
    val items = listOf(
        MartItem("transit", "10% off a transit pass", "🎫", 600, "City transit", RewardCategory.TRANSPORT,
            "Applies to your next monthly bus or train pass."),
        MartItem("ecotaxi", "15% off an eco-taxi ride", "🚕", 350, "Electric taxi partner", RewardCategory.TRANSPORT,
            "Valid on any electric or hybrid taxi booked through our partner."),
        MartItem("bike_service", "Free bike tune-up", "🔧", 1000, "Local bike shop", RewardCategory.TRANSPORT,
            "Brakes, gears and tyres checked so every ride is easier."),
        MartItem("ecostay", "15% off an eco-certified stay", "🏡", 1200, "Green hotels network", RewardCategory.ECO_STAYS,
            "Applies to one booking at a certified sustainable hotel or hostel."),
        MartItem("camping", "Free night at an eco-campsite", "⛺", 1500, "Eco campsites", RewardCategory.ECO_STAYS,
            "One night for two at a low-impact campsite."),
        MartItem("coffee", "Coffee in your own cup", "☕", 150, "Partner café", RewardCategory.FOOD,
            "A free coffee when you bring a reusable cup."),
        MartItem("plantmeal", "20% off a plant-based meal", "🥗", 300, "Green kitchen", RewardCategory.FOOD,
            "Valid at partner restaurants serving local, plant-based food."),
        MartItem("bottle", "Reusable water bottle", "🧴", 400, "Eco store", RewardCategory.ACTIVITIES,
            "Say goodbye to single-use plastic on your journeys."),
        MartItem("tree", "Plant a tree in your name", "🌳", 800, "Reforestation NGO", RewardCategory.ACTIVITIES,
            "We plant a native tree and send you its location."),
        MartItem("tour", "Guided nature walk", "🥾", 900, "Local guides", RewardCategory.ACTIVITIES,
            "A small-group walk with a local conservation guide."),
    )

    fun byId(id: String): MartItem? = items.firstOrNull { it.id == id }
}
