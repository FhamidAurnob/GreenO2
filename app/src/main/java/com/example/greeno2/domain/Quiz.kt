package com.example.greeno2.domain

import com.example.greeno2.data.TransportMode

/**
 * The 5-question "Quiz for Personalized Sustainable Travel" from the GreenMiles UX case study.
 * Answers produce a 0–100 sustainability score, a preferred transport mode for the Track screen,
 * and a one-time Green Points welcome bonus.
 */
data class QuizOption(
    val label: String,
    val emoji: String,
    val points: Int,
    /** Mode this answer implies (used to preselect the Track screen). */
    val mode: TransportMode? = null,
    /** For multi-select questions: picking this clears the others ("Not yet..."). */
    val exclusive: Boolean = false,
)

data class QuizQuestion(
    val icon: String,
    val text: String,
    val options: List<QuizOption>,
    val multiSelect: Boolean = false,
    val hint: String? = null,
)

data class QuizResult(
    val score: Int,
    val preferredMode: TransportMode?,
    val headline: String,
    val advice: String,
)

object SustainabilityQuiz {
    const val WELCOME_BONUS = 50

    val questions = listOf(
        QuizQuestion(
            "🚆", "How do you get around most days?",
            listOf(
                QuizOption("Walking", "🚶", 20, TransportMode.WALK),
                QuizOption("Bicycle or e-bike", "🚲", 20, TransportMode.BIKE),
                QuizOption("Public transport", "🚌", 15, TransportMode.BUS),
                QuizOption("Train", "🚆", 15, TransportMode.TRAIN),
                QuizOption("Private car", "🚗", 0, TransportMode.CAR),
            ),
        ),
        QuizQuestion(
            "⛺", "How do you typically pick your lodging when you travel?",
            listOf(
                QuizOption("Green accommodation (hotels/hostels)", "💚", 20),
                QuizOption("Airbnb with sustainability initiatives", "🏡", 15),
                QuizOption("Camping / glamping", "⛺", 15),
                QuizOption("Standard hotels / resorts", "🏨", 5),
                QuizOption("Cruises or all-inclusive stays", "🚢", 0),
            ),
        ),
        QuizQuestion(
            "🥗", "What do you eat while traveling?",
            listOf(
                QuizOption("Plant-based / vegan", "🌱", 20),
                QuizOption("Partly vegetarian with a little meat", "🍗", 15),
                QuizOption("Locally sourced food", "🐟", 15),
                QuizOption("Fast food / ready-made meals", "🍔", 5),
                QuizOption("No specific preference", "🍽️", 5),
            ),
        ),
        QuizQuestion(
            "🚲", "How do you usually get around at your destination?",
            listOf(
                QuizOption("Bicycle or walk", "🚲", 20, TransportMode.BIKE),
                QuizOption("Public transport", "🚌", 15, TransportMode.BUS),
                QuizOption("Ridesharing (Uber, Pathao…)", "🚕", 5, TransportMode.CAR),
                QuizOption("Rental car", "🚗", 0, TransportMode.CAR),
                QuizOption("Motorbike / scooter", "🛵", 5, TransportMode.E_SCOOTER),
            ),
        ),
        QuizQuestion(
            "🌍", "What do you do sustainably when you travel?",
            listOf(
                QuizOption("Bring a reusable water bottle", "🧴", 5),
                QuizOption("Avoid single-use plastics", "🛍️", 5),
                QuizOption("Get involved in environmental efforts", "🤝", 5),
                QuizOption("Offset carbon emissions", "🌳", 5),
                QuizOption("Not yet, but I'd love to learn more!", "💡", 0, exclusive = true),
            ),
            multiSelect = true,
            hint = "Select all that apply",
        ),
    )

    /** Toggles [index] in [current] for [question], honouring single/multi/exclusive rules. */
    fun toggle(question: QuizQuestion, current: Set<Int>, index: Int): Set<Int> {
        if (!question.multiSelect) return setOf(index)
        val option = question.options[index]
        return when {
            index in current -> current - index
            option.exclusive -> setOf(index)
            else -> current.filterNot { question.options[it].exclusive }.toSet() + index
        }
    }

    /** [answers] holds the selected option indexes for each question, in order. */
    fun score(answers: List<Set<Int>>): QuizResult {
        var total = 0
        questions.forEachIndexed { q, question ->
            val picked = answers.getOrNull(q).orEmpty()
            total += picked.sumOf { question.options.getOrNull(it)?.points ?: 0 }
        }
        total = total.coerceIn(0, 100)

        // Everyday travel (Q1) decides the default mode; fall back to Q4.
        val mode = answers.getOrNull(0)?.firstOrNull()?.let { questions[0].options.getOrNull(it)?.mode }
            ?: answers.getOrNull(3)?.firstOrNull()?.let { questions[3].options.getOrNull(it)?.mode }

        val (headline, advice) = when {
            total >= 80 -> "Eco champion 🌲" to "You already live green. Track your trips to prove it and climb the leaderboard."
            total >= 55 -> "On the right track 🌳" to "Great habits! Swap one or two short car rides a week for walking or cycling to level up."
            total >= 30 -> "Growing green 🌿" to "There's lots of room to save. Start by tracking a walk or bus trip today."
            else -> "Just getting started 🌱" to "Every journey is a chance to save CO₂. Try one low-carbon trip this week and watch your points grow."
        }
        return QuizResult(total, mode, headline, advice)
    }
}
