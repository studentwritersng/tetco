package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyllabusTopicDto(
    val id: String,
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
