package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SchoolDto(
    val id: String,
    val teacher_id: String,
    val name: String,
    val address: String? = null,
    val logo_url: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)
