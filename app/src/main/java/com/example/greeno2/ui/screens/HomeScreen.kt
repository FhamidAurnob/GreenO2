package com.example.greeno2.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import com.example.greeno2.data.QuizState
import com.example.greeno2.data.TransportMode
import com.example.greeno2.domain.SustainabilityQuiz
import com.example.greeno2.domain.TripPlanner
import com.example.greeno2.ui.components.OutlineButton
import kotlin.math.roundToInt
import com.example.greeno2.data.Trip
import com.example.greeno2.data.UserStats
import com.example.greeno2.domain.CarbonCalculator
import com.example.greeno2.domain.EcoLevel
import com.example.greeno2.domain.EcoTips
import com.example.greeno2.tracking.LiveTrip
import com.example.greeno2.tracking.TrackingService
import com.example.greeno2.ui.UiState
import com.example.greeno2.ui.components.AppLogo
import com.example.greeno2.ui.components.GlassStat
import com.example.greeno2.ui.components.GradientButton
import com.example.greeno2.ui.components.GradientCard
import com.example.greeno2.ui.components.Pill
import com.example.greeno2.ui.components.ProgressTrack
import com.example.greeno2.ui.components.RouteSketch
import com.example.greeno2.ui.components.SectionCard
import com.example.greeno2.ui.components.SectionHeader
import com.example.greeno2.ui.components.fmtDuration
import com.example.greeno2.ui.components.fmtKg
import com.example.greeno2.ui.components.fmtKm
import com.example.greeno2.ui.theme.Brand
import com.example.greeno2.ui.theme.Mint
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Locale

@Composable
fun HomeScreen(
    ui: UiState,
    live: LiveTrip?,
    saving: Boolean,
    lastCompleted: Trip?,
    onDismissLast: () -> Unit,
    onMessage: (String) -> Unit,
    onTakeQuiz: () -> Unit,
) {
    val context = LocalContext.current
    val preferred = ui.user?.preferredMode
    // Start on the mode recommended by the onboarding quiz, when there is one.
    var selectedMode by rememberSaveable(preferred) { mutableStateOf(preferred ?: TransportMode.WALK) }
    var showLocationInfo by remember { mutableStateOf(false) }
    val userId = ui.user?.id ?: -1L

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startIfLocationOn(context, selectedMode, userId, onMessage)
        } else {
            onMessage("Location access was declined. Allow precise location in Settings to track trips.")
        }
    }

    val onStartClicked: () -> Unit = {
        if (hasFineLocation(context)) {
            startIfLocationOn(context, selectedMode, userId, onMessage)
        } else {
            // Explain why before the system prompt ("Location Access" step in the design).
            showLocationInfo = true
        }
    }

    if (showLocationInfo) {
        AlertDialog(
            onDismissRequest = { showLocationInfo = false },
            icon = { Text("📍", style = MaterialTheme.typography.headlineMedium) },
            title = { Text("Allow location access") },
            text = {
                Text(
                    "GreenO2 uses your precise location only while a trip is recording, to measure the " +
                        "distance and calculate your CO₂ savings. Your route stays on this phone.\n\n" +
                        "We'll also ask to show a notification so you can stop the trip from your lock screen."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showLocationInfo = false
                    val perms = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showLocationInfo = false }) { Text("Not now") } },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(ui.user?.name.orEmpty(), ui.stats)

        if (ui.user?.quizState == QuizState.SKIPPED && live == null) {
            QuizPromptCard(onTakeQuiz)
        }

        when {
            saving -> SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("Calculating your carbon savings…", style = MaterialTheme.typography.bodyLarge)
                }
            }
            live != null -> LiveTripCard(live, onStop = { TrackingService.stop(context) })
            else -> ModePicker(selectedMode, { selectedMode = it }, onStartClicked)
        }

        if (lastCompleted != null && live == null && !saving) {
            TripSummaryCard(lastCompleted, onDismissLast)
        }

        if (live == null && !saving) {
            TripPlannerCard(onPick = { selectedMode = it })
            TipCard()
        }
    }
}

