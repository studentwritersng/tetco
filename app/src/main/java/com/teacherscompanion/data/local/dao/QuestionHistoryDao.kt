package com.teacherscompanion.data.local.dao

import androidx.room.*
import com.teacherscompanion.data.local.entity.QuestionHistoryEntity

@Dao
interface QuestionHistoryDao {
    @Query("SELECT * FROM question_history ORDER BY createdAt DESC")
    suspend fun getAll(): List<QuestionHistoryEntity>

    @Query("SELECT * FROM question_history WHERE id = :id")
    suspend fun getById(id: String): QuestionHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QuestionHistoryEntity)

    @Query("DELETE FROM question_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM question_history")
    suspend fun deleteAll()
}
