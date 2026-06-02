package com.teacherscompanion.ui.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.SyllabusTopicDto
import com.teacherscompanion.data.repository.SyllabusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectDetailUiState(
    val topics: List<SyllabusTopicDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    fun loadTopics(subjectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val topics = syllabusRepository.getTopicsForSubject(subjectId)
                _uiState.value = SubjectDetailUiState(topics = topics, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun reorderTopics(topicOrders: Map<String, Int>) {
        viewModelScope.launch {
            try {
                syllabusRepository.reorderTopics(topicOrders)
            } catch (_: Exception) { }
        }
    }
}
