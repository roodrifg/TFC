

package com.policar.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.policar.data.PolicarPreferencesRepository
import com.policar.data.model.ConnectionState
import com.policar.data.model.ModoGrabacion
import com.policar.data.model.TipoDeporte
import com.policar.sensor.PolarManagerProvider
import com.polar.sdk.api.model.PolarDeviceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
//  POLICAR — HOME VIEW MODEL
//
//  Responsabilidades:
//  ① DataStore: persiste y restaura el deviceId del H10 entre sesiones
//  ② Conexión BLE: gestiona connect/disconnect con retroalimentación de estado
//  ③ Auto-Reconexión: cuando la conexión se pierde en modo "en vivo", reintenta
//     automáticamente con backoff exponencial (máx. 5 intentos)
//  ④ Selección de deporte + modo de grabación antes del entrenamiento
//  ⑤ UI State: expone HomeUiState como StateFlow para HomeScreen
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG                   = "HomeViewModel"
private const val MAX_RECONNECT_ATTEMPTS = 5
private const val RECONNECT_BASE_DELAY_MS = 3_000L    // 3s inicial
private const val RECONNECT_MAX_DELAY_MS  = 30_000L   // 30s máximo

data class HomeUiState(
    val connectionState: ConnectionState               = ConnectionState.Disconnected,
    val savedDeviceId: String                      = "",
    val inputDeviceId: String                      = "",
    val discoveredDevices: List<PolarDeviceInfo>      = emptyList(),
    val selectedDeporte: TipoDeporte?               = null,
    val selectedModo: ModoGrabacion                 = ModoGrabacion.EN_VIVO,
    val canStartWorkout: Boolean                     = false,
    val showReconnectBanner: Boolean                = false,
    val reconnectAttempt: Int                     = 0,
    val errorMessage: String?                     = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependencias ──────────────────────────────────────────────────────────
    private val polarManager = PolarManagerProvider.get(application)
    private val prefsRepo    = PolicarPreferencesRepository(application)

    // ── State interno ─────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Job para el bucle de auto-reconexión (cancelable)
    private var reconnectJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    //  INIT — Observar DataStore + Estado de conexión BLE
    // ─────────────────────────────────────────────────────────────────────────

    init {
        // Cargar deviceId y preferencias guardadas desde DataStore
        viewModelScope.launch {
            combine(
                prefsRepo.deviceIdFlow,
                prefsRepo.lastSportFlow,
                prefsRepo.lastModeFlow
            ) { savedId, lastSport, lastMode ->
                Triple(savedId, lastSport, lastMode)
            }.collect { (savedId, lastSport, lastMode) ->
                val sport = runCatching { TipoDeporte.valueOf(lastSport) }.getOrNull()
                    ?: TipoDeporte.FUTBOL
                val modo  = runCatching { ModoGrabacion.valueOf(lastMode) }.getOrNull()
                    ?: ModoGrabacion.EN_VIVO

                _uiState.update { state ->
                    state.copy(
                        savedDeviceId  = savedId,
                        inputDeviceId  = if (state.inputDeviceId.isBlank()) savedId else state.inputDeviceId,
                        selectedDeporte = sport,
                        selectedModo   = modo
                    )
                }

                // Si hay un deviceId guardado y no estamos conectados → intentar reconexión inicial
                if (savedId.isNotBlank() && !polarManager.connectionState.value.isConnected) {
                    Log.d(TAG, "🔁 Intentando reconexión automática con deviceId guardado: $savedId")
                    polarManager.connect(savedId)
                }
            }
        }

        // Observar estado de conexión BLE y actualizar UI
        viewModelScope.launch {
            polarManager.connectionState.collect { connState ->
                val savedId = _uiState.value.savedDeviceId
                val deporte = _uiState.value.selectedDeporte

                _uiState.update { state ->
                    state.copy(
                        connectionState  = connState,
                        canStartWorkout  = connState.isConnected && deporte != null,
                        showReconnectBanner = connState is ConnectionState.Reconnecting,
                        reconnectAttempt = (connState as? ConnectionState.Reconnecting)?.attempt ?: 0,
                        errorMessage     = (connState as? ConnectionState.Error)?.message
                    )
                }

                when (connState) {
                    is ConnectionState.Scanning -> {
                        // Escaneo activo - no hacer nada especial en UI
                    }
                    is ConnectionState.Disconnected -> {
                        // Solo iniciar auto-reconexión si teníamos un deviceId guardado y
                        // la desconexión no fue intencional (reconnectJob no activo ya)
                        if (savedId.isNotBlank() && reconnectJob?.isActive != true) {
                            Log.d(TAG, "⚡ Desconexión inesperada — iniciando auto-reconexión")
                            startAutoReconnect(savedId)
                        }
                    }
                    is ConnectionState.Connected -> {
                        // Conexión exitosa → cancelar job de reconexión
                        reconnectJob?.cancel()
                        reconnectJob = null
                        Log.d(TAG, "✅ Conectado, job de reconexión cancelado")
                    }
                    is ConnectionState.Error -> {
                        Log.e(TAG, "❌ Error BLE: ${connState.message}")
                    }
                    else -> { /* Connecting / Reconnecting — no acción adicional */ }
                }
            }
        }

        // Observar dispositivos descubiertos durante escaneo
        viewModelScope.launch {
            polarManager.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(discoveredDevices = devices) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACCIONES PÚBLICAS — Llamadas desde HomeScreen
    // ─────────────────────────────────────────────────────────────────────────

    /** Actualiza el campo de texto del deviceId en la UI */
    fun onDeviceIdInputChanged(input: String) {
        _uiState.update { it.copy(inputDeviceId = input.uppercase()) }
    }

    /**
     * Conecta al sensor. Usa el deviceId guardado o el último conectado.
     */
    fun connectDevice() {
        val deviceId = _uiState.value.inputDeviceId.trim().uppercase()
            .ifBlank { _uiState.value.savedDeviceId }

        if (deviceId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No hay sensor guardado") }
            return
        }

        Log.d(TAG, "Conectando a: $deviceId")
        reconnectJob?.cancel()

        viewModelScope.launch {
            prefsRepo.saveDeviceId(deviceId)
        }
        polarManager.connect(deviceId)
        _uiState.update { it.copy(errorMessage = null, inputDeviceId = deviceId) }
    }

    /** Inicia el escaneo BLE para buscar dispositivos Polar cercanos */
    fun startDeviceScan() {
        Log.d(TAG, "Iniciando escaneo de dispositivos...")
        _uiState.update { it.copy(errorMessage = null) }
        polarManager.startDeviceSearch()
    }

    /** Detiene el escaneo BLE */
    fun stopDeviceScan() {
        Log.d(TAG, "Deteniendo escaneo...")
        polarManager.stopDeviceSearch()
    }

    /** Conecta a un dispositivo discovered */
    fun connectToDiscoveredDevice(device: PolarDeviceInfo) {
        val deviceId = device.deviceId
        Log.d(TAG, "Conectando a dispositivo descubierto: $deviceId")
        polarManager.stopDeviceSearch()

        viewModelScope.launch {
            prefsRepo.saveDeviceId(deviceId)
        }
        polarManager.connect(deviceId)
        _uiState.update { it.copy(inputDeviceId = deviceId, errorMessage = null) }
    }

    /**
     * Desconecta el sensor de forma intencionada.
     * Cancela la auto-reconexión y limpia el deviceId guardado.
     */
    fun disconnectDevice() {
        val deviceId = _uiState.value.connectionState.deviceIdOrNull
            ?: _uiState.value.savedDeviceId

        Log.d(TAG, "🔌 Desconectando intencionalmente: $deviceId")
        reconnectJob?.cancel()    // Detener reconexión automática
        reconnectJob = null

        if (deviceId.isNotBlank()) {
            polarManager.disconnect()
        }

        viewModelScope.launch {
            prefsRepo.clearDeviceId()    // Limpiar del DataStore al desconectar manualmente
        }

        _uiState.update { it.copy(
            savedDeviceId  = "",
            inputDeviceId  = "",
            errorMessage   = null
        )}
    }

    /** Selecciona el deporte y lo persiste en DataStore */
    fun selectDeporte(deporte: TipoDeporte) {
        val isConnected = _uiState.value.connectionState.isConnected
        _uiState.update { state ->
            state.copy(
                selectedDeporte = deporte,
                canStartWorkout = isConnected,
                // Si Gimnasio → forzar modo EN_VIVO (no soporta offline)
                selectedModo    = if (!deporte.supportsOffline) ModoGrabacion.EN_VIVO
                else state.selectedModo
            )
        }
        viewModelScope.launch { prefsRepo.saveLastSport(deporte) }
    }

    /** Selecciona el modo de grabación y lo persiste en DataStore */
    fun selectModo(modo: ModoGrabacion) {
        _uiState.update { it.copy(selectedModo = modo) }
        viewModelScope.launch { prefsRepo.saveLastMode(modo) }
    }

    /** Limpia el mensaje de error actual */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AUTO-RECONEXIÓN — Backoff exponencial
    //
    //  Intenta reconectar hasta MAX_RECONNECT_ATTEMPTS veces con esperas
    //  exponenciales: 3s → 6s → 12s → 24s → 30s (máx.)
    // ─────────────────────────────────────────────────────────────────────────

    private fun startAutoReconnect(deviceId: String) {
        if (deviceId.isBlank()) return

        reconnectJob = viewModelScope.launch {
            var attempt = 0
            while (attempt < MAX_RECONNECT_ATTEMPTS) {
                attempt++
                val delayMs = (RECONNECT_BASE_DELAY_MS * (1 shl (attempt - 1)))
                    .coerceAtMost(RECONNECT_MAX_DELAY_MS)

                Log.d(TAG, "⏳ Reconexión intento $attempt/$MAX_RECONNECT_ATTEMPTS en ${delayMs}ms")

                _uiState.update { it.copy(
                    showReconnectBanner = true,
                    reconnectAttempt    = attempt
                )}

                delay(delayMs)

                // Comprobar si se reconectó mientras esperábamos
                if (polarManager.connectionState.value.isConnected) {
                    Log.d(TAG, "✅ Ya conectado antes del intento $attempt — abortando bucle")
                    break
                }

                polarManager.reconnect(deviceId, attempt)

                // Esperar un momento para que el callback de conexión llegue
                delay(2_000L)

                if (polarManager.connectionState.value.isConnected) {
                    Log.d(TAG, "✅ Reconexión exitosa en intento $attempt")
                    break
                }
            }

            // Si agotamos los intentos sin éxito
            if (!polarManager.connectionState.value.isConnected) {
                Log.e(TAG, "❌ Auto-reconexión fallida tras $MAX_RECONNECT_ATTEMPTS intentos")
                _uiState.update { it.copy(
                    showReconnectBanner = false,
                    reconnectAttempt    = 0,
                    errorMessage        = "No se pudo reconectar al sensor. Verifica que el H10 está encendido."
                )}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
        Log.d(TAG, "HomeViewModel limpiado")
    }
}