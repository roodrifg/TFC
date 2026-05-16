package com.policar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.policar.data.model.DashboardStats
import com.policar.data.model.DailyVolume
import com.policar.data.model.TrainingSession
import com.policar.data.model.ViewPeriod
import com.policar.ui.components.BarChart
import com.policar.ui.components.LineChart
import com.policar.ui.components.PeriodSelector
import com.policar.ui.components.StatCard
import com.policar.ui.theme.NeonCyan
import com.policar.ui.theme.NeonGreen
import com.policar.ui.theme.NeonRed
import com.policar.ui.theme.SportFutbol
import com.policar.ui.theme.SportGym
import com.policar.ui.theme.SportPadel
import com.policar.ui.theme.SurfaceCard
import com.policar.ui.theme.SurfaceDark
import com.policar.ui.theme.SurfaceElevated
import com.policar.ui.theme.TextPrimary
import com.policar.ui.theme.TextSecondary
import com.policar.ui.theme.TextTertiary
import com.policar.ui.viewmodel.HistoryViewModel
import com.policar.R

private val BorderSubtle = Color.White.copy(alpha = 0.06f)

@Composable
fun DashboardScreen(
    viewModel: HistoryViewModel,
    navController: NavController,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showContent = true
        viewModel.setUserId("demo_user")
        viewModel.calculateDashboardStats()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
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
                            painter = painterResource(id = R.drawable.ic_futbol),
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DASHBOARD",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        ),
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_futbol),
                        contentDescription = "History",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
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

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonRed,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                val stats = uiState.dailyStats ?: createMockStats(uiState.selectedPeriod, uiState.sessions)

                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(300, delayMillis = 100)) + slideInVertically(tween(300, delayMillis = 100) { it / 4 })
                ) {
                    Column {
                        OverviewSection(stats = stats, formatDuration = { viewModel.formatDurationLong(it) })

                        Spacer(modifier = Modifier.height(20.dp))

                        HeartRateSection(stats = stats)

                        Spacer(modifier = Modifier.height(20.dp))

                        SportBreakdownSection(stats = stats)

                        Spacer(modifier = Modifier.height(20.dp))

                        ActivityVolumeSection(volumes = stats.weeklyVolumes)

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(
    stats: DashboardStats,
    formatDuration: (Long) -> String
) {
    Column {
        SectionTitle(title = "OVERVIEW")

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Sessions",
                value = stats.totalSessions.toString(),
                modifier = Modifier.weight(1f),
                valueColor = NeonCyan
            )
            StatCard(
                label = "Duration",
                value = formatDuration(stats.totalDurationSeconds),
                modifier = Modifier.weight(1f),
                valueColor = NeonGreen
            )
            StatCard(
                label = "Avg HR",
                value = if (stats.avgHr > 0) stats.avgHr.toString() else "--",
                unit = if (stats.avgHr > 0) "bpm" else "",
                modifier = Modifier.weight(1f),
                valueColor = NeonRed
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                label = "Streak",
                value = stats.currentStreak.toString(),
                unit = "days",
                modifier = Modifier.weight(1f),
                valueColor = NeonGreen
            )
            StatCard(
                label = "Avg RPE",
                value = if (stats.avgRpe > 0) String.format("%.1f", stats.avgRpe) else "--",
                modifier = Modifier.weight(1f),
                valueColor = TextPrimary
            )
            StatCard(
                label = "Best Streak",
                value = stats.longestStreak.toString(),
                unit = "days",
                modifier = Modifier.weight(1f),
                valueColor = TextSecondary
            )
        }
    }
}

