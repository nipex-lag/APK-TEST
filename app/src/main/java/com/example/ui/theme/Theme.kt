package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NipexDarkColorScheme = darkColorScheme(
    primary = NeonCrimson,
    onPrimary = TextPrimary,
    primaryContainer = NeonCrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricGreen,
    onSecondary = ObsidianBgDeep,
    secondaryContainer = ElectricGreenDark,
    onSecondaryContainer = ElectricGreen,
    tertiary = CyberCyan,
    onTertiary = ObsidianBgDeep,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = GlassSurfaceSolid,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderSubtle,
    error = NeonCrimson,
    onError = TextPrimary,
)

private val NipexShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NipexLagTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NipexDarkColorScheme,
        typography = NipexTypography,
        shapes = NipexShapes,
        content = content,
    )
}
