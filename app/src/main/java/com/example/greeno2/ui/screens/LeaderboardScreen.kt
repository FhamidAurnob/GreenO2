package com.example.greeno2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.LeaderboardEntry
import com.example.greeno2.ui.components.Avatar
import com.example.greeno2.ui.components.GradientCard
import com.example.greeno2.ui.components.Pill
import com.example.greeno2.ui.components.ScreenTitle
import com.example.greeno2.ui.components.fmtKg
import com.example.greeno2.ui.components.fmtKm

private val GoldC = Color(0xFFFFC93C)
private val SilverC = Color(0xFFCFD8DC)
private val BronzeC = Color(0xFFE0A06B)

@Composable
fun LeaderboardScreen(entries: List<LeaderboardEntry>, currentUserId: Long) {
    val myRank = entries.indexOfFirst { it.userId == currentUserId }
    val podium = entries.take(3)
    val rest = entries.drop(3)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("Community", "Ranked by CO₂ saved. Flagged trips don't count.") }
        if (podium.isNotEmpty()) {
            item { Podium(podium, currentUserId) }
        }
        if (myRank >= 0) {
            item {
                val me = entries[myRank]
                val ahead = entries.getOrNull(myRank - 1)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${myRank + 1}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            if (ahead == null) "You're leading the community. Keep it up! 🏆"
                            else "Save ${fmtKg((ahead.savedKg - me.savedKg).coerceAtLeast(0.001))} more to pass ${ahead.name}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        itemsIndexed(rest, key = { _, e -> e.userId }) { index, entry ->
            RankRow(index + 4, entry, entry.userId == currentUserId)
        }
    }
}

@Composable
private fun Podium(top: List<LeaderboardEntry>, currentUserId: Long) {
    GradientCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Visual order: 2nd, 1st, 3rd
            val order = listOf(1, 0, 2)
            order.forEach { i ->
                val entry = top.getOrNull(i)
                Box(Modifier.weight(1f)) {
                    if (entry != null) {
                        PodiumColumn(
                            rank = i + 1,
                            entry = entry,
                            isMe = entry.userId == currentUserId,
                            height = when (i) { 0 -> 96.dp; 1 -> 70.dp; else -> 52.dp },
                            medal = when (i) { 0 -> GoldC; 1 -> SilverC; else -> BronzeC },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(rank: Int, entry: LeaderboardEntry, isMe: Boolean, height: Dp, medal: Color) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (rank == 1) Text("👑", style = MaterialTheme.typography.titleLarge)
        Avatar(entry.name, if (rank == 1) 64.dp else 52.dp, ring = medal)
        Spacer(Modifier.height(6.dp))
        Text(
            if (isMe) "You" else entry.name,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(fmtKg(entry.savedKg), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                "$rank",
                color = medal,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RankRow(rank: Int, entry: LeaderboardEntry, isMe: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isMe) scheme.primaryContainer else scheme.surface),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 40.dp),
            )
            Avatar(entry.name, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isMe) "${entry.name} (you)" else entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                    )
                    if (entry.isDemo) {
                        Spacer(Modifier.width(6.dp))
                        Pill("demo", scheme.surfaceVariant, scheme.onSurfaceVariant)
                    }
                }
                Text(
                    "${entry.tripCount} trips · ${fmtKm(entry.distanceKm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(fmtKg(entry.savedKg), style = MaterialTheme.typography.titleMedium, color = scheme.primary)
        }
    }
}
