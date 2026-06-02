package com.teacherscompanion.ui.schools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.ClassLevelDto
import com.teacherscompanion.data.remote.dto.SchoolClassWithLevelDto
import com.teacherscompanion.data.remote.dto.SchoolDto
import com.teacherscompanion.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchoolDetailUiState(
    val school: SchoolDto? = null,
    val classes: List<SchoolClassWithLevelDto> = emptyList(),
    val classLevels: List<ClassLevelDto> = emptyList(),
    val isLoading: Boolean = false,
    val isAddingClass: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SchoolDetailViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchoolDetailUiState())
    val uiState: StateFlow<SchoolDetailUiState> = _uiState.asStateFlow()

    fun loadSchool(schoolId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val school = schoolRepository.getSchoolById(schoolId)
                val classes = schoolRepository.getClassesForSchool(schoolId)
                val classLevels = schoolRepository.getClassLevels()
                _uiState.value = SchoolDetailUiState(school = school, classes = classes, classLevels = classLevels, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun showAddClassSheet() {
        _uiState.value = _uiState.value.copy(isAddingClass = true)
    }

    fun hideAddClassSheet() {
        _uiState.value = _uiState.value.copy(isAddingClass = false)
    }

    fun addClassToSchool(schoolId: String, classLevelId: Int) {
        viewModelScope.launch {
            try {
                schoolRepository.addClassToSchool(schoolId, classLevelId)
                hideAddClassSheet()
                loadSchool(schoolId)
            } catch (_: Exception) { }
        }
    }
}
