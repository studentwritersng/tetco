package com.teacherscompanion.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.local.dao.AlarmDao
import com.teacherscompanion.data.local.entity.AlarmEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmUiState(
    val label: String = "",
    val hour: Int = 6,
    val minute: Int = 0,
    val vibrate: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmDao: AlarmDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    fun updateLabel(label: String) { _uiState.value = _uiState.value.copy(label = label) }
    fun updateTime(hour: Int, minute: Int) { _uiState.value = _uiState.value.copy(hour = hour, minute = minute) }
    fun updateVibrate(vibrate: Boolean) { _uiState.value = _uiState.value.copy(vibrate = vibrate) }

    fun saveAlarm(dayIndices: List<Int>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val state = _uiState.value
            val alarm = AlarmEntity(
                label = state.label.ifBlank { "Alarm" },
                type = "wake_up",
                timeHour = state.hour,
                timeMinute = state.minute,
                repeatDays = dayIndices.joinToString(","),
                vibrate = state.vibrate
            )
            alarmDao.insertAlarm(alarm)
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}
