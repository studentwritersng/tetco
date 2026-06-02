package com.teacherscompanion.data.repository

import com.teacherscompanion.core.SyncManager
import com.teacherscompanion.data.local.dao.ClassLevelDao
import com.teacherscompanion.data.local.dao.LessonNoteDao
import com.teacherscompanion.data.local.dao.QuestionDao
import com.teacherscompanion.data.local.dao.QuestionHistoryDao
import com.teacherscompanion.data.local.dao.SchoolClassDao
import com.teacherscompanion.data.local.dao.SubjectDao
import com.teacherscompanion.data.local.dao.SyllabusTopicDao
import com.teacherscompanion.data.local.entity.LessonNoteEntity
import com.teacherscompanion.data.local.entity.QuestionEntity
import com.teacherscompanion.data.local.entity.QuestionHistoryEntity
import com.teacherscompanion.data.local.entity.SyllabusTopicEntity
import com.teacherscompanion.data.remote.dto.LessonNoteDto
import com.teacherscompanion.data.remote.dto.QuestionDto
import com.teacherscompanion.data.remote.dto.SyllabusTopicDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.invoke
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class TopicInfo(
    val topicId: String,
    val subjectId: String,
    val topicTitle: String,
    val subjectName: String,
    val className: String,
    val term: String?,
    val weekNumber: Int?
)

