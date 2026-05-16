package com.policar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════════
//  POLICAR — DATA MODELS (UNIFIED)
//  Todos los modelos son @Serializable para Supabase (postgrest-kt)
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Deportes disponibles ────────────────────────────────────────────────────

enum class TipoDeporte(
    val displayName: String,
    val emoji: String,
    val supportsOffline: Boolean,
    val description: String
) {
    FUTBOL("Fútbol", "⚽", true, "Carga G · Impactos · Balance"),
    PADEL("Pádel", "🎾", true, "Rotación X/Y · Smashes"),
    GIMNASIO("Gimnasio", "🏋️", false, "Reps · Velocidad Concéntrica")
}

typealias SportType = TipoDeporte

// ─── Zonas de frecuencia cardíaca (Karvonen) ──────────────────────────────

enum class HRZone(val label: String, val minPercent: Float, val maxPercent: Float) {
    ZONE_1("Recuperación", 0.50f, 0.60f),
    ZONE_2("Base aeróbica", 0.60f, 0.70f),
    ZONE_3("Aeróbica", 0.70f, 0.80f),
    ZONE_4("Umbral láctico", 0.80f, 0.90f),
    ZONE_5("VO2 Max", 0.90f, 1.00f)
}

// ─── Nivel de estrés HRV ──────────────────────────────────────────────────

enum class StressLevel(val label: String) {
    VERY_LOW("Muy bajo"),
    LOW("Bajo"),
    MODERATE("Moderado"),
    HIGH("Alto"),
    VERY_HIGH("Muy alto")
}

// ─── Modo de grabación ────────────────────────────────────────────────────────

enum class ModoGrabacion(val label: String, val description: String) {
    EN_VIVO("EN VIVO", "Telemetría en tiempo real"),
    OFFLINE("GRABAR EN SENSOR", "H10 graba internamente · Sincroniza al finalizar")
}

// ─── Estado de conexión BLE ───────────────────────────────────────────────────

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Scanning : ConnectionState()
    data class Connecting(val deviceId: String) : ConnectionState()
    data class Connected(val deviceId: String) : ConnectionState()
    data class Reconnecting(val deviceId: String, val attempt: Int) : ConnectionState()
    data class Error(val deviceId: String, val message: String) : ConnectionState()

    val isConnected: Boolean get() = this is Connected
    val isSearching: Boolean get() = this is Scanning || this is Connecting || this is Reconnecting
    val deviceIdOrNull: String? get() = when (this) {
        is Connected -> deviceId
        is Connecting -> deviceId
        is Reconnecting -> deviceId
        is Error -> deviceId
        is Scanning, is Disconnected -> null
    }
}

// ─── Estado del pipeline Sync & Clean ─────────────────────────────────────────

sealed class SyncState {
    data object Idle : SyncState()
    data object Downloading : SyncState()
    data object Uploading : SyncState()
    data object Clearing : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()

    val label: String get() = when (this) {
        is Idle -> ""
        is Downloading -> "↓ Descargando del sensor…"
        is Uploading -> "☁ Subiendo a Supabase…"
        is Clearing -> "🗑 Limpiando memoria H10…"
        is Success -> "✓ Sincronizado correctamente"
        is Error -> "✗ Error: $message"
    }
    val isInProgress: Boolean get() = this is Downloading || this is Uploading || this is Clearing
}

// ─── Estado de grabación en el sensor ───────────────────────────────────────

data class RecordingStatus(
    val isRecording: Boolean = false,
    val exerciseId: String = ""
)

// ─── Datos biomecánicos en tiempo real ───────────────────────────────────────

data class DatosBiomecanicos(
    val deporte: String,
    val impacto_fuerza_g: Double = 0.0,
    val carga_mecanica_g: Double = 0.0,
    val rotacion_tronco_x: Double = 0.0,
    val rotacion_tronco_y: Double = 0.0,
    val repeticiones: Int = 0,
    val velocidad_concentrica_promedio: Double = 0.0,
    val velocity_drop_warning: Boolean = false,
    val picos_g_history: List<Double> = emptyList(),
    val smash_count: Int = 0
)

