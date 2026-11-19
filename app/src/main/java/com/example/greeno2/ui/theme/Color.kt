package com.example.greeno2.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Forest = Color(0xFF0B5A33)
val Leaf = Color(0xFF1E8E4E)
val Lime = Color(0xFF34C26A)
val Mint = Color(0xFFB9F6CA)
val MintContainer = Color(0xFFDDF4E3)
val Sky = Color(0xFF0277BD)
val SkyContainer = Color(0xFFD9EEFA)
val Sun = Color(0xFFF59E0B)
val SunContainer = Color(0xFFFFF0CC)
val Ink = Color(0xFF14201A)
val Paper = Color(0xFFF4F8F3)
val Warning = Color(0xFFC62828)
val WarningContainer = Color(0xFFFDE3E0)

val NightBg = Color(0xFF0C130F)
val NightSurface = Color(0xFF151F19)
val NightMint = Color(0xFF7DDC9C)

val DeepNight = Color(0xFF07170F)

/** Brand gradients. They are deliberately the same in light and dark mode. */
object Brand {
    val hero = Brush.linearGradient(listOf(Forest, Leaf, Lime))
    val live = Brush.linearGradient(listOf(DeepNight, Color(0xFF0E3B24), Forest))
    val gold = Brush.linearGradient(listOf(Color(0xFFFFB300), Color(0xFFF57C00)))
    val sky = Brush.linearGradient(listOf(Color(0xFF01579B), Color(0xFF0288D1), Color(0xFF26C6DA)))
    val button = Brush.horizontalGradient(listOf(Leaf, Lime))
    val danger = Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE53935)))

    val avatarColors = listOf(
        Color(0xFF1E8E4E), Color(0xFF0288D1), Color(0xFF8E24AA),
        Color(0xFFF57C00), Color(0xFFD81B60), Color(0xFF00897B),
    )
}
