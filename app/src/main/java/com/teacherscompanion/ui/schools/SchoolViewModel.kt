package com.teacherscompanion.ui.schools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.SchoolDto
import com.teacherscompanion.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchoolListUiState(
    val schools: List<SchoolDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SchoolViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchoolListUiState())
    val uiState: StateFlow<SchoolListUiState> = _uiState.asStateFlow()

    init {
        loadSchools()
    }

    fun loadSchools() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val schools = schoolRepository.getSchools()
                _uiState.value = SchoolListUiState(schools = schools, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = SchoolListUiState(isLoading = false, error = e.message)
            }
        }
    }
}
