package com.teacherscompanion.data.repository

import com.teacherscompanion.core.SyncManager
import com.teacherscompanion.data.local.dao.ClassLevelDao
import com.teacherscompanion.data.local.dao.ProfileDao
import com.teacherscompanion.data.local.dao.SchoolClassDao
import com.teacherscompanion.data.local.dao.SchoolDao
import com.teacherscompanion.data.local.dao.SubjectDao
import com.teacherscompanion.data.local.dao.SyllabusTopicDao
import com.teacherscompanion.data.local.entity.ClassLevelEntity
import com.teacherscompanion.data.local.entity.ProfileEntity
import com.teacherscompanion.data.local.entity.SchoolClassEntity
import com.teacherscompanion.data.local.entity.SchoolEntity
import com.teacherscompanion.data.local.entity.SubjectEntity
import com.teacherscompanion.data.remote.dto.ClassLevelDto
import com.teacherscompanion.data.remote.dto.ProfileDto
import com.teacherscompanion.data.remote.dto.SchoolClassDto
import com.teacherscompanion.data.remote.dto.SchoolClassWithLevelDto
import com.teacherscompanion.data.remote.dto.SchoolDto
import com.teacherscompanion.data.remote.dto.SubjectDto
import com.teacherscompanion.data.remote.dto.SubjectWithCoverageDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val schoolDao: SchoolDao,
    private val schoolClassDao: SchoolClassDao,
    private val subjectDao: SubjectDao,
    private val classLevelDao: ClassLevelDao,
    private val profileDao: ProfileDao,
    private val syllabusTopicDao: SyllabusTopicDao,
    private val syncManager: SyncManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun refreshSchools() {
        val schools = supabaseClient.from("schools")
            .select()
            .decodeList<SchoolDto>()
        schoolDao.upsertAll(schools.map { it.toEntity() })
    }

    suspend fun refreshClassesForSchool(schoolId: String) {
        val classes = supabaseClient.from("school_classes")
            .select() {
                eq("school_id", schoolId)
            }
            .decodeList<SchoolClassDto>()
        schoolClassDao.upsertAll(classes.map { it.toEntity() })
    }

    suspend fun refreshSubjectsForClass(classId: String) {
        val subjects = supabaseClient.from("subjects")
            .select() {
                eq("school_class_id", classId)
            }
            .decodeList<SubjectDto>()
        subjectDao.upsertAll(subjects.map { it.toEntity() })
    }

    suspend fun refreshClassLevels() {
        val levels = supabaseClient.from("class_levels")
            .select()
            .decodeList<ClassLevelDto>()
        classLevelDao.deleteAll()
        classLevelDao.upsertAll(levels.map { it.toEntity() })
    }

    suspend fun getSchools(): List<SchoolDto> {
        return schoolDao.getAllActive().map { it.toDto() }
    }

    suspend fun getSchoolById(schoolId: String): SchoolDto {
        return schoolDao.getById(schoolId)?.toDto()
            ?: throw Exception("School not found: $schoolId")
    }

    suspend fun createSchool(name: String, address: String?, logoUrl: String?): SchoolDto {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")
        val schoolId = UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()
        val entity = SchoolEntity(
            id = schoolId,
            teacher_id = teacherId,
            name = name,
            address = address,
            logo_url = logoUrl,
            is_active = true,
            created_at = now,
            updated_at = now,
            deleted_at = null
        )
        schoolDao.upsert(entity)
        val dto = entity.toDto()
        syncManager.queueSync("school", schoolId, "insert", json.encodeToString(dto))
        return dto
    }

    suspend fun updateSchool(schoolId: String, name: String, address: String?, logoUrl: String?) {
        val existing = schoolDao.getById(schoolId) ?: return
        val now = java.time.Instant.now().toString()
        val updated = existing.copy(
            name = name,
            address = address,
            logo_url = logoUrl,
            updated_at = now
        )
        schoolDao.upsert(updated)
        val dto = updated.toDto()
        syncManager.queueSync("school", schoolId, "update", json.encodeToString(dto))
    }

    suspend fun deleteSchool(schoolId: String) {
        val now = java.time.Instant.now().toString()
        schoolDao.softDelete(schoolId, now)
        syncManager.queueSync("school", schoolId, "delete")
    }

    suspend fun setActiveSchool(schoolId: String) {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return
        profileDao.setActiveSchool(teacherId, schoolId)
        val profile = profileDao.getById(teacherId) ?: return
        val dto = profile.toDto()
        syncManager.queueSync("profile", teacherId, "update", json.encodeToString(dto))
    }

    suspend fun getClassesForSchool(schoolId: String): List<SchoolClassWithLevelDto> {
        val classLevels = classLevelDao.getAll()
        return schoolClassDao.getClassesForSchool(schoolId).map { classItem ->
            val level = classLevels.find { it.id == classItem.class_level_id }
            SchoolClassWithLevelDto(
                id = classItem.id,
                school_id = classItem.school_id,
                teacher_id = classItem.teacher_id,
                class_level_id = classItem.class_level_id,
                class_name = level?.name ?: "Unknown",
                category = level?.category ?: "Unknown",
                display_order = level?.display_order ?: 0
            )
        }
    }

    suspend fun addClassToSchool(schoolId: String, classLevelId: Int): SchoolClassDto {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")
        val classId = UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()
        val entity = SchoolClassEntity(
            id = classId,
            school_id = schoolId,
            teacher_id = teacherId,
            class_level_id = classLevelId,
            created_at = now,
            deleted_at = null
        )
        schoolClassDao.upsert(entity)
        val dto = entity.toDto()
        syncManager.queueSync("school_class", classId, "insert", json.encodeToString(dto))
        return dto
    }

    suspend fun removeClass(classId: String) {
        val now = java.time.Instant.now().toString()
        schoolClassDao.softDelete(classId, now)
        syncManager.queueSync("school_class", classId, "delete")
    }

    suspend fun getSubjectsForClass(classId: String): List<SubjectWithCoverageDto> {
        return subjectDao.getSubjectsForClass(classId).map { subject ->
            val topics = syllabusTopicDao.getTopicsForSubject(subject.id)
            SubjectWithCoverageDto(
                id = subject.id,
                school_class_id = subject.school_class_id,
                teacher_id = subject.teacher_id,
                name = subject.name,
                total_topics = topics.size,
                covered_topics = topics.count { it.has_lesson_note }
            )
        }
    }

    suspend fun addSubject(classId: String, name: String): SubjectDto {
        val teacherId = supabaseClient.auth.currentSessionOrNull()?.user?.id
            ?: throw Exception("Not authenticated")
        val subjectId = UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()
        val entity = SubjectEntity(
            id = subjectId,
            school_class_id = classId,
            teacher_id = teacherId,
            name = name,
            created_at = now,
            updated_at = now,
            deleted_at = null
        )
        subjectDao.upsert(entity)
        val dto = entity.toDto()
        syncManager.queueSync("subject", subjectId, "insert", json.encodeToString(dto))
        return dto
    }

    suspend fun updateSubject(subjectId: String, name: String) {
        subjectDao.updateName(subjectId, name)
        val entity = subjectDao.getById(subjectId) ?: return
        val dto = entity.toDto()
        syncManager.queueSync("subject", subjectId, "update", json.encodeToString(dto))
    }

    suspend fun deleteSubject(subjectId: String) {
        val now = java.time.Instant.now().toString()
        subjectDao.softDelete(subjectId, now)
        syncManager.queueSync("subject", subjectId, "delete")
    }

    suspend fun getClassLevels(): List<ClassLevelDto> {
        return classLevelDao.getAll().map { it.toDto() }
    }

    private fun SchoolEntity.toDto() = SchoolDto(
        id = id, teacher_id = teacher_id, name = name, address = address,
        logo_url = logo_url, is_active = is_active, created_at = created_at,
        updated_at = updated_at, deleted_at = deleted_at
    )

    private fun SchoolDto.toEntity() = SchoolEntity(
        id = id, teacher_id = teacher_id, name = name, address = address,
        logo_url = logo_url, is_active = is_active, created_at = created_at,
        updated_at = updated_at, deleted_at = deleted_at
    )

    private fun SchoolClassEntity.toDto() = SchoolClassDto(
        id = id, school_id = school_id, teacher_id = teacher_id,
        class_level_id = class_level_id, created_at = created_at, deleted_at = deleted_at
    )

    private fun SchoolClassDto.toEntity() = SchoolClassEntity(
        id = id, school_id = school_id, teacher_id = teacher_id,
        class_level_id = class_level_id, created_at = created_at, deleted_at = deleted_at
    )

    private fun SubjectEntity.toDto() = SubjectDto(
        id = id, school_class_id = school_class_id, teacher_id = teacher_id,
        name = name, created_at = created_at, updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun SubjectDto.toEntity() = SubjectEntity(
        id = id, school_class_id = school_class_id, teacher_id = teacher_id,
        name = name, created_at = created_at, updated_at = updated_at,
        deleted_at = deleted_at
    )

    private fun ClassLevelEntity.toDto() = ClassLevelDto(
        id = id, category = category, name = name, display_order = display_order
    )

    private fun ClassLevelDto.toEntity() = ClassLevelEntity(
        id = id, category = category, name = name, display_order = display_order
    )

    private fun ProfileEntity.toDto() = ProfileDto(
        id = id, full_name = full_name, phone = phone, avatar_url = avatar_url,
        referral_code = referral_code, referred_by = referred_by, plan = plan,
        plan_expires_at = plan_expires_at, ai_credits_used = ai_credits_used,
        fcm_token = fcm_token, active_school_id = active_school_id,
        gap_digest_enabled = gap_digest_enabled, gap_immediate_enabled = gap_immediate_enabled,
        notification_sound = notification_sound, notification_vibrate = notification_vibrate,
        plan_id = plan_id, plan_name = plan_name, referred_by_code = referred_by_code,
        referral_reward_issued = referral_reward_issued, created_at = created_at,
        updated_at = updated_at, deleted_at = deleted_at
    )
}
