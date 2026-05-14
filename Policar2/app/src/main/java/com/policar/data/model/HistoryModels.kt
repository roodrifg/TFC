package com.policar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ViewPeriod(val label: String, val days: Int) {
    WEEK("WEEK", 7),
    MONTH("MONTH", 30),
    QUARTER("QUARTER", 90)
}

@Serializable
data class TrainingSession(
    @SerialName("id") val id: Long = 0,
    @SerialName("user_id") val userId: String = "",
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("sport_type") val sportType: String = "",
    @SerialName("start_timestamp") val startTimestamp: String = "",
    @SerialName("end_timestamp") val endTimestamp: String = "",
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("hr_avg") val hrAvg: Int = 0,
    @SerialName("hr_max") val hrMax: Int = 0,
    @SerialName("hr_min") val hrMin: Int = 0,
    @SerialName("rpe") val rpe: Int = 0,
    @SerialName("hr_samples") val hrSamples: String = "",
    @SerialName("futbol_biomechanics") val futbolBiomechanics: String = "",
    @SerialName("padel_biomechanics") val padelBiomechanics: String = "",
    @SerialName("gym_biomechanics") val gymBiomechanics: String = "",
    @SerialName("notes") val notes: String = ""
)

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
    val error: String? = null
)
