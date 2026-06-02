package com.teacherscompanion.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.local.dao.SchoolClassDao
import com.teacherscompanion.data.local.dao.SubjectDao
import com.teacherscompanion.data.local.dao.ClassLevelDao
import com.teacherscompanion.data.local.dao.SyllabusTopicDao
import com.teacherscompanion.data.local.entity.ClassLevelEntity
import com.teacherscompanion.data.local.entity.QuestionHistoryEntity
import com.teacherscompanion.data.local.entity.SchoolClassEntity
import com.teacherscompanion.data.local.entity.SubjectEntity
import com.teacherscompanion.data.remote.dto.QuestionDto
import com.teacherscompanion.data.repository.ProfileRepository
import com.teacherscompanion.data.repository.SyllabusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionGeneratorUiState(
    val classes: List<ClassItem> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedClassId: String = "",
    val selectedClassLabel: String = "",
    val selectedSubjectId: String = "",
    val selectedSubjectName: String = "",
    val questionType: String = "mcq",
    val questionCount: Int = 20,
    val difficulty: String = "medium",
    val weekStart: Int = 1,
    val weekEnd: Int = 2,
    val minWeek: Int = 1,
    val maxWeek: Int = 14,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val formattedText: String? = null,
    val questions: List<QuestionDto> = emptyList(),
    val history: List<QuestionHistoryEntity> = emptyList(),
    val error: String? = null
)

data class ClassItem(val id: String, val label: String)

@HiltViewModel
class QuestionGeneratorViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val profileRepository: ProfileRepository,
    private val schoolClassDao: SchoolClassDao,
    private val subjectDao: SubjectDao,
    private val classLevelDao: ClassLevelDao,
    private val syllabusTopicDao: SyllabusTopicDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionGeneratorUiState())
    val uiState: StateFlow<QuestionGeneratorUiState> = _uiState.asStateFlow()

    init {
        loadClasses()
        loadHistory()
    }

    private fun loadClasses() {
        viewModelScope.launch {
            try {
                val classLevels = classLevelDao.getAll().associateBy { it.id }
                val classes = schoolClassDao.getAllClasses().map {
                    val levelName = classLevels[it.class_level_id]?.name ?: "Unknown"
                    ClassItem(id = it.id, label = "$levelName (${it.id.take(4)})")
                }
                _uiState.value = _uiState.value.copy(classes = classes)
            } catch (_: Exception) { }
        }
    }

    fun selectClass(classId: String) {
        viewModelScope.launch {
            val classItem = _uiState.value.classes.find { it.id == classId }
            val subjects = subjectDao.getSubjectsForClass(classId)
            _uiState.value = _uiState.value.copy(
                selectedClassId = classId,
                selectedClassLabel = classItem?.label ?: "",
                subjects = subjects,
                selectedSubjectId = "",
                selectedSubjectName = ""
            )
        }
    }

    fun selectSubject(subjectId: String) {
        viewModelScope.launch {
            val subject = _uiState.value.subjects.find { it.id == subjectId }
            val topics = syllabusTopicDao.getTopicsForSubject(subjectId)
            val weeks = topics.mapNotNull { it.week_number }.sorted()
            _uiState.value = _uiState.value.copy(
                selectedSubjectId = subjectId,
                selectedSubjectName = subject?.name ?: "",
                minWeek = weeks.firstOrNull() ?: 1,
                maxWeek = weeks.lastOrNull() ?: 14,
                weekStart = weeks.firstOrNull() ?: 1,
                weekEnd = weeks.lastOrNull() ?: 2
            )
        }
    }

    fun setQuestionType(type: String) {
        val defaultCount = if (type == "mcq") 20 else 5
        _uiState.value = _uiState.value.copy(questionType = type, questionCount = defaultCount)
    }

    fun setQuestionCount(count: Int) { _uiState.value = _uiState.value.copy(questionCount = count) }
    fun setDifficulty(d: String) { _uiState.value = _uiState.value.copy(difficulty = d) }
    fun setWeekStart(w: Int) { _uiState.value = _uiState.value.copy(weekStart = w) }
    fun setWeekEnd(w: Int) { _uiState.value = _uiState.value.copy(weekEnd = w) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun generate() {
        val state = _uiState.value
        if (state.selectedSubjectId.isEmpty()) {
            _uiState.value = state.copy(error = "Please select a class and subject first")
            return
        }
        if (state.weekStart > state.weekEnd) {
            _uiState.value = state.copy(error = "Week start must be ≤ week end")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null, formattedText = null)
            try {
                val (text, questions) = syllabusRepository.generateQuestions(
                    subjectId = state.selectedSubjectId,
                    classId = state.selectedClassId,
                    weekStart = state.weekStart,
                    weekEnd = state.weekEnd,
                    type = state.questionType,
                    questionCount = state.questionCount,
                    difficulty = state.difficulty,
                    subjectName = state.selectedSubjectName,
                    className = state.selectedClassLabel
                )
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    formattedText = text,
                    questions = questions
                )
                loadHistory()
            } catch (e: Exception) {
                val msg = e.message ?: "Generation failed"
                _uiState.value = _uiState.value.copy(isGenerating = false, error = msg)
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val h = syllabusRepository.getQuestionHistory()
                _uiState.value = _uiState.value.copy(history = h)
            } catch (_: Exception) { }
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            syllabusRepository.deleteQuestionHistoryItem(id)
            loadHistory()
        }
    }
}
