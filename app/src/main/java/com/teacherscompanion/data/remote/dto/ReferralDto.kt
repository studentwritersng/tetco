package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReferralCreditDto(
    val id: String,
    val teacher_id: String,
    val month: String,
    val lesson_note_credits: Int = 0,
    val mcq_credits: Int = 0,
    val essay_credits: Int = 0,
    val guide_credits: Int = 0,
    val referral_source: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class ReferralHistoryDto(
    val referrer_id: String,
    val referee_id: String,
    val redacted_email: String,
    val qualified: Boolean,
    val joined_at: String
)
