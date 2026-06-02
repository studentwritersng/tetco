package com.teacherscompanion.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.local.dao.SubjectDao
import com.teacherscompanion.data.local.dao.SyllabusTopicDao
import com.teacherscompanion.data.remote.dto.SyllabusTopicDto
import com.teacherscompanion.data.repository.SyllabusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GapListUiState(
    val topics: List<SyllabusTopicDto> = emptyList(),
    val totalTopics: Int = 0,
    val completedTopics: Int = 0,
    val uncoveredTopics: Int = 0,
    val completionPercent: Float = 0f,
    val subjectGroups: Map<String, List<SyllabusTopicDto>> = emptyMap(),
    val isLoading: Boolean = false
)

@HiltViewModel
class GapListViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val syllabusTopicDao: SyllabusTopicDao,
    private val subjectDao: SubjectDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GapListUiState())
    val uiState: StateFlow<GapListUiState> = _uiState.asStateFlow()

    init {
        loadGaps()
    }

    fun loadGaps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val (total, completed, uncovered) = syllabusRepository.getGapStats()
                val gapTopics = syllabusTopicDao.getGapTopics().map { entity ->
                    SyllabusTopicDto(
                        id = entity.id,
                        subject_id = entity.subject_id,
                        teacher_id = entity.teacher_id,
                        title = entity.title,
                        term = entity.term,
                        week_number = entity.week_number,
                        display_order = entity.display_order,
                        has_lesson_note = entity.has_lesson_note,
                        status = entity.status,
                        created_at = entity.created_at,
                        updated_at = entity.updated_at,
                        deleted_at = entity.deleted_at
                    )
                }
                val subjectIds = gapTopics.map { it.subject_id }.distinct()
                val subjectMap = subjectIds.associateWith { id ->
                    subjectDao.getById(id)?.name ?: "Unknown"
                }
                val groups = gapTopics.groupBy { subjectMap[it.subject_id] ?: "Unknown" }
                val percent = if (total > 0) (completed.toFloat() / total) * 100f else 0f
                _uiState.value = GapListUiState(
                    topics = gapTopics,
                    totalTopics = total,
                    completedTopics = completed,
                    uncoveredTopics = uncovered,
                    completionPercent = percent,
                    subjectGroups = groups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateTopicStatus(topicId: String, status: String) {
        viewModelScope.launch {
            syllabusRepository.updateTopicStatus(topicId, status)
            loadGaps()
        }
    }
}
