package com.example.greeno2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Olive,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = Olive,
    secondary = OliveSoft,
    onSecondary = Color.White,
    secondaryContainer = PaperTint,
    onSecondaryContainer = Olive,
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Color(0xFF5A3E08),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperTint,
    onSurfaceVariant = InkSoft,
    outline = Outline,
    outlineVariant = Outline,
    error = Warning,
    errorContainer = WarningContainer,
)

private val DarkColors = darkColorScheme(
    primary = NightSage,
    onPrimary = OliveDark,
    primaryContainer = Color(0xFF263421),
    onPrimaryContainer = Color(0xFFD8E8CB),
    secondary = Color(0xFFBCCFAB),
    secondaryContainer = Color(0xFF1F2A1A),
    onSecondaryContainer = Color(0xFFD8E8CB),
    tertiary = Color(0xFFE2BE72),
    tertiaryContainer = Color(0xFF443511),
    onTertiaryContainer = Color(0xFFF7EDD6),
    background = NightBg,
    onBackground = Color(0xFFE3E9DE),
    surface = NightSurface,
    onSurface = Color(0xFFE3E9DE),
    surfaceVariant = Color(0xFF1E281A),
    onSurfaceVariant = Color(0xFFB6C2AE),
    outline = NightOutline,
    outlineVariant = NightOutline,
)

@Composable
fun GreenO2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
