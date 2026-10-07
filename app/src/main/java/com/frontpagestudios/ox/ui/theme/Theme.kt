package com.frontpagestudios.ox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.frontpagestudios.ox.R

val Lime = Color(0xFFD2F53C)
val LimeSoft = Color(0xFFE8FA9A)
val Ink = Color(0xFF0D0D0D)
val Graphite = Color(0xFF1A1A1A)
val Coal = Color(0xFF262626)
val Paper = Color(0xFFF0F0EC)
val Snow = Color(0xFFFFFFFF)
val Mist = Color(0xFFE3E3DE)
val Muted = Color(0xFF8C8C86)
val Danger = Color(0xFFFF5A4E)
const val LimeArgb = 0xFFD2F53C.toInt()

@OptIn(ExperimentalTextApi::class)
private fun grotesk(w: Int, weight: FontWeight) =
    Font(R.font.space_grotesk, weight, variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Grotesk = FontFamily(
    grotesk(300, FontWeight.Light),
    grotesk(400, FontWeight.Normal),
    grotesk(500, FontWeight.Medium),
    grotesk(600, FontWeight.SemiBold),
    grotesk(700, FontWeight.Bold),
)

private fun ts(size: Int, weight: FontWeight, line: Int = (size * 1.15).toInt(), spacing: Double = 0.0) =
    TextStyle(fontFamily = Grotesk, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

val OxType = Typography(
    displayLarge = ts(60, FontWeight.Light, 58, -2.2),
    displayMedium = ts(46, FontWeight.Light, 46, -1.6),
    displaySmall = ts(34, FontWeight.Normal, 36, -1.0),
    headlineLarge = ts(28, FontWeight.Medium, 32, -0.6),
    headlineMedium = ts(23, FontWeight.Medium, 28, -0.4),
    headlineSmall = ts(20, FontWeight.Medium, 24, -0.2),
    titleLarge = ts(19, FontWeight.SemiBold, 24, -0.2),
    titleMedium = ts(16, FontWeight.SemiBold, 20),
    titleSmall = ts(14, FontWeight.SemiBold, 18),
    bodyLarge = ts(16, FontWeight.Normal, 22),
    bodyMedium = ts(14, FontWeight.Normal, 20),
    bodySmall = ts(12, FontWeight.Normal, 16),
    labelLarge = ts(14, FontWeight.SemiBold, 18),
    labelMedium = ts(12, FontWeight.Medium, 16, 0.2),
    labelSmall = ts(11, FontWeight.Medium, 14, 0.8),
)

private val scheme = lightColorScheme(
    primary = Ink,
    onPrimary = Snow,
    secondary = Lime,
    onSecondary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Muted,
    error = Danger,
)

@Composable
fun OxTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = OxType, content = content)
}
