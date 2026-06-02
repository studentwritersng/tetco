package com.teacherscompanion.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "class_levels")
data class ClassLevelEntity(
    @PrimaryKey val id: Int,
    val category: String,
    val name: String,
    val display_order: Int
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
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

@Entity(tableName = "profiles", indices = [Index("referred_by")])
data class ProfileEntity(
    @PrimaryKey val id: String,
    val full_name: String? = null,
    val phone: String? = null,
    val avatar_url: String? = null,
    val referral_code: String,
    val referred_by: String? = null,
    val plan: String = "basic",
    val plan_expires_at: String? = null,
    val ai_credits_used: Int = 0,
    val fcm_token: String? = null,
    val active_school_id: String? = null,
    val gap_digest_enabled: Boolean = true,
    val gap_immediate_enabled: Boolean = true,
    val notification_sound: Boolean = true,
    val notification_vibrate: Boolean = true,
    val plan_id: String? = null,
    val plan_name: String = "Basic",
    val referred_by_code: String? = null,
    val referral_reward_issued: Boolean = false,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null,
    val cached_at: Long = System.currentTimeMillis()
)

@Entity(tableName = "referral_history")
data class ReferralHistoryEntity(
    @PrimaryKey val referee_id: String,
    val referrer_id: String,
    val redacted_email: String,
    val qualified: Boolean,
    val joined_at: String
)
