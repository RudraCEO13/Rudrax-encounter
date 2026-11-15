package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RudraXDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = PitchBlack,
    primaryContainer = ColorTokens.CyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonBlue,
    onSecondary = PureWhite,
    secondaryContainer = ColorTokens.BlueContainer,
    onSecondaryContainer = ElectricBlue,
    tertiary = CyberPurple,
    onTertiary = PureWhite,
    tertiaryContainer = ColorTokens.PurpleContainer,
    onTertiaryContainer = CyberPurple,
    background = PitchBlack,
    onBackground = PureWhite,
    surface = DeepObsidian,
    onSurface = PureWhite,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = CoolSlate,
    outline = GlassBorderSubtle,
    outlineVariant = GlassBorder,
    error = CrimsonAlert,
    onError = PureWhite
)

private object ColorTokens {
    val CyanContainer = androidx.compose.ui.graphics.Color(0xFF062C36)
    val BlueContainer = androidx.compose.ui.graphics.Color(0xFF0F2347)
    val PurpleContainer = androidx.compose.ui.graphics.Color(0xFF231642)
}

val MotionXShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RudraXMotionXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RudraXDarkColorScheme,
        typography = MotionXTypography,
        shapes = MotionXShapes,
        content = content
    )
}
