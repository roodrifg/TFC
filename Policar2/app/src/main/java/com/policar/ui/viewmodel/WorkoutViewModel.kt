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
import com.policar.data.model.ModoGrabacion
import com.policar.data.model.TipoDeporte
import com.policar.data.model.WorkoutState
import com.policar.data.model.WorkoutSummary
import com.policar.data.model.TelemetryState
import com.policar.data.model.calculateHRZone
import com.policar.data.remote.SupabaseConfig
import com.policar.sensor.PolarManagerProvider
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
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
    private var wasDisconnectedDuringWorkout = false

    val connectionState: StateFlow<ConnectionState> = polarManager.connectionState
    val hrValue: StateFlow<Int> = polarManager.hrValue
    val hrvStress: StateFlow<Double> = polarManager.hrvStress
    val rmssd: StateFlow<Double> = polarManager.rmssd
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
        // Reiniciar streaming si el BLE reconecta durante un entrenamiento activo
        viewModelScope.launch {
            polarManager.connectionState.collect { state ->
                val workout = _uiState.value
                when {
                    workout.isActive && (state is ConnectionState.Disconnected || state is ConnectionState.Error) -> {
                        wasDisconnectedDuringWorkout = true
                        Log.d(TAG, "BLE perdido durante entrenamiento activo")
                    }
                    workout.isActive && state is ConnectionState.Connected && wasDisconnectedDuringWorkout -> {
                        wasDisconnectedDuringWorkout = false
                        polarManager.restartStreaming(state.deviceId, workout.selectedSport)
                        Log.d(TAG, "BLE reconectado — streaming reanudado sin perder datos")
                    }
                    state is ConnectionState.Connected -> {
                        wasDisconnectedDuringWorkout = false
                    }
                }
            }
        }
    }

    fun startWorkout(sport: TipoDeporte, deviceId: String, mode: ModoGrabacion = ModoGrabacion.EN_VIVO) {
        timerJob?.cancel()
        syncJob?.cancel()
        heartRateHistory.clear()
        wasDisconnectedDuringWorkout = false
        polarManager.stopStreaming()

        val startTime = System.currentTimeMillis()
        _uiState.update { it.copy(
            isActive = true,
            isPaused = false,
            selectedSport = sport,
            deviceId = deviceId,
            startTime = startTime,
            elapsedSeconds = 0L,
            isSaving = false,
            savedSuccessfully = false,
            syncState = SyncState.Idle,
            savedWorkoutSummary = null,
            errorMessage = null
        ) }
        polarManager.startStreaming(deviceId, sport)
        if (mode == ModoGrabacion.OFFLINE) {
            val exId = "WK${(startTime % 100000000L).toString().take(8)}"
            polarManager.startRecording(deviceId, exId)
            // Grabar ACC en H10 para biomecánica sin BLE (partido real)
            if (sport == TipoDeporte.FUTBOL) {
                polarManager.startAccOfflineRecording(deviceId)
            }
            Log.d(TAG, "Grabacion interna H10 iniciada: exId=$exId")
        }
        startTimer()
        Log.d(TAG, "Entrenamiento iniciado: $sport modo=${mode.name} en $startTime")
    }

    fun discardWorkout() {
        timerJob?.cancel()
        syncJob?.cancel()
        wasDisconnectedDuringWorkout = false
        val deviceId = _uiState.value.deviceId
        if (deviceId.isNotEmpty()) {
            polarManager.stopStreaming()
            polarManager.stopRecording(deviceId)
        }
        heartRateHistory.clear()
        _uiState.value = WorkoutState()
        Log.d(TAG, "Entrenamiento descartado")
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

                val startTs = state.startTime
                val endTs = System.currentTimeMillis()
                val startTsIso = Instant.ofEpochMilli(startTs).toString()
                val endTsIso   = Instant.ofEpochMilli(endTs).toString()

                val hrAvg = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toFloat() else 0f
                val hrMax = heartRateHistory.maxOrNull() ?: 0
                val hrMin = heartRateHistory.filter { it > 0 }.minOrNull() ?: 0

                val hrSamplesJson = buildJsonArray { heartRateHistory.forEach { add(it) } }
                val zones = calcZoneSeconds(heartRateHistory)

                // Para FUTBOL en modo OFFLINE: intentar descargar ACC grabado en H10
                // Permite tener biomecánica aunque el móvil estuviera lejos durante el partido
                val offlineFutbolBio: FutbolBiomechanics? = if (state.selectedSport == TipoDeporte.FUTBOL) {
                    try {
                        val accSamples = polarManager.fetchOfflineAccSamples(deviceId)
                        if (accSamples.isNotEmpty()) {
                            Log.d(TAG, "[Sync] ACC offline: ${accSamples.size} muestras procesando...")
                            polarManager.processOfflineAccForFutbol(accSamples)
                        } else null
                    } catch (e: Exception) {
                        Log.w(TAG, "[Sync] ACC offline no disponible, usando datos live: ${e.message}")
                        null
                    }
                } else null

                val futbolBioJson: JsonElement? = if (state.selectedSport == TipoDeporte.FUTBOL) {
                    val bio = offlineFutbolBio ?: futbolBio.value
                    SupabaseConfig.supabaseJson.encodeToJsonElement(FutbolBiomechanics.serializer(), bio)
                } else null
                val padelBioJson: JsonElement? = if (state.selectedSport == TipoDeporte.PADEL)
                    SupabaseConfig.supabaseJson.encodeToJsonElement(PadelBiomechanics.serializer(), padelBio.value) else null
                val gymBioJson: JsonElement? = if (state.selectedSport == TipoDeporte.GIMNASIO)
                    SupabaseConfig.supabaseJson.encodeToJsonElement(GymBiomechanics.serializer(), gymBio.value) else null

                Log.d(TAG, "Preparando datos: device=$validDeviceId sport=$sportType start=$startTsIso dur=${state.elapsedSeconds}s")

                val entrenamiento = Entrenamiento(
                    device_id          = validDeviceId,
                    sport_type         = sportType,
                    start_timestamp    = startTsIso,
                    end_timestamp      = endTsIso,
                    duration_seconds   = state.elapsedSeconds,
                    hr_avg             = hrAvg,
                    hr_max             = hrMax,
                    hr_min             = hrMin,
                    hr_samples         = hrSamplesJson,
                    rpe                = validRpe,
                    zone1_seconds      = zones[0],
                    zone2_seconds      = zones[1],
                    zone3_seconds      = zones[2],
                    zone4_seconds      = zones[3],
                    zone5_seconds      = zones[4],
                    futbol_biomechanics = futbolBioJson,
                    padel_biomechanics  = padelBioJson,
                    gym_biomechanics    = gymBioJson
                )
                Log.d(TAG, "Insertando en Supabase tabla 'entrenamientos'...")
                insertEntrenamiento(entrenamiento)
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

        try {
            val startTs = state.startTime
            val endTs = System.currentTimeMillis()
            val startTsIso = Instant.ofEpochMilli(startTs).toString()
            val endTsIso   = Instant.ofEpochMilli(endTs).toString()

            val hrAvg = if (heartRateHistory.isNotEmpty()) heartRateHistory.average().toFloat() else 0f
            val hrMax = heartRateHistory.maxOrNull() ?: 0
            val hrMin = heartRateHistory.filter { it > 0 }.minOrNull() ?: 0

            val hrSamplesJson = buildJsonArray { heartRateHistory.forEach { add(it) } }
            val zones = calcZoneSeconds(heartRateHistory)
            val futbolBioJson: JsonElement? = if (state.selectedSport == TipoDeporte.FUTBOL)
                SupabaseConfig.supabaseJson.encodeToJsonElement(FutbolBiomechanics.serializer(), futbolBio.value) else null
            val padelBioJson: JsonElement? = if (state.selectedSport == TipoDeporte.PADEL)
                SupabaseConfig.supabaseJson.encodeToJsonElement(PadelBiomechanics.serializer(), padelBio.value) else null
            val gymBioJson: JsonElement? = if (state.selectedSport == TipoDeporte.GIMNASIO)
                SupabaseConfig.supabaseJson.encodeToJsonElement(GymBiomechanics.serializer(), gymBio.value) else null

            Log.d(TAG, "[Live] deviceId=$deviceId sport=$sportType start=$startTsIso dur=${state.elapsedSeconds}s")

            val entrenamiento = Entrenamiento(
                device_id          = deviceId.ifBlank { "H10_UNKNOWN" },
                sport_type         = sportType,
                start_timestamp    = startTsIso,
                end_timestamp      = endTsIso,
                duration_seconds   = state.elapsedSeconds,
                hr_avg             = hrAvg,
                hr_max             = hrMax,
                hr_min             = hrMin,
                hr_samples         = hrSamplesJson,
                rpe                = validRpe,
                zone1_seconds      = zones[0],
                zone2_seconds      = zones[1],
                zone3_seconds      = zones[2],
                zone4_seconds      = zones[3],
                zone5_seconds      = zones[4],
                futbol_biomechanics = futbolBioJson,
                padel_biomechanics  = padelBioJson,
                gym_biomechanics    = gymBioJson
            )
            Log.d(TAG, "[Fallback] Insertando en Supabase...")
            insertEntrenamiento(entrenamiento)
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

    private fun calcZoneSeconds(hrSamples: List<Int>): IntArray {
        val zones = IntArray(5)
        for (hr in hrSamples) {
            if (hr <= 0) continue
            zones[calculateHRZone(hr).ordinal]++
        }
        return zones
    }

    private suspend fun insertEntrenamiento(entrenamiento: Entrenamiento) {
        val jsonObj = SupabaseConfig.supabaseJson
            .encodeToJsonElement(Entrenamiento.serializer(), entrenamiento)
            .jsonObject
        supabaseClient.from("entrenamientos").insert(jsonObj)
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