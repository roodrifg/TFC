package com.policar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.policar.ui.theme.NeonRed
import com.policar.ui.theme.TextTertiary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val glow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "g"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(1200),
        label = "la"
    )

    LaunchedEffect(Unit) {
        delay(1800)
        onSplashComplete()
    }

    val context = LocalContext.current
    val logoResId = remember {
        context.resources.getIdentifier("p_policar_sin_fondo", "drawable", context.packageName)
    }
    val hasLogo = remember(logoResId) { logoResId != 0 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
                .alpha(glow * 0.2f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonRed.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(100.dp)
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.Center)
                .alpha(glow * 0.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonRed, Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .blur(50.dp)
        )

        if (hasLogo) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "POLICAR",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(160.dp)
                    .alpha(logoAlpha),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "v1.0.0",
            fontSize = 8.sp,
            color = TextTertiary.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(logoAlpha * 0.5f)
        )
    }
}