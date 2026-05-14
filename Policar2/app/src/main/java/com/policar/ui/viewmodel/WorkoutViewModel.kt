package com.policar.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.policar.data.model.ConnectionState
import com.policar.data.model.DatosBiomecanicos
import com.policar.data.model.ExerciseRecordingData
import com.policar.data.model.FutbolBiomechanics
import com.policar.data.model.GymBiomechanics
import com.policar.data.model.PadelBiomechanics
import com.policar.data.model.RecordingStatus
import com.policar.data.model.SesionActiva
import com.policar.data.model.SyncState
import com.policar.data.model.TipoDeporte
import com.policar.data.model.WorkoutState
import com.policar.data.model.TelemetryState
import com.policar.data.remote.SupabaseConfig
import com.policar.sensor.PolarManagerProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

private const val TAG = "WorkoutViewModel"

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val polarManager = PolarManagerProvider.get(application)
    private val supabaseClient = SupabaseConfig.supabase

    private var _uiState = MutableStateFlow(WorkoutState())
    val uiState: StateFlow<WorkoutState> = _uiState.asStateFlow()

    private val _tick = MutableStateFlow(0L)
    val tick: StateFlow<Long> = _tick.asStateFlow()

    private val heartRateHistory = mutableListOf<Int>()
    private val biomechanics = MutableStateFlow(DatosBiomecanicos(TipoDeporte.FUTBOL.displayName))

    private var timerJob: Job? = null
    private var syncJob: Job? = null

    val connectionState: StateFlow<ConnectionState> = polarManager.connectionState
    val hrValue: StateFlow<Int> = polarManager.hrValue
    val hrvStress: StateFlow<Double> = polarManager.hrvStress
    val rrIntervalMs: StateFlow<Long> = polarManager.rrIntervalMs
    val ecgSamples: StateFlow<List<Float>> = polarManager.ecgSamples
    val recordingStatus: StateFlow<RecordingStatus> = polarManager.recordingStatus
    val syncState: StateFlow<SyncState> = polarManager.syncState

    val selectedSport: TipoDeporte get() = _uiState.value.selectedSport
    val batteryLevel: Int get() = _uiState.value.batteryLevel
    val telemetry: TelemetryState get() = _uiState.value.telemetry

    val futbolBio: StateFlow<FutbolBiomechanics> = MutableStateFlow(FutbolBiomechanics()).also { flow ->
        viewModelScope.launch {
            polarManager.futbolBio.collect { flow.value = it }
        }
    }
    val padelBio: StateFlow<PadelBiomechanics> = MutableStateFlow(PadelBiomechanics()).also { flow ->
        viewModelScope.launch {
            polarManager.padelBio.collect { flow.value = it }
        }
    }
    val gymBio: StateFlow<GymBiomechanics> = MutableStateFlow(GymBiomechanics()).also { flow ->
        viewModelScope.launch {
            polarManager.gymBio.collect { flow.value = it }
        }
    }

    init {
        viewModelScope.launch {
            polarManager.hrValue.collect { hr ->
                if (hr > 0) heartRateHistory.add(hr)
            }
        }
        viewModelScope.launch {
            polarManager.biomechanics.collect { bio ->
                biomechanics.value = bio
            }
        }
    }

    fun startWorkout(sport: TipoDeporte, deviceId: String) {
        val state = SesionActiva(
            fecha_inicio = System.currentTimeMillis(),
            tipo_deporte = sport.name,
            deviceId = deviceId
        )
        _uiState.update { it.copy(isActive = true, selectedSport = sport, deviceId = deviceId) }
        polarManager.startStreaming(deviceId, sport)
        startTimer()
        Log.d(TAG, "Entrenamiento iniciado: $sport")
    }

    fun stopWorkout() {
        timerJob?.cancel()
        syncJob?.cancel()
        val deviceId = _uiState.value.deviceId
        if (deviceId.isNotEmpty()) {
            polarManager.stopStreaming()
        }
        _uiState.update { it.copy(isActive = false, isPaused = false) }
        Log.d(TAG, "Entrenamiento detenido")
    }

    fun pauseWorkout() {
        timerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
        Log.d(TAG, "Entrenamiento pausado")
    }

    fun resumeWorkout() {
        _uiState.update { it.copy(isPaused = false) }
        startTimer()
        Log.d(TAG, "Entrenamiento reanudado")
    }

    fun saveWorkout(rpe: Int) {
        val state = _uiState.value
        if (!state.isActive || state.deviceId.isEmpty()) return
        executeSyncPipeline(rpe, state, state.deviceId)
    }

    private fun executeSyncPipeline(rpe: Int, state: WorkoutState, deviceId: String) {
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(syncState = SyncState.Downloading) }
            Log.d(TAG, "[Sync Paso 1/3] Descargando ejercicios del H10")

            val exercisesData: List<ExerciseRecordingData> = suspendCancellableCoroutine { cont ->
                polarManager.fetchAllExercises(deviceId) { result ->
                    cont.resume(result)
                }
            }

            Log.d(TAG, "[Sync Paso 1] ${exercisesData.size} ejercicios descargados")

            if (exercisesData.isEmpty()) {
                Log.w(TAG, "No hay ejercicios en el H10")
                finishLiveWorkout(rpe, state, deviceId)
                return@launch
            }

            val consolidatedData = consolidateExercises(exercisesData)

            _uiState.update { it.copy(syncState = SyncState.Uploading) }
            Log.d(TAG, "[Sync Paso 2/3] Subiendo datos a Supabase")

            val endTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val startTs = dateFormat.format(Date(System.currentTimeMillis()))
            val endTs = dateFormat.format(Date(endTime))

            val validRpe = rpe.coerceIn(0, 10)
            val validDeviceId = deviceId.ifBlank { "H10_UNKNOWN" }
            val sportType = normalizeSportType(state.selectedSport.name)
            val hrSamples = if (heartRateHistory.isEmpty()) "0" else heartRateHistory.joinToString(",")
            val bio = biomechanics.value

            val bioJson = when (state.selectedSport) {
                TipoDeporte.PADEL -> """{"smash_count":${bio.smash_count},"rotacion_tronco_x":${bio.rotacion_tronco_x},"rotacion_tronco_y":${bio.rotacion_tronco_y}}"""
                TipoDeporte.FUTBOL -> """{"impacto_fuerza_g":${bio.impacto_fuerza_g},"carga_mecanica_g":${bio.carga_mecanica_g},"picos_g":${bio.picos_g_history.size}}"""
                else -> """{"repeticiones":${bio.repeticiones},"velocidad_concentrica_promedio":${bio.velocidad_concentrica_promedio}}"""
            }

            try {
                val data = mapOf(
                    "user_id" to "demo_user",
                    "device_id" to validDeviceId,
                    "sport_type" to sportType,
                    "start_time" to startTs,
                    "end_time" to endTs,
                    "duration_seconds" to state.elapsedSeconds.toString(),
                    "avg_hr" to (if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toInt() else 0).toString(),
                    "max_hr" to (heartRateHistory.maxOrNull() ?: 0).toString(),
                    "min_hr" to (heartRateHistory.filter { it > 0 }.minOrNull() ?: 0).toString(),
                    "hr_samples" to hrSamples,
                    "rpe" to validRpe.toString(),
                    "biomechanics" to bioJson,
                    "recorded_samples" to consolidatedData.hrSamples.size.toString()
                )
                supabaseClient.from("sessions").insert(data)
                Log.d(TAG, "[Sync Paso 2] Entrenamiento guardado en Supabase")
                _uiState.update { it.copy(syncState = SyncState.Success, isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                Log.e(TAG, "[Sync Paso 2] Error de upload: ${e.message}")
                _uiState.update { it.copy(
                    syncState = SyncState.Error("Upload fallido: ${e.message}"),
                    errorMessage = "No se pudo guardar el entrenamiento: ${e.message}"
                )}
                return@launch
            }

            _uiState.update { it.copy(syncState = SyncState.Clearing) }
            Log.d(TAG, "[Sync Paso 3/3] Limpiando memoria del H10")

            try {
                polarManager.removeAllExercises(deviceId)
                Log.d(TAG, "[Sync Paso 3] Memoria del H10 limpiada")
            } catch (e: Exception) {
                Log.w(TAG, "[Sync Paso 3] Error al limpiar memoria (no critico): ${e.message}")
            }

            _uiState.update { it.copy(syncState = SyncState.Success, savedSuccessfully = true) }
        }
    }

    private suspend fun finishLiveWorkout(rpe: Int, state: WorkoutState, deviceId: String) {
        Log.d(TAG, "[Fallback] Subiendo datos del stream HR en vivo")
        _uiState.update { it.copy(syncState = SyncState.Uploading) }

        val endTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val startTs = dateFormat.format(Date(System.currentTimeMillis()))
        val endTs = dateFormat.format(Date(endTime))

        val validRpe = rpe.coerceIn(0, 10)
        val sportType = normalizeSportType(state.selectedSport.name)
        val hrSamples = if (heartRateHistory.isEmpty()) "0" else heartRateHistory.joinToString(",")
        val bio = biomechanics.value

        val bioJson = when (state.selectedSport) {
            TipoDeporte.PADEL -> """{"smash_count":${bio.smash_count}}"""
            TipoDeporte.FUTBOL -> """{"impacto_fuerza_g":${bio.impacto_fuerza_g}}"""
            else -> """{"repeticiones":${bio.repeticiones}}"""
        }

        try {
            val data = mapOf(
                "user_id" to "demo_user",
                "device_id" to deviceId.ifBlank { "H10_UNKNOWN" },
                "sport_type" to sportType,
                "start_time" to startTs,
                "end_time" to endTs,
                "duration_seconds" to state.elapsedSeconds.toString(),
                "avg_hr" to (if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toInt() else 0).toString(),
                "max_hr" to (heartRateHistory.maxOrNull() ?: 0).toString(),
                "min_hr" to (heartRateHistory.filter { it > 0 }.minOrNull() ?: 0).toString(),
                "hr_samples" to hrSamples,
                "rpe" to validRpe.toString(),
                "biomechanics" to bioJson,
                "recorded_samples" to "0"
            )
            supabaseClient.from("sessions").insert(data)
            _uiState.update { it.copy(syncState = SyncState.Success, savedSuccessfully = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(syncState = SyncState.Error("Fallback fallido: ${e.message}")) }
        }
    }

    private fun normalizeSportType(raw: String): String = when (raw) {
        "FUTBOL", "Football" -> "FUTBOL"
        "PADEL", "Padel" -> "PADEL"
        "GIMNASIO", "Gym", "GYM" -> "GIMNASIO"
        else -> raw
    }

    private fun consolidateExercises(exercises: List<ExerciseRecordingData>): ExerciseRecordingData {
        if (exercises.isEmpty()) return ExerciseRecordingData("", emptyList(), 1, 0, 0, 0, 0, "")
        if (exercises.size == 1) return exercises.first()

        val allSamples = exercises.flatMap { it.hrSamples }
        val intervalSec = exercises.map { it.intervalSeconds }.average().toInt()
        val totalDuration = allSamples.size * intervalSec
        val avgHr = if (allSamples.isNotEmpty()) allSamples.average().toInt() else 0
        val maxHr = allSamples.maxOrNull() ?: 0
        val minHr = allSamples.filter { it > 0 }.minOrNull() ?: 0

        return ExerciseRecordingData(
            entryPath = "consolidated",
            hrSamples = allSamples,
            intervalSeconds = intervalSec,
            totalDurationSeconds = totalDuration,
            avgHr = avgHr,
            maxHr = maxHr,
            minHr = minHr,
            sparklineCsv = allSamples.joinToString(",")
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val state = _uiState.value
                if (state.isActive) {
                    val newDuration = state.elapsedSeconds + 1L
                    _uiState.update { it.copy(elapsedSeconds = newDuration) }
                    _tick.value = newDuration
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSyncState() {
        _uiState.update { it.copy(syncState = SyncState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        syncJob?.cancel()
        Log.d(TAG, "WorkoutViewModel limpiado")
    }
}