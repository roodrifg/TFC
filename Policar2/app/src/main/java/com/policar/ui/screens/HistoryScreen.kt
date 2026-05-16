package com.policar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.policar.data.model.CalendarDay
import com.policar.data.model.FutbolBiomechanics
import com.policar.data.model.GymBiomechanics
import com.policar.data.model.PadelBiomechanics
import com.policar.data.model.TipoDeporte
import com.policar.data.model.TrainingSession
import com.policar.data.model.ViewPeriod
import kotlinx.serialization.json.Json
import com.policar.ui.components.*
import com.policar.ui.theme.*
import com.policar.ui.viewmodel.HistoryViewModel
import com.policar.R

private val BorderSubtle = Color.White.copy(alpha = 0.06f)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    navController: NavController,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showContent by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.selectedSession != null) {
        viewModel.clearSelectedSession()
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
        viewModel.setUserId("demo_user")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030303),
                        Color(0xFF080808),
                        Color(0xFF050505)
                    )
                )
            )
    ) {
        uiState.selectedSession?.let { session ->
            SessionDetailView(
                session = session,
                onClose = { viewModel.clearSelectedSession() },
                formatDuration = { viewModel.formatDuration(it) },
                formatDateFull = { viewModel.formatDateFull(it) }
            )
        } ?: Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HISTORY",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onNavigateToDashboard) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Dashboard",
                        tint = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300) { it / 4 })
            ) {
                PeriodSelector(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(tween(300, delayMillis = 100) { it / 4 })
            ) {
                CalendarView(
                    calendarDays = uiState.calendarDays,
                    selectedDate = uiState.selectedDate,
                    period = uiState.selectedPeriod,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonRed,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                val sessionsForDate = viewModel.getSessionsForDate(uiState.selectedDate)

                if (sessionsForDate.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO SESSIONS",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "for this period",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = TextTertiary.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${sessionsForDate.size} SESSIONS",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        ),
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(sessionsForDate) { session ->
                            SessionCard(
                                session = session,
                                onClick = { viewModel.selectSession(session) },
                                onDelete = { viewModel.deleteSession(session.id) },
                                formatDuration = { viewModel.formatDuration(it) },
                                formatTime = { viewModel.formatTime(session.startTimestamp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SessionCard(
    session: TrainingSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    formatDuration: (Int) -> String,
    formatTime: (Long) -> String
) {
    val sportColor = when (session.sportType.lowercase()) {
        "futbol" -> SportFutbol
        "padel" -> SportPadel
        "gimnasio" -> SportGym
        else -> NeonCyan
    }

    val sportIcon: ImageVector = when (session.sportType.lowercase()) {
        "futbol" -> Icons.Default.SportsSoccer
        "padel" -> Icons.Default.SportsTennis
        else -> Icons.Default.FitnessCenter
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SurfaceCard, SurfaceDark)
                )
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        sportColor.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .border(1.dp, sportColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = sportIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.sportType.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = sportColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTime(session.startTimestamp),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatDuration(session.durationSeconds),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextSecondary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = NeonRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${session.hrAvg.toInt()}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = " BPM",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        ),
                        color = TextTertiary
                    )
                }

                if (session.rpe > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RPE ${session.rpe}/10",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionDetailView(
    session: TrainingSession,
    onClose: () -> Unit,
    formatDuration: (Int) -> String,
    formatDateFull: (Long) -> String
) {
    val json = remember { Json { ignoreUnknownKeys = true } }
    val futbolBio = remember(session.id) {
        session.futbolBiomechanics?.let {
            runCatching { json.decodeFromJsonElement(FutbolBiomechanics.serializer(), it) }.getOrNull()
        }
    }
    val padelBio = remember(session.id) {
        session.padelBiomechanics?.let {
            runCatching { json.decodeFromJsonElement(PadelBiomechanics.serializer(), it) }.getOrNull()
        }
    }
    val gymBio = remember(session.id) {
        session.gymBiomechanics?.let {
            runCatching { json.decodeFromJsonElement(GymBiomechanics.serializer(), it) }.getOrNull()
        }
    }

    val sportColor = when (session.sportType.uppercase()) {
        "FUTBOL" -> SportFutbol
        "PADEL" -> SportPadel
        "GIMNASIO" -> SportGym
        else -> NeonCyan
    }
    val sportIcon: ImageVector = when (session.sportType.uppercase()) {
        "FUTBOL" -> Icons.Default.SportsSoccer
        "PADEL" -> Icons.Default.SportsTennis
        else -> Icons.Default.FitnessCenter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cerrar",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = sportIcon, contentDescription = null, tint = sportColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = session.sportType.uppercase(),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = sportColor
                    )
                    Text(
                        text = formatDateFull(session.startTimestamp),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailStatCard(modifier = Modifier.weight(1f), label = "DURACION", value = formatDuration(session.durationSeconds), color = TextPrimary)
                DetailStatCard(modifier = Modifier.weight(1f), label = "RPE", value = if (session.rpe > 0) "${session.rpe}/10" else "--", color = NeonYellow)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailStatCard(modifier = Modifier.weight(1f), label = "FC MEDIA", value = "${session.hrAvg.toInt()} bpm", color = NeonRed)
                DetailStatCard(modifier = Modifier.weight(1f), label = "FC MAX", value = "${session.hrMax} bpm", color = StatusDanger)
                DetailStatCard(modifier = Modifier.weight(1f), label = "FC MIN", value = "${session.hrMin} bpm", color = NeonGreen)
            }

            val totalZoneSeconds = session.zone1Seconds + session.zone2Seconds + session.zone3Seconds + session.zone4Seconds + session.zone5Seconds
            if (totalZoneSeconds > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                DetailSectionHeader(title = "ZONAS FC", color = NeonRed, icon = Icons.Default.Favorite)
                Spacer(modifier = Modifier.height(10.dp))
                ZonesDetailRow(session = session, total = totalZoneSeconds)
            }

            when (session.sportType.uppercase()) {
                "FUTBOL" -> futbolBio?.let { bio ->
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSectionHeader(title = "BIOMECANICA · FUTBOL", color = sportColor, icon = Icons.Default.SportsSoccer)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard(modifier = Modifier.weight(1f), label = "IMPACTOS", value = "${bio.totalImpacts}", color = sportColor)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "ALTA INTENS.", value = "${bio.highIntensityImpacts}", color = StatusDanger)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "G MAX", value = "${"%.1f".format(bio.maxGForce)} G", color = NeonYellow)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard(modifier = Modifier.weight(1f), label = "SPRINTS", value = "${bio.sprintCount}", color = NeonYellow)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "SALTOS", value = "${bio.jumpCount}", color = NeonCyan)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "ASIMETRIA", value = "${"%.0f".format(bio.asymmetryScore)}%", color = if (bio.asymmetryScore < 15f) NeonGreen else NeonYellow)
                    }
                }
                "PADEL" -> padelBio?.let { bio ->
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSectionHeader(title = "BIOMECANICA · PADEL", color = sportColor, icon = Icons.Default.SportsTennis)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard(modifier = Modifier.weight(1f), label = "SMASHES", value = "${bio.totalSmashes}", color = sportColor)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "ROT. MEDIA", value = "${"%.0f".format(bio.avgRotationDps)} d/s", color = NeonCyan)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "ROT. MAX", value = "${"%.0f".format(bio.maxRotationDps)} d/s", color = NeonYellow)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard(modifier = Modifier.weight(1f), label = "ASIMETRIA", value = "${"%.0f".format(bio.asymmetryScore * 100)}%", color = if (bio.asymmetryScore < 0.2f) NeonGreen else NeonYellow)
                    }
                }
                "GIMNASIO" -> gymBio?.let { bio ->
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSectionHeader(title = "BIOMECANICA · GYM", color = sportColor, icon = Icons.Default.FitnessCenter)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard(modifier = Modifier.weight(1f), label = "REPS", value = "${bio.totalReps}", color = sportColor)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "SERIES", value = "${bio.totalSets}", color = NeonYellow)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "VEL. MEDIA", value = "${"%.2f".format(bio.avgVelocityMs)} m/s", color = NeonGreen)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStatCard(modifier = Modifier.weight(1f), label = "PERD. VEL.", value = "${"%.0f".format(bio.velocityLossPct)}%", color = if (bio.velocityLossPct < 20f) NeonGreen else StatusDanger)
                        DetailStatCard(modifier = Modifier.weight(1f), label = "FATIGA", value = "${"%.1f".format(bio.fatigueIndex)}", color = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailStatCard(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 8.sp, color = TextTertiary, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun DetailSectionHeader(title: String, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = color.copy(alpha = 0.2f))
    }
}

@Composable
private fun ZonesDetailRow(session: TrainingSession, total: Int) {
    val zoneColors = listOf(StatusGood, NeonGreen, NeonYellow, Color(0xFFFF9100), StatusDanger)
    val zoneLabels = listOf("Z1", "Z2", "Z3", "Z4", "Z5")
    val zoneSeconds = listOf(session.zone1Seconds, session.zone2Seconds, session.zone3Seconds, session.zone4Seconds, session.zone5Seconds)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        zoneSeconds.forEachIndexed { i, secs ->
            val fraction = if (total > 0) secs.toFloat() / total else 0f
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(maxOf(fraction, 0.04f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(zoneColors[i].copy(alpha = if (fraction > 0.01f) 0.75f else 0.1f))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = zoneLabels[i], fontSize = 9.sp, color = zoneColors[i], fontWeight = FontWeight.Bold)
                val mins = secs / 60
                val s = secs % 60
                Text(text = "${mins}:${"%02d".format(s)}", fontSize = 7.sp, color = TextTertiary, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
