package com.policar

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.policar.ui.screens.ActiveWorkoutScreen
import com.policar.ui.screens.DashboardScreen
import com.policar.ui.screens.HistoryScreen
import com.policar.ui.screens.HomeScreen
import com.policar.ui.screens.SplashScreen
import com.policar.ui.theme.PolicarTheme
import com.policar.ui.viewmodel.HistoryViewModel
import com.policar.ui.viewmodel.HomeViewModel
import com.policar.ui.viewmodel.WorkoutViewModel
import com.policar.data.model.TipoDeporte
import com.policar.data.model.ModoGrabacion

// ═══════════════════════════════════════════════════════════════════════
//  MAIN ACTIVITY — Punto de entrada de la aplicación POLICAR
// ═══════════════════════════════════════════════════════════════════════

object NavRoutes {
    const val SPLASH    = "splash"
    const val HOME      = "home"
    const val WORKOUT  = "workout"
    const val HISTORY  = "history"
    const val DASHBOARD = "dashboard"
}

class MainActivity : ComponentActivity() {

    // ── BLE Permission Launcher ───────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            android.util.Log.w("MainActivity", "Algunos permisos BLE denegados: $permissions")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos BLE según versión de Android
        requestBlePermissions()

        setContent {
            PolicarTheme {
                PolicarApp()
            }
        }
    }

    private fun requestBlePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    /** Forzar pantalla encendida durante entrenamiento activo */
    fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  COMPOSABLE ROOT — Navigation Graph
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun PolicarApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val workoutViewModel: WorkoutViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = NavRoutes.SPLASH,
        enterTransition  = {
            fadeIn(tween(400)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, tween(400)
            )
        },
        exitTransition   = {
            fadeOut(tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(tween(400)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, tween(400)
            )
        },
        popExitTransition  = {
            fadeOut(tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, tween(300)
            )
        }
    ) {
        // ── Splash Screen ─────────────────────────────────────────────
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Home Screen ───────────────────────────────────────────────
        composable(NavRoutes.HOME) {
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                viewModel = homeViewModel,
                historyViewModel = historyViewModel,
                onStartWorkout = { sport, mode ->
                    // Obtener deviceId: primero del campo input, luego del estado de conexión, luego del guardado
                    val currentState = homeViewModel.uiState.value
                    val deviceId = currentState.inputDeviceId.ifBlank {
                        currentState.connectionState.deviceIdOrNull ?: currentState.savedDeviceId
                    }

                    // Solo iniciar si hay un deviceId válido
                    if (deviceId.isNotBlank()) {
                        workoutViewModel.startWorkout(sport, deviceId, mode)
                    }

                    navController.navigate(NavRoutes.WORKOUT)
                },
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY)
                }
            )
        }

        // ── Active Workout Screen ─────────────────────────────────────
        composable(NavRoutes.WORKOUT) {
            ActiveWorkoutScreen(
                navController = navController,
                viewModel     = workoutViewModel
            )
        }

        // ── History Screen ───────────────────────────────────────────
        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                viewModel = historyViewModel,
                navController = navController,
                onNavigateToDashboard = {
                    navController.navigate(NavRoutes.DASHBOARD)
                }
            )
        }

        // ── Dashboard Screen ─────────────────────────────────────────
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                viewModel = historyViewModel,
                navController = navController,
                onNavigateToHistory = {
                    navController.navigate(NavRoutes.HISTORY) {
                        popUpTo(NavRoutes.DASHBOARD) { inclusive = true }
                    }
                }
            )
        }
    }
}