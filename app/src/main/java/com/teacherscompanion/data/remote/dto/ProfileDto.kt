package com.teacherscompanion.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
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
    val deleted_at: String? = null
)
