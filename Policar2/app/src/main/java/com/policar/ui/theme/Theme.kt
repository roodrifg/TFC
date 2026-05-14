package com.policar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolicarDarkColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = TextPrimary,
    primaryContainer = NeonRedDeep,
    onPrimaryContainer = NeonRed,

    secondary = NeonCyan,
    onSecondary = BackgroundPure,
    secondaryContainer = Color(0xFF003D30),
    onSecondaryContainer = NeonCyan,

    tertiary = NeonGreen,
    onTertiary = BackgroundPure,
    tertiaryContainer = Color(0xFF003D10),
    onTertiaryContainer = NeonGreen,

    background = BackgroundPure,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,

    outline = BorderSubtle,
    outlineVariant = TextTertiary,

    error = NeonRedAlert,
    onError = TextPrimary,
    errorContainer = Color(0xFF4D0010),
    onErrorContainer = NeonRedAlert,

    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundPure,
    inversePrimary = NeonRedDeep,

    scrim = Color(0xCC000000),
    surfaceTint = NeonRed,
)

@Composable
fun PolicarTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PolicarDarkColorScheme,
        typography = PolicarTypography,
        content = content
    )
}