package com.example.greeno2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.greeno2.R
import com.example.greeno2.ui.screens.AuthScreen
import com.example.greeno2.ui.screens.HistoryScreen
import com.example.greeno2.ui.screens.HomeScreen
import com.example.greeno2.ui.screens.LeaderboardScreen
import com.example.greeno2.ui.screens.MartScreen
import com.example.greeno2.ui.screens.ProfileScreen

private enum class Tab(val label: String, val icon: Int) {
    TRACK("Track", R.drawable.ic_nav_track),
    HISTORY("Trips", R.drawable.ic_nav_history),
    RANKS("Ranks", R.drawable.ic_nav_rank),
    MART("Mart", R.drawable.ic_nav_mart),
    PROFILE("Profile", R.drawable.ic_nav_profile),
}

@Composable
fun GreenMilesRoot(vm: AppViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val live by vm.liveTrip.collectAsState()
    val saving by vm.saving.collectAsState()
    val lastCompleted by vm.lastCompleted.collectAsState()

    when {
        ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ui.user == null -> AuthScreen(
            ui = ui,
            onLogin = vm::login,
            onRegister = vm::register,
            onClearError = vm::clearAuthError,
        )
        else -> {
            var tab by rememberSaveable { mutableIntStateOf(Tab.TRACK.ordinal) }
            val snackbar = remember { SnackbarHostState() }
            LaunchedEffect(ui.message) {
                val msg = ui.message ?: return@LaunchedEffect
                snackbar.showSnackbar(msg)
                vm.clearMessage()
            }
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t.ordinal,
                                onClick = { tab = t.ordinal },
                                icon = { Icon(painterResource(t.icon), contentDescription = t.label) },
                                label = { Text(t.label, fontWeight = if (tab == t.ordinal) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (Tab.entries[tab]) {
                        Tab.TRACK -> HomeScreen(
                            ui = ui,
                            live = live,
                            saving = saving,
                            lastCompleted = lastCompleted,
                            onDismissLast = vm::dismissLastTrip,
                            onMessage = vm::showMessage,
                        )
                        Tab.HISTORY -> HistoryScreen(ui.trips)
                        Tab.RANKS -> LeaderboardScreen(ui.leaderboard, ui.user?.id ?: -1L)
                        Tab.MART -> MartScreen(ui.stats.creditBalance, ui.redemptions, vm::redeem)
                        Tab.PROFILE -> ProfileScreen(
                            ui = ui,
                            trackingActive = live != null || saving,
                            onRename = vm::updateName,
                            onLogout = vm::logout,
                        )
                    }
                }
            }
        }
    }
}
