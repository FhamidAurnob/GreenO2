package com.example.greeno2.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Palette from the GreenMiles UX case study: deep olive on clean white, thin outlines.
val Olive = Color(0xFF2F4423)
val OliveDark = Color(0xFF1F2E17)
val OliveSoft = Color(0xFF4F6B3D)
val Sage = Color(0xFF7E9A68)
val SageContainer = Color(0xFFE9EFE3)
val Lime = Color(0xFFD9F27A)
val Mint = Color(0xFFCFE8B8)
val Gold = Color(0xFFB7862C)
val GoldContainer = Color(0xFFF7EDD6)
val Ink = Color(0xFF1E2619)
val InkSoft = Color(0xFF5A6553)
val Outline = Color(0xFFD9DED3)
val Paper = Color(0xFFFFFFFF)
val PaperTint = Color(0xFFF6F8F3)
val Warning = Color(0xFFB3261E)
val WarningContainer = Color(0xFFFBE4E1)

val NightBg = Color(0xFF10150D)
val NightSurface = Color(0xFF182014)
val NightOutline = Color(0xFF2E3A27)
val NightSage = Color(0xFFA9C98F)

/** Brand fills. Kept deliberately flat and calm to match the case study's look. */
object Brand {
    val hero = Brush.linearGradient(listOf(Olive, OliveSoft))
    val live = Brush.linearGradient(listOf(OliveDark, Olive))
    val gold = Brush.linearGradient(listOf(Color(0xFFC9963A), Gold))
    val button = Brush.linearGradient(listOf(Olive, Olive))
    val danger = Brush.linearGradient(listOf(Color(0xFF9E2A22), Color(0xFFB3261E)))

    val avatarColors = listOf(
        Olive, Color(0xFF5B7F47), Color(0xFF8A6D3B),
        Color(0xFF3E6B6B), Color(0xFF7A4E5B), Color(0xFF4B5A8A),
    )
}
