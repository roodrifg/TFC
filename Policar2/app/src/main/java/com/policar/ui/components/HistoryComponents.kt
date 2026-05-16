package com.policar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policar.data.model.CalendarDay
import com.policar.data.model.DailyVolume
import com.policar.data.model.ViewPeriod
import com.policar.ui.theme.NeonCyan
import com.policar.ui.theme.NeonGreen
import com.policar.ui.theme.NeonRed
import com.policar.ui.theme.NeonRedGlow
import com.policar.ui.theme.SurfaceCard
import com.policar.ui.theme.SurfaceDark
import com.policar.ui.theme.SurfaceElevated
import com.policar.ui.theme.TextPrimary
import com.policar.ui.theme.TextSecondary
import com.policar.ui.theme.TextTertiary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import androidx.compose.foundation.layout.statusBarsPadding

private val NeonRed = Color(0xFFFF0022)
private val NeonRedDark = Color(0xFF8B0000)
private val BorderSubtle = Color.White.copy(alpha = 0.06f)

@Composable
fun CalendarView(
    calendarDays: List<CalendarDay>,
    selectedDate: Long,
    period: ViewPeriod,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (period) {
            ViewPeriod.WEEK -> WeekView(
                calendarDays = calendarDays,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
            ViewPeriod.MONTH -> MonthView(
                calendarDays = calendarDays,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
            ViewPeriod.QUARTER -> QuarterView(
                calendarDays = calendarDays,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun WeekView(
    calendarDays: List<CalendarDay>,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        calendarDays.forEach { day ->
            val isSelected = day.date == selectedDate
            val borderAnim by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(200),
                label = "border"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onDateSelected(day.date) }
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = Instant.ofEpochMilli(day.date)
                        .atZone(ZoneId.systemDefault())
                        .dayOfWeek
                        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                        .uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = if (day.isToday) NeonRed else TextTertiary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> NeonRed.copy(alpha = 0.15f)
                                day.hasData -> NeonGreen.copy(alpha = 0.1f)
                                day.isToday -> NeonCyan.copy(alpha = 0.1f)
                                else -> Color.Transparent
                            }
                        )
                        .then(
                            if (isSelected || day.hasData || day.isToday) {
                                Modifier.border(
                                    width = 1.dp,
                                    color = when {
                                        isSelected -> NeonRed
                                        day.hasData -> NeonGreen.copy(alpha = 0.4f)
                                        else -> NeonCyan.copy(alpha = 0.3f)
                                    },
                                    shape = CircleShape
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = when {
                            isSelected -> NeonRed
                            day.hasData -> NeonGreen
                            day.isToday -> NeonCyan
                            else -> TextPrimary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            when {
                                day.hasData && day.sessions.size >= 3 -> NeonRed
                                day.hasData -> NeonGreen
                                else -> Color.Transparent
                            },
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun MonthView(
    calendarDays: List<CalendarDay>,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val weekDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = TextTertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val weeks = calendarDays.chunked(7)
        weeks.forEach { weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDays.forEach { day ->
                    val isSelected = day.date == selectedDate

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> NeonRed.copy(alpha = 0.15f)
                                    day.hasData -> NeonGreen.copy(alpha = 0.1f)
                                    day.isToday -> NeonCyan.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, NeonRed, CircleShape)
                                } else if (day.isToday) {
                                    Modifier.border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape)
                                } else Modifier
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onDateSelected(day.date) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = when {
                                    isSelected -> NeonRed
                                    day.hasData -> NeonGreen
                                    day.isToday -> NeonCyan
                                    !day.isCurrentMonth -> TextTertiary.copy(alpha = 0.3f)
                                    else -> TextPrimary
                                }
                            )
                            if (day.hasData) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            if (day.sessions.size >= 2) NeonRed else NeonGreen,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuarterView(
    calendarDays: List<CalendarDay>,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMM")

    val monthGroups = calendarDays.groupBy { day ->
        Instant.ofEpochMilli(day.date).atZone(ZoneId.systemDefault()).toLocalDate().month
    }

    Column {
        monthGroups.forEach { (month, days) ->
            val totalSessions = days.sumOf { it.sessions.size }
            val intensity = (totalSessions.toFloat() / days.size).coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextSecondary,
                    modifier = Modifier.width(40.dp)
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    days.takeLast(28).forEach { day ->
                        val isSelected = day.date == selectedDate

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        isSelected -> NeonRed.copy(alpha = 0.3f)
                                        day.hasData -> NeonGreen.copy(alpha = intensity * 0.6f + 0.1f)
                                        else -> SurfaceDark
                                    }
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.dp, NeonRed, RoundedCornerShape(2.dp))
                                    } else Modifier
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { onDateSelected(day.date) }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: ViewPeriod,
    onPeriodSelected: (ViewPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ViewPeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) NeonRed.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onPeriodSelected(period) }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = if (isSelected) NeonRed else TextTertiary
                )
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<DailyVolume>,
    modifier: Modifier = Modifier,
    barColor: Color = NeonCyan,
    maxBarColor: Color = NeonRed
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOfOrNull { it.durationSeconds } ?: 1

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val barWidth = (size.width - (data.size - 1) * 8.dp.toPx()) / data.size
        val maxHeight = size.height - 24.dp.toPx()

        data.forEachIndexed { index, volume ->
            val barHeight = (volume.durationSeconds.toFloat() / maxValue) * maxHeight
            val x = index * (barWidth + 8.dp.toPx())

            val color = if (volume.sessionCount >= 2) maxBarColor else barColor

            drawRoundRect(
                color = color.copy(alpha = 0.3f),
                topLeft = Offset(x, size.height - 20.dp.toPx()),
                size = Size(barWidth, 16.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )

            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - 20.dp.toPx() - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
fun LineChart(
    data: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = NeonCyan,
    fillColor: Color = NeonCyan.copy(alpha = 0.2f)
) {
    if (data.size < 2) return

    val maxValue = data.maxOfOrNull { it } ?: 1
    val minValue = data.minOfOrNull { it } ?: 0
    val range = (maxValue - minValue).coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val stepX = size.width / (data.size - 1)
        val heightRange = size.height - 20.dp.toPx()

        val fillPath = Path().apply {
            moveTo(0f, size.height - 20.dp.toPx())

            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - 20.dp.toPx() - ((value - minValue).toFloat() / range) * heightRange
                if (index == 0) lineTo(x, y) else quadraticTo(
                    x1 = (index - 1) * stepX + stepX / 2,
                    y1 = if (index > 0) {
                        size.height - 20.dp.toPx() - ((data[index - 1] - minValue).toFloat() / range) * heightRange
                    } else y,
                    x2 = x,
                    y2 = y
                )
            }

            lineTo(size.width, size.height - 20.dp.toPx())
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent)
            )
        )

        val linePath = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = size.height - 20.dp.toPx() - ((value - minValue).toFloat() / range) * heightRange
                if (index == 0) moveTo(x, y) else quadraticTo(
                    x1 = (index - 1) * stepX + stepX / 2,
                    y1 = if (index > 0) {
                        size.height - 20.dp.toPx() - ((data[index - 1] - minValue).toFloat() / range) * heightRange
                    } else y,
                    x2 = x,
                    y2 = y
                )
            }
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - 20.dp.toPx() - ((value - minValue).toFloat() / range) * heightRange
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun DonutChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(NeonGreen, NeonCyan, NeonRed)
) {
    if (data.isEmpty()) return

    val total = data.values.sum().toFloat()
    if (total == 0f) return

    Canvas(
        modifier = modifier.size(100.dp)
    ) {
        val strokeWidth = 12.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        data.entries.forEachIndexed { index, (_, value) ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
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
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextSecondary
                )
            }
        }
    }
}
