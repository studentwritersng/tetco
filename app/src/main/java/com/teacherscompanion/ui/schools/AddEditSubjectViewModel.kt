package com.teacherscompanion.ui.schools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditSubjectUiState(
    val name: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class AddEditSubjectViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditSubjectUiState())
    val uiState: StateFlow<AddEditSubjectUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun saveSubject(classId: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.length < 2) {
            _uiState.value = state.copy(nameError = "Name must be at least 2 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                schoolRepository.addSubject(classId, state.name)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, nameError = e.message)
            }
        }
    }
}
