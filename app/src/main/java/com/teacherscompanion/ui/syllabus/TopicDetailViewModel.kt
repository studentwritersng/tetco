package com.teacherscompanion.ui.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.QuestionDto
import com.teacherscompanion.data.repository.ProfileRepository
import com.teacherscompanion.data.repository.SyllabusRepository
import com.teacherscompanion.data.repository.TopicInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicDetailUiState(
    val topicTitle: String = "",
    val subjectName: String = "",
    val className: String = "",
    val topicTerm: String? = null,
    val topicWeek: Int? = null,
    val noteContent: String? = null,
    val teachingGuide: String? = null,
    val questions: List<QuestionDto> = emptyList(),
    val planName: String = "basic",
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isGeneratingGuide: Boolean = false,
    val error: String? = null,
    val allTopics: List<TopicInfo> = emptyList(),
    val showTopicPicker: Boolean = false,
    val selectedTopicInfo: TopicInfo? = null
)

@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicDetailUiState())
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    fun loadTopic(topicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val profile = profileRepository.getProfile()
                _uiState.value = _uiState.value.copy(planName = profile.plan)
            } catch (_: Exception) { }

            try {
                val allInfo = syllabusRepository.getAllTopicsWithInfo()
                val info = allInfo.find { it.topicId == topicId }
                if (info != null) {
                    _uiState.value = _uiState.value.copy(
                        topicTitle = info.topicTitle,
                        subjectName = info.subjectName,
                        className = info.className,
                        topicTerm = info.term,
                        topicWeek = info.weekNumber,
                        allTopics = allInfo
                    )
                }
                val note = syllabusRepository.getLessonNoteForTopic(topicId)
                if (note != null) {
                    _uiState.value = _uiState.value.copy(
                        noteContent = note.content,
                        teachingGuide = note.teaching_guide
                    )
                    val questions = syllabusRepository.getQuestionsForNote(note.id)
                    _uiState.value = _uiState.value.copy(questions = questions, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun showTopicPicker() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val allInfo = syllabusRepository.getAllTopicsWithInfo()
                _uiState.value = _uiState.value.copy(allTopics = allInfo, showTopicPicker = true, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun hideTopicPicker() {
        _uiState.value = _uiState.value.copy(showTopicPicker = false)
    }

    fun selectTopic(info: TopicInfo) {
        _uiState.value = _uiState.value.copy(selectedTopicInfo = info)
    }

    fun generateLessonNote(onGenerated: (String) -> Unit) {
        val info = _uiState.value.selectedTopicInfo ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, showTopicPicker = false, error = null)
            try {
                val content = syllabusRepository.generateLessonNote(
                    topicId = info.topicId,
                    topicTitle = info.topicTitle,
                    subjectName = info.subjectName,
                    className = info.className,
                    term = info.term,
                    weekNumber = info.weekNumber
                )
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    noteContent = content,
                    topicTitle = info.topicTitle,
                    subjectName = info.subjectName,
                    className = info.className,
                    topicTerm = info.term,
                    topicWeek = info.weekNumber
                )
                onGenerated(info.topicId)
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to generate lesson note"
                _uiState.value = _uiState.value.copy(isGenerating = false, error = msg)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun generateTeachingGuide(topicId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingGuide = true, error = null)
            try {
                val guide = syllabusRepository.generateTeachingGuide(
                    topicId = topicId,
                    topicTitle = _uiState.value.topicTitle,
                    subjectName = _uiState.value.subjectName,
                    className = _uiState.value.className,
                    term = _uiState.value.topicTerm,
                    weekNumber = _uiState.value.topicWeek
                )
                _uiState.value = _uiState.value.copy(isGeneratingGuide = false, teachingGuide = guide)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGeneratingGuide = false, error = e.message ?: "Failed to generate teaching guide")
            }
        }
    }
}
