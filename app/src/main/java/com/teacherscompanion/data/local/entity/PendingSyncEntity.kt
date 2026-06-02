package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payload: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
