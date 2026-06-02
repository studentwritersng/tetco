package com.teacherscompanion.data.repository

import com.teacherscompanion.core.SyncManager
import com.teacherscompanion.data.local.dao.PlanDao
import com.teacherscompanion.data.local.dao.ProfileDao
import com.teacherscompanion.data.local.dao.ReferralHistoryDao
import com.teacherscompanion.data.local.entity.PlanEntity
import com.teacherscompanion.data.local.entity.ProfileEntity
import com.teacherscompanion.data.local.entity.ReferralHistoryEntity
import com.teacherscompanion.data.remote.dto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val profileDao: ProfileDao,
    private val planDao: PlanDao,
    private val referralHistoryDao: ReferralHistoryDao,
    private val syncManager: SyncManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheValidityMs = 5 * 60 * 1000L

    suspend fun getProfile(): ProfileDto {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")
        val cached = profileDao.getById(userId)
        if (cached != null && (System.currentTimeMillis() - cached.cached_at) < cacheValidityMs) {
            return cached.toDto()
        }
        return refreshProfile()
    }

    suspend fun refreshProfile(): ProfileDto {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")
        val dto = supabaseClient.from("profiles")
            .select()
            .decodeList<ProfileDto>()
            .first { it.id == userId }
        profileDao.upsert(dto.toEntity())
        return dto
    }

    suspend fun updateProfile(fullName: String, phone: String?) {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return
        val cached = profileDao.getById(userId)
        val updated = (cached ?: ProfileEntity(
            id = userId,
            referral_code = "",
            full_name = fullName,
            phone = phone
        )).copy(
            full_name = fullName,
            phone = phone,
            cached_at = System.currentTimeMillis()
        )
        profileDao.upsert(updated)
        val payload = json.encodeToString(
            buildJsonObject {
                put("full_name", fullName)
                put("phone", phone)
            }
        )
        syncManager.queueSync(
            entityType = "profile",
            entityId = userId,
            action = "update",
            payload = payload
        )
    }

    suspend fun uploadAvatar(bytes: ByteArray, contentType: String): String {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")
        val path = "$userId/profile.jpg"
        supabaseClient.storage.from("avatars").upload(path, bytes, upsert = true)
        val url = supabaseClient.storage.from("avatars").publicUrl(path)
        val cached = profileDao.getById(userId)
        val updated = (cached ?: ProfileEntity(
            id = userId,
            referral_code = ""
        )).copy(
            avatar_url = url,
            cached_at = System.currentTimeMillis()
        )
        profileDao.upsert(updated)
        val payload = json.encodeToString(
            buildJsonObject {
                put("avatar_url", url)
            }
        )
        syncManager.queueSync(
            entityType = "profile",
            entityId = userId,
            action = "update",
            payload = payload
        )
        return url
    }

    suspend fun getSubscription(): SubscriptionDto? {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return null
        return supabaseClient.from("subscriptions")
            .select()
            .decodeList<SubscriptionDto>()
            .firstOrNull { it.teacher_id == userId }
    }

    suspend fun getPlans(): List<PlanDto> {
        val cached = planDao.getAllActive()
        if (cached.isNotEmpty()) {
            return cached.map { it.toDto() }
        }
        return refreshPlans()
    }

    suspend fun refreshPlans(): List<PlanDto> {
        val dtos = supabaseClient.from("plans")
            .select()
            .decodeList<PlanDto>()
        planDao.deleteAll()
        planDao.upsertAll(dtos.map { it.toEntity() })
        return dtos
    }

    suspend fun getAiUsage(): AiUsageDto? {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return null
        val month = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
        return supabaseClient.from("ai_usage")
            .select()
            .decodeList<AiUsageDto>()
            .firstOrNull { it.teacher_id == userId && it.month == month }
    }

    suspend fun getReferralCredits(): List<ReferralCreditDto> {
        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return emptyList()
        return supabaseClient.from("referral_credits")
            .select()
            .decodeList<ReferralCreditDto>()
            .filter { it.teacher_id == userId }
    }

    suspend fun getReferralHistory(): List<ReferralHistoryDto> {
        val cached = referralHistoryDao.getAll()
        if (cached.isNotEmpty()) {
            return cached.map { it.toDto() }
        }
        return refreshReferralHistory()
    }

    suspend fun refreshReferralHistory(): List<ReferralHistoryDto> {
        val dtos = supabaseClient.from("referral_history")
            .select()
            .decodeList<ReferralHistoryDto>()
        referralHistoryDao.deleteAll()
        referralHistoryDao.upsertAll(dtos.map { it.toEntity() })
        return dtos
    }

    private fun ProfileEntity.toDto() = ProfileDto(
        id = id,
        full_name = full_name,
        phone = phone,
        avatar_url = avatar_url,
        referral_code = referral_code,
        referred_by = referred_by,
        plan = plan,
        plan_expires_at = plan_expires_at,
        ai_credits_used = ai_credits_used,
        fcm_token = fcm_token,
        active_school_id = active_school_id,
        gap_digest_enabled = gap_digest_enabled,
        gap_immediate_enabled = gap_immediate_enabled,
        notification_sound = notification_sound,
        notification_vibrate = notification_vibrate,
        plan_id = plan_id,
        plan_name = plan_name,
        referred_by_code = referred_by_code,
        referral_reward_issued = referral_reward_issued,
        created_at = created_at,
        updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun ProfileDto.toEntity() = ProfileEntity(
        id = id,
        full_name = full_name,
        phone = phone,
        avatar_url = avatar_url,
        referral_code = referral_code,
        referred_by = referred_by,
        plan = plan,
        plan_expires_at = plan_expires_at,
        ai_credits_used = ai_credits_used,
        fcm_token = fcm_token,
        active_school_id = active_school_id,
        gap_digest_enabled = gap_digest_enabled,
        gap_immediate_enabled = gap_immediate_enabled,
        notification_sound = notification_sound,
        notification_vibrate = notification_vibrate,
        plan_id = plan_id,
        plan_name = plan_name,
        referred_by_code = referred_by_code,
        referral_reward_issued = referral_reward_issued,
        created_at = created_at,
        updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun PlanEntity.toDto() = PlanDto(
        id = id,
        name = name,
        price_ngn = price_ngn,
        is_free = is_free,
        lesson_note_limit = lesson_note_limit,
        mcq_limit = mcq_limit,
        essay_limit = essay_limit,
        teaching_guide_limit = teaching_guide_limit,
        mcq_per_generation = mcq_per_generation,
        essay_per_generation = essay_per_generation,
        paystack_plan_code = paystack_plan_code,
        is_active = is_active
    )

    private fun PlanDto.toEntity() = PlanEntity(
        id = id,
        name = name,
        price_ngn = price_ngn,
        is_free = is_free,
        lesson_note_limit = lesson_note_limit,
        mcq_limit = mcq_limit,
        essay_limit = essay_limit,
        teaching_guide_limit = teaching_guide_limit,
        mcq_per_generation = mcq_per_generation,
        essay_per_generation = essay_per_generation,
        paystack_plan_code = paystack_plan_code,
        is_active = is_active
    )

    private fun ReferralHistoryEntity.toDto() = ReferralHistoryDto(
        referrer_id = referrer_id,
        referee_id = referee_id,
        redacted_email = redacted_email,
        qualified = qualified,
        joined_at = joined_at
    )

    private fun ReferralHistoryDto.toEntity() = ReferralHistoryEntity(
        referrer_id = referrer_id,
        referee_id = referee_id,
        redacted_email = redacted_email,
        qualified = qualified,
        joined_at = joined_at
    )
}
