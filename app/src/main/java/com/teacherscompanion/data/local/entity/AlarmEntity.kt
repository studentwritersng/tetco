package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val teacherId: String = "",
    val type: String = "custom",
    val label: String? = null,
    val timeHour: Int = 6,
    val timeMinute: Int = 0,
    val repeatDays: String = "1,2,3,4,5",
    val sound: String? = null,
    val vibrate: Boolean = true,
    val isActive: Boolean = true,
    val advanceMinutes: Int = 0,
    val schoolId: String? = null,
    val subjectId: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
