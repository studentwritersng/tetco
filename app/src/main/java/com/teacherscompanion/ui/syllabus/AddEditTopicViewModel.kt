package com.teacherscompanion.ui.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.repository.SyllabusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditTopicUiState(
    val title: String = "",
    val term: String = "",
    val weekNumber: Int = 0,
    val titleError: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class AddEditTopicViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTopicUiState())
    val uiState: StateFlow<AddEditTopicUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title, titleError = null)
    }

    fun updateTerm(term: String) {
        _uiState.value = _uiState.value.copy(term = term)
    }

    fun updateWeek(week: Int) {
        _uiState.value = _uiState.value.copy(weekNumber = week)
    }

    fun saveTopic(subjectId: String, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.length < 3) {
            _uiState.value = state.copy(titleError = "Title must be at least 3 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                syllabusRepository.addTopic(subjectId, state.title, state.term, state.weekNumber)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, titleError = e.message)
            }
        }
    }
}
