package com.policar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.policar.data.model.CalendarDay
import com.policar.data.model.HistoryUiState
import com.policar.data.model.TrainingSession
import com.policar.data.model.ViewPeriod
import com.policar.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

class HistoryViewModel : ViewModel() {

    private val repository = HistoryRepository()
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun setUserId(userId: String) {
        if (currentUserId != userId) {
            currentUserId = userId
            loadHistory()
        }
    }

    fun loadHistory() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = repository.fetchSessionsForPeriod(currentUserId, _uiState.value.selectedPeriod)

            result.fold(
                onSuccess = { sessions ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            sessions = sessions,
                            error = null
                        )
                    }
                    calculateCalendarDays()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error loading history"
                        )
                    }
                }
            )
        }
    }

    fun selectPeriod(period: ViewPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadHistory()
    }

    fun selectDate(date: Long) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun calculateCalendarDays() {
        val period = _uiState.value.selectedPeriod
        val today = LocalDate.now()
        val startDate = when (period) {
            ViewPeriod.WEEK -> today.minusDays(today.dayOfWeek.value.toLong() - 1)
            ViewPeriod.MONTH -> today.withDayOfMonth(1)
            ViewPeriod.QUARTER -> today.minusWeeks(12 / 3)
        }

        val endDate = when (period) {
            ViewPeriod.WEEK -> startDate.plusDays(6)
            ViewPeriod.MONTH -> today.withDayOfMonth(today.lengthOfMonth())
            ViewPeriod.QUARTER -> startDate.plusWeeks(12)
        }

        val weekFields = WeekFields.of(Locale.getDefault())
        val firstDayOfWeek = weekFields.firstDayOfWeek

        val calendarDays = mutableListOf<CalendarDay>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val sessionsForDay = _uiState.value.sessions.filter { session ->
                try {
                    val instant = Instant.parse(session.startTimestamp)
                    val sessionDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                    sessionDate == currentDate
                } catch (e: Exception) {
                    false
                }
            }

            calendarDays.add(
                CalendarDay(
                    date = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    dayOfMonth = currentDate.dayOfMonth,
                    isCurrentMonth = currentDate.month == today.month,
                    isToday = currentDate == today,
                    sessions = sessionsForDay,
                    hasData = sessionsForDay.isNotEmpty()
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        _uiState.update { it.copy(calendarDays = calendarDays) }
    }

    fun calculateDashboardStats() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repository.calculateDashboardStats(currentUserId, _uiState.value.selectedPeriod)

            result.fold(
                onSuccess = { stats ->
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun getSessionsForDate(date: Long): List<TrainingSession> {
        val targetDate = Instant.ofEpochMilli(date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        return _uiState.value.sessions.filter { session ->
            try {
                val instant = Instant.parse(session.startTimestamp)
                val sessionDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                sessionDate == targetDate
            } catch (e: Exception) {
                false
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%d:%02d", minutes, secs)
        }
    }

    fun formatDurationLong(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun formatTime(timestamp: String): String {
        return try {
            val instant = Instant.parse(timestamp)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun formatDateFull(timestamp: String): String {
        return try {
            val instant = Instant.parse(timestamp)
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) {
            "--"
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            val result = repository.deleteSession(sessionId)
            result.fold(
                onSuccess = {
                    loadHistory()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Error deleting session")
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
