package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SchoolClassDto(
    val id: String,
    val school_id: String,
    val teacher_id: String,
    val class_level_id: Int,
    val created_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class SchoolClassWithLevelDto(
    val id: String,
    val school_id: String,
    val teacher_id: String,
    val class_level_id: Int,
    val class_name: String,
    val category: String,
    val display_order: Int,
    val subject_count: Int = 0
)