@Serializable
data class AccData(
    val gForce: Double = 0.0,
    val isImpact: Boolean = false,
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0
)

@Serializable
data class TelemetryData(
    val heartRate: Int = 0,
    val hrZone: Int = 0,
    val rmssd: Double = 0.0,
    val rrInterval: Int = 0,
    val ecgBuffer: List<Int> = emptyList(),
    val hrHistory: List<Int> = emptyList(),
    val stressLevel: String = "Low"
)

// ─── Datos de un ejercicio descargado del H10 ────────────────────────────────

data class ExerciseRecordingData(
    val entryPath: String,
    val hrSamples: List<Int>,
    val intervalSeconds: Int,
    val totalDurationSeconds: Int,
    val avgHr: Int,
    val maxHr: Int,
    val minHr: Int,
    val sparklineCsv: String
)

// ─────────────────────────────────────────────────────────────────────────
//  BIOMECHANICS DATA MODELS
// ─────────────────────────────────────────────────────────────────────────

@Serializable
data class ImpactEvent(
    @SerialName("timestamp_ms") val timestampMs: Long,
    @SerialName("g_force") val gForce: Float,
    @SerialName("x_mG") val xMilliG: Int,
    @SerialName("y_mG") val yMilliG: Int,
    @SerialName("z_mG") val zMilliG: Int
)

@Serializable
data class PadelSmashEvent(
    @SerialName("timestamp_ms") val timestampMs: Long,
    @SerialName("peak_rotation_dps") val peakRotationDps: Float,
    @SerialName("x_peak_mG") val xPeakMilliG: Int,
    @SerialName("y_peak_mG") val yPeakMilliG: Int,
    @SerialName("duration_ms") val durationMs: Long
)

@Serializable
data class GymRepData(
    @SerialName("rep_number") val repNumber: Int,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("concentric_duration_ms") val concentricDurationMs: Long,
    @SerialName("eccentric_duration_ms") val eccentricDurationMs: Long,
    @SerialName("peak_velocity") val peakVelocity: Float,
    @SerialName("mean_velocity") val meanVelocity: Float,
    @SerialName("velocity_loss_pct") val velocityLossPct: Float,
    @SerialName("hr_at_rep") val hrAtRep: Int
)

@Serializable
data class FutbolBiomechanics(
    @SerialName("total_impacts") val totalImpacts: Int = 0,
    @SerialName("high_intensity_impacts") val highIntensityImpacts: Int = 0,
    @SerialName("max_g_force") val maxGForce: Float = 0f,
    @SerialName("avg_g_force") val avgGForce: Float = 0f,
    @SerialName("mechanical_load_score") val mechanicalLoadScore: Float = 0f,
    @SerialName("cardiovascular_load_score") val cardiovascularLoadScore: Float = 0f,
    @SerialName("load_ratio") val loadRatio: Float = 0f,
    @SerialName("flight_time_ms") val flightTimeMs: Long = 0L,
    @SerialName("impacts") val impacts: List<ImpactEvent> = emptyList()
)

@Serializable
data class PadelBiomechanics(
    @SerialName("total_smashes") val totalSmashes: Int = 0,
    @SerialName("avg_rotation_dps") val avgRotationDps: Float = 0f,
    @SerialName("max_rotation_dps") val maxRotationDps: Float = 0f,
    @SerialName("asymmetry_score") val asymmetryScore: Float = 0f,
    @SerialName("smashes") val smashes: List<PadelSmashEvent> = emptyList()
)

@Serializable
data class GymBiomechanics(
    @SerialName("total_reps") val totalReps: Int = 0,
    @SerialName("total_sets") val totalSets: Int = 0,
    @SerialName("avg_velocity_ms") val avgVelocityMs: Float = 0f,
    @SerialName("velocity_loss_pct") val velocityLossPct: Float = 0f,
    @SerialName("fatigue_index") val fatigueIndex: Float = 0f,
    @SerialName("reps_data") val repsData: List<GymRepData> = emptyList()
)

// ─── Modelo Entrenamiento (tabla Supabase) ─────────────────────────────────────

