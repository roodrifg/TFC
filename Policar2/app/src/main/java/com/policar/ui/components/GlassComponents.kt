package com.policar.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policar.ui.theme.*
import kotlin.math.sin

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowEnabled: Boolean = false,
    glowColor: Color = NeonRed,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .then(
                if (glowEnabled) {
                    Modifier.drawBehind {
                        val radius = cornerRadius.toPx()
                        for (i in 0..2) {
                            drawRoundRect(
                                color = glowColor.copy(alpha = glowAlpha * 0.1f * (3 - i)),
                                topLeft = Offset(-i * 4f, -i * 4f),
                                size = Size(size.width + i * 8, size.height + i * 8),
                                cornerRadius = CornerRadius(radius + i * 4)
                            )
                        }
                    }
                } else Modifier
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassSurface,
                        GlassOverlay.copy(alpha = 0.5f),
                        SurfaceCard.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (glowEnabled) {
                        listOf(
                            glowColor.copy(alpha = 0.6f),
                            glowColor.copy(alpha = 0.2f),
                            glowColor.copy(alpha = 0.4f)
                        )
                    } else {
                        listOf(
                            GlassBorder.copy(alpha = 0.3f),
                            GlassBorder.copy(alpha = 0.1f),
                            GlassBorder.copy(alpha = 0.2f)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeonRed.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassCardInteractive(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    cornerRadius: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "selected_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(
                            NeonRed.copy(alpha = 0.08f),
                            NeonRed.copy(alpha = 0.03f),
                            SurfaceCard
                        )
                    } else {
                        listOf(
                            GlassSurface,
                            SurfaceCard.copy(alpha = 0.9f)
                        )
                    }
                )
            )
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        val gradientColors = listOf(
                            NeonRed.copy(alpha = pulseAlpha),
                            NeonRed.copy(alpha = pulseAlpha * 0.3f),
                            NeonRed.copy(alpha = pulseAlpha)
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(gradientColors),
                            cornerRadius = CornerRadius(cornerRadius.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                } else {
                    Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(cornerRadius))
                }
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun HudOverlay(
    modifier: Modifier = Modifier,
    showCornerBrackets: Boolean = true,
    showScanLine: Boolean = true,
    showGrid: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        if (showScanLine) {
            ScanLineEffect(
                modifier = Modifier.matchParentSize()
            )
        }

        if (showGrid) {
            GridOverlay(
                modifier = Modifier.matchParentSize(),
                gridColor = NeonRed.copy(alpha = 0.03f)
            )
        }

        if (showCornerBrackets) {
            CornerBrackets(
                modifier = Modifier.matchParentSize(),
                bracketColor = NeonCyan.copy(alpha = 0.3f)
            )
        }

        content()
    }
}

@Composable
private fun ScanLineEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    Canvas(modifier = modifier) {
        for (i in 0..10) {
            val alpha = 0.02f + (sin(i * 0.5f) * 0.01f).toFloat()
            drawLine(
                color = NeonRed.copy(alpha = alpha),
                start = Offset(0f, size.height * i / 10f),
                end = Offset(size.width, size.height * i / 10f),
                strokeWidth = 1f
            )
        }

        drawLine(
            color = NeonRed.copy(alpha = 0.1f),
            start = Offset(0f, size.height * scanY),
            end = Offset(size.width, size.height * scanY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
private fun GridOverlay(modifier: Modifier = Modifier, gridColor: Color) {
    Canvas(modifier = modifier) {
        val gridSize = 40.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSize
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSize
        }
    }
}

@Composable
private fun CornerBrackets(
    modifier: Modifier = Modifier,
    bracketColor: Color,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val padding = 12.dp.toPx()
        val cornerRadius = 8.dp.toPx()

        Path().apply {
            moveTo(padding + cornerRadius, padding)
            lineTo(padding, padding)
            lineTo(padding, padding + cornerRadius)
        }.also { path ->
            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(width = strokeWidth)
            )
        }

        Path().apply {
            moveTo(size.width - padding - cornerRadius, padding)
            lineTo(size.width - padding, padding)
            lineTo(size.width - padding, padding + cornerRadius)
        }.also { path ->
            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(width = strokeWidth)
            )
        }

        Path().apply {
            moveTo(padding, size.height - padding - cornerRadius)
            lineTo(padding, size.height - padding)
            lineTo(padding + cornerRadius, size.height - padding)
        }.also { path ->
            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(width = strokeWidth)
            )
        }

        Path().apply {
            moveTo(size.width - padding - cornerRadius, size.height - padding)
            lineTo(size.width - padding, size.height - padding)
            lineTo(size.width - padding, size.height - padding - cornerRadius)
        }.also { path ->
            drawPath(
                path = path,
                color = bracketColor,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn_glow")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    val bgColors = if (isPrimary) {
        if (enabled) {
            listOf(NeonRed, NeonRedBright, NeonRed)
        } else {
            listOf(SurfaceCard, SurfaceElevated, SurfaceCard)
        }
    } else {
        listOf(SurfaceCard, SurfaceElevated, SurfaceCard)
    }

    val textColor = if (enabled) Color.White else TextTertiary
    val glowColor = if (enabled) NeonRed else Color.Transparent

    Box(
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier.drawBehind {
                        for (i in 0..2) {
                            drawRoundRect(
                                color = glowColor.copy(alpha = glowIntensity * 0.15f * (3 - i)),
                                topLeft = Offset(-i * 3f, -i * 3f),
                                size = Size(size.width + i * 6, size.height + i * 6),
                                cornerRadius = CornerRadius(14.dp.toPx() + i * 3)
                            )
                        }
                    }
                } else Modifier
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(bgColors)
            )
            .border(
                width = 1.dp,
                color = if (enabled) NeonRed.copy(alpha = 0.5f) else BorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = textColor
        )
    }
}

@Composable
fun MetricDisplay(
    value: String,
    label: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    valueColor: Color = NeonRed,
    showGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metric_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            ),
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showGlow) {
                Box(
                    modifier = Modifier
                        .drawBehind {
                            drawCircle(
                                color = valueColor.copy(alpha = glowAlpha),
                                radius = size.minDimension / 2 + 10.dp.toPx()
                            )
                        }
                )
            }
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
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

@Composable
fun DataIndicator(
    label: String,
    value: String,
    status: IndicatorStatus = IndicatorStatus.NORMAL,
    modifier: Modifier = Modifier
) {
    val (color, bgColor) = when (status) {
        IndicatorStatus.NORMAL -> NeonCyan to NeonCyan.copy(alpha = 0.1f)
        IndicatorStatus.WARNING -> NeonRed to NeonRed.copy(alpha = 0.1f)
        IndicatorStatus.SUCCESS -> NeonGreen to NeonGreen.copy(alpha = 0.1f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(bgColor, Color.Transparent)
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

enum class IndicatorStatus {
    NORMAL, WARNING, SUCCESS
}

@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = NeonGreen,
    size: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.3f),
                    radius = this.size.minDimension / 2 * scale * 1.5f
                )
            }
            .background(color.copy(alpha = alpha), RoundedCornerShape(size / 2))
    )
}

@Composable
fun SectionDivider(
    modifier: Modifier = Modifier,
    color: Color = NeonRed
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = 0.3f),
                            color.copy(alpha = 0.6f),
                            color.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
            }
    )
}

@Composable
fun TacticalHeader(
    title: String,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = true,
    indicatorColor: Color = NeonRed
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIndicator) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(indicatorColor, indicatorColor.copy(alpha = 0f))
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = TextSecondary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BorderSubtle,
                            BorderSubtle.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
