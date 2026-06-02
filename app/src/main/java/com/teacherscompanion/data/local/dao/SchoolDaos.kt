package com.teacherscompanion.data.local.dao

import androidx.room.*
import com.teacherscompanion.data.local.entity.SchoolEntity
import com.teacherscompanion.data.local.entity.SchoolClassEntity
import com.teacherscompanion.data.local.entity.SubjectEntity

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools WHERE deleted_at IS NULL ORDER BY created_at DESC")
    suspend fun getAllActive(): List<SchoolEntity>

    @Query("SELECT * FROM schools WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): SchoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(school: SchoolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(schools: List<SchoolEntity>)

    @Query("UPDATE schools SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)
}

@Dao
interface SchoolClassDao {
    @Query("SELECT * FROM school_classes WHERE school_id = :schoolId AND deleted_at IS NULL ORDER BY created_at ASC")
    suspend fun getClassesForSchool(schoolId: String): List<SchoolClassEntity>

    @Query("SELECT * FROM school_classes WHERE deleted_at IS NULL")
    suspend fun getAllClasses(): List<SchoolClassEntity>

    @Query("SELECT * FROM school_classes WHERE id = :id")
    suspend fun getById(id: String): SchoolClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(classItem: SchoolClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(classes: List<SchoolClassEntity>)

    @Query("UPDATE school_classes SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE school_class_id = :classId AND deleted_at IS NULL ORDER BY name ASC")
    suspend fun getSubjectsForClass(classId: String): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE deleted_at IS NULL")
    suspend fun getAllSubjects(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<SubjectEntity>)

    @Query("UPDATE subjects SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("UPDATE subjects SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String)
}
