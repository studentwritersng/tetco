package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LessonNoteDto(
    val id: String,
    val syllabus_topic_id: String,
    val teacher_id: String,
    val content: String? = null,
    val ai_generated: Boolean = false,
    val teaching_guide: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)
