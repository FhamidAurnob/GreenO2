package com.example.greeno2.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.greeno2.appContainer
import com.example.greeno2.data.LeaderboardEntry
import com.example.greeno2.data.MartItem
import com.example.greeno2.data.QuizState
import com.example.greeno2.data.Redemption
import com.example.greeno2.data.Trip
import com.example.greeno2.data.User
import com.example.greeno2.data.UserStats
import com.example.greeno2.domain.ChallengeProgress
import com.example.greeno2.domain.Challenges
import com.example.greeno2.domain.QuizResult
import com.example.greeno2.domain.SustainabilityQuiz
import com.example.greeno2.tracking.TrackingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = true,
    val authBusy: Boolean = false,
    val authError: String? = null,
    val user: User? = null,
    val stats: UserStats = UserStats(),
    val trips: List<Trip> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
    val challenges: List<ChallengeProgress> = emptyList(),
    val joinedChallenges: Set<String> = emptySet(),
    val claimedChallenges: Set<String> = emptySet(),
    val rewardsGuideSeen: Boolean = true,
    /** Set while the onboarding quiz screen is open (first run or "Retake quiz"). */
    val showQuiz: Boolean = false,
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val repository = container.repository
    private val session = container.session

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val liveTrip = TrackingStore.live
    val saving = TrackingStore.saving
    val lastCompleted = TrackingStore.lastCompleted

    init {
        viewModelScope.launch {
            val user = session.userId?.let { repository.getUser(it) }
            if (user == null) session.userId = null
            // Keep the branded splash visible briefly so the launch feels intentional.
            kotlinx.coroutines.delay(700)
            _ui.update { it.copy(loading = false, user = user, showQuiz = user?.quizState == QuizState.NOT_SEEN) }
            reload()
        }
        // Any write (trip saved by the service, redemption, ...) triggers a refresh.
        viewModelScope.launch { repository.changes.collect { reload() } }
    }

    private suspend fun reload() {
        val current = _ui.value.user ?: return
        val user = repository.getUser(current.id) ?: current
        val stats = repository.stats(user.id)
        val trips = repository.trips(user.id)
        val board = repository.leaderboard()
        val redemptions = repository.redemptions(user.id)
        val period = Challenges.periodKey()
        val joined = repository.joinedChallenges(user.id, period)
        val claimed = repository.claimedChallenges(user.id, period)
        _ui.update {
            // Ignore results if the user logged out (or switched account) while we were loading.
            if (it.user?.id != user.id) return@update it
            it.copy(
                user = user,
                stats = stats,
                trips = trips,
                leaderboard = board,
                redemptions = redemptions,
                challenges = Challenges.progress(trips),
                joinedChallenges = joined,
                claimedChallenges = claimed,
                rewardsGuideSeen = session.rewardsGuideSeen(user.id),
            )
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _ui.update { it.copy(authError = "Enter your email and password") }
            return
        }
        runAuth { repository.login(email, password) }
    }

    fun register(name: String, email: String, password: String) {
        val error = when {
            name.isBlank() -> "Enter your name"
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        if (error != null) {
            _ui.update { it.copy(authError = error) }
            return
        }
        runAuth { repository.register(name, email, password) }
    }

    private fun runAuth(block: suspend () -> Result<User>) {
        viewModelScope.launch {
            _ui.update { it.copy(authBusy = true, authError = null) }
            block().fold(
                onSuccess = { user ->
                    session.userId = user.id
                    _ui.update {
                        it.copy(authBusy = false, user = user, showQuiz = user.quizState == QuizState.NOT_SEEN)
                    }
                    reload()
                },
                onFailure = { e -> _ui.update { it.copy(authBusy = false, authError = e.message) } },
            )
        }
    }

    fun clearAuthError() = _ui.update { it.copy(authError = null) }

    fun logout() {
        session.userId = null
        TrackingStore.dismissLastCompleted()
        _ui.value = UiState(loading = false)
    }

    fun updateName(name: String) {
        val user = _ui.value.user ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateName(user.id, name)
            _ui.update { it.copy(user = user.copy(name = name.trim())) }
        }
    }

    fun redeem(item: MartItem) {
        val user = _ui.value.user ?: return
        viewModelScope.launch {
            repository.redeem(user.id, item).fold(
                onSuccess = { showMessage("Redeemed ${it.title}. Your code: ${it.code}") },
                onFailure = { showMessage(it.message ?: "Could not redeem this reward") },
            )
        }
    }

    fun dismissLastTrip() = TrackingStore.dismissLastCompleted()

    // ---------- Onboarding quiz ----------

    fun openQuiz() = _ui.update { it.copy(showQuiz = true) }

    fun skipQuiz() {
        val user = _ui.value.user ?: return
        _ui.update { it.copy(showQuiz = false) }
        viewModelScope.launch { repository.skipQuiz(user.id) }
    }

    /** Saves the result; the quiz screen shows its own result page and then calls [closeQuiz]. */
    fun submitQuiz(result: QuizResult, onSaved: (awarded: Int) -> Unit) {
        val user = _ui.value.user ?: return
        viewModelScope.launch {
            val awarded = repository.completeQuiz(user.id, result.score, result.preferredMode, SustainabilityQuiz.WELCOME_BONUS)
            onSaved(awarded)
        }
    }

    fun closeQuiz() = _ui.update { it.copy(showQuiz = false) }

    // ---------- Challenges & rewards ----------

    fun joinChallenge(id: String) {
        val user = _ui.value.user ?: return
        viewModelScope.launch {
            repository.joinChallenge(user.id, id, Challenges.periodKey())
            showMessage("Challenge joined. Good luck! 🌿")
        }
    }

    fun claimChallenge(id: String) {
        val user = _ui.value.user ?: return
        val progress = _ui.value.challenges.firstOrNull { it.challenge.id == id } ?: return
        if (!progress.complete || id !in _ui.value.joinedChallenges) return
        viewModelScope.launch {
            val ok = repository.claimChallenge(user.id, id, Challenges.periodKey(), progress.challenge.points)
            showMessage(if (ok) "+${progress.challenge.points} Green Points earned! 🎉" else "Already claimed this week")
        }
    }

    fun dismissRewardsGuide() {
        val user = _ui.value.user ?: return
        session.setRewardsGuideSeen(user.id)
        _ui.update { it.copy(rewardsGuideSeen = true) }
    }

    fun showMessage(text: String) = _ui.update { it.copy(message = text) }
    fun clearMessage() = _ui.update { it.copy(message = null) }
}
