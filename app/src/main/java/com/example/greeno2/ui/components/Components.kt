package com.example.greeno2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.greeno2.ui.theme.Serif
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.greeno2.R
import com.example.greeno2.ui.theme.Brand
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun fmtKg(kg: Double): String = when {
    kg < 1.0 -> String.format(Locale.US, "%.0f g", kg * 1000)
    kg < 100 -> String.format(Locale.US, "%.2f kg", kg)
    else -> String.format(Locale.US, "%.0f kg", kg)
}

fun fmtKm(km: Double): String =
    if (km < 1.0) String.format(Locale.US, "%.0f m", km * 1000) else String.format(Locale.US, "%.2f km", km)

fun fmtDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun fmtDateTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

fun fmtTime(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

fun fmtDate(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The app logo (same artwork as the launcher icon). */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f)),
    )
}

/** Full-width card with a brand gradient; put white content inside. */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = Brand.hero,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(brush),
    ) {
        // Soft decorative circles give the flat gradient some depth.
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.minDimension * 0.6f, center = Offset(size.width, 0f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(0f, size.height))
        }
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** Translucent stat chip for use on gradient cards. */
@Composable
fun RowScope.GlassStat(value: String, label: String) {
    Surface(
        modifier = Modifier.weight(1f),
        color = Color.White.copy(alpha = 0.15f),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f), maxLines = 1)
        }
    }
}

@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    fill: Brush = Brand.button,
    height: Dp = 8.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0.03f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(fill),
            )
        }
    }
}

@Composable
fun Avatar(name: String, size: Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    val color = Brand.avatarColors[abs(name.hashCode() % Brand.avatarColors.size)]
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(ring ?: Color.Transparent)
            .padding(if (ring != null) 3.dp else 0.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EmojiBadge(
    emoji: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { Column(Modifier.padding(18.dp), content = content) }
}

/** Big pill-shaped call-to-action with a gradient fill. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = Brand.button,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color.Gray, Color.Gray)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Secondary action: white with an olive outline. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
    }
}

/** Serif "GreenO2" wordmark with a small leaf, as on the case study's splash and login screens. */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: TextUnit = 40.sp,
    tagline: String? = "Your guide to greener journeys",
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Top) {
            Text("GreenO", color = color, fontFamily = Serif, fontSize = size, fontWeight = FontWeight.Medium)
            Text(
                "2",
                color = color,
                fontFamily = Serif,
                fontSize = size * 0.55f,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = (size.value * 0.55f).dp),
            )
            Text("🌿", fontSize = size * 0.42f, modifier = Modifier.padding(start = 2.dp))
        }
        if (tagline != null) {
            Text(
                tagline,
                color = color.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Underlined text tabs, like "Eco-stays  Restaurants  Activities" in the rewards design. */
@Composable
fun TextTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val active = i == selected
            Column(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (active) 28.dp else 0.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

/** Heading with a thin rule either side: "—— Certificate ——". */
@Composable
fun RuledHeading(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
    }
}

@Composable
fun KeyValueRow(key: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Draws the recorded route scaled to fit, on a faint grid, without needing a maps API key. */
@Composable
fun RouteSketch(
    path: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    line: Color = MaterialTheme.colorScheme.primary,
    end: Color = MaterialTheme.colorScheme.tertiary,
    grid: Color = Color.White.copy(alpha = 0.06f),
) {
    Canvas(modifier) {
        val step = 22.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height)); x += step }
        var y = 0f
        while (y < size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y)); y += step }

        if (path.size < 2) return@Canvas
        val lats = path.map { it.first }
        val lons = path.map { it.second }
        val minLat = lats.min(); val maxLat = lats.max()
        val minLon = lons.min(); val maxLon = lons.max()
        val spanLat = (maxLat - minLat).takeIf { it > 0 } ?: 1e-6
        val spanLon = (maxLon - minLon).takeIf { it > 0 } ?: 1e-6
        val pad = 18.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        val scale = minOf(w / spanLon, h / spanLat)
        val offX = pad + (w - spanLon * scale) / 2
        val offY = pad + (h - spanLat * scale) / 2
        fun toOffset(p: Pair<Double, Double>) = Offset(
            (offX + (p.second - minLon) * scale).toFloat(),
            (offY + (maxLat - p.first) * scale).toFloat(),
        )
        val route = Path().apply {
            val first = toOffset(path.first())
            moveTo(first.x, first.y)
            path.drop(1).forEach { val o = toOffset(it); lineTo(o.x, o.y) }
        }
        drawPath(route, line.copy(alpha = 0.25f), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(route, line, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color.White, radius = 7.dp.toPx(), center = toOffset(path.first()))
        drawCircle(line, radius = 4.dp.toPx(), center = toOffset(path.first()))
        drawCircle(end.copy(alpha = 0.35f), radius = 13.dp.toPx(), center = toOffset(path.last()))
        drawCircle(end, radius = 7.dp.toPx(), center = toOffset(path.last()))
    }
}

/** Simple rounded bar chart (e.g. CO₂ saved per day). The last bar is highlighted. */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val bar = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val empty = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            val n = values.size.coerceAtLeast(1)
            val max = (values.maxOrNull() ?: 0f).coerceAtLeast(0.0001f)
            val slot = size.width / n
            val bw = slot * 0.5f
            val minH = 6.dp.toPx()
            values.forEachIndexed { i, v ->
                val left = i * slot + (slot - bw) / 2
                val h = if (v <= 0f) minH else ((v / max) * size.height).coerceAtLeast(minH)
                val color = when {
                    v <= 0f -> empty
                    i == values.lastIndex -> bar
                    else -> muted
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, size.height - h),
                    size = Size(bw, h),
                    cornerRadius = CornerRadius(bw / 2, bw / 2),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        ) {
            labels.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Opens the Android share sheet (social sharing of achievements, Invite & Earn). */
fun shareText(context: android.content.Context, text: String, title: String = "Share via") {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(android.content.Intent.EXTRA_TEXT, text)
    context.startActivity(android.content.Intent.createChooser(send, title))
}
