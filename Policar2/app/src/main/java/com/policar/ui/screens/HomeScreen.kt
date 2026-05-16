package com.policar.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.policar.R
import com.policar.data.model.ConnectionState
import com.policar.data.model.ModoGrabacion
import com.policar.data.model.TipoDeporte
import com.policar.ui.components.*
import com.policar.ui.viewmodel.HomeUiState
import com.policar.ui.viewmodel.HomeViewModel
import com.policar.ui.viewmodel.HistoryViewModel
import com.policar.ui.theme.*
import com.polar.sdk.api.model.PolarDeviceInfo

private val BorderActive = Color(0x40FF0022)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    historyViewModel: HistoryViewModel,
    onStartWorkout: (TipoDeporte, ModoGrabacion) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isCompact = screenWidth < 360
    val isTablet = screenWidth >= 600

    val horizontalPadding = when {
        isTablet -> 48.dp
        isCompact -> 12.dp
        else -> 20.dp
    }

    val scaleFactor = if (isTablet) 1.2f else if (isCompact) 0.85f else 1f
    val screenWidthDp = screenWidth.dp
    val screenHeightDp = screenHeight.dp

    var showContent by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        showContent = true
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
            .onSizeChanged { containerSize = it }
    ) {
        val glowSize = screenWidthDp * 0.8f

        Box(
            modifier = Modifier
                .size(glowSize)
                .offset(x = (screenWidthDp - glowSize) / 2, y = (-glowSize / 4))
                .alpha(0.06f)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonRed, Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(glowSize * 0.6f)
                .offset(x = screenWidthDp - glowSize * 0.3f, y = screenHeightDp * 0.6f)
                .alpha(0.05f)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan, Color.Transparent)
                    )
                )
        )

        HudOverlay(
            modifier = Modifier.fillMaxSize(),
            showCornerBrackets = false,
            showScanLine = true,
            showGrid = true
        ) { }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = horizontalPadding)
                .verticalScroll(rememberScrollState(), enabled = false)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            HeaderSection(
                connectionState = uiState.connectionState,
                onDisconnect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.disconnectDevice()
                },
                scaleFactor = scaleFactor
            )

            Spacer(modifier = Modifier.height((24 * scaleFactor).dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500) { it / 3 })
            ) {
                ConnectionSection(
                    uiState = uiState,
                    scaleFactor = scaleFactor,
                    onScanClick = { viewModel.startDeviceScan() }
                )

                DiscoveredDevicesList(
                    devices = uiState.discoveredDevices,
                    onDeviceClick = { viewModel.connectToDiscoveredDevice(it) },
                    scaleFactor = scaleFactor
                )
            }

            Spacer(modifier = Modifier.height((28 * scaleFactor).dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 100)) + slideInVertically(tween(500, delayMillis = 100) { it / 3 })
            ) {
                Column {
                    SectionHeader(text = "SELECT SPORT", scaleFactor = scaleFactor)
                    Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
                }
            }

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 150)) + slideInVertically(tween(500, delayMillis = 150) { it / 3 })
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = (280 * scaleFactor).dp)
                ) {
                    itemsIndexed(TipoDeporte.entries.toList()) { index, sport ->
                        val delay = 200 + (index * 80)
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(300, delayMillis = delay)) + slideInHorizontally(
                                tween(300, delayMillis = delay) { it / 3 }
                            )
                        ) {
                            SportCard(
                                sport = sport,
                                isSelected = uiState.selectedDeporte == sport,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectDeporte(sport)
                                },
                                scaleFactor = scaleFactor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

            AnimatedVisibility(
                visible = uiState.selectedDeporte?.supportsOffline == true && showContent,
                enter = fadeIn(tween(300, delayMillis = 400)) + expandVertically()
            ) {
                ModeSelector(
                    selectedMode = uiState.selectedModo,
                    onModeSelected = { mode ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.selectModo(mode)
                    },
                    scaleFactor = scaleFactor
                )
            }

            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 450)) + slideInVertically(tween(500, delayMillis = 450) { it / 2 })
            ) {
                StartButton(
                    enabled = uiState.canStartWorkout,
                    mode = uiState.selectedModo,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sport = uiState.selectedDeporte ?: return@StartButton
                        val mode = uiState.selectedModo
                        onStartWorkout(sport, mode)
                    },
                    scaleFactor = scaleFactor
                )
            }

            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, delayMillis = 500))
            ) {
                HistoryButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToHistory()
                    },
                    scaleFactor = scaleFactor
                )
            }

            Spacer(modifier = Modifier.height((32 * scaleFactor).dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String, scaleFactor: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width((3 * scaleFactor).dp)
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
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = (10 * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = TextTertiary
        )
    }
}

