package com.teacherscompanion.data.local.dao

import androidx.room.*
import com.teacherscompanion.data.local.entity.ClassLevelEntity
import com.teacherscompanion.data.local.entity.PlanEntity
import com.teacherscompanion.data.local.entity.ProfileEntity
import com.teacherscompanion.data.local.entity.ReferralHistoryEntity

@Dao
interface ClassLevelDao {
    @Query("SELECT * FROM class_levels ORDER BY display_order")
    suspend fun getAll(): List<ClassLevelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(levels: List<ClassLevelEntity>)

    @Query("DELETE FROM class_levels")
    suspend fun deleteAll()
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans WHERE is_active = 1")
    suspend fun getAllActive(): List<PlanEntity>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getById(id: String): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(plans: List<PlanEntity>)

    @Query("DELETE FROM plans")
    suspend fun deleteAll()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE profiles SET active_school_id = :schoolId WHERE id = :id")
    suspend fun setActiveSchool(id: String, schoolId: String)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}

@Dao
interface ReferralHistoryDao {
    @Query("SELECT * FROM referral_history ORDER BY joined_at DESC")
    suspend fun getAll(): List<ReferralHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(history: List<ReferralHistoryEntity>)

    @Query("DELETE FROM referral_history")
    suspend fun deleteAll()
}
