package com.aiva.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF0B0F1A)
val GlassCard = Color(0xFF141B2D)
val GlassBorder = Color(0xFF23304C)
val ElectricCyan = Color(0xFF38BDF8)
val VioletPulse = Color(0xFF818CF8)
val EmeraldSafe = Color(0xFF34D399)
val AmberCaution = Color(0xFFFBBF24)
val RoseAlert = Color(0xFFFB7185)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val AivaDarkScheme = darkColorScheme(
    primary = ElectricCyan,
    secondary = VioletPulse,
    tertiary = EmeraldSafe,
    background = ObsidianBg,
    surface = GlassCard,
    onPrimary = ObsidianBg,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = RoseAlert
)

@Composable
fun AivaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AivaDarkScheme,
        content = content
    )
}
