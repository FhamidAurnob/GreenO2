package com.example.greeno2.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.greeno2.appContainer
import com.example.greeno2.data.LeaderboardEntry
import com.example.greeno2.data.MartItem
import com.example.greeno2.data.Redemption
import com.example.greeno2.data.Trip
import com.example.greeno2.data.User
import com.example.greeno2.data.UserStats
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
            _ui.update { it.copy(loading = false, user = user) }
            reload()
        }
        // Any write (trip saved by the service, redemption, ...) triggers a refresh.
        viewModelScope.launch { repository.changes.collect { reload() } }
    }

    private suspend fun reload() {
        val user = _ui.value.user ?: return
        val stats = repository.stats(user.id)
        val trips = repository.trips(user.id)
        val board = repository.leaderboard()
        val redemptions = repository.redemptions(user.id)
        _ui.update { it.copy(stats = stats, trips = trips, leaderboard = board, redemptions = redemptions) }
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
                    _ui.update { it.copy(authBusy = false, user = user) }
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

    fun showMessage(text: String) = _ui.update { it.copy(message = text) }
    fun clearMessage() = _ui.update { it.copy(message = null) }
}
