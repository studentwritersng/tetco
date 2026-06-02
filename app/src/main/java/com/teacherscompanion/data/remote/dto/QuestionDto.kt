package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class QuestionDto(
    val id: String,
    val lesson_note_id: String,
    val teacher_id: String,
    val type: String,
    val question_text: String,
    val options: JsonObject? = null,
    val correct_answer: String? = null,
    val answer_guide: String? = null,
    val marks: Int? = null,
    val display_order: Int = 0,
    val ai_generated: Boolean = false,
    val created_at: String? = null,
    val deleted_at: String? = null
)