@Composable
private fun QuizPromptCard(onTakeQuiz: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📝", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Personalise GreenO2", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Take the 5-question quiz to get your sustainability score and " +
                        "+${SustainabilityQuiz.WELCOME_BONUS} Green Points.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlineButton("Take the quiz", onTakeQuiz)
    }
}

/**
 * Trip planner from the design: enter a distance, compare CO₂ for every mode in real time,
 * and get the greenest realistic alternative.
 */
@Composable
private fun TripPlannerCard(onPick: (TransportMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var km by rememberSaveable { mutableFloatStateOf(5f) }
    val options = TripPlanner.compare(km.toDouble())
    val suggestion = TripPlanner.suggest(km.toDouble())
    val carKg = options.first { it.mode == TransportMode.CAR }.emittedKg
    val maxKg = options.maxOf { it.emittedKg }.coerceAtLeast(0.0001)

    SectionCard {
        SectionHeader("Plan a trip", trailing = String.format(Locale.US, "%.1f km", km))
        Text(
            "Compare the footprint of each way to get there before you go.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Slider(
            value = km,
            onValueChange = { km = (it * 2).roundToInt() / 2f },
            valueRange = 0.5f..60f,
        )
        options.forEach { o ->
            val isSuggested = o.mode == suggestion
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSuggested) scheme.primaryContainer else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(o.mode.emoji, modifier = Modifier.width(30.dp))
                Text(
                    o.mode.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSuggested) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(84.dp),
                )
                Box(Modifier.weight(1f).padding(end = 10.dp)) {
                    ProgressTrack((o.emittedKg / maxKg).toFloat(), height = 6.dp, fill = androidx.compose.ui.graphics.SolidColor(
                        if (o.mode == TransportMode.CAR) scheme.error else scheme.primary
                    ))
                }
                Text(fmtKg(o.emittedKg), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
            }
        }
        Spacer(Modifier.height(10.dp))
        val saved = carKg - options.first { it.mode == suggestion }.emittedKg
        Surface(shape = RoundedCornerShape(12.dp), color = scheme.primaryContainer.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
            Text(
                "💡 Greener alternative: ${suggestion.emoji} ${suggestion.label.lowercase()} saves about ${fmtKg(saved)} " +
                    "of CO₂ versus driving and earns ~${CarbonCalculator.calculate(suggestion, km * 1000.0).credits} Green Points.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlineButton("Use ${suggestion.label.lowercase()} for my next trip", { onPick(suggestion) })
    }
}

@Composable
private fun HeroCard(name: String, stats: UserStats) {
    val level = EcoLevel.of(stats.savedKg)
    val next = level.next
    val progress by animateFloatAsState(EcoLevel.progress(stats.savedKg), tween(900), label = "level")
    val white = Color.White

    GradientCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Hi, ${name.substringBefore(' ').ifBlank { "there" }} 👋",
                    style = MaterialTheme.typography.titleMedium,
                    color = white.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(4.dp))
                Surface(color = white.copy(alpha = 0.18f), contentColor = white, shape = RoundedCornerShape(50)) {
                    Text(
                        "${level.emoji}  ${level.title}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            AppLogo(44.dp)
        }
        Spacer(Modifier.height(18.dp))
        Text("CO₂ saved", style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.8f))
        Text(
            fmtKg(stats.savedKg),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = white,
        )
        Spacer(Modifier.height(10.dp))
        ProgressTrack(progress, track = white.copy(alpha = 0.2f), fill = androidx.compose.ui.graphics.SolidColor(Mint), height = 10.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (next != null) "${fmtKg(next.minKg - stats.savedKg)} more to reach ${next.emoji} ${next.title}"
            else "You've reached the top level. Legend!",
            style = MaterialTheme.typography.bodySmall,
            color = white.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassStat(stats.creditBalance.toString(), "Green Points")
            GlassStat(fmtKm(stats.distanceKm), "travelled")
            GlassStat(stats.tripCount.toString(), "trips")
        }
    }
}