@Serializable
data class Entrenamiento(
    @SerialName("device_id") val device_id: String,
    @SerialName("sport_type") val sport_type: String,
    @SerialName("start_timestamp") val start_timestamp: String,
    @SerialName("end_timestamp") val end_timestamp: String,
    @SerialName("duration_seconds") val duration_seconds: Int,
    @SerialName("hr_avg") val hr_avg: Int = 0,
    @SerialName("hr_max") val hr_max: Int = 0,
    @SerialName("hr_min") val hr_min: Int = 0,
    @SerialName("rpe") val rpe: Int = 0,
    @SerialName("hr_samples") val hr_samples: String = "",
    @SerialName("futbol_biomechanics") val futbol_biomechanics: String = "",
    @SerialName("padel_biomechanics") val padel_biomechanics: String = "",
    @SerialName("gym_biomechanics") val gym_biomechanics: String = "",
    @SerialName("notes") val notes: String = ""
)

// ─────────────────────────────────────────────────────────────────────────
//  UI STATE MODELS
// ─────────────────────────────────────────────────────────────────────────

data class SesionActiva(
    val tipo_deporte: String = "",
    val fecha_inicio: Long = 0L,
    val is_active: Boolean = false,
    val isPaused: Boolean = false,
    val is_recording_offline: Boolean = false,
    val modo_grabacion: ModoGrabacion = ModoGrabacion.EN_VIVO,
    val deviceId: String = "",
    val duracion_segundos: Long = 0L,
    val frecuencia_cardiaca_actual: Int = 0,
    val frecuencia_cardiaca_promedio: Int = 0,
    val frecuencia_cardiaca_max: Int = 0,
    val frecuencia_cardiaca_min: Int = 200,
    val errorMessage: String? = null,
    val syncState: SyncState = SyncState.Idle
)

data class TelemetryState(
    val heartRate: Int = 0,
    val rrInterval: Long = 0L,
    val rmssd: Float = 0f,
    val stressLevel: StressLevel = StressLevel.LOW,
    val hrZone: HRZone = HRZone.ZONE_1,
    val ecgBuffer: ArrayDeque<Float> = ArrayDeque(500),
    val hrHistory: List<Int> = emptyList()
)

data class AccelerometerState(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0,
    val gForce: Float = 0f,
    val isImpact: Boolean = false
)

data class WorkoutState(
    val isConnected: Boolean = false,
    val isSearching: Boolean = false,
    val deviceId: String = "",
    val batteryLevel: Int = 0,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startTime: Long = 0L,
    val selectedSport: SportType = SportType.FUTBOL,
    val telemetry: TelemetryState = TelemetryState(),
    val acc: AccelerometerState = AccelerometerState(),
    val futbolBiomechanics: FutbolBiomechanics = FutbolBiomechanics(),
    val padelBiomechanics: PadelBiomechanics = PadelBiomechanics(),
    val gymBiomechanics: GymBiomechanics = GymBiomechanics(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val syncState: SyncState = SyncState.Idle,
    val duracion_segundos: Long = 0L,
    val savedWorkoutSummary: WorkoutSummary? = null
)

data class WorkoutSummary(
    val sportType: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Int,
    val avgHr: Int,
    val maxHr: Int,
    val rpe: Int
)

// ─────────────────────────────────────────────────────────────────────────
//  HELPER FUNCTIONS
// ─────────────────────────────────────────────────────────────────────────

fun calculateGForce(xmG: Int, ymG: Int, zmG: Int): Float {
    val xG = xmG / 1000f
    val yG = ymG / 1000f
    val zG = zmG / 1000f
    return kotlin.math.sqrt(xG * xG + yG * yG + zG * zG)
}

fun calculateHRZone(hr: Int, maxHr: Int = 190): HRZone {
    val pct = hr.toFloat() / maxHr
    return when {
        pct < 0.60f -> HRZone.ZONE_1
        pct < 0.70f -> HRZone.ZONE_2
        pct < 0.80f -> HRZone.ZONE_3
        pct < 0.90f -> HRZone.ZONE_4
        else -> HRZone.ZONE_5
    }
}
