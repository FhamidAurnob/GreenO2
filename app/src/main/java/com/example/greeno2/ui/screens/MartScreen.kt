package com.example.greeno2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.MartCatalog
import com.example.greeno2.data.MartItem
import com.example.greeno2.data.Redemption
import com.example.greeno2.data.RewardCategory
import com.example.greeno2.data.GreenRepository
import com.example.greeno2.domain.SustainabilityQuiz
import com.example.greeno2.ui.components.EmojiBadge
import com.example.greeno2.ui.components.GradientButton
import com.example.greeno2.ui.components.GradientCard
import com.example.greeno2.ui.components.ProgressTrack
import com.example.greeno2.ui.components.SectionCard
import com.example.greeno2.ui.components.SectionHeader
import com.example.greeno2.ui.components.TextTabs
import com.example.greeno2.ui.components.fmtDate
import com.example.greeno2.ui.theme.Brand

/** "Rewards & Achievements" from the case study: spend Green Points on partner rewards. */
@Composable
fun RewardsScreen(
    balance: Int,
    redemptions: List<Redemption>,
    guideSeen: Boolean,
    onDismissGuide: () -> Unit,
    onRedeem: (MartItem) -> Unit,
) {
    var confirming by remember { mutableStateOf<MartItem?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme
    val tabs = listOf("All") + RewardCategory.entries.map { it.label }
    val items = if (tab == 0) MartCatalog.items else MartCatalog.items.filter { it.category == RewardCategory.entries[tab - 1] }
    val nextReward = MartCatalog.items.filter { it.cost > balance }.minByOrNull { it.cost }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rewards & Achievements", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { showGuide = true }) { Text("ⓘ How it works") }
            }
        }
        if (!guideSeen) {
            item { HowRewardsWorkCard(onGotIt = onDismissGuide) }
        }
        item {
            GradientCard {
                Text("Green Points wallet", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$balance", style = MaterialTheme.typography.displayMedium, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("points", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(
                    if (nextReward != null) "${nextReward.cost - balance} more to unlock ${nextReward.emoji} ${nextReward.title}"
                    else "You can unlock every reward. Treat yourself! 🎁",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        item {
            Column {
                Text("Save more with your rewards", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                TextTabs(tabs, tab, { tab = it })
            }
        }
        items(items, key = { it.id }) { item ->
            RewardCard(item, balance) { confirming = item }
        }
        if (redemptions.isNotEmpty()) {
            item { SectionHeader("Your vouchers", Modifier.padding(top = 8.dp), trailing = "${redemptions.size}") }
            items(redemptions, key = { "r${it.id}" }) { r -> VoucherCard(r) }
        }
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text("How Rewards Work") },
            text = { Column { HowRewardsWorkBody() } },
            confirmButton = { Button(onClick = { showGuide = false; onDismissGuide() }) { Text("Got it") } },
        )
    }

    confirming?.let { item ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            icon = { Text(item.emoji, style = MaterialTheme.typography.displaySmall) },
            title = { Text("Redeem ${item.title}?") },
            text = { Text("This uses ${item.cost} of your $balance Green Points. You'll get a voucher code to show at ${item.partner.lowercase()}.") },
            confirmButton = { Button(onClick = { onRedeem(item); confirming = null }) { Text("Redeem") } },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RewardCard(item: MartItem, balance: Int, onRedeem: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val affordable = balance >= item.cost
    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(item.partner, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            EmojiBadge(item.emoji, container = scheme.surfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⭐ ${item.cost} points", style = MaterialTheme.typography.labelLarge, color = scheme.tertiary, modifier = Modifier.weight(1f))
            TextButton(onClick = onRedeem, enabled = affordable) {
                Text(if (affordable) "Use now" else "${item.cost - balance} to go", fontWeight = FontWeight.Bold)
            }
        }
        if (!affordable) {
            ProgressTrack(balance.toFloat() / item.cost, height = 5.dp, fill = Brand.gold)
        }
    }
}

@Composable
private fun VoucherCard(r: Redemption) {
    val scheme = MaterialTheme.colorScheme
    val emoji = MartCatalog.byId(r.itemId)?.emoji ?: "🎟️"
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        border = BorderStroke(1.5.dp, scheme.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.title, style = MaterialTheme.typography.titleSmall)
                Text(r.code, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, color = scheme.primary)
                Text("${fmtDate(r.time)} · ${r.cost} points", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

/** The short guide added after usability testing showed people were confused by rewards. */
@Composable
fun HowRewardsWorkCard(onGotIt: () -> Unit) {
    SectionCard {
        Text("How Rewards Work", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        HowRewardsWorkBody()
        Spacer(Modifier.height(12.dp))
        GradientButton("Got it", onGotIt)
    }
}

@Composable
private fun HowRewardsWorkBody() {
    val scheme = MaterialTheme.colorScheme
    val steps = listOf(
        "🚶" to "Earn Green Points by tracking eco-friendly trips: walking, cycling, e-scooters, buses and trains. Every 100 g of CO₂ you avoid compared with driving = 1 point.",
        "🎯" to "Boost your points: +${SustainabilityQuiz.WELCOME_BONUS} for the welcome quiz, plus bonus points for completing weekly challenges in Community.",
        "🎁" to "Redeem points here for discounts on eco-stays, transport, food and activities from green partners.",
        "🛡️" to "Fair play: trips that are too short or too fast for the chosen mode earn nothing, and trip points are capped at ${GreenRepository.DAILY_CREDIT_CAP} a day.",
    )
    steps.forEach { (icon, text) ->
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
    Text(
        "Start small, make a big impact, and get rewarded for going green!",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = scheme.primary,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = 6.dp),
    )
}
