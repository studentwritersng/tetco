package com.teacherscompanion.ui.schools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditSchoolUiState(
    val name: String = "",
    val address: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val schoolId: String? = null
)

@HiltViewModel
class AddEditSchoolViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditSchoolUiState())
    val uiState: StateFlow<AddEditSchoolUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    fun loadSchool(schoolId: String) {
        viewModelScope.launch {
            try {
                val school = schoolRepository.getSchoolById(schoolId)
                _uiState.value = _uiState.value.copy(
                    schoolId = schoolId,
                    name = school.name,
                    address = school.address ?: ""
                )
            } catch (_: Exception) { }
        }
    }

    fun saveSchool(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.length < 2) {
            _uiState.value = state.copy(nameError = "Name must be at least 2 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                if (state.schoolId != null) {
                    schoolRepository.updateSchool(state.schoolId, state.name, state.address.ifBlank { null }, null)
                } else {
                    schoolRepository.createSchool(state.name, state.address.ifBlank { null }, null)
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
