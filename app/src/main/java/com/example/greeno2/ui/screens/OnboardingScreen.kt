package com.example.greeno2.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greeno2.data.User
import com.example.greeno2.domain.QuizQuestion
import com.example.greeno2.domain.QuizResult
import com.example.greeno2.domain.SustainabilityQuiz
import com.example.greeno2.ui.components.GradientButton
import com.example.greeno2.ui.components.OutlineButton
import com.example.greeno2.ui.components.ProgressTrack
import com.example.greeno2.ui.components.Wordmark
import com.example.greeno2.ui.theme.Olive

private enum class Step { INTRO, QUESTIONS, RESULT }

/**
 * First-run flow from the case study: welcome page with a "Take Quiz" modal (Let's start / Skip),
 * five one-at-a-time questions with a "Question n/5" indicator, then the sustainability score.
 * Also opened from Profile → "Retake the quiz".
 */
@Composable
fun OnboardingScreen(
    user: User,
    onSkip: () -> Unit,
    onSubmit: (QuizResult, (awarded: Int) -> Unit) -> Unit,
    onFinish: (openRewards: Boolean) -> Unit,
) {
    var step by remember { mutableStateOf(Step.INTRO) }
    var index by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf<Set<Int>>().apply { repeat(SustainabilityQuiz.questions.size) { add(emptySet()) } } }
    var result by remember { mutableStateOf<QuizResult?>(null) }
    var awarded by remember { mutableIntStateOf(-1) }

    BackHandler(enabled = step == Step.QUESTIONS) {
        if (index > 0) index-- else step = Step.INTRO
    }

    when (step) {
        Step.INTRO -> IntroPage(
            name = user.name,
            retake = user.quizState != com.example.greeno2.data.QuizState.NOT_SEEN,
            onStart = { step = Step.QUESTIONS },
            onSkip = onSkip,
        )
        Step.QUESTIONS -> QuestionPage(
            index = index,
            selected = answers[index],
            onToggle = { opt -> answers[index] = SustainabilityQuiz.toggle(SustainabilityQuiz.questions[index], answers[index], opt) },
            onBack = { if (index > 0) index-- else step = Step.INTRO },
            onNext = {
                if (index < SustainabilityQuiz.questions.lastIndex) {
                    index++
                } else {
                    val r = SustainabilityQuiz.score(answers.toList())
                    result = r
                    step = Step.RESULT
                    onSubmit(r) { awarded = it }
                }
            },
        )
        Step.RESULT -> result?.let { ResultPage(it, awarded, onFinish) }
    }
}

@Composable
private fun IntroPage(name: String, retake: Boolean, onStart: () -> Unit, onSkip: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Olive)
            .systemBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Wordmark(color = Color.White, tagline = "Your guide to greener journeys")
            Spacer(Modifier.height(20.dp))
            Text(
                "Travel smarter, travel greener! We help you reduce your carbon footprint on every " +
                    "commute and trip. Your journey matters. Make every trip a step toward a greener future!",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Text("© 2026 GreenO2", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }

        // The "Take Quiz" modal
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (retake) "Retake Quiz" else "Take Quiz",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hi ${name.substringBefore(' ')}! Here's a quick quiz with 5 questions to personalise " +
                        "your carbon tracking to your habits and preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You'll get a sustainability score, a suggested default travel mode and " +
                        "${SustainabilityQuiz.WELCOME_BONUS} Green Points to start with.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                GradientButton("Let's start!", onStart)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text(if (retake) "Not now" else "Skip quiz", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun QuestionPage(
    index: Int,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val total = SustainabilityQuiz.questions.size
    val progress by animateFloatAsState((index + 1f) / total, label = "quizProgress")
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) { Text("‹ Back") }
        Text("Quiz for Personalised Sustainable Travel", style = MaterialTheme.typography.titleLarge)
        Text("Help us curate the best experience", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Question ${index + 1}/$total",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                modifier = Modifier.width(110.dp),
            )
            ProgressTrack(progress, Modifier.weight(1f), height = 6.dp)
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
            label = "question",
            modifier = Modifier.weight(1f),
        ) { i ->
            QuestionBody(SustainabilityQuiz.questions[i], i, selected, onToggle)
        }

        GradientButton(
            if (index == total - 1) "See my score" else "Next",
            onNext,
            enabled = selected.isNotEmpty(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Terms of Use · Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuestionBody(q: QuizQuestion, index: Int, selected: Set<Int>, onToggle: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 22.dp, bottom = 16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(76.dp)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text(q.icon, style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(18.dp))
        Text("${index + 1}. ${q.text}", style = MaterialTheme.typography.titleMedium)
        if (q.hint != null) {
            Text(q.hint, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        q.options.forEachIndexed { i, option ->
            val checked = i in selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (checked) scheme.primaryContainer else scheme.surface,
                border = BorderStroke(if (checked) 1.5.dp else 1.dp, if (checked) scheme.primary else scheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggle(i) },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Checkbox for multi-select, radio dot for single-select
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(if (q.multiSelect) RoundedCornerShape(5.dp) else CircleShape)
                            .background(if (checked) scheme.primary else Color.Transparent)
                            .then(
                                if (checked) Modifier
                                else Modifier.background(scheme.outline.copy(alpha = 0.35f))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (checked) Text("✓", color = scheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("${option.emoji}  ${option.label}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ResultPage(result: QuizResult, awarded: Int, onFinish: (openRewards: Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val animated by animateFloatAsState(result.score / 100f, label = "score")
    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Your sustainability score", style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
            ScoreRing(animated, Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${result.score}", style = MaterialTheme.typography.displayMedium, color = scheme.primary)
                Text("out of 100", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
        Text(result.headline, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(result.advice, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = scheme.onSurfaceVariant)

        if (result.preferredMode != null) {
            Surface(shape = RoundedCornerShape(12.dp), color = scheme.primaryContainer) {
                Text(
                    "${result.preferredMode.emoji}  We'll start your Track screen on ${result.preferredMode.label.lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        if (awarded > 0) {
            Surface(shape = RoundedCornerShape(12.dp), color = scheme.tertiaryContainer) {
                Text(
                    "⭐  +$awarded Green Points welcome bonus!",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        GradientButton("Go to my dashboard", { onFinish(false) })
        OutlineButton("How do Green Points work?", { onFinish(true) })
    }
}

@Composable
private fun ScoreRing(fraction: Float, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.primaryContainer
    val fill = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        val style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawArc(track, -90f, 360f, false, topLeft, arcSize, style = style)
        drawArc(fill, -90f, 360f * fraction, false, topLeft, arcSize, style = style)
    }
}