@Singleton
class SyllabusRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val syncManager: SyncManager,
    private val syllabusTopicDao: SyllabusTopicDao,
    private val lessonNoteDao: LessonNoteDao,
    private val questionDao: QuestionDao,
    private val questionHistoryDao: QuestionHistoryDao,
    private val subjectDao: SubjectDao,
    private val schoolClassDao: SchoolClassDao,
    private val classLevelDao: ClassLevelDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getTopicsForSubject(subjectId: String): List<SyllabusTopicDto> {
        return syllabusTopicDao.getTopicsForSubject(subjectId).map { it.toDto() }
    }

    suspend fun refreshTopicsForSubject(subjectId: String) {
        val topics = supabaseClient.from("syllabus_topics")
            .select()
            .decodeList<SyllabusTopicDto>()
            .filter { it.subject_id == subjectId }
        syllabusTopicDao.upsertAll(topics.map { it.toEntity() })
    }

    suspend fun addTopic(subjectId: String, title: String, term: String, weekNumber: Int): SyllabusTopicDto {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: throw Exception("Not authenticated")
        val maxOrder = syllabusTopicDao.getTopicsForSubject(subjectId)
            .filter { it.term == term }
            .maxOfOrNull { it.display_order } ?: 0
        val now = Instant.now().toString()
        val entity = SyllabusTopicEntity(
            id = UUID.randomUUID().toString(),
            subject_id = subjectId,
            teacher_id = teacherId,
            title = title,
            term = term,
            week_number = weekNumber,
            display_order = maxOrder + 1,
            has_lesson_note = false,
            created_at = now,
            updated_at = now,
            deleted_at = null
        )
        syllabusTopicDao.upsert(entity)
        val dto = entity.toDto()
        val payload = json.encodeToString(SyllabusTopicDto.serializer(), dto)
        syncManager.queueSync("syllabus_topic", entity.id, "insert", payload)
        return dto
    }

    suspend fun updateTopic(topicId: String, title: String, term: String, weekNumber: Int) {
        val existing = syllabusTopicDao.getById(topicId) ?: return
        val now = Instant.now().toString()
        val updated = existing.copy(title = title, term = term, week_number = weekNumber, updated_at = now)
        syllabusTopicDao.upsert(updated)
        val payload = json.encodeToString(SyllabusTopicDto.serializer(), updated.toDto())
        syncManager.queueSync("syllabus_topic", topicId, "update", payload)
    }

    suspend fun deleteTopic(topicId: String) {
        val now = Instant.now().toString()
        syllabusTopicDao.softDelete(topicId, now)
        syncManager.queueSync("syllabus_topic", topicId, "delete")
    }

    suspend fun reorderTopics(topicOrders: Map<String, Int>) {
        for ((topicId, order) in topicOrders) {
            syllabusTopicDao.updateDisplayOrder(topicId, order)
            val payload = buildJsonObject { put("display_order", order) }.toString()
            syncManager.queueSync("syllabus_topic", topicId, "update", payload)
        }
    }

    suspend fun getLessonNoteForTopic(topicId: String): LessonNoteDto? {
        return lessonNoteDao.getForTopic(topicId)?.toDto()
    }

    suspend fun createLessonNote(topicId: String, content: String, aiGenerated: Boolean = false): LessonNoteDto {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: throw Exception("Not authenticated")
        val now = Instant.now().toString()
        val entity = LessonNoteEntity(
            id = UUID.randomUUID().toString(),
            syllabus_topic_id = topicId,
            teacher_id = teacherId,
            content = content,
            ai_generated = aiGenerated,
            teaching_guide = null,
            created_at = now,
            updated_at = now,
            deleted_at = null
        )
        lessonNoteDao.upsert(entity)
        syllabusTopicDao.updateHasLessonNote(topicId, true)
        val dto = entity.toDto()
        val payload = json.encodeToString(LessonNoteDto.serializer(), dto)
        syncManager.queueSync("lesson_note", entity.id, "insert", payload)
        val topicUpdatePayload = buildJsonObject { put("has_lesson_note", true) }.toString()
        syncManager.queueSync("syllabus_topic", topicId, "update", topicUpdatePayload)
        return dto
    }

    suspend fun updateLessonNote(noteId: String, content: String) {
        lessonNoteDao.updateContent(noteId, content)
        val payload = buildJsonObject { put("content", content) }.toString()
        syncManager.queueSync("lesson_note", noteId, "update", payload)
    }

    suspend fun updateTeachingGuide(noteId: String, guide: String) {
        lessonNoteDao.updateTeachingGuide(noteId, guide)
        val payload = buildJsonObject { put("teaching_guide", guide) }.toString()
        syncManager.queueSync("lesson_note", noteId, "update", payload)
    }

    suspend fun getQuestionsForNote(noteId: String): List<QuestionDto> {
        return questionDao.getForNote(noteId).map { it.toDto() }
    }

    suspend fun refreshQuestionsForNote(noteId: String) {
        val questions = supabaseClient.from("questions")
            .select()
            .decodeList<QuestionDto>()
            .filter { it.lesson_note_id == noteId }
        questionDao.deleteForNote(noteId)
        questionDao.upsertAll(questions.map { it.toEntity() })
    }

    suspend fun getUncoveredTopics(): List<SyllabusTopicDto> {
        return syllabusTopicDao.getUncoveredTopics().map { it.toDto() }
    }

    suspend fun refreshUncoveredTopics() {
        val topics = supabaseClient.from("syllabus_topics")
            .select()
            .decodeList<SyllabusTopicDto>()
            .filter { !it.has_lesson_note && it.deleted_at == null }
        syllabusTopicDao.upsertAll(topics.map { it.toEntity() })
    }

    suspend fun getAllTopicsWithInfo(): List<TopicInfo> {
        val classLevels = classLevelDao.getAll().associateBy { it.id }
        val allSubjects = subjectDao.getAllSubjects()
        val allClasses = schoolClassDao.getAllClasses()
        val result = mutableListOf<TopicInfo>()

        for (subject in allSubjects) {
            val classItem = allClasses.find { it.id == subject.school_class_id }
            val className = classItem?.let { classLevels[it.class_level_id]?.name } ?: "Unknown"
            val topics = syllabusTopicDao.getTopicsForSubject(subject.id)
            for (topic in topics) {
                result.add(
                    TopicInfo(
                        topicId = topic.id,
                        subjectId = subject.id,
                        topicTitle = topic.title,
                        subjectName = subject.name,
                        className = className,
                        term = topic.term,
                        weekNumber = topic.week_number
                    )
                )
            }
        }
        return result.sortedBy { it.topicTitle }
    }

    suspend fun generateLessonNote(
        topicId: String,
        topicTitle: String,
        subjectName: String,
        className: String,
        term: String?,
        weekNumber: Int?
    ): String {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")

        val response = supabaseClient.functions.invoke("generate-lesson-note") {
            setBody(mapOf(
                "topic_id" to topicId,
                "topic_title" to topicTitle,
                "subject_name" to subjectName,
                "class_name" to className,
                "term" to (term ?: "First"),
                "week_number" to (weekNumber ?: 1)
            ))
        }

        fun extractContent(raw: Any?): String {
            return when (raw) {
                is String -> {
                    val parsed = try { json.parseToJsonElement(raw) } catch (_: Exception) { null }
                    val map = parsed as? kotlinx.serialization.json.JsonObject
                    map?.get("content")?.let { kotlinx.serialization.json.Json.decodeFromJsonElement<String>(it) }
                        ?: map?.get("error")?.let { throw Exception(it.toString().trim('"')) }
                        ?: raw
                }
                is Map<*, *> -> {
                    raw["content"] as? String
                        ?: raw["error"] as? String?.let { throw Exception(it) }
                        ?: throw Exception("No content in AI response")
                }
                else -> raw?.toString() ?: throw Exception("Empty AI response")
            }
        }

        val content = extractContent(response)

        val now = Instant.now().toString()

        val existing = lessonNoteDao.getForTopic(topicId)
        val entity = LessonNoteEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            syllabus_topic_id = topicId,
            teacher_id = teacherId,
            content = content,
            ai_generated = true,
            teaching_guide = existing?.teaching_guide,
            created_at = existing?.created_at ?: now,
            updated_at = now,
            deleted_at = null
        )
        lessonNoteDao.upsert(entity)
        syllabusTopicDao.updateHasLessonNote(topicId, true)

        val payload = json.encodeToString(LessonNoteDto.serializer(), entity.toDto())
        syncManager.queueSync("lesson_note", entity.id, "insert", payload)
        syncManager.queueSync("syllabus_topic", topicId, "update",
            buildJsonObject { put("has_lesson_note", true) }.toString())

        return content
    }

    suspend fun generateQuestions(
        subjectId: String,
        classId: String,
        weekStart: Int,
        weekEnd: Int,
        type: String,
        questionCount: Int,
        difficulty: String,
        subjectName: String,
        className: String
    ): Pair<String, List<QuestionDto>> {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")

        val topics = syllabusTopicDao.getTopicsForSubject(subjectId)
            .filter { it.week_number != null && it.week_number in weekStart..weekEnd }
            .sortedBy { it.week_number }

        if (topics.isEmpty()) throw Exception("No topics found in the selected week range")

        val lessonNoteContents = topics.mapNotNull { topic ->
            lessonNoteDao.getForTopic(topic.id)?.content
        }

        if (lessonNoteContents.isEmpty()) throw Exception("No lesson notes found in selected weeks. Generate lesson notes first.")

        val response = supabaseClient.functions.invoke("generate-questions") {
            setBody(mapOf(
                "lesson_note_contents" to lessonNoteContents,
                "type" to type,
                "question_count" to questionCount,
                "subject_name" to subjectName,
                "class_name" to className,
                "difficulty" to difficulty,
                "week_start" to weekStart,
                "week_end" to weekEnd
            ))
        }

        val responseMap = response as? Map<*, *> ?: throw Exception("Invalid response from AI")
        val formattedText = responseMap["formatted_text"] as? String ?: ""
        val questionsRaw = responseMap["questions"] as? List<*> ?: emptyList<Any>()

        val questions = questionsRaw.mapNotNull { it as? Map<*, *> }.map { q ->
            val options = q["options"] as? Map<*, *>
            QuestionDto(
                id = UUID.randomUUID().toString(),
                lesson_note_id = "",
                teacher_id = teacherId,
                type = q["type"] as? String ?: type,
                question_text = q["question_text"] as? String ?: "",
                options = options?.let { json.parseToJsonElement(json.encodeToString(it)) as? JsonObject },
                correct_answer = q["correct_answer"] as? String,
                answer_guide = q["answer_guide"] as? String,
                marks = (q["marks"] as? Number)?.toInt(),
                display_order = 0,
                ai_generated = true,
                created_at = Instant.now().toString(),
                deleted_at = null
            )
        }

        questionHistoryDao.insert(
            QuestionHistoryEntity(
                id = UUID.randomUUID().toString(),
                subjectName = subjectName,
                className = className,
                type = type,
                questionCount = questionCount,
                difficulty = difficulty,
                weekStart = weekStart,
                weekEnd = weekEnd,
                formattedText = formattedText
            )
        )

        return Pair(formattedText, questions)
    }

    suspend fun updateTopicStatus(topicId: String, status: String) {
        syllabusTopicDao.updateStatus(topicId, status)
        val now = Instant.now().toString()
        val payload = buildJsonObject { put("status", status); put("updated_at", now) }.toString()
        syncManager.queueSync("syllabus_topic", topicId, "update", payload)
    }

    suspend fun getGapStats(): Triple<Int, Int, Int> {
        val total = syllabusTopicDao.getTotalCount()
        val completed = syllabusTopicDao.getCompletedCount()
        val uncovered = syllabusTopicDao.getUncoveredCount()
        return Triple(total, completed, uncovered)
    }

    suspend fun generateTeachingGuide(
        topicId: String,
        topicTitle: String,
        subjectName: String,
        className: String,
        term: String?,
        weekNumber: Int?
    ): String {
        val existing = lessonNoteDao.getForTopic(topicId)
            ?: throw Exception("Generate a lesson note first before creating a teaching guide")

        val response = supabaseClient.functions.invoke("generate-teaching-guide") {
            setBody(mapOf(
                "lesson_note_id" to existing.id,
                "topic_title" to topicTitle,
                "subject_name" to subjectName,
                "class_name" to className,
                "term" to (term ?: "First"),
                "week_number" to (weekNumber ?: 1),
                "lesson_note_content" to (existing.content ?: "")
            ))
        }

        fun extractGuide(raw: Any?): String {
            return when (raw) {
                is String -> {
                    val parsed = try { json.parseToJsonElement(raw) } catch (_: Exception) { null }
                    val map = parsed as? kotlinx.serialization.json.JsonObject
                    map?.get("teaching_guide")?.let { kotlinx.serialization.json.Json.decodeFromJsonElement<String>(it) }
                        ?: map?.get("error")?.let { throw Exception(it.toString().trim('"')) }
                        ?: raw
                }
                is Map<*, *> -> {
                    raw["teaching_guide"] as? String
                        ?: raw["error"] as? String?.let { throw Exception(it) }
                        ?: throw Exception("No teaching guide in AI response")
                }
                else -> raw?.toString() ?: throw Exception("Empty AI response")
            }
        }

        val guide = extractGuide(response)
        lessonNoteDao.updateTeachingGuide(existing.id, guide)
        syncManager.queueSync("lesson_note", existing.id, "update",
            buildJsonObject { put("teaching_guide", guide) }.toString())
        return guide
    }

    suspend fun getQuestionHistory(): List<QuestionHistoryEntity> {
        return questionHistoryDao.getAll()
    }

    suspend fun deleteQuestionHistoryItem(id: String) {
        questionHistoryDao.delete(id)
    }

    private fun SyllabusTopicEntity.toDto() = SyllabusTopicDto(
        id = id,
        subject_id = subject_id,
        teacher_id = teacher_id,
        title = title,
        term = term,
        week_number = week_number,
        display_order = display_order,
        has_lesson_note = has_lesson_note,
        status = status,
        created_at = created_at,
        updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun SyllabusTopicDto.toEntity() = SyllabusTopicEntity(
        id = id,
        subject_id = subject_id,
        teacher_id = teacher_id,
        title = title,
        term = term,
        week_number = week_number,
        display_order = display_order,
        has_lesson_note = has_lesson_note,
        status = status,
        created_at = created_at,
        updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun LessonNoteEntity.toDto() = LessonNoteDto(
        id = id,
        syllabus_topic_id = syllabus_topic_id,
        teacher_id = teacher_id,
        content = content,
        ai_generated = ai_generated,
        teaching_guide = teaching_guide,
        created_at = created_at,
        updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun LessonNoteDto.toEntity() = LessonNoteEntity(
        id = id,
        syllabus_topic_id = syllabus_topic_id,
        teacher_id = teacher_id,
        content = content,
        ai_generated = ai_generated,
        teaching_guide = teaching_guide,
        created_at = created_at,
        updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun QuestionEntity.toDto() = QuestionDto(
        id = id,
        lesson_note_id = lesson_note_id,
        teacher_id = teacher_id,
        type = type,
        question_text = question_text,
        options = options_json?.let { json.parseToJsonElement(it) as? JsonObject },
        correct_answer = correct_answer,
        answer_guide = answer_guide,
        marks = marks,
        display_order = display_order,
        ai_generated = ai_generated,
        created_at = created_at,
        deleted_at = deleted_at
    )

    private fun QuestionDto.toEntity() = QuestionEntity(
        id = id,
        lesson_note_id = lesson_note_id,
        teacher_id = teacher_id,
        type = type,
        question_text = question_text,
        options_json = options?.let { json.encodeToString(it) },
        correct_answer = correct_answer,
        answer_guide = answer_guide,
        marks = marks,
        display_order = display_order,
        ai_generated = ai_generated,
        created_at = created_at,
        deleted_at = deleted_at
    )
}
