package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faq_items_cache")
data class FaqItemCache(
    @PrimaryKey val id: String,
    val categoryId: String,
    val categoryName: String,
    val question: String,
    val answer: String,
    val displayOrder: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
