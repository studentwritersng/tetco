package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlarmDto(
    val id: String,
    val teacher_id: String,
    val type: String,
    val label: String? = null,
    val time: String,
    val days_of_week: List<Int> = listOf(1, 2, 3, 4, 5),
    val is_active: Boolean = true,
    val sound_url: String? = null,
    val vibrate: Boolean = true,
    val school_id: String? = null,
    val subject_id: String? = null,
    val advance_minutes: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class PeriodReminderDto(
    val id: String,
    val teacher_id: String,
    val school_id: String? = null,
    val subject_id: String? = null,
    val name: String,
    val start_time: String,
    val repeat_days: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI"),
    val advance_minutes: Int = 10,
    val is_enabled: Boolean = true,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)
