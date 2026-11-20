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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.LeaderboardEntry
import com.example.greeno2.domain.ChallengeProgress
import com.example.greeno2.ui.UiState
import com.example.greeno2.ui.components.Avatar
import com.example.greeno2.ui.components.GradientCard
import com.example.greeno2.ui.components.Pill
import com.example.greeno2.ui.components.ProgressTrack
import com.example.greeno2.ui.components.SectionCard
import com.example.greeno2.ui.components.TextTabs
import com.example.greeno2.ui.components.fmtKg
import com.example.greeno2.ui.components.fmtKm
import com.example.greeno2.ui.components.shareText
import java.util.Locale

private val GoldC = Color(0xFFE2BE72)
private val SilverC = Color(0xFFD5DBD0)
private val BronzeC = Color(0xFFC9946A)

/** Community & Challenges + User Impact Leaderboard from the case study's information architecture. */
@Composable
fun CommunityScreen(
    ui: UiState,
    onJoin: (String) -> Unit,
    onClaim: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val currentUserId = ui.user?.id ?: -1L
    val entries = ui.leaderboard
    val myRank = entries.indexOfFirst { it.userId == currentUserId }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Community", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Join challenges, compete with the community and share your impact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                TextTabs(listOf("Challenges", "Leaderboard"), tab, { tab = it })
            }
        }

        if (tab == 0) {
            item {
                Text(
                    "This week's challenges · resets every Monday",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(ui.challenges, key = { it.challenge.id }) { p ->
                ChallengeCard(
                    progress = p,
                    joined = p.challenge.id in ui.joinedChallenges,
                    claimed = p.challenge.id in ui.claimedChallenges,
                    onJoin = { onJoin(p.challenge.id) },
                    onClaim = { onClaim(p.challenge.id) },
                    onShare = {
                        shareText(
                            context,
                            "I just completed the \"${p.challenge.title}\" challenge on GreenO2 ${p.challenge.emoji} " +
                                "and I've saved ${fmtKg(ui.stats.savedKg)} of CO₂ so far. Join me in making every trip greener! 🌿",
                        )
                    },
                )
            }
        } else {
            if (entries.isNotEmpty()) item { Podium(entries.take(3), currentUserId) }
            if (myRank >= 0) {
                item {
                    val me = entries[myRank]
                    val ahead = entries.getOrNull(myRank - 1)
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${myRank + 1}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                if (ahead == null) "You're leading the community. Keep it up! 🏆"
                                else "Save ${fmtKg((ahead.savedKg - me.savedKg).coerceAtLeast(0.001))} more to pass ${ahead.name}.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            itemsIndexed(entries.drop(3), key = { _, e -> e.userId }) { index, entry ->
                RankRow(index + 4, entry, entry.userId == currentUserId)
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    progress: ChallengeProgress,
    joined: Boolean,
    claimed: Boolean,
    onJoin: () -> Unit,
    onClaim: () -> Unit,
    onShare: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val c = progress.challenge
    val currentText = if (c.unit == "km") String.format(Locale.US, "%.1f", progress.current) else progress.current.toInt().toString()
    val targetText = if (c.unit == "km") String.format(Locale.US, "%.0f", c.target) else c.target.toInt().toString()

    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Text(c.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, style = MaterialTheme.typography.titleMedium)
                Text(c.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Pill("+${c.points}", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        }
        Spacer(Modifier.height(12.dp))
        ProgressTrack(progress.fraction, height = 7.dp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$currentText / $targetText ${c.unit}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            when {
                claimed -> TextButton(onClick = onShare) { Text("Claimed ✓ · Share") }
                joined && progress.complete -> Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.tertiary),
                ) { Text("Claim +${c.points}") }
                joined -> Text("Joined ✓", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                else -> OutlinedButton(onClick = onJoin) { Text("Join") }
            }
        }
    }
}

@Composable
private fun Podium(top: List<LeaderboardEntry>, currentUserId: Long) {
    GradientCard {
        Text("Top savers", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(1, 0, 2).forEach { i ->
                val entry = top.getOrNull(i)
                Box(Modifier.weight(1f)) {
                    if (entry != null) {
                        PodiumColumn(
                            rank = i + 1,
                            entry = entry,
                            isMe = entry.userId == currentUserId,
                            height = when (i) { 0 -> 92.dp; 1 -> 68.dp; else -> 50.dp },
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
        Avatar(entry.name, if (rank == 1) 62.dp else 50.dp, ring = medal)
        Spacer(Modifier.height(6.dp))
        Text(
            if (isMe) "You" else entry.name,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(fmtKg(entry.savedKg), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                "$rank",
                color = medal,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun RankRow(rank: Int, entry: LeaderboardEntry, isMe: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isMe) scheme.primaryContainer else scheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outline),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 40.dp))
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
                Text("${entry.tripCount} trips · ${fmtKm(entry.distanceKm)}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Text(fmtKg(entry.savedKg), style = MaterialTheme.typography.titleMedium, color = scheme.primary)
        }
    }
}
