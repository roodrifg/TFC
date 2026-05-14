package com.policar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════
//  POLICAR TYPOGRAPHY SYSTEM — Elite Telemetry
// ═══════════════════════════════════════════════════════════════════════════════

private val DisplayFont = FontFamily.Default
private val BodyFont = FontFamily.Default
private val MonospaceFont = FontFamily.Monospace

val PolicarTypography = Typography(
    // ═══ DISPLAY — Large telemetry numbers ═══
    displayLarge = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Black,
        fontSize = 96.sp,
        lineHeight = 88.sp,
        letterSpacing = (-4).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        lineHeight = 60.sp,
        letterSpacing = (-2).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = MonospaceFont,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp,
    ),

    // ═══ HEADLINE — Section headers ═══
    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),

    // ═══ TITLE — Card titles ═══
    titleLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),

    // ═══ BODY — Regular text ═══
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ═══ LABEL — Small labels ═══
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ═══ CUSTOM TELEMETRY STYLES ═══
// These are used for BPM, G-force, velocity displays

val TelemetryBPM = TextStyle(
    fontFamily = MonospaceFont,
    fontWeight = FontWeight.Black,
    fontSize = 72.sp,
    lineHeight = 64.sp,
    letterSpacing = (-2).sp,
)

val TelemetryLarge = TextStyle(
    fontFamily = MonospaceFont,
    fontWeight = FontWeight.Bold,
    fontSize = 48.sp,
    lineHeight = 44.sp,
    letterSpacing = (-1).sp,
)

val TelemetryMedium = TextStyle(
    fontFamily = MonospaceFont,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.5).sp,
)

val TelemetrySmall = TextStyle(
    fontFamily = MonospaceFont,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
)

val TelemetryMicro = TextStyle(
    fontFamily = MonospaceFont,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.5.sp,
)

val TelemetryLabel = TextStyle(
    fontFamily = MonospaceFont,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 10.sp,
    letterSpacing = 1.sp,
)

// Headers
val HeaderSection = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 12.sp,
    letterSpacing = 2.sp,
)

val HeaderSport = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 18.sp,
    lineHeight = 20.sp,
    letterSpacing = 1.sp,
)