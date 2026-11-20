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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.example.greeno2.data.GreenRepository
import com.example.greeno2.data.QuizState
import com.example.greeno2.data.TransportMode
import com.example.greeno2.domain.SustainabilityQuiz
import com.example.greeno2.ui.components.OutlineButton
import com.example.greeno2.ui.components.RuledHeading
import com.example.greeno2.ui.components.shareText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    onRetakeQuiz: () -> Unit,
) {
    val context = LocalContext.current
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
                GlassStat(stats.creditBalance.toString(), "Green Points")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Member since ${fmtDate(user.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        val unlocked = achievements.count { it.unlocked }
        // "Achievements" headboard from the case study: congratulations + certificate + invite.
        RuledHeading("Achievements")
        SectionCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Congratulations!", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (unlocked > 0) "You earned $unlocked achievement${if (unlocked == 1) "" else "s"} since ${fmtDate(user.createdAt)}"
                    else "Record your first green trip to earn your first achievement",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        CertificateCard(
            name = user.name,
            level = level,
            savedKg = stats.savedKg,
            onShare = {
                shareText(
                    context,
                    "🌿 I've reached ${level.emoji} ${level.title} on GreenO2 and saved ${fmtKg(stats.savedKg)} of CO₂ " +
                        "by choosing greener ways to travel. Every trip counts!",
                )
            },
        )
        InviteCard(onInvite = {
            shareText(
                context,
                "Help the planet, help your wallet! 🌍 I'm using GreenO2 to track my low-carbon trips and earn " +
                    "Green Points for eco rewards. Join me with my invite code ${inviteCode(user.id)}.",
                "Invite friends",
            )
        })

        SectionCard {
            SectionHeader("Badges", trailing = "$unlocked/${achievements.size}")
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
            KeyValueRow("Green Points earned", stats.creditsEarned.toString())
            if (stats.bonusPoints > 0) KeyValueRow("  of which bonuses", stats.bonusPoints.toString())
            KeyValueRow("Green Points spent", stats.creditsSpent.toString())
            KeyValueRow("Balance", stats.creditBalance.toString())
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
            SectionHeader("Sustainability profile")
            Spacer(Modifier.height(6.dp))
            if (user.quizState == QuizState.COMPLETED) {
                KeyValueRow("Quiz score", "${user.ecoScore} / 100")
                user.preferredMode?.let { KeyValueRow("Default travel mode", "${it.emoji} ${it.label}") }
            } else {
                Text(
                    "You haven't taken the sustainability quiz yet. It personalises your default travel " +
                        "mode and gives you +${SustainabilityQuiz.WELCOME_BONUS} Green Points.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlineButton(if (user.quizState == QuizState.COMPLETED) "Retake the quiz" else "Take the quiz", onRetakeQuiz)
        }

        SectionCard {
            SectionHeader("Help & FAQ")
            Spacer(Modifier.height(4.dp))
            Faq.entries.forEach { FaqItem(it.first, it.second) }
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


@Composable
private fun CertificateCard(name: String, level: EcoLevel, savedKg: Double, onShare: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SectionCard {
        RuledHeading("Certificate")
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("This certificate is proudly presented to", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "for reaching ${level.emoji} ${level.title} and saving ${fmtKg(savedKg)} of CO₂",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text("Scope date", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text("Signed by", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Text("The GreenO2 Team", fontFamily = FontFamily.Cursive, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onShare) { Text("Share my certificate") }
        }
    }
}

@Composable
private fun InviteCard(onInvite: () -> Unit) {
    SectionCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Help the Planet, Help Your Wallet", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text("Invite & Earn!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Share GreenO2 with friends. The more people travel green, the bigger our community impact.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onInvite) { Text("✉️  Invite friends") }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(question, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(if (open) "−" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(open) {
            Text(
                answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

private object Faq {
    val entries = listOf(
        "How do I earn Green Points?" to
            "Track trips by walking, cycling, e-bike, e-scooter, bus or train. You earn 1 point for every 100 g of CO₂ " +
            "you avoid compared with driving. The welcome quiz and weekly challenges give bonus points.",
        "Why did my trip earn 0 points?" to
            "Trips under 100 m or 1 minute don't earn points, and trips that are too fast for the mode you chose " +
            "(for example \"walking\" at 40 km/h) are flagged. Trip points are also capped at " +
            "${GreenRepository.DAILY_CREDIT_CAP} a day.",
        "How is my CO₂ saving calculated?" to
            "We compare your trip with driving the same distance in an average car (0.17 kg CO₂ per km), using " +
            "estimates based on UK government conversion factors. See the table below.",
        "Is my location shared?" to
            "No. GPS is only used while a trip is recording, and your route and history are stored on this phone.",
        "How do I use a reward?" to
            "Redeem it in Rewards and show the voucher code to the partner. Your codes are listed under Your vouchers.",
    )
}

/** Short, stable invite code derived from the user id. */
private fun inviteCode(userId: Long): String = "GO2-" + (userId * 7919 + 1000).toString(36).uppercase()
