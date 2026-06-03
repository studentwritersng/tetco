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

data class EditLessonNoteUiState(
    val noteId: String? = null,
    val noteContent: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditLessonNoteViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditLessonNoteUiState())
    val uiState: StateFlow<EditLessonNoteUiState> = _uiState.asStateFlow()

    fun loadNote(topicId: String) {
        viewModelScope.launch {
            try {
                val note = syllabusRepository.getLessonNoteForTopic(topicId)
                if (note != null) {
                    _uiState.value = EditLessonNoteUiState(noteId = note.id, noteContent = note.content ?: "")
                }
            } catch (_: Exception) { }
        }
    }

    fun saveNote(topicId: String, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state.noteId != null) {
                    syllabusRepository.updateLessonNote(state.noteId, content)
                } else {
                    val note = syllabusRepository.createLessonNote(topicId, content)
                    _uiState.value = _uiState.value.copy(noteId = note.id, noteContent = content)
                }
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}
