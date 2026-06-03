package com.teacherscompanion.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.local.dao.AlarmDao
import com.teacherscompanion.data.local.entity.AlarmEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeriodReminderUiState(
    val name: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val advanceMinutes: Int = 10,
    val isSaving: Boolean = false
)

@HiltViewModel
class PeriodReminderViewModel @Inject constructor(
    private val alarmDao: AlarmDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeriodReminderUiState())
    val uiState: StateFlow<PeriodReminderUiState> = _uiState.asStateFlow()

    fun updateName(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun updateTime(hour: Int, minute: Int) { _uiState.value = _uiState.value.copy(hour = hour, minute = minute) }
    fun updateAdvance(minutes: Int) { _uiState.value = _uiState.value.copy(advanceMinutes = minutes) }

    fun saveReminder(dayNames: List<String>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val state = _uiState.value
            val dayIndices = dayNames.map {
                when (it) {
                    "MON" -> 1; "TUE" -> 2; "WED" -> 3; "THU" -> 4; "FRI" -> 5; "SAT" -> 6; "SUN" -> 7; else -> 1
                }
            }
            val alarm = AlarmEntity(
                label = state.name,
                type = "period",
                timeHour = state.hour,
                timeMinute = state.minute,
                repeatDays = dayIndices.joinToString(","),
                advanceMinutes = state.advanceMinutes
            )
            alarmDao.insertAlarm(alarm)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}
