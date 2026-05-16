package com.policar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

enum class ViewPeriod(val label: String, val days: Int) {
    WEEK("WEEK", 7),
    MONTH("MONTH", 30),
    QUARTER("QUARTER", 90)
}

@Serializable
data class TrainingSession(
    @SerialName("id")                val id: String = "",
    @SerialName("device_id")         val deviceId: String = "",
    @SerialName("sport_type")        val sportType: String = "",
    @SerialName("start_timestamp")   val startTimestampStr: String = "",
    @SerialName("end_timestamp")     val endTimestampStr: String = "",
    @SerialName("duration_seconds")  val durationSeconds: Int = 0,
    @SerialName("hr_avg")            val hrAvg: Float = 0f,
    @SerialName("hr_max")            val hrMax: Int = 0,
    @SerialName("hr_min")            val hrMin: Int = 0,
    @SerialName("rpe")               val rpe: Int = 0,
    @SerialName("hrv_rmssd_avg")     val hrvRmssdAvg: Float? = null,
    @SerialName("hrv_stress_level")  val hrvStressLevel: String? = null,
    @SerialName("zone1_seconds")     val zone1Seconds: Int = 0,
    @SerialName("zone2_seconds")     val zone2Seconds: Int = 0,
    @SerialName("zone3_seconds")     val zone3Seconds: Int = 0,
    @SerialName("zone4_seconds")     val zone4Seconds: Int = 0,
    @SerialName("zone5_seconds")     val zone5Seconds: Int = 0,
    @SerialName("notes")             val notes: String = "",
    @SerialName("futbol_biomechanics") val futbolBiomechanics: JsonElement? = null,
    @SerialName("padel_biomechanics")  val padelBiomechanics: JsonElement? = null,
    @SerialName("gym_biomechanics")    val gymBiomechanics: JsonElement? = null
) {
    val startTimestamp: Long get() = parseIsoTimestamp(startTimestampStr)
    val endTimestamp: Long get() = parseIsoTimestamp(endTimestampStr)
}

private fun parseIsoTimestamp(str: String): Long {
    if (str.isBlank()) return 0L
    return try {
        java.time.Instant.parse(str).toEpochMilli()
    } catch (e: Exception) {
        try {
            java.time.OffsetDateTime.parse(str).toInstant().toEpochMilli()
        } catch (e2: Exception) { 0L }
    }
}

data class CalendarDay(
    val date: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val sessions: List<TrainingSession>,
    val hasData: Boolean
)

data class WeeklyStats(
    val totalSessions: Int,
    val totalDurationSeconds: Long,
    val avgHr: Int,
    val maxHr: Int,
    val totalImpacts: Int,
    val totalSmashes: Int,
    val totalReps: Int,
    val sessionsBySport: Map<String, Int>,
    val dailyVolumes: List<DailyVolume>
)

data class MonthlyStats(
    val totalSessions: Int,
    val totalDurationSeconds: Long,
    val avgHr: Int,
    val maxHr: Int,
    val totalImpacts: Int,
    val totalSmashes: Int,
    val totalReps: Int,
    val avgSessionsPerWeek: Float,
    val sessionsBySport: Map<String, Int>,
    val weeklyVolumes: List<WeeklyVolume>,
    val hrTrend: List<Int>,
    val bestDay: String,
    val bestDaySessions: Int
)

data class DailyVolume(
    val date: Long,
    val dayLabel: String,
    val durationSeconds: Long,
    val sessionCount: Int,
    val avgHr: Int
)

data class WeeklyVolume(
    val weekStart: Long,
    val weekLabel: String,
    val totalDurationSeconds: Long,
    val sessionCount: Int,
    val avgHr: Int
)

data class DashboardStats(
    val period: ViewPeriod,
    val totalSessions: Int,
    val totalDurationSeconds: Long,
    val totalMinutes: Int,
    val avgSessionDurationMinutes: Int,
    val avgHr: Int,
    val maxHr: Int,
    val minHr: Int,
    val avgRpe: Float,
    val sessionsBySport: Map<String, Int>,
    val hrBySport: Map<String, Int>,
    val durationBySport: Map<String, Long>,
    val weeklyVolumes: List<DailyVolume>,
    val hrTrend: List<Int>,
    val totalImpacts: Int,
    val totalSmashes: Int,
    val totalReps: Int,
    val currentStreak: Int,
    val longestStreak: Int
)

data class HistoryUiState(
    val isLoading: Boolean = false,
    val sessions: List<TrainingSession> = emptyList(),
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedPeriod: ViewPeriod = ViewPeriod.WEEK,
    val dailyStats: DashboardStats? = null,
    val weeklyStats: DashboardStats? = null,
    val monthlyStats: DashboardStats? = null,
    val error: String? = null,
    val selectedSession: TrainingSession? = null
)
