package com.policar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.policar.MainActivity
import com.policar.NavRoutes
import com.policar.R
import com.policar.data.model.FutbolBiomechanics
import com.policar.data.model.GymBiomechanics
import com.policar.data.model.GymRepData
import com.policar.data.model.HRZone
import com.policar.data.model.PadelBiomechanics
import com.policar.data.model.StressLevel
import com.policar.data.model.TipoDeporte
import com.policar.data.model.WorkoutSummary
import com.policar.ui.screens.GlassCard
import com.policar.ui.screens.GlassContainer
import com.policar.ui.theme.BackgroundPure
import com.policar.ui.theme.BorderSubtle
import com.policar.ui.theme.NeonCyan
import com.policar.ui.theme.NeonGreen
import com.policar.ui.theme.NeonRed
import com.policar.ui.theme.NeonRedAlert
import com.policar.ui.theme.StatusDanger
import com.policar.ui.theme.StatusGood
import com.policar.ui.theme.NeonYellow
import com.policar.ui.theme.SportFutbol
import com.policar.ui.theme.SportGym
import com.policar.ui.theme.SportPadel
import com.policar.ui.theme.SurfaceCard
import com.policar.ui.theme.SurfaceDark
import com.policar.ui.theme.SurfaceElevated
import com.policar.ui.theme.TextPrimary
import com.policar.ui.theme.TextSecondary
import com.policar.ui.theme.TextTertiary
import com.policar.ui.viewmodel.WorkoutViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private fun computeHrZone(hr: Int): HRZone = when {
    hr == 0 -> HRZone.ZONE_1
    hr < 100 -> HRZone.ZONE_1
    hr < 130 -> HRZone.ZONE_2
    hr < 160 -> HRZone.ZONE_3
    hr < 180 -> HRZone.ZONE_4
    else -> HRZone.ZONE_5
}

