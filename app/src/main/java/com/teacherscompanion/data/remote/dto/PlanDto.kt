package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlanDto(
    val id: String,
    val name: String,
    val price_ngn: Int = 0,
    val is_free: Boolean = false,
    val lesson_note_limit: Int? = null,
    val mcq_limit: Int? = null,
    val essay_limit: Int? = null,
    val teaching_guide_limit: Int? = null,
    val mcq_per_generation: Int = 5,
    val essay_per_generation: Int = 3,
    val paystack_plan_code: String? = null,
    val is_active: Boolean = true
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val teacher_id: String,
    val plan_id: String,
    val status: String = "active",
    val paystack_customer_id: String? = null,
    val paystack_subscription_code: String? = null,
    val current_period_start: String? = null,
    val current_period_end: String? = null,
    val cancel_at_period_end: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class AiUsageDto(
    val id: String,
    val teacher_id: String,
    val month: String,
    val lesson_notes_generated: Int = 0,
    val questions_generated: Int = 0,
    val teaching_guides_generated: Int = 0,
    val updated_at: String? = null
)