@Composable
private fun HeaderSection(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_policar_sin_fondo),
            contentDescription = "POLICAR",
            modifier = Modifier
                .height((36 * scaleFactor).dp)
                .width((36 * scaleFactor).dp * 3),
            contentScale = ContentScale.Fit
        )

        ConnectionChip(
            state = connectionState,
            onDisconnect = onDisconnect,
            scaleFactor = scaleFactor
        )
    }
}

@Composable
private fun ConnectionChip(
    state: ConnectionState,
    onDisconnect: () -> Unit,
    scaleFactor: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    val (indicatorColor, label, showDot) = when (state) {
        is ConnectionState.Scanning -> Triple(NeonCyan, "SEARCHING", true)
        is ConnectionState.Connected -> Triple(NeonGreen, "ONLINE", true)
        is ConnectionState.Connecting -> Triple(NeonYellow, "CONNECTING", true)
        is ConnectionState.Reconnecting -> Triple(NeonOrange, "RECONNECTING", true)
        is ConnectionState.Error -> Triple(NeonRed, "ERROR", true)
        ConnectionState.Disconnected -> Triple(TextTertiary, "OFFLINE", false)
    }

    val isActive = when (state) {
        is ConnectionState.Scanning -> true
        is ConnectionState.Connecting -> true
        is ConnectionState.Reconnecting -> true
        else -> false
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape((20 * scaleFactor).dp),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.25f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = (10 * scaleFactor).dp, vertical = (6 * scaleFactor).dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size((6 * scaleFactor).dp)
                        .background(indicatorColor, CircleShape)
                        .alpha(if (isActive) pulse else 0.9f)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (9 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = indicatorColor
            )
            if (state is ConnectionState.Connected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnect",
                    tint = indicatorColor.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size((12 * scaleFactor).dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDisconnect
                        )
                )
            }
        }
    }
}

