package com.policar.data.repository

import com.policar.data.model.DashboardStats
import com.policar.data.model.DailyVolume
import com.policar.data.model.TrainingSession
import com.policar.data.model.ViewPeriod
import com.policar.data.remote.SupabaseConfig
import com.policar.data.model.FutbolBiomechanics
import com.policar.data.model.PadelBiomechanics
import com.policar.data.model.GymBiomechanics
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class HistoryRepository {
    private val supabase = SupabaseConfig.supabase
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchSessions(userId: String, limit: Int = 100): Result<List<TrainingSession>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.postgrest["entrenamientos"]
                .select()
            val allSessions = response.decodeList<TrainingSession>()
            val filtered = allSessions.filter { session -> session.userId == userId }
            Result.success(filtered.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSessionsByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): Result<List<TrainingSession>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.postgrest["entrenamientos"]
                .select()
            val allSessions = response.decodeList<TrainingSession>()
            val start = LocalDate.parse(startDate.substringBefore("T"))
            val end = LocalDate.parse(endDate.substringBefore("T"))
            val filtered = allSessions.filter { session ->
                session.userId == userId && try {
                    val sessionDate = Instant.ofEpochMilli(session.startTimestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    !sessionDate.isBefore(start) && !sessionDate.isAfter(end)
                } catch (e: Exception) {
                    false
                }
            }
            Result.success(filtered.sortedByDescending { it.startTimestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSessionsForPeriod(
        userId: String,
        period: ViewPeriod
    ): Result<List<TrainingSession>> = withContext(Dispatchers.IO) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(period.days.toLong())
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        val startStr = startDate.atStartOfDay().format(formatter)
        val endStr = endDate.plusDays(1).atStartOfDay().format(formatter)

        fetchSessionsByDateRange(userId, startStr, endStr)
    }

    suspend fun calculateDashboardStats(
        userId: String,
        period: ViewPeriod
    ): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            val sessionsResult = fetchSessionsForPeriod(userId, period)
            val sessions = sessionsResult.getOrThrow()

            if (sessions.isEmpty()) {
                return@withContext Result.success(
                    DashboardStats(
                        period = period,
                        totalSessions = 0,
                        totalDurationSeconds = 0,
                        totalMinutes = 0,
                        avgSessionDurationMinutes = 0,
                        avgHr = 0,
                        maxHr = 0,
                        minHr = 0,
                        avgRpe = 0f,
                        sessionsBySport = emptyMap(),
                        hrBySport = emptyMap(),
                        durationBySport = emptyMap(),
                        weeklyVolumes = emptyList(),
                        hrTrend = emptyList(),
                        totalImpacts = 0,
                        totalSmashes = 0,
                        totalReps = 0,
                        currentStreak = 0,
                        longestStreak = 0
                    )
                )
            }

            val totalSessions = sessions.size
            var totalDurationSeconds: Long = 0
            var totalMinutes: Int = 0
            for (s in sessions) {
                totalDurationSeconds += s.durationSeconds.toLong()
            }
            totalMinutes = (totalDurationSeconds / 60).toInt()
            val avgSessionDurationMinutes = if (totalSessions > 0) totalMinutes / totalSessions else 0

            var hrSum = 0
            var hrCount = 0
            for (s in sessions) {
                if (s.hrAvg > 0) {
                    hrSum += s.hrAvg
                    hrCount++
                }
            }
            val avgHr = if (hrCount > 0) hrSum / hrCount else 0

            var maxHr = 0
            for (s in sessions) {
                if (s.hrMax > maxHr) maxHr = s.hrMax
            }

            var minHr = Int.MAX_VALUE
            for (s in sessions) {
                if (s.hrMin > 0 && s.hrMin < minHr) minHr = s.hrMin
            }
            if (minHr == Int.MAX_VALUE) minHr = 0

            var rpeSum = 0f
            var rpeCount = 0
            for (s in sessions) {
                if (s.rpe > 0) {
                    rpeSum += s.rpe
                    rpeCount++
                }
            }
            val avgRpe = if (rpeCount > 0) rpeSum / rpeCount else 0f

            val sessionsBySportCount = sessions.groupBy { it.sportType }
                .mapValues { it.value.size }

            val sessionsBySportList = sessions.groupBy { it.sportType }
                .mapValues { it.value.toList() }

            val hrBySport = mutableMapOf<String, Int>()
            for ((sport, sportSessionsList) in sessionsBySportList) {
                var sportHrSum = 0
                var sportHrCount = 0
                for (s in sportSessionsList) {
                    if (s.hrAvg > 0) {
                        sportHrSum += s.hrAvg
                        sportHrCount++
                    }
                }
                hrBySport[sport] = if (sportHrCount > 0) sportHrSum / sportHrCount else 0
            }

            val durationBySport = mutableMapOf<String, Long>()
            for ((sport, sportSessionsList) in sessionsBySportList) {
                var duration: Long = 0
                for (s in sportSessionsList) {
                    duration += s.durationSeconds.toLong()
                }
                durationBySport[sport] = duration
            }

            val weeklyVolumes = calculateWeeklyVolumes(sessions)
            val hrTrend = calculateHrTrend(sessions)

            var totalImpacts = 0
            for (session in sessions) {
                try {
                    if (session.sportType == "FUTBOL" && session.impactForceG > 0) {
                        totalImpacts++
                    }
                } catch (e: Exception) { }
            }

            var totalSmashes = 0
            for (session in sessions) {
                try {
                    if (session.sportType == "PADEL" && session.trunkRotationX > 0) {
                        totalSmashes++
                    }
                } catch (e: Exception) { }
            }

            var totalReps = 0
            for (session in sessions) {
                try {
                    if (session.sportType == "GIMNASIO" && session.reps > 0) {
                        totalReps += session.reps
                    }
                } catch (e: Exception) { }
            }

            val currentStreak = calculateCurrentStreak(sessions)
            val longestStreak = calculateLongestStreak(sessions)

            Result.success(
                DashboardStats(
                    period = period,
                    totalSessions = totalSessions,
                    totalDurationSeconds = totalDurationSeconds,
                    totalMinutes = totalMinutes,
                    avgSessionDurationMinutes = avgSessionDurationMinutes,
                    avgHr = avgHr,
                    maxHr = maxHr,
                    minHr = minHr,
                avgRpe = avgRpe,
                sessionsBySport = sessionsBySportCount,
                hrBySport = hrBySport,
                    durationBySport = durationBySport,
                    weeklyVolumes = weeklyVolumes,
                    hrTrend = hrTrend,
                    totalImpacts = totalImpacts,
                    totalSmashes = totalSmashes,
                    totalReps = totalReps,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateWeeklyVolumes(sessions: List<TrainingSession>): List<DailyVolume> {
        val dailyGroups = sessions.groupBy { session ->
            try {
                val instant = Instant.ofEpochMilli(session.startTimestamp)
                instant.atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (e: Exception) {
                LocalDate.now()
            }
        }

        val dayFormatter = DateTimeFormatter.ofPattern("EEE")

        return dailyGroups.map { (date, daySessions) ->
            var duration: Long = 0
            for (s in daySessions) {
                duration += s.durationSeconds.toLong()
            }
            var hrSum = 0
            var hrCount = 0
            for (s in daySessions) {
                if (s.hrAvg > 0) {
                    hrSum += s.hrAvg
                    hrCount++
                }
            }
            val avgHr = if (hrCount > 0) hrSum / hrCount else 0

            DailyVolume(
                date = date.toEpochDay() * 24 * 60 * 60 * 1000,
                dayLabel = date.format(dayFormatter),
                durationSeconds = duration,
                sessionCount = daySessions.size,
                avgHr = avgHr
            )
        }.sortedBy { it.date }
    }

    private fun calculateHrTrend(sessions: List<TrainingSession>): List<Int> {
        return sessions
            .sortedBy { it.startTimestamp }
            .map { it.hrAvg }
            .filter { it > 0 }
            .takeLast(30)
    }

    private fun calculateCurrentStreak(sessions: List<TrainingSession>): Int {
        if (sessions.isEmpty()) return 0

        val sessionDates = sessions.mapNotNull { session ->
            try {
                val instant = Instant.ofEpochMilli(session.startTimestamp)
                instant.atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (e: Exception) {
                null
            }
        }.distinct().sortedDescending()

        val today = LocalDate.now()
        var streak = 0
        var currentDate = today

        for (date in sessionDates) {
            val daysDiff = ChronoUnit.DAYS.between(date, currentDate)
            if (daysDiff <= 1) {
                streak++
                currentDate = date
            } else {
                break
            }
        }

        return streak
    }

    private fun calculateLongestStreak(sessions: List<TrainingSession>): Int {
        if (sessions.isEmpty()) return 0

        val sessionDates = sessions.mapNotNull { session ->
            try {
                val instant = Instant.ofEpochMilli(session.startTimestamp)
                instant.atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (e: Exception) {
                null
            }
        }.distinct().sorted()

        if (sessionDates.isEmpty()) return 0

        var longestStreak = 1
        var currentStreak = 1

        for (i in 1 until sessionDates.size) {
            val daysDiff = ChronoUnit.DAYS.between(sessionDates[i - 1], sessionDates[i])
            if (daysDiff == 1L) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return longestStreak
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