@Composable
private fun HeartRateSection(stats: DashboardStats) {
    Column {
        SectionTitle(title = "HEART RATE")

        Spacer(modifier = Modifier.height(12.dp))

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
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricItem(
                        label = "AVG",
                        value = if (stats.avgHr > 0) stats.avgHr.toString() else "--",
                        unit = "bpm",
                        color = NeonRed
                    )
                    MetricItem(
                        label = "MAX",
                        value = if (stats.maxHr > 0) stats.maxHr.toString() else "--",
                        unit = "bpm",
                        color = NeonRed.copy(alpha = 0.8f)
                    )
                    MetricItem(
                        label = "MIN",
                        value = if (stats.minHr > 0) stats.minHr.toString() else "--",
                        unit = "bpm",
                        color = NeonCyan
                    )
                }

                if (stats.hrTrend.size >= 2) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HR TREND",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LineChart(
                        data = stats.hrTrend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SportBreakdownSection(stats: DashboardStats) {
    Column {
        SectionTitle(title = "SPORTS BREAKDOWN")

        Spacer(modifier = Modifier.height(12.dp))

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
                .padding(16.dp)
        ) {
            Column {
                stats.sessionsBySport.forEach { (sport, count) ->
                    val sportColor = when (sport.lowercase()) {
                        "futbol" -> SportFutbol
                        "padel" -> SportPadel
                        "gimnasio" -> SportGym
                        else -> NeonCyan
                    }

                    val sportIcon = when (sport.lowercase()) {
                        "futbol" -> R.drawable.ic_futbol
                        "padel" -> R.drawable.ic_padel
                        "gimnasio" -> R.drawable.ic_gym
                        else -> R.drawable.ic_gym
                    }

                    val totalSessions = stats.totalSessions
                    val percentage = if (totalSessions > 0) count.toFloat() / totalSessions else 0f

                    SportRow(
                        name = sport.uppercase(),
                        iconRes = sportIcon,
                        color = sportColor,
                        sessions = count,
                        percentage = percentage,
                        avgHr = stats.hrBySport[sport] ?: 0
                    )

                    if (sport != stats.sessionsBySport.keys.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (stats.sessionsBySport.isEmpty()) {
                    Text(
                        text = "No data for this period",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun SportRow(
    name: String,
    iconRes: Int,
    color: Color,
    sessions: Int,
    percentage: Float,
    avgHr: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = color
                )
                Text(
                    text = "$sessions sessions",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (avgHr > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$avgHr bpm",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.8f), color)
                        )
                    )
            )
        }
    }
}

@Composable
private fun ActivityVolumeSection(volumes: List<DailyVolume>) {
    Column {
        SectionTitle(title = "ACTIVITY VOLUME")

        Spacer(modifier = Modifier.height(12.dp))

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
                .padding(16.dp)
        ) {
            Column {
                if (volumes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MINUTES",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            ),
                            color = TextTertiary
                        )
                        Text(
                            text = volumes.maxOfOrNull { (it.durationSeconds / 60).toInt() }?.let { "${it}m" } ?: "0m",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    BarChart(
                        data = volumes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        volumes.takeLast(7).forEach { volume ->
                            Text(
                                text = volume.dayLabel,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp
                                ),
                                color = TextTertiary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No activity data",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonRed, NeonRed.copy(alpha = 0f))
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = TextTertiary
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = TextSecondary
                )
            }
        }
    }
}

private fun createMockStats(period: ViewPeriod, sessions: List<TrainingSession>): DashboardStats {
    return DashboardStats(
        period = period,
        totalSessions = sessions.size,
        totalDurationSeconds = sessions.sumOf { it.durationSeconds.toLong() },
        totalMinutes = sessions.sumOf { it.durationSeconds } / 60,
        avgSessionDurationMinutes = if (sessions.isNotEmpty()) sessions.sumOf { it.durationSeconds } / 60 / sessions.size else 0,
        avgHr = sessions.map { it.hrAvg }.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0,
        maxHr = sessions.maxOfOrNull { it.hrMax } ?: 0,
        minHr = sessions.filter { it.hrMin > 0 }.minOfOrNull { it.hrMin } ?: 0,
        avgRpe = sessions.map { it.rpe }.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f,
        sessionsBySport = sessions.groupBy { it.sportType }.mapValues { it.value.size },
        hrBySport = sessions.groupBy { it.sportType }.mapValues { entries ->
            entries.value.map { it.hrAvg }.filter { hr -> hr > 0 }.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
        },
        durationBySport = sessions.groupBy { it.sportType }.mapValues { it.value.sumOf { s -> s.durationSeconds.toLong() } },
        weeklyVolumes = emptyList(),
        hrTrend = sessions.map { it.hrAvg }.filter { it > 0 },
        totalImpacts = 0,
        totalSmashes = 0,
        totalReps = 0,
        currentStreak = 0,
        longestStreak = 0
    )
}
