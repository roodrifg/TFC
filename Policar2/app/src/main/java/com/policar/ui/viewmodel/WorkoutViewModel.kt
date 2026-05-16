package com.policar.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.policar.data.model.ConnectionState
import com.policar.data.model.DatosBiomecanicos
import com.policar.data.model.Entrenamiento
import com.policar.data.model.ExerciseRecordingData
import com.policar.data.model.FutbolBiomechanics
import com.policar.data.model.GymBiomechanics
import com.policar.data.model.PadelBiomechanics
import com.policar.data.model.RecordingStatus
import com.policar.data.model.SesionActiva
import com.policar.data.model.SyncState
import com.policar.data.model.TipoDeporte
import com.policar.data.model.WorkoutState
import com.policar.data.model.WorkoutSummary
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
        val startTime = System.currentTimeMillis()
        val state = SesionActiva(
            fecha_inicio = startTime,
            tipo_deporte = sport.name,
            deviceId = deviceId
        )
        _uiState.update { it.copy(isActive = true, selectedSport = sport, deviceId = deviceId, startTime = startTime) }
        polarManager.startStreaming(deviceId, sport)
        startTimer()
        Log.d(TAG, "Entrenamiento iniciado: $sport en ${startTime}")
    }

    fun stopWorkout() {
        timerJob?.cancel()
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
        if (state.deviceId.isEmpty()) return
        
        timerJob?.cancel()
        val deviceId = state.deviceId
        if (deviceId.isNotEmpty()) {
            polarManager.stopStreaming()
        }
        _uiState.update { it.copy(isActive = false, isPaused = false, isSaving = true) }
        executeSyncPipeline(rpe, state, deviceId)
    }

    private fun executeSyncPipeline(rpe: Int, state: WorkoutState, deviceId: String) {
        syncJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(syncState = SyncState.Downloading) }
                Log.d(TAG, "[Sync Paso 1/3] Verificando ejercicios del H10")

                var exercisesData: List<ExerciseRecordingData> = emptyList()
                try {
                    exercisesData = suspendCancellableCoroutine { cont ->
                        val timeoutJob = launch {
                            delay(5_000L)
                            cont.resume(emptyList())
                        }
                        polarManager.fetchAllExercises(deviceId) { result ->
                            timeoutJob.cancel()
                            cont.resume(result)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error al descargar ejercicios: ${e.message}")
                }

                Log.d(TAG, "[Sync Paso 1] ${exercisesData.size} ejercicios descargados")

                if (exercisesData.isEmpty()) {
                    Log.w(TAG, "No hay ejercicios en el H10, guardando modo EN_VIVO")
                    finishLiveWorkout(rpe, state, deviceId)
                    return@launch
                }

                val consolidatedData = consolidateExercises(exercisesData)

                _uiState.update { it.copy(syncState = SyncState.Uploading) }
                Log.d(TAG, "[Sync Paso 2/3] Subiendo datos a Supabase")

                val validRpe = rpe.coerceIn(0, 10)
                val validDeviceId = deviceId.ifBlank { "H10_UNKNOWN" }
                val sportType = normalizeSportType(state.selectedSport.name)
                val hrSamples = if (heartRateHistory.isEmpty()) "0" else heartRateHistory.joinToString(",")
                val bio = biomechanics.value

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                val startTs = dateFormat.format(Date(state.startTime))
                val endTs = dateFormat.format(Date(System.currentTimeMillis()))
                val hrAvg = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toInt() else 0
                val hrMax = heartRateHistory.maxOrNull() ?: 0
                val hrMin = heartRateHistory.filter { it > 0 }.minOrNull() ?: 0

                Log.d(TAG, "Preparando datos para Supabase:")
                Log.d(TAG, "  - device_id: $validDeviceId")
                Log.d(TAG, "  - sport_type: $sportType")
                Log.d(TAG, "  - start_timestamp: $startTs")
                Log.d(TAG, "  - end_timestamp: $endTs")
                Log.d(TAG, "  - duration_seconds: ${state.elapsedSeconds}")

                val bioJson = """{"carga_mecanica_g":${bio.carga_mecanica_g},"impacto_fuerza_g":${bio.impacto_fuerza_g},"rotacion_tronco_x":${bio.rotacion_tronco_x},"rotacion_tronco_y":${bio.rotacion_tronco_y},"repeticiones":${bio.repeticiones},"velocidad_concentrica_promedio":${bio.velocidad_concentrica_promedio}}"""

                val entrenamiento = Entrenamiento(
                    device_id = validDeviceId,
                    sport_type = sportType,
                    start_timestamp = startTs,
                    end_timestamp = endTs,
                    duration_seconds = state.elapsedSeconds.toInt(),
                    hr_avg = hrAvg,
                    hr_max = hrMax,
                    hr_min = hrMin,
                    hr_samples = hrSamples,
                    rpe = validRpe,
                    futbol_biomechanics = bioJson
                )
                Log.d(TAG, "Insertando en Supabase tabla 'entrenamientos'...")
                supabaseClient.from("entrenamientos").insert(entrenamiento)
                Log.d(TAG, "[Sync Paso 2] Entrenamiento guardado en Supabase")

                val summary = WorkoutSummary(
                    sportType = sportType,
                    startTime = state.startTime,
                    endTime = state.startTime + (state.elapsedSeconds * 1000),
                    durationSeconds = state.elapsedSeconds.toInt(),
                    avgHr = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toInt() else 0,
                    maxHr = heartRateHistory.maxOrNull() ?: 0,
                    rpe = validRpe
                )
                _uiState.update { it.copy(
                    syncState = SyncState.Success,
                    isSaving = false,
                    savedSuccessfully = true,
                    savedWorkoutSummary = summary
                ) }

                _uiState.update { it.copy(syncState = SyncState.Clearing) }
                Log.d(TAG, "[Sync Paso 3/3] Limpiando memoria del H10")

                try {
                    polarManager.removeAllExercises(deviceId)
                    Log.d(TAG, "[Sync Paso 3] Memoria del H10 limpiada")
                } catch (e: Exception) {
                    Log.w(TAG, "[Sync Paso 3] Error al limpiar memoria (no critico): ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "[Sync] Error general: ${e.message}")
                Log.e(TAG, "[Sync] Stack: ${e.stackTraceToString()}")
                _uiState.update { it.copy(
                    syncState = SyncState.Error("Error: ${e.message}"),
                    isSaving = false,
                    errorMessage = "Error al guardar: ${e.message}"
                ) }
            }
        }
    }

    private suspend fun finishLiveWorkout(rpe: Int, state: WorkoutState, deviceId: String) {
        Log.d(TAG, "[Fallback] Subiendo datos del stream HR en vivo")
        _uiState.update { it.copy(syncState = SyncState.Uploading) }

        val validRpe = rpe.coerceIn(0, 10)
        val sportType = normalizeSportType(state.selectedSport.name)
        val hrSamples = if (heartRateHistory.isEmpty()) "0" else heartRateHistory.joinToString(",")
        val bio = biomechanics.value

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val startTs = dateFormat.format(Date(state.startTime))
            val endTs = dateFormat.format(Date(System.currentTimeMillis()))
            val hrAvg = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toDouble() else 0.0
            val hrMax = heartRateHistory.maxOrNull() ?: 0
            val hrMin = heartRateHistory.filter { it > 0 }.minOrNull() ?: 0

            Log.d(TAG, "[Fallback] Preparando datos: deviceId=${deviceId}, sport=$sportType, start=$startTs, duracion=${state.elapsedSeconds}")

            val bioJson = """{"carga_mecanica_g":${bio.carga_mecanica_g},"impacto_fuerza_g":${bio.impacto_fuerza_g},"rotacion_tronco_x":${bio.rotacion_tronco_x},"rotacion_tronco_y":${bio.rotacion_tronco_y},"repeticiones":${bio.repeticiones},"velocidad_concentrica_promedio":${bio.velocidad_concentrica_promedio}}"""

            val entrenamiento = Entrenamiento(
                device_id = deviceId.ifBlank { "H10_UNKNOWN" },
                sport_type = sportType,
                start_timestamp = startTs,
                end_timestamp = endTs,
                duration_seconds = state.elapsedSeconds.toInt(),
                hr_avg = hrAvg.toInt(),
                hr_max = hrMax,
                hr_min = hrMin,
                hr_samples = hrSamples,
                rpe = validRpe,
                futbol_biomechanics = bioJson
            )
            Log.d(TAG, "[Fallback] Insertando en Supabase...")
            supabaseClient.from("entrenamientos").insert(entrenamiento)
            Log.d(TAG, "[Fallback] Insertado exitosamente")

            val summary = WorkoutSummary(
                sportType = sportType,
                startTime = state.startTime,
                endTime = state.startTime + (state.elapsedSeconds * 1000),
                durationSeconds = state.elapsedSeconds.toInt(),
                avgHr = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toInt() else 0,
                maxHr = heartRateHistory.maxOrNull() ?: 0,
                rpe = validRpe
            )
            _uiState.update { it.copy(
                syncState = SyncState.Success,
                isSaving = false,
                savedSuccessfully = true,
                savedWorkoutSummary = summary
            ) }
            Log.d(TAG, "[Fallback] UI actualizada - savedSuccessfully=true")
        } catch (e: Exception) {
            Log.e(TAG, "[Fallback] Error al guardar: ${e.message}")
            Log.e(TAG, "[Fallback] Stack: ${e.stackTraceToString()}")
            _uiState.update { it.copy(
                syncState = SyncState.Error("Fallback fallido: ${e.message}"),
                isSaving = false,
                errorMessage = "Error al guardar: ${e.message}"
            ) }
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

    fun resetWorkout() {
        timerJob?.cancel()
        syncJob?.cancel()
        heartRateHistory.clear()
        _uiState.update {
            it.copy(
                isActive = false,
                isPaused = false,
                elapsedSeconds = 0L,
                startTime = 0L,
                isSaving = false,
                savedSuccessfully = false,
                syncState = SyncState.Idle,
                savedWorkoutSummary = null,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        syncJob?.cancel()
        Log.d(TAG, "WorkoutViewModel limpiado")
    }
}