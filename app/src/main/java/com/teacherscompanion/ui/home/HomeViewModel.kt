package com.teacherscompanion.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.local.dao.AlarmDao
import com.teacherscompanion.data.repository.ProfileRepository
import com.teacherscompanion.data.repository.SyllabusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val teacherName: String = "Teacher",
    val planName: String = "BASIC PLAN",
    val syllabusPercent: Int = 0,
    val uncoveredNotesCount: Int = 0,
    val nextAlarmLabel: String? = null,
    val nextAlarmTime: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val syllabusRepository: SyllabusRepository,
    private val alarmDao: AlarmDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Load Profile
                val profile = profileRepository.getProfile()
                val planStr = "${profile.plan_name.uppercase()} PLAN"

                // Load Syllabus stats
                val (total, completed, uncovered) = syllabusRepository.getGapStats()
                val percent = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

                // Load alarms
                val alarms = alarmDao.getActiveAlarms()
                val nextAlarm = alarms.minByOrNull { it.timeHour * 60 + it.timeMinute }
                val nextLabel = nextAlarm?.label ?: nextAlarm?.type?.replaceFirstChar { it.uppercase() }
                val nextTime = nextAlarm?.let {
                    val ampm = if (it.timeHour >= 12) "PM" else "AM"
                    val hr = if (it.timeHour % 12 == 0) 12 else it.timeHour % 12
                    String.format("%02d:%02d %s", hr, it.timeMinute, ampm)
                }

                _uiState.value = HomeUiState(
                    teacherName = profile.full_name ?: "Teacher",
                    planName = planStr,
                    syllabusPercent = percent,
                    uncoveredNotesCount = uncovered,
                    nextAlarmLabel = nextLabel,
                    nextAlarmTime = nextTime,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
