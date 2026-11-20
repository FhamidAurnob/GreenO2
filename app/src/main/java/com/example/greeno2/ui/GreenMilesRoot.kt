package com.example.greeno2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.greeno2.ui.screens.CommunityScreen
import com.example.greeno2.ui.screens.OnboardingScreen
import com.example.greeno2.ui.screens.RewardsScreen
import com.example.greeno2.ui.screens.SplashScreen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import com.example.greeno2.ui.screens.ProfileScreen

/** Bottom navigation, following the case study's Explore / Community / Leaderboard / Rewards / Profile. */
private enum class Tab(val label: String, val icon: Int) {
    TRACK("Track", R.drawable.ic_nav_leaf),
    HISTORY("Trips", R.drawable.ic_nav_history),
    COMMUNITY("Community", R.drawable.ic_nav_community),
    REWARDS("Rewards", R.drawable.ic_nav_gift),
    PROFILE("Profile", R.drawable.ic_nav_profile),
}

@Composable
fun GreenMilesRoot(vm: AppViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    val live by vm.liveTrip.collectAsState()
    val saving by vm.saving.collectAsState()
    val lastCompleted by vm.lastCompleted.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(Tab.TRACK.ordinal) }
    val user = ui.user

    when {
        ui.loading -> SplashScreen()
        user == null -> AuthScreen(
            ui = ui,
            onLogin = vm::login,
            onRegister = vm::register,
            onClearError = vm::clearAuthError,
        )
        ui.showQuiz -> OnboardingScreen(
            user = user,
            onSkip = vm::skipQuiz,
            onSubmit = vm::submitQuiz,
            onFinish = { openRewards ->
                vm.closeQuiz()
                if (openRewards) tab = Tab.REWARDS.ordinal
            },
        )
        else -> {
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
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t.ordinal,
                                    onClick = { tab = t.ordinal },
                                    icon = { Icon(painterResource(t.icon), contentDescription = t.label) },
                                    label = { Text(t.label, maxLines = 1, fontWeight = if (tab == t.ordinal) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
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
                            onTakeQuiz = vm::openQuiz,
                        )
                        Tab.HISTORY -> HistoryScreen(ui.trips)
                        Tab.COMMUNITY -> CommunityScreen(ui, vm::joinChallenge, vm::claimChallenge)
                        Tab.REWARDS -> RewardsScreen(
                            balance = ui.stats.creditBalance,
                            redemptions = ui.redemptions,
                            guideSeen = ui.rewardsGuideSeen,
                            onDismissGuide = vm::dismissRewardsGuide,
                            onRedeem = vm::redeem,
                        )
                        Tab.PROFILE -> ProfileScreen(
                            ui = ui,
                            trackingActive = live != null || saving,
                            onRename = vm::updateName,
                            onLogout = vm::logout,
                            onRetakeQuiz = vm::openQuiz,
                        )
                    }
                }
            }
        }
    }
}
