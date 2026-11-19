package com.example.greeno2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.TransportMode
import com.example.greeno2.domain.Achievement
import com.example.greeno2.domain.Achievements
import com.example.greeno2.domain.CarbonCalculator
import com.example.greeno2.domain.EcoLevel
import com.example.greeno2.ui.UiState
import com.example.greeno2.ui.components.Avatar
import com.example.greeno2.ui.components.GlassStat
import com.example.greeno2.ui.components.GradientCard
import com.example.greeno2.ui.components.KeyValueRow
import com.example.greeno2.ui.components.SectionCard
import com.example.greeno2.ui.components.SectionHeader
import com.example.greeno2.ui.components.fmtDate
import com.example.greeno2.ui.components.fmtKg
import com.example.greeno2.ui.components.fmtKm
import java.util.Locale

@Composable
fun ProfileScreen(
    ui: UiState,
    trackingActive: Boolean,
    onRename: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val user = ui.user ?: return
    val stats = ui.stats
    val scheme = MaterialTheme.colorScheme
    val level = EcoLevel.of(stats.savedKg)
    val achievements = remember(ui.trips) { Achievements.evaluate(ui.trips) }
    var editing by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GradientCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(user.name, 68.dp, ring = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Surface(color = Color.White.copy(alpha = 0.18f), contentColor = Color.White, shape = RoundedCornerShape(50)) {
                        Text(
                            "${level.emoji}  ${level.title}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                TextButton(onClick = { editing = true }) { Text("Edit", color = Color.White) }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassStat(fmtKg(stats.savedKg), "CO₂ saved")
                GlassStat(stats.tripCount.toString(), "trips")
                GlassStat(stats.creditsEarned.toString(), "credits earned")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Member since ${fmtDate(user.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        SectionCard {
            SectionHeader("Achievements", trailing = "${achievements.count { it.unlocked }}/${achievements.size}")
            Spacer(Modifier.height(12.dp))
            achievements.chunked(4).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { AchievementBadge(it, Modifier.weight(1f)) }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        SectionCard {
            SectionHeader("Your impact")
            Spacer(Modifier.height(6.dp))
            KeyValueRow("Distance travelled", fmtKm(stats.distanceKm))
            KeyValueRow("CO₂ saved vs driving", fmtKg(stats.savedKg))
            KeyValueRow("CO₂ emitted", fmtKg(stats.emittedKg))
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = scheme.surfaceVariant)
            KeyValueRow("Credits earned", stats.creditsEarned.toString())
            KeyValueRow("Credits spent", stats.creditsSpent.toString())
            KeyValueRow("Credit balance", stats.creditBalance.toString())
            if (stats.savedKg > 0) {
                val treeDays = stats.savedKg / CarbonCalculator.TREE_KG_PER_YEAR * 365
                Spacer(Modifier.height(10.dp))
                Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "🌳 That's about ${String.format(Locale.US, "%.0f", treeDays)} days of CO₂ absorbed by one tree.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        SectionCard {
            SectionHeader("How we calculate")
            Text(
                "Savings compare your trip with driving the same distance in an average car " +
                    "(${fmtKg(CarbonCalculator.BASELINE_CAR_KG_PER_KM)} CO₂/km). Factors are estimates " +
                    "based on UK government conversion factors.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            TransportMode.entries.forEach { mode ->
                KeyValueRow("${mode.emoji}  ${mode.label}", "${fmtKg(mode.kgCo2PerKm)} / km")
            }
        }

        OutlinedButton(
            onClick = onLogout,
            enabled = !trackingActive,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
        ) { Text(if (trackingActive) "Stop your trip before logging out" else "Log out") }
        Spacer(Modifier.height(4.dp))
    }

    if (editing) {
        var name by remember { mutableStateOf(user.name) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit name") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = { onRename(name); editing = false }, enabled = name.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AchievementBadge(a: Achievement, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.alpha(if (a.unlocked) 1f else 0.45f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (a.unlocked) scheme.tertiaryContainer else scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (a.unlocked) a.emoji else "🔒", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            a.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            a.description,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
