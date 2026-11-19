package com.example.greeno2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = Forest,
    secondary = Sky,
    onSecondary = Color.White,
    secondaryContainer = SkyContainer,
    onSecondaryContainer = Color(0xFF01416A),
    tertiary = Sun,
    tertiaryContainer = SunContainer,
    onTertiaryContainer = Color(0xFF5C3D00),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7EEE6),
    onSurfaceVariant = Color(0xFF4A574D),
    error = Warning,
    errorContainer = WarningContainer,
)

private val DarkColors = darkColorScheme(
    primary = NightMint,
    onPrimary = Color(0xFF00390B),
    primaryContainer = Color(0xFF1F4D24),
    onPrimaryContainer = Color(0xFFC8EFC9),
    secondary = Color(0xFF8CCDF5),
    secondaryContainer = Color(0xFF0B3D5C),
    onSecondaryContainer = Color(0xFFD6EEFB),
    tertiary = Color(0xFFFFD36B),
    tertiaryContainer = Color(0xFF4F3A00),
    onTertiaryContainer = Color(0xFFFFF1C9),
    background = NightBg,
    onBackground = Color(0xFFE2EAE2),
    surface = NightSurface,
    onSurface = Color(0xFFE2EAE2),
    surfaceVariant = Color(0xFF26302A),
    onSurfaceVariant = Color(0xFFB9C7BB),
)

/** Brand colours are used on purpose (no dynamic colour) so the app always reads as "green". */
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
