package com.example.greeno2.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.TransportMode
import com.example.greeno2.data.Trip
import com.example.greeno2.ui.components.BarChart
import com.example.greeno2.ui.components.EmojiBadge
import com.example.greeno2.ui.components.Pill
import com.example.greeno2.ui.components.ScreenTitle
import com.example.greeno2.ui.components.SectionCard
import com.example.greeno2.ui.components.SectionHeader
import com.example.greeno2.ui.components.fmtDuration
import com.example.greeno2.ui.components.fmtKg
import com.example.greeno2.ui.components.fmtKm
import com.example.greeno2.ui.components.fmtTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(trips: List<Trip>) {
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val byDay = remember(trips) {
        trips.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
    }
    val week = remember(trips) {
        (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            day to (byDay[day]?.sumOf { it.savedKg } ?: 0.0)
        }
    }
    val weekTotal = week.sumOf { it.second }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Your trips", "${trips.size} recorded") }
        item {
            SectionCard {
                SectionHeader("This week", trailing = "${fmtKg(weekTotal)} saved")
                Spacer(Modifier.height(14.dp))
                BarChart(
                    values = week.map { it.second.toFloat() },
                    labels = week.map { (d, _) ->
                        if (d == today) "Today" else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    },
                )
            }
        }
        if (trips.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No trips yet.\nStart one from the Track tab and it will show up here.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        byDay.forEach { (day, dayTrips) ->
            item(key = "day-$day") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        dayLabel(day, today),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${fmtKg(dayTrips.sumOf { it.savedKg })} saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(dayTrips, key = { it.id }) { trip -> TripRow(trip) }
        }
    }
}

private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
}

@Composable
private fun TripRow(trip: Trip) {
    val scheme = MaterialTheme.colorScheme
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(
                trip.mode.emoji,
                container = if (trip.flagged) scheme.errorContainer else scheme.primaryContainer,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${trip.mode.label} · ${fmtKm(trip.distanceKm)}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${fmtTime(trip.startTime)} · ${fmtDuration(trip.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (trip.mode == TransportMode.CAR) {
                    Text(fmtKg(trip.emittedKg), style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
                    Text("emitted", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                } else {
                    Text(fmtKg(trip.savedKg), style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                    Text("+${trip.credits} points", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        if (trip.flagged || trip.note != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trip.flagged) {
                    Pill("Flagged", scheme.errorContainer, scheme.error)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    trip.note.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (trip.flagged) scheme.error else scheme.onSurfaceVariant,
                )
            }
        }
    }
}
