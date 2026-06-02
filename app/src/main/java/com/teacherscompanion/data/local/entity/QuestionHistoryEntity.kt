package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_history")
data class QuestionHistoryEntity(
    @PrimaryKey val id: String,
    val subjectName: String,
    val className: String,
    val type: String,
    val questionCount: Int,
    val difficulty: String,
    val weekStart: Int? = null,
    val weekEnd: Int? = null,
    val formattedText: String,
    val createdAt: Long = System.currentTimeMillis()
)
