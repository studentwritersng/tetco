package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val id: String,
    val school_class_id: String,
    val teacher_id: String,
    val name: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class SubjectWithCoverageDto(
    val id: String,
    val school_class_id: String,
    val teacher_id: String,
    val name: String,
    val total_topics: Int = 0,
    val covered_topics: Int = 0
)
