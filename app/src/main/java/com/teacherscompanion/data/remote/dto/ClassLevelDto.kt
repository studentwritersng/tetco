package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassLevelDto(
    val id: Int,
    val category: String,
    val name: String,
    val display_order: Int
)
