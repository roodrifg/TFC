package com.policar.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ═══════════════════════════════════════════════════════════════════════
//  POLICAR DESIGN SYSTEM — ELITE CYBERPUNK
//  Estética: Tactical HUD / F1 Telemetry / Military Grade
// ═══════════════════════════════════════════════════════════════════════

// ── BACKGROUND LAYERS ───────────────────────────────────────────────────
val BackgroundPure = Color(0xFF030303)
val BackgroundDeep = Color(0xFF050505)
val SurfaceBlack = Color(0xFF080808)
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceCard = Color(0xFF141414)
val SurfaceElevated = Color(0xFF1A1A1A)

// Glassmorphism surfaces
val GlassSurface = Color(0x0AFFFFFF)
val GlassOverlay = Color(0x14FFFFFF)
val GlassBorder = Color(0x1FFFFFFF)
val GlassHighlight = Color(0x0AFFFFFF)

// ── NEON RED — Primary Brand ──────────────────────────────────────────────────
val NeonRed = Color(0xFFFF0022)
val NeonRedBright = Color(0xFFFF1A33)
val NeonRedDim = Color(0x80FF0022)
val NeonRedGlow = Color(0x40FF0022)
val NeonRedGlowIntense = Color(0x60FF0022)
val NeonRedDeep = Color(0xFF8B0000)
val NeonRedDark = Color(0xFF4D0000)

// Gradient variants
val GradientNeonRed = listOf(NeonRed, NeonRedDark)
val GradientNeonRedHorizontal = listOf(NeonRed, NeonRedBright, NeonRed)
val GradientNeonRedRadial = listOf(NeonRed.copy(alpha = 0.3f), Color.Transparent)

// ── NEON CYAN — ECG / Active Signal ─────────────────────────────────
val NeonCyan = Color(0xFF00FFCC)
val NeonCyanDim = Color(0x4D00FFCC)
val NeonCyanGlow = Color(0x3000FFCC)
val NeonCyanTrace = Color(0xFF00FFDD)
val NeonCyanDeep = Color(0xFF006655)

// ── STATUS GREEN — Connected / Good ───────────────────────────────────
val NeonGreen = Color(0xFF00FF66)
val NeonGreenDim = Color(0x4D00FF66)
val NeonGreenGlow = Color(0x3000FF66)
val NeonGreenPulse = Color(0xFF00FF41)

// ── WARNING / ATTENTION ───────────────────────────────────────────────────
val NeonYellow = Color(0xFFFFE500)
val NeonYellowDim = Color(0x4DFFE500)
val NeonYellowGlow = Color(0x30FFE500)

val NeonOrange = Color(0xFFFF9100)
val NeonOrangeDim = Color(0x4DFF9100)

val NeonAmber = Color(0xFFFFAB00)

// ── DANGER / ERROR ────────────────────────────────────────────────────
val NeonRedAlert = Color(0xFFFF1744)
val NeonRedAlertDim = Color(0x4DFF1744)
val NeonRedAlertGlow = Color(0x30FF1744)

// Legacy aliases for backward compatibility
val StatusGood = NeonGreen
val StatusWarning = NeonYellow
val StatusDanger = NeonRedAlert

// ── HR ZONES — Heart Rate Training Zones ────────────────────────────────
val Zone1Recovery = Color(0xFF00FF66)  // 50-60% - Green
val Zone2Base = Color(0xFF4CAF50)       // 60-70% - Light green
val Zone3Aerobic = Color(0xFFFFEB3B)  // 70-80% - Yellow
val Zone4Threshold = Color(0xFFFF9800)  // 80-90% - Orange
val Zone5Max = Color(0xFFFF1744)     // 90-100% - Red

val ZoneGradient = listOf(
    Zone1Recovery,
    Zone2Base,
    Zone3Aerobic,
    Zone4Threshold,
    Zone5Max
)

// ── SPORT ACCENTS ────────────────────────────────────────────────────────
val SportFutbol = Color(0xFF00C853)
val SportFutbolDim = Color(0x4D00C853)
val SportFutbolGlow = Color(0x2000C853)

val SportPadel = Color(0xFF2979FF)
val SportPadelDim = Color(0x4D2979FF)
val SportPadelGlow = Color(0x202979FF)

