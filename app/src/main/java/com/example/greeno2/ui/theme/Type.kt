package com.example.greeno2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The case study pairs an elegant serif for headings ("How Rewards Work", "Certificate",
 * the GreenMiles wordmark) with a clean sans-serif for everything else. We use the
 * platform serif so no font files need to be bundled or downloaded.
 */
val Serif = FontFamily.Serif

private val Default = Typography()

val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
    displayMedium = Default.displayMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
    displaySmall = Default.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Medium),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Default.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
)
