package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "schools", indices = [Index("teacher_id")])
data class SchoolEntity(
    @PrimaryKey val id: String,
    val teacher_id: String,
    val name: String,
    val address: String? = null,
    val logo_url: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Entity(tableName = "school_classes", indices = [Index("school_id"), Index("teacher_id")])
data class SchoolClassEntity(
    @PrimaryKey val id: String,
    val school_id: String,
    val teacher_id: String,
    val class_level_id: Int,
    val created_at: String? = null,
    val deleted_at: String? = null
)

@Entity(tableName = "subjects", indices = [Index("school_class_id"), Index("teacher_id")])
data class SubjectEntity(
    @PrimaryKey val id: String,
    val school_class_id: String,
    val teacher_id: String,
    val name: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)