val SportGym = Color(0xFFFF6D00)
val SportGymDim = Color(0x4FFF6D00)
val SportGymGlow = Color(0x20FF6D00)

// Sport gradients
val GradientFutbol = listOf(SportFutbol, NeonCyan)
val GradientPadel = listOf(SportPadel, NeonCyan)
val GradientGym = listOf(SportGym, NeonRed)

// ── TEXT COLORS ────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Color(0xFF737373)
val TextDisabled = Color(0xFF4D4D4D)
val TextInverse = Color(0xFF000000)

// Monospace text for telemetry
val MonospacePrimary = TextPrimary
val MonospaceSecondary = TextSecondary.copy(alpha = 0.7f)

// ── BORDERS ────────────────────────────────────────────────────────
val BorderSubtle = Color(0x0DFFFFFF)
val BorderMedium = Color(0x1AFFFFFF)
val BorderActive = Color(0x40FF0022)
val BorderGlow = Color(0x60FF0022)

// ── GLOW EFFECTS ──────────────────────────────────────────────────
val GlowRed = listOf(
    NeonRedGlow.copy(alpha = 0.4f),
    NeonRedGlow.copy(alpha = 0.2f),
    Color.Transparent
)

val GlowGreen = listOf(
    NeonGreenGlow.copy(alpha = 0.4f),
    NeonGreenGlow.copy(alpha = 0.2f),
    Color.Transparent
)

val GlowCyan = listOf(
    NeonCyanGlow.copy(alpha = 0.4f),
    NeonCyanGlow.copy(alpha = 0.2f),
    Color.Transparent
)

// ── GRADIENT COMBOS ─────────────────────────────────────────────────
val GradientBackground = listOf(
    BackgroundPure,
    BackgroundDeep,
    SurfaceBlack
)

val GradientCard = listOf(
    SurfaceCard,
    SurfaceDark
)

val GradientElevated = listOf(
    SurfaceElevated,
    SurfaceCard
)

val GradientECG = listOf(
    NeonCyan,
    NeonCyanTrace,
    NeonCyan
)

// ── ANIMATION COLORS ────────────────────────────────────────────────
val ScanLine = Color(0x08FFFFFF)
val ScanLineGlow = NeonRed.copy(alpha = 0.3f)

// Pulse animation colors
val PulseDotActive = NeonGreen
val PulseDotInactive = TextTertiary

// Loading states
val LoadingPrimary = NeonCyan
val LoadingSecondary = NeonRed

// ── ALPHA VARIANTS ─────────────────────────────────────────────────
val Alpha90 = 0.90f
val Alpha80 = 0.80f
val Alpha70 = 0.70f
val Alpha60 = 0.60f
val Alpha50 = 0.50f
val Alpha40 = 0.40f
val Alpha30 = 0.30f
val Alpha25 = 0.25f
val Alpha20 = 0.20f
val Alpha15 = 0.15f
val Alpha10 = 0.10f
val Alpha05 = 0.05f

// ═══════════════════════════════════════════════════════════════════════
//  HELPER FUNCTIONS
// ══════════════════════════════════════════════════════════════════���═��══

fun getZoneColor(zone: Int): Color = when (zone) {
    1 -> Zone1Recovery
    2 -> Zone2Base
    3 -> Zone3Aerobic
    4 -> Zone4Threshold
    5 -> Zone5Max
    else -> TextTertiary
}

fun getSportColor(sport: String): Color = when (sport.lowercase()) {
    "futbol", "football" -> SportFutbol
    "padel" -> SportPadel
    "gimnasio", "gym", "gymnasium" -> SportGym
    else -> NeonRed
}

fun getSportGradient(sport: String): List<Color> = when (sport.lowercase()) {
    "futbol", "football" -> GradientFutbol
    "padel" -> GradientPadel
    "gimnasio", "gym", "gymnasium" -> GradientGym
    else -> GradientNeonRed
}

fun getConnectionStatusColor(isConnected: Boolean, isSearching: Boolean): Color = when {
    isConnected -> NeonGreen
    isSearching -> NeonYellow
    else -> TextTertiary
}