@Composable
private fun ConnectionSection(
    uiState: HomeUiState,
    scaleFactor: Float,
    onScanClick: () -> Unit
) {
    val isConnected = uiState.connectionState.isConnected
    val isSearching = uiState.connectionState.isSearching
    val deviceId = uiState.connectionState.deviceIdOrNull

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((16 * scaleFactor).dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SurfaceCard, SurfaceDark)
                )
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape((16 * scaleFactor).dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { if (!isConnected) onScanClick() }
            )
            .padding((16 * scaleFactor).dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size((48 * scaleFactor).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = when {
                                isConnected -> listOf(NeonGreen.copy(alpha = 0.15f), NeonGreen.copy(alpha = 0.02f))
                                isSearching -> listOf(NeonYellow.copy(alpha = 0.15f), NeonYellow.copy(alpha = 0.02f))
                                else -> listOf(SurfaceElevated, SurfaceCard)
                            }
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            isConnected -> NeonGreen.copy(alpha = 0.3f)
                            isSearching -> NeonYellow.copy(alpha = 0.3f)
                            else -> BorderSubtle
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size((22 * scaleFactor).dp),
                        color = NeonYellow,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = when {
                            isConnected -> NeonGreen
                            isSearching -> NeonYellow
                            else -> TextTertiary
                        },
                        modifier = Modifier.size((22 * scaleFactor).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width((14 * scaleFactor).dp))

            Column {
                Text(
                    text = "POLAR H10",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (13 * scaleFactor).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        isConnected -> deviceId ?: "Connected"
                        isSearching -> "Scanning..."
                        else -> "Tap to scan"
                    },
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (10 * scaleFactor).sp
                    ),
                    color = when {
                        isConnected -> NeonGreen
                        isSearching -> NeonYellow
                        else -> TextTertiary
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isConnected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier
                        .size((20 * scaleFactor).dp)
                        .alpha(0.8f)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.BluetoothDisabled,
                    contentDescription = "Scan",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size((20 * scaleFactor).dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoveredDevicesList(
    devices: List<PolarDeviceInfo>,
    onDeviceClick: (PolarDeviceInfo) -> Unit,
    scaleFactor: Float
) {
    if (devices.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height((12 * scaleFactor).dp))

        devices.forEach { device ->
            DeviceListItem(
                device = device,
                onClick = { onDeviceClick(device) },
                scaleFactor = scaleFactor
            )
            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
        }
    }
}

@Composable
private fun DeviceListItem(
    device: PolarDeviceInfo,
    onClick: () -> Unit,
    scaleFactor: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((12 * scaleFactor).dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape((12 * scaleFactor).dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding((12 * scaleFactor).dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size((18 * scaleFactor).dp)
            )

            Spacer(modifier = Modifier.width((10 * scaleFactor).dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Polar H10",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (11 * scaleFactor).sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextPrimary
                )
                Text(
                    text = device.deviceId,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (9 * scaleFactor).sp
                    ),
                    color = TextTertiary
                )
            }

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Connect",
                tint = NeonCyan,
                modifier = Modifier.size((18 * scaleFactor).dp)
            )
        }
    }
}

@Composable
private fun SportCard(
    sport: TipoDeporte,
    isSelected: Boolean,
    onClick: () -> Unit,
    scaleFactor: Float
) {
    val cardHeight = (80 * scaleFactor).dp

    GlassCardInteractive(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        isSelected = isSelected,
        onClick = onClick,
        cornerRadius = (14 * scaleFactor).dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val sportColor = when (sport) {
                TipoDeporte.FUTBOL -> SportFutbol
                TipoDeporte.PADEL -> SportPadel
                TipoDeporte.GIMNASIO -> SportGym
            }

            Box(
                modifier = Modifier
                    .size((44 * scaleFactor).dp)
                    .drawBehind {
                        if (isSelected) {
                            drawCircle(
                                color = NeonRed.copy(alpha = 0.2f),
                                radius = size.minDimension / 2 + 8.dp.toPx()
                            )
                        }
                    }
                    .background(
                        color = if (isSelected) NeonRed.copy(alpha = 0.12f) else SurfaceElevated,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) NeonRed.copy(alpha = 0.5f) else BorderSubtle,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val sportIcon = when (sport) {
                    TipoDeporte.FUTBOL -> Icons.Default.SportsSoccer
                    TipoDeporte.PADEL -> Icons.Default.SportsTennis
                    TipoDeporte.GIMNASIO -> Icons.Default.FitnessCenter
                }
                Icon(
                    imageVector = sportIcon,
                    contentDescription = null,
                    tint = if (isSelected) NeonRed else Color.White,
                    modifier = Modifier.size((28 * scaleFactor).dp)
                )
            }

            Spacer(modifier = Modifier.width((14 * scaleFactor).dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sport.displayName.uppercase(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (15 * scaleFactor).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = if (isSelected) NeonRed else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size((4 * scaleFactor).dp)
                            .background(
                                if (sport.supportsOffline) NeonRed else TextTertiary,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (sport.supportsOffline) "OFFLINE ENABLED" else "LIVE ONLY",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = (8 * scaleFactor).sp,
                            letterSpacing = 1.sp
                        ),
                        color = if (sport.supportsOffline) NeonRed.copy(alpha = 0.6f) else TextTertiary
                    )
                }
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .size((24 * scaleFactor).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(NeonRed, NeonRedDark)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((14 * scaleFactor).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    selectedMode: ModoGrabacion,
    onModeSelected: (ModoGrabacion) -> Unit,
    scaleFactor: Float
) {
    Column {
        SectionHeader(text = "RECORDING MODE", scaleFactor = scaleFactor)
        Spacer(modifier = Modifier.height((10 * scaleFactor).dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
        ) {
            ModoGrabacion.entries.toList().forEach { mode ->
                val isSelected = mode == selectedMode
                val cornerRadius = RoundedCornerShape((12 * scaleFactor).dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(cornerRadius)
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        NeonRed.copy(alpha = 0.1f),
                                        NeonRed.copy(alpha = 0.04f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(SurfaceCard, SurfaceDark)
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonRed.copy(alpha = 0.5f) else BorderSubtle,
                            shape = cornerRadius
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onModeSelected(mode) }
                        )
                        .padding(vertical = (12 * scaleFactor).dp, horizontal = (12 * scaleFactor).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (mode) {
                                ModoGrabacion.EN_VIVO -> Icons.Default.Cloud
                                ModoGrabacion.OFFLINE -> Icons.Default.Storage
                            },
                            contentDescription = null,
                            tint = if (isSelected) NeonRed else TextTertiary,
                            modifier = Modifier.size((16 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width((6 * scaleFactor).dp))
                        Text(
                            text = when (mode) {
                                ModoGrabacion.EN_VIVO -> "LIVE"
                                ModoGrabacion.OFFLINE -> "RECORD"
                            },
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = (11 * scaleFactor).sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (isSelected) NeonRed else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartButton(
    enabled: Boolean,
    mode: ModoGrabacion,
    onClick: () -> Unit,
    scaleFactor: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )

    val buttonHeight = (56 * scaleFactor).dp
    val cornerRadius = RoundedCornerShape((14 * scaleFactor).dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight + (8 * scaleFactor).dp)
    ) {
        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .align(Alignment.BottomCenter)
                    .alpha(glow * 0.5f)
                    .shadow((24 * scaleFactor).dp, RoundedCornerShape((14 * scaleFactor).dp), spotColor = NeonRed)
            )
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight)
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TextTertiary
            ),
            contentPadding = PaddingValues(0.dp),
            shape = cornerRadius
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (enabled) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NeonRedDark,
                                    NeonRed,
                                    NeonRedDark
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(SurfaceCard, SurfaceElevated, SurfaceCard)
                            )
                        },
                        shape = cornerRadius
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (enabled) {
                        Icon(
                            imageVector = if (mode == ModoGrabacion.EN_VIVO) Icons.Default.PlayArrow
                                         else Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size((20 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
                    }
                    Text(
                        text = when {
                            !enabled -> "SELECT SPORT TO CONTINUE"
                            mode == ModoGrabacion.EN_VIVO -> "START TELEMETRY"
                            else -> "START RECORDING"
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = (13 * scaleFactor).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (enabled) Color.White else TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryButton(
    onClick: () -> Unit,
    scaleFactor: Float
) {
    val buttonHeight = (48 * scaleFactor).dp
    val cornerRadius = RoundedCornerShape((12 * scaleFactor).dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .clip(cornerRadius)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(SurfaceCard, SurfaceDark)
                )
            )
            .border(1.dp, BorderActive.copy(alpha = 0.3f), cornerRadius)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size((18 * scaleFactor).dp)
            )
            Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
            Text(
                text = "VIEW HISTORY & ANALYTICS",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = (11 * scaleFactor).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = TextSecondary
            )
        }
    }
}