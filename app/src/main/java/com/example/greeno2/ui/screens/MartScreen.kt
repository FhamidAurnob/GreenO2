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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.MartCatalog
import com.example.greeno2.data.MartItem
import com.example.greeno2.data.Redemption
import com.example.greeno2.ui.components.EmojiBadge
import com.example.greeno2.ui.components.GradientCard
import com.example.greeno2.ui.components.ProgressTrack
import com.example.greeno2.ui.components.ScreenTitle
import com.example.greeno2.ui.components.SectionCard
import com.example.greeno2.ui.components.SectionHeader
import com.example.greeno2.ui.components.fmtDate
import com.example.greeno2.ui.theme.Brand

@Composable
fun MartScreen(balance: Int, redemptions: List<Redemption>, onRedeem: (MartItem) -> Unit) {
    var confirming by remember { mutableStateOf<MartItem?>(null) }
    val scheme = MaterialTheme.colorScheme
    val cheapestLocked = MartCatalog.items.filter { it.cost > balance }.minByOrNull { it.cost }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Eco Mart", "Spend credits on rewards from green partners.") }
        item {
            GradientCard(brush = Brand.gold) {
                Text("Your balance", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$balance", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("credits", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(
                    if (cheapestLocked != null) "${cheapestLocked.cost - balance} more credits unlocks ${cheapestLocked.emoji} ${cheapestLocked.title}"
                    else "You can afford every reward. Treat yourself! 🎁",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(4.dp))
                Text("1 credit = 100 g of CO₂ avoided", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
            }
        }
        item { SectionHeader("Rewards", trailing = "${MartCatalog.items.size} available") }
        items(MartCatalog.items, key = { it.id }) { item ->
            val affordable = balance >= item.cost
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(item.emoji, container = scheme.tertiaryContainer)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.partner, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Button(onClick = { confirming = item }, enabled = affordable) { Text("⭐ ${item.cost}") }
                }
                if (!affordable) {
                    Spacer(Modifier.height(12.dp))
                    ProgressTrack(balance.toFloat() / item.cost, height = 6.dp, fill = Brand.gold)
                    Text(
                        "${item.cost - balance} credits to go",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (redemptions.isNotEmpty()) {
            item { SectionHeader("Your vouchers", Modifier.padding(top = 8.dp), trailing = "${redemptions.size}") }
            items(redemptions, key = { "r${it.id}" }) { r ->
                val emoji = MartCatalog.byId(r.itemId)?.emoji ?: "🎟️"
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.5.dp, scheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                r.code,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                            )
                            Text("${fmtDate(r.time)} · ${r.cost} credits", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    confirming?.let { item ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            icon = { Text(item.emoji, style = MaterialTheme.typography.displaySmall) },
            title = { Text("Redeem ${item.title}?") },
            text = { Text("This uses ${item.cost} of your $balance credits. You'll get a voucher code to show at ${item.partner.lowercase()}.") },
            confirmButton = {
                Button(onClick = { onRedeem(item); confirming = null }) { Text("Redeem") }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancel") } },
        )
    }
}
