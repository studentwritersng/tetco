package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "syllabus_topics", indices = [Index("subject_id"), Index("teacher_id")])
data class SyllabusTopicEntity(
    @PrimaryKey val id: String,
    val subject_id: String,
    val teacher_id: String,
    val title: String,
    val term: String? = null,
    val week_number: Int? = null,
    val display_order: Int = 0,
    val has_lesson_note: Boolean = false,
    val status: String = "pending",
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Entity(tableName = "lesson_notes", indices = [Index("syllabus_topic_id"), Index("teacher_id")])
data class LessonNoteEntity(
    @PrimaryKey val id: String,
    val syllabus_topic_id: String,
    val teacher_id: String,
    val content: String? = null,
    val ai_generated: Boolean = false,
    val teaching_guide: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Entity(tableName = "questions", indices = [Index("lesson_note_id"), Index("teacher_id")])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val lesson_note_id: String,
    val teacher_id: String,
    val type: String,
    val question_text: String,
    val options_json: String? = null,
    val correct_answer: String? = null,
    val answer_guide: String? = null,
    val marks: Int? = null,
    val display_order: Int = 0,
    val ai_generated: Boolean = false,
    val created_at: String? = null,
    val deleted_at: String? = null
)