@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val heartRate     by viewModel.hrValue.collectAsStateWithLifecycle()
    val hrvStress    by viewModel.hrvStress.collectAsStateWithLifecycle()
    val ecgSamples   by viewModel.ecgSamples.collectAsStateWithLifecycle()
    val recording   by viewModel.recordingStatus.collectAsStateWithLifecycle()
    val tick        by viewModel.tick.collectAsStateWithLifecycle()
    val futbolBio   by viewModel.futbolBio.collectAsStateWithLifecycle()
    val padelBio    by viewModel.padelBio.collectAsStateWithLifecycle()
    val gymBio      by viewModel.gymBio.collectAsStateWithLifecycle()
    val haptic      = LocalHapticFeedback.current
    val context     = LocalContext.current

    DisposableEffect(Unit) {
        (context as? MainActivity)?.setKeepScreenOn(true)
        onDispose {
            (context as? MainActivity)?.setKeepScreenOn(false)
        }
    }

    var showStopDialog  by remember { mutableStateOf(false) }
    var showRpeDialog   by remember { mutableStateOf(false) }
    var selectedRpe     by remember { mutableStateOf(5) }
    var showSuccessScreen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedSuccessfully, uiState.savedWorkoutSummary) {
        if (uiState.savedSuccessfully && uiState.savedWorkoutSummary != null) {
            showSuccessScreen = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPure)
    ) {
        if (showSuccessScreen && uiState.savedWorkoutSummary != null) {
            WorkoutSavedScreen(
                summary = uiState.savedWorkoutSummary!!,
                onExit = {
                    showSuccessScreen = false
                    viewModel.resetWorkout()
                    navController.popBackStack(NavRoutes.HOME, inclusive = false)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                WorkoutTopBar(
                    sport          = viewModel.selectedSport,
                    elapsedSeconds = uiState.elapsedSeconds,
                    isPaused       = uiState.isPaused,
                    batteryLevel   = viewModel.batteryLevel,
                    onPauseResume  = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (uiState.isPaused) viewModel.resumeWorkout()
                        else viewModel.pauseWorkout()
                    },
                    onStop = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showStopDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                BpmHeroSection(
                    heartRate = heartRate,
                    hrZone    = computeHrZone(heartRate),
                    rmssd     = hrvStress.toFloat(),
                    rrMs      = hrvStress.toLong()
                )

                Spacer(modifier = Modifier.height(12.dp))

                HrvPanel(
                    rmssd       = viewModel.telemetry.rmssd.toFloat(),
                    stressLevel = StressLevel.LOW,
                    rrInterval  = viewModel.telemetry.rrInterval.toLong()
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (viewModel.selectedSport) {
                    TipoDeporte.FUTBOL -> FutbolBiomechanicsPanel(
                        data = futbolBio,
                        currentGForce = futbolBio.maxGForce,
                        isImpact = futbolBio.avgGForce > 2.0f
                    )
                    TipoDeporte.PADEL -> PadelBiomechanicsPanel(
                        data = padelBio,
                        currentX = 0,
                        currentY = padelBio.maxRotationDps.toInt()
                    )
                    TipoDeporte.GIMNASIO -> GymBiomechanicsPanel(
                        data = gymBio,
                        velocityAlert = gymBio.velocityLossPct > 20f
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            if (uiState.syncState.toString() == "UPLOADING" || uiState.syncState.toString() == "DOWNLOADING") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Guardando en la nube…", color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest  = { showStopDialog = false },
            containerColor    = SurfaceDark,
            titleContentColor = TextPrimary,
            title = { Text("Finalizar entrenamiento", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "¿Deseas terminar y guardar el entrenamiento en Supabase?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStopDialog = false
                        showRpeDialog  = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) {
                    Text("Finalizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Continuar", color = NeonCyan)
                }
            }
        )
    }

    if (showRpeDialog) {
        RpeDialog(
            selectedRpe   = selectedRpe,
            onRpeSelected = { selectedRpe = it },
            hrSamples     = viewModel.telemetry.hrHistory,
            onConfirm     = {
                showRpeDialog = false
                viewModel.saveWorkout(selectedRpe)
            },
            onDismiss     = {
                showRpeDialog = false
                viewModel.saveWorkout(0)
            }
        )
    }
}

@Composable
private fun WorkoutSavedScreen(
    summary: WorkoutSummary,
    onExit: () -> Unit
) {
    val sportIcon = when (summary.sportType.uppercase()) {
        "FUTBOL" -> R.drawable.ic_futbol
        "PADEL" -> R.drawable.ic_padel
        "GIMNASIO" -> R.drawable.ic_gym
        else -> R.drawable.ic_gym
    }

    val sportColor = when (summary.sportType.uppercase()) {
        "FUTBOL" -> SportFutbol
        "PADEL" -> SportPadel
        "GIMNASIO" -> SportGym
        else -> NeonCyan
    }

    val sportLabel = when (summary.sportType.uppercase()) {
        "FUTBOL" -> "Futbol"
        "PADEL" -> "Padel"
        "GIMNASIO" -> "Gimnasio"
        else -> summary.sportType
    }

    val dateFormatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    val timeFormatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val startDateStr = dateFormatter.format(java.util.Date(summary.startTime))
    val startTimeStr = timeFormatter.format(java.util.Date(summary.startTime))
    val endDateStr = dateFormatter.format(java.util.Date(summary.endTime))
    val endTimeStr = timeFormatter.format(java.util.Date(summary.endTime))

    val hours = summary.durationSeconds / 3600
    val minutes = (summary.durationSeconds % 3600) / 60
    val seconds = summary.durationSeconds % 60
    val durationStr = if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF030303), Color(0xFF0A0A0A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(sportColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(sportIcon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = sportColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Entrenamiento_finalizado",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Subido correctamente a Supabase",
                fontSize = 16.sp,
                color = NeonGreen,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard.copy(alpha = 0.5f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = sportLabel,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = sportColor,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Fecha inicio", fontSize = 11.sp, color = TextTertiary)
                            Text(text = startDateStr, fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Hora inicio", fontSize = 11.sp, color = TextTertiary)
                            Text(text = startTimeStr, fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Fecha fin", fontSize = 11.sp, color = TextTertiary)
                            Text(text = endDateStr, fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Hora fin", fontSize = 11.sp, color = TextTertiary)
                            Text(text = endTimeStr, fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Deporte", fontSize = 11.sp, color = TextTertiary)
                            Text(text = sportLabel, fontSize = 14.sp, color = sportColor, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Duracion", fontSize = 11.sp, color = TextTertiary)
                            Text(text = durationStr, fontSize = 14.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "FC Max", fontSize = 11.sp, color = TextTertiary)
                            Text(text = "${summary.maxHr} bpm", fontSize = 14.sp, color = NeonRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Aceptar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun WorkoutTopBar(
    sport: TipoDeporte,
    elapsedSeconds: Long,
    isPaused: Boolean,
    batteryLevel: Int,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    val hours   = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeStr = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = sport.displayName.take(1), fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text       = sport.displayName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text  = "Polar H10",
                    fontSize = 9.sp,
                    color = NeonGreen
                )
            }
        }

        AnimatedContent(
            targetState   = timeStr,
            transitionSpec = {
                slideInVertically { -it } + fadeIn() togetherWith
                        slideOutVertically { it } + fadeOut()
            },
            label = "timer"
        ) { time ->
            Text(
                text       = time,
                fontWeight = FontWeight.Black,
                fontSize   = 22.sp,
                color      = if (isPaused) NeonYellow else TextPrimary,
                letterSpacing = 2.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick  = onPauseResume,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Reanudar" else "Pausar",
                    tint = NeonYellow,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick  = onStop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StatusDanger.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Detener",
                    tint = StatusDanger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BpmHeroSection(
    heartRate: Int,
    hrZone: HRZone,
    rmssd: Float,
    rrMs: Long
) {
    val zoneColor = when (hrZone) {
        HRZone.ZONE_1 -> StatusGood
        HRZone.ZONE_2 -> NeonGreen
        HRZone.ZONE_3 -> NeonYellow
        HRZone.ZONE_4 -> Color(0xFFFF9100)
        HRZone.ZONE_5 -> StatusDanger
    }

    val beatDurationMs = if (rrMs > 0) rrMs.toInt() else 800
    val infiniteTransition = rememberInfiniteTransition(label = "bpm_glow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(beatDurationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bpm_pulse"
    )
    val beatScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(beatDurationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beat_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .alpha(glowRadius * 0.25f)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(zoneColor.copy(alpha = 0.8f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
                    .blur(40.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(beatScale)
            ) {
                Text(text = "BPM", fontSize = 14.sp, color = zoneColor.copy(alpha = 0.8f))
                AnimatedContent(
                    targetState = heartRate,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { -it } + fadeIn() togetherWith
                                    slideOutVertically { it } + fadeOut()
                        } else {
                            slideInVertically { it } + fadeIn() togetherWith
                                    slideOutVertically { -it } + fadeOut()
                        }
                    },
                    label = "bpm_odometer"
                ) { bpm ->
                    Text(
                        text = if (bpm > 0) "$bpm" else "--",
                        fontWeight = FontWeight.Black,
                        fontSize   = 88.sp,
                        color      = zoneColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "BPM", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = zoneColor, letterSpacing = 3.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(zoneColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = hrZone.label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = zoneColor, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HrvPanel(
    rmssd: Float,
    stressLevel: StressLevel,
    rrInterval: Long
) {
    val stressColor = when (stressLevel) {
        StressLevel.VERY_LOW, StressLevel.LOW -> NeonGreen
        StressLevel.MODERATE                  -> NeonYellow
        StressLevel.HIGH, StressLevel.VERY_HIGH -> StatusDanger
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(modifier = Modifier.weight(1f), label = "HRV RMSSD", value = "${"%.0f".format(rmssd)}", unit = "ms", color = NeonCyan, icon = "HRV")
        MetricCard(modifier = Modifier.weight(1f), label = "ESTRÉS", value = stressLevel.label, unit = "", color = stressColor, icon = if (stressLevel.ordinal < 2) "OK" else "ALERT")
        MetricCard(modifier = Modifier.weight(1f), label = "R-R", value = "$rrInterval", unit = "ms", color = TextSecondary, icon = "RR")
    }
}

@Composable
private fun FutbolBiomechanicsPanel(data: FutbolBiomechanics, currentGForce: Float, isImpact: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "impact")
    val impactAlpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "impact_alpha")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(icon = "FOOTBALL", title = "BIOMECHANICS · FOOTBALL", color = SportFutbol)
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "FUERZA G ACTUAL", fontSize = 10.sp, color = TextTertiary, letterSpacing = 1.sp)
                    if (isImpact) {
                        Box(modifier = Modifier.alpha(impactAlpha).clip(RoundedCornerShape(4.dp)).background(StatusDanger.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(text = "IMPACT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusDanger, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${"%.2f".format(currentGForce)} G", fontWeight = FontWeight.Black, fontSize = 32.sp, color = if (isImpact) StatusDanger else SportFutbol)
                Spacer(modifier = Modifier.height(8.dp))
                GForceBar(normalized = min(currentGForce / 8f, 1f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(modifier = Modifier.weight(1f), label = "IMPACTOS", value = "${data.totalImpacts}", unit = "", color = SportFutbol, icon = "IMPACT")
            MetricCard(modifier = Modifier.weight(1f), label = "ALTA INTENSIDAD", value = "${data.highIntensityImpacts}", unit = ">3G", color = StatusDanger, icon = "HIR")
            MetricCard(modifier = Modifier.weight(1f), label = "G MAX", value = "${"%.1f".format(data.maxGForce)}", unit = "G", color = NeonYellow, icon = "GMAX")
        }

        Spacer(modifier = Modifier.height(10.dp))
        GlassCard {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "BALANCE DE CARGA", fontSize = 10.sp, color = TextTertiary, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LoadBar(label = "Mecánica", value = data.mechanicalLoadScore, maxValue = 10f, color = SportFutbol, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    LoadBar(label = "Cardiovascular", value = data.cardiovascularLoadScore, maxValue = 100f, color = NeonRed, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PadelBiomechanicsPanel(data: PadelBiomechanics, currentX: Int, currentY: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(icon = "PADEL", title = "BIOMECHANICS · PADEL", color = SportPadel)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(modifier = Modifier.weight(1f), label = "SMASHES", value = "${data.totalSmashes}", unit = "", color = SportPadel, icon = "PADEL")
            MetricCard(modifier = Modifier.weight(1f), label = "ROT. PICO", value = "${"%.0f".format(data.maxRotationDps)}", unit = "deg/s", color = NeonCyan, icon = "ROT")
            MetricCard(modifier = Modifier.weight(1f), label = "ASIMETRIA", value = "${"%.0f".format(data.asymmetryScore * 100)}", unit = "%", color = if (data.asymmetryScore < 0.2f) NeonGreen else NeonYellow, icon = "ASYMM")
        }
        Spacer(modifier = Modifier.height(10.dp))
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "ROTACIÓN DE TRONCO (X/Y)", fontSize = 10.sp, color = TextTertiary, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("EJE X", fontSize = 9.sp, color = NeonCyan)
                            Text("${currentX} mG", fontSize = 9.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AxisBar(value = currentX, max = 3000, color = NeonCyan)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("EJE Y", fontSize = 9.sp, color = SportPadel)
                            Text("${currentY} mG", fontSize = 9.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        AxisBar(value = currentY, max = 3000, color = SportPadel)
                    }
                }
            }
        }
    }
}

@Composable
private fun GymBiomechanicsPanel(data: GymBiomechanics, velocityAlert: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionHeader(icon = "GYM", title = "BIOMECHANICS · GYM", color = SportGym)
        Spacer(modifier = Modifier.height(10.dp))

        AnimatedVisibility(visible = velocityAlert) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(StatusDanger.copy(alpha = 0.12f)).border(1.dp, StatusDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Velocidad concéntrica -${"%.0f".format(data.velocityLossPct)}%", fontSize = 12.sp, color = StatusDanger, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(modifier = Modifier.weight(1f), label = "REPS", value = "${data.totalReps}", unit = "", color = SportGym, icon = "REPS")
            MetricCard(modifier = Modifier.weight(1f), label = "SERIES", value = "${data.totalSets}", unit = "", color = NeonYellow, icon = "SETS")
            MetricCard(modifier = Modifier.weight(1f), label = "VELOCIDAD", value = "${"%.2f".format(data.avgVelocityMs)}", unit = "m/s", color = if (!velocityAlert) NeonGreen else StatusDanger, icon = "VEL")
        }

        if (data.repsData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "VELOCIDAD POR REP", fontSize = 10.sp, color = TextTertiary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    VelocitySparklineChart(reps = data.repsData)
                }
            }
        }
    }
}

@Composable
private fun RpeDialog(selectedRpe: Int, onRpeSelected: (Int) -> Unit, hrSamples: List<Int>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = TextPrimary,
        title = { Column { Text("Esfuerzo Percibido (RPE)", fontWeight = FontWeight.Bold); Text(text = "Escala CR10 de Borg", fontSize = 11.sp, color = TextTertiary) } },
        text = {
            Column {
                if (hrSamples.size > 5) {
                    Text(text = "PULSO DE LA SESIÓN", fontSize = 9.sp, color = TextTertiary, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
                    HrSparkline(samples = hrSamples, modifier = Modifier.fillMaxWidth().height(60.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(text = "¿Cómo fue el esfuerzo?", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))
                Slider(value = selectedRpe.toFloat(), onValueChange = { onRpeSelected(it.toInt()) }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed, inactiveTrackColor = SurfaceElevated))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Muy fácil", fontSize = 9.sp, color = NeonGreen); Text(text = "RPE $selectedRpe/10", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonRed); Text("Máximo", fontSize = 9.sp, color = StatusDanger) }
                val rpeLabel = when (selectedRpe) { 1, 2 -> "Muy ligero"; 3, 4 -> "Ligero"; 5, 6 -> "Moderado"; 7, 8 -> "Duro"; 9 -> "Muy duro"; 10 -> "Esfuerzo máximo"; else -> "" }
                Text(text = rpeLabel, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            }
        },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = NeonRed)) { Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text("Guardar en Supabase") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Omitir", color = TextTertiary) } }
    )
}

@Composable
fun HrSparkline(samples: List<Int>, modifier: Modifier = Modifier) {
    if (samples.size < 2) return
    val minHr = samples.minOrNull() ?: 0
    val maxHr = samples.maxOrNull() ?: 1
    val range = maxOf(maxHr - minHr, 1)

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        val w = size.width
        val h = size.height
        val stepX = w / (samples.size - 1).toFloat()

        val path = Path()
        val fillPath = Path()

        samples.forEachIndexed { i, hr ->
            val x = i * stepX
            val norm = (hr - minHr).toFloat() / range.toFloat()
            val y = h - (norm * h * 0.85f) - h * 0.075f

            if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, h); fillPath.lineTo(x, y) }
            else { path.lineTo(x, y); fillPath.lineTo(x, y) }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(NeonRed.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(path = path, color = NeonRed, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun VelocitySparklineChart(reps: List<GymRepData>) {
    if (reps.size < 2) return
    val maxV = reps.maxOfOrNull { it.meanVelocity }?.takeIf { it > 0f } ?: 1f

    Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        val w = size.width
        val h = size.height
        val stepX = w / (reps.size - 1).toFloat()

        val path = Path()
        reps.forEachIndexed { i, rep ->
            val x = i * stepX
            val norm = rep.meanVelocity / maxV
            val y = h - norm * h * 0.9f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = NeonGreen, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        reps.forEachIndexed { i, rep ->
            val x = i * stepX
            val norm = rep.meanVelocity / maxV
            val y = h - norm * h * 0.9f
            val color = if (rep.velocityLossPct > 20f) StatusDanger else NeonGreen
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier = Modifier, label: String, value: String, unit: String, color: Color, icon: String) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark).border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Column {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedContent(targetState = value, transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() }, label = "metric_$label") { v ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = v, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
                    if (unit.isNotBlank()) { Spacer(modifier = Modifier.width(2.dp)); Text(text = unit, fontSize = 9.sp, color = TextTertiary, modifier = Modifier.padding(bottom = 2.dp)) }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 8.sp, color = TextTertiary, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun SectionHeader(icon: String, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = color.copy(alpha = 0.2f))
    }
}

@Composable
private fun GForceBar(normalized: Float) {
    val color = when { normalized < 0.3f -> NeonGreen; normalized < 0.6f -> NeonYellow; else -> StatusDanger }
    val animNorm by animateFloatAsState(targetValue = normalized, animationSpec = tween(200), label = "gforce_bar")
    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(SurfaceElevated)) {
        Box(modifier = Modifier.fillMaxWidth(animNorm).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(brush = Brush.horizontalGradient(listOf(NeonGreen, color))))
    }
}

@Composable
private fun AxisBar(value: Int, max: Int, color: Color) {
    val normalized = min(kotlin.math.abs(value).toFloat() / max.toFloat(), 1f)
    val animNorm by animateFloatAsState(targetValue = normalized, animationSpec = tween(100), label = "axis_bar")
    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(SurfaceElevated)) {
        Box(modifier = Modifier.fillMaxWidth(animNorm).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
    }
}

@Composable
private fun LoadBar(modifier: Modifier = Modifier, label: String, value: Float, maxValue: Float, color: Color) {
    val normalized = min(value / maxValue, 1f)
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontSize = 9.sp, color = TextTertiary); Text("${"%.1f".format(value)}", fontSize = 9.sp, color = color) }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceElevated)) {
            val animNorm by animateFloatAsState(normalized, tween(400), label = "load_$label")
            Box(modifier = Modifier.fillMaxWidth(animNorm).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}