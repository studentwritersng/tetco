package com.teacherscompanion.data.local.dao

import androidx.room.*
import com.teacherscompanion.data.local.entity.SyllabusTopicEntity
import com.teacherscompanion.data.local.entity.LessonNoteEntity
import com.teacherscompanion.data.local.entity.QuestionEntity

@Dao
interface SyllabusTopicDao {
    @Query("SELECT * FROM syllabus_topics WHERE subject_id = :subjectId AND deleted_at IS NULL ORDER BY term, week_number, display_order")
    suspend fun getTopicsForSubject(subjectId: String): List<SyllabusTopicEntity>

    @Query("SELECT * FROM syllabus_topics WHERE id = :id")
    suspend fun getById(id: String): SyllabusTopicEntity?

    @Query("SELECT * FROM syllabus_topics WHERE deleted_at IS NULL AND has_lesson_note = 0")
    suspend fun getUncoveredTopics(): List<SyllabusTopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(topic: SyllabusTopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(topics: List<SyllabusTopicEntity>)

    @Query("UPDATE syllabus_topics SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("UPDATE syllabus_topics SET display_order = :order WHERE id = :id")
    suspend fun updateDisplayOrder(id: String, order: Int)

    @Query("UPDATE syllabus_topics SET has_lesson_note = :hasNote WHERE id = :id")
    suspend fun updateHasLessonNote(id: String, hasNote: Boolean)

    @Query("UPDATE syllabus_topics SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM syllabus_topics WHERE deleted_at IS NULL AND status != 'completed' ORDER BY term, week_number, display_order")
    suspend fun getGapTopics(): List<SyllabusTopicEntity>

    @Query("SELECT COUNT(*) FROM syllabus_topics WHERE deleted_at IS NULL")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM syllabus_topics WHERE deleted_at IS NULL AND status = 'completed'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM syllabus_topics WHERE deleted_at IS NULL AND has_lesson_note = 0")
    suspend fun getUncoveredCount(): Int
}

@Dao
interface LessonNoteDao {
    @Query("SELECT * FROM lesson_notes WHERE syllabus_topic_id = :topicId AND deleted_at IS NULL")
    suspend fun getForTopic(topicId: String): LessonNoteEntity?

    @Query("SELECT * FROM lesson_notes WHERE id = :id")
    suspend fun getById(id: String): LessonNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: LessonNoteEntity)

    @Query("UPDATE lesson_notes SET content = :content WHERE id = :id")
    suspend fun updateContent(id: String, content: String)

    @Query("UPDATE lesson_notes SET teaching_guide = :guide WHERE id = :id")
    suspend fun updateTeachingGuide(id: String, guide: String)

    @Query("UPDATE lesson_notes SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE lesson_note_id = :noteId AND deleted_at IS NULL ORDER BY display_order")
    suspend fun getForNote(noteId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(questions: List<QuestionEntity>)

    @Query("UPDATE questions SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM questions WHERE lesson_note_id = :noteId")
    suspend fun deleteForNote(noteId: String)
}