@Composable
private fun ModePicker(selected: TransportMode, onSelect: (TransportMode) -> Unit, onStart: () -> Unit) {
    SectionCard {
        SectionHeader("How are you travelling?")
        Spacer(Modifier.height(12.dp))
        TransportMode.entries.chunked(4).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { mode ->
                    ModeChip(mode, mode == selected, { onSelect(mode) }, Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        val perKm = CarbonCalculator.calculate(selected, 1000.0)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (selected == TransportMode.CAR) {
                    "🚗 Car trips show their footprint (≈${fmtKg(perKm.emittedKg)} CO₂/km) but don't earn Green Points."
                } else {
                    "${selected.emoji} Saves ≈${fmtKg(perKm.savedKg)} CO₂ per km vs driving · " +
                        "${String.format(Locale.US, "%.1f", perKm.savedKg * CarbonCalculator.CREDITS_PER_KG_SAVED)} points/km"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        GradientButton("▶  Start ${selected.label.lowercase()} trip", onStart)
    }
}

@Composable
private fun ModeChip(mode: TransportMode, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = 0.55f),
        label = "chipBg",
    )
    val scale by animateFloatAsState(if (selected) 1.06f else 1f, label = "chipScale")
    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = if (selected) BorderStroke(2.dp, scheme.primary) else null,
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(mode.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                mode.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveTripCard(live: LiveTrip, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(live.startTimeMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot",
    )
    val estimate = CarbonCalculator.calculate(live.mode, live.distanceMeters)
    val white = Color.White

    GradientCard(brush = Brand.live) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${live.mode.emoji}  ${live.mode.label} trip",
                style = MaterialTheme.typography.titleMedium,
                color = white,
                modifier = Modifier.weight(1f),
            )
            Surface(color = white.copy(alpha = 0.12f), contentColor = white, shape = RoundedCornerShape(50)) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252).copy(alpha = dotAlpha)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            fmtDuration(now - live.startTimeMs),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = white,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassStat(fmtKm(live.distanceMeters / 1000.0), "distance")
            GlassStat(String.format(Locale.US, "%.1f", live.currentSpeedKmh), "km/h")
            if (live.mode == TransportMode.CAR) {
                GlassStat(fmtKg(estimate.emittedKg), "emitted")
            } else {
                GlassStat(fmtKg(estimate.savedKg), "CO₂ saved")
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            color = white.copy(alpha = 0.06f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                RouteSketch(live.path, Modifier.fillMaxSize(), line = Mint, end = Color(0xFFFFD54F))
                if (live.path.size < 2) {
                    Text(
                        if (live.gpsFixes == 0) "📡  Waiting for GPS signal…" else "Start moving to draw your route",
                        color = white.copy(alpha = 0.75f),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GradientButton("■  Stop & save trip", onStop, brush = Brand.danger)
        Text(
            "Keeps tracking with the screen off. You can also stop from the notification.",
            style = MaterialTheme.typography.bodySmall,
            color = white.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}

@Composable
private fun TripSummaryCard(trip: Trip, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val good = trip.credits > 0
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (good) "🎉" else trip.mode.emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (good) "Great trip!" else "Trip saved", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${trip.mode.label} · ${fmtKm(trip.distanceKm)} · ${fmtDuration(trip.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (trip.mode == TransportMode.CAR) {
                Pill("${fmtKg(trip.emittedKg)} CO₂ emitted", scheme.surfaceVariant, scheme.onSurfaceVariant)
            } else {
                Pill("🌿 ${fmtKg(trip.savedKg)} saved", scheme.primaryContainer, scheme.onPrimaryContainer)
            }
            Pill("⭐ +${trip.credits} points", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        }
        if (trip.note != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                (if (trip.flagged) "⚠️ " else "ℹ️ ") + trip.note,
                style = MaterialTheme.typography.bodySmall,
                color = if (trip.flagged) scheme.error else scheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
    }
}

@Composable
private fun TipCard() {
    val tip = remember { EcoTips.forDay(LocalDate.now().toEpochDay()) }
    SectionCard {
        Row(verticalAlignment = Alignment.Top) {
            Text("💡", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Eco tip of the day", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun hasFineLocation(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun startIfLocationOn(context: Context, mode: TransportMode, userId: Long, onMessage: (String) -> Unit) {
    if (userId <= 0) return
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(lm)) {
        onMessage("Turn on Location in your phone settings to track trips")
        return
    }
    TrackingService.start(context, mode, userId)
}